package khora.surfaces.contract;

import static khora.surfaces.contract.GateContract.READ;
import static khora.surfaces.contract.GateContract.REASON;
import static khora.surfaces.contract.GateContract.SAME;
import static khora.surfaces.contract.ProjectsContractTest.IVAN;
import static khora.surfaces.contract.ProjectsContractTest.KITCHEN;
import static khora.surfaces.contract.ProjectsContractTest.MIA;
import static khora.surfaces.contract.ProjectsContractTest.create;
import static khora.surfaces.contract.ProjectsContractTest.read;
import static khora.surfaces.contract.ProjectsContractTest.rename;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;
import khora.events.Action;
import khora.events.Assertion;
import khora.events.Basic;
import khora.events.By;
import khora.events.Event;
import khora.events.Identity;
import khora.events.json.Json;
import khora.surfaces.Gate;
import khora.surfaces.Interest;
import khora.surfaces.Surface;
import khora.surfaces.stub.Projects;
import org.junit.jupiter.api.DynamicNode;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.Test;
import org.opentest4j.TestAbortedException;

/** Each gate rule fails for a gate that breaks it, and only for that one. */
class GateRulesTest {
    static final List<String> RULES = List.of(REASON, READ, SAME);

    /** A creation, then a rename of its name. */
    static final Scenario RENAME = Scenario.named("rename")
            .given(create())
            .when(new When.Submit(rename("rename", IVAN, KITCHEN, "Studio")))
            .then(new Then.Logged());

    /** A creation, then a read of it. */
    static final Scenario READING = Scenario.named("reading")
            .given(create())
            .when(new When.Submit(read("read", IVAN)))
            .then(new Then.Logged());

    static boolean isRename(Action action) {
        return Assertion.in(action).orElse(List.of()).stream()
                .anyMatch(t -> t.attribute().equals(Optional.of(Basic.RETRACTED)));
    }

    /** Projects, except that it also guards reads, and decides as {@code decide} says; null leaves it to Projects. */
    interface Misbehaviour {
        Gate.Verdict decide(Action action);
    }

    static Gate projectsBut(Misbehaviour misbehaviour) {
        return new Gate() {
            final Projects projects = new Projects();

            @Override
            public String name() {
                return projects.name();
            }

            @Override
            public boolean guards(Action action) {
                return projects.guards(action) || Basic.readOf(action).isPresent();
            }

            @Override
            public Verdict decide(Action action, By by) {
                var verdict = misbehaviour.decide(action);
                return verdict != null ? verdict : projects.decide(action, by);
            }

            @Override
            public void take(Event event) {
                projects.take(event);
            }
        };
    }

    static GateContract contract(Supplier<Gate> gate, Scenario... scenarios) {
        return new GateContract() {
            @Override
            protected Gate gate() {
                return gate.get();
            }

            @Override
            protected List<Scenario> scenarios() {
                return List.of(scenarios);
            }
        };
    }

    /** Runs every test of the contract: per container, per test, what it threw, or null. */
    static Map<String, Map<String, Throwable>> results(GateContract contract) {
        var results = new LinkedHashMap<String, Map<String, Throwable>>();
        contract.contract().forEach(container -> {
            var tests = new LinkedHashMap<String, Throwable>();
            container.getChildren().forEach((DynamicNode node) -> {
                Throwable thrown = null;
                try {
                    ((DynamicTest) node).getExecutable().execute();
                } catch (Throwable t) {
                    thrown = t;
                }
                tests.put(node.getDisplayName(), thrown);
            });
            results.put(container.getDisplayName(), tests);
        });
        return results;
    }

    /** {@code broken} fails in the scenario's container and the other rules pass. */
    static void onlyBreaks(String broken, Map<String, Throwable> tests) {
        assertTrue(tests.keySet().containsAll(RULES), tests.keySet().toString());
        assertInstanceOf(AssertionError.class, tests.get(broken), broken);
        for (var rule : RULES) {
            if (!rule.equals(broken)) {
                assertNull(tests.get(rule), rule);
            }
        }
    }

    @Test
    void aBlankRefusalBreaksTheReasonRule() {
        var tests = results(contract(() -> projectsBut(a -> isRename(a) ? new Gate.Refuse("") : null), RENAME))
                .get("rename");
        onlyBreaks(REASON, tests);
        assertTrue(tests.get(REASON).getMessage().startsWith("rename: ivan's [assert "), tests.get(REASON).getMessage());
    }

