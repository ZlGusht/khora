package khora.surfaces.contract;

import static khora.surfaces.contract.EchoContractTest.LABEL;
import static khora.surfaces.contract.EchoContractTest.NAME;
import static khora.surfaces.contract.EchoContractTest.PROJECT;
import static khora.surfaces.contract.EchoContractTest.labels;
import static khora.surfaces.contract.EchoContractTest.tell;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Supplier;
import java.util.stream.Stream;
import khora.events.AcceptedAction;
import khora.events.Action;
import khora.events.Assertion;
import khora.events.Event;
import khora.events.Identity;
import khora.events.json.Json;
import khora.events.log.EventLog;
import khora.surfaces.Authority;
import khora.surfaces.Interest;
import khora.surfaces.Surface;
import org.junit.jupiter.api.DynamicContainer;
import org.junit.jupiter.api.DynamicNode;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.Test;
import org.opentest4j.TestAbortedException;

/** Each surface rule fails for a surface that breaks it; Echo keeps them all, but for the known gap. */
class SurfaceRulesTest {
    static final List<String> RULES = List.of(SurfaceContract.MATCH, SurfaceContract.OWN, SurfaceContract.CAUSED,
            SurfaceContract.SAME, SurfaceContract.ORDERED);

    /** A project is created; the surface is attached from nothing. */
    static final Scenario CREATED = Scenario.named("created")
            .given(tell("create", PROJECT, NAME, "Kitchen"))
            .when(new When.Attach(List.of()))
            .then(labels("create"));

    static SurfaceContract contract(Supplier<Surface> surface, Supplier<Authority> authority, Scenario... scenarios) {
        return new SurfaceContract() {
            @Override
            protected Surface surface() {
                return surface.get();
            }

            @Override
            protected Authority authority() {
                return authority.get();
            }

            @Override
            protected List<Scenario> scenarios() {
                return List.of(scenarios);
            }
        };
    }

