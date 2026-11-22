package khora.kernel.event;

/**
 * The basic vocabulary: its one action, {@code assert}, and the well-known attributes the kernel
 * reads. An assert carries one or more {@link Assertion}s.
 *
 * <p>Provisional: more well-known attributes come when the kernel needs them.
 */
public final class Basic {
    /** The definition of {@code assert}. */
    public static final Identity ASSERT = Identity.named("khora/basic/assert");

    /** The attribute that marks an assertion as retracted. */
    public static final Identity RETRACTED = Identity.named("khora/basic/retracted");

    /** The attribute that gives a thing a name for people. */
    public static final Identity LABEL = Identity.named("khora/basic/label");

    private Basic() {}
}
