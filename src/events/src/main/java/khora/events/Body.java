package khora.events;

/**
 * What an event records. An emitted action is one kind of body.
 *
 * <p>Provisional: further kinds of event are added as further kinds of body.
 */
public sealed interface Body permits Emitted {

    /** The kind of event this body makes, as named in the event's JSON. */
    String kind();
}
