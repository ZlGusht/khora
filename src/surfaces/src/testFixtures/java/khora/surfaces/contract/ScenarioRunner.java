package khora.surfaces.contract;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;
import khora.events.Basic;
import khora.events.Emitted;
import khora.events.Event;
import khora.events.EventId;
import khora.events.Hlc;
import khora.events.Statement;
import khora.events.log.EventLog;
import khora.events.log.InMemoryEventLog;
import khora.surfaces.Answer;
import khora.surfaces.Context;
import khora.surfaces.Gate;
import khora.surfaces.Kernel;
import khora.surfaces.Surface;
import khora.surfaces.stub.Entities;

/**
 * Plays a scenario through a fresh {@link Kernel}: a new in-memory log, a clock fixed at 1000, the
 * given steps submitted in order (the surface under test attached where a {@link Given.AttachSurface}
 * says), then the {@code When}. Events no step made are numbered as emitted, in log order.
 *
 * <p>A scenario that reads gets the reference reader, {@link Entities}, attached before anything;
 * its answers are the read's statements.
 */
public final class ScenarioRunner {
    /** The event {@link Step#UNKNOWN} stands for: one no log of a run holds. */
    private static final EventId UNKNOWN = EventId.hashOf("khora/contract/unknown");

    /** Who a {@link When.Read} is by. */
    static final Context READER = new Context("contract-reader", Optional.empty(), "contract", 0);

    private static final String ENTITIES = new Entities().name();

    private ScenarioRunner() {}

    /**
     * Runs {@code s} with {@code gates}, and {@code surface} if the scenario attaches one. Each gate is
     * wrapped in a {@link RecordingGate}, so the run holds every decision, including those on actions
     * a surface emitted; the surface in a fresh {@link RecordingSurface}, so it holds every delivery
     * to it.
     *
     * @throws IllegalArgumentException if the scenario repeats a step name, names a step that is not
     *     an earlier one, attaches with no surface given, attaches the surface more than once, or has a
     *     step by the surface's name
     * @throws IllegalStateException if a given step is refused
     */
    public static Run run(Scenario s, List<Gate> gates, Optional<Surface> surface) {
        check(s, surface);
        var log = new InMemoryEventLog();
        var decisions = new ArrayList<Run.Decision>();
        var kernel = new Kernel(log, new Hlc(() -> 1000),
                gates.stream().<Gate>map(g -> new RecordingGate(g, decisions)).toList());
        if (reads(s)) {
            kernel.attach(new Entities(), 0);
        }
        var recordingSurface = surface.map(it -> new RecordingSurface(it, log));
        var bindings = new Bindings();
        refs(s).filter(Ref.Named.class::isInstance).forEach(ref -> bindings.identity(ref, Optional.empty()));
        for (var given : s.given()) {
            switch (given) {
                case Given.AttachSurface attach ->
                        kernel.attach(recordingSurface.orElseThrow(), position(log, bindings, attach.from()));
                case Step step -> {
                    switch (submit(kernel, bindings, step)) {
                        case Answer.Logged a -> bindings.bind(step.name(), a.event().id());
                        case Answer.Refused r -> throw new IllegalStateException("scenario " + s.name()
                                + ": given step " + step.name() + " was refused: " + r.reason());
                    }
                }
            }
        }
        Optional<Answer> answer = Optional.empty();
        List<Statement> statements = List.of();
        switch (s.when()) {
            case When.Submit submit -> {
                var answered = submit(kernel, bindings, submit.step());
                answer = Optional.of(answered);
                if (answered instanceof Answer.Logged a) {
                    bindings.bind(submit.step().name(), a.event().id());
                    statements = statements(kernel, a);
                }
            }
            case When.Attach attach -> kernel.attach(recordingSurface.orElseThrow(), position(log, bindings, attach.from()));
            case When.Read read -> {
                var answered = kernel.submit(Basic.read(bindings.identity(read.thing(), Optional.empty())), READER);
                statements = statements(kernel, (Answer.Logged) answered);
            }
        }
        var bound = new HashSet<EventId>();
        s.steps().forEach(step -> bound.add(bindings.event(step.name())));
        if (s.when() instanceof When.Submit submit && answer.orElseThrow() instanceof Answer.Logged) {
            bound.add(bindings.event(submit.step().name()));
        }
        for (var event : events(log)) {
            if (!bound.contains(event.id())) {
                bindings.emitted(event.id());
            }
        }
        return new Run(bindings, answer, statements, decisions,
                recordingSurface.map(RecordingSurface::deliveries).orElse(List.of()), events(log));
    }

