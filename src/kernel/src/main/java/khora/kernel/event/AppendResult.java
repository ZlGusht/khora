package khora.kernel.event;

import java.util.Objects;

/**
 * Whether an event was appended to an {@link EventGraph}.
 *
 * <p>Provisional: refusals are not recorded yet
 * ([[An event is an accepted action, and an assert is the basic action]]).
 */
public sealed interface AppendResult permits AppendResult.Appended, AppendResult.Refused {

    EventId id();

    record Appended(EventId id) implements AppendResult {
        public Appended {
            Objects.requireNonNull(id, "id");
        }
    }

    record Refused(EventId id, Reason reason) implements AppendResult {
        public Refused {
            Objects.requireNonNull(id, "id");
            Objects.requireNonNull(reason, "reason");
        }
    }

    enum Reason {
        /** A cause is not in the graph. */
        MISSING_CAUSE,
        /** The recorded time is not later than every cause's. */
        NOT_AFTER_CAUSES,
        /** The event is already in the graph. */
        DUPLICATE,
        /** A named link points to an event outside the event's causal past. */
        LINK_OUTSIDE_PAST
    }
}
