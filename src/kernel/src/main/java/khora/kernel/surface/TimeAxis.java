package khora.kernel.surface;

/**
 * Which time a read's frontier is measured in.
 *
 * <p>Provisional: taken from the query's time in the implementation plan's shared contracts.
 */
public enum TimeAxis {
    RECORDED,
    OCCURRED
}
