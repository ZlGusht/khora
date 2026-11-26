package khora.surfaces.contract;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import khora.events.Action;
import khora.events.Event;
import khora.events.EventId;
import khora.events.Statement;
import khora.surfaces.Answer;
import khora.surfaces.Authority;

/**
 * What happened when a scenario ran.
 *
 * @param bindings the step names and emitted events of the run
 * @param answer the answer to a {@link When.Submit}; empty for any other {@code When}
 * @param statements what a {@link When.Read} found, or what a submitted read answered with
 * @param decisions every decision the authority made, in order
 * @param deliveries every event the surface under test received, in order
 * @param events every event of the log when the run ended, in log order
 */
public record Run(Bindings bindings, Optional<Answer> answer, List<Statement> statements, List<Decision> decisions,
        List<Delivery> deliveries, List<Event> events) {
    public Run {
        Objects.requireNonNull(bindings, "bindings");
        Objects.requireNonNull(answer, "answer");
        statements = List.copyOf(statements);
        decisions = List.copyOf(decisions);
        deliveries = List.copyOf(deliveries);
        events = List.copyOf(events);
    }

    /** One decision of the authority: its verdict, and whether it tried to change the log while deciding. */
    public record Decision(Event candidate, Authority.Verdict verdict, boolean appendedDuringDecide) {
        public Decision {
            Objects.requireNonNull(candidate, "candidate");
            Objects.requireNonNull(verdict, "verdict");
        }
    }

    /** One event the surface received, what it returned, and which events the log held before. */
    public record Delivery(Event event, List<Action> returned, Set<EventId> logBefore) {
        public Delivery {
            Objects.requireNonNull(event, "event");
            returned = List.copyOf(returned);
            logBefore = Set.copyOf(logBefore);
        }
    }
}
