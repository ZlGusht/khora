package khora.surfaces.contract;

import java.util.Objects;
import java.util.Optional;
import khora.surfaces.Context;

/** One event of a scenario: who submits it, the earlier step it responds to (its trigger), and its action. */
public record Step(String name, Context who, Optional<String> trigger, ActionSpec action) implements Given {
    /**
     * A name for a trigger that stands for an event no log holds. No step may take it. It lets a
     * scenario say "a trigger the kernel does not know" without writing an event id.
     */
    public static final String UNKNOWN = "?unknown";

    public Step {
        Check.name(name);
        Objects.requireNonNull(who, "who");
        Objects.requireNonNull(trigger, "trigger");
        Objects.requireNonNull(action, "action");
    }

    public static Builder named(String name) {
        return new Builder(name);
    }

    /** Builds a step: {@code Step.named(..).by(..).after(..).does(..)}. */
    public static final class Builder {
        private final String name;
        private Context who;
        private Optional<String> trigger = Optional.empty();

        private Builder(String name) {
            this.name = name;
        }

        public Builder by(Context who) {
            this.who = who;
            return this;
        }

        /** In response to the event of {@code step}. */
        public Builder after(String step) {
            this.trigger = Optional.of(step);
            return this;
        }

        public Step does(ActionSpec action) {
            return new Step(name, who, trigger, action);
        }
    }
}
