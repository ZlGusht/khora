package khora.query.neo4j;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;
import java.util.Optional;
import khora.events.Emitted;
import khora.events.Assertion;
import khora.events.Event;
import khora.query.EventsDatastore;
import khora.query.Operator.Scan;
import khora.query.Query;
import org.junit.jupiter.api.Test;

/** How the datastore takes events in: from where it stopped, never twice, and as they arrive. */
class TakingInTest {

    @Test
    void catchingUpTakesInOnlyWhatIsNew() {
        var generated = new GeneratedLog(11, 30);
        var store = Neo4jTestServer.empty();
        assertEquals(30, store.catchUp(generated.log));
        assertEquals(0, store.catchUp(generated.log));
        assertEquals(0, store.take(generated.log.read(5, 10), generated.log));
        assertEquals(30, store.position());
        assertEquals(new EventsDatastore(generated.log).answer(Query.of(Scan.things("t"))).at(), store.position());
    }

    @Test
    void aGapIsRefused() {
        var generated = new GeneratedLog(12, 10);
        var store = Neo4jTestServer.empty();
        store.take(generated.log.read(1, 3), generated.log);
        assertThrows(IllegalStateException.class, () -> store.take(generated.log.read(5, 1), generated.log));
        assertEquals(3, store.position());
    }

    @Test
    void followingTakesInNewEvents() throws InterruptedException {
        var generated = new GeneratedLog(13, 5);
        var store = Neo4jTestServer.empty();
        var following = store.follow(generated.log);
        try {
            assertEquals(5, store.position());
            var next = Event.create(Optional.empty(), 2000, Optional.empty(),
                    new Emitted(GeneratedLog.BY, Assertion.action(Assertion.entity())));
            generated.log.append(List.of(next));
            for (int i = 0; i < 200 && store.position() < 6; i++) {
                Thread.sleep(25);
            }
            assertEquals(6, store.position());
            assertEquals(new EventsDatastore(generated.log).answer(Query.of(Scan.things("t"))).at(), store.position());
            assertEquals(EquivalenceTest.sorted(new EventsDatastore(generated.log).answer(Query.of(Scan.things("t")))),
                    EquivalenceTest.sorted(store.answer(Query.of(Scan.things("t")))));
        } finally {
            following.close();
        }
    }
}
