package khora.query.neo4j;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import khora.events.Frontier;
import khora.query.EventsDatastore;
import khora.query.Operator.Scan;
import khora.query.Query;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** A rebuild from a snapshot equals a rebuild from the start of the log (ADR-0XY decision 10). */
class SnapshotTest {
    @TempDir
    Path dir;

    @Test
    void aRebuildFromASnapshotEqualsARebuildFromTheStart() throws IOException {
        var generated = new GeneratedLog(7, 300);
        var store = Neo4jTestServer.empty();
        assertEquals(300, store.catchUp(generated.log));
        var fromStart = store.snapshot();
        var shape = store.shape();

        store.clear();
        store.take(generated.log.read(1, 120), generated.log);
        var file = dir.resolve("at-120.jsonl");
        store.snapshot().write(file);
        store.clear();

        store.rebuild(Snapshot.read(file), generated.log);
        assertEquals(fromStart, store.snapshot());
        assertEquals(shape, store.shape());
        assertEquals(300, store.position());
        var all = Query.of(Scan.statements("t", GeneratedLog.NAME, "n"));
        assertEquals(EquivalenceTest.sorted(new EventsDatastore(generated.log).answer(all)),
                EquivalenceTest.sorted(store.answer(all)));
    }

    @Test
    void aSnapshotIsAtAFrontier() throws IOException {
        var generated = new GeneratedLog(8, 40);
        var store = Neo4jTestServer.empty();
        store.take(generated.log.read(1, 25), generated.log);
        var snapshot = store.snapshot();
        assertEquals(25, snapshot.position());
        assertEquals(Frontier.of(generated.events.subList(0, 25).stream().map(e -> e.id()).toList(), generated.log),
                snapshot.at());
        var file = dir.resolve("s.jsonl");
        snapshot.write(file);
        assertEquals(snapshot, Snapshot.read(file));
        assertFalse(snapshot.rows().isEmpty());
    }

    @Test
    void anEmptyDatastoreHasAnEmptySnapshot() {
        var snapshot = Neo4jTestServer.empty().snapshot();
        assertEquals(new Snapshot(0, Frontier.empty(), Optional.empty(), List.of()), snapshot);
    }

    @Test
    void aSnapshotOfAnotherLogIsRefused() {
        var one = new GeneratedLog(9, 10);
        var other = new GeneratedLog(10, 10);
        var store = Neo4jTestServer.empty();
        store.catchUp(one.log);
        var snapshot = store.snapshot();
        assertThrows(IllegalArgumentException.class, () -> store.rebuild(snapshot, other.log));
    }

    @Test
    void aMalformedSnapshotIsRefused() throws IOException {
        var file = dir.resolve("bad.jsonl");
        Files.writeString(file, "{\"type\":\"snapshot\",\"version\":2,\"position\":0,\"at\":[]}\n{\"type\":\"event\"}\n");
        assertThrows(IllegalArgumentException.class, () -> Snapshot.read(file));
        Files.writeString(file, "{\"type\":\"snapshot\",\"version\":1,\"position\":0,\"at\":[]}\n");
        assertThrows(IllegalArgumentException.class, () -> Snapshot.read(file));
    }
}
