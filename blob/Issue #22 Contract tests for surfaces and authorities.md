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