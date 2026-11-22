package khora.kernel.surface;

import khora.kernel.event.EventId;

/**
 * Where a surface records the action each call is (ADR-0YA decision 2: one call is one action).
 *
 * Reads are recorded too: a read is an action someone takes ([[Reads are events]]).
 *
 * <p>Provisional: K3 and K10 back it with the log.
 */
public interface Actions {

    /**
     * Records one action.
     *
     * @param context the call's context
     * @param call which call it is: {@code "read"} or {@code "write"}
     * @return the action's id
     */
    EventId record(Context context, String call);
}
