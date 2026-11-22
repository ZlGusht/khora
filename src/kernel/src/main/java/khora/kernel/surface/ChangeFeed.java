package khora.kernel.surface;

import java.util.Optional;

/**
 * A live feed of a surface's changes, read on a virtual thread.
 *
 * <p>Out with events: becomes the feed of a surface's event stream, yielding events. Its blocking
 * {@code next} and {@code close} stay ([[Every surface has one event stream, read from a frontier]]).
 * {@code java.util.concurrent.Flow} can be adapted to it later.
 */
public interface ChangeFeed extends AutoCloseable {

    /**
     * The next change, waiting until there is one.
     *
     * @return the change, or empty once the feed is closed
     * @throws InterruptedException if the reading thread is interrupted while waiting
     */
    Optional<Change> next() throws InterruptedException;

    /** Closes the feed and wakes a waiting {@link #next()}. Closing twice does nothing. */
    @Override
    void close();
}
