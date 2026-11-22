---
identity__uuid: 25aaea8a-20aa-4f94-a519-56140ab8053b
identity__type: particular
identity__created_at: 2026-11-19
identity__tags: []
relationships__is_a:
  - "[[Process Instance v1.0.0]]"
  - "[[Issue Loop v1.0.0]]"
relationships__in_process:
  - "[[Milestone Stage 1 kernel core]]"
relationships__in_context_of:
  - "[[Surfaces]]"
properties__status: closed
properties__started_at: 2026-11-19
properties__current_phase: Process(v)
properties__issue: 21
properties__author: agent:claude-opus-5-5
properties__ended_at: 2026-11-22
---
Issue #21 (plan task K4) of [[Milestone Stage 1 kernel core]]: the surface contract, its declaration and its context as Java types. Opened with [[Simeon Petkov]].

## Loop Config

- **Domain:** Complicated. The contract is specified in ADR-0XY decisions 1 and 2; the work is translating it into types.
- **Scope of Authority:** the surface contract types and a stub surface under `src/`, their tests, and this run's entities in `blob/`. Documents in `docs/` change only through Update View from Decisions.
- **Reservoir capacity:** one pull request.

## Vector

- **Attractor:** `declares`, `read`, `write`, `changes`, the Declaration and the context `{ principal, grant, budget, cause }` as Java types, with a stub surface that implements them. Each call is one action.
- **Repulsor:** no event log, storage, query engine, access checks or API. K1's real event ids, K5's contract tests, K8's `Query` and K9's outcome delivery stay in their own issues.
- **First move:** the Maven project and the context types (Task 1).
- **Budget:** five tasks, one pull request.
- **Close early if:** a contract type needs a design choice ADR-0XY does not make. Stop and agree it as a Decision first.

## Decisions

