package khora.events;

import java.util.Objects;
import java.util.function.LongSupplier;

/**
 * A hybrid logical clock for recorded time: close to wall time, never going backwards, and always
 * after an event's causes (ADR-0YB decision 1).
 *
 * <p>A time is the wall time in milliseconds in the high 48 bits and a counter in the low 16 bits.
 * A counter overflow carries into the milliseconds.
 *
 * <p>A time to follow that is more than {@link #MAX_DRIFT_MILLIS} ahead of wall time is refused,
 * and the clock does not move: otherwise one event from a skewed clock would pull every later time
 * into the future, or, near the largest time, stop the clock.
 *
 * <p>Provisional: one clock per process; how clocks of several writers meet is not decided.
 */
public final class Hlc {
    /** How far ahead of wall time a time to follow may be. */
    public static final long MAX_DRIFT_MILLIS = 60_000;

    private final LongSupplier wallMillis;
    private long last = Long.MIN_VALUE;

    public Hlc(LongSupplier wallMillis) {
        this.wallMillis = Objects.requireNonNull(wallMillis, "wallMillis");
    }

    /** A clock on the system's wall time. */
    public static Hlc system() {
        return new Hlc(System::currentTimeMillis);
    }

    /** A time later than every time this clock gave before. */
    public long next() {
        return next(Long.MIN_VALUE);
    }

    /**
     * A time later than {@code after} and than every time this clock gave before.
     *
     * @throws IllegalArgumentException when {@code after} is more than {@link #MAX_DRIFT_MILLIS}
     *     ahead of wall time, or no such time exists
     */
    public synchronized long next(long after) {
        long wall = wallMillis.getAsLong();
        long limit = wall + MAX_DRIFT_MILLIS >= millis(Long.MAX_VALUE) ? Long.MAX_VALUE
                : pack(wall + MAX_DRIFT_MILLIS, 0xFFFF);
        if (after > limit) {
            throw new IllegalArgumentException("time " + after + " is more than " + MAX_DRIFT_MILLIS
                    + " ms ahead of wall time");
        }
        if (after == Long.MAX_VALUE || last == Long.MAX_VALUE) {
            throw new IllegalArgumentException("no time is later than the largest one");
        }
        var candidate = Math.max(pack(wall, 0), Math.max(last + 1, after + 1));
        last = candidate;
        return candidate;
    }

    public static long pack(long millis, int counter) {
        return millis << 16 | (counter & 0xFFFF);
    }

    public static long millis(long time) {
        return time >>> 16;
    }

    public static int counter(long time) {
        return (int) (time & 0xFFFF);
    }
}
