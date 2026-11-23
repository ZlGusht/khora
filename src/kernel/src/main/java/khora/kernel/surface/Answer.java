package khora.kernel.surface;

import java.util.List;
import java.util.Objects;
import khora.kernel.event.Event;
import khora.kernel.event.Statement;

/**
 * What submitting an action answers. Only an accepted action becomes an event
 * ([[An accepted action is one kind of event]]).
 *
 * <p>Provisional: a rejection is returned, not kept.
 */
public sealed interface Answer permits Answer.Accepted, Answer.Rejected {

    /** Accepted, as {@code event}; a read also answers with the statements that hold. */
    record Accepted(Event event, List<Statement> statements) implements Answer {
        public Accepted {
            Objects.requireNonNull(event, "event");
            statements = List.copyOf(statements);
        }
    }

    /** Rejected, and why. Nothing was appended. */
    record Rejected(String reason) implements Answer {
        public Rejected {
            Objects.requireNonNull(reason, "reason");
        }
    }
}
