package khora.surfaces.contract;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import khora.events.Assertion;
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

    @Test
    void aSurfaceWhoseInterestsThrowDoesNotFailTheSubmit() {
        KnownGaps.judge(INTERESTS_THROW, () -> {
            var log = new InMemoryEventLog();
            var kernel = new Kernel(new Projects(), log, new Hlc(() -> 1000));
            var bad = new Surface() {
                @Override
                public String name() {
                    return "bad";
                }

                @Override
                public Set<Interest> interests() {
                    throw new IllegalStateException("interests broke");
                }

                @Override
                public List<khora.events.Action> receive(khora.events.Event event) {
                    return List.of();
                }
            };
            var echo = new RecordingSurface(new Echo("echo", Set.of(Interest.any()), List.of()), log);
            kernel.attach(bad, Frontier.empty());
            kernel.attach(echo, Frontier.empty());
            var creation = Assertion.action(Assertion.entity(), Assertion.of(Identity.local(0), Identity.named("name"),
                    new Assertion.Literal(new Json.Str("Kitchen"))));
            var answer = assertDoesNotThrow(() -> kernel.submit(creation,
                    new Context("anna", Optional.empty(), "g3", 10), Frontier.empty(), Map.of()));
            var accepted = assertInstanceOf(Answer.Accepted.class, answer);
            assertEquals(List.of(accepted.event()), echo.deliveries().stream().map(Run.Delivery::event).toList());
        });
    }
}
