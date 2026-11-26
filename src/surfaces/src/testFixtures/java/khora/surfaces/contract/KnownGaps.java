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
            SurfaceContract.ORDERED,
            "#36: a surface never receives its own emissions, so its frontier does not include them"
                    + " (review of #36: Kernel.offer emits at the frontier the surface has taken)",
            "a surface whose interests throw does not fail the submit", // tested by KernelGapsTest
            "#36: Kernel.wants calls interests() outside the guard around receive in Kernel.offer"
                    + " (review of #36)",
            "a retraction of a statement outside its causal past is rejected", // tested by KernelGapsTest
            "#36: Causality checks causes and links, not the statements a tuple names"
                    + " (review of #36: Causality.check)",
            "a retraction of a statement that does not exist is rejected", // tested by KernelGapsTest
            "#36: nothing checks that a tuple names a statement that exists (review of #36: Causality.check)",
            "a retracted attribute with another value does not retract", // tested by KernelGapsTest
            "#36: Statements treats every tuple with the retracted attribute as a retraction, whatever its"
                    + " value (review of #36: Statements.at)",
            "a tuple about a local identity its event does not carry is rejected", // tested by KernelGapsTest
            "#36: a local /n is not checked against the tuples of its event (review of #36: Assertion.parse)");

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
