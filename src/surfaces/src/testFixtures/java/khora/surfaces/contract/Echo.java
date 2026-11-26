package khora.surfaces.contract;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import khora.events.Action;
import khora.events.Event;
import khora.events.EventSource;
import khora.surfaces.Interest;
import khora.surfaces.Surface;

/**
 * The reference surface: its interests and its responses are data. For each event it receives, each
 * rule whose {@code when} matches the event emits its actions, rule by rule, in order. In those
 * actions, {@link Ref.Received} names a tuple of the received event; Echo knows no steps, so
 * {@link Ref.Step} and {@link Ref.Tuple} are refused when it is made.
 *
 * <p>Echo keeps nothing between events. A rule's {@code when} is matched against the received event
 * alone, so a statement about a statement of an earlier event (such as a retraction) is about only
 * what the event itself names.
 */
public record Echo(String name, Set<Interest> interests, List<Echo.Rule> rules) implements Surface {
    public Echo {
        Check.name(name);
        interests = Set.copyOf(interests);
        rules = List.copyOf(rules);
        for (var rule : rules) {
            rule.emit().forEach(action -> refuseSteps(name, action));
        }
    }

    /** For each received event {@code when} matches, emit {@code emit}. */
    public record Rule(Interest when, List<ActionSpec> emit) {
        public Rule {
            Objects.requireNonNull(when, "when");
            emit = List.copyOf(emit);
        }
    }

    @Override
    public List<Action> receive(Event event) {
        EventSource only = id -> id.equals(event.id()) ? Optional.of(event) : Optional.empty();
        var bindings = new Bindings();
        return rules.stream()
                .filter(rule -> rule.when().matches(event, only))
                .flatMap(rule -> rule.emit().stream())
                .map(action -> action.resolve(bindings, Optional.of(event)))
                .toList();
    }

    private static void refuseSteps(String name, ActionSpec action) {
        switch (action) {
            case ActionSpec.Assert a -> a.tuples().forEach(t -> {
                t.identity().ifPresent(r -> refuseStep(name, r));
                t.attribute().ifPresent(r -> refuseStep(name, r));
                t.value().ifPresent(v -> {
                    if (v instanceof ValueSpec.RefValue ref) {
                        refuseStep(name, ref.ref());
                    }
                });
            });
            case ActionSpec.Read r -> refuseStep(name, r.thing());
            case ActionSpec.Raw ignored -> { }
        }
    }

    private static void refuseStep(String name, Ref ref) {
        var step = switch (ref) {
            case Ref.Step s -> s.name();
            case Ref.Tuple t -> t.step();
            default -> null;
        };
        if (step != null) {
            throw new IllegalArgumentException("Echo " + name + " knows no steps, but names step " + step);
        }
    }
}
