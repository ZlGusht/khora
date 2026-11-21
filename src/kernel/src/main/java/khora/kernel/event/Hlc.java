package khora.kernel.event;

import java.util.Objects;
import java.util.function.LongSupplier;

/**
 * A hybrid logical clock for recorded time: close to wall time, never going backwards, and always
 * after an event's causes (ADR-0YB decision 1).
 *
 * <p>A time is the wall time in milliseconds in the high 48 bits and a counter in the low 16 bits.
 * A counter overflow carries into the milliseconds.
 *
 * <p>Provisional: one clock per process; how clocks of several writers meet is not decided.
 */
public final class Hlc {
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
     * @throws IllegalArgumentException when no such time exists
     */
    public synchronized long next(long after) {
        if (after == Long.MAX_VALUE || last == Long.MAX_VALUE) {
            throw new IllegalArgumentException("no time is later than the largest one");
        }
        var candidate = Math.max(pack(wallMillis.getAsLong(), 0), Math.max(last + 1, after + 1));
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
