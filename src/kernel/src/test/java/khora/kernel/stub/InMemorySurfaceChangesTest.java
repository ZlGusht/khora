package khora.kernel.stub;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import khora.kernel.surface.Change;
import khora.kernel.surface.ChangeFeed;
import khora.kernel.surface.Condition;
import khora.kernel.surface.Frontier;
import khora.kernel.surface.Operation;
import khora.kernel.surface.WriteAnswer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class InMemorySurfaceChangesTest {

    InMemorySurface surface;
    long writes;

    @BeforeEach
    void setUp() {
        surface = new InMemorySurface(new InMemorySurfaceTest.CountingActions());
        writes = 0;
    }

    /** Writes target=value, conditioned on the surface's current frontier. */
    WriteAnswer write(String target, String value) {
        var read = writes == 0 ? Frontier.empty() : Frontier.empty().advance(InMemorySurface.ORIGIN, writes);
        var current = surface.read(InMemorySurfaceTest.readLatest(target), InMemorySurfaceTest.CONTEXT).value();
        var answer = surface.write(new Operation(target, value), new Condition(read, current),
                InMemorySurfaceTest.CONTEXT);
        writes++;
        return answer;
    }

    /** Reads the next change on a virtual thread. */
    static CompletableFuture<Optional<Change>> nextOnVirtualThread(ChangeFeed feed) {
        var result = new CompletableFuture<Optional<Change>>();
        Thread.ofVirtual().start(() -> {
            try {
                result.complete(feed.next());
            } catch (Throwable t) {
                result.completeExceptionally(t);
            }
        });
        return result;
    }

    @Test
    void feedReplaysWritesAfterSince() throws Exception {
        write("x", "1");
        var second = write("x", "2");
        var third = write("x", "3");

        try (var feed = surface.changes(Frontier.empty().advance(InMemorySurface.ORIGIN, 1))) {
            var a = feed.next().orElseThrow();
            var b = feed.next().orElseThrow();

            assertEquals(second.action(), a.event());
            assertEquals("2", a.value());
            assertEquals(Frontier.empty().advance(InMemorySurface.ORIGIN, 2), a.at());
            assertEquals(third.action(), b.event());
            assertEquals("3", b.value());
        }
    }

    @Test
    void feedDeliversLaterWrite() throws Exception {
        try (var feed = surface.changes(Frontier.empty())) {
            var next = nextOnVirtualThread(feed);
            Thread.sleep(50);

            var answer = write("x", "1");

            var change = next.get(1, TimeUnit.SECONDS).orElseThrow();
            assertEquals(answer.action(), change.event());
        }
    }

    @Test
    void closeWakesBlockedNext() throws Exception {
        var feed = surface.changes(Frontier.empty());
        var next = nextOnVirtualThread(feed);
        Thread.sleep(50);

        feed.close();

        assertTrue(next.get(1, TimeUnit.SECONDS).isEmpty());
    }

    @Test
    void closeIsIdempotent() throws Exception {
        var feed = surface.changes(Frontier.empty());
        feed.close();
        feed.close();
        assertTrue(feed.next().isEmpty());
    }
}
