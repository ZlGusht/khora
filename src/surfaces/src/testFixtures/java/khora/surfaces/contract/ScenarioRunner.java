package khora.surfaces.contract;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import khora.events.Event;
import khora.events.EventId;
import khora.events.Frontier;
import khora.events.Hlc;
import khora.events.Statement;
import khora.events.log.EventLog;
import khora.events.log.InMemoryEventLog;
import khora.surfaces.Answer;
import khora.surfaces.Authority;
import khora.surfaces.Kernel;
import khora.surfaces.Surface;

/**
 * Plays a scenario through a fresh {@link Kernel}: a new in-memory log, a clock fixed at 1000, the
 * given steps submitted in order, then the {@code When}.
 */
public final class ScenarioRunner {
    /** The event {@link Step#UNKNOWN} stands for: one no log of a run holds. */
    private static final EventId UNKNOWN = EventId.hashOf("khora/contract/unknown");

    private ScenarioRunner() {}

    /**
     * Runs {@code s} with {@code authority}, and {@code surface} if the scenario attaches one. The
     * run's decisions are recorded only if {@code authority} is a {@link RecordingAuthority}; see
     * {@link #recorded}.
     *
     * @throws IllegalArgumentException if the scenario repeats a step name, names a step that is not
     *     an earlier one, or attaches with no surface given
     * @throws IllegalStateException if a given step is rejected
     */
    public static Run run(Scenario s, Authority authority, Optional<Surface> surface) {
        check(s, surface);
        var log = new InMemoryEventLog();
        var kernel = new Kernel(authority, log, new Hlc(() -> 1000));
        var bindings = new Bindings();
        for (var step : s.given()) {
            switch (submit(kernel, log, bindings, step)) {
                case Answer.Accepted a -> bindings.bind(step.name(), a.event().id());
                case Answer.Rejected r -> throw new IllegalStateException(
                        "scenario " + s.name() + ": given step " + step.name() + " was rejected: " + r.reason());
            }
        }
        long after = events(log, 1).size();
        Optional<Answer> answer = Optional.empty();
        List<Statement> statements = List.of();
        switch (s.when()) {
            case When.Submit submit -> {
                var answered = submit(kernel, log, bindings, submit.step());
                answer = Optional.of(answered);
                if (answered instanceof Answer.Accepted a) {
                    bindings.bind(submit.step().name(), a.event().id());
                    statements = a.statements();
                }
            }
            case When.Attach attach -> kernel.attach(surface.orElseThrow(), frontier(log, bindings, attach.from()));
            case When.Read read -> statements = kernel.statements(bindings.identity(read.thing(), Optional.empty()),
                    frontier(log, bindings, read.at()));
        }
        var bound = new HashSet<EventId>();
        s.given().forEach(step -> bound.add(bindings.event(step.name())));
        if (s.when() instanceof When.Submit submit && answer.orElseThrow() instanceof Answer.Accepted) {
            bound.add(bindings.event(submit.step().name()));
        }
        for (var event : events(log, after + 1)) {
            if (!bound.contains(event.id())) {
                bindings.emitted(event.id());
            }
        }
        var decisions = authority instanceof RecordingAuthority recording ? recording.decisions() : List.<Run.Decision>of();
        return new Run(bindings, answer, statements, decisions, List.of());
    }

    /**
     * Runs {@code s} as {@link #run} does, with {@code authority} wrapped in a fresh
     * {@link RecordingAuthority}: the run holds every decision, including those on actions a surface
     * emitted.
     */
    public static Run recorded(Scenario s, Authority authority, Optional<Surface> surface) {
        return run(s, new RecordingAuthority(authority), surface);
    }

    private static Answer submit(Kernel kernel, EventLog log, Bindings bindings, Step step) {
        var links = new HashMap<String, Set<EventId>>();
        step.links().forEach((name, steps) -> links.put(name, new TreeSet<>(ids(bindings, steps))));
        return kernel.submit(step.action().resolve(bindings, Optional.empty()), step.who(),
                frontier(log, bindings, step.knew()), links);
    }

    /** The frontier of the named steps, reduced to its heads; an unknown event stays a head. */
    private static Frontier frontier(EventLog log, Bindings bindings, List<String> names) {
        var known = new ArrayList<EventId>();
        var unknown = new TreeSet<EventId>();
        for (var id : ids(bindings, names)) {
            (log.get(id).isPresent() ? known : unknown).add(id);
        }
        var heads = new TreeSet<EventId>(Frontier.of(known, log).heads());
        heads.addAll(unknown);
        return new Frontier(heads);
    }

    private static List<EventId> ids(Bindings bindings, List<String> names) {
        return names.stream().map(n -> n.equals(Step.UNKNOWN) ? UNKNOWN : bindings.event(n)).toList();
    }

    /** The events of {@code log} from position {@code from}, in log order. */
    private static List<Event> events(EventLog log, long from) {
        var events = new ArrayList<Event>();
        for (long position = from; ; ) {
            var page = log.read(position, EventLog.MAX_APPEND);
            if (page.isEmpty()) {
                return events;
            }
            page.forEach(logged -> events.add(logged.event()));
            position = page.getLast().position() + 1;
        }
    }

    /** Refuses a scenario whose step names repeat or point to no earlier step. */
    private static void check(Scenario s, Optional<Surface> surface) {
        var earlier = new HashSet<String>();
        var steps = new ArrayList<>(s.given());
        if (s.when() instanceof When.Submit submit) {
            steps.add(submit.step());
        }
        for (var step : steps) {
            for (var name : step.knew()) {
                checkEarlier(s, step.name(), name, earlier);
            }
            for (Map.Entry<String, List<String>> link : step.links().entrySet()) {
                for (var name : link.getValue()) {
                    checkEarlier(s, step.name(), name, earlier);
                }
            }
            if (step.name().equals(Step.UNKNOWN) || !earlier.add(step.name())) {
                throw new IllegalArgumentException("scenario " + s.name() + ": step " + step.name()
                        + (step.name().equals(Step.UNKNOWN) ? " takes the reserved name" : " repeats a step name"));
            }
        }
        var given = new HashSet<String>();
        s.given().forEach(step -> given.add(step.name()));
        switch (s.when()) {
            case When.Submit ignored -> { }
            case When.Attach attach -> {
                attach.from().forEach(name -> checkEarlier(s, "attach", name, given));
                if (surface.isEmpty()) {
                    throw new IllegalArgumentException("scenario " + s.name() + " attaches a surface, but none is given");
                }
            }
            case When.Read read -> read.at().forEach(name -> checkEarlier(s, "read", name, given));
        }
    }

    private static void checkEarlier(Scenario s, String step, String name, Set<String> earlier) {
        if (!name.equals(Step.UNKNOWN) && !earlier.contains(name)) {
            throw new IllegalArgumentException("scenario " + s.name() + ": step " + step + " names " + name
                    + ", which is no earlier step");
        }
    }
}
