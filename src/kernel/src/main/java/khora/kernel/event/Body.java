package khora.kernel.event;

/**
 * What an event records. An accepted action is one kind of body
 * ([[An accepted action is one kind of event]]).
 *
 * <p>Provisional: further kinds of event are added as further kinds of body.
 */
public sealed interface Body permits AcceptedAction {}
