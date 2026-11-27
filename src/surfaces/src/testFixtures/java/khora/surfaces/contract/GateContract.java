package khora.surfaces.contract;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import khora.events.Assertion;
import khora.events.Basic;
import khora.events.Identity;
import khora.events.Statement;
import khora.events.json.Json;
import khora.surfaces.Answer;
import khora.surfaces.Gate;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.DynamicContainer;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;

/**
 * The contract every gate passes. A subclass gives the gate and its scenarios; each scenario runs
 * once through a fresh kernel and becomes a container named after it, holding the test
 * {@code expectation} (its {@link Then} holds) and one test per gate rule, each judged over every
 * decision of the run:
 *
 * <ul>
 *   <li>{@value #REASON}: a refusal's reason is not blank;
 *   <li>{@value #READ}: a read is not refused;
 *   <li>{@value #SAME}: the scenario is played a second time from scratch, with a new gate, and it
 *       decides the same actions the same way, compared symbolically.
 * </ul>
 *
 * <p>A gate judges against what it has taken in, never the log, so it cannot change the log while
 * deciding.
 *
 * <p>A scenario the runner cannot play fails every test of its container with the runner's message.
 * A scenario in which the gate was never asked (it guards none of its actions) aborts the rule tests
 * rather than pass them with nothing to judge.
 */
public abstract class GateContract {

    public static final String REASON = "a refusal has a reason";
    public static final String READ = "a read is not refused";
    public static final String SAME = "the same events give the same verdicts";

    /** A new instance of the gate under test; called once per scenario run. */
    protected abstract Gate gate();

    /** The scenarios the gate is held to. Their names are distinct. */
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
        return scenarios.stream().map(s -> {
            var run = new Once<>(() -> play(s));
            return DynamicContainer.dynamicContainer(s.name(), Stream.of(
                    DynamicTest.dynamicTest("expectation", () -> expect(s, run.get())),
                    rule(REASON, s, run, (scenario, r) -> r.decisions().forEach(d -> refusalHasReason(scenario, r, d))),
                    rule(READ, s, run, (scenario, r) -> r.decisions().forEach(d -> readIsNotRefused(scenario, r, d))),
                    rule(SAME, s, run, (scenario, r) -> sameTwice(scenario, r, play(scenario)))));
        });
    }

    private Run play(Scenario s) {
        return ScenarioRunner.run(s, List.of(gate()), Optional.empty());
    }

    private interface Rule {
        void check(Scenario s, Run run);
    }

    private static DynamicTest rule(String name, Scenario s, Once<Run> once, Rule rule) {
        return DynamicTest.dynamicTest(name, () -> {
            var run = once.get();
            if (run.decisions().isEmpty()) {
                Assumptions.abort("the gate was not asked in this scenario");
            }
            rule.check(s, run);
        });
    }

    /** A decision, for messages: the action and who it was by. */
    private static String about(Scenario s, Run run, Run.Decision d) {
        return s.name() + ": " + d.by().principal() + "'s " + SurfaceContract.symbolic(List.of(d.action()), run.bindings());
    }

    static void refusalHasReason(Scenario s, Run run, Run.Decision d) {
        if (d.verdict() instanceof Gate.Refuse r && r.reason().isBlank()) {
            fail(about(s, run, d) + ": refused with a blank reason");
        }
    }

    /**
     * A read is not refused. Stage 1 has no access checks, so the only reason to refuse a read would
     * be a conflict, and a read conflicts with nothing. When access checks arrive, this must narrow to
     * "not refused for a conflict".
     */
    static void readIsNotRefused(Scenario s, Run run, Run.Decision d) {
        if (Basic.readOf(d.action()).isPresent() && d.verdict() instanceof Gate.Refuse r) {
            fail(about(s, run, d) + ": a read was refused: " + r.reason());
        }
    }

    static void sameTwice(Scenario s, Run first, Run second) {
        assertEquals(verdicts(first), verdicts(second),
                s.name() + ": played again from scratch, the gate decided something else");
    }

    /** Each decision as the action and whether it passed, symbolically. */
    private static List<String> verdicts(Run run) {
        return run.decisions().stream()
                .map(d -> d.by().principal() + " " + SurfaceContract.symbolic(List.of(d.action()), run.bindings())
                        + " -> " + (d.verdict() instanceof Gate.Pass ? "pass" : "refuse"))
                .toList();
    }

    /** Checks that the run of {@code s} meets its {@link Then}. */
    static void expect(Scenario s, Run run) {
        switch (s.then()) {
            case Then.Logged ignored -> {
                if (!(answer(s, run) instanceof Answer.Logged)) {
                    fail(s.name() + ": expected logged, was " + answer(s, run));
                }
            }
            case Then.Refused refused -> {
                if (!(answer(s, run) instanceof Answer.Refused r)) {
                    fail(s.name() + ": expected refused, was logged");
                    return;
                }
                refused.reasonContains().ifPresent(text -> assertTrue(r.reason().contains(text),
                        s.name() + ": the reason \"" + r.reason() + "\" does not contain \"" + text + "\""));
            }
            case Then.Statements expected -> {
                switch (s.when()) {
                    case When.Read ignored -> { }
                    case When.Submit submit -> {
                        if (!(submit.step().action() instanceof ActionSpec.Read)) {
                            fail(s.name() + ": broken scenario: expects statements, but submits "
                                    + submit.step().name() + ", which is not a read");
                        }
                        if (answer(s, run) instanceof Answer.Refused r) {
                            fail(s.name() + ": expected the read's statements, but it was refused: " + r.reason());
                        }
                    }
                    case When.Attach ignored -> fail(s.name() + ": an attach answers with no statements");
                }
                var bindings = run.bindings();
                assertEquals(expected.tuples().stream()
                                .map(t -> symbolic(t.resolve(bindings, Optional.empty()), bindings)).toList(),
                        symbolic(run.statements(), bindings), s.name() + ": statements");
            }
            case Then.Emits ignored -> fail(s.name() + ": the gate contract does not check emissions");
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
