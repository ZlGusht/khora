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
        return Step.named("create").by(ANNA).does(new ActionSpec.Assert(List.of(TupleSpec.entity(),
                TupleSpec.of(Ref.local(0), NAME, ValueSpec.text("Kitchen")))));
    }

    static Step read(String name, Context who) {
        return Step.named(name).by(who).knew("create").does(new ActionSpec.Read(PROJECT));
    }

    /** Renames the project from Kitchen, based on the read {@code basedOn}. */
    static Step rename(String name, Context who, String basedOn, String to) {
        return Step.named(name).by(who).knew(basedOn).link("based_on", basedOn)
                .does(new ActionSpec.Assert(List.of(TupleSpec.retraction(Ref.tuple("create", 1)),
                        TupleSpec.of(PROJECT, NAME, ValueSpec.text(to)))));
    }

    static Then rejected(String reason) {
        return new Then.Rejected(Optional.of(reason));
    }

    @Override
    protected Authority authority() {
        return new Projects();
    }

    @Override
    protected List<Scenario> scenarios() {
        return List.of(
                Scenario.named("anAcceptedActionIsAnEventByItsCaller")
                        .when(new When.Submit(create()))
                        .then(new Then.Accepted()),
                Scenario.named("aReadIsAnEventAndAnswersWithStatements")
                        .given(create())
                        .when(new When.Submit(read("read", IVAN)))
                        .then(new Then.Statements(List.of(TupleSpec.entity(),
                                TupleSpec.of(PROJECT, NAME, ValueSpec.text("Kitchen"))))),
                Scenario.named("aWriteFollowsTheReadItWasBasedOn")
                        .given(create(), read("read", IVAN))
                        .when(new When.Submit(rename("rename", IVAN, "read", "Studio")))
                        .then(new Then.Accepted()),
                Scenario.named("theRenamedNameHoldsAtTheRename")
                        .given(create(), read("read", IVAN), rename("rename", IVAN, "read", "Studio"))
                        .when(new When.Read(PROJECT, List.of("rename")))
                        .then(new Then.Statements(List.of(TupleSpec.entity(),
                                TupleSpec.of(PROJECT, NAME, ValueSpec.text("Studio"))))),
                Scenario.named("aConcurrentRenameOfTheSameNameIsRejected")
                        .given(create(), read("ivanRead", IVAN), read("miaRead", MIA),
                                rename("ivanRename", IVAN, "ivanRead", "Studio"))
                        .when(new When.Submit(rename("miaRename", MIA, "miaRead", "Atelier")))
                        .then(rejected("conflicts with")),
                Scenario.named("anUnrelatedWriteDoesNotConflict")
                        .given(create(), read("read", IVAN),
                                Step.named("invoice").by(BOB).does(new ActionSpec.Assert(List.of(
                                        TupleSpec.of(Ref.named("invoice"), NAME, ValueSpec.text("INV-1"))))))
                        .when(new When.Submit(rename("rename", IVAN, "read", "Studio")))
                        .then(new Then.Accepted()),
                Scenario.named("aBlindWriteIsAccepted")
                        .given(create(), read("read", IVAN), rename("rename", IVAN, "read", "Studio"))
                        .when(new When.Submit(Step.named("blind").by(MIA).does(new ActionSpec.Assert(List.of(
                                TupleSpec.of(PROJECT, NAME, ValueSpec.text("Atelier")))))))
                        .then(new Then.Accepted()),
                Scenario.named("anUnknownFrontierIsRejected")
                        .given(create())
                        .when(new When.Submit(Step.named("read").by(IVAN).knew(Step.UNKNOWN)
                                .does(new ActionSpec.Read(PROJECT))))
                        .then(rejected("unknown event")),
                Scenario.named("actionsTheAuthorityDoesNotTakeAreRejected")
                        .given(create())
                        .when(new When.Submit(Step.named("order").by(ANNA).knew("create")
                                .does(new ActionSpec.Raw(Identity.named("orders/place"), Json.Obj.of(Map.of())))))
                        .then(rejected("projects takes only reads and asserts")));
    }
}
