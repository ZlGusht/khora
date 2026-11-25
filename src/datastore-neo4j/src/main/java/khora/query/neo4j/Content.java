package khora.query.neo4j;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import khora.events.AcceptedAction;
import khora.events.Assertion;
import khora.events.Assertion.Literal;
import khora.events.Assertion.Ref;
import khora.events.Assertion.Value;
import khora.events.Basic;
import khora.events.EventId;
import khora.events.Identity;
import khora.events.json.Json;
import khora.events.log.Logged;
import org.neo4j.driver.Record;

/**
 * What {@link Neo4jDatastore} keeps, as rows: one per event, one per statement, one per retraction.
 * The same rows are written when events are taken in, read back for a snapshot, and written again
 * when a snapshot is restored. Things and the relationships between nodes are worked out from them.
 */
final class Content {
    private Content() {}

    /** A row of the datastore's content. */
    sealed interface Row permits EventRow, StatementRow, RetractionRow {
        /** The row as canonical JSON, as a snapshot keeps it. */
        Json.Obj toJson();
    }

    /** An event: its id, its position in the log, its recorded time and its causes. */
    record EventRow(EventId id, long position, long recorded, List<EventId> causes) implements Row {
        EventRow {
            Objects.requireNonNull(id, "id");
            causes = List.copyOf(causes);
        }

        Map<String, Object> params() {
            return Map.of("id", id.value(), "position", position, "recorded", recorded,
                    "causes", causes.stream().map(EventId::value).toList());
        }

        static EventRow of(Record r) {
            return new EventRow(new EventId(r.get("id").asString()), r.get("position").asLong(),
                    r.get("recorded").asLong(), r.get("causes").asList(v -> new EventId(v.asString())));
        }

        @Override
        public Json.Obj toJson() {
            return Json.Obj.of(Map.of("type", new Json.Str("event"), "id", new Json.Str(id.value()),
                    "position", new Json.Int(position), "recorded", new Json.Int(recorded),
                    "causes", new Json.Arr(causes.stream().<Json>map(c -> new Json.Str(c.value())).toList())));
        }
    }

    /**
     * A statement that brings a thing into being (no attribute and no value; its thing is itself) or
     * states an attribute of a thing.
     */
    record StatementRow(Identity id, EventId event, long position, long recorded, int index, Identity thing,
            Optional<Identity> attribute, Optional<Value> value) implements Row {
        StatementRow {
            Objects.requireNonNull(id, "id");
            Objects.requireNonNull(event, "event");
            Objects.requireNonNull(thing, "thing");
            if (attribute.isPresent() != value.isPresent()) {
                throw new IllegalArgumentException("a statement has both an attribute and a value, or neither");
            }
        }

        boolean creation() {
            return attribute.isEmpty();
        }

        Map<String, Object> params() {
            var p = new HashMap<String, Object>();
            p.put("id", id.value());
            p.put("event", event.value());
            p.put("position", position);
            p.put("recorded", recorded);
            p.put("index", index);
            p.put("thing", thing.value());
            p.put("creation", creation());
            p.put("attribute", attribute.map(Identity::value).orElse(null));
            p.put("kind", value.map(Content::kind).orElse(null));
            p.put("value", value.map(Content::stored).orElse(null));
            return p;
        }

        static StatementRow of(Record r) {
            Optional<Identity> attribute = r.get("attribute").isNull() ? Optional.empty()
                    : Optional.of(new Identity(r.get("attribute").asString()));
            Optional<Value> value = r.get("kind").isNull() ? Optional.empty() : Optional.of(Content.value(r));
            return new StatementRow(new Identity(r.get("id").asString()), new EventId(r.get("event").asString()),
                    r.get("position").asLong(), r.get("recorded").asLong(), r.get("index").asInt(),
                    new Identity(r.get("thing").asString()), attribute, value);
        }

        @Override
        public Json.Obj toJson() {
            var m = new HashMap<String, Json>();
            m.put("type", new Json.Str("statement"));
            m.put("id", new Json.Str(id.value()));
            m.put("event", new Json.Str(event.value()));
            m.put("position", new Json.Int(position));
            m.put("recorded", new Json.Int(recorded));
            m.put("index", new Json.Int(index));
            m.put("thing", new Json.Str(thing.value()));
            attribute.ifPresent(a -> m.put("attribute", new Json.Str(a.value())));
            value.ifPresent(v -> m.put("value", Assertion.valueJson(v)));
            return Json.Obj.of(m);
        }
    }

    /** A statement that retracts the statement {@code target}. */
    record RetractionRow(Identity statement, Identity target, EventId event, long position) implements Row {
        RetractionRow {
            Objects.requireNonNull(statement, "statement");
            Objects.requireNonNull(target, "target");
            Objects.requireNonNull(event, "event");
        }

        Map<String, Object> params() {
            return Map.of("statement", statement.value(), "target", target.value(), "event", event.value(),
                    "position", position);
        }

        static RetractionRow of(Record r) {
            return new RetractionRow(new Identity(r.get("statement").asString()),
                    new Identity(r.get("target").asString()), new EventId(r.get("event").asString()),
                    r.get("position").asLong());
        }

