package khora.kernel.surface;

/**
 * What a call may spend.
 *
 * <p>Provisional: a plain count of units until budgets are defined.
 */
public record Budget(long units) {
    public Budget {
        if (units < 0) {
            throw new IllegalArgumentException("units is negative: " + units);
        }
    }
}
