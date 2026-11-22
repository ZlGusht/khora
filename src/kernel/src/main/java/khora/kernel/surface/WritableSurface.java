package khora.kernel.surface;

/**
 * A surface that takes writes (ADR-0XY decisions 1, 7 and 8). Each write is one action.
 *
 * <p>Provisional: the write takes an {@link Operation}, not yet a surface's own event
 * ([[Events are the basic unit of khora]]).
 */
public interface WritableSurface extends Surface {

    /** Writes, if the condition still holds. */
    WriteAnswer write(Operation operation, Condition condition, Context context);
}
