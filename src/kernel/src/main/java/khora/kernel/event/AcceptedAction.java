package khora.kernel.event;

import java.util.Objects;

/**
 * An event's body when khora accepted an action: who acted, and the action.
 *
 * <p>Provisional: as {@link By} and {@link Action}.
 *
 * @param by who acted
 * @param action what was done
 */
public record AcceptedAction(By by, Action action) implements Body {
    public AcceptedAction {
        Objects.requireNonNull(by, "by");
        Objects.requireNonNull(action, "action");
    }
}
