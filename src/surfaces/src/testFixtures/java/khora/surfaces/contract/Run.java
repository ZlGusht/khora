package khora.surfaces.contract;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import khora.events.Action;
import khora.events.By;
import khora.events.Event;
import khora.events.EventId;
import khora.events.Statement;
import khora.surfaces.Answer;
import khora.surfaces.Gate;

/**
 * What happened when a scenario ran.
 *
 * @param bindings the step names and emitted events of the run
 * @param answer the answer to a {@link When.Submit}; empty for any other {@code When}
 * @param statements what the reference reader answered a read with
 * @param decisions every decision the gates made, in order
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

    /** One decision of a gate: the action, who it was by, and the verdict. */
    public record Decision(Action action, By by, Gate.Verdict verdict) {
        public Decision {
            Objects.requireNonNull(action, "action");
            Objects.requireNonNull(by, "by");
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
