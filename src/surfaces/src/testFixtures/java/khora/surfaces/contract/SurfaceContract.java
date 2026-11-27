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
import khora.events.Emitted;
import khora.events.Action;
import khora.events.Assertion;
import khora.events.Basic;
import khora.events.Event;
import khora.events.EventId;
import khora.events.EventSource;
import khora.events.Identity;
import khora.surfaces.Gate;
import khora.surfaces.Surface;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.DynamicContainer;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;

/**
 * The contract every surface passes. A subclass gives the surface, its scenarios and, if it wants,
 * the gates they run with (by default none). Each scenario runs once through a fresh kernel and
 * becomes a container named after it, holding the test {@code expectation} (its {@link Then} holds)
 * and one test per surface rule, each judged over the deliveries to the surface under test:
 *
 * <ul>
 *   <li>{@value #MATCH}: each event it received matches one of its interests, came in log order,
 *       and came after its trigger was in the log;
 *   <li>{@value #OWN}: no event it received is one it emitted;
 *   <li>{@value #TRIGGERED}: the actions it returned for an event became events, in order, each
 *       naming that event as its trigger;
 *   <li>{@value #SAME}: the scenario is played a second time from scratch, with a new surface and
 *       gates, and the surface receives the same events and returns the same actions, compared
 *       symbolically.
 * </ul>
 *
 * <p>The events a surface emitted are those of the log by its name whose trigger is an event it
 * received. A scenario attaches the surface once: among its given steps
 * ({@link Given.AttachSurface}), after which later steps and a submitted {@code When} are delivered
 * live, or as its {@code When} ({@link When.Attach}), when it only catches up.
 *
 * <p>Rules {@value #MATCH}, {@value #OWN} and {@value #TRIGGERED} mostly judge the kernel: it decides
 * what a surface receives, keeps its own emissions from it, and links what it emits to their trigger.
 * What holds a surface to the right interests and the right answers are its scenarios, through their
 * {@code expectation}.
 *
 * <p>Emitted actions are compared symbolically, with the events of the run named by their steps. A
 * surface's emitted actions must therefore reference events only through the tuples of an assert and
 * the thing of a read: an event id inside a {@link ActionSpec.Raw} argument or a literal string is
 * compared as text, and ids differ between runs, so such a surface fails {@value #SAME} and its
 * expectation even when it is right.
 *
 * <p>Rule {@value #TRIGGERED} fails when a gate refuses what the surface emits, so run a surface
 * contract with gates that let through what the surface emits (the default, none, does).
 *
 * <p>A scenario the runner cannot play fails every test of its container with the runner's message.
 * A scenario in which the surface received nothing aborts the four rule tests rather than pass them
 * with nothing to judge.
 */
public abstract class SurfaceContract {

    public static final String MATCH = "receives only what its interests match, in log order";
    public static final String OWN = "never receives what it emitted";
    public static final String TRIGGERED = "what it emits is triggered by what it received";
    public static final String SAME = "the same events give the same actions";

    /** A new instance of the surface under test; called once per scenario run. */
    protected abstract Surface surface();

    /** The scenarios the surface is held to. Their names are distinct. */
    protected abstract List<Scenario> scenarios();

    /** New instances of the gates the scenarios run with; called once per scenario run. */
    protected List<Gate> gates() {
        return List.of();
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
                    rule(TRIGGERED, s, played, SurfaceContract::triggeredByWhatItReceived),
                    rule(SAME, s, played, (scenario, first) -> sameTwice(scenario, first, play(scenario)))));
        });
    }

    private Played play(Scenario s) {
        var surface = surface();
        return new Played(surface, ScenarioRunner.run(s, gates(), Optional.of(surface)));
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
            rule.check(s, played);
        });
    }

    /** The events of {@code run}'s log, by id. */
    private static EventSource source(Run run) {
        var byId = new HashMap<EventId, Event>();
        run.events().forEach(e -> byId.put(e.id(), e));
        return id -> Optional.ofNullable(byId.get(id));
    }

    /** The events the surface emitted, in log order, by the event they respond to. */
    private static Map<EventId, List<Event>> emittedByTrigger(Played p) {
        var received = new HashSet<EventId>();
        p.run().deliveries().forEach(d -> received.add(d.event().id()));
        var emitted = new HashMap<EventId, List<Event>>();
        for (var e : p.run().events()) {
            if (e.body() instanceof Emitted a && a.by().principal().equals(p.surface().name())) {
                var trigger = e.trigger();
                if (trigger.isPresent() && received.contains(trigger.get())) {
                    emitted.computeIfAbsent(trigger.get(), k -> new ArrayList<>()).add(e);
                }
            }
        }
        return emitted;
    }

    /** Delivery {@code index} of {@code s}, for messages. */
    private static String about(Scenario s, int index, Run run) {
        return s.name() + ": delivery " + index + " (" + render(run.deliveries().get(index).event().id(), run.bindings()) + ")";
    }

    static void receivesWhatMatches(Scenario s, Played p) {
        var run = p.run();
        var source = source(run);
        var order = new HashMap<EventId, Integer>();
        for (int i = 0; i < run.events().size(); i++) {
            order.put(run.events().get(i).id(), i);
        }
        int last = -1;
        for (int i = 0; i < run.deliveries().size(); i++) {
            var d = run.deliveries().get(i);
            var event = d.event();
            if (p.surface().interests().stream().noneMatch(interest -> interest.matches(event, source))) {
                fail(about(s, i, run) + ": matches none of its interests");
            }
            if (event.trigger().isPresent() && !d.logBefore().contains(event.trigger().get())) {
                fail(about(s, i, run) + ": received before its trigger " + render(event.trigger().get(), run.bindings())
                        + " was in the log");
            }
            int at = order.getOrDefault(event.id(), -1);
            if (at <= last) {
                fail(about(s, i, run) + ": received out of log order");
            }
            last = at;
        }
    }

    static void neverItsOwn(Scenario s, Played p) {
        var run = p.run();
        var own = new HashSet<EventId>();
        emittedByTrigger(p).values().forEach(es -> es.forEach(e -> own.add(e.id())));
        for (int i = 0; i < run.deliveries().size(); i++) {
            if (own.contains(run.deliveries().get(i).event().id())) {
                fail(about(s, i, run) + ": the surface received an event it emitted");
            }
        }
    }

    static void triggeredByWhatItReceived(Scenario s, Played p) {
        var run = p.run();
        var bindings = run.bindings();
        var byTrigger = emittedByTrigger(p);
        for (int i = 0; i < run.deliveries().size(); i++) {
            var d = run.deliveries().get(i);
            var events = byTrigger.getOrDefault(d.event().id(), List.of());
            assertEquals(symbolic(d.returned(), bindings),
                    symbolic(events.stream().map(e -> ((Emitted) e.body()).action()).toList(), bindings),
                    about(s, i, run) + ": the actions it returned, and the events they became");
        }
    }

    static void sameTwice(Scenario s, Played first, Played second) {
        assertEquals(deliveries(first.run()), deliveries(second.run()),
                s.name() + ": played again from scratch, the surface received or returned something else");
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
            GateContract.expect(s, run);
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
            return "assert " + tuples.get().stream().map(t -> GateContract.symbolic(t, bindings)).toList();
        }
        return action.definition().value() + " " + action.args().canonical();
    }

    private static String render(EventId id, Bindings bindings) {
        return bindings.render(new Identity(id.value()));
    }
}
