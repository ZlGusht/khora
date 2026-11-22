package khora.kernel.surface;

/**
 * A surface that reports its changes (ADR-0XY decision 1).
 *
 * <p>Provisional: {@code changes} takes no context, as in the ADR, so a feed has no principal and
 * access to it cannot be checked yet. The feed yields {@link Change}s, not yet
 * events ([[Events are the basic unit of khora]]).
 */
public interface ChangingSurface extends Surface {

    /** The changes after {@code since}, then new ones as they happen. */
    ChangeFeed changes(Frontier since);
}
