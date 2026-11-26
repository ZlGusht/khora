package khora.query.neo4j;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.TreeMap;
import java.util.TreeSet;
import khora.events.AcceptedAction;
import khora.events.Assertion;
import khora.events.Assertion.Literal;
import khora.events.Assertion.Ref;
import khora.events.Assertion.Value;
import khora.events.Basic;
import khora.events.Event;
import khora.events.EventId;
import khora.events.EventSource;
import khora.events.Frontier;
import khora.events.Identity;
import khora.events.json.Json;
import khora.events.log.EventLog;
import khora.events.log.Following;
import khora.events.log.Logged;
import khora.query.Answer;
import khora.query.Datastore;
import khora.query.Evaluator;
import khora.query.Graph;
import khora.query.Query;
import org.neo4j.driver.AuthTokens;
import org.neo4j.driver.Driver;
import org.neo4j.driver.GraphDatabase;
import org.neo4j.driver.Session;
import org.neo4j.driver.SessionConfig;
import org.neo4j.driver.TransactionContext;

/**
 * The graph datastore, on Neo4j: the entities, attributes and relationships that the log's
 * events assert, as they stand now ([[The graph datastore materializes entities, not events]]).
 *
 * <p>Each thing is a {@code Thing} node with its identity as {@code id}. A thing that was brought
 * into being, {@code (,,)}, is also an {@code Entity}. A literal value is a property of its thing,
 * keyed by the attribute's identity as it is: one value is the property, several are a list. A
 * reference is a relationship from the thing to the thing it names, typed by the attribute's
 * identity, holding the identity of its statement. A retraction deletes what its target
 * asserted: the entity label, the value, or the relationship. The target is read from its
 * event. Events are not stored; a {@code Progress} node holds the last log position taken in and
 * the frontier of everything taken in.
 *
 * <p>It answers only at that frontier. The datastore owns its database: {@link #clear()} deletes
 * everything with these labels.
 *
 * <p>Provisional: a thing's values of one attribute are a set: asserting a value it has adds
 * nothing, and retracting it removes it, though another statement asserted it too. Values of one
 * attribute of one thing are all integers or all strings: Neo4j lists hold one type, and an
 * assert that mixes them is refused. {@link Evaluator} answers over three Cypher lookups; nothing
 * is pushed down into Cypher yet (P2).
 */
public final class Neo4jDatastore implements Datastore, AutoCloseable {
    static final int PAGE = 1000;

    private static final List<String> SCHEMA = List.of(
            "CREATE CONSTRAINT khora_thing IF NOT EXISTS FOR (t:Thing) REQUIRE t.id IS UNIQUE",
            "CREATE CONSTRAINT khora_progress IF NOT EXISTS FOR (p:Progress) REQUIRE p.key IS UNIQUE");

    private final Driver driver;
    private final boolean ownsDriver;
    private final SessionConfig config;

    /**
     * A datastore in the database {@code neo4j} of the server at {@code uri}, which it closes with
     * itself.
     */
    public static Neo4jDatastore open(String uri, String user, String password) {
        var driver = GraphDatabase.driver(uri, AuthTokens.basic(user, password));
        try {
            driver.verifyConnectivity();
            return new Neo4jDatastore(driver, "neo4j", true);
        } catch (RuntimeException e) {
            driver.close();
            throw e;
        }
    }

    /** A datastore in {@code database}, through {@code driver}, which the caller keeps and closes. */
    public Neo4jDatastore(Driver driver, String database) {
        this(driver, database, false);
    }

    private Neo4jDatastore(Driver driver, String database, boolean ownsDriver) {
        this.driver = Objects.requireNonNull(driver, "driver");
        this.config = SessionConfig.forDatabase(Objects.requireNonNull(database, "database"));
        this.ownsDriver = ownsDriver;
        try (var session = session()) {
            for (var statement : SCHEMA) {
                session.executeWriteWithoutResult(tx -> tx.run(statement).consume());
            }
        }
    }

    private Session session() {
        return driver.session(config);
    }

    // Progress

    /** The last log position taken in, the frontier of everything taken in, and the last event's id. */
    private record Progress(long position, Frontier at, Optional<EventId> last) {}

