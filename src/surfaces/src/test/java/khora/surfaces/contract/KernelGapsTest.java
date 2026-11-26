package khora.surfaces.contract;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import khora.events.Action;
import khora.events.Assertion;
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

    static final Context ANNA = new Context("anna", Optional.empty(), "g3", 10);

    static Action creation() {
        return Assertion.action(Assertion.entity(), Assertion.of(Identity.local(0), Identity.named("name"),
                new Assertion.Literal(new Json.Str("Kitchen"))));
    }

    /** Submits a creation to a kernel with the surfaces attached; returns what Echo received. */
    private static List<Event> submitTo(boolean withThrowing) {
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
        return received;
    }

    @Test
    void aSurfaceWhoseInterestsThrowDoesNotFailTheSubmit() {
        // control: without the throwing surface the same submit is accepted and delivered to Echo
        submitTo(false);
        KnownGaps.judge(INTERESTS_THROW, () -> submitTo(true));
    }
}
