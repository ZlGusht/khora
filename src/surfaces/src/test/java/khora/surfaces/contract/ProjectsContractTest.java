package khora.surfaces.contract;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import khora.events.Identity;
import khora.events.json.Json;
import khora.surfaces.Authority;
import khora.surfaces.Context;
import khora.surfaces.stub.Projects;

/**
 * {@link Projects} against the authority contract, with the scenarios of {@code KernelTest}. Who
 * acted ({@code by}), an event's causes and a read's {@code at} link are kernel guarantees, and stay
 * covered by {@code KernelTest}.
 */
class ProjectsContractTest extends AuthorityContract {
    static final Context ANNA = new Context("anna", Optional.empty(), "g3", 10);
    static final Context IVAN = new Context("ivan", Optional.empty(), "g3", 10);
    static final Context MIA = new Context("mia", Optional.empty(), "g3", 10);
    static final Context BOB = new Context("bob", Optional.empty(), "g3", 10);

    static final Ref NAME = Ref.named("name");
    static final Ref PROJECT = Ref.tuple("create", 0);

    static Step create() {
        return Step.named("create").by(ANNA).does(ActionSpec.asserts(TupleSpec.entity(),
                TupleSpec.of(Ref.local(0), NAME, ValueSpec.text("Kitchen"))));
    }

    static Step read(String name, Context who) {
        return Step.named(name).by(who).knew("create").does(ActionSpec.read(PROJECT));
    }

    /** Renames the project from Kitchen, based on the read {@code basedOn}. */
    static Step rename(String name, Context who, String basedOn, String to) {
        return Step.named(name).by(who).knew(basedOn).link("based_on", basedOn)
                .does(ActionSpec.asserts(TupleSpec.retraction(Ref.tuple("create", 1)),
                        TupleSpec.of(PROJECT, NAME, ValueSpec.text(to))));
    }

    @Override
    protected Authority authority() {
        return new Projects();
    }

    @Override
    protected List<Scenario> scenarios() {
        return List.of(
                Scenario.named("anAcceptedActionIsAnEventByItsCaller")
                        .when(When.submit(create()))
                        .then(Then.accepted()),
                Scenario.named("aReadIsAnEventAndAnswersWithStatements")
                        .given(create())
                        .when(When.submit(read("read", IVAN)))
                        .then(Then.statements(TupleSpec.entity(),
                                TupleSpec.of(PROJECT, NAME, ValueSpec.text("Kitchen")))),
                Scenario.named("aWriteFollowsTheReadItWasBasedOn")
                        .given(create(), read("read", IVAN))
                        .when(When.submit(rename("rename", IVAN, "read", "Studio")))
                        .then(Then.accepted()),
                Scenario.named("theRenamedNameHoldsAtTheRename")
                        .given(create(), read("read", IVAN), rename("rename", IVAN, "read", "Studio"))
                        .when(When.read(PROJECT, "rename"))
                        .then(Then.statements(TupleSpec.entity(),
                                TupleSpec.of(PROJECT, NAME, ValueSpec.text("Studio")))),
                Scenario.named("aReadAtAnEarlierFrontierIgnoresLaterWrites")
                        .given(create(), read("read", IVAN), rename("rename", IVAN, "read", "Studio"))
                        .when(When.read(PROJECT, "create"))
                        .then(Then.statements(TupleSpec.entity(),
                                TupleSpec.of(PROJECT, NAME, ValueSpec.text("Kitchen")))),
                Scenario.named("aConcurrentRenameOfTheSameNameIsRejected")
                        .given(create(), read("ivanRead", IVAN), read("miaRead", MIA),
                                rename("ivanRename", IVAN, "ivanRead", "Studio"))
                        .when(When.submit(rename("miaRename", MIA, "miaRead", "Atelier")))
                        .then(Then.rejected("conflicts with")),
                Scenario.named("anUnrelatedWriteDoesNotConflict")
                        .given(create(), read("read", IVAN),
                                Step.named("invoice").by(BOB).does(ActionSpec.asserts(
                                        TupleSpec.of(Ref.named("invoice"), NAME, ValueSpec.text("INV-1")))))
                        .when(When.submit(rename("rename", IVAN, "read", "Studio")))
                        .then(Then.accepted()),
                Scenario.named("aBlindWriteIsAccepted")
                        .given(create(), read("read", IVAN), rename("rename", IVAN, "read", "Studio"))
                        .when(When.submit(Step.named("blind").by(MIA).knew("create").does(ActionSpec.asserts(
                                TupleSpec.of(PROJECT, NAME, ValueSpec.text("Atelier"))))))
                        .then(Then.accepted()),
                Scenario.named("aWriteAboutAThingItDoesNotKnowIsRejected")
                        .given(create(), read("read", IVAN), rename("rename", IVAN, "read", "Studio"))
                        .when(When.submit(Step.named("blind").by(MIA).does(ActionSpec.asserts(
                                TupleSpec.of(PROJECT, NAME, ValueSpec.text("Atelier"))))))
                        .then(Then.rejected("STATEMENT_OUTSIDE_PAST")),
                Scenario.named("anUnknownFrontierIsRejected")
                        .given(create())
                        .when(When.submit(Step.named("read").by(IVAN).knew(Step.UNKNOWN)
                                .does(ActionSpec.read(PROJECT))))
                        .then(Then.rejected("unknown event")),
                Scenario.named("actionsTheAuthorityDoesNotTakeAreRejected")
                        .given(create())
                        .when(When.submit(Step.named("order").by(ANNA).knew("create")
                                .does(new ActionSpec.Raw(Identity.named("orders/place"), Json.Obj.of(Map.of())))))
                        .then(Then.rejected("projects takes only reads and asserts")),
                Scenario.named("retractingARetractionDoesNotRestoreTheStatement")
                        .given(create(), read("read", IVAN), rename("rename", IVAN, "read", "Studio"),
                                Step.named("unretract").by(IVAN).knew("rename")
                                        .does(ActionSpec.asserts(TupleSpec.retraction(Ref.tuple("rename", 0)))))
                        .when(When.read(PROJECT, "unretract"))
                        .then(Then.statements(TupleSpec.entity(),
                                TupleSpec.of(PROJECT, NAME, ValueSpec.text("Studio")))));
    }
}
