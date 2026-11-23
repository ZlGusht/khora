package khora.kernel.log;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * A {@link LogStore} in memory, for tests and as the reference for the contract. Its deduplication
 * window has no limit: an event id is never stored twice.
 */
public final class InMemoryLogStore implements LogStore {

    private final List<LogRecord> records = new ArrayList<>();
    private final Map<String, Long> positionByEventId = new HashMap<>();
    private final List<Subscription> subscriptions = new ArrayList<>();
    private boolean closed;

    @Override
    public synchronized List<Long> append(List<LogRecord> batch) {
        if (closed) {
            throw new LogStoreException("append to a closed log store");
        }
        if (batch.size() > MAX_APPEND) {
            throw new LogStoreException("append of " + batch.size() + " records exceeds " + MAX_APPEND);
        }
        List<Long> positions = new ArrayList<>(batch.size());
        for (LogRecord record : batch) {
            Long existing = positionByEventId.get(record.eventId());
            if (existing != null) {
                positions.add(existing);
                continue;
            }
            long position = records.size() + 1L;
            records.add(record.withPosition(position));
            positionByEventId.put(record.eventId(), position);
            positions.add(position);
        }
        notifyAll();
        return positions;
    }

    @Override
    public synchronized List<LogRecord> read(long fromPosition, int max) {
        int from = (int) Math.max(fromPosition, 1) - 1;
        if (max <= 0 || from >= records.size()) {
            return List.of();
        }
        return List.copyOf(records.subList(from, (int) Math.min((long) from + max, records.size())));
    }

    @Override
    public synchronized List<LogRecord> history(String entityId) {
        if (entityId == null || entityId.isBlank()) {
            throw new IllegalArgumentException("entity id must not be null or blank");
        }
        return records.stream()
                .filter(r -> r.primaryEntity().filter(entityId::equals).isPresent())
                .toList();
    }

    @Override
    public synchronized Subscription subscribe(long fromPosition, Consumer<LogRecord> handler) {
        Delivery delivery = new Delivery(Math.max(fromPosition, 1), handler);
        subscriptions.add(delivery);
        delivery.thread.start();
        return delivery;
    }

    @Override
    public void close() {
        List<Subscription> toClose;
        synchronized (this) {
            closed = true;
            toClose = List.copyOf(subscriptions);
            notifyAll();
        }
        toClose.forEach(Subscription::close);
    }

    private final class Delivery implements Subscription {
        private final Consumer<LogRecord> handler;
        private final Thread thread;
        private long next;
        private boolean stopped;

        Delivery(long from, Consumer<LogRecord> handler) {
            this.next = from;
            this.handler = handler;
            this.thread = Thread.ofVirtual().unstarted(this::run);
        }

        private void run() {
            while (true) {
                LogRecord record;
                synchronized (InMemoryLogStore.this) {
                    while (!stopped && !closed && next > records.size()) {
                        try {
                            InMemoryLogStore.this.wait();
                        } catch (InterruptedException e) {
                            return;
                        }
                    }
                    if (stopped || closed) {
                        return;
                    }
                    record = records.get((int) next - 1);
                    next++;
                }
                try {
                    handler.accept(record);
                } catch (RuntimeException e) {
                    // A failing handler does not end the subscription; the next record is delivered.
                }
            }
        }

        @Override
        public void close() {
            synchronized (InMemoryLogStore.this) {
                stopped = true;
                subscriptions.remove(this);
                InMemoryLogStore.this.notifyAll();
            }
        }
    }
}
