package khora.kernel.surface;

import java.util.Objects;
import khora.kernel.event.EventId;

/**
 * What a write call answers (ADR-0XY decisions 1 and 8). Confirmation arrives later, through changes.
 *
 * <p>Provisional.
 */
public sealed interface WriteAnswer {

    /** The action this write was. */
    EventId action();

    /** Applied locally; not yet committed. */
    record Unconfirmed(EventId action) implements WriteAnswer {
        public Unconfirmed {
            Objects.requireNonNull(action, "action");
        }
    }

    /** Refused, and why. Nothing was changed. */
    record Rejected(EventId action, String reason) implements WriteAnswer {
        public Rejected {
            Objects.requireNonNull(action, "action");
            Objects.requireNonNull(reason, "reason");
        }
    }
}
