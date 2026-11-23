package khora.kernel.log;

/**
 * A live feed of records from a {@link LogStore}. Closing it stops delivery: no new record is
 * handed to the handler once {@link #close()} returns, but a record already being delivered may
 * still reach the handler after it returns.
 */
public interface Subscription extends AutoCloseable {

    @Override
    void close();
}
