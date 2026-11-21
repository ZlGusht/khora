package khora.kernel.surface;

/**
 * The contract every surface implements (ADR-0XY decision 1). Each read is one action.
 *
 * <p>Provisional. A surface that takes writes also implements {@link WritableSurface}; one that
 * reports changes implements {@link ChangingSurface}.
 */
public interface Surface {

    /** What the surface states about itself. */
    Declaration declares();

    /** Reads at the time the request states. */
    Result read(ReadRequest request, Context context);
}