    /** Whether the scenario reads, and so needs the reference reader. */
    private static boolean reads(Scenario s) {
        return s.when() instanceof When.Read || s.then() instanceof Then.Statements;
    }

    /** The statements the reference reader answered {@code read} with. */
    private static List<Statement> statements(Kernel kernel, Answer.Logged read) {
        return read.responses().stream()
                .filter(e -> ((Emitted) e.body()).by().principal().equals(ENTITIES))
                .flatMap(e -> Basic.answerOf(((Emitted) e.body()).action()).orElse(List.of()).stream())
                .map(id -> kernel.statement(id).orElseThrow())
                .toList();
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
            case Then.Logged ignored -> Stream.<Ref>empty();
            case Then.Refused ignored -> Stream.<Ref>empty();
        };
        return Stream.of(steps, when, then).flatMap(it -> it);
    }

    private static Answer submit(Kernel kernel, Bindings bindings, Step step) {
        return kernel.submit(step.action().resolve(bindings, Optional.empty()), step.who(),
                step.trigger().map(n -> n.equals(Step.UNKNOWN) ? UNKNOWN : bindings.event(n)));
    }

    /** The log position of the latest of the named steps; 0 for none. */
    private static long position(EventLog log, Bindings bindings, List<String> names) {
        var ids = new HashSet<EventId>();
        names.forEach(n -> ids.add(bindings.event(n)));
        long position = 0;
        for (var logged : log.read(1, Integer.MAX_VALUE)) {
            if (ids.contains(logged.event().id())) {
                position = logged.position();
            }
        }
        return position;
    }

    /** The events of {@code log}, in log order. */
    private static List<Event> events(EventLog log) {
        var events = new ArrayList<Event>();
        for (long position = 1; ; ) {
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
     * other than once with one given, or that has a step by the surface's or the reader's name: the
     * events a surface emitted are those by its name, so a step by that name would pass for one.
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
        if (surface.isPresent() && reads(s) && surface.get().name().equals(ENTITIES)) {
            throw new IllegalArgumentException("scenario " + s.name() + " reads, and the surface under test"
                    + " has the reference reader's name, " + ENTITIES);
        }
        if (s.when() instanceof When.Submit submit) {
            steps.add(submit.step());
        }
        var reserved = new HashMap<String, String>();
        surface.ifPresent(it -> reserved.put(it.name(), "the name of the surface under test"));
        if (reads(s)) {
            reserved.put(ENTITIES, "the name of the reference reader");
        }
        for (var step : steps) {
            var why = reserved.get(step.who().principal());
            if (why != null) {
                throw new IllegalArgumentException("scenario " + s.name() + ": step " + step.name() + " is by "
                        + step.who().principal() + ", " + why + ", by which its emissions are known");
            }
            step.trigger().ifPresent(name -> checkEarlier(s, step.name(), name, earlier));
            if (step.name().equals(Step.UNKNOWN) || !earlier.add(step.name())) {
                throw new IllegalArgumentException("scenario " + s.name() + ": step " + step.name()
                        + (step.name().equals(Step.UNKNOWN) ? " takes the reserved name" : " repeats a step name"));
            }
        }
        var given = new HashSet<String>();
        s.steps().forEach(step -> given.add(step.name()));
        if (s.when() instanceof When.Attach attach) {
            attach.from().forEach(name -> checkEarlier(s, "attach", name, given));
        }
    }

    private static void checkEarlier(Scenario s, String step, String name, Set<String> earlier) {
        if (!name.equals(Step.UNKNOWN) && !earlier.contains(name)) {
            throw new IllegalArgumentException("scenario " + s.name() + ": step " + step + " names " + name
                    + ", which is no earlier step");
        }
    }
}
