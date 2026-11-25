package khora.query.neo4j;

import khora.events.log.EventLog;
import khora.query.Datastore;
import khora.query.DatastoreContract;

/** The contract, over a datastore that took in the log from its start. */
class Neo4jDatastoreTest extends DatastoreContract {
    @Override
    protected Datastore over(EventLog log) {
        var store = Neo4jTestServer.empty();
        store.catchUp(log);
        return store;
    }
}