    /** Runs every test of the containers: per container, per test, what it threw, or null. */
    static Map<String, Map<String, Throwable>> results(Stream<DynamicContainer> containers) {
        var results = new LinkedHashMap<String, Map<String, Throwable>>();
        containers.forEach(container -> {
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

    /** Labels the project with a random value for every event about it. */
    static Surface randomLabeller() {
        var about = Interest.about(Identity.named("project"));
        return new Surface() {
            @Override
            public String name() {
                return "random";
            }

            @Override
            public Set<Interest> interests() {
                return Set.of(about);
            }

            @Override
            public List<Action> receive(Event event) {
                return List.of(Assertion.action(Assertion.of(Identity.named("project"), Identity.named("khora/basic/label"),
                        new Assertion.Literal(new Json.Str(UUID.randomUUID().toString())))));
            }
        };
    }

    @Test
    void aSurfaceThatEmitsRandomlyBreaksTheSameEventsRule() {
        var tests = results(contract(SurfaceRulesTest::randomLabeller, AcceptAll::new, CREATED).contract()).get("created");
        assertInstanceOf(AssertionError.class, tests.get(SurfaceContract.SAME));
        assertNull(tests.get(SurfaceContract.MATCH));
        assertNull(tests.get(SurfaceContract.OWN));
        assertNull(tests.get(SurfaceContract.CAUSED));
        assertFalse(tests.get(SurfaceContract.ORDERED) instanceof AssertionError);
    }

    @Test
    void anEmissionTheAuthorityRejectsBreaksTheCausedRule() {
        Supplier<Authority> rejectsSurfaces = () -> new Authority() {
            @Override
            public String name() {
                return "no-surfaces";
            }

            @Override
            public Verdict decide(Event candidate, EventLog log) {
                return ((AcceptedAction) candidate.body()).by().grant().equals("surface")
                        ? new Reject("no emissions") : new Accept();
            }
        };
        var tests = results(contract(EchoContractTest::labeller, rejectsSurfaces, CREATED).contract()).get("created");
        assertInstanceOf(AssertionError.class, tests.get(SurfaceContract.CAUSED));
        assertNull(tests.get(SurfaceContract.MATCH));
        assertNull(tests.get(SurfaceContract.SAME));
    }

    @Test
    void echoKeepsEverySurfaceRule() {
        var results = results(new EchoContractTest().contract());
        assertEquals(6, results.size());
        var gap = new HashSet<String>();
        results.forEach((scenario, tests) -> {
            assertEquals(6, tests.size(), scenario);
            assertNull(tests.get("expectation"), scenario + ": expectation");
            for (var rule : RULES) {
                if (!rule.equals(SurfaceContract.ORDERED)) {
                    assertNull(tests.get(rule), scenario + ": " + rule);
                }
            }
            var ordered = assertInstanceOf(TestAbortedException.class, tests.get(SurfaceContract.ORDERED), scenario);
            if (ordered.getMessage().contains(KnownGaps.RULES.get(SurfaceContract.ORDERED))) {
                gap.add(scenario);
            } else {
                assertTrue(ordered.getMessage().contains("fewer than two"), ordered.getMessage());
            }
        });
        assertEquals(Set.of("anAttachedSurfaceCatchesUpThenReceivesWhatMatches",
                "catchingUpKeepsLogOrderAndSkipsOwnEmissions"), gap);
    }

    @Test
    void theRulesAbortWhenTheSurfaceReceivedNothing() {
        var nothing = Scenario.named("nothing")
                .given(tell("invoice", Ref.named("invoice"), NAME, "INV-1"))
                .when(new When.Attach(List.of()))
                .then(new Then.Emits(List.of()));
        var tests = results(contract(EchoContractTest::labeller, AcceptAll::new, nothing).contract()).get("nothing");
        assertNull(tests.get("expectation"));
        for (var rule : RULES) {
            assertInstanceOf(TestAbortedException.class, tests.get(rule), rule);
            assertTrue(tests.get(rule).getMessage().contains("the surface received nothing in this scenario"),
                    tests.get(rule).getMessage());
        }
    }

    @Test
    void aWrongEmissionFailsTheExpectation() {
        var wrong = Scenario.named("wrong")
                .given(tell("create", PROJECT, NAME, "Kitchen"), tell("rename", PROJECT, NAME, "Studio", "create"))
                .when(new When.Attach(List.of()))
                .then(labels("rename", "create"));
        var tests = results(contract(EchoContractTest::labeller, AcceptAll::new, wrong).contract()).get("wrong");
        assertInstanceOf(AssertionError.class, tests.get("expectation"));
    }

    @Test
    void echoEmitsForEveryMatchingRuleInOrderWithReceivedRefsBound() {
        var about = Interest.about(Identity.named("project"));
        var echo = new Echo("echo", Set.of(Interest.any()), List.of(
                new Echo.Rule(about, List.of(new ActionSpec.Read(Ref.received(0)))),
                new Echo.Rule(about("invoice"), List.of(new ActionSpec.Read(Ref.named("invoice")))),
                new Echo.Rule(Interest.any(), List.of(new ActionSpec.Assert(List.of(
                        TupleSpec.of(PROJECT, LABEL, ValueSpec.ref(Ref.received(1)))))))));
        var run = ScenarioRunner.run(CREATED, new AcceptAll(), Optional.of(echo));
        var event = run.deliveries().getFirst().event();
        assertEquals(List.of(khora.events.Basic.read(Identity.of(event.id(), 0)),
                Assertion.action(Assertion.of(Identity.named("project"), Identity.named("khora/basic/label"),
                        new Assertion.Ref(Identity.of(event.id(), 1))))), echo.receive(event));
    }

    static Interest about(String name) {
        return Interest.about(Identity.named(name));
    }

    @Test
    void echoRefusesStepRefs() {
        var any = Interest.any();
        assertThrows(IllegalArgumentException.class, () -> new Echo("echo", Set.of(any),
                List.of(new Echo.Rule(any, List.of(new ActionSpec.Read(Ref.step("create")))))));
        var e = assertThrows(IllegalArgumentException.class, () -> new Echo("echo", Set.of(any),
                List.of(new Echo.Rule(any, List.of(new ActionSpec.Assert(List.of(
                        TupleSpec.of(PROJECT, LABEL, ValueSpec.ref(Ref.tuple("create", 0))))))))));
        assertTrue(e.getMessage().contains("create"), e.getMessage());
    }
}
