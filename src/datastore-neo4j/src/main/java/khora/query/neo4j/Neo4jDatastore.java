package khora.query.neo4j;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import khora.events.Assertion.Value;
import khora.events.EventId;
import khora.events.Frontier;
import khora.events.Identity;
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
import org.neo4j.driver.Record;
import org.neo4j.driver.Session;
import org.neo4j.driver.SessionConfig;
import org.neo4j.driver.TransactionContext;

/**
 * The graph datastore, on Neo4j: things and the statements about them, materialized from the
 * events of a log ([[Stage 1's tasks and gate are stated in events]], item 2). It answers queries
 * at any frontier it holds, with the results of {@code EventsDatastore} (ADR-0XY decision 10).
 *
 * <p>The graph: each event taken in is an {@code Event} node, {@code HAS_CAUSE} its causes. Each
 * statement that brings a thing into being or states an attribute is a {@code Statement} node,
 * {@code ABOUT} its {@code Thing}; a reference value also {@code REFERS_TO} its {@code Thing}. A
 * tuple that retracts a statement adds a {@code Retraction} of it. A {@code Progress} node holds the
 * last log position taken in and the frontier of everything taken in. The datastore owns its
 * database: {@link #clear()} deletes these labels.
 *
 * <p>A statement holds at a frontier when its event is in the frontier and no retraction in the
 * frontier names it, as {@code Statements.at} says. A frontier is checked against the datastore's
 * own events; the log is read only to take events in.
 *
 * <p>Provisional: {@link Evaluator} answers over three lookups, each a Cypher read; no operator is
 * pushed down into Cypher yet (P2). A frontier that is not the first events taken in is filtered by
 * its events, passed as a list. One write transaction per page taken in, and one for a restore.
 */
public final class Neo4jDatastore implements Datastore, AutoCloseable {
    static final int PAGE = 1000;
    private static final String ORDER = " ORDER BY s.recorded, s.event, s.index";

    private static final List<String> SCHEMA = List.of(
            "CREATE CONSTRAINT khora_event IF NOT EXISTS FOR (e:Event) REQUIRE e.id IS UNIQUE",
            "CREATE CONSTRAINT khora_statement IF NOT EXISTS FOR (s:Statement) REQUIRE s.id IS UNIQUE",
            "CREATE CONSTRAINT khora_thing IF NOT EXISTS FOR (t:Thing) REQUIRE t.id IS UNIQUE",
            "CREATE CONSTRAINT khora_progress IF NOT EXISTS FOR (p:Progress) REQUIRE p.key IS UNIQUE",
            "CREATE INDEX khora_event_position IF NOT EXISTS FOR (e:Event) ON (e.position)",
            "CREATE INDEX khora_statement_attribute IF NOT EXISTS FOR (s:Statement) ON (s.attribute)",
            "CREATE INDEX khora_statement_creation IF NOT EXISTS FOR (s:Statement) ON (s.creation)",
            "CREATE INDEX khora_retraction_target IF NOT EXISTS FOR (r:Retraction) ON (r.target)");

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

    // Taking events in

    /** The last log position taken in; 0 before any. */
    public long position() {
        try (var session = session()) {
            return session.executeRead(Neo4jDatastore::progress).position();
        }
    }

    /** The frontier of every event taken in. */
    public Frontier at() {
        try (var session = session()) {
            return session.executeRead(Neo4jDatastore::progress).at();
        }
    }

    /**
     * Takes in the events of {@code log} after the last position taken in, a page at a time, and
     * returns how many it took in.
     */
    public long catchUp(EventLog log) {
        long taken = 0;
        while (true) {
            var page = log.read(position() + 1, PAGE);
            if (page.isEmpty()) {
                return taken;
            }
            taken += take(page);
        }
    }

    /**
     * Catches up with {@code log}, then takes in each new event as the log appends it, until the
     * returned {@link Following} is closed.
     */
    public Following follow(EventLog log) {
        catchUp(log);
        return log.follow(position() + 1, logged -> take(List.of(logged)));
    }

