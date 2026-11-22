package khora.kernel.event;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import khora.kernel.json.Json;

/**
 * An assertion: the tuple (identity, attribute, value), carried by one assert action
 * ([[Assertions are tuples with identity]]). The event that carries it gives it identity, so the
 * event's id identifies what it asserts: the empty tuple {@code (,,)} instantiates an entity, a full
 * tuple is that attribute of that thing.
 *
 * <p>Provisional: any combination of present and absent parts is accepted; which partial tuples
 * mean what is left open.
 *
 * @param identity the thing the assertion is about
 * @param attribute the attribute asserted, itself an identity
 * @param value the attribute's value
 */
public record Assertion(Optional<EventId> identity, Optional<EventId> attribute, Optional<Value> value) {
    private static final Set<String> KEYS = Set.of("identity", "attribute", "value");

    public Assertion {
        Objects.requireNonNull(identity, "identity");
        Objects.requireNonNull(attribute, "attribute");
        Objects.requireNonNull(value, "value");
    }

    /** A value: a literal, or a reference to an identity. */
    public sealed interface Value permits Literal, Ref {}

    /** A literal string or integer. */
    public record Literal(Json value) implements Value {
        public Literal {
            Objects.requireNonNull(value, "value");
            if (!(value instanceof Json.Str || value instanceof Json.Int)) {
                throw new IllegalArgumentException("a literal is a string or an integer");
            }
        }
    }

    /** A reference to an identity. */
    public record Ref(EventId id) implements Value {
        public Ref {
            Objects.requireNonNull(id, "id");
        }
    }

    /** The empty tuple, which instantiates an entity. */
    public static Assertion entity() {
        return new Assertion(Optional.empty(), Optional.empty(), Optional.empty());
    }

    /** The full tuple. */
    public static Assertion of(EventId identity, EventId attribute, Value value) {
        return new Assertion(Optional.of(identity), Optional.of(attribute), Optional.of(value));
    }

    /** Asserts that the assertion {@code assertion} is retracted. */
    public static Assertion retraction(EventId assertion) {
        return of(assertion, Basic.RETRACTED, new Literal(new Json.Str("true")));
    }

    /** The assert action carrying this tuple. */
    public Action toAction() {
        var args = new HashMap<String, Json>();
        identity.ifPresent(i -> args.put("identity", new Json.Str(i.value())));
        attribute.ifPresent(a -> args.put("attribute", new Json.Str(a.value())));
        value.ifPresent(v -> args.put("value", switch (v) {
            case Literal l -> Json.Obj.of(Map.of("literal", l.value()));
            case Ref r -> Json.Obj.of(Map.of("ref", new Json.Str(r.id().value())));
        }));
        return new Action(Basic.ASSERT, Json.Obj.of(args));
    }

    /**
     * The tuple an action carries, or empty if the action is not an assert.
     *
     * @throws IllegalArgumentException if it is an assert whose arguments are not a tuple
     */
    public static Optional<Assertion> from(Action action) {
        if (!action.definition().equals(Basic.ASSERT)) {
            return Optional.empty();
        }
        var args = action.args();
        for (var key : args.members().keySet()) {
            if (!KEYS.contains(key)) {
                throw new IllegalArgumentException("unknown member " + key);
            }
        }
        return Optional.of(new Assertion(id(args, "identity"), id(args, "attribute"),
                args.get("value").isPresent() ? Optional.of(value(args.obj("value"))) : Optional.empty()));
    }

    private static Optional<EventId> id(Json.Obj args, String key) {
        return args.get(key).isPresent() ? Optional.of(new EventId(args.str(key))) : Optional.empty();
    }

    private static Value value(Json.Obj v) {
        var keys = v.members().keySet();
        if (keys.equals(Set.of("literal"))) {
            return new Literal(v.members().get("literal"));
        }
        if (keys.equals(Set.of("ref"))) {
            return new Ref(new EventId(v.str("ref")));
        }
        throw new IllegalArgumentException("a value has exactly one of literal or ref");
    }
}
