package khora.events;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import khora.events.json.Json;

/**
 * The basic vocabulary: its actions, {@code assert}, {@code read} and {@code answer}, and the
 * well-known attributes the kernel reads. An assert carries one or more {@link Assertion}s. A read is
 * an event like any other ([[Reads are events]]).
 *
 * <p>A read names the one thing it reads, {@code {"of": identity}}. A surface that holds that
 * thing's shape answers with an {@code answer} triggered by the read: the statements it holds about
 * the thing, by their identities, {@code {"of": identity, "statements": [identity…]}}. Two surfaces
 * may answer the same read differently.
 *
 * <p>Provisional: more well-known attributes come when the kernel needs them.
 */
public final class Basic {
    /** The definition of {@code assert}. */
    public static final Identity ASSERT = Identity.named("khora/basic/assert");

    /** The definition of {@code read}. */
    public static final Identity READ = Identity.named("khora/basic/read");

    /** The definition of {@code answer}. */
    public static final Identity ANSWER = Identity.named("khora/basic/answer");

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

    /** An answer about {@code thing}: the statements that stand for the answering surface. */
    public static Action answer(Identity thing, List<Identity> statements) {
        var items = new ArrayList<Json>();
        statements.forEach(id -> items.add(new Json.Str(id.value())));
        return new Action(ANSWER, Json.Obj.of(Map.of("of", new Json.Str(thing.value()), "statements", new Json.Arr(items))));
    }

    /**
     * The statements an action answers with, or empty if it is not an answer.
     *
     * @throws IllegalArgumentException if it is an answer whose arguments are malformed
     */
    public static Optional<List<Identity>> answerOf(Action action) {
        if (!action.definition().equals(ANSWER)) {
            return Optional.empty();
        }
        var args = action.args();
        if (!args.members().keySet().equals(Set.of("of", "statements"))) {
            throw new IllegalArgumentException("an answer names what it is of and its statements");
        }
        var ids = new ArrayList<Identity>();
        for (var item : args.arr("statements").items()) {
            if (!(item instanceof Json.Str s)) {
                throw new IllegalArgumentException("a statement is named by its identity");
            }
            ids.add(new Identity(s.value()));
        }
        return Optional.of(List.copyOf(ids));
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
