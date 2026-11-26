package khora.surfaces.contract;

import static khora.surfaces.contract.AuthorityContract.LOG;
import static khora.surfaces.contract.AuthorityContract.READ;
import static khora.surfaces.contract.AuthorityContract.REASON;
import static khora.surfaces.contract.AuthorityContract.TWICE;
import static khora.surfaces.contract.ProjectsContractTest.IVAN;
import static khora.surfaces.contract.ProjectsContractTest.create;
import static khora.surfaces.contract.ProjectsContractTest.read;
import static khora.surfaces.contract.ProjectsContractTest.rename;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;
import khora.events.AcceptedAction;
import khora.events.Action;
import khora.events.Basic;
import khora.events.Event;
import khora.events.Identity;
import khora.events.log.EventLog;
import khora.surfaces.Authority;
import khora.surfaces.Interest;
import khora.surfaces.Surface;
import khora.surfaces.stub.Projects;
import org.junit.jupiter.api.DynamicNode;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.Test;
import org.opentest4j.AssertionFailedError;
import org.opentest4j.TestAbortedException;

/** Each authority rule fails for an authority that breaks it, and only for that one. */
class AuthorityRulesTest {
    static final List<String> RULES = List.of(LOG, TWICE, REASON, READ);

    /** A creation, a read of it, then a rename based on that read. */
    static final Scenario RENAME = Scenario.named("rename")
            .given(create(), read("read", IVAN))
            .when(new When.Submit(rename("rename", IVAN, "read", "Studio")))
            .then(new Then.Accepted());

    /** A creation, then a read of it. */
    static final Scenario READING = Scenario.named("reading")
            .given(create())
            .when(new When.Submit(read("read", IVAN)))
            .then(new Then.Accepted());

    static boolean isRead(Event candidate) {
        return Basic.readOf(((AcceptedAction) candidate.body()).action()).isPresent();
    }

    static boolean isRename(Event candidate) {
        return ((AcceptedAction) candidate.body()).links().containsKey("based_on");
    }

    /** Projects, except as {@code decide} says; {@code decide} returns null to leave it to Projects. */
    interface Misbehaviour {
        Authority.Verdict decide(Event candidate, EventLog log);
    }

    static Authority projectsBut(Misbehaviour misbehaviour) {
        return new Authority() {
            final Projects projects = new Projects();

            @Override
            public String name() {
                return projects.name();
            }

            @Override
            public Verdict decide(Event candidate, EventLog log) {
                var verdict = misbehaviour.decide(candidate, log);
                return verdict != null ? verdict : projects.decide(candidate, log);
            }
        };
    }

