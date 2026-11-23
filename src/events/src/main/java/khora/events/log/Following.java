package khora.events.log;

/**
 * A handler following a log. Closing it stops delivery: once {@link #close()} returns, no further
 * event is handed over; one whose delivery had already begun may still complete.
 */
public interface Following extends AutoCloseable {
    @Override
    void close();
}
