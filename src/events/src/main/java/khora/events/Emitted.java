package khora.events;

import java.util.Objects;

/**
 * An event's body when someone emitted an action: who, and the action. It is a fact about what they
 * said, true whatever anyone else said.
 *
 * <p>Exploration: replaces the accepted action. Nothing accepts a fact; a gate may refuse an action
 * on a shape it guards before it becomes one, and then there is no event.
 *
 * @param by who emitted it
 * @param action what was done
 */
public record Emitted(By by, Action action) implements Body {
    public Emitted {
        Objects.requireNonNull(by, "by");
        Objects.requireNonNull(action, "action");
    }

    @Override
    public String kind() {
        return "emitted";
    }
}
