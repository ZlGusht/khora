package khora.query;

import khora.events.log.EventLog;

class EventsDatastoreTest extends DatastoreContract {
    @Override
    protected Datastore over(EventLog log) {
        return new EventsDatastore(log);
    }
}