        @Override
        public Json.Obj toJson() {
            return Json.Obj.of(Map.of("type", new Json.Str("retraction"), "statement", new Json.Str(statement.value()),
                    "target", new Json.Str(target.value()), "event", new Json.Str(event.value()),
                    "position", new Json.Int(position)));
        }
    }

    /**
     * The rows of one logged event, read as {@code Statements.at} reads it: every tuple of an accepted
     * assert is a statement; the empty tuple and full tuples are kept as statements, and a tuple
     * whose attribute is {@code khora/basic/retracted} retracts the statement it is about.
     */
    static List<Row> of(Logged logged) {
        var event = logged.event();
        var rows = new ArrayList<Row>();
        rows.add(new EventRow(event.id(), logged.position(), event.recorded(), List.copyOf(event.causes())));
        if (!(event.body() instanceof AcceptedAction accepted)) {
            return rows;
        }
        var tuples = Assertion.in(accepted.action()).orElse(List.of());
        for (int i = 0; i < tuples.size(); i++) {
            var tuple = tuples.get(i).in(event.id());
            var id = Identity.of(event.id(), i);
            if (tuple.equals(Assertion.entity())) {
                rows.add(new StatementRow(id, event.id(), logged.position(), event.recorded(), i, id,
                        Optional.empty(), Optional.empty()));
            } else if (tuple.identity().isPresent() && tuple.attribute().isPresent() && tuple.value().isPresent()) {
                rows.add(new StatementRow(id, event.id(), logged.position(), event.recorded(), i,
                        tuple.identity().get(), tuple.attribute(), tuple.value()));
            }
            if (tuple.attribute().filter(Basic.RETRACTED::equals).isPresent() && tuple.identity().isPresent()) {
                rows.add(new RetractionRow(id, tuple.identity().get(), event.id(), logged.position()));
            }
        }
        return rows;
    }

    /**
     * The row {@code o} writes, as {@link Row#toJson()} writes it.
     *
     * @throws IllegalArgumentException if {@code o} is not a row
     */
    static Row fromJson(Json.Obj o) {
        return switch (o.str("type")) {
            case "event" -> {
                members(o, "type", "id", "position", "recorded", "causes");
                var causes = new ArrayList<EventId>();
                for (var c : o.arr("causes").items()) {
                    if (!(c instanceof Json.Str s)) {
                        throw new IllegalArgumentException("a cause is an event id");
                    }
                    causes.add(new EventId(s.value()));
                }
                yield new EventRow(new EventId(o.str("id")), o.integer("position"), o.integer("recorded"), causes);
            }
            case "statement" -> {
                boolean full = o.get("attribute").isPresent();
                if (full) {
                    members(o, "type", "id", "event", "position", "recorded", "index", "thing", "attribute", "value");
                } else {
                    members(o, "type", "id", "event", "position", "recorded", "index", "thing");
                }
                yield new StatementRow(new Identity(o.str("id")), new EventId(o.str("event")), o.integer("position"),
                        o.integer("recorded"), Math.toIntExact(o.integer("index")), new Identity(o.str("thing")),
                        full ? Optional.of(new Identity(o.str("attribute"))) : Optional.empty(),
                        full ? Optional.of(Assertion.valueOf(o.obj("value"))) : Optional.empty());
            }
            case "retraction" -> {
                members(o, "type", "statement", "target", "event", "position");
                yield new RetractionRow(new Identity(o.str("statement")), new Identity(o.str("target")),
                        new EventId(o.str("event")), o.integer("position"));
            }
            default -> throw new IllegalArgumentException("unknown row type " + o.str("type"));
        };
    }

    private static void members(Json.Obj o, String... keys) {
        if (!o.members().keySet().equals(java.util.Set.of(keys))) {
            throw new IllegalArgumentException("a " + o.str("type") + " row has the members " + List.of(keys)
                    + ", not " + o.members().keySet());
        }
    }

    /** How a value is kept: {@code int}, {@code str} or {@code ref}. */
    static String kind(Value v) {
        return switch (v) {
            case Literal(Json.Int n) -> "int";
            case Literal(Json.Str s) -> "str";
            case Literal l -> throw new IllegalStateException("a literal is a string or an integer");
            case Ref r -> "ref";
        };
    }

    /** The property a value is kept as: a 64-bit integer, or a string. */
    static Object stored(Value v) {
        return switch (v) {
            case Literal(Json.Int n) -> n.value();
            case Literal(Json.Str s) -> s.value();
            case Literal l -> throw new IllegalStateException("a literal is a string or an integer");
            case Ref r -> r.id().value();
        };
    }

    /** The value of a record's {@code kind} and {@code value}. */
    static Value value(Record r) {
        var v = r.get("value");
        return switch (r.get("kind").asString()) {
            case "int" -> new Literal(new Json.Int(v.asLong()));
            case "str" -> new Literal(new Json.Str(v.asString()));
            case "ref" -> new Ref(new Identity(v.asString()));
            default -> throw new IllegalStateException("unknown kind of value " + r.get("kind").asString());
        };
    }
}
