package khora.surfaces.contract;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;
import khora.events.AcceptedAction;
import khora.events.Action;
import khora.events.Assertion;
import khora.events.Basic;
import khora.events.Event;
import khora.events.EventId;
import khora.events.EventSource;
import khora.events.FrontierCheck;
import khora.events.Identity;
import khora.surfaces.Authority;
import khora.surfaces.Surface;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.DynamicContainer;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;

/**
 * The contract every surface passes. A subclass gives the surface, its scenarios and, if it wants,
 * the authority (by default {@link AcceptAll}). Each scenario runs once through a fresh kernel and
 * becomes a container named after it, holding the test {@code expectation} (its {@link Then} holds)
 * and one test per surface rule, each judged over the deliveries to the surface under test:
 *
 * <ul>
 *   <li>{@value #MATCH}: each event it received matches one of its interests, and came after those
 *       of its causes it also received; every cause was in the log;
 *   <li>{@value #OWN}: no event it received is one it emitted;
 *   <li>{@value #CAUSED}: the actions it returned for an event became events, in order, linked to
 *       that event as {@code source}, knowing every event it had received so far;
 *   <li>{@value #SAME}: the scenario is played a second time from scratch, with a new surface and
 *       authority, and the surface receives the same events and returns the same actions, compared
 *       symbolically;
 *   <li>{@value #ORDERED}: each event it emitted knew the one it emitted before.
 * </ul>
 *
 * <p>The events a surface emitted are those of the log by its name whose {@code source} is an event
 * it received. A scenario attaches the surface once: among its given steps
 * ({@link Given.AttachSurface}), after which later steps and a submitted {@code When} are delivered
 * live, or as its {@code When} ({@link When.Attach}), when it only catches up.
 *
 * <p>Rules {@value #MATCH}, {@value #OWN} and {@value #CAUSED} mostly judge the kernel: it decides
 * what a surface receives, keeps its own emissions from it, and links what it emits to their cause.
 * What holds a surface to the right interests and the right answers are its scenarios, through their
 * {@code expectation}.
 *
 * <p>Emitted actions are compared symbolically, with the events of the run named by their steps. A
 * surface's emitted actions must therefore reference events only through the tuples of an assert and
 * the thing of a read: an event id inside a {@link ActionSpec.Raw} argument or a literal string is
 * compared as text, and ids differ between runs, so such a surface fails {@value #SAME} and its
 * expectation even when it is right.
 *
 * <p>Rule {@value #CAUSED} fails when the contract's authority rejects what the surface emits, so
 * run a surface contract with an authority that accepts what the surface emits (the default
 * {@link AcceptAll} does).
 *
 * <p>A scenario the runner cannot play fails every test of its container with the runner's message.
 * A scenario in which the surface received nothing aborts the five rule tests rather than pass them
 * with nothing to judge; {@value #ORDERED} also aborts when the surface emitted fewer than two
 * events. Rules listed in {@link KnownGaps} are judged by it.
 */
public abstract class SurfaceContract {

    public static final String MATCH = "receives only what its interests match, after its causes";
    public static final String OWN = "never receives what it emitted";
    public static final String CAUSED = "what it emits is caused by what it received";
    public static final String SAME = "the same events give the same actions";
    public static final String ORDERED = "its successive emissions are causally ordered";

    /** A new instance of the surface under test; called once per scenario run. */
    protected abstract Surface surface();

    /** The scenarios the surface is held to. Their names are distinct. */
    protected abstract List<Scenario> scenarios();

    /** A new instance of the authority the scenarios run with; called once per scenario run. */
    protected Authority authority() {
        return new AcceptAll();
    }

    /** A run, and the surface it was played with. */
    private record Played(Surface surface, Run run) {}

    /**
     * One container per scenario.
     *
     * @throws IllegalArgumentException if two scenarios share a name
     */
    @TestFactory
    public final Stream<DynamicContainer> contract() {
        var scenarios = scenarios();
        var names = new HashSet<String>();
        for (var s : scenarios) {
            if (!names.add(s.name())) {
                throw new IllegalArgumentException("two scenarios are named " + s.name());
            }
        }
        return scenarios.stream().map(s -> {
            var played = new Once<>(() -> play(s));
            return DynamicContainer.dynamicContainer(s.name(), Stream.of(
                    DynamicTest.dynamicTest("expectation", () -> expect(s, played.get().run())),
                    rule(MATCH, s, played, SurfaceContract::receivesWhatMatches),
                    rule(OWN, s, played, SurfaceContract::neverItsOwn),
                    rule(CAUSED, s, played, SurfaceContract::causedByWhatItReceived),
                    rule(SAME, s, played, (scenario, first) -> sameTwice(scenario, first, play(scenario))),
                    rule(ORDERED, s, played, SurfaceContract::emissionsOrdered)));
        });
    }

