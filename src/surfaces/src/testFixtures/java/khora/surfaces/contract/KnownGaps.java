package khora.surfaces.contract;

import static org.junit.jupiter.api.Assertions.fail;

import java.util.Map;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.function.Executable;

/**
 * The contract rules the kernel is known not to keep yet, each with why. A rule listed here that
 * fails is aborted (skipped) with its reason; one that passes fails, so the entry is removed once
 * the gap is fixed.
 */
public final class KnownGaps {
    /** Rule name to the reason it does not hold yet. */
    public static final Map<String, String> RULES = Map.of();

    private KnownGaps() {}

    /** Runs {@code check} for {@code rule}, judged against {@link #RULES}. */
    public static void judge(String rule, Executable check) {
        judge(RULES, rule, check);
    }

    /** Runs {@code check} for {@code rule}, judged against {@code gaps}. */
    static void judge(Map<String, String> gaps, String rule, Executable check) {
        var reason = gaps.get(rule);
        try {
            check.execute();
        } catch (Throwable t) {
            if (reason != null) {
                Assumptions.abort(reason);
            }
            switch (t) {
                case RuntimeException e -> throw e;
                case Error e -> throw e;
                default -> throw new AssertionError(t.getMessage(), t);
            }
        }
        if (reason != null) {
            fail("known gap appears fixed: remove it from KnownGaps: " + rule);
        }
    }
}
