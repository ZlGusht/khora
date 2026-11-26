package khora.surfaces.contract;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import khora.events.Identity;
import khora.surfaces.Context;
import khora.surfaces.Interest;
import khora.surfaces.Surface;

/**
 * {@link Echo} against the surface contract, with the scenarios of {@code ReceivingTest} and the
 * delivery-order ones of {@code DeliveryTest}.
 *
 * <p>A scenario holds no ids, and Echo's interests are data, so the project is the named thing
 * {@code project} rather than a created entity. Echo labels the project once for every event about it
 * that it receives, with the event's first tuple as the label's value, so what it emits shows what it
 * received and in which order. The surface under test is attached by the {@code When}, after the
 * given steps, so these scenarios check delivery as the surface catches up; delivery to other
 * surfaces and to a surface attached before an event stays covered by {@code ReceivingTest} and
 * {@code DeliveryTest}.
 */
class EchoContractTest extends SurfaceContract {
    static final Context ANNA = new Context("anna", Optional.empty(), "g3", 10);

    static final Ref PROJECT = Ref.named("project");
    static final Ref NAME = Ref.named("name");
    static final Ref NOTE = Ref.named("note");
    static final Ref LABEL = Ref.named("khora/basic/label");

    /** Echo, labelling the project from every event about it it hears about. */
    static Echo labeller() {
        var aboutProject = Interest.about(Identity.named("project"));
        return new Echo("echo", Set.of(aboutProject), List.of(new Echo.Rule(aboutProject,
                List.of(new ActionSpec.Assert(List.of(TupleSpec.of(PROJECT, LABEL, ValueSpec.ref(Ref.received(0)))))))));
    }

    /** Asserts {@code (thing, attribute, value)}, knowing {@code knew}. */
    static Step tell(String step, Ref thing, Ref attribute, String value, String... knew) {
        return Step.named(step).by(ANNA).knew(knew)
                .does(new ActionSpec.Assert(List.of(TupleSpec.of(thing, attribute, ValueSpec.text(value)))));
    }

    /** Echo's labels for the first tuple of each of {@code steps}, in order. */
    static Then labels(String... steps) {
        return new Then.Emits(Arrays.stream(steps).<ActionSpec>map(step -> new ActionSpec.Assert(List.of(
                TupleSpec.of(PROJECT, LABEL, ValueSpec.ref(Ref.tuple(step, 0)))))).toList());
    }

    @Override
    protected Surface surface() {
        return labeller();
    }

    @Override
    protected List<Scenario> scenarios() {
        return List.of(
                Scenario.named("anAttachedSurfaceCatchesUpThenReceivesWhatMatches")
                        .given(tell("create", PROJECT, NAME, "Kitchen"),
                                tell("invoice", Ref.named("invoice"), NAME, "INV-1", "create"),
                                tell("rename", PROJECT, NAME, "Studio", "invoice"))
                        .when(new When.Attach(List.of()))
                        .then(labels("create", "rename")),
                Scenario.named("attachingFromAFrontierSkipsWhatIsInIt")
                        .given(tell("create", PROJECT, NAME, "Kitchen"),
                                tell("rename", PROJECT, NAME, "Studio", "create"),
                                tell("again", PROJECT, NAME, "Atelier", "rename"))
                        .when(new When.Attach(List.of("rename")))
                        .then(labels("again")),
                Scenario.named("readsAreReceivedToo")
                        .given(tell("create", PROJECT, NAME, "Kitchen"),
                                Step.named("read").by(ANNA).knew("create").does(new ActionSpec.Read(PROJECT)))
                        .when(new When.Attach(List.of("create")))
                        .then(labels("read")),
                Scenario.named("whatASurfaceEmitsIsAnEventCausedByWhatItKnew")
                        .given(tell("create", PROJECT, NAME, "Kitchen"),
                                tell("rename", PROJECT, NAME, "Studio", "create"))
                        .when(new When.Attach(List.of("create")))
                        .then(labels("rename")),
                Scenario.named("anEmissionReachesOthersAfterItsCause")
                        .given(tell("hello", PROJECT, NOTE, "hello"))
                        .when(new When.Attach(List.of()))
                        .then(labels("hello")),
                Scenario.named("catchingUpKeepsLogOrderAndSkipsOwnEmissions")
                        .given(tell("v0", PROJECT, NOTE, "v0"), tell("v1", PROJECT, NOTE, "v1"),
                                tell("v2", PROJECT, NOTE, "v2"))
                        .when(new When.Attach(List.of()))
                        .then(labels("v0", "v1", "v2")));
    }
}
