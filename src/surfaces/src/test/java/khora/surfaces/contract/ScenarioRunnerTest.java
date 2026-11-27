package khora.surfaces.contract;

import static khora.surfaces.contract.ProjectsContractTest.ANNA;
import static khora.surfaces.contract.ProjectsContractTest.IVAN;
import static khora.surfaces.contract.ProjectsContractTest.KITCHEN;
import static khora.surfaces.contract.ProjectsContractTest.MIA;
import static khora.surfaces.contract.ProjectsContractTest.NAME;
import static khora.surfaces.contract.ProjectsContractTest.PROJECT;
import static khora.surfaces.contract.ProjectsContractTest.create;
import static khora.surfaces.contract.ProjectsContractTest.read;
import static khora.surfaces.contract.ProjectsContractTest.rename;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Optional;
import khora.events.Identity;
import khora.surfaces.Answer;
import khora.surfaces.Gate;
import khora.surfaces.stub.Projects;
import org.junit.jupiter.api.Test;

class ScenarioRunnerTest {

    static Run run(Scenario s) {
        return ScenarioRunner.run(s, List.of(new Projects()), Optional.empty());
    }

    @Test
    void aRefusedGivenStepBreaksTheScenario() {
        var s = Scenario.named("broken")
                .given(create(), rename("ivan", IVAN, KITCHEN, "Studio"), rename("mia", MIA, KITCHEN, "Atelier"))
                .when(new When.Submit(read("read", IVAN)))
                .then(new Then.Logged());
        var e = assertThrows(IllegalStateException.class, () -> run(s));
        assertTrue(e.getMessage().contains("broken"), e.getMessage());
        assertTrue(e.getMessage().contains("mia"), e.getMessage());
        assertTrue(e.getMessage().contains("already replaced"), e.getMessage());
    }

    @Test
    void duplicateScenarioNamesAreRefused() {
        var s = Scenario.named("twice").when(new When.Submit(create())).then(new Then.Logged());
        var contract = new GateContract() {
            @Override
            protected Gate gate() {
                return new Projects();
            }

            @Override
            protected List<Scenario> scenarios() {
                return List.of(s, s);
            }
        };
        var e = assertThrows(IllegalArgumentException.class, contract::contract);
        assertTrue(e.getMessage().contains("twice"), e.getMessage());
    }

    @Test
    void theSameScenarioRunsTheSameTwice() {
        var s = Scenario.named("renamed")
                .given(create(), rename("rename", IVAN, KITCHEN, "Studio"))
                .when(new When.Read(PROJECT))
                .then(new Then.Statements(List.of(TupleSpec.entity(),
                        TupleSpec.of(PROJECT, NAME, ValueSpec.text("Studio")))));
        var first = run(s);
        var second = run(s);
        assertFalse(first.statements().isEmpty());
        assertEquals(GateContract.symbolic(first.statements(), first.bindings()),
                GateContract.symbolic(second.statements(), second.bindings()));
        assertEquals(List.of("(, , )", "(create/0, name, \"Studio\")"),
                GateContract.symbolic(first.statements(), first.bindings()));
    }

    @Test
    void aSubmittedReadAnswersWithItsStatements() {
        var s = Scenario.named("read").given(create())
                .when(new When.Submit(read("read", IVAN))).then(new Then.Statements(List.of()));
        var run = run(s);
        assertInstanceOf(Answer.Logged.class, run.answer().orElseThrow());
        assertEquals(2, run.statements().size());
        assertEquals(1, run.decisions().size(), "the gate guards the creation's name, not the read");
        assertInstanceOf(Gate.Pass.class, run.decisions().getFirst().verdict());
        assertTrue(run.deliveries().isEmpty());
    }

    @Test
    void aRepeatedStepNameIsRefused() {
        var s = Scenario.named("repeat").given(create())
                .when(new When.Submit(create())).then(new Then.Logged());
        var e = assertThrows(IllegalArgumentException.class, () -> run(s));
        assertTrue(e.getMessage().contains("repeat") && e.getMessage().contains("create"), e.getMessage());
    }

    @Test
    void aTriggerOfNoEarlierStepIsRefused() {
        var s = Scenario.named("dangling").given(create())
                .when(new When.Submit(Step.named("again").by(IVAN).after("nowhere").does(ActionSpec.read(PROJECT))))
                .then(new Then.Logged());
        var e = assertThrows(IllegalArgumentException.class, () -> run(s));
        assertTrue(e.getMessage().contains("dangling") && e.getMessage().contains("nowhere"), e.getMessage());
    }

