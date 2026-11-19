package khora.kernel.surface;

/**
 * Whether a surface's reads can be repeated (ADR-0XY decision 2).
 *
 * <p>Provisional: the values are the ADR's.
 */
public enum Reads {
    DETERMINISTIC,
    RECORDED,
    NONDETERMINISTIC
}
