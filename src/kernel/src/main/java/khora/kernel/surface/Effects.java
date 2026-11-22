package khora.kernel.surface;

/**
 * How a surface's effects come about (ADR-0XY decision 2).
 *
 * <p>Provisional: the values are the ADR's. K1 (#18) has no effect events, so what this declares in
 * terms of events is open.
 */
public enum Effects {
    NATIVE,
    OBSERVED,
    NONE
}