    private Played play(Scenario s) {
        var surface = surface();
        return new Played(surface, ScenarioRunner.run(s, authority(), Optional.of(surface)));
    }

    private interface Rule {
        void check(Scenario s, Played played);
    }

    private static DynamicTest rule(String name, Scenario s, Once<Played> once, Rule rule) {
        return DynamicTest.dynamicTest(name, () -> {
            var played = once.get();
            if (played.run().deliveries().isEmpty()) {
                Assumptions.abort("the surface received nothing in this scenario");
            }
            KnownGaps.judge(name, () -> rule.check(s, played));
        });
    }

    /** The events of {@code run}'s log, by id. */
    private static EventSource source(Run run) {
        var byId = new HashMap<EventId, Event>();
        run.events().forEach(e -> byId.put(e.id(), e));
        return id -> Optional.ofNullable(byId.get(id));
    }

    /** The events the surface emitted, in log order, by the event they respond to. */
    private static Map<EventId, List<Event>> emittedBySource(Played p) {
        var received = new HashSet<EventId>();
        p.run().deliveries().forEach(d -> received.add(d.event().id()));
        var emitted = new HashMap<EventId, List<Event>>();
        for (var e : p.run().events()) {
            if (e.body() instanceof AcceptedAction a && a.by().principal().equals(p.surface().name())) {
                var source = a.links().getOrDefault("source", Set.of());
                if (source.size() == 1 && received.contains(source.iterator().next())) {
                    emitted.computeIfAbsent(source.iterator().next(), k -> new ArrayList<>()).add(e);
                }
            }
        }
        return emitted;
    }

    private static List<Event> emitted(Played p) {
        var bySource = emittedBySource(p);
        var ids = new HashSet<EventId>();
        bySource.values().forEach(es -> es.forEach(e -> ids.add(e.id())));
        return p.run().events().stream().filter(e -> ids.contains(e.id())).toList();
    }

    /** Delivery {@code index} of {@code s}, for messages. */
    private static String about(Scenario s, int index, Run run) {
        return s.name() + ": delivery " + index + " (" + render(run.deliveries().get(index).event().id(), run.bindings()) + ")";
    }

    static void receivesWhatMatches(Scenario s, Played p) {
        var run = p.run();
        var source = source(run);
        var earlier = new HashSet<EventId>();
        var received = new HashSet<EventId>();
        run.deliveries().forEach(d -> received.add(d.event().id()));
        for (int i = 0; i < run.deliveries().size(); i++) {
            var d = run.deliveries().get(i);
            var event = d.event();
            if (p.surface().interests().stream().noneMatch(interest -> interest.matches(event, source))) {
                fail(about(s, i, run) + ": matches none of its interests");
            }
            for (var cause : event.causes()) {
                if (!d.logBefore().contains(cause)) {
                    fail(about(s, i, run) + ": received before its cause " + render(cause, run.bindings())
                            + " was in the log");
                }
                if (received.contains(cause) && !earlier.contains(cause)) {
                    fail(about(s, i, run) + ": received before its cause " + render(cause, run.bindings()));
                }
            }
            earlier.add(event.id());
        }
    }

    static void neverItsOwn(Scenario s, Played p) {
        var run = p.run();
        var own = new HashSet<EventId>();
        emitted(p).forEach(e -> own.add(e.id()));
        for (int i = 0; i < run.deliveries().size(); i++) {
            if (own.contains(run.deliveries().get(i).event().id())) {
                fail(about(s, i, run) + ": the surface received an event it emitted");
            }
        }
    }

