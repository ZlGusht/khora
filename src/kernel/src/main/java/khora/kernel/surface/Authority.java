package khora.kernel.surface;

import java.util.Objects;
import khora.kernel.event.Event;
import khora.kernel.event.EventGraph;

/**
 * The surface that decides whether an action on its things is accepted
 * ([[Surfaces receive and emit events by interest]]). It sees the action as the event it would become.
 *
 * <p>Provisional: one authority per kernel; which surface is the authority for which things is not
 * modelled yet.
 */
public interface Authority {

    /** Its name: what an accepted action went through ({@code via}). */
    String name();

    /** Whether {@code candidate} is accepted, given what {@code log} holds. */
    Verdict decide(Event candidate, EventGraph log);

    sealed interface Verdict permits Accept, Reject {}

    record Accept() implements Verdict {}

    record Reject(String reason) implements Verdict {
        public Reject {
            Objects.requireNonNull(reason, "reason");
        }
    }
}