    private static Progress progress(TransactionContext tx) {
        var records = tx.run("MATCH (p:Progress {key: 'khora'}) RETURN p.position AS position, p.heads AS heads, "
                + "p.last AS last").list();
        if (records.isEmpty()) {
            return new Progress(0, Frontier.empty(), Optional.empty());
        }
        var r = records.get(0);
        return new Progress(r.get("position").asLong(),
                new Frontier(new TreeSet<>(r.get("heads").asList(v -> new EventId(v.asString())))),
                r.get("last").isNull() ? Optional.empty() : Optional.of(new EventId(r.get("last").asString())));
    }

    private static void setProgress(TransactionContext tx, Progress p) {
        var params = new HashMap<String, Object>();
        params.put("position", p.position());
        params.put("heads", p.at().heads().stream().map(EventId::value).toList());
        params.put("last", p.last().map(EventId::value).orElse(null));
        tx.run("MERGE (p:Progress {key: 'khora'}) SET p.position = $position, p.heads = $heads, p.last = $last",
                params).consume();
    }

    /** The last log position taken in; 0 before any. */
    public long position() {
        try (var session = session()) {
            return session.executeRead(Neo4jDatastore::progress).position();
        }
    }

    /** The frontier of every event taken in: the one frontier the datastore answers at. */
    public Frontier at() {
        try (var session = session()) {
            return session.executeRead(Neo4jDatastore::progress).at();
        }
    }

    // Taking events in

    /**
     * Takes in the events of {@code log} after the last position taken in, a page per transaction,
     * and returns how many it took in.
     */
    public long catchUp(EventLog log) {
        long taken = 0;
        while (true) {
            var page = log.read(position() + 1, PAGE);
            if (page.isEmpty()) {
                return taken;
            }
            taken += take(page, log);
        }
    }

    /**
     * Catches up with {@code log}, then takes in each new event as the log appends it, until the
     * returned {@link Following} is closed.
     */
    public Following follow(EventLog log) {
        catchUp(log);
        return log.follow(position() + 1, logged -> take(List.of(logged), log));
    }

    /**
     * Takes in {@code page} in one transaction and returns how many events it took in. Events at or
     * before the last position taken in are skipped. A retraction's target is looked up in the page,
     * then in {@code source}.
     *
     * @throws IllegalStateException if the page skips a position
     */
    synchronized long take(List<Logged> page, EventSource source) {
        var inPage = new HashMap<EventId, Event>();
        page.forEach(l -> inPage.put(l.event().id(), l.event()));
        EventSource lookup = id -> Optional.ofNullable(inPage.get(id)).or(() -> source.get(id));
        try (var session = session()) {
            return session.executeWrite(tx -> {
                var progress = progress(tx);
                long next = progress.position() + 1;
                var heads = new TreeSet<EventId>(progress.at().heads());
                var last = progress.last();
                var writes = new Writes(tx);
                for (var logged : page) {
                    if (logged.position() < next) {
                        continue;
                    }
                    if (logged.position() > next) {
                        throw new IllegalStateException("expected log position " + next + ", got " + logged.position());
                    }
                    writes.event(logged.event(), lookup);
                    heads.removeAll(logged.event().causes());
                    heads.add(logged.event().id());
                    last = Optional.of(logged.event().id());
                    next++;
                }
                writes.flush();
                setProgress(tx, new Progress(next - 1, new Frontier(heads), last));
                return next - 1 - progress.position();
            });
        }
    }

    /**
     * The writes of a page, in order. Asserts are gathered and written together; a retraction first
     * writes what was gathered, so it deletes what came before it.
     */
    private static final class Writes {
        private final TransactionContext tx;
        private final LinkedHashSet<String> entities = new LinkedHashSet<>();
        /** Per thing and attribute, the literal values to add. */
        private final Map<List<String>, LinkedHashSet<Object>> literals = new LinkedHashMap<>();
        private final List<Map<String, Object>> relations = new ArrayList<>();

        Writes(TransactionContext tx) {
            this.tx = tx;
        }

        void event(Event event, EventSource lookup) {
            if (!(event.body() instanceof AcceptedAction accepted)) {
                return;
            }
            var tuples = Assertion.in(accepted.action()).orElse(List.of());
            for (int i = 0; i < tuples.size(); i++) {
                var tuple = tuples.get(i).in(event.id());
                var id = Identity.of(event.id(), i);
                if (tuple.equals(Assertion.entity())) {
                    entities.add(id.value());
                } else if (tuple.attribute().filter(Basic.RETRACTED::equals).isPresent()) {
                    tuple.identity().ifPresent(target -> retract(target, lookup));
                } else if (tuple.identity().isPresent() && tuple.attribute().isPresent() && tuple.value().isPresent()) {
                    var thing = tuple.identity().get().value();
                    var attribute = tuple.attribute().get().value();
                    switch (tuple.value().get()) {
                        case Ref r -> relations.add(Map.of("thing", thing, "attribute", attribute,
                                "target", r.id().value(), "statement", id.value()));
                        case Literal l -> literals.computeIfAbsent(List.of(thing, attribute),
                                k -> new LinkedHashSet<>()).add(stored(l));
                    }
                }
            }
        }

