package khora.surfaces.contract;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;

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

/**
 * Kernel behaviour the contract tests found missing. Those fixed are plain regression tests; the
 * one still open is judged through {@link KnownGaps}.
 */
class KernelFindingsTest {
    static final String RETRACTED_FALSE = "a retracted attribute with another value does not retract";

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
        var accepted = assertInstanceOf(Answer.Accepted.class, kernel.submit(creation(), ANNA, Frontier.empty(), Map.of()));
        var received = echo.deliveries().stream().map(Run.Delivery::event).toList();
        assertEquals(List.of(accepted.event()), received);
    }

    @Test
    void aSurfaceWhoseInterestsThrowDoesNotFailTheSubmit() {
        // control: without the throwing surface the same submit is accepted and delivered to Echo
        submitTo(false);
        submitTo(true);
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
        play(Scenario.named("blind").given(CREATE)
                .when(When.submit(Step.named("retract").by(ANNA)
                        .does(ActionSpec.asserts(TupleSpec.retraction(Ref.tuple("create", 1))))))
                .then(Then.rejected("STATEMENT_OUTSIDE_PAST")));
    }

    @Test
    void aRetractionOfAMissingStatementIsRejected() {
        // control: the same retraction of a statement that exists is accepted (as in the test above)
        play(Scenario.named("exists").given(CREATE)
                .when(When.submit(Step.named("retract").by(ANNA).knew("create")
                        .does(ActionSpec.asserts(TupleSpec.retraction(Ref.tuple("create", 1))))))
                .then(Then.accepted()));
        play(Scenario.named("missing").given(CREATE)
                .when(When.submit(Step.named("retract").by(ANNA).knew("create")
                        .does(ActionSpec.asserts(TupleSpec.retraction(Ref.tuple("create", 5))))))
                .then(Then.rejected("UNKNOWN_STATEMENT")));
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
    void aTupleAboutAMissingLocalIdentityCannotBeBuilt() {
        // control: a tuple about a local identity its event carries is built and accepted
        play(Scenario.named("local").when(When.submit(CREATE)).then(Then.accepted()));
        var e = assertThrows(IllegalArgumentException.class, () -> Assertion.action(
                Assertion.of(Identity.local(5), Identity.named("name"), new Assertion.Literal(new Json.Str("x")))));
        assertEquals("no tuple /5 in an assert of 1", e.getMessage());
    }
}
