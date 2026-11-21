package khora.kernel.event;

import java.util.Map;
import khora.kernel.json.Json;

/**
 * The basic vocabulary: its one action, {@code assert}, and the reserved {@code retracted} attribute.
 * An assert's id is the identity of what it asserts ([[An assert's id is the identity of what it asserts]]).
 *
 * <p>Provisional: the argument shapes of asserts are settled during execution.
 */
public final class Basic {
    /** The definition of {@code assert}. */
    public static final EventId ASSERT = EventId.hashOf("khora/basic/assert");

    /** The attribute that marks an assert as retracted. */
    public static final EventId RETRACTED = EventId.hashOf("khora/basic/retracted");

    private Basic() {}

    /** Asserts an entity. */
    public static Action entity() {
        return assertion(Map.of());
    }

    /** Asserts a property of {@code thing}. */
    public static Action property(EventId thing, EventId attribute, Json value) {
        return assertion(Map.of("thing", id(thing), "attribute", id(attribute), "value", value));
    }

    /** Asserts a relationship from {@code from} to {@code to}. */
    public static Action relationship(EventId from, EventId to, EventId relator) {
        return assertion(Map.of("from", id(from), "to", id(to), "relator", id(relator)));
    }

    /** Asserts that {@code behavior} is bound to {@code thing}. */
    public static Action binding(EventId behavior, EventId thing) {
        return assertion(Map.of("behavior", id(behavior), "thing", id(thing)));
    }

    /** Asserts that {@code assertion} is retracted. */
    public static Action retraction(EventId assertion) {
        return property(assertion, RETRACTED, new Json.Str("true"));
    }

    private static Action assertion(Map<String, Json> args) {
        return new Action(ASSERT, Json.Obj.of(args));
    }

    private static Json id(EventId id) {
        return new Json.Str(id.value());
    }
}
