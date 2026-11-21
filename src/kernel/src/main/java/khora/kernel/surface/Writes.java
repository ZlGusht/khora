package khora.kernel.surface;

/**
 * Which writes a surface takes (ADR-0XY decision 2).
 *
 * <p>Provisional: the values are the ADR's.
 */
public enum Writes {
    NONE,
    UNCONDITIONAL,
    CONDITIONAL,
    MERGEABLE
}
