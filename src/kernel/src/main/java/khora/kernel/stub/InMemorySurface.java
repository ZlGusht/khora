package khora.kernel.stub;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
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
import khora.kernel.surface.Frontier;
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
 * <p>Its one origin is {@value #ORIGIN}. Its frontier is {@code {stub: n}} after n applied writes,
 * and empty before the first.
 */
public final class InMemorySurface implements WritableSurface, ChangingSurface {

    /** The name of the stub's one origin. */
    public static final String ORIGIN = "stub";

    private static final Declaration DECLARATION =
            new Declaration(true, Effects.NATIVE, History.NONE, Reads.DETERMINISTIC, Writes.CONDITIONAL);

    private final Actions actions;
    private final ReentrantLock lock = new ReentrantLock();
    private final java.util.concurrent.locks.Condition changed = lock.newCondition();
    private final List<Change> changes = new ArrayList<>();
    private final Map<String, String> values = new HashMap<>();
    private Frontier frontier = Frontier.empty();
    private long applied;

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
        var action = actions.record(context, "read");
        lock.lock();
        try {
            var value = Optional.ofNullable(values.get(request.target()));
            var wanted = request.time().at();
            Completeness completeness = wanted.isEmpty() || frontier.reaches(wanted.get())
                    ? new Completeness.Complete()
                    : new Completeness.Partial("frontier not reached");
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
        var action = actions.record(context, "write");
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
            applied++;
            frontier = frontier.advance(ORIGIN, applied);
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
        return new Feed(since.positions().getOrDefault(ORIGIN, 0L));
    }

    /** A feed over the surface's change list, with its own read position. */
    private final class Feed implements ChangeFeed {
        /** Index of the next change to return: changes up to {@code since} are skipped. */
        private long position;
        private boolean closed;

        Feed(long since) {
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
                return Optional.of(changes.get((int) position++));
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
