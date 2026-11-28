package khora.surfaces.contract;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import khora.events.Identity;
import khora.events.json.Json;
import khora.surfaces.Context;
import khora.surfaces.Gate;
import khora.surfaces.stub.Projects;

/**
 * {@link Projects} against the gate contract, with the scenarios of {@code KernelTest}. Who acted
 * ({@code by}) and an event's trigger are kernel guarantees, and stay covered by {@code KernelTest}.
 */
class ProjectsContractTest extends GateContract {
    static final Context ANNA = new Context("anna", Optional.empty(), "g3", 10);
    static final Context IVAN = new Context("ivan", Optional.empty(), "g3", 10);
    static final Context MIA = new Context("mia", Optional.empty(), "g3", 10);
    static final Context BOB = new Context("bob", Optional.empty(), "g3", 10);

    static final Ref NAME = Ref.named("name");
    static final Ref PROJECT = Ref.tuple("create", 0);
    static final Ref KITCHEN = Ref.tuple("create", 1);

    static Step create() {
        return Step.named("create").by(ANNA).does(ActionSpec.asserts(TupleSpec.entity(),
                TupleSpec.of(Ref.local(0), NAME, ValueSpec.text("Kitchen"))));
    }

    static Step read(String name, Context who) {
        return Step.named(name).by(who).does(ActionSpec.read(PROJECT));
    }

    /** Renames the project, replacing the name {@code replaces}. */
    static Step rename(String name, Context who, Ref replaces, String to) {
        return Step.named(name).by(who).does(ActionSpec.asserts(TupleSpec.retraction(replaces),
                TupleSpec.of(PROJECT, NAME, ValueSpec.text(to))));
    }

    @Override
    protected Gate gate() {
        return new Projects();
    }

    @Override
    protected List<Scenario> scenarios() {
        return List.of(
                Scenario.named("aNewProjectIsLogged")
                        .when(When.submit(create()))
                        .then(Then.logged()),
                Scenario.named("aReadIsAnsweredWithStatements")
                        .given(create())
                        .when(When.submit(read("read", IVAN)))
                        .then(Then.statements(TupleSpec.entity(),
                                TupleSpec.of(PROJECT, NAME, ValueSpec.text("Kitchen")))),
                Scenario.named("aRenameOfTheCurrentNameIsLogged")
                        .given(create(), read("read", IVAN))
                        .when(When.submit(rename("rename", IVAN, KITCHEN, "Studio")))
                        .then(Then.logged()),
                Scenario.named("theNewNameStands")
                        .given(create(), rename("rename", IVAN, KITCHEN, "Studio"))
                        .when(When.read(PROJECT))
                        .then(Then.statements(TupleSpec.entity(),
                                TupleSpec.of(PROJECT, NAME, ValueSpec.text("Studio")))),
                Scenario.named("aSecondRenameOfOneNameIsRefused")
                        .given(create(), read("ivanRead", IVAN), read("miaRead", MIA),
                                rename("ivanRename", IVAN, KITCHEN, "Studio"))
                        .when(When.submit(rename("miaRename", MIA, KITCHEN, "Atelier")))
                        .then(Then.refused("already replaced")),
                Scenario.named("aRenameOfTheNewNameIsLogged")
                        .given(create(), rename("ivanRename", IVAN, KITCHEN, "Studio"))
                        .when(When.submit(rename("miaRename", MIA, Ref.tuple("ivanRename", 1), "Atelier")))
                        .then(Then.logged()),
                Scenario.named("anUnrelatedWriteDoesNotConflict")
                        .given(create(), Step.named("invoice").by(BOB).does(ActionSpec.asserts(
                                TupleSpec.of(Ref.named("invoice"), NAME, ValueSpec.text("INV-1")))))
                        .when(When.submit(rename("rename", IVAN, KITCHEN, "Studio")))
                        .then(Then.logged()),
                Scenario.named("aBlindWriteIsLogged")
                        .given(create(), rename("rename", IVAN, KITCHEN, "Studio"))
                        .when(When.submit(Step.named("blind").by(MIA).does(ActionSpec.asserts(
                                TupleSpec.of(PROJECT, NAME, ValueSpec.text("Atelier"))))))
                        .then(Then.logged()),
                Scenario.named("replacingAStatementTheGateDoesNotKnowIsRefused")
                        .given(create())
                        .when(When.submit(rename("rename", MIA, Ref.tuple("create", 5), "Atelier")))
                        .then(Then.refused("not a statement projects knows")),
                Scenario.named("anUnknownTriggerIsRefused")
                        .given(create())
                        .when(When.submit(Step.named("read").by(IVAN).after(Step.UNKNOWN)
                                .does(ActionSpec.read(PROJECT))))
                        .then(Then.refused("unknown trigger")),
                Scenario.named("actionsNoGateGuardsAreLogged")
                        .given(create())
                        .when(When.submit(Step.named("order").by(ANNA)
                                .does(new ActionSpec.Raw(Identity.named("orders/place"), Json.Obj.of(Map.of())))))
                        .then(Then.logged()),
                Scenario.named("retractingARetractionDoesNotRestoreTheStatement")
                        .given(create(), rename("rename", IVAN, KITCHEN, "Studio"),
                                Step.named("unretract").by(IVAN)
                                        .does(ActionSpec.asserts(TupleSpec.retraction(Ref.tuple("rename", 0)))))
                        .when(When.read(PROJECT))
                        .then(Then.statements(TupleSpec.entity(),
                                TupleSpec.of(PROJECT, NAME, ValueSpec.text("Studio")))));
    }
}
