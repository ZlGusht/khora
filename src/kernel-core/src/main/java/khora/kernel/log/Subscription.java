package khora.kernel.log;

/** A live feed of records from a {@link LogStore}. Closing it stops delivery. */
public interface Subscription extends AutoCloseable {

    @Override
    void close();
}
