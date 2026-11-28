package khora.surfaces;

import java.util.List;
import java.util.Objects;
import khora.events.Event;

/**
 * What submitting an action answers: logged as an event, or refused by a gate.
 *
 * <p>Exploration: a read is logged like any action; its answers are the events surfaces emitted in
 * response to it, each naming it as its trigger.
 */
public sealed interface Answer permits Answer.Logged, Answer.Refused {

    /** Logged as {@code event}; {@code responses} are what surfaces emitted in response to it. */
    record Logged(Event event, List<Event> responses) implements Answer {
        public Logged {
            Objects.requireNonNull(event, "event");
            responses = List.copyOf(responses);
        }
    }

    /** Refused, and why. Nothing was appended. */
    record Refused(String reason) implements Answer {
        public Refused {
            Objects.requireNonNull(reason, "reason");
        }
    }
}
