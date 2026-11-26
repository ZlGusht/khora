package khora.surfaces.contract;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import khora.events.Action;
import khora.events.Assertion;
import khora.events.Basic;
import khora.events.Event;
import khora.events.Frontier;
import khora.events.Hlc;
import khora.events.Identity;
import khora.events.json.Json;
import khora.events.log.InMemoryEventLog;
import khora.surfaces.Answer;
import khora.surfaces.Context;
import khora.surfaces.Interest;
import khora.surfaces.Kernel;
import khora.surfaces.Surface;
import khora.surfaces.stub.Projects;
import org.junit.jupiter.api.Test;

/** Kernel behaviour the contract tests found missing; each is judged through {@link KnownGaps}. */
class KernelGapsTest {
    static final String INTERESTS_THROW = "a surface whose interests throw does not fail the submit";
    static final String RETRACT_OUTSIDE_PAST = "a retraction of a statement outside its causal past is rejected";
    static final String RETRACT_MISSING = "a retraction of a statement that does not exist is rejected";
    static final String RETRACTED_FALSE = "a retracted attribute with another value does not retract";
    static final String DANGLING_LOCAL = "a tuple about a local identity its event does not carry is rejected";

    static final Context ANNA = new Context("anna", Optional.empty(), "g3", 10);

    static Action creation() {
        return Assertion.action(Assertion.entity(), Assertion.of(Identity.local(0), Identity.named("name"),
                new Assertion.Literal(new Json.Str("Kitchen"))));
    }

    /** Submits a creation to a kernel with the surfaces attached; Echo receives it alone. */
    private static void submitTo(boolean withThrowing) {
        var log = new InMemoryEventLog();
        var kernel = new Kernel(new Projects(), log, new Hlc(() -> 1000));
        var echo = new RecordingSurface(new Echo("echo", Set.of(Interest.any()), List.of()), log);
        if (withThrowing) {
            kernel.attach(new Surface() {
                @Override
                public String name() {
                    return "bad";
                }

                @Override
                public Set<Interest> interests() {
                    throw new IllegalStateException("interests broke");
                }

                @Override
                public List<Action> receive(Event event) {
                    return List.of();
                }
            }, Frontier.empty());
        }
        kernel.attach(echo, Frontier.empty());
        Answer answer;
        try {
            answer = kernel.submit(creation(), ANNA, Frontier.empty(), Map.of());
        } catch (IllegalStateException e) {
            if ("interests broke".equals(e.getMessage())) {
                throw new AssertionError("the submit failed because a surface's interests threw", e);
            }
            throw e;
        }
        var accepted = assertInstanceOf(Answer.Accepted.class, answer);
        var received = echo.deliveries().stream().map(Run.Delivery::event).toList();
        assertEquals(List.of(accepted.event()), received);
    }

    @Test
    void aSurfaceWhoseInterestsThrowDoesNotFailTheSubmit() {
        // control: without the throwing surface the same submit is accepted and delivered to Echo
        submitTo(false);
        KnownGaps.judge(INTERESTS_THROW, () -> submitTo(true));
    }

    static final Ref NAME = Ref.named("name");
    static final Ref PROJECT = Ref.tuple("create", 0);
    static final Step CREATE = Step.named("create").by(ANNA).does(ActionSpec.asserts(TupleSpec.entity(),
            TupleSpec.of(Ref.local(0), NAME, ValueSpec.text("Kitchen"))));

    /** Plays {@code s} with {@link Projects} and checks its expectation. */
    private static void play(Scenario s) {
        AuthorityContract.expect(s, ScenarioRunner.run(s, new Projects(), Optional.empty()));
    }

    @Test
    void aRetractionOutsideItsCausalPastIsRejected() {
        // control: a retraction that knew the statement is accepted
        play(Scenario.named("knew").given(CREATE)
                .when(When.submit(Step.named("retract").by(ANNA).knew("create")
                        .does(ActionSpec.asserts(TupleSpec.retraction(Ref.tuple("create", 1))))))
                .then(Then.accepted()));
        KnownGaps.judge(RETRACT_OUTSIDE_PAST, () -> play(Scenario.named("blind").given(CREATE)
                .when(When.submit(Step.named("retract").by(ANNA)
                        .does(ActionSpec.asserts(TupleSpec.retraction(Ref.tuple("create", 1))))))
                .then(Then.rejected())));
    }

    @Test
    void aRetractionOfAMissingStatementIsRejected() {
        // control: the same retraction of a statement that exists is accepted (as in the test above)
        play(Scenario.named("exists").given(CREATE)
                .when(When.submit(Step.named("retract").by(ANNA).knew("create")
                        .does(ActionSpec.asserts(TupleSpec.retraction(Ref.tuple("create", 1))))))
                .then(Then.accepted()));
        KnownGaps.judge(RETRACT_MISSING, () -> play(Scenario.named("missing").given(CREATE)
                .when(When.submit(Step.named("retract").by(ANNA).knew("create")
                        .does(ActionSpec.asserts(TupleSpec.retraction(Ref.tuple("create", 5))))))
                .then(Then.rejected())));
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

    @Test
    void aTupleAboutAMissingLocalIdentityIsRejected() {
        // control: a tuple about a local identity its event carries is accepted
        play(Scenario.named("local").when(When.submit(CREATE)).then(Then.accepted()));
        KnownGaps.judge(DANGLING_LOCAL, () -> play(Scenario.named("dangling")
                .when(When.submit(Step.named("dangling").by(ANNA)
                        .does(ActionSpec.asserts(TupleSpec.of(Ref.local(5), NAME, ValueSpec.text("x"))))))
                .then(Then.rejected())));
    }
}
