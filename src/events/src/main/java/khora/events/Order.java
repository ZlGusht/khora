package khora.events;

/**
 * How two frontiers relate. {@code BEFORE} means the first frontier's events are a strict subset of
 * the second's; {@code CONCURRENT} means neither holds all of the other's.
 *
 * <p>Provisional: within the khora log only.
 */
public enum Order {
    BEFORE,
    AFTER,
    EQUAL,
    CONCURRENT
}
