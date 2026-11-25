---
identity__uuid: 70aa1989-a908-44b3-a484-509793fc51e2
identity__type: particular
identity__created_at: 2026-11-25
identity__tags: []
relationships__is_a:
  - "[[Process Instance v1.0.0]]"
  - "[[Issue Loop v1.0.0]]"
relationships__in_process:
  - "[[Milestone Stage 1 kernel core]]"
relationships__in_context_of:
  - "[[Surfaces]]"
properties__status: open
properties__started_at: 2026-11-25
properties__current_phase: Operate
properties__issue: 22
properties__author: agent:claude-opus-5-5
---
Run for GitHub issue #22 (K5), on branch `issue/22-contract-tests-surfaces-authorities`, stacked on #36 (K1), whose `surfaces` module it tests. Superpowers brainstorming is the Laydown.

## Loop Config

- **Domain:** Complicated. What the kernel guarantees is set by the Decisions of #36; what a contract for any surface or authority can check is not.
- **Scope of Authority:** test fixtures and tests in `src/surfaces/`, `src/surfaces/build.gradle.kts`, this run and its Letters and Decisions. No change to the kernel's main code: what the contract finds wrong in it goes into Letters and the review of #36.
- **Reservoir capacity:** about two working days.

## Vector

- **Attractor:** a contract any surface and any authority can run, in one test format, that the stub authority `Projects` passes, covering delivery by interest, emission in response, acceptance and rejection, and reads at a frontier.
- **Repulsor:** changing the kernel, the log or the query module; a general authority (K9); access checks; asynchronous delivery.

## Design

### Intent