- [[The kernel is implemented in Java]]
- [[EventId starts as a minimal placeholder record]]
- [[The kernel builds with Maven on Java 25]]
- [[Contract unions are sealed interfaces with records]]
- [[Optional surface capabilities are separate interfaces]]
- [[Surface changes are read through a blocking ChangeFeed]]
- [[K1 runs in its own Issue run after K4]]
- [[Kernel types taken from ADRs are provisional]]
- [[The order capability is left out of the Declaration for now]]
- [[Surface changes take no context for now]]
- [[Events are the basic unit of khora]]
- [[Reads are events]]
- [[An event's causes are what it knew]]

## Questions

- [[Which parts of an entity are reified and keep identity]], answered by [[Events are the basic unit of khora]]
- [[What the basic event vocabulary contains]]
- [[Whether reads are events]], answered by [[Reads are events]]
- [[How overlapping event vocabularies are aligned]]
- [[How translations between vocabulary versions are kept]]

## Plan

> **For agentic workers:** use superpowers:executing-plans or superpowers:subagent-driven-development to carry out this plan task by task. Steps use checkboxes.

**Goal:** the surface contract from ADR-0XY decisions 1 and 2 as Java types, with an in-memory stub surface that implements it.

**Architecture:** one Maven project in `src/kernel/`, package `khora.kernel`. Records hold data, sealed interfaces hold the unions, enums hold the plain choices. `Surface`, `WritableSurface` and `ChangingSurface` are the contract. The stub records one action per `read` and `write` call through an `Actions` interface; K3 and K10 later back it with the log.

**Tech stack:** Java 25 (Temurin), Maven 3.10.1 through `mvnw`, JUnit 6.1.3, maven-compiler-plugin 3.16.0, maven-surefire-plugin 3.6.1, maven-wrapper-plugin 3.3.4.

**Spec:** `docs/adr/surfaces.md` (ADR-0XY) decisions 1, 2, 4, 7 and 8; `docs/adr/origins-behaviors-surfaces-executors.md` (ADR-0YA) decision 2; the Decisions linked above.

### Global constraints

- `JAVA_HOME=$HOME/.local/share/jdks/jdk-25.0.5.1+2` for every command; build only with `src/kernel/mvnw`.
- `maven.compiler.release` is 25. No dependencies except JUnit, in test scope.
- Every record field is non-null: compact constructors call `Objects.requireNonNull` with the field's name. Absent values are `Optional`.
- The signatures follow ADR-0XY decision 1: `declares()` and `changes(since)` take no context; `read` and `write` do.
- `EventId` is a placeholder (Decision: EventId starts as a minimal placeholder record). K1 replaces it.
- Every type taken from an ADR has a Javadoc line `Provisional: <what may change>` (Decision: Kernel types taken from ADRs are provisional). A type the ADR doesn't fix, or one that looks wrong, is not coded until a Decision settles it: stop and ask.

### Review focus

1. Null in any record field: `NullPointerException` naming the field, at construction. Task 1 and Task 3 tests.
2. A read at a frontier the stub has not reached: a `Partial` result, never silently complete (decision 4). Task 4 test.
3. A write whose condition frontier is stale: `Rejected`, and the stored value is unchanged (decision 7). Task 4 test.
4. `ChangeFeed.next()` blocked when `close()` is called: it returns empty instead of hanging. Task 5 test.
5. A negative budget: `IllegalArgumentException`. Task 1 test.

### Files

All paths are under `src/kernel/`. Main code is in `src/main/java/khora/kernel/`, tests in `src/test/java/khora/kernel/`.

| File | Responsibility |
|---|---|
| `pom.xml`, `mvnw`, `.mvn/wrapper/` | The build |
| `event/EventId.java` | The placeholder event id |
| `surface/Principal.java`, `Grant.java`, `Budget.java`, `Context.java` | The call context |
| `surface/Frontier.java`, `ReadTime.java` | Where and when a read happens |
| `surface/Declaration.java` and its enums | What a surface declares |
| `surface/ReadRequest.java`, `Result.java`, `Completeness.java`, `Annotation.java` | Reads |
| `surface/Operation.java`, `Condition.java`, `WriteAnswer.java` | Writes |
| `surface/Change.java`, `ChangeFeed.java` | Changes |
| `surface/Surface.java`, `WritableSurface.java`, `ChangingSurface.java`, `Actions.java` | The contract |
| `stub/InMemorySurface.java` | The stub surface |

### Task 1: Maven project and the call context

**Files:** create `pom.xml`, `mvnw`, `.mvn/wrapper/maven-wrapper.properties`, `event/EventId.java`, `surface/Principal.java`, `surface/Grant.java`, `surface/Budget.java`, `surface/Context.java`. Test: `surface/ContextTest.java`. Modify `.gitignore` at the repository root: add `src/kernel/target/`.

**Produces:**
```java
record EventId(String value)                  // non-blank
record Principal(String id)                   // non-blank
record Grant(String id)                       // non-blank
record Budget(long units)                     // units >= 0
record Context(Principal principal, Grant grant, Budget budget, Optional<EventId> cause)
```

- [x] Download Maven 3.10.1 into the scratchpad, check its SHA-512 against Maven Central, and run `mvn -N wrapper:wrapper -Dmaven=3.10.1` in `src/kernel/`. Expected: `mvnw` and `.mvn/wrapper/maven-wrapper.properties` exist. Then delete the downloaded Maven.
- [x] Write `pom.xml`: groupId `khora`, artifactId `kernel`, version `0.1.0-SNAPSHOT`, the JUnit BOM 6.1.3, `junit-jupiter` in test scope, and the compiler and surefire plugins at the versions above.
- [x] Write `ContextTest`: `contextHoldsItsParts` (a built `Context` returns what it was given, with `cause` empty); `nullPrincipalIsRefused` (`assertThrows(NullPointerException.class, …)`, message contains `"principal"`); `blankEventIdIsRefused` (`new EventId(" ")` throws `IllegalArgumentException`); `negativeBudgetIsRefused` (`new Budget(-1)` throws `IllegalArgumentException`).
- [x] Run `./mvnw -q test`. Expected: compile failure, because the types don't exist.
- [x] Write the five records.
- [x] Run `./mvnw -q test`. Expected: 4 tests pass.
- [x] Commit: `Add the kernel Maven project and the surface call context`.

### Task 2: Frontiers and read time

**Files:** create `surface/Frontier.java`, `surface/ReadTime.java`. Test: `surface/FrontierTest.java`.

**Produces:**
```java
record Frontier(Map<String, Long> positions)  // origin -> position; copied with Map.copyOf; positions >= 0
  static Frontier empty()
  Frontier advance(String origin, long position)   // a new frontier with that origin at that position
  boolean reaches(Frontier wanted)                 // every origin in wanted is here at an equal or later position
enum TimeAxis { RECORDED, OCCURRED }
enum Meaning { THEN, NOW }
record ReadTime(Optional<Frontier> at, TimeAxis by, Meaning meaning)
  static ReadTime latest()                         // at empty, RECORDED, THEN
```

Empty `at` means "the latest frontier the surface has reached". `TimeAxis` and `Meaning` come from the query's `time` in the implementation plan's shared contracts.

- [x] Write `FrontierTest`: `emptyReachesEmpty`; `laterReachesEarlier` (`{a:5}` reaches `{a:3}`); `earlierDoesNotReachLater` (`{a:3}` does not reach `{a:5}`); `missingOriginIsNotReached` (`{a:5}` does not reach `{b:1}`); `negativePositionIsRefused`; `positionsAreCopied` (changing the map passed in doesn't change the frontier).
- [x] Run the tests. Expected: compile failure.
- [x] Write `Frontier`, `TimeAxis`, `Meaning` and `ReadTime`.
- [x] Run the tests. Expected: all pass.
- [x] Commit: `Add frontiers and read time`.

### Task 3: The contract types

**Files:** create `surface/Declaration.java` with its enums, `ReadRequest.java`, `Result.java`, `Completeness.java`, `Annotation.java`, `Operation.java`, `Condition.java`, `WriteAnswer.java`, `Change.java`, `ChangeFeed.java`, `Surface.java`, `WritableSurface.java`, `ChangingSurface.java`, `Actions.java`. Test: `surface/ContractTypesTest.java`.

**Consumes:** `EventId`, `Context`, `Frontier`, `ReadTime`.

**Produces:**
```java
enum Effects { NATIVE, OBSERVED, NONE }
enum History { FULL, SNAPSHOTS, NONE }
enum Reads { DETERMINISTIC, RECORDED, NONDETERMINISTIC }
enum Writes { NONE, UNCONDITIONAL, CONDITIONAL, MERGEABLE }
record Declaration(boolean authoritative, Effects effects, History history, Reads reads, Writes writes)

record ReadRequest(String target, ReadTime time)          // target: placeholder until K8's Query
sealed interface Completeness permits Complete, Partial
  record Complete() ; record Partial(String reason)
record Annotation(String text)                             // e.g. "as observed at 10:42"
record Result(EventId action, Optional<String> value, Completeness completeness, Frontier seen, List<Annotation> annotations)

record Operation(String target, String value)              // placeholder: set target to value
record Condition(Frontier read, Optional<String> expected) // what the write read
sealed interface WriteAnswer permits Unconfirmed, Rejected
  record Unconfirmed(EventId action) ; record Rejected(EventId action, String reason)

record Change(EventId event, String target, String value, Frontier at)
interface ChangeFeed extends AutoCloseable
  Optional<Change> next() throws InterruptedException      // blocks; empty once closed
  void close()                                             // idempotent; wakes a blocked next()

interface Actions { EventId record(Context context, String call); }  // call is "read" or "write"
interface Surface { Declaration declares(); Result read(ReadRequest request, Context context); }
interface WritableSurface extends Surface { WriteAnswer write(Operation operation, Condition condition, Context context); }
interface ChangingSurface extends Surface { ChangeFeed changes(Frontier since); }
```

`Declaration` has no `order` capability; its Javadoc says so, as provisional (Decision: The order capability is left out of the Declaration for now). `ChangingSurface.changes` takes no context; its Javadoc says so, as provisional (Decision: Surface changes take no context for now).

- [x] Write `ContractTypesTest`: `writeAnswerSwitchCoversBothCases` (a `switch` over `WriteAnswer` with no `default` returns `"unconfirmed"` or `"rejected"`); `completenessSwitchCoversBothCases` (the same over `Completeness`); `nullDeclarationFieldIsRefused` (null `effects` throws `NullPointerException` naming `effects`); `resultAnnotationsAreCopied` (changing the list passed in doesn't change the result).
- [x] Run the tests. Expected: compile failure.
- [x] Write the types.
- [x] Run the tests. Expected: all pass.
- [x] Commit: `Add the surface contract types`.

### Task 4: The stub surface: declares, read and write

**Files:** create `stub/InMemorySurface.java`. Test: `stub/InMemorySurfaceTest.java`.

**Consumes:** everything in Task 3.

**Produces:** `final class InMemorySurface implements WritableSurface, ChangingSurface`, with `InMemorySurface(Actions actions)`.

- It holds `target -> value` in memory under the origin `"stub"`. Its frontier is `{stub: n}`, where n counts the writes applied.
- `declares()` returns `authoritative=true, NATIVE, NONE, DETERMINISTIC, CONDITIONAL`.
- `read` calls `actions.record(context, "read")` once. If the requested frontier is reached, the result is `Complete`. If not, it is `Partial("frontier not reached")` with the current value; it does not wait. `seen` is the current frontier.
- `write` calls `actions.record(context, "write")` once. If the current frontier differs from `condition.read`, or the current value differs from `condition.expected` (empty means the target has no value), it answers `Rejected` and changes nothing. Otherwise it applies the write, advances the frontier, and answers `Unconfirmed`.
- `changes` throws `UnsupportedOperationException` until Task 5.

- [x] Write `InMemorySurfaceTest`, with a counting `Actions` that returns `new EventId("a" + n)`: `declaresConditionalWrites`; `eachReadIsOneAction` (two reads record two `"read"` actions, and each `Result.action` is the recorded id); `eachWriteIsOneAction`; `writeThenReadSeesValue` (write `x=1` at the empty frontier, read latest: `Complete`, value `1`, `seen` `{stub:1}`); `readBeyondFrontierIsPartial` (read at `{stub:5}` on a new stub gives `Partial`); `staleConditionIsRejected` (two writes both conditioned on the empty frontier: the second is `Rejected`, and the value stays the first); `unexpectedValueIsRejected`.
- [x] Run the tests. Expected: compile failure.
- [x] Write `InMemorySurface`.
- [x] Run the tests. Expected: all pass.
- [x] Commit: `Add an in-memory stub surface`.

### Task 5: The stub surface: changes

**Files:** modify `stub/InMemorySurface.java`. Test: `stub/InMemorySurfaceChangesTest.java`.

- `changes(since)` returns a feed that first yields every applied write after `since`, in order, then blocks for new ones. Each write appends a `Change` with the write's action id and the frontier after it.
- Several feeds may be open at once. A closed feed returns empty from `next()`, including a `next()` already blocked.

- [x] Write `InMemorySurfaceChangesTest`: `feedReplaysWritesAfterSince` (three writes, feed from `{stub:1}` yields the second and third); `feedDeliversLaterWrite` (a virtual thread blocks on `next()`, a write is applied, the thread gets it within 1 s); `closeWakesBlockedNext` (a blocked `next()` returns empty within 1 s of `close()`); `closeIsIdempotent`.
- [x] Run the tests. Expected: `UnsupportedOperationException`.
- [x] Implement the feed with a lock and condition over the surface's change list; each feed keeps its own read position.
- [x] Run `./mvnw -q test`. Expected: every test in the project passes.
- [x] Commit: `Let the stub surface report its changes`.

## Verified

**Done:** Tasks 1–5 of the plan, in `src/kernel/`, package `khora.kernel`: the call context, frontiers and read time, the contract types, and an in-memory stub surface that implements `WritableSurface` and `ChangingSurface`. 27 tests pass on Java 25 with Maven 3.10.1.

**Checked:**
- Each test was seen failing before its code was written. The stale-frontier test was checked against a version of the stub without the check.
- A fresh reviewer read the whole branch. Its one Important finding (a read at a past frontier came back `Complete` with today's value) and two findings raised to Important (the stale-frontier check had no test of its own; a `null` action id left the stub half-written) were fixed, each with a test.
- `khora-validate` passes on every entity this run wrote.
- `khora-stale`: this run's Decisions make `docs/adr/surfaces.md`, `docs/adr/runtime-and-executors.md` and `docs/implementation-plan.md` stale. Update View brings them in, in a later run.
- No sentence was added to `docs/`.

**Not done:**
- Frontiers are a placeholder that only names a frontier; `ReadTime` holds only that frontier, without the time axis and meaning (they come with K8's `Query`). Why, and what K1 has to handle: [[Frontiers need more than one position per origin]].
- Deferred review findings: collection nulls don't name their field; four types say only "Provisional."; the call kind in `Actions` is a string; the stub records actions outside its lock, so action order can differ from change order. The ordering one belongs to K3.
- The contract is not yet reshaped around events ([[Events are the basic unit of khora]]): writes take a basic set, not a surface's own event, and `changes` yields values, not events. K4 merges as provisional, before K1 (#18), whose branch builds on it; the reshaping is a follow-up issue after K1. What K1 has to follow: [[Causes and reads for K1's events]].