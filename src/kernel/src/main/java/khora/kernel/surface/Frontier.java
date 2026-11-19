package khora.kernel.surface;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * One position per origin a read depends on (ADR-0XY, Terms).
 *
 * <p>Provisional: origins are plain names and positions are plain counts.
 *
 * @param positions origin to position; every position is zero or more
 */
public record Frontier(Map<String, Long> positions) {
    public Frontier {
        Objects.requireNonNull(positions, "positions");
        positions = Map.copyOf(positions);
        positions.forEach((origin, position) -> {
            if (position < 0) {
                throw new IllegalArgumentException("position of " + origin + " is negative: " + position);
            }
        });
    }

    /** The frontier before anything happened. */
    public static Frontier empty() {
        return new Frontier(Map.of());
    }

    /** A new frontier with {@code origin} at {@code position}. */
    public Frontier advance(String origin, long position) {
        Objects.requireNonNull(origin, "origin");
        var next = new HashMap<>(positions);
        next.put(origin, position);
        return new Frontier(next);
    }

    /** Whether every origin in {@code wanted} is here at an equal or later position. */
    public boolean reaches(Frontier wanted) {
        Objects.requireNonNull(wanted, "wanted");
        return wanted.positions.entrySet().stream()
                .allMatch(e -> positions.getOrDefault(e.getKey(), -1L) >= e.getValue());
    }
}
