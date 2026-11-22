package khora.kernel.surface;

import khora.kernel.event.Frontier;

/**
 * A surface that reports its changes (ADR-0XY decision 1).
 *
 * <p>Out with events: goes. Every surface has an event stream, read from a frontier with a context,
 * so {@code changes} moves to {@link Surface} as the stream
 * ([[Every surface has one event stream, read from a frontier]]).
 */
public interface ChangingSurface extends Surface {

    /** The changes after {@code since}, then new ones as they happen. */
    ChangeFeed changes(Frontier since);
}
