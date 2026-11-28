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
 * that it receives, with the literal {@code seen} as the label's value: a label never names a tuple
 * the received event may lack (a read carries none). What it emits is compared delivery by delivery,
 * so each label still shows which event it answers, and in which order. Scenarios that attach Echo
 * among the given steps check live delivery (and catching up on what came before); those that attach it as the {@code When} check catching up
 * alone. Delivery among several surfaces stays covered by {@code DeliveryTest}.
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
                List.of(label()))));
    }

    /** Asserts {@code (thing, attribute, value)}. */
    static Step tell(String step, Ref thing, Ref attribute, String value) {
        return Step.named(step).by(ANNA)
                .does(ActionSpec.asserts(TupleSpec.of(thing, attribute, ValueSpec.text(value))));
    }

    /** Echo's label of the project, the same whatever event it answers. */
    static ActionSpec label() {
        return ActionSpec.asserts(TupleSpec.of(PROJECT, LABEL, ValueSpec.text("seen")));
    }

    /** Echo answers the event of each of {@code steps}, in order, with its label. */
    static Then labels(String... steps) {
        return new Then.Emits(Arrays.stream(steps).map(step -> Then.Emits.after(step, label())).toList());
    }

    @Override
    protected Surface surface() {
        return labeller();
    }

    @Override
    protected List<Scenario> scenarios() {
        return List.of(
                Scenario.named("anAttachedSurfaceCatchesUpThenReceivesWhatMatches")
                        .given(tell("create", PROJECT, NAME, "Kitchen"), Given.attach(),
                                tell("invoice", Ref.named("invoice"), NAME, "INV-1"))
                        .when(When.submit(tell("rename", PROJECT, NAME, "Studio")))
                        .then(labels("create", "rename")),
                Scenario.named("attachingFromAPositionSkipsWhatIsBeforeIt")
                        .given(tell("create", PROJECT, NAME, "Kitchen"),
                                tell("rename", PROJECT, NAME, "Studio"),
                                tell("again", PROJECT, NAME, "Atelier"))
                        .when(When.attach("rename"))
                        .then(labels("again")),
                Scenario.named("readsAreReceivedToo")
                        .given(tell("create", PROJECT, NAME, "Kitchen"), Given.attach("create"))
                        .when(When.submit(Step.named("read").by(ANNA).does(ActionSpec.read(PROJECT))))
                        .then(labels("read")),
                Scenario.named("whatASurfaceEmitsIsAnEventTriggeredByWhatItReceived")
                        .given(tell("create", PROJECT, NAME, "Kitchen"), Given.attach("create"))
                        .when(When.submit(tell("rename", PROJECT, NAME, "Studio")))
                        .then(labels("rename")),
                Scenario.named("anEmissionComesAfterItsTrigger")
                        .given(Given.attach())
                        .when(When.submit(tell("hello", PROJECT, NOTE, "hello")))
                        .then(labels("hello")),
                Scenario.named("catchingUpKeepsLogOrderAndSkipsOwnEmissions")
                        .given(tell("v0", PROJECT, NOTE, "v0"), tell("v1", PROJECT, NOTE, "v1"),
                                tell("v2", PROJECT, NOTE, "v2"))
                        .when(When.attach())
                        .then(labels("v0", "v1", "v2")));
    }
}