Stage 1's gate asks that "the contract tests pass" ([[Stage 1's tasks and gate are stated in events]]). Surfaces and authorities are written by many hands (the stub `Projects` now; K9's general authority, B1, B2 and every domain system later), and the kernel will keep changing. This run gives them one way to state and check their behaviour: a test format that runs through the real kernel, and rules every implementation is held to for free. It is the surfaces' counterpart of `EventLogContract` and `DatastoreContract`.

### Goals

- A scenario format: given a sequence of submitted actions, when one more thing happens (a submit, an attach, a read), then the expected verdict, emitted actions or statements.
- A runner that plays a scenario through the real `Kernel` with an `InMemoryEventLog` and a fixed clock, so a run is the same every time.
- `AuthorityContract` and `SurfaceContract`: abstract classes a test class extends with its authority or surface and its scenarios. Each scenario is checked against its expectations and against the general rules below.
- `Projects` passes `AuthorityContract`, with scenarios carried over from `KernelTest`.
- A reference surface, `Echo`, configured as data, passes `SurfaceContract`, with scenarios carried over from `ReceivingTest` and `DeliveryTest`. It proves the surface contract and covers delivery and emission. *Builder's call.*
- Coverage of issue #22: delivery by interest, emission in response, acceptance and rejection, reads at a frontier.

### Non-goals

- Changing the kernel, the log, `events` or `query`. Where the contract finds the kernel wrong, the test is skipped with its reason (see "Known kernel gaps"), and the gap goes to the review of #36.
- Security tests: the kernel checks no access yet (Letter "Delivery checks no access"), so there is nothing to test. A Letter proposes a separate issue for when scopes and grants exist.
- JSON scenario files. The format is records so it can become JSON later, not now.
- Asynchronous delivery, K9's general authority, the graph datastore.

### Anti-goals

- A contract that only `Projects` can pass: rules that hold for every implementation stay separate from scenarios that hold for one.
- Scenarios that depend on event ids, salts or recorded times, which change from run to run.
- Mocking the kernel: the contract tests what the kernel actually does.

### Constraints

- Java 25, JUnit 6 (as #36). Everything lives in `src/surfaces`, as test fixtures (`java-test-fixtures`), so other modules and domain systems can use it.
- Builds on #36's `Surface`, `Authority`, `Interest`, `Kernel`, `Answer`, `Context`, `Basic`, `Assertion`; stacked on #36.

### Approaches

| Approach | Trade-off | Chosen |
| --- | --- | --- |
| A. General rules only | Holds for any implementation; says nothing about what one surface should do, and covers issue #22 only in part | Part of C |
| B. Scenarios and a runner | Is the test format the plan asks for, and covers issue #22; each implementation writes its own scenarios | Part of C |
| **C. B and A**: scenarios, each also checked against the general rules | Both; a little more to build | Yes ([[Contract tests are scenarios checked against general rules]]) |

| Scenario form | Trade-off | Chosen |
| --- | --- | --- |
| Java builder over lambdas | Quickest; scenarios are code and cannot become data | No |
| JSON files | Logic as data; needs symbolic ids and a loader now | Later |
| **Records with symbolic references, and a small builder** | As quick as a builder; plain data that can be written as JSON later | Yes ([[Contract scenarios are records with symbolic references]]) |

### Decided

**The format.** A `Scenario` is a record: a name, `List<Step> given`, one `When`, one `Then`.
- A `Step` is named and submits an action: who (a `Context`), what it knew (names of earlier steps; empty means nothing), named links (link name to step names), and the action.
- Actions are written with symbolic references, resolved by the runner: `Ref.step("create")` is that step's event, `Ref.tuple("create", 0)` its tuple `e/0`, `Ref.named("name")` is `Identity.named("name")`, `Ref.local(0)` is `/0` inside the action. The forms are the ones `Basic` and `Assertion` build: assert with tuples (entity, full tuple, retraction) and read. Other definitions take their arguments as JSON with no references.
- `When` is one of: `Submit(step)` (one more submission), `Attach(surface frontier: step names)` (the surface under test attaches), `Read(thing, at: step names)` (the statements about a thing at a frontier, through `Kernel.statements`).
- `Then` is one of: `Accepted`, `Rejected(reason contains, optional)`, `Emits(list of actions, in order, symbolic)`, `Statements(list of tuples, symbolic, in recorded order)`.
- Expectations compare symbolic forms: ids, salts and times never appear in a scenario.

**The runner.** For each scenario: a fresh `InMemoryEventLog`, `new Hlc(() -> 1000)`, a `Kernel` with the authority under test (or, for a surface contract, an authority that accepts everything), then the given steps in order, then the `When`, then the check of `Then`. A given step that is rejected fails the scenario as broken, not the implementation.

**The contracts.** `AuthorityContract` asks for `authority()` and `scenarios()`; `SurfaceContract` asks for `surface()`, `scenarios()` and, optionally, `authority()`. Each runs its scenarios as JUnit dynamic tests: one container per scenario, with one test for its expectation and one per general rule, so each failure names the scenario and the rule.

**General rules for every authority** (checked on every submit of every scenario):
1. `decide` does not append to or change the log: the log it gets is a read-only view, and appending to it fails the test.
2. The same candidate and log give the same verdict: each submit is decided twice.
3. A rejection has a non-empty reason.
4. A read is not rejected for a conflict ([[Reads return statements and writes follow a read]]).

**General rules for every surface** (checked on every delivery of every scenario):
1. It receives only events its interests match (`Interest.matches`), each after its causes.
2. It never receives what it emitted.
3. What it emits becomes an event whose causes include the event it responded to.
4. The same events give the same emitted actions: the scenario is run twice from scratch and the emitted actions compared symbolically.
5. Its successive emissions are causally ordered: the second knew the first.

**Known kernel gaps.** Rules that #36's kernel breaks today are run, and skipped with a reason that names the review of #36, instead of being left out ([[Contract tests skip known kernel gaps instead of leaving them out]]). Today that is surface rule 5 ("a surface's own emissions are not causally ordered"), and, as a kernel scenario, a surface whose `interests()` throws must not fail the submit or the other surfaces' delivery. Removing the skip is the fix's test.

**`Projects`.** `ProjectsContractTest extends AuthorityContract`, with the scenarios of `KernelTest`: accept a creation, a read answers with statements, a write follows its read, a concurrent rename is rejected, an unrelated write and a blind write are accepted, an unknown frontier and a non-assert are rejected. Issue #22's "acceptance and rejection" and "reads at a frontier".

**`Echo`, the reference surface.** A surface in the fixtures whose interests and responses are data: a set of `Interest`s, and rules "for each received event that matches this interest, emit these actions" (symbolic, as in steps). `EchoContractTest extends SurfaceContract`, with the scenarios of `ReceivingTest` (an attached surface catches up, then receives what matches; attaching from a frontier skips what is in it; reads are received; an emission is caused by what was known) and the delivery-order ones of `DeliveryTest`. Issue #22's "delivery by interest" and "emission in response". *Builder's call:* `ReceivingTest` tests delivery with a surface of its own, which an authority contract has no place for.

The existing tests stay; the scenarios cover the same ground through the contracts.

**Answers to the builder check** (all *builder's call*):
- `Then.Emits` compares the actions the surface under test returned from `receive`, in order, captured by a recording wrapper around it. Whether they were accepted is surface rule 3, read from the log.
- Symbolic comparison: in expected actions, step references resolve to real ids and `/n` stays local, as the action carries it. In `Then.Statements`, only `(identity, attribute, value)` are compared, never the statement's own id; identities are rendered back to symbols: a named step's event or tuple by its name (`create/0`), any other event by `emitted#k` in log order after the `When`. `links` and `by` are not compared; rule 3 checks the causes.
- Observations: the runner runs a scenario once, wrapping the surface under test (deliveries, returns) and the authority (decisions, a read-only log view, deciding twice); the rules are judged on what was recorded and reported as one dynamic test each. Authority rules apply to every decision, including those on emitted actions. Surface rule 4 runs the scenario a second time from scratch. Rules 2 and 3 check the kernel and the surface together, passively, on what the scenario does.
- A read in `given` is a submitted `Basic.read`, recorded as an event and usable as `based_on`. `When.Read` is the non-recording `Kernel.statements`. Authority rule 4 applies to submitted reads.
- Known gaps: a rule listed as a known gap is run; if it fails, the test is aborted (`Assumptions.abort`) with the reason, shown as skipped; if it passes, the test fails with "known gap appears fixed: remove it from the list", so a fix is noticed. The kernel scenario "a surface whose `interests()` throws must not fail the submit or the other surfaces' delivery" is a plain test, `KernelGapsTest`, with the same mechanism.
- Build: `java-test-fixtures` in `src/surfaces`, with `testFixturesApi(testFixtures(project(":events")))` and JUnit from the existing catalog (JUnit 6.1.3, as in #36).

### Left to the builder

- Package and class names inside `khora.surfaces.contract`, the builder's method names, how symbolic references are resolved.
- How the read-only log view and the run-twice checks are implemented.
- How a probe surface records deliveries for the surface rules.

## Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** contract tests any surface or authority can run, as scenarios through the real kernel, each checked against general rules; `Projects` and a reference surface `Echo` pass them.

**Architecture:** test fixtures of `src/surfaces`, package `khora.surfaces.contract`. Scenarios are records with symbolic references; `ScenarioRunner` plays one through a fresh `Kernel` and records what happened in a `Run`; `AuthorityContract` and `SurfaceContract` turn each `Run` into JUnit dynamic tests, one per expectation and one per rule.

**Tech Stack:** Java 25, Gradle (`java-test-fixtures`), JUnit 6.1.3 (`@TestFactory`, `DynamicContainer`, `Assumptions.abort`), the `events` and `surfaces` modules of #36.

**Spec:** the Design section above.

### Global Constraints

- Everything new lives in `src/surfaces/src/testFixtures/java/khora/surfaces/contract/`, and its tests in `src/surfaces/src/test/java/khora/surfaces/contract/`. No change to `src/surfaces/src/main`, `src/events`, `src/log-nats` or `src/query`.
- Every scenario runs on a fresh `InMemoryEventLog` and `new Hlc(() -> 1000)`.
- Scenarios never contain event ids, salts or recorded times; expectations compare symbolic forms.
- The existing tests (`KernelTest`, `DeliveryTest`, `ReceivingTest`, `InterestTest`, `ContextTest`) stay unchanged and keep passing.
- A rule listed in `KnownGaps` that fails is aborted with its reason; one that passes fails with "known gap appears fixed: remove it from KnownGaps".
- Commits as Isiooo, local only; stage explicit paths; never `.obsidian/`, `.superpowers/`, `.DS_Store`, build output.

### Review Focus

1. A rule that can never fail asserts nothing: each authority rule and surface rule 4 has a test with a deliberately bad implementation that the rule catches (Tasks 3 and 4).
2. A given step that is rejected must fail the scenario as broken, naming the step, not pass or blame the implementation (Task 2, `aRejectedGivenStepBreaksTheScenario`).
3. A symbolic reference to an unknown step must fail with the step's name, not a `NullPointerException` (Task 1, `anUnknownStepIsNamed`).
4. Two scenarios with the same name in one contract must be refused, since dynamic test names would collide (Task 2, `duplicateScenarioNamesAreRefused`).
5. An emitted event that no step names must render as `emitted#k` in log order, so statement expectations stay readable (Task 1, `unnamedEventsRenderAsEmitted`).

---

### Task 1: The scenario format

**Files:**
- Modify: `src/surfaces/build.gradle.kts` (add `java-test-fixtures`; `testFixturesApi(testFixtures(project(":events")))`, `testFixturesApi(libs.junit.jupiter)`)
- Create in `testFixtures/.../contract/`: `Ref.java`, `ValueSpec.java`, `TupleSpec.java`, `ActionSpec.java`, `Step.java`, `When.java`, `Then.java`, `Scenario.java`, `Bindings.java`
- Test: `src/surfaces/src/test/java/khora/surfaces/contract/ScenarioFormatTest.java`

**Interfaces:**
- Produces:
  - `sealed interface Ref`: `Step(String name)`, `Tuple(String step, int index)`, `Named(String name)`, `Local(int index)`, `Received(int index)` (a tuple of the event a surface responds to; used by `Echo`). Factories `Ref.step(..)`, `Ref.tuple(..)`, `Ref.named(..)`, `Ref.local(..)`, `Ref.received(..)`.
  - `sealed interface ValueSpec`: `Literal(Json value)`, `RefValue(Ref ref)`; factories `ValueSpec.text(String)`, `ValueSpec.integer(long)`, `ValueSpec.ref(Ref)`.
  - `record TupleSpec(Optional<Ref> identity, Optional<Ref> attribute, Optional<ValueSpec> value)`; `entity()`, `of(Ref, Ref, ValueSpec)`, `retraction(Ref)`.
  - `sealed interface ActionSpec`: `Assert(List<TupleSpec>)`, `Read(Ref thing)`, `Raw(Identity definition, Json.Obj args)`; `Action resolve(Bindings b, Optional<Event> received)`.
  - `record Step(String name, Context who, List<String> knew, Map<String, List<String>> links, ActionSpec action)`.
  - `sealed interface When`: `Submit(Step step)`, `Attach(List<String> from)`, `Read(Ref thing, List<String> at)`.
  - `sealed interface Then`: `Accepted()`, `Rejected(Optional<String> reasonContains)`, `Emits(List<ActionSpec> actions)`, `Statements(List<TupleSpec> tuples)`.
  - `record Scenario(String name, List<Step> given, When when, Then then)` with a builder `Scenario.named(String).given(Step...).when(When).then(Then)` and `Step.named(String).by(Context).knew(String...).link(String name, String... steps).does(ActionSpec)`.
  - `final class Bindings`: `void bind(String step, EventId id)`, `EventId event(String step)`, `Identity identity(Ref ref, Optional<Event> received)`, `Frontier frontier(List<String> steps)`, `void emitted(EventId id)`, `String render(Identity id)` (a named step's event as `name`, its tuple as `name/n`, an emitted event as `emitted#k`, k from 1 in the order `emitted` was called, anything else as its value).

- [ ] **Step 1: Write the failing tests** in `ScenarioFormatTest`:
  - `refsResolveAgainstBoundSteps`: bind `create` to an event id; `Ref.tuple("create", 0)` resolves to `Identity.of(id, 0)`; `Ref.named("name")` to `Identity.named("name")`; `Ref.local(1)` to `Identity.local(1)`.
  - `anUnknownStepIsNamed`: `bindings.event("missing")` throws `IllegalArgumentException` whose message contains `missing`.
  - `assertSpecResolvesToTheAssertAction`: `ActionSpec.Assert` of `entity()` and `of(local(0), named("name"), text("Kitchen"))` resolves to `Assertion.action(Assertion.entity(), Assertion.of(Identity.local(0), Identity.named("name"), new Assertion.Literal(new Json.Str("Kitchen"))))`.
  - `readSpecResolvesToBasicRead`: `ActionSpec.Read(Ref.tuple("create", 0))` resolves to `Basic.read(Identity.of(id, 0))`.
  - `receivedRefsNeedAReceivedEvent`: resolving `Ref.received(0)` with an empty `received` throws `IllegalArgumentException`.
  - `unnamedEventsRenderAsEmitted`: with `create` bound and two ids passed to `emitted`, `render` gives `create`, `create/0`, `emitted#1/0`, `emitted#2`.
  - `theBuilderMakesTheRecords`: a scenario built with the builder equals the one built from the records.
- [ ] **Step 2:** Run `./gradlew :surfaces:test --tests '*ScenarioFormatTest'`. Expected: FAIL, the types do not exist.
- [ ] **Step 3:** Implement the types and `Bindings`. Records validate in their constructors (non-blank names, non-negative indexes, `Assert` with at least one tuple).
- [ ] **Step 4:** Run the same command. Expected: PASS. Then `./gradlew :surfaces:test`: all existing tests still pass.
- [ ] **Step 5: Commit** `Add the contract scenario format`.

### Task 2: The runner and the authority contract's expectations

**Files:**
- Create: `ScenarioRunner.java`, `Run.java`, `AuthorityContract.java`, `AcceptAll.java` (in `testFixtures/.../contract/`)
- Test: `src/surfaces/src/test/java/khora/surfaces/contract/ProjectsContractTest.java`, `ScenarioRunnerTest.java`

**Interfaces:**
- Consumes: Task 1.
- Produces:
  - `record Run(Bindings bindings, Optional<Answer> answer, List<Statement> statements, List<Decision> decisions, List<Delivery> deliveries)`; `record Decision(Event candidate, Authority.Verdict first, Authority.Verdict second, boolean appendedDuringDecide)`; `record Delivery(Event event, List<Action> returned, Set<EventId> logBefore)`.
  - `static Run ScenarioRunner.run(Scenario s, Authority authority, Optional<Surface> surface)`: fresh log, `new Hlc(() -> 1000)`, `new Kernel(recordingAuthority, log, clock)`; submits each given step (its `knew` as `bindings.frontier(knew)`, its links resolved to event ids, binding the step name to the accepted event); then the `When`: `Submit` → `answer`; `Attach` → `kernel.attach(recordingSurface, bindings.frontier(from))`; `Read` → `statements = kernel.statements(...)`. Events appended after the `When` are passed to `bindings.emitted` in log order.
  - `class AcceptAll implements Authority`: accepts everything; name `accept-all`.
  - `abstract class AuthorityContract`: `protected abstract Authority authority()`, `protected abstract List<Scenario> scenarios()`, `@TestFactory Stream<DynamicContainer> contract()`: one container per scenario, holding the test `expectation`.

- [ ] **Step 1: Write the failing tests:**
  - `ProjectsContractTest extends AuthorityContract` with `new Projects()` and eight scenarios carried over from `KernelTest`, same names: `anAcceptedActionIsAnEventByItsCaller` (then `Accepted`), `aReadIsAnEventAndAnswersWithStatements` (`When.Read` of the project at `create` → `Statements` of `entity()` and the name tuple), `aWriteFollowsTheReadItWasBasedOn`, `aConcurrentRenameOfTheSameNameIsRejected` (`Rejected` with reason containing `conflicts with`), `anUnrelatedWriteDoesNotConflict`, `aBlindWriteIsAccepted`, `anUnknownFrontierIsRejected`, `actionsTheAuthorityDoesNotTakeAreRejected` (a `Raw` definition → `Rejected` containing `projects takes only reads and asserts`). Use `KernelTest`'s contexts `anna`, `ivan`, `mia` with grant `g3` and budget 10.
  - `ScenarioRunnerTest`: `aRejectedGivenStepBreaksTheScenario` (a given step rejected by `Projects` makes `run` throw `IllegalStateException` naming the step); `duplicateScenarioNamesAreRefused` (a contract whose `scenarios()` holds two with one name fails its `contract()` factory with `IllegalArgumentException`); `theSameScenarioRunsTheSameTwice` (two runs of one scenario give equal symbolic statements).
- [ ] **Step 2:** Run `./gradlew :surfaces:test --tests '*contract*'`. Expected: FAIL, the runner does not exist.
- [ ] **Step 3:** Implement `Run`, `ScenarioRunner`, `AcceptAll`, `AuthorityContract` (expectation only). Expectation checks: `Accepted` / `Rejected` against `answer`; `Statements` against `statements` mapped to symbolic tuples through `bindings.render` (compare identity, attribute, value; never statement ids).
- [ ] **Step 4:** Run the same command, then `./gradlew :surfaces:test`. Expected: all PASS.
- [ ] **Step 5: Commit** `Run contract scenarios through the kernel; Projects passes its scenarios`.

### Task 3: The authority rules

**Files:**
- Create: `RecordingAuthority.java`, `ReadOnlyEventLog.java`, `KnownGaps.java`
- Modify: `ScenarioRunner.java` (wrap the authority), `AuthorityContract.java` (four rule tests per scenario)
- Test: `src/surfaces/src/test/java/khora/surfaces/contract/AuthorityRulesTest.java`

**Interfaces:**
- Consumes: Task 2.
- Produces:
  - `ReadOnlyEventLog(EventLog inner) implements EventLog`: reads delegate; `append`, `follow` and `close` throw `UnsupportedOperationException`.
  - `RecordingAuthority(Authority inner)`: for each `decide`, calls `inner.decide` twice on a `ReadOnlyEventLog`, records a `Decision` (an `UnsupportedOperationException` from `append` sets `appendedDuringDecide` and counts as a reject), returns the first verdict.
  - `final class KnownGaps`: `static final Map<String, String> RULES` (rule name → reason), and `static void judge(String rule, Executable check)`: runs `check`; if the rule is in `RULES` and the check fails, `Assumptions.abort(reason)`; if it is in `RULES` and passes, `fail("known gap appears fixed: remove it from KnownGaps: " + rule)`; otherwise the check's own result.
  - Rule test names in each scenario container: `authority does not change the log`, `authority decides the same twice`, `a rejection has a reason`, `a read is not rejected for a conflict`.
- [ ] **Step 1: Write the failing tests** in `AuthorityRulesTest`, each running one ordinary scenario (a creation, then a rename) through the contract machinery with a bad authority and asserting the named rule fails while the others pass: `anAuthorityThatAppendsBreaksTheLogRule`, `anAuthorityThatFlipsBreaksTheSameTwiceRule` (accepts on odd calls), `aBlankRejectionBreaksTheReasonRule` (rejects with `""`), `rejectingAReadBreaksTheReadRule`. Plus `projectsKeepsEveryAuthorityRule`.
- [ ] **Step 2:** Run `./gradlew :surfaces:test --tests '*AuthorityRulesTest'`. Expected: FAIL.
- [ ] **Step 3:** Implement as in the Interfaces. Rules are judged over every `Decision` in the `Run`, including decisions on emitted actions.
- [ ] **Step 4:** Run `./gradlew :surfaces:test`. Expected: all PASS; `ProjectsContractTest` shows 5 tests per scenario.
- [ ] **Step 5: Commit** `Hold every authority to the general rules`.

### Task 4: The surface contract and `Echo`

**Files:**
- Create: `Echo.java`, `RecordingSurface.java`, `SurfaceContract.java`
- Modify: `ScenarioRunner.java` (wrap the surface), `KnownGaps.java` (add surface rule 5)
- Test: `src/surfaces/src/test/java/khora/surfaces/contract/EchoContractTest.java`, `SurfaceRulesTest.java`

**Interfaces:**
- Consumes: Tasks 1-3.
- Produces:
  - `record Echo(String name, Set<Interest> interests, List<Echo.Rule> rules) implements Surface`; `record Rule(Interest when, List<ActionSpec> emit)`; `receive` returns, for each rule whose `when` matches the event, its actions resolved with `Ref.received` bound to the event; `Ref.step` is refused inside `Echo`.
  - `RecordingSurface(Surface inner, EventLog log)`: records a `Delivery` per `receive` (the event, the returned actions, the ids in the log at that moment).
  - `abstract class SurfaceContract`: `protected abstract Surface surface()`, `protected abstract List<Scenario> scenarios()`, `protected Authority authority()` (default `new AcceptAll()`); `@TestFactory` like `AuthorityContract`, with `expectation` (for `Emits`: the actions returned by deliveries after the `When`, flattened in order, compared to the `ActionSpec`s resolved per delivery) and the five rules, named `receives only what its interests match, after its causes`, `never receives what it emitted`, `what it emits is caused by what it received`, `the same events give the same actions` (runs the scenario a second time from scratch), `its successive emissions are causally ordered`.
  - `KnownGaps.RULES` gains `its successive emissions are causally ordered` → `"#36: a surface never receives its own emissions, so its frontier does not include them (review of #36, Kernel.java:219)"`.
- [ ] **Step 1: Write the failing tests:**
  - `EchoContractTest extends SurfaceContract` with an `Echo` labelling every project it hears about, and scenarios carried over from `ReceivingTest` (same names): `anAttachedSurfaceCatchesUpThenReceivesWhatMatches`, `attachingFromAFrontierSkipsWhatIsInIt`, `readsAreReceivedToo`, `whatASurfaceEmitsIsAnEventCausedByWhatItKnew`, and from `DeliveryTest`: `anEmissionReachesOthersAfterItsCause`, `catchingUpKeepsLogOrderAndSkipsOwnEmissions`.
  - `SurfaceRulesTest`: `aSurfaceThatEmitsRandomlyBreaksTheSameEventsRule` (emits a label with a random value); `echoKeepsEverySurfaceRule` (all rules pass except `its successive emissions are causally ordered`, which is aborted with the #36 reason).
- [ ] **Step 2:** Run `./gradlew :surfaces:test --tests '*Echo*' --tests '*SurfaceRulesTest'`. Expected: FAIL.
- [ ] **Step 3:** Implement as in the Interfaces.
- [ ] **Step 4:** Run `./gradlew :surfaces:test`. Expected: all PASS, with the known-gap rule shown as skipped.
- [ ] **Step 5: Commit** `Add the surface contract and the reference surface Echo`.

### Task 5: The kernel gap test and the security Letter

**Files:**
- Create: `src/surfaces/src/test/java/khora/surfaces/contract/KernelGapsTest.java`
- Modify: `KnownGaps.java` (add the kernel scenario)
- Entity (controller, through the Obsidian CLI): a Letter in_context_of this run, "Security tests wait for access checks"

**Interfaces:**
- Consumes: Tasks 1-4.

- [ ] **Step 1: Write the test** `aSurfaceWhoseInterestsThrowDoesNotFailTheSubmit`: a kernel with `Projects`, an attached surface whose `interests()` throws, and an `Echo` with `Interest.any()`; a submitted creation must return `Accepted`, and the `Echo` must have received it. Judged through `KnownGaps.judge("a surface whose interests throw does not fail the submit", ...)`, with the reason `"#36: Kernel.wants calls interests() outside the guard around receive (review of #36, Kernel.java:178)"`.
- [ ] **Step 2:** Run `./gradlew :surfaces:test --tests '*KernelGapsTest'`. Expected: the test is skipped with that reason (the gap exists today).
- [ ] **Step 3:** Run `./gradlew test` (all modules; Docker running for `log-nats`). Expected: all PASS or skipped as above; output quiet.
- [ ] **Step 4: Commit** `Pin the kernel gaps found in the review of #36`.