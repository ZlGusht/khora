package khora.kernel.event;

/**
 * The basic vocabulary: its one action, {@code assert}, and the well-known attributes the kernel
 * reads. An assert carries one {@link Assertion}.
 *
 * <p>Provisional: more well-known attributes come when the kernel needs them.
 */
public final class Basic {
    /** The definition of {@code assert}. */
    public static final EventId ASSERT = EventId.hashOf("khora/basic/assert");

    /** The attribute that marks an assertion as retracted. */
    public static final EventId RETRACTED = EventId.hashOf("khora/basic/retracted");

    /** The attribute that gives a thing a name for people. */
    public static final EventId LABEL = EventId.hashOf("khora/basic/label");

    private Basic() {}
}
