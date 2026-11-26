package khora.surfaces.contract;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import khora.events.AcceptedAction;
import khora.events.Assertion;
import khora.events.Basic;
import khora.events.Identity;
import khora.events.Statement;
import khora.events.json.Json;
import khora.surfaces.Answer;
import khora.surfaces.Authority;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.DynamicContainer;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;

/**
 * The contract every authority passes. A subclass gives the authority and its scenarios; each
 * scenario runs once through a fresh kernel and becomes a container named after it, holding the test
 * {@code expectation} (its {@link Then} holds) and one test per authority rule, each judged over
 * every decision of the run:
 *
 * <ul>
 *   <li>{@value #LOG}: it neither appends to, follows nor closes the log while deciding;
 *   <li>{@value #TWICE}: asked twice on the same log, it gives the same verdict;
 *   <li>{@value #REASON}: a rejection's reason is not blank;
 *   <li>{@value #READ}: see {@link #readIsNotRejected}.
 * </ul>
 *
 * <p>To judge {@value #TWICE}, {@code decide} is called twice for every submit, each time on the same
 * log, so it must be a function of the candidate and the log: an authority that keeps state between
 * calls sees two calls per submit.
 *
 * <p>Authority rules are judged in an authority's own contract run, on the steps its scenarios
 * submit. A surface contract run records the decisions on the actions its surface emitted, but does
 * not judge them: an authority is held to its rules on such actions by scenarios in its own contract.
 *
 * <p>The reason and read rules judge the verdict the authority answered with, the first; a second
 * verdict that differs from it is caught by {@value #TWICE}.
 *
 * <p>A scenario the runner cannot play fails every test of its container with the runner's message.
 * A scenario in which the authority was never asked (the kernel rejected everything before it) aborts
 * the four rule tests rather than pass them with nothing to judge.
 * Rules listed in {@link KnownGaps} are judged by it.
 */
public abstract class AuthorityContract {

    public static final String LOG = "authority does not change the log";
    public static final String TWICE = "authority decides the same twice";
    public static final String REASON = "a rejection has a reason";
    public static final String READ = "a read is not rejected for a conflict";

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
        return scenarios.stream().map(s -> {
            var run = new Once<>(() -> ScenarioRunner.run(s, authority(), Optional.empty()));
            return DynamicContainer.dynamicContainer(s.name(), Stream.of(
                    DynamicTest.dynamicTest("expectation", () -> expect(s, run.get())),
                    rule(LOG, s, run, AuthorityContract::logIsUnchanged),
                    rule(TWICE, s, run, AuthorityContract::sameTwice),
                    rule(REASON, s, run, AuthorityContract::rejectionHasReason),
                    rule(READ, s, run, AuthorityContract::readIsNotRejected)));
        });
    }

    private interface Rule {
        void check(Scenario s, int index, Run.Decision decision);
    }

    private static DynamicTest rule(String name, Scenario s, Once<Run> once, Rule rule) {
        return DynamicTest.dynamicTest(name, () -> {
            var run = once.get();
            if (run.decisions().isEmpty()) {
                Assumptions.abort("the authority was not asked in this scenario");
            }
            KnownGaps.judge(name, () -> {
                var decisions = run.decisions();
                for (int i = 0; i < decisions.size(); i++) {
                    rule.check(s, i, decisions.get(i));
                }
            });
        });
    }

    /** Decision {@code index} of {@code s}, for messages. */
    private static String about(Scenario s, int index, Run.Decision d) {
        var action = ((AcceptedAction) d.candidate().body()).action();
        return s.name() + ": decision " + index + " (on " + action.definition().value() + ")";
    }

    static void logIsUnchanged(Scenario s, int index, Run.Decision d) {
        if (d.appendedDuringDecide()) {
            fail(about(s, index, d) + ": the authority tried to change the log while deciding");
        }
    }

    static void sameTwice(Scenario s, int index, Run.Decision d) {
        assertEquals(d.first(), d.second(), about(s, index, d) + ": deciding again on the same log");
    }

    /**
     * Judged on the first verdict, the one the authority answered with; a second verdict that differs
     * from it is caught by {@value #TWICE}.
     */
    static void rejectionHasReason(Scenario s, int index, Run.Decision d) {
        if (d.first() instanceof Authority.Reject r && r.reason().isBlank()) {
            fail(about(s, index, d) + ": rejected with a blank reason");
        }
    }

    /**
     * A submitted read is not rejected. Stage 1 has no access checks, so the only reason to reject a
     * read would be a conflict, which [[Reads return statements and writes follow a read]] forbids.
     * When access checks arrive, this must narrow to "not rejected for a conflict". Judged on the
     * first verdict, the one the authority answered with; a second verdict that differs from it is
     * caught by {@value #TWICE}.
     */
    static void readIsNotRejected(Scenario s, int index, Run.Decision d) {
        var action = ((AcceptedAction) d.candidate().body()).action();
        if (Basic.readOf(action).isPresent() && d.first() instanceof Authority.Reject r) {
            fail(about(s, index, d) + ": a read was rejected: " + r.reason());
        }
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
                    case When.Submit submit -> {
                        if (!(submit.step().action() instanceof ActionSpec.Read)) {
                            fail(s.name() + ": broken scenario: expects statements, but submits "
                                    + submit.step().name() + ", which is not a read");
                        }
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