    /**
     * Takes in {@code page}, in one transaction, and returns how many events it took in. Events at or
     * before the last position taken in are skipped.
     *
     * @throws IllegalStateException if the page skips a position
     */
    synchronized long take(List<Logged> page) {
        try (var session = session()) {
            return session.executeWrite(tx -> {
                var progress = progress(tx);
                long next = progress.position() + 1;
                var heads = new TreeSet<EventId>(progress.at().heads());
                var rows = new ArrayList<Content.Row>();
                for (var logged : page) {
                    if (logged.position() < next) {
                        continue;
                    }
                    if (logged.position() > next) {
                        throw new IllegalStateException("expected log position " + next + ", got " + logged.position());
                    }
                    heads.removeAll(logged.event().causes());
                    heads.add(logged.event().id());
                    rows.addAll(Content.of(logged));
                    next++;
                }
                write(tx, rows);
                setProgress(tx, next - 1, heads);
                return next - 1 - progress.position();
            });
        }
    }

    private static void write(TransactionContext tx, List<Content.Row> rows) {
        var events = new ArrayList<Map<String, Object>>();
        var statements = new ArrayList<Map<String, Object>>();
        var refs = new ArrayList<Map<String, Object>>();
        var retractions = new ArrayList<Map<String, Object>>();
        for (var row : rows) {
            switch (row) {
                case Content.EventRow e -> events.add(e.params());
                case Content.StatementRow s -> {
                    var p = s.params();
                    statements.add(p);
                    if ("ref".equals(p.get("kind"))) {
                        refs.add(p);
                    }
                }
                case Content.RetractionRow r -> retractions.add(r.params());
            }
        }
        tx.run("""
                UNWIND $rows AS e
                CREATE (:Event {id: e.id, position: e.position, recorded: e.recorded, causes: e.causes})""",
                Map.of("rows", events)).consume();
        tx.run("""
                UNWIND $rows AS e
                MATCH (n:Event {id: e.id})
                UNWIND e.causes AS cause
                MATCH (c:Event {id: cause})
                CREATE (n)-[:HAS_CAUSE]->(c)""", Map.of("rows", events)).consume();
        tx.run("""
                UNWIND $rows AS s
                CREATE (n:Statement {id: s.id, event: s.event, position: s.position, recorded: s.recorded,
                        index: s.index, thing: s.thing, creation: s.creation})
                SET n.attribute = s.attribute, n.kind = s.kind, n.value = s.value
                MERGE (t:Thing {id: s.thing})
                CREATE (n)-[:ABOUT]->(t)""", Map.of("rows", statements)).consume();
        tx.run("""
                UNWIND $rows AS s
                MATCH (n:Statement {id: s.id})
                MERGE (t:Thing {id: s.value})
                CREATE (n)-[:REFERS_TO]->(t)""", Map.of("rows", refs)).consume();
        tx.run("""
                UNWIND $rows AS r
                CREATE (:Retraction {statement: r.statement, target: r.target, event: r.event, position: r.position})""",
                Map.of("rows", retractions)).consume();
    }

    private record Progress(long position, Frontier at) {}

    private static Progress progress(TransactionContext tx) {
        var records = tx.run("MATCH (p:Progress {key: 'khora'}) RETURN p.position AS position, p.heads AS heads").list();
        if (records.isEmpty()) {
            return new Progress(0, Frontier.empty());
        }
        var r = records.get(0);
        return new Progress(r.get("position").asLong(),
                new Frontier(new TreeSet<>(r.get("heads").asList(v -> new EventId(v.asString())))));
    }

    private static void setProgress(TransactionContext tx, long position, Set<EventId> heads) {
        tx.run("MERGE (p:Progress {key: 'khora'}) SET p.position = $position, p.heads = $heads",
                Map.of("position", position, "heads", heads.stream().map(EventId::value).toList())).consume();
    }

