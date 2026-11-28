package khora.surfaces;

import java.util.List;
import java.util.Set;
import khora.events.Action;
import khora.events.Event;

/**
 * Something that receives events and emits events in response, and nothing else
 * ([[Surfaces receive and emit events by interest]]).
 *
 * <p>A surface owns its reading of the events: what a thing is for it, and whether two events
 * clash. Another surface may read the same events otherwise.
 *
 * <p>Provisional: what it emits becomes an event triggered by what it received, through the gates
 * that guard it; delivery is synchronous, and a surface does not receive what it emitted.
 */
public interface Surface {

    /** Its name: who its emitted actions are by. */
    String name();

    /** Which events it receives. */
    Set<Interest> interests();

    /** Takes one event in; returns the actions it emits in response. */
    List<Action> receive(Event event);
}