        /**
         * Deletes what the statement {@code target} asserted. A target that is not a statement of an
         * event in {@code lookup}, or that asserted nothing the datastore keeps, deletes nothing.
         */
        private void retract(Identity target, EventSource lookup) {
            var asserted = target.event().flatMap(lookup::get).flatMap(e -> tupleAt(e, target));
            if (asserted.isEmpty()) {
                return;
            }
            flush();
            var tuple = asserted.get();
            if (tuple.equals(Assertion.entity())) {
                tx.run("MATCH (t:Thing {id: $id}) REMOVE t:Entity", Map.of("id", target.value())).consume();
                return;
            }
            if (tuple.identity().isEmpty() || tuple.attribute().isEmpty() || tuple.value().isEmpty()
                    || tuple.attribute().get().equals(Basic.RETRACTED)) {
                return;
            }
            var thing = tuple.identity().get().value();
            var attribute = tuple.attribute().get().value();
            switch (tuple.value().get()) {
                case Ref r -> tx.run("MATCH (:Thing {id: $thing})-[r:$($attribute) {statement: $statement}]->() DELETE r",
                        Map.of("thing", thing, "attribute", attribute, "statement", target.value())).consume();
                case Literal l -> tx.run("""
                        MATCH (t:Thing {id: $thing})
                        WITH t, t[$attribute] AS old
                        WHERE old IS NOT NULL
                        WITH t, CASE WHEN valueType(old) STARTS WITH 'LIST' THEN [x IN old WHERE x <> $value]
                                     WHEN old = $value THEN [] ELSE [old] END AS kept
                        SET t[$attribute] = CASE size(kept) WHEN 0 THEN null WHEN 1 THEN kept[0] ELSE kept END""",
                        Map.of("thing", thing, "attribute", attribute, "value", stored(l))).consume();
            }
        }

        private static Optional<Assertion> tupleAt(Event event, Identity target) {
            if (!(event.body() instanceof AcceptedAction accepted)) {
                return Optional.empty();
            }
            int index = Integer.parseInt(target.value().substring(target.value().indexOf('/') + 1));
            var tuples = Assertion.in(accepted.action()).orElse(List.of());
            return index < tuples.size() ? Optional.of(tuples.get(index).in(event.id())) : Optional.empty();
        }

        /** Writes the gathered asserts. */
        void flush() {
            if (!entities.isEmpty()) {
                tx.run("UNWIND $ids AS id MERGE (t:Thing {id: id}) SET t:Entity",
                        Map.of("ids", List.copyOf(entities))).consume();
                entities.clear();
            }
            if (!literals.isEmpty()) {
                var rows = new ArrayList<Map<String, Object>>();
                literals.forEach((k, values) -> rows.add(Map.of("thing", k.get(0), "attribute", k.get(1),
                        "values", List.copyOf(values))));
                tx.run("""
                        UNWIND $rows AS r
                        MERGE (t:Thing {id: r.thing})
                        WITH t, r, CASE WHEN t[r.attribute] IS NULL THEN []
                                        WHEN valueType(t[r.attribute]) STARTS WITH 'LIST' THEN t[r.attribute]
                                        ELSE [t[r.attribute]] END AS old
                        WITH t, r, old + [v IN r.values WHERE NOT v IN old] AS merged
                        SET t[r.attribute] = CASE size(merged) WHEN 1 THEN merged[0] ELSE merged END""",
                        Map.of("rows", rows)).consume();
                literals.clear();
            }
            if (!relations.isEmpty()) {
                tx.run("""
                        UNWIND $rows AS r
                        MERGE (t:Thing {id: r.thing})
                        MERGE (o:Thing {id: r.target})
                        CREATE (t)-[:$(r.attribute) {statement: r.statement}]->(o)""",
                        Map.of("rows", List.copyOf(relations))).consume();
                relations.clear();
            }
        }
    }

