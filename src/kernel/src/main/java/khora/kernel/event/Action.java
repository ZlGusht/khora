package khora.kernel.event;

import java.util.Objects;
import khora.kernel.json.Json;

/**
 * An action: the id of its definition and its arguments
 * ([[An event is an accepted action, and an assert is the basic action]]).
 *
 * <p>Provisional: the kernel reads only the basic vocabulary's asserts; other definitions are opaque.
 *
 * @param definition the id of the action's definition, which says its vocabulary and version
 * @param args the action's arguments
 */
public record Action(EventId definition, Json.Obj args) {
    public Action {
        Objects.requireNonNull(definition, "definition");
        Objects.requireNonNull(args, "args");
    }
}
