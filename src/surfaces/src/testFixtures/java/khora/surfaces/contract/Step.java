package khora.surfaces.contract;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import khora.surfaces.Context;

/**
 * One event of a scenario: who submits it, which earlier steps it knew (its causes), the named
 * links it carries (name to steps), and its action.
 */
public record Step(String name, Context who, List<String> knew, Map<String, List<String>> links,
        ActionSpec action) implements Given {
    /**
     * A name for {@code knew} and links that stands for an event no log holds. No step may take it.
     * It lets a scenario say "a frontier the kernel does not know" without writing an event id.
     */
    public static final String UNKNOWN = "?unknown";

    public Step {
        Check.name(name);
        Objects.requireNonNull(who, "who");
        Objects.requireNonNull(action, "action");
        knew = List.copyOf(knew);
        var copy = new LinkedHashMap<String, List<String>>();
        links.forEach((k, v) -> copy.put(k, List.copyOf(v)));
        links = java.util.Collections.unmodifiableMap(copy);
    }

    public static Builder named(String name) {
        return new Builder(name);
    }

    /** Builds a step: {@code Step.named(..).by(..).knew(..).link(..).does(..)}. */
    public static final class Builder {
        private final String name;
        private Context who;
        private final List<String> knew = new ArrayList<>();
        private final Map<String, List<String>> links = new LinkedHashMap<>();

        private Builder(String name) {
            this.name = name;
        }

        public Builder by(Context who) {
            this.who = who;
            return this;
        }

        public Builder knew(String... steps) {
            knew.addAll(List.of(steps));
            return this;
        }

        public Builder link(String name, String... steps) {
            links.put(name, List.of(steps));
            return this;
        }

        public Step does(ActionSpec action) {
            return new Step(name, who, knew, links, action);
        }
    }
}
