package khora.surfaces.contract;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Stream;
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
 * given steps submitted in order (the surface under test attached where a {@link Given.AttachSurface}
 * says), then the {@code When}. Events no step made are numbered as emitted, in log order.
 */
public final class ScenarioRunner {
    /** The event {@link Step#UNKNOWN} stands for: one no log of a run holds. */
    private static final EventId UNKNOWN = EventId.hashOf("khora/contract/unknown");

    private ScenarioRunner() {}

    /**
     * Runs {@code s} with {@code authority}, and {@code surface} if the scenario attaches one. The
     * authority is wrapped in a fresh {@link RecordingAuthority}, so the run holds every decision,
     * including those on actions a surface emitted; the surface in a fresh {@link RecordingSurface}, so
     * it holds every delivery to it.
     *
     * @throws IllegalArgumentException if the scenario repeats a step name, names a step that is not
     *     an earlier one, attaches with no surface given, attaches the surface more than once, or has a
     *     step by the surface's name
     * @throws IllegalStateException if a given step is rejected
     */
    public static Run run(Scenario s, Authority authority, Optional<Surface> surface) {
        check(s, surface);
        var log = new InMemoryEventLog();
        var recording = new RecordingAuthority(authority);
        var kernel = new Kernel(recording, log, new Hlc(() -> 1000));
        var recordingSurface = surface.map(it -> new RecordingSurface(it, log));
        var bindings = new Bindings();
        refs(s).filter(Ref.Named.class::isInstance).forEach(ref -> bindings.identity(ref, Optional.empty()));
        for (var given : s.given()) {
            switch (given) {
                case Given.AttachSurface attach ->
                        kernel.attach(recordingSurface.orElseThrow(), frontier(log, bindings, attach.from()));
                case Step step -> {
                    switch (submit(kernel, log, bindings, step)) {
                        case Answer.Accepted a -> bindings.bind(step.name(), a.event().id());
                        case Answer.Rejected r -> throw new IllegalStateException("scenario " + s.name()
                                + ": given step " + step.name() + " was rejected: " + r.reason());
                    }
                }
            }
        }
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
            case When.Attach attach -> kernel.attach(recordingSurface.orElseThrow(), frontier(log, bindings, attach.from()));
            case When.Read read -> statements = kernel.statements(bindings.identity(read.thing(), Optional.empty()),
                    frontier(log, bindings, read.at()));
        }
        var bound = new HashSet<EventId>();
        s.steps().forEach(step -> bound.add(bindings.event(step.name())));
        if (s.when() instanceof When.Submit submit && answer.orElseThrow() instanceof Answer.Accepted) {
            bound.add(bindings.event(submit.step().name()));
        }
        for (var event : events(log, 1)) {
            if (!bound.contains(event.id())) {
                bindings.emitted(event.id());
            }
        }
        return new Run(bindings, answer, statements, recording.decisions(),
                recordingSurface.map(RecordingSurface::deliveries).orElse(List.of()), events(log, 1));
    }

    /**
     * Every ref the scenario writes, its {@code Then} included. The named ones are resolved before the
     * run, so every run of a scenario renders the same names, whatever was resolved after it.
     */
    private static Stream<Ref> refs(Scenario s) {
        var steps = s.steps().stream().map(Step::action).flatMap(ActionSpec::refs);
        var when = switch (s.when()) {
            case When.Submit submit -> submit.step().action().refs();
            case When.Read read -> Stream.of(read.thing());
            case When.Attach ignored -> Stream.<Ref>empty();
        };
        var then = switch (s.then()) {
            case Then.Statements statements -> statements.tuples().stream().flatMap(TupleSpec::refs);
            case Then.Emits emits -> emits.groups().stream().flatMap(g -> g.actions().stream()).flatMap(ActionSpec::refs);
            case Then.Accepted ignored -> Stream.<Ref>empty();
            case Then.Rejected ignored -> Stream.<Ref>empty();
        };
        return Stream.of(steps, when, then).flatMap(it -> it);
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

    /**
     * Refuses a scenario whose step names repeat or point to no earlier step, that attaches the surface
     * other than once with one given, or that has a step by the surface's name: the events a surface
     * emitted are those by its name, so a step by that name would pass for one.
     */
    private static void check(Scenario s, Optional<Surface> surface) {
        var earlier = new HashSet<String>();
        var steps = new ArrayList<Step>();
        var attaches = 0;
        for (var given : s.given()) {
            switch (given) {
                case Step step -> steps.add(step);
                case Given.AttachSurface attach -> {
                    attaches++;
                    var before = new HashSet<String>();
                    steps.forEach(step -> before.add(step.name()));
                    attach.from().forEach(name -> checkEarlier(s, "attach", name, before));
                }
            }
        }
        if (s.when() instanceof When.Attach) {
            attaches++;
        }
        if (attaches > 1) {
            throw new IllegalArgumentException("scenario " + s.name() + " attaches the surface " + attaches
                    + " times; it attaches it at most once");
        }
        if (attaches > 0 && surface.isEmpty()) {
            throw new IllegalArgumentException("scenario " + s.name() + " attaches a surface, but none is given");
        }
        if (s.when() instanceof When.Submit submit) {
            steps.add(submit.step());
        }
        for (var step : steps) {
            if (surface.isPresent() && step.who().principal().equals(surface.get().name())) {
                throw new IllegalArgumentException("scenario " + s.name() + ": step " + step.name() + " is by "
                        + step.who().principal() + ", the name of the surface under test, by which its"
                        + " emissions are known");
            }
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
        s.steps().forEach(step -> given.add(step.name()));
        switch (s.when()) {
            case When.Submit ignored -> { }
            case When.Attach attach -> attach.from().forEach(name -> checkEarlier(s, "attach", name, given));
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