    /** Deletes everything the datastore holds, the progress first. */
    public synchronized void clear() {
        try (var session = session()) {
            session.executeWriteWithoutResult(tx -> tx.run("MATCH (p:Progress) DELETE p").consume());
            session.run("""
                    MATCH (n:Thing)
                    CALL (n) { DETACH DELETE n } IN TRANSACTIONS OF 10000 ROWS""").consume();
        }
    }

    // Snapshots

    /** What the datastore holds now. */
    public Snapshot snapshot() {
        try (var session = session()) {
            return session.executeRead(tx -> {
                var progress = progress(tx);
                var rows = new ArrayList<String>();
                tx.run("MATCH (t:Thing) RETURN t.id AS id, t:Entity AS entity, properties(t) AS p ORDER BY t.id")
                        .forEachRemaining(r -> {
                            var attributes = new TreeMap<String, Json>();
                            r.get("p").asMap().forEach((k, v) -> {
                                if (!k.equals("id")) {
                                    var values = new ArrayList<Json>();
                                    valuesOf(v).forEach(x -> values.add(Assertion.valueJson(x)));
                                    attributes.put(k, new Json.Arr(values));
                                }
                            });
                            rows.add(Json.Obj.of(Map.of("type", new Json.Str(r.get("entity").asBoolean() ? "entity" : "thing"),
                                    "id", new Json.Str(r.get("id").asString()), "attributes", new Json.Obj(attributes)))
                                    .canonical());
                        });
                tx.run("MATCH (t:Thing)-[r]->(o:Thing) RETURN t.id AS thing, type(r) AS attribute, o.id AS target, "
                        + "r.statement AS statement ORDER BY r.statement").forEachRemaining(r -> rows.add(Json.Obj.of(Map.of(
                                "type", new Json.Str("relation"), "statement", new Json.Str(r.get("statement").asString()),
                                "thing", new Json.Str(r.get("thing").asString()),
                                "attribute", new Json.Str(r.get("attribute").asString()),
                                "target", new Json.Str(r.get("target").asString()))).canonical()));
                return new Snapshot(progress.position(), progress.at(), progress.last(), rows);
            });
        }
    }

    /** Replaces everything the datastore holds with {@code snapshot}. */
    public synchronized void restore(Snapshot snapshot) {
        var things = new ArrayList<Map<String, Object>>();
        var relations = new ArrayList<Map<String, Object>>();
        for (var row : snapshot.parsed()) {
            switch (row.str("type")) {
                case "entity", "thing" -> {
                    var attributes = new HashMap<String, Object>();
                    row.obj("attributes").members().forEach((k, v) -> {
                        var values = ((Json.Arr) v).items().stream()
                                .map(x -> stored((Literal) Assertion.valueOf((Json.Obj) x))).toList();
                        attributes.put(k, values.size() == 1 ? values.get(0) : values);
                    });
                    things.add(Map.of("id", row.str("id"), "entity", row.str("type").equals("entity"),
                            "attributes", attributes));
                }
                case "relation" -> relations.add(Map.of("thing", row.str("thing"), "attribute", row.str("attribute"),
                        "target", row.str("target"), "statement", row.str("statement")));
                default -> throw new IllegalArgumentException("unknown row type " + row.str("type"));
            }
        }
        clear();
        try (var session = session()) {
            session.executeWriteWithoutResult(tx -> {
                tx.run("""
                        UNWIND $rows AS r
                        CREATE (t:Thing {id: r.id})
                        SET t += r.attributes
                        WITH t, r WHERE r.entity
                        SET t:Entity""", Map.of("rows", things)).consume();
                tx.run("""
                        UNWIND $rows AS r
                        MATCH (t:Thing {id: r.thing}), (o:Thing {id: r.target})
                        CREATE (t)-[:$(r.attribute) {statement: r.statement}]->(o)""", Map.of("rows", relations)).consume();
                setProgress(tx, new Progress(snapshot.position(), snapshot.at(), snapshot.last()));
            });
        }
    }

    /**
     * Restores {@code snapshot}, then catches up with {@code log}.
     *
     * @throws IllegalArgumentException if the snapshot is not of {@code log}: the log's event at the
     *     snapshot's position is not the snapshot's last event
     */
    public synchronized void rebuild(Snapshot snapshot, EventLog log) {
        if (snapshot.position() > 0) {
            var at = log.read(snapshot.position(), 1);
            if (at.isEmpty() || !Optional.of(at.get(0).event().id()).equals(snapshot.last())) {
                throw new IllegalArgumentException("the snapshot is not of this log at position " + snapshot.position());
            }
        }
        restore(snapshot);
        catchUp(log);
    }

