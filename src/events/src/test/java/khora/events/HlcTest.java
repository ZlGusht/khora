package khora.events;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.atomic.AtomicLong;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.Test;

class HlcTest {

    @Test
    void followsWallClock() {
        var t = new Hlc(() -> 1000).next();
        assertEquals(1000, Hlc.millis(t));
        assertEquals(0, Hlc.counter(t));
    }

    @Test
    void neverGoesBackwards() {
        var wall = new AtomicLong(1000);
        var clock = new Hlc(wall::get);
        var first = clock.next();
        wall.set(900);
        assertTrue(clock.next() > first);
    }

    @Test
    void manyInOneMillisecondIncrease() {
        var clock = new Hlc(() -> 1000);
        var last = clock.next();
        for (int i = 0; i < 70_000; i++) {
            var t = clock.next();
            assertTrue(t > last);
            last = t;
        }
    }

    @Test
    void nextAfterCauseIsLater() {
        var cause = Hlc.pack(5000, 3);
        assertTrue(new Hlc(() -> 1000).next(cause) > cause);
    }

    @Test
    void packSplitsIntoMillisAndCounter() {
        var t = Hlc.pack(123_456, 7);
        assertEquals(123_456, Hlc.millis(t));
        assertEquals(7, Hlc.counter(t));
    }

    @Test
    void refusesToPassTheLargestTime() {
        long end = Hlc.millis(Long.MAX_VALUE);
        assertThrows(IllegalArgumentException.class, () -> new Hlc(() -> end).next(Long.MAX_VALUE));
        var clock = new Hlc(() -> end);
        assertEquals(Long.MAX_VALUE, clock.next(Long.MAX_VALUE - 1));
        assertThrows(IllegalArgumentException.class, clock::next);
    }

    @Test
    void aTimeFarAheadOfWallTimeIsRefusedAndTheClockDoesNotMove() {
        var clock = new Hlc(() -> 1000);
        var before = clock.next();
        assertThrows(IllegalArgumentException.class, () -> clock.next(Long.MAX_VALUE - 1));
        assertThrows(IllegalArgumentException.class,
                () -> clock.next(Hlc.pack(1000 + Hlc.MAX_DRIFT_MILLIS + 1, 0)));
        var after = clock.next();
        assertEquals(1000, Hlc.millis(after));
        assertTrue(after > before);
        var ahead = Hlc.pack(1000 + Hlc.MAX_DRIFT_MILLIS, 5);
        assertTrue(clock.next(ahead) > ahead);
    }
}
