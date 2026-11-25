package khora.surfaces.contract;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import khora.events.Assertion;
import khora.events.Identity;
import khora.events.Statement;
import khora.events.json.Json;
import khora.surfaces.Answer;
import khora.surfaces.Authority;
import org.junit.jupiter.api.DynamicContainer;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;

/**
 * The contract every authority passes. A subclass gives the authority and its scenarios; each
 * scenario runs through a fresh kernel and becomes a container named after it, holding the test
 * {@code expectation}: its {@link Then} holds.
 */
public abstract class AuthorityContract {

    /** A new instance of the authority under test; called once per scenario run. */
    protected abstract Authority authority();

    /** The scenarios the authority is held to. Their names are distinct. */
    protected abstract List<Scenario> scenarios();

    /**
     * One container per scenario.
     *
     * @throws IllegalArgumentException if two scenarios share a name
     */
    @TestFactory
    public final Stream<DynamicContainer> contract() {
        var scenarios = scenarios();
        var names = new HashSet<String>();
        for (var s : scenarios) {
            if (!names.add(s.name())) {
                throw new IllegalArgumentException("two scenarios are named " + s.name());
            }
        }
        return scenarios.stream().map(s -> DynamicContainer.dynamicContainer(s.name(), Stream.of(
                DynamicTest.dynamicTest("expectation", () -> expect(s,
                        ScenarioRunner.run(s, authority(), Optional.empty()))))));
    }

    /** Checks that the run of {@code s} meets its {@link Then}. */
    static void expect(Scenario s, Run run) {
        switch (s.then()) {
            case Then.Accepted ignored -> {
                if (!(answer(s, run) instanceof Answer.Accepted)) {
                    fail(s.name() + ": expected accepted, was " + answer(s, run));
                }
            }
            case Then.Rejected rejected -> {
                if (!(answer(s, run) instanceof Answer.Rejected r)) {
                    fail(s.name() + ": expected rejected, was accepted");
                    return;
                }
                rejected.reasonContains().ifPresent(text -> assertTrue(r.reason().contains(text),
                        s.name() + ": the reason \"" + r.reason() + "\" does not contain \"" + text + "\""));
            }
            case Then.Statements expected -> {
                switch (s.when()) {
                    case When.Read ignored -> { }
                    case When.Submit ignored -> {
                        if (answer(s, run) instanceof Answer.Rejected r) {
                            fail(s.name() + ": expected the read's statements, but it was rejected: " + r.reason());
                        }
                    }
                    case When.Attach ignored -> fail(s.name() + ": an attach answers with no statements");
                }
                var bindings = run.bindings();
                assertEquals(expected.tuples().stream()
                                .map(t -> symbolic(t.resolve(bindings, Optional.empty()), bindings)).toList(),
                        symbolic(run.statements(), bindings), s.name() + ": statements");
            }
            case Then.Emits ignored -> fail(s.name() + ": the authority contract does not check emissions");
        }
    }

    private static Answer answer(Scenario s, Run run) {
        return run.answer().orElseThrow(() -> new AssertionError(s.name() + ": expects an answer, but submits nothing"));
    }

    /** The statements as symbolic tuples: identity, attribute and value; never the statement's id. */
    public static List<String> symbolic(List<Statement> statements, Bindings bindings) {
        return statements.stream().map(st -> symbolic(st.assertion(), bindings)).toList();
    }

    /** A tuple as {@code (identity, attribute, value)}, with identities rendered by {@code bindings}. */
    public static String symbolic(Assertion tuple, Bindings bindings) {
        return "(" + tuple.identity().map(bindings::render).orElse("")
                + ", " + tuple.attribute().map(bindings::render).orElse("")
                + ", " + tuple.value().map(v -> switch (v) {
                    case Assertion.Ref r -> render(r.id(), bindings);
                    case Assertion.Literal l -> switch (l.value()) {
                        case Json.Str str -> "\"" + str.value() + "\"";
                        case Json.Int i -> Long.toString(i.value());
                        default -> l.value().toString();
                    };
                }).orElse("") + ")";
    }

    private static String render(Identity id, Bindings bindings) {
        return "ref " + bindings.render(id);
    }
}
