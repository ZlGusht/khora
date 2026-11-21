package khora.kernel.stub;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import khora.kernel.surface.Change;
import khora.kernel.surface.ChangeFeed;
import khora.kernel.surface.Frontier;
import khora.kernel.surface.Operation;
import khora.kernel.surface.WriteAnswer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class InMemorySurfaceChangesTest {

    InMemorySurface surface;

    @BeforeEach
    void setUp() {
        surface = new InMemorySurface(new InMemorySurfaceTest.CountingActions());
    }

    /** Writes target=value, conditioned on what a read sees now. */
    WriteAnswer write(String target, String value) {
        return surface.write(new Operation(target, value), InMemorySurfaceTest.asRead(surface, target),
                InMemorySurfaceTest.CONTEXT);
    }

    Frontier current() {
        return surface.read(InMemorySurfaceTest.readLatest("x"), InMemorySurfaceTest.CONTEXT).seen();
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
        var since = current();
        var second = write("x", "2");
        var third = write("x", "3");

        try (var feed = surface.changes(since)) {
            var a = feed.next().orElseThrow();
            var b = feed.next().orElseThrow();

            assertEquals(second.action(), a.event());
            assertEquals("2", a.value());
            assertEquals(third.action(), b.event());
            assertEquals("3", b.value());
            assertEquals(current(), b.at());
        }
    }

    @Test
    void feedDeliversLaterWrite() throws Exception {
        try (var feed = surface.changes(current())) {
            var next = nextOnVirtualThread(feed);
            Thread.sleep(50);

            var answer = write("x", "1");

            var change = next.get(1, TimeUnit.SECONDS).orElseThrow();
            assertEquals(answer.action(), change.event());
        }
    }

    @Test
    void unknownSinceIsRefused() {
        assertThrows(IllegalArgumentException.class, () -> surface.changes(new Frontier("somewhere else")));
    }

    @Test
    void closeWakesBlockedNext() throws Exception {
        var feed = surface.changes(current());
        var next = nextOnVirtualThread(feed);
        Thread.sleep(50);

        feed.close();

        assertTrue(next.get(1, TimeUnit.SECONDS).isEmpty());
    }

    @Test
    void closeIsIdempotent() throws Exception {
        var feed = surface.changes(current());
        feed.close();
        feed.close();
        assertTrue(feed.next().isEmpty());
    }
}
