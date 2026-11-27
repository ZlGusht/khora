package khora.query.neo4j;

import khora.events.log.EventLog;
import khora.query.Datastore;
import khora.query.DatastoreContract;

/**
 * The contract, over a datastore rebuilt from a snapshot taken after the log's first event, then
 * caught up with the rest of the log.
 */
class Neo4jSnapshotDatastoreTest extends DatastoreContract {
    @Override
    protected Datastore over(EventLog log) {
        var store = Neo4jTestServer.empty();
        store.take(log.read(1, 1), log);
        var snapshot = store.snapshot();
        store.clear();
        store.rebuild(snapshot, log);
        return store;
    }

    @Override
    protected boolean answersPastFrontiers() {
        return false;
    }
}
