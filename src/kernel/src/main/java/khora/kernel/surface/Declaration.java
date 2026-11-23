package khora.kernel.surface;

import java.util.Objects;

/**
 * What a surface states about its capabilities (ADR-0XY decision 2).
 *
 * <p>Provisional: the ADR also lists an {@code order} capability. It is left out until its meaning
 * and values are decided. Operations offered and what the surface builds on (ADR-0XY, Terms) are
 * left out too.
 *
 * @param authoritative whether the surface is the authority for what it holds
 * @param effects how its effects come about
 * @param history how much of its past it keeps
 * @param reads whether its reads can be repeated
 * @param writes which writes it takes
 */
public record Declaration(boolean authoritative, Effects effects, History history, Reads reads, Writes writes) {
    public Declaration {
        Objects.requireNonNull(effects, "effects");
        Objects.requireNonNull(history, "history");
        Objects.requireNonNull(reads, "reads");
        Objects.requireNonNull(writes, "writes");
    }
}