    static AuthorityContract contract(Supplier<Authority> authority, Scenario... scenarios) {
        return new AuthorityContract() {
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

    /** Runs every test of the contract: per container, per test, what it threw, or null. */
    static Map<String, Map<String, Throwable>> results(AuthorityContract contract) {
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
    void anAuthorityThatAppendsBreaksTheLogRule() {
        var results = results(contract(() -> projectsBut((candidate, log) -> {
            if (isRename(candidate)) {
                log.append(List.of(candidate));
            }
            return null;
        }), RENAME));
        onlyBreaks(LOG, results.get("rename"));
    }

    @Test
    void anAuthorityThatCatchesTheRefusalAndAcceptsStillBreaksTheLogRule() {
        var results = results(contract(() -> projectsBut((candidate, log) -> {
            if (!isRename(candidate)) {
                return null;
            }
            try {
                log.append(List.of(candidate));
            } catch (UnsupportedOperationException e) {
                // swallowed: the read-only log remembers the try
            }
            return new Authority.Accept();
        }), RENAME));
        var tests = results.get("rename");
        onlyBreaks(LOG, tests);
        assertNull(tests.get("expectation"), "the rename is accepted");
    }

    @Test
    void anAuthorityThatFlipsBreaksTheSameTwiceRule() {
        var results = results(contract(() -> {
            var calls = new AtomicInteger();
            return projectsBut((candidate, log) ->
                    calls.incrementAndGet() % 2 == 1 ? new Authority.Accept() : new Authority.Reject("flipped"));
        }, RENAME));
        onlyBreaks(TWICE, results.get("rename"));
        assertNull(results.get("rename").get("expectation"));
    }

    @Test
    void aBlankRejectionBreaksTheReasonRule() {
        var results = results(contract(() -> projectsBut((candidate, log) ->
                isRename(candidate) ? new Authority.Reject("") : null), RENAME));
        onlyBreaks(REASON, results.get("rename"));
    }

    @Test
    void rejectingAReadBreaksTheReadRule() {
        var results = results(contract(() -> projectsBut((candidate, log) ->
                isRead(candidate) ? new Authority.Reject("conflicts with an earlier write") : null), READING));
        onlyBreaks(READ, results.get("reading"));
    }

    @Test
    void projectsKeepsEveryAuthorityRule() {
        var results = results(new ProjectsContractTest());
        assertEquals(10, results.size());
        results.forEach((scenario, tests) -> {
            assertEquals(5, tests.size(), scenario);
            tests.forEach((test, thrown) -> assertNull(thrown, scenario + ": " + test));
        });
    }

    @Test
    void aBrokenScenarioFailsEveryTestOfItsContainerAndRunsOnce() {
        var authorities = new AtomicInteger();
        var broken = Scenario.named("broken")
                .given(create(), Step.named("order").by(IVAN).knew("create")
                        .does(new ActionSpec.Raw(Identity.named("orders/place"), khora.events.json.Json.Obj.of(Map.of()))))
                .when(new When.Submit(read("read", IVAN)))
                .then(new Then.Accepted());
        var results = results(contract(() -> {
            authorities.incrementAndGet();
            return new Projects();
        }, broken));
        var tests = results.get("broken");
        assertEquals(5, tests.size());
        tests.forEach((test, thrown) -> {
            assertInstanceOf(AssertionError.class, thrown, test);
            assertTrue(thrown.getMessage().contains("projects takes only reads and asserts"), thrown.getMessage());
        });
        assertEquals(1, authorities.get());
    }

    @Test
    void theRulesAbortWhenTheAuthorityWasNotAsked() {
        var unknown = Scenario.named("unknown")
                .when(new When.Submit(Step.named("read").by(IVAN).knew(Step.UNKNOWN)
                        .does(new ActionSpec.Read(Ref.named("thing")))))
                .then(new Then.Rejected(Optional.of("unknown event")));
        assertTrue(ScenarioRunner.run(unknown, new Projects(), Optional.empty()).decisions().isEmpty());
        var tests = results(contract(Projects::new, unknown)).get("unknown");
        assertNull(tests.get("expectation"));
        for (var rule : RULES) {
            assertInstanceOf(TestAbortedException.class, tests.get(rule), rule);
            assertTrue(tests.get(rule).getMessage().contains("the authority was not asked in this scenario"),
                    tests.get(rule).getMessage());
        }
    }

    @Test
    void theRulesJudgeTheGivenStepsWhenTheKernelRejectsTheWhen() {
        var run = ScenarioRunner.run(new ProjectsContractTest().scenarios().stream()
                .filter(s -> s.name().equals("anUnknownFrontierIsRejected")).findFirst().orElseThrow(),
                new Projects(), Optional.empty());
        assertEquals(1, run.decisions().size(), "the given creation");
    }

    @Test
    void decisionsOnEmittedActionsAreRecorded() {
        var surface = new Surface() {
            @Override
            public String name() {
                return "reader";
            }

            @Override
            public Set<Interest> interests() {
                return Set.of(Interest.any());
            }

            @Override
            public List<Action> receive(Event event) {
                return isRead(event) ? List.of() : List.of(Basic.read(Identity.of(event.id(), 0)));
            }
        };
        var s = Scenario.named("emits").given(create()).when(new When.Attach(List.of())).then(new Then.Accepted());
        var run = ScenarioRunner.run(s, new Projects(), Optional.of(surface));
        assertEquals(2, run.decisions().size());
        var emitted = run.decisions().get(1);
        assertTrue(isRead(emitted.candidate()));
        assertTrue(((AcceptedAction) emitted.candidate().body()).links().containsKey("source"));
        assertInstanceOf(Authority.Accept.class, emitted.first());
        assertFalse(emitted.appendedDuringDecide());
    }

    @Test
    void aReadOnlyLogRefusesChanges() {
        var log = new ReadOnlyEventLog(new khora.events.log.InMemoryEventLog());
        assertThrows(UnsupportedOperationException.class, () -> log.append(List.of()));
        assertThrows(UnsupportedOperationException.class, () -> log.follow(1, logged -> { }));
        assertThrows(UnsupportedOperationException.class, log::close);
        assertTrue(log.read(1, 10).isEmpty());
    }

    @Test
    void aKnownGapThatFailsAborts() {
        var gaps = Map.of("some rule", "not yet");
        var e = assertThrows(TestAbortedException.class,
                () -> KnownGaps.judge(gaps, "some rule", () -> { throw new AssertionFailedError("broken"); }));
        assertTrue(e.getMessage().contains("not yet"), e.getMessage());
    }

    @Test
    void aKnownGapThatPassesFails() {
        var gaps = Map.of("some rule", "not yet");
        var e = assertThrows(AssertionFailedError.class, () -> KnownGaps.judge(gaps, "some rule", () -> { }));
        assertEquals("known gap appears fixed: remove it from KnownGaps: some rule", e.getMessage());
    }

    @Test
    void aRuleThatIsNoKnownGapIsJudgedByItsCheck() {
        var gaps = Map.of("some rule", "not yet");
        assertThrows(AssertionFailedError.class,
                () -> KnownGaps.judge(gaps, "other rule", () -> { throw new AssertionFailedError("broken"); }));
        KnownGaps.judge(gaps, "other rule", () -> { });
        assertEquals(Set.of(SurfaceContract.ORDERED, KernelGapsTest.INTERESTS_THROW), KnownGaps.RULES.keySet());
    }

    @Test
    void aKnownGapThatThrowsSomethingElseFails() {
        var gaps = Map.of("some rule", "not yet");
        var e = assertThrows(IllegalStateException.class,
                () -> KnownGaps.judge(gaps, "some rule", () -> { throw new IllegalStateException("a bug"); }));
        assertEquals("a bug", e.getMessage());
    }

    @Test
    void anErrorInTheRunFailsEveryTestOfItsContainerAndRunsOnce() {
        var authorities = new AtomicInteger();
        var results = results(contract(() -> {
            authorities.incrementAndGet();
            throw new LinkageError("no authority");
        }, READING));
        var tests = results.get("reading");
        assertEquals(5, tests.size());
        tests.forEach((test, thrown) -> {
            assertInstanceOf(AssertionError.class, thrown, test);
            assertEquals("no authority", thrown.getMessage(), test);
        });
        assertEquals(1, authorities.get());
    }
}
