package khora.surfaces.contract;

import java.util.List;
import java.util.Objects;

/** A named scenario: given steps already in the log, a thing done, and what is expected. */
public record Scenario(String name, List<Step> given, When when, Then then) {
    public Scenario {
        Check.name(name);
        given = List.copyOf(given);
        Objects.requireNonNull(when, "when");
        Objects.requireNonNull(then, "then");
    }

    public static Builder named(String name) {
        return new Builder(name);
    }

    /** Builds a scenario: {@code Scenario.named(..).given(..).when(..).then(..)}. */
    public static final class Builder {
        private final String name;
        private List<Step> given = List.of();
        private When when;

        private Builder(String name) {
            this.name = name;
        }

        public Builder given(Step... steps) {
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
