package khora.surfaces.contract;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;
import khora.events.Event;
import khora.events.EventId;
import khora.events.Identity;
import khora.events.log.EventLog;
import khora.events.log.Following;
import khora.events.log.Logged;

/**
 * A log an authority decides on: it reads the log it wraps, and refuses {@link #append},
 * {@link #follow} and {@link #close} with {@link UnsupportedOperationException}. It remembers
 * whether any of them was tried, even if the caller caught the exception.
 */
public final class ReadOnlyEventLog implements EventLog {
    private final EventLog inner;
    private boolean tried;

    public ReadOnlyEventLog(EventLog inner) {
        this.inner = Objects.requireNonNull(inner, "inner");
    }

    /** Whether {@link #append}, {@link #follow} or {@link #close} was called. */
    public boolean tried() {
        return tried;
    }

    @Override
    public List<Long> append(List<Event> events) {
        throw refuse("append");
    }

    @Override
    public List<Logged> read(long from, int max) {
        return inner.read(from, max);
    }

    @Override
    public Optional<Event> get(EventId id) {
        return inner.get(id);
    }

    @Override
    public List<Event> about(Identity thing) {
        return inner.about(thing);
    }

    @Override
    public Following follow(long from, Consumer<Logged> handler) {
        throw refuse("follow");
    }

    @Override
    public void close() {
        throw refuse("close");
    }

    private UnsupportedOperationException refuse(String what) {
        tried = true;
        return new UnsupportedOperationException("an authority may not " + what + " the log it decides on");
    }
}
