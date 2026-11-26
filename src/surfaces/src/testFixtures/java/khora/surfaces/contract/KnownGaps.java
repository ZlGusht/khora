package khora.surfaces.contract;

import static org.junit.jupiter.api.Assertions.fail;

import java.util.Map;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.function.Executable;

/**
 * The contract rules the kernel is known not to keep yet, each with why. A rule listed here that
 * fails (its check throws an {@link AssertionError}) is aborted (skipped) with its reason; one that
 * passes fails, so the entry is removed once the gap is fixed. Anything else the check throws is a
 * real failure, listed or not.
 */
public final class KnownGaps {
    /** Rule name to the reason it does not hold yet. */
    public static final Map<String, String> RULES = Map.of(
            "a retracted attribute with another value does not retract", // tested by KernelFindingsTest
            "#36: Statements treats every tuple with the retracted attribute as a retraction, whatever its"
                    + " value (review of #36: Statements.about)");

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
        } catch (AssertionError e) {
            if (reason != null) {
                Assumptions.abort(reason);
            }
            throw e;
        } catch (RuntimeException | Error e) {
            throw e;
        } catch (Throwable t) {
            throw new AssertionError(t.getMessage(), t);
        }
        if (reason != null) {
            fail("known gap appears fixed: remove it from KnownGaps: " + rule);
        }
    }
}
