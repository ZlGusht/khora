package khora.surfaces.contract;

import java.util.List;
import java.util.Objects;

/**
 * A named scenario: given steps already in the log (and, if it says where, the surface under test
 * attached among them), a thing done, and what is expected.
 */
public record Scenario(String name, List<Given> given, When when, Then then) {
    public Scenario {
        Check.name(name);
        given = List.copyOf(given);
        Objects.requireNonNull(when, "when");
        Objects.requireNonNull(then, "then");
    }

    /** The given steps, without where the surface is attached. */
    public List<Step> steps() {
        return given.stream().<Step>mapMulti((g, out) -> {
            if (g instanceof Step step) {
                out.accept(step);
            }
        }).toList();
    }

    public static Builder named(String name) {
        return new Builder(name);
    }

    /** Builds a scenario: {@code Scenario.named(..).given(..).when(..).then(..)}. */
    public static final class Builder {
        private final String name;
        private List<Given> given = List.of();
        private When when;

        private Builder(String name) {
            this.name = name;
        }

        public Builder given(Given... steps) {
            given = List.of(steps);
            return this;
        }

        public Builder when(When when) {
            this.when = when;
            return this;
        }

        public Scenario then(Then then) {
            return new Scenario(name, given, when, then);
        }
    }
}
