package khora.kernel.log;

/**
 * A handler following a log. Closing it stops delivery: no event is handed over once
 * {@link #close()} returns, though one already being delivered may still arrive.
 */
public interface Following extends AutoCloseable {
    @Override
    void close();
}
