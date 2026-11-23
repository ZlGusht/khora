package khora.events;

import java.util.Map;
import java.util.Optional;
import java.util.Set;
import khora.events.json.Json;

/**
 * The basic vocabulary: its actions, {@code assert} and {@code read}, and the well-known attributes
 * the kernel reads. An assert carries one or more {@link Assertion}s. A read is an event like any
 * other ([[Reads are events]]).
 *
 * <p>A read names the one thing it reads, {@code {"of": identity}}; the frontier it reads at is its
 * {@code at} link.
 *
 * <p>Provisional: more well-known attributes come when the kernel needs them.
 */
public final class Basic {
    /** The definition of {@code assert}. */
    public static final Identity ASSERT = Identity.named("khora/basic/assert");

    /** The definition of {@code read}. */
    public static final Identity READ = Identity.named("khora/basic/read");

    /** The attribute that marks an assertion as retracted. */
    public static final Identity RETRACTED = Identity.named("khora/basic/retracted");

    /** The attribute that gives a thing a name for people. */
    public static final Identity LABEL = Identity.named("khora/basic/label");

    private Basic() {}

    /** A read of {@code thing}. */
    public static Action read(Identity thing) {
        return new Action(READ, Json.Obj.of(Map.of("of", new Json.Str(thing.value()))));
    }

    /**
     * The thing an action reads, or empty if it is not a read.
     *
     * @throws IllegalArgumentException if it is a read whose arguments do not name one thing
     */
    public static Optional<Identity> readOf(Action action) {
        return action.definition().equals(READ) ? Optional.of(readArgs(action.args())) : Optional.empty();
    }

    static Identity readArgs(Json.Obj args) {
        if (!args.members().keySet().equals(Set.of("of"))) {
            throw new IllegalArgumentException("a read names one thing: {\"of\": identity}");
        }
        var thing = new Identity(args.str("of"));
        if (thing.event().isEmpty() && thing.value().startsWith("/")) {
            throw new IllegalArgumentException("a read names a thing, not a tuple of its own event: " + thing.value());
        }
        return thing;
    }
}
