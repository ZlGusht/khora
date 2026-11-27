package khora.surfaces.contract;

import java.util.List;
import java.util.Objects;
import khora.events.Action;
import khora.events.By;
import khora.events.Event;
import khora.surfaces.Gate;

/** Wraps a gate and records each of its decisions ({@link Run.Decision}) in {@code decisions}. */
public final class RecordingGate implements Gate {
    private final Gate inner;
    private final List<Run.Decision> decisions;

    public RecordingGate(Gate inner, List<Run.Decision> decisions) {
        this.inner = Objects.requireNonNull(inner, "inner");
        this.decisions = Objects.requireNonNull(decisions, "decisions");
    }

    @Override
    public String name() {
        return inner.name();
    }

    @Override
    public boolean guards(Action action) {
        return inner.guards(action);
    }

    @Override
    public Verdict decide(Action action, By by) {
        var verdict = Objects.requireNonNull(inner.decide(action, by), "the verdict of " + inner.name());
        decisions.add(new Run.Decision(action, by, verdict));
        return verdict;
    }

    @Override
    public void take(Event event) {
        inner.take(event);
    }
}
