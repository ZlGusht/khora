package khora.kernel.surface;

/**
 * How much of its past a surface keeps (ADR-0XY decision 2).
 *
 * <p>Provisional: the values are the ADR's.
 */
public enum History {
    FULL,
    SNAPSHOTS,
    NONE
}
