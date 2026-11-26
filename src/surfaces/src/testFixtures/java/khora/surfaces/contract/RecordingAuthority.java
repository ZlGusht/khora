package khora.surfaces.contract;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import khora.events.Event;
import khora.events.log.EventLog;
import khora.surfaces.Authority;

/**
 * Wraps an authority and records each of its decisions ({@link Run.Decision}). The inner authority
 * decides on a {@link ReadOnlyEventLog} over the log.
 *
 * <p>An inner authority that tries to append to, follow or close the log has
 * {@code appendedDuringDecide} set. If the {@link UnsupportedOperationException} the read-only log
 * throws leaves {@code decide}, that try counts as a rejection.
 */
public final class RecordingAuthority implements Authority {
    private final Authority inner;
    private final List<Run.Decision> decisions = new ArrayList<>();

    public RecordingAuthority(Authority inner) {
        this.inner = Objects.requireNonNull(inner, "inner");
    }

    /** Every decision so far, in order. */
    public synchronized List<Run.Decision> decisions() {
        return List.copyOf(decisions);
    }

    @Override
    public String name() {
        return inner.name();
    }

    @Override
    public synchronized Verdict decide(Event candidate, EventLog log) {
        var readOnly = new ReadOnlyEventLog(log);
        var verdict = decideOn(candidate, readOnly);
        decisions.add(new Run.Decision(candidate, verdict, readOnly.tried()));
        return verdict;
    }

    private Verdict decideOn(Event candidate, ReadOnlyEventLog log) {
        try {
            return Objects.requireNonNull(inner.decide(candidate, log), "the verdict of " + inner.name());
        } catch (UnsupportedOperationException e) {
            if (!log.tried()) {
                throw e;
            }
            return new Reject("authority " + inner.name() + " tried to change the log while deciding: " + e.getMessage());
        }
    }
}