    @Test
    void refusingAReadBreaksTheReadRule() {
        var results = results(contract(() -> projectsBut(a ->
                Basic.readOf(a).isPresent() ? new Gate.Refuse("conflicts with an earlier write") : null), READING));
        onlyBreaks(READ, results.get("reading"));
    }

    @Test
    void aGateThatDecidesByChanceBreaksTheSameRule() {
        var made = new AtomicInteger();
        var results = results(contract(() -> {
            boolean refuses = made.getAndIncrement() % 2 == 1;
            return projectsBut(a -> isRename(a) && refuses ? new Gate.Refuse("by chance") : null);
        }, RENAME));
        var tests = results.get("rename");
        onlyBreaks(SAME, tests);
        assertNull(tests.get("expectation"), "the first run passes the rename");
    }

    @Test
    void projectsKeepsEveryGateRule() {
        var results = results(new ProjectsContractTest());
        assertFalse(results.isEmpty());
        assertEquals(new ProjectsContractTest().scenarios().size(), results.size());
        results.forEach((scenario, tests) -> {
            assertEquals(4, tests.size(), scenario);
            tests.forEach((test, thrown) -> assertNull(thrown, scenario + ": " + test));
        });
    }

    @Test
    void aBrokenScenarioFailsEveryTestOfItsContainerAndRunsOnce() {
        var gates = new AtomicInteger();
        var broken = Scenario.named("broken")
                .given(create(), rename("ivan", IVAN, KITCHEN, "Studio"), rename("mia", MIA, KITCHEN, "Atelier"))
                .when(new When.Submit(read("read", IVAN)))
                .then(new Then.Logged());
        var results = results(contract(() -> {
            gates.incrementAndGet();
            return new Projects();
        }, broken));
        var tests = results.get("broken");
        assertEquals(4, tests.size());
        tests.forEach((test, thrown) -> {
            assertInstanceOf(AssertionError.class, thrown, test);
            assertTrue(thrown.getMessage().contains("already replaced"), thrown.getMessage());
        });
        assertEquals(1, gates.get());
    }

    @Test
    void theRulesAbortWhenTheGateWasNotAsked() {
        var unknown = Scenario.named("unknown")
                .when(new When.Submit(Step.named("read").by(IVAN).after(Step.UNKNOWN)
                        .does(new ActionSpec.Read(Ref.named("thing")))))
                .then(new Then.Refused(Optional.of("unknown trigger")));
        assertTrue(ScenarioRunner.run(unknown, List.of(new Projects()), Optional.empty()).decisions().isEmpty());
        var tests = results(contract(Projects::new, unknown)).get("unknown");
        assertNull(tests.get("expectation"));
        for (var rule : RULES) {
            assertInstanceOf(TestAbortedException.class, tests.get(rule), rule);
            assertTrue(tests.get(rule).getMessage().contains("the gate was not asked in this scenario"),
                    tests.get(rule).getMessage());
        }
    }

    @Test
    void decisionsOnEmittedActionsAreRecorded() {
        var surface = new Surface() {
            @Override
            public String name() {
                return "namer";
            }

            @Override
            public Set<Interest> interests() {
                return Set.of(new Interest(Set.of(Basic.ASSERT), Set.of()));
            }

            @Override
            public List<Action> receive(Event event) {
                return List.of(Assertion.action(Assertion.of(Identity.named("label"), Projects.NAME,
                        new Assertion.Literal(new Json.Str("seen")))));
            }
        };
        var s = Scenario.named("emits").given(create()).when(new When.Attach(List.of())).then(new Then.Logged());
        var run = ScenarioRunner.run(s, List.of(new Projects()), Optional.of(surface));
        assertEquals(2, run.decisions().size());
        var emitted = run.decisions().get(1);
        assertEquals("namer", emitted.by().principal());
        assertInstanceOf(Gate.Pass.class, emitted.verdict());
    }

    @Test
    void anErrorInTheRunFailsEveryTestOfItsContainerAndRunsOnce() {
        var gates = new AtomicInteger();
        var results = results(contract(() -> {
            gates.incrementAndGet();
            throw new LinkageError("no gate");
        }, READING));
        var tests = results.get("reading");
        assertEquals(4, tests.size());
        tests.forEach((test, thrown) -> {
            assertInstanceOf(AssertionError.class, thrown, test);
            assertEquals("no gate", thrown.getMessage(), test);
        });
        assertEquals(1, gates.get());
    }
}