    static void causedByWhatItReceived(Scenario s, Played p) {
        var run = p.run();
        var bindings = run.bindings();
        var source = source(run);
        var bySource = emittedBySource(p);
        var received = new HashSet<EventId>();
        for (int i = 0; i < run.deliveries().size(); i++) {
            var d = run.deliveries().get(i);
            received.add(d.event().id());
            var events = bySource.getOrDefault(d.event().id(), List.of());
            assertEquals(symbolic(d.returned(), bindings),
                    symbolic(events.stream().map(e -> ((AcceptedAction) e.body()).action()).toList(), bindings),
                    about(s, i, run) + ": the actions it returned, and the events they became");
            for (var e : events) {
                var knew = FrontierCheck.closure(e.causes(), source);
                assertTrue(e.causes().contains(d.event().id()),
                        about(s, i, run) + ": " + render(e.id(), bindings) + " is not caused by the event it responds to");
                for (var r : received) {
                    assertTrue(knew.contains(r), about(s, i, run) + ": " + render(e.id(), bindings)
                            + " does not know " + render(r, bindings) + ", which the surface had received");
                }
            }
        }
    }

    static void sameTwice(Scenario s, Played first, Played second) {
        assertEquals(deliveries(first.run()), deliveries(second.run()),
                s.name() + ": played again from scratch, the surface received or returned something else");
    }

    static void emissionsOrdered(Scenario s, Played p) {
        var emitted = emitted(p);
        if (emitted.size() < 2) {
            Assumptions.abort("the surface emitted fewer than two events in this scenario");
        }
        var source = source(p.run());
        var bindings = p.run().bindings();
        for (int i = 1; i < emitted.size(); i++) {
            var before = emitted.get(i - 1);
            var after = emitted.get(i);
            assertTrue(FrontierCheck.closure(after.causes(), source).contains(before.id()), s.name() + ": "
                    + render(after.id(), bindings) + " does not know " + render(before.id(), bindings)
                    + ", which the surface emitted before it");
        }
    }

    /** Each delivery as the event received and the actions returned, symbolically. */
    private static List<String> deliveries(Run run) {
        return run.deliveries().stream()
                .map(d -> render(d.event().id(), run.bindings()) + " -> " + symbolic(d.returned(), run.bindings()))
                .toList();
    }

    /**
     * Checks that the run of {@code s} meets its {@link Then}. For {@link Then.Emits}, delivery by
     * delivery from the moment the surface was attached (catching up, then live), the step whose event
     * it received and the actions it returned are compared symbolically to the expected groups, each
     * resolved without a received event. Deliveries that returned nothing are compared only if a group
     * names their step.
     */
    static void expect(Scenario s, Run run) {
        if (!(s.then() instanceof Then.Emits emits)) {
            AuthorityContract.expect(s, run);
            return;
        }
        var steps = new HashSet<String>();
        s.steps().forEach(step -> steps.add(step.name()));
        if (s.when() instanceof When.Submit submit) {
            steps.add(submit.step().name());
        }
        var bindings = run.bindings();
        var listed = new HashSet<String>();
        var expected = new ArrayList<String>();
        for (var group : emits.groups()) {
            if (!steps.contains(group.step())) {
                fail(s.name() + ": expects emissions after " + group.step() + ", which is no step of the scenario");
            }
            if (group.actions().stream().flatMap(ActionSpec::refs).anyMatch(Ref.Received.class::isInstance)) {
                fail(s.name() + ": the emissions expected after " + group.step() + " name Ref.received; an"
                        + " expectation is resolved without a received event, so it names the step's tuples"
                        + " (Ref.tuple) instead");
            }
            listed.add(group.step());
            expected.add(group.step() + " -> " + symbolic(
                    group.actions().stream().map(a -> a.resolve(bindings, Optional.empty())).toList(), bindings));
        }
        var actual = new ArrayList<String>();
        for (var d : run.deliveries()) {
            var step = render(d.event().id(), bindings);
            if (!d.returned().isEmpty() || listed.contains(step)) {
                actual.add(step + " -> " + symbolic(d.returned(), bindings));
            }
        }
        assertEquals(expected, actual, s.name() + ": what the surface returned, delivery by delivery");
    }

    /** Actions as symbolic text: asserts by their tuples, reads by their thing, others as JSON. */
    public static List<String> symbolic(List<Action> actions, Bindings bindings) {
        return actions.stream().map(a -> symbolic(a, bindings)).toList();
    }

    private static String symbolic(Action action, Bindings bindings) {
        var read = Basic.readOf(action);
        if (read.isPresent()) {
            return "read " + bindings.render(read.get());
        }
        var tuples = Assertion.in(action);
        if (tuples.isPresent()) {
            return "assert " + tuples.get().stream().map(t -> AuthorityContract.symbolic(t, bindings)).toList();
        }
        return action.definition().value() + " " + action.args().canonical();
    }

    private static String render(EventId id, Bindings bindings) {
        return bindings.render(new Identity(id.value()));
    }
}
