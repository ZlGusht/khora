package khora.kernel.surface;

import khora.kernel.event.EventId;

/**
 * Where a surface records the action each call is (ADR-0YA decision 2: one call is one action). Reads
 * are recorded too: a read is an action someone takes ([[Reads are events]]).
 *
 * <p>Provisional: K3 and K10 back it with the log. A write is recorded before the surface decides on
 * it, so a rejected write has an id too; in K1 only accepted actions are events, and how rejections
 * are kept is not decided yet.
 */
public interface Actions {

    /**
     * Records one action.
     *
     * @param context the call's context
     * @param call which call it is: {@code "read"} or {@code "write"}. Out with events: goes, as each
 *     call is an event with its own definition
     * @return the action's id
     */
    EventId record(Context context, String call);
}
