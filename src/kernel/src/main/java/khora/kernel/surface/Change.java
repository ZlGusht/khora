package khora.kernel.surface;

import java.util.Objects;
import khora.kernel.event.EventId;

/**
 * One change a surface reports.
 *
 * <p>Provisional: a value set on a target. Under [[Events are the basic unit of khora]] surfaces are
 * connected by event streams, so a change becomes an event; this changes after K1 (#18).
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
