package khora.surfaces.contract;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import khora.events.Event;
import khora.events.EventId;
import khora.events.Identity;

/**
 * The events a scenario run has made, by step name, those it emitted unnamed, and the well-known
 * names it has resolved. Scenarios hold names; ids exist only here.
 */
public final class Bindings {
    private final Map<String, EventId> steps = new HashMap<>();
    private final List<EventId> emitted = new ArrayList<>();
    /** The identity of each {@link Ref.Named} resolved so far, by value, to its name. */
    private final Map<String, String> names = new HashMap<>();

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
     * The identity a ref names. A step ref names an event, not a tuple, and has none. A named ref is
     * remembered, so {@link #render} shows its name.
     *
     * @throws IllegalArgumentException for an unknown step, a step ref, or a received ref with no
     *     received event
     */
    public Identity identity(Ref ref, Optional<Event> received) {
        return switch (ref) {
            case Ref.Step s -> throw new IllegalArgumentException(
                    "step " + s.name() + " is an event; name one of its tuples");
            case Ref.Tuple t -> Identity.of(event(t.step()), t.index());
            case Ref.Named n -> {
                var id = Identity.named(n.name());
                names.put(id.value(), n.name());
                yield id;
            }
            case Ref.Local l -> Identity.local(l.index());
            case Ref.Received r -> Identity.of(received.orElseThrow(() -> new IllegalArgumentException(
                    "no received event for a received ref")).id(), r.index());
        };
    }

    /** Records an event no step made, numbered from 1 in the order called. */
    public void emitted(EventId id) {
        emitted.add(id);
    }

    /**
     * An identity for people: step name, {@code name/n}, the name of a resolved {@link Ref.Named},
     * {@code emitted#k}, or its value; a local identity, {@code /n}, as it is.
     */
    public String render(Identity id) {
        var text = id.value();
        if (id.isLocal()) {
            return text;
        }
        var slash = text.indexOf('/');
        var event = slash > 0 ? text.substring(0, slash) : text;
        var suffix = slash > 0 ? text.substring(slash) : "";
        for (var e : steps.entrySet()) {
            if (e.getValue().value().equals(event)) {
                return e.getKey() + suffix;
            }
        }
        var name = names.get(text);
        if (name != null) {
            return name;
        }
        var k = emitted.indexOf(new EventId(event));
        return k >= 0 ? "emitted#" + (k + 1) + suffix : text;
    }
}
