package khora.surfaces.stub;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import khora.events.Action;
import khora.events.Assertion;
import khora.events.Basic;
import khora.events.By;
import khora.events.Emitted;
import khora.events.Event;
import khora.events.EventId;
import khora.events.Identity;
import khora.surfaces.Gate;

/**
 * A stub gate for project names: a project has one current name. It guards asserts of
 * {@link #NAME}. A rename replaces the name it read by retracting it; it is refused when that name
 * is already replaced, or is not a statement this gate knows. A name asserted without retracting one
 * is blind, and passes.
 *
 * <p>Exploration: the conflict is found from what the write says it replaces, not from what it
 * knew. Without this gate both renames become events, and each surface reads them its own way.
 */
public final class Projects implements Gate {
    /** The attribute this gate guards. */
    public static final Identity NAME = Identity.named("name");

    private final Set<Identity> statements = new HashSet<>();
    private final Map<Identity, EventId> replaced = new HashMap<>();

    @Override
    public String name() {
        return "projects";
    }

    @Override
    public boolean guards(Action action) {
        return tuples(action).stream().anyMatch(t -> t.attribute().equals(Optional.of(NAME)));
    }

    @Override
    public Verdict decide(Action action, By by) {
        for (var tuple : tuples(action)) {
            if (!tuple.attribute().equals(Optional.of(Basic.RETRACTED)) || tuple.identity().isEmpty()) {
                continue;
            }
            var target = tuple.identity().get();
            if (!statements.contains(target)) {
                return new Refuse("replaces " + target.value() + ", which is not a statement projects knows");
            }
            var by_ = replaced.get(target);
            if (by_ != null) {
                return new Refuse("replaces " + target.value() + ", which " + by_.value() + " already replaced");
            }
        }
        return new Pass();
    }

    @Override
    public void take(Event event) {
        if (!(event.body() instanceof Emitted emitted)) {
            return;
        }
        var tuples = Assertion.in(emitted.action()).orElse(List.of());
        for (int i = 0; i < tuples.size(); i++) {
            var tuple = tuples.get(i).in(event.id());
            statements.add(Identity.of(event.id(), i));
            if (tuple.attribute().equals(Optional.of(Basic.RETRACTED)) && tuple.identity().isPresent()) {
                replaced.putIfAbsent(tuple.identity().get(), event.id());
            }
        }
    }

    private static List<Assertion> tuples(Action action) {
        return Assertion.in(action).orElse(List.of());
    }
}