    @Test
    void statementsOfASubmittedNonReadAreABrokenScenario() {
        var s = Scenario.named("noRead").when(new When.Submit(create()))
                .then(new Then.Statements(List.of()));
        var run = run(s);
        var e = assertThrows(AssertionError.class, () -> GateContract.expect(s, run));
        assertTrue(e.getMessage().contains("noRead"), e.getMessage());
    }

    @Test
    void attachingTheSurfaceTwiceIsRefused() {
        var twice = Scenario.named("twice")
                .given(create(), Given.attach("create"))
                .when(new When.Attach(List.of()))
                .then(new Then.Emits(List.of()));
        var e = assertThrows(IllegalArgumentException.class,
                () -> ScenarioRunner.run(twice, List.of(), Optional.of(EchoContractTest.labeller())));
        assertTrue(e.getMessage().contains("twice") && e.getMessage().contains("at most once"), e.getMessage());
        var given = Scenario.named("givenTwice")
                .given(Given.attach(), create(), Given.attach("create"))
                .when(new When.Submit(read("read", IVAN)))
                .then(new Then.Emits(List.of()));
        assertThrows(IllegalArgumentException.class,
                () -> ScenarioRunner.run(given, List.of(), Optional.of(EchoContractTest.labeller())));
    }

    @Test
    void aStepByTheSurfacesNameIsRefused() {
        var echo = EchoContractTest.labeller();
        var asEcho = new khora.surfaces.Context(echo.name(), Optional.empty(), "g3", 10);
        var s = Scenario.named("impostor")
                .given(Given.attach(), Step.named("forged").by(asEcho).does(new ActionSpec.Assert(List.of(
                        TupleSpec.of(EchoContractTest.PROJECT, NAME, ValueSpec.text("Kitchen"))))))
                .when(new When.Submit(read("read", IVAN)))
                .then(new Then.Emits());
        var e = assertThrows(IllegalArgumentException.class,
                () -> ScenarioRunner.run(s, List.of(), Optional.of(echo)));
        assertTrue(e.getMessage().contains("impostor") && e.getMessage().contains("forged")
                && e.getMessage().contains(echo.name()), e.getMessage());
    }

    @Test
    void aStepByTheReadersNameIsRefused() {
        var asReader = new khora.surfaces.Context("entities", Optional.empty(), "g3", 10);
        var s = Scenario.named("impostor")
                .given(create(), Step.named("forged").by(asReader).does(ActionSpec.read(PROJECT)))
                .when(new When.Read(PROJECT))
                .then(new Then.Statements(List.of()));
        var e = assertThrows(IllegalArgumentException.class, () -> run(s));
        assertTrue(e.getMessage().contains("reference reader"), e.getMessage());
    }

    @Test
    void attachingFromALaterStepIsRefused() {
        var later = Scenario.named("later")
                .given(Given.attach("create"), create())
                .when(new When.Submit(read("read", IVAN)))
                .then(new Then.Emits(List.of()));
        var e = assertThrows(IllegalArgumentException.class,
                () -> ScenarioRunner.run(later, List.of(), Optional.of(EchoContractTest.labeller())));
        assertTrue(e.getMessage().contains("create"), e.getMessage());
    }

    @Test
    void aSurfaceAttachedAmongTheGivenStepsReceivesTheLaterOnesLive() {
        var live = Scenario.named("live")
                .given(EchoContractTest.tell("create", EchoContractTest.PROJECT, NAME, "Kitchen"), Given.attach(),
                        EchoContractTest.tell("rename", EchoContractTest.PROJECT, NAME, "Studio"))
                .when(new When.Submit(EchoContractTest.tell("again", EchoContractTest.PROJECT, NAME, "Atelier")))
                .then(new Then.Emits(List.of()));
        var run = ScenarioRunner.run(live, List.of(), Optional.of(EchoContractTest.labeller()));
        assertEquals(List.of("create", "rename", "again"),
                run.deliveries().stream().map(d -> run.bindings().render(new Identity(d.event().id().value()))).toList());
        assertEquals(6, run.events().size(), "three steps and three labels");
        assertEquals("emitted#1", run.bindings().render(new Identity(run.events().get(1).id().value())));
        assertEquals(Optional.of(run.events().get(0).id()), run.events().get(1).trigger());
    }

    @Test
    void aStepNamesItsTrigger() {
        var s = Scenario.named("reply").given(create())
                .when(new When.Submit(Step.named("reply").by(ANNA).after("create").does(ActionSpec.read(PROJECT))))
                .then(new Then.Logged());
        var run = run(s);
        assertEquals(Optional.of(run.bindings().event("create")), run.events().get(1).trigger());
    }
}
