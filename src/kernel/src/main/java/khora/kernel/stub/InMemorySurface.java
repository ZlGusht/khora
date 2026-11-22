package khora.kernel.stub;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.locks.ReentrantLock;
import khora.kernel.surface.Actions;
import khora.kernel.surface.Change;
import khora.kernel.surface.ChangeFeed;
import khora.kernel.surface.ChangingSurface;
import khora.kernel.surface.Completeness;
import khora.kernel.surface.Condition;
import khora.kernel.surface.Context;
import khora.kernel.surface.Declaration;
import khora.kernel.surface.Effects;
import khora.kernel.event.Frontier;
import khora.kernel.surface.History;
import khora.kernel.surface.Operation;
import khora.kernel.surface.ReadRequest;
import khora.kernel.surface.Reads;
import khora.kernel.surface.Result;
import khora.kernel.surface.WritableSurface;
import khora.kernel.surface.WriteAnswer;
import khora.kernel.surface.Writes;

/**
 * A stub surface that holds values in memory, for tests and for building against the contract.
 *
 * <p>It reports a new frontier after each applied write, made of that write's action, and recognises
 * only frontiers it reported, by equality.
 * It keeps no history, so it answers only at its current frontier.
 *
 * <p>Out with events: the map of values and the change list go. The stub keeps events and works out
 * values from them, and serves its event stream.
 */
public final class InMemorySurface implements WritableSurface, ChangingSurface {

    private static final Declaration DECLARATION =
            new Declaration(true, Effects.NATIVE, History.NONE, Reads.DETERMINISTIC, Writes.CONDITIONAL);

    private final Actions actions;
    private final ReentrantLock lock = new ReentrantLock();
    private final java.util.concurrent.locks.Condition changed = lock.newCondition();
    private final List<Change> changes = new ArrayList<>();
    private final Map<String, String> values = new HashMap<>();
    /** Every frontier the stub reported, in order: the one after n changes is at index n. */
    private final List<Frontier> frontiers = new ArrayList<>(List.of(Frontier.empty()));
    private Frontier frontier = frontiers.getFirst();

    public InMemorySurface(Actions actions) {
        this.actions = Objects.requireNonNull(actions, "actions");
    }

    @Override
    public Declaration declares() {
        return DECLARATION;
    }

    @Override
    public Result read(ReadRequest request, Context context) {
        Objects.requireNonNull(request, "request");
        Objects.requireNonNull(context, "context");
        var action = Objects.requireNonNull(actions.record(context, "read"), "action");
        lock.lock();
        try {
            var value = Optional.ofNullable(values.get(request.target()));
            var wanted = request.time().at();
            // The stub keeps no history (History.NONE): it can only answer at its current frontier.
            Completeness completeness = wanted.isEmpty() || frontier.equals(wanted.get())
                    ? new Completeness.Complete()
                    : new Completeness.Partial("only the current frontier can be read: value is as of it");
            return new Result(action, value, completeness, frontier, List.of());
        } finally {
            lock.unlock();
        }
    }

    @Override
    public WriteAnswer write(Operation operation, Condition condition, Context context) {
        Objects.requireNonNull(operation, "operation");
        Objects.requireNonNull(condition, "condition");
        Objects.requireNonNull(context, "context");
        var action = Objects.requireNonNull(actions.record(context, "write"), "action");
        lock.lock();
        try {
            if (!frontier.equals(condition.read())) {
                return new WriteAnswer.Rejected(action, "the surface moved past the frontier the write read");
            }
            var current = Optional.ofNullable(values.get(operation.target()));
            if (!current.equals(condition.expected())) {
                return new WriteAnswer.Rejected(action, "the value differs from the one the write expected");
            }
            values.put(operation.target(), operation.value());
            frontier = new Frontier(new TreeSet<>(Set.of(action)));
            frontiers.add(frontier);
            changes.add(new Change(action, operation.target(), operation.value(), frontier));
            changed.signalAll();
            return new WriteAnswer.Unconfirmed(action);
        } finally {
            lock.unlock();
        }
    }

    @Override
    public ChangeFeed changes(Frontier since) {
        Objects.requireNonNull(since, "since");
        lock.lock();
        try {
            int position = frontiers.indexOf(since);
            if (position < 0) {
                throw new IllegalArgumentException("not a frontier this surface reported: " + since.heads());
            }
            return new Feed(position);
        } finally {
            lock.unlock();
        }
    }

    /** A feed over the surface's change list, with its own read position. */
    private final class Feed implements ChangeFeed {
        /** Index of the next change to return: changes up to {@code since} are skipped. */
        private int position;
        private boolean closed;

        Feed(int since) {
            this.position = since;
        }

        @Override
        public Optional<Change> next() throws InterruptedException {
            lock.lock();
            try {
                while (!closed && position >= changes.size()) {
                    changed.await();
                }
                if (closed) {
                    return Optional.empty();
                }
                return Optional.of(changes.get(position++));
            } finally {
                lock.unlock();
            }
        }

        @Override
        public void close() {
            lock.lock();
            try {
                closed = true;
                changed.signalAll();
            } finally {
                lock.unlock();
            }
        }
    }
}
