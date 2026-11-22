package khora.kernel.event;

import java.util.Objects;
import khora.kernel.json.Json;

/**
 * An action: the id of its definition and its arguments
 * ([[An accepted action is one kind of event]]).
 *
 * <p>An assert's arguments are checked on construction, so a malformed assert never reaches the log.
 *
 * <p>Provisional: the kernel reads only the basic vocabulary's asserts; other definitions are opaque.
 *
 * @param definition the id of the action's definition, which says its vocabulary and version
 * @param args the action's arguments
 */
public record Action(Identity definition, Json.Obj args) {
    public Action {
        Objects.requireNonNull(definition, "definition");
        Objects.requireNonNull(args, "args");
        if (definition.equals(Basic.ASSERT)) {
            Assertion.parse(args);
        }
    }
}
