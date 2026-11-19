package khora.kernel.surface;

/**
 * A surface that takes writes (ADR-0XY decisions 1, 7 and 8). Each write is one action.
 *
 * <p>Provisional.
 */
public interface WritableSurface extends Surface {

    /** Writes, if the condition still holds. */
    WriteAnswer write(Operation operation, Condition condition, Context context);
}
