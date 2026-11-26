package khora.surfaces.contract;

import khora.events.Event;
import khora.events.log.EventLog;
import khora.surfaces.Authority;

/** An authority that accepts everything, for scenarios about something else. */
public final class AcceptAll implements Authority {

    @Override
    public String name() {
        return "accept-all";
    }

    @Override
    public Verdict decide(Event candidate, EventLog log) {
        return new Accept();
    }
}
