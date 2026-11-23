package khora.kernel.log;

import java.util.List;
import java.util.function.Consumer;

/**
 * An append-only log of {@link LogRecord}s. Positions start at 1 and increase by 1.
 *
 * <p>Within the store's deduplication window, a record whose event id is already stored is not
 * stored again. The window is 2 minutes on NATS ({@code NatsLogStore}); a store may keep no limit.
 * Beyond the window, a store may store an event id again. Uniqueness there comes from the event
 * ids themselves: they are content hashes (K1), so two different records never share one, and
 * the same record is not appended again after the window has passed.
 */
public interface LogStore extends AutoCloseable {

    /** The most records one {@link #append} call accepts. */
    int MAX_APPEND = 1000;

    /**
     * Appends the records in order and returns one position per record, all or none of them stored.
     * Within the deduplication window, a record whose event id is already stored, or appears earlier
     * in {@code records}, is not stored again and gets the position of the stored record.
     *
     * <p>Some refusals happen before anything is sent, and store nothing: more than
     * {@value #MAX_APPEND} records, a record too large for the store, an event id or entity id the
     * store cannot hold, or a store that is closed. They throw {@link LogStoreException}.
     *
     * <p>Any other {@link LogStoreException} means the outcome is unknown: the records are stored
     * all or none, but the caller cannot always tell which. An ack timeout or a dropped connection
     * after the store committed the batch leaves it fully stored. Retrying with the same event ids
     * within the deduplication window is safe: it stores nothing twice and returns the positions.
     */
    List<Long> append(List<LogRecord> records);

    /** Up to {@code max} records, in position order, starting at {@code fromPosition}. */
    List<LogRecord> read(long fromPosition, int max);

    /**
     * The records whose primary entity is {@code entityId}, in position order. A null or blank
     * {@code entityId} is refused with {@link IllegalArgumentException}: no record has one.
     */
    List<LogRecord> history(String entityId);

    /**
     * Delivers the records from {@code fromPosition} on, then each new record, to {@code handler}.
     * Delivery runs on another thread, one record at a time, and stops when the subscription is
     * closed. A handler that throws does not stop the subscription: the next record is delivered.
     *
     * <p>A store may stop a subscription on a stored record it cannot read; it logs that record's
     * position. The handler receives no further records then.
     */
    Subscription subscribe(long fromPosition, Consumer<LogRecord> handler);

    @Override
    void close();
}
