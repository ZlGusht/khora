package khora.events.log;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import khora.events.About;
import khora.events.Event;
import khora.events.EventId;
import khora.events.Identity;

/** An event log in memory, for tests and for kernels that need no durability. */
public final class InMemoryEventLog implements EventLog {
    private final List<Event> events = new ArrayList<>();
    private final Map<EventId, Long> positions = new HashMap<>();
    private boolean closed;

    @Override
    public synchronized List<Long> append(List<Event> batch) {
        if (closed) {
            throw new EventLogException("the log is closed");
        }
        if (batch.size() > MAX_APPEND) {
            throw new EventLogException(batch.size() + " events in one append; the most is " + MAX_APPEND);
        }
        var added = new LinkedHashMap<EventId, Long>();
        var result = new ArrayList<Long>();
        long next = events.size() + 1;
        for (var event : batch) {
            var known = positions.containsKey(event.id()) ? positions.get(event.id()) : added.get(event.id());
            if (known == null) {
                known = next++;
                added.put(event.id(), known);
            }
            result.add(known);
        }
        for (var event : batch) {
            if (added.containsKey(event.id()) && !positions.containsKey(event.id())) {
                events.add(event);
                positions.put(event.id(), (long) events.size());
            }
        }
        notifyAll();
        return result;
    }

    @Override
    public synchronized List<Logged> read(long from, int max) {
        var result = new ArrayList<Logged>();
        for (long p = Math.max(from, 1); p <= events.size() && result.size() < max; p++) {
            result.add(new Logged(p, events.get((int) p - 1)));
        }
        return result;
    }

    @Override
    public synchronized Optional<Event> get(EventId id) {
        return lookup(id);
    }

    private Optional<Event> lookup(EventId id) {
        var position = positions.get(id);
        return position == null ? Optional.empty() : Optional.of(events.get((int) (position - 1)));
    }

    @Override
    public synchronized List<Event> about(Identity thing) {
        return events.stream().filter(e -> About.of(e, this::lookup).contains(thing)).toList();
    }

    @Override
    public Following follow(long from, Consumer<Logged> handler) {
        var stopped = new boolean[1];
        var thread = Thread.ofVirtual().start(() -> {
            long next = Math.max(from, 1);
            while (true) {
                List<Logged> batch;
                synchronized (this) {
                    while (!stopped[0] && !closed && next > events.size()) {
                        try {
                            wait();
                        } catch (InterruptedException e) {
                            return;
                        }
                    }
                    if (stopped[0] || closed) {
                        return;
                    }
                    batch = read(next, Integer.MAX_VALUE);
                }
                for (var logged : batch) {
                    synchronized (this) {
                        if (stopped[0]) {
                            return;
                        }
                    }
                    try {
                        handler.accept(logged);
                    } catch (RuntimeException e) {
                        // A failing handler does not stop following.
                    }
                    next = logged.position() + 1;
                }
            }
        });
        return () -> {
            synchronized (this) {
                stopped[0] = true;
                notifyAll();
            }
            thread.interrupt();
        };
    }

    @Override
    public synchronized void close() {
        closed = true;
        notifyAll();
    }
}
