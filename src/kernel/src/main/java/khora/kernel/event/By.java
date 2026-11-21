package khora.kernel.event;

import java.util.Objects;
import java.util.Optional;

/**
 * Who took an action, under which grant, and through what.
 *
 * <p>Provisional: principals, grants and the surface or adapter an action went through are plain
 * names until they are entities.
 *
 * @param principal who acted
 * @param onBehalfOf for whom, if not for themselves
 * @param grant the grant the action ran under
 * @param via the surface or adapter the action went through
 */
public record By(String principal, Optional<String> onBehalfOf, String grant, String via) {
    public By {
        Objects.requireNonNull(principal, "principal");
        Objects.requireNonNull(onBehalfOf, "onBehalfOf");
        Objects.requireNonNull(grant, "grant");
        Objects.requireNonNull(via, "via");
    }
}
