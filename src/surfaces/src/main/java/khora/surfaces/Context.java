package khora.surfaces;

import java.util.Objects;
import java.util.Optional;
import khora.events.By;

/**
 * Who a call is made by and for, under which grant, and what it may spend.
 *
 * <p>Provisional: principals and grants are plain names, and the budget a count of units, until they
 * are defined (ADR-0XZ). Nothing spends the budget yet.
 *
 * @param principal who acts
 * @param onBehalfOf for whom, if not for themselves
 * @param grant the grant the call runs under
 * @param budget what it may spend; zero or more
 */
public record Context(String principal, Optional<String> onBehalfOf, String grant, long budget) {
    public Context {
        Objects.requireNonNull(principal, "principal");
        Objects.requireNonNull(onBehalfOf, "onBehalfOf");
        Objects.requireNonNull(grant, "grant");
        if (budget < 0) {
            throw new IllegalArgumentException("budget is negative: " + budget);
        }
    }

    /** Who an action taken through {@code via} is by. */
    public By by(String via) {
        return new By(principal, onBehalfOf, grant, via);
    }
}
