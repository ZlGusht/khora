package khora.surfaces.contract;

import java.util.Optional;
import khora.events.Basic;
import khora.surfaces.Context;
import khora.surfaces.stub.Projects;
import org.junit.jupiter.api.Test;

/**
 * Kernel behaviour the contract tests found missing and #36 has not fixed yet, judged through
 * {@link KnownGaps}. The other findings of the review of #36 were fixed there and are covered by
 * its own tests ({@code CausalityTest}, {@code AssertionTest}, {@code DeliveryTest}) and by the
 * contracts.
 */
class KernelGapsTest {
    static final String RETRACTED_FALSE = "a retracted attribute with another value does not retract";

    static final Context ANNA = new Context("anna", Optional.empty(), "g3", 10);
    static final Ref NAME = Ref.named("name");
    static final Ref PROJECT = Ref.tuple("create", 0);
    static final Step CREATE = Step.named("create").by(ANNA).does(ActionSpec.asserts(TupleSpec.entity(),
            TupleSpec.of(Ref.local(0), NAME, ValueSpec.text("Kitchen"))));

    /** Plays {@code s} with {@link Projects} and checks its expectation. */
    private static void play(Scenario s) {
        AuthorityContract.expect(s, ScenarioRunner.run(s, new Projects(), Optional.empty()));
    }

    @Test
    void aRetractedAttributeWithAnotherValueDoesNotRetract() {
        var notRetracted = Step.named("notRetracted").by(ANNA).knew("create").does(ActionSpec.asserts(
                TupleSpec.of(Ref.tuple("create", 1), Ref.known(Basic.RETRACTED), ValueSpec.text("false"))));
        // control: without that tuple the name holds. The gap assumes the kernel accepts the tuple; a fix that
        // rejects it at submit instead fails the given step, and this test then fails outright: rewrite it then.
        play(Scenario.named("plain").given(CREATE).when(When.read(PROJECT, "create"))
                .then(Then.statements(TupleSpec.entity(), TupleSpec.of(PROJECT, NAME, ValueSpec.text("Kitchen")))));
        KnownGaps.judge(RETRACTED_FALSE, () -> play(Scenario.named("false").given(CREATE, notRetracted)
                .when(When.read(PROJECT, "notRetracted"))
                .then(Then.statements(TupleSpec.entity(), TupleSpec.of(PROJECT, NAME, ValueSpec.text("Kitchen"))))));
    }
}
