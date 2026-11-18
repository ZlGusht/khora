package khora.kernel.log;

import java.util.List;
import java.util.function.Consumer;

/**
 * An append-only log of {@link LogRecord}s. Positions start at 1 and increase by 1.
 * A record whose event id is already stored is not stored again.
 */
public interface LogStore extends AutoCloseable {

    /** The most records one {@link #append} call accepts. */
    int MAX_APPEND = 1000;

    /**
     * Appends the records in order and returns one position per record. A record whose event id is
     * already stored gets the position of the stored record. More than {@value #MAX_APPEND} records
     * are refused with a {@link LogStoreException}, and nothing is stored.
     */
    List<Long> append(List<LogRecord> records);

    /** Up to {@code max} records, in position order, starting at {@code fromPosition}. */
    List<LogRecord> read(long fromPosition, int max);

    /** The records whose primary entity is {@code entityId}, in position order. */
    List<LogRecord> history(String entityId);

    /**
     * Delivers the records from {@code fromPosition} on, then each new record, to {@code handler}.
     * Delivery runs on another thread, one record at a time, and stops when the subscription is
     * closed. A handler that throws does not stop the subscription: the next record is delivered.
     */
    Subscription subscribe(long fromPosition, Consumer<LogRecord> handler);

    @Override
    void close();
}
