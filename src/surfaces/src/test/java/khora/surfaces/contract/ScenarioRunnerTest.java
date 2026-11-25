package khora.surfaces.contract;

import static khora.surfaces.contract.ProjectsContractTest.ANNA;
import static khora.surfaces.contract.ProjectsContractTest.IVAN;
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
import java.util.Map;
import java.util.Optional;
import khora.events.Identity;
import khora.events.json.Json;
import khora.surfaces.Answer;
import khora.surfaces.Authority;
import khora.surfaces.stub.Projects;
import org.junit.jupiter.api.Test;

class ScenarioRunnerTest {

    static Run run(Scenario s) {
        return ScenarioRunner.run(s, new Projects(), Optional.empty());
    }

    @Test
    void aRejectedGivenStepBreaksTheScenario() {
        var s = Scenario.named("broken")
                .given(create(), Step.named("order").by(ANNA).knew("create")
                        .does(new ActionSpec.Raw(Identity.named("orders/place"), Json.Obj.of(Map.of()))))
                .when(new When.Submit(read("read", IVAN)))
                .then(new Then.Accepted());
        var e = assertThrows(IllegalStateException.class, () -> run(s));
        assertTrue(e.getMessage().contains("broken"), e.getMessage());
        assertTrue(e.getMessage().contains("order"), e.getMessage());
        assertTrue(e.getMessage().contains("projects takes only reads and asserts"), e.getMessage());
    }

    @Test
    void duplicateScenarioNamesAreRefused() {
        var s = Scenario.named("twice").when(new When.Submit(create())).then(new Then.Accepted());
        var contract = new AuthorityContract() {
            @Override
            protected Authority authority() {
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
                .given(create(), read("read", IVAN), rename("rename", IVAN, "read", "Studio"))
                .when(new When.Read(PROJECT, List.of("rename")))
                .then(new Then.Statements(List.of(TupleSpec.entity(),
                        TupleSpec.of(PROJECT, NAME, ValueSpec.text("Studio")))));
        var first = run(s);
        var second = run(s);
        assertFalse(first.statements().isEmpty());
        assertEquals(AuthorityContract.symbolic(first.statements(), first.bindings()),
                AuthorityContract.symbolic(second.statements(), second.bindings()));
        assertEquals(List.of("(, , )", "(create/0, " + Identity.named("name").value() + ", \"Studio\")"),
                AuthorityContract.symbolic(first.statements(), first.bindings()));
    }

    @Test
    void aSubmittedReadAnswersWithItsStatements() {
        var s = Scenario.named("read").given(create())
                .when(new When.Submit(read("read", IVAN))).then(new Then.Accepted());
        var run = run(s);
        assertInstanceOf(Answer.Accepted.class, run.answer().orElseThrow());
        assertEquals(2, run.statements().size());
        assertTrue(run.decisions().isEmpty());
        assertTrue(run.deliveries().isEmpty());
    }

    @Test
    void aRepeatedStepNameIsRefused() {
        var s = Scenario.named("repeat").given(create())
                .when(new When.Submit(create())).then(new Then.Accepted());
        var e = assertThrows(IllegalArgumentException.class, () -> run(s));
        assertTrue(e.getMessage().contains("repeat") && e.getMessage().contains("create"), e.getMessage());
    }

    @Test
    void aKnewOfNoEarlierStepIsRefused() {
        var s = Scenario.named("ahead").given(read("read", IVAN), create())
                .when(new When.Submit(read("again", IVAN))).then(new Then.Accepted());
        var e = assertThrows(IllegalArgumentException.class, () -> run(s));
        assertTrue(e.getMessage().contains("ahead") && e.getMessage().contains("read"), e.getMessage());
    }

    @Test
    void aLinkToNoEarlierStepIsRefused() {
        var s = Scenario.named("dangling").given(create())
                .when(new When.Submit(rename("rename", IVAN, "nowhere", "Studio"))).then(new Then.Accepted());
        var e = assertThrows(IllegalArgumentException.class, () -> run(s));
        assertTrue(e.getMessage().contains("dangling") && e.getMessage().contains("nowhere"), e.getMessage());
    }
}
