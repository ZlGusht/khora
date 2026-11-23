package khora.kernel.surface;

import java.util.List;
import java.util.Set;
import khora.kernel.event.Action;
import khora.kernel.event.Event;

/**
 * Something that receives events and emits events in response, and nothing else
 * ([[Surfaces receive and emit events by interest]]).
 *
 * <p>Provisional: what it emits is submitted to the kernel's authority like any action; delivery is
 * synchronous, and a surface does not receive what it emitted.
 */
public interface Surface {

    /** Its name: who its emitted actions are by. */
    String name();

    /** Which events it receives. */
    Set<Interest> interests();

    /** Takes one event in; returns the actions it emits in response. */
    List<Action> receive(Event event);
}