    /**
     * Deletes everything the datastore holds. The progress goes first, so a clear cut short leaves a
     * datastore that refuses to take events in (they are already there), rather than one that
     * answers from part of its content.
     */
    public synchronized void clear() {
        try (var session = session()) {
            session.executeWriteWithoutResult(tx -> tx.run("MATCH (p:Progress) DELETE p").consume());
            session.run("""
                    MATCH (n:Event|Statement|Thing|Retraction|Progress)
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
                tx.run("MATCH (e:Event) RETURN e.id AS id, e.position AS position, e.recorded AS recorded, "
                        + "e.causes AS causes ORDER BY e.position").forEachRemaining(
                                r -> rows.add(Content.EventRow.of(r).toJson().canonical()));
                tx.run("MATCH (s:Statement) RETURN s.id AS id, s.event AS event, s.position AS position, "
                        + "s.recorded AS recorded, s.index AS index, s.thing AS thing, s.attribute AS attribute, "
                        + "s.kind AS kind, s.value AS value ORDER BY s.position, s.index").forEachRemaining(
                                r -> rows.add(Content.StatementRow.of(r).toJson().canonical()));
                tx.run("MATCH (r:Retraction) RETURN r.statement AS statement, r.target AS target, r.event AS event, "
                        + "r.position AS position ORDER BY r.position, r.statement").forEachRemaining(
                                r -> rows.add(Content.RetractionRow.of(r).toJson().canonical()));
                return new Snapshot(progress.position(), progress.at(), rows);
            });
        }
    }

    /** Replaces everything the datastore holds with {@code snapshot}. */
    public synchronized void restore(Snapshot snapshot) {
        var rows = snapshot.content();
        clear();
        try (var session = session()) {
            session.executeWriteWithoutResult(tx -> {
                write(tx, rows);
                setProgress(tx, snapshot.position(), snapshot.at().heads());
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
            var last = log.read(snapshot.position(), 1);
            var expected = snapshot.content().stream().filter(r -> r instanceof Content.EventRow e
                    && e.position() == snapshot.position()).map(r -> ((Content.EventRow) r).id()).findFirst();
            if (last.isEmpty() || expected.isEmpty() || !last.get(0).event().id().equals(expected.get())) {
                throw new IllegalArgumentException("the snapshot is not of this log at position " + snapshot.position());
            }
        }
        restore(snapshot);
        catchUp(log);
    }

    /** The number of nodes per label and of relationships per type, for tests. */
    Map<String, Long> shape() {
        try (var session = session()) {
            return session.executeRead(tx -> {
                var shape = new TreeMap<String, Long>();
                tx.run("MATCH (n) UNWIND labels(n) AS label RETURN label, count(*) AS n").forEachRemaining(
                        r -> shape.put(r.get("label").asString(), r.get("n").asLong()));
                tx.run("MATCH ()-[r]->() RETURN type(r) AS type, count(*) AS n").forEachRemaining(
                        r -> shape.put("-" + r.get("type").asString() + "->", r.get("n").asLong()));
                return shape;
            });
        }
    }

    // Answering

    @Override
    public Answer answer(Query query) {
        try (var session = session()) {
            return session.executeRead(tx -> {
                var view = view(tx, query.at());
                return Evaluator.answer(query, query.at().orElse(view.at()), new TxGraph(tx, view));
            });
        }
    }

    /**
     * Which statements a read sees: those of the first {@code upTo} events taken in, or, when
     * {@code events} is present, those of these events.
     */
    private record View(Frontier at, long upTo, Optional<List<String>> events) {
        String holds() {
            return events.isEmpty()
                    ? "s.position <= $upTo AND NOT EXISTS { MATCH (r:Retraction {target: s.id}) WHERE r.position <= $upTo }"
                    : "s.event IN $events AND NOT EXISTS { MATCH (r:Retraction {target: s.id}) WHERE r.event IN $events }";
        }

        Map<String, Object> params(Map<String, Object> more) {
            var p = new HashMap<String, Object>(more);
            p.put("upTo", upTo);
            events.ifPresent(e -> p.put("events", e));
            return p;
        }
    }

    /**
     * The view at {@code at}, or at everything taken in.
     *
     * @throws IllegalArgumentException if the frontier holds an event the datastore lacks
     */
    private static View view(TransactionContext tx, Optional<Frontier> at) {
        var progress = progress(tx);
        if (at.isEmpty() || at.get().heads().equals(progress.at().heads())) {
            return new View(progress.at(), progress.position(), Optional.empty());
        }
        var heads = at.get().heads();
        if (heads.isEmpty()) {
            return new View(at.get(), 0, Optional.empty());
        }
        long top = 0;
        for (var r : tx.run("UNWIND $ids AS id OPTIONAL MATCH (e:Event {id: id}) RETURN id, e.position AS position",
                Map.of("ids", heads.stream().map(EventId::value).toList())).list()) {
            if (r.get("position").isNull()) {
                throw new IllegalArgumentException("unknown event " + r.get("id").asString());
            }
            top = Math.max(top, r.get("position").asLong());
        }
        var needed = new HashSet<String>();
        heads.forEach(h -> needed.add(h.value()));
        var closure = new ArrayList<String>();
        for (long high = top; high >= 1 && !needed.isEmpty(); high -= PAGE) {
            for (var r : tx.run("MATCH (e:Event) WHERE e.position <= $high AND e.position > $low "
                    + "RETURN e.id AS id, e.causes AS causes ORDER BY e.position DESC",
                    Map.of("high", high, "low", high - PAGE)).list()) {
                if (needed.remove(r.get("id").asString())) {
                    closure.add(r.get("id").asString());
                    needed.addAll(r.get("causes").asList(org.neo4j.driver.Value::asString));
                }
            }
        }
        if (!needed.isEmpty()) {
            throw new IllegalArgumentException("unknown event " + needed.iterator().next());
        }
        return closure.size() == top
                ? new View(at.get(), top, Optional.empty())
                : new View(at.get(), top, Optional.of(closure));
    }

    /** The three lookups of {@link Graph}, as reads in one transaction. */
    private static final class TxGraph implements Graph {
        private final TransactionContext tx;
        private final View view;
        private final Map<List<Identity>, List<Value>> values = new HashMap<>();

        TxGraph(TransactionContext tx, View view) {
            this.tx = tx;
            this.view = view;
        }

        private List<Record> read(String match, String returns, Map<String, Object> params) {
            return tx.run(match + " WHERE " + view.holds() + " RETURN " + returns + ORDER, view.params(params)).list();
        }

        @Override
        public List<Identity> things() {
            return read("MATCH (s:Statement {creation: true})", "s.id AS id", Map.of()).stream()
                    .map(r -> new Identity(r.get("id").asString())).toList();
        }

        @Override
        public List<Fact> with(Identity attribute) {
            return read("MATCH (s:Statement {attribute: $attribute})", "s.thing AS thing, s.kind AS kind, s.value AS value",
                    Map.of("attribute", attribute.value())).stream()
                    .map(r -> new Fact(new Identity(r.get("thing").asString()), Content.value(r))).toList();
        }

        @Override
        public List<Value> values(Identity thing, Identity attribute) {
            return values.computeIfAbsent(List.of(thing, attribute), k -> read(
                    "MATCH (:Thing {id: $thing})<-[:ABOUT]-(s:Statement {attribute: $attribute})",
                    "s.kind AS kind, s.value AS value", Map.of("thing", thing.value(), "attribute", attribute.value()))
                    .stream().map(Content::value).toList());
        }
    }

    @Override
    public void close() {
        if (ownsDriver) {
            driver.close();
        }
    }
}