    /** The number of nodes per label and of relationships, for tests. */
    Map<String, Long> shape() {
        try (var session = session()) {
            return session.executeRead(tx -> {
                var shape = new TreeMap<String, Long>();
                tx.run("MATCH (n) UNWIND labels(n) AS label RETURN label, count(*) AS n").forEachRemaining(
                        r -> shape.put(r.get("label").asString(), r.get("n").asLong()));
                tx.run("MATCH ()-[r]->() RETURN count(r) AS n").forEachRemaining(r -> shape.put("relationships",
                        r.get("n").asLong()));
                return shape;
            });
        }
    }

    // Answering

    /**
     * {@inheritDoc}
     *
     * @throws IllegalArgumentException if the query names a frontier other than everything taken in
     */
    @Override
    public Answer answer(Query query) {
        try (var session = session()) {
            return session.executeRead(tx -> {
                var at = progress(tx).at();
                if (query.at().isPresent() && !query.at().get().heads().equals(at.heads())) {
                    throw new IllegalArgumentException("the graph datastore answers only at everything it has taken in");
                }
                return Evaluator.answer(query, at, new TxGraph(tx));
            });
        }
    }

    /** The three lookups of {@link Graph}, as reads in one transaction. */
    private static final class TxGraph implements Graph {
        private final TransactionContext tx;
        private final Map<List<Identity>, List<Value>> values = new HashMap<>();

        TxGraph(TransactionContext tx) {
            this.tx = tx;
        }

        @Override
        public List<Identity> things() {
            return tx.run("MATCH (t:Entity) RETURN t.id AS id ORDER BY id").list().stream()
                    .map(r -> new Identity(r.get("id").asString())).toList();
        }

        @Override
        public List<Fact> with(Identity attribute) {
            var facts = new ArrayList<Fact>();
            var params = Map.<String, Object>of("attribute", attribute.value());
            tx.run("MATCH (t:Thing) WHERE t[$attribute] IS NOT NULL RETURN t.id AS id, t[$attribute] AS v ORDER BY id",
                    params).forEachRemaining(r -> valuesOf(r.get("v").asObject())
                            .forEach(v -> facts.add(new Fact(new Identity(r.get("id").asString()), v))));
            tx.run("MATCH (t:Thing)-[r:$($attribute)]->(o:Thing) RETURN t.id AS id, o.id AS o ORDER BY r.statement",
                    params).forEachRemaining(r -> facts.add(new Fact(new Identity(r.get("id").asString()),
                            new Ref(new Identity(r.get("o").asString())))));
            return facts;
        }

        @Override
        public List<Value> values(Identity thing, Identity attribute) {
            return values.computeIfAbsent(List.of(thing, attribute), k -> {
                var result = new ArrayList<Value>();
                var params = Map.<String, Object>of("thing", thing.value(), "attribute", attribute.value());
                tx.run("MATCH (t:Thing {id: $thing}) RETURN t[$attribute] AS v", params)
                        .forEachRemaining(r -> result.addAll(valuesOf(r.get("v").asObject())));
                tx.run("MATCH (:Thing {id: $thing})-[r:$($attribute)]->(o:Thing) RETURN o.id AS o ORDER BY r.statement",
                        params).forEachRemaining(r -> result.add(new Ref(new Identity(r.get("o").asString()))));
                return result;
            });
        }
    }

    /** The property a literal is kept as: a 64-bit integer, or a string. */
    static Object stored(Literal l) {
        return switch (l.value()) {
            case Json.Int n -> n.value();
            case Json.Str s -> s.value();
            default -> throw new IllegalStateException("a literal is a string or an integer");
        };
    }

    /** The literals a property holds: none, one, or a list. */
    static List<Value> valuesOf(Object property) {
        return switch (property) {
            case null -> List.of();
            case List<?> list -> list.stream().map(Neo4jDatastore::literal).toList();
            default -> List.of(literal(property));
        };
    }

    private static Value literal(Object o) {
        return switch (o) {
            case Long n -> new Literal(new Json.Int(n));
            case String s -> new Literal(new Json.Str(s));
            default -> throw new IllegalStateException("a property holds integers or strings, not " + o.getClass());
        };
    }

    @Override
    public void close() {
        if (ownsDriver) {
            driver.close();
        }
    }
}
