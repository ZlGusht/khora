package khora.kernel.surface;

/**
 * Whether past data is read with the definitions it had then, or with today's (ADR-0XY decision 5).
 *
 * <p>Provisional: taken from the query's time in the implementation plan's shared contracts.
 */
public enum Meaning {
    THEN,
    NOW
}
