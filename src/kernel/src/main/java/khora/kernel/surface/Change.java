package khora.kernel.surface;

import khora.kernel.event.Frontier;
import java.util.Objects;
import khora.kernel.event.EventId;

/**
 * One change a surface reports.
 *
 * <p>Out with events: goes. A surface's stream carries events, not changes
 * ([[Every surface has one event stream, read from a frontier]]).
 *
 * @param event the event behind the change
 * @param target what changed
 * @param value its new value
 * @param at the surface's frontier after the change
 */
public record Change(EventId event, String target, String value, Frontier at) {
    public Change {
        Objects.requireNonNull(event, "event");
        Objects.requireNonNull(target, "target");
        Objects.requireNonNull(value, "value");
        Objects.requireNonNull(at, "at");
    }
}
