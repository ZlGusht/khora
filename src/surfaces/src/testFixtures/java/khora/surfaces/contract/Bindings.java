package khora.surfaces.contract;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeSet;
import khora.events.Event;
import khora.events.EventId;
import khora.events.Frontier;
import khora.events.Identity;

/**
 * The events a scenario run has made, by step name, and those it emitted unnamed. Scenarios hold
 * names; ids exist only here.
 */
public final class Bindings {
    private final Map<String, EventId> steps = new HashMap<>();
    private final List<EventId> emitted = new java.util.ArrayList<>();

    /** Records the event a step made. */
    public void bind(String step, EventId id) {
        steps.put(step, id);
    }

    /** The event a step made. */
    public EventId event(String step) {
        var id = steps.get(step);
        if (id == null) {
            throw new IllegalArgumentException("unknown step " + step);
        }
        return id;
    }

    /**
     * The identity a ref names. A step ref names an event, not a tuple, and has none.
     *
     * @throws IllegalArgumentException for an unknown step, a step ref, or a received ref with no
     *     received event
     */
    public Identity identity(Ref ref, Optional<Event> received) {
        return switch (ref) {
            case Ref.Step s -> throw new IllegalArgumentException(
                    "step " + s.name() + " is an event; name one of its tuples");
            case Ref.Tuple t -> Identity.of(event(t.step()), t.index());
            case Ref.Named n -> Identity.named(n.name());
            case Ref.Local l -> Identity.local(l.index());
            case Ref.Received r -> Identity.of(received.orElseThrow(() -> new IllegalArgumentException(
                    "no received event for a received ref")).id(), r.index());
        };
    }

    /** The frontier of the given steps' events, as their heads. */
    public Frontier frontier(List<String> stepNames) {
        var heads = new TreeSet<EventId>();
        stepNames.forEach(s -> heads.add(event(s)));
        return new Frontier(heads);
    }

    /** Records an event no step made, numbered from 1 in the order called. */
    public void emitted(EventId id) {
        emitted.add(id);
    }

    /** An identity for people: step name, {@code name/n}, {@code emitted#k}, or its value. */
    public String render(Identity id) {
        var text = id.value();
        var slash = text.indexOf('/');
        var event = slash > 0 ? text.substring(0, slash) : text;
        var suffix = slash > 0 ? text.substring(slash) : "";
        for (var e : steps.entrySet()) {
            if (e.getValue().value().equals(event)) {
                return e.getKey() + suffix;
            }
        }
        var k = emitted.indexOf(new EventId(event));
        return k >= 0 ? "emitted#" + (k + 1) + suffix : text;
    }
}
