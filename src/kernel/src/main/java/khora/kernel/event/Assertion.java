package khora.kernel.event;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import khora.kernel.json.Json;

/**
 * An assertion: the tuple (identity, attribute, value) ([[Assertions are tuples with identity]]).
 * An assert action carries one or more; each is identified by its event's id and its index
 * ([[An event carries several assertions]]). The empty tuple {@code (,,)} instantiates an entity; a
 * full tuple is that attribute of that thing.
 *
 * <p>Provisional: any combination of present and absent parts is accepted; which partial tuples
 * mean what is left open.
 *
 * @param identity the thing the assertion is about
 * @param attribute the attribute asserted, itself an identity
 * @param value the attribute's value
 */
public record Assertion(Optional<Identity> identity, Optional<Identity> attribute, Optional<Value> value) {
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
    public record Ref(Identity id) implements Value {
        public Ref {
            Objects.requireNonNull(id, "id");
        }
    }

    /** The empty tuple, which instantiates an entity. */
    public static Assertion entity() {
        return new Assertion(Optional.empty(), Optional.empty(), Optional.empty());
    }

    /** The full tuple. */
    public static Assertion of(Identity identity, Identity attribute, Value value) {
        return new Assertion(Optional.of(identity), Optional.of(attribute), Optional.of(value));
    }

    /** Asserts that the assertion {@code assertion} is retracted. */
    public static Assertion retraction(Identity assertion) {
        return of(assertion, Basic.RETRACTED, new Literal(new Json.Str("true")));
    }

    /** The identity of the {@code index}-th assertion of the event {@code event}. */
    public static Identity identityIn(EventId event, int index) {
        return Identity.of(event, index);
    }

    /** The assert action carrying {@code tuples}, in order. */
    public static Action action(Assertion... tuples) {
        return action(List.of(tuples));
    }

    /** The assert action carrying {@code tuples}, in order; refuses an empty list. */
    public static Action action(List<Assertion> tuples) {
        if (tuples.isEmpty()) {
            throw new IllegalArgumentException("an assert carries at least one tuple");
        }
        var items = new ArrayList<Json>();
        tuples.forEach(a -> items.add(a.toJson()));
        return new Action(Basic.ASSERT, Json.Obj.of(Map.of("tuples", new Json.Arr(items))));
    }

    /**
     * The tuples an action carries, in order, or empty if the action is not an assert.
     *
     * @throws IllegalArgumentException if it is an assert whose arguments are not one or more tuples
     */
    public static Optional<List<Assertion>> in(Action action) {
        if (!action.definition().equals(Basic.ASSERT)) {
            return Optional.empty();
        }
        return Optional.of(parse(action.args()));
    }

    /** The tuples of an assert's arguments; refuses anything but one or more well-formed tuples. */
    static List<Assertion> parse(Json.Obj args) {
        if (!args.members().keySet().equals(Set.of("tuples"))) {
            throw new IllegalArgumentException("an assert's arguments are its tuples");
        }
        var tuples = new ArrayList<Assertion>();
        for (var item : args.arr("tuples").items()) {
            if (!(item instanceof Json.Obj o)) {
                throw new IllegalArgumentException("a tuple is an object");
            }
            tuples.add(fromJson(o));
        }
        if (tuples.isEmpty()) {
            throw new IllegalArgumentException("an assert carries at least one tuple");
        }
        return List.copyOf(tuples);
    }

    private Json.Obj toJson() {
        var members = new HashMap<String, Json>();
        identity.ifPresent(i -> members.put("identity", new Json.Str(i.value())));
        attribute.ifPresent(a -> members.put("attribute", new Json.Str(a.value())));
        value.ifPresent(v -> members.put("value", switch (v) {
            case Literal l -> Json.Obj.of(Map.of("literal", l.value()));
            case Ref r -> Json.Obj.of(Map.of("ref", new Json.Str(r.id().value())));
        }));
        return Json.Obj.of(members);
    }

    private static Assertion fromJson(Json.Obj o) {
        for (var key : o.members().keySet()) {
            if (!KEYS.contains(key)) {
                throw new IllegalArgumentException("unknown member " + key);
            }
        }
        return new Assertion(id(o, "identity"), id(o, "attribute"),
                o.get("value").isPresent() ? Optional.of(value(o.obj("value"))) : Optional.empty());
    }

    private static Optional<Identity> id(Json.Obj o, String key) {
        return o.get(key).isPresent() ? Optional.of(new Identity(o.str(key))) : Optional.empty();
    }

    private static Value value(Json.Obj v) {
        var keys = v.members().keySet();
        if (keys.equals(Set.of("literal"))) {
            return new Literal(v.members().get("literal"));
        }
        if (keys.equals(Set.of("ref"))) {
            return new Ref(new Identity(v.str("ref")));
        }
        throw new IllegalArgumentException("a value has exactly one of literal or ref");
    }
}
