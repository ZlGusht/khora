---
identity__uuid: 484082cc-f957-4535-b88c-c9c6f3aeb42b
identity__type: particular
identity__created_at: 2026-11-19
identity__tags: []
relationships__is_a:
  - "[[Process Instance v1.0.0]]"
  - "[[Issue Loop v1.0.0]]"
relationships__in_process:
  - "[[Milestone Stage 1 kernel core]]"
relationships__in_context_of:
  - "[[Log]]"
  - "[[Runtime]]"
properties__status: open
properties__started_at: 2026-11-19
properties__current_phase: Operate
properties__issue: 18
properties__author: agent:claude-opus-5-5
---
Issue #18 (plan task K1) of [[Milestone Stage 1 kernel core]]: the four event types, their content-hash ids and the hybrid logical clock, in Java. Opened with [[Simeon Petkov]]. It runs in parallel with [[Issue #21 Surface contract types]].

## Loop Config

- **Domain:** Complicated. ADR-0YB decisions 1, 3 and 5 specify the events, their ids and the clock; the work is translating them into types and choosing the hashing and clock details.
- **Scope of Authority:** the event types, event ids and the clock under `src/`, their tests, and this run's entities in `blob/`. The code lives in its own worktree on branch `issue/18-event-types-ids-and-clock`. Documents in `docs/` change only through Update View from Decisions.
- **Reservoir capacity:** one pull request.

## Vector

- **Attractor:** the event as an accepted action, its salted content-hash id, the hybrid logical clock, and frontiers as sets of events closed under causes, in Java. An event round-trips; a frontier that lacks an event's cause is refused.
- **Repulsor:** frontiers over external origins, layered surfaces, writes conditioned across origins, branches and other deployments (the other cases in [[Frontiers need more than one position per origin]]); authority checks; encryption and shredding; keeping rejections; changes to K4's contract beyond replacing its `EventId` and `Frontier` placeholders.

## Decisions

- [[K1 runs in parallel with K4]]
- [[Parallel runs keep code in separate worktrees and share one vault]]
- [[An event is an accepted action, and an assert is the basic action]]
- [[An assert's id is the identity of what it asserts]]
- [[Event ids are salted hashes of canonical JSON]]
- [[Assertions are tuples with identity]]

Taken in from the Issue #21 run: [[Events are the basic unit of khora]] (a frontier is the set of events a surface has taken in, closed under causes; K1 decides the basic vocabulary).

## Design

Types are provisional ([[Kernel types taken from ADRs are provisional]]); unions are sealed interfaces with records ([[Contract unions are sealed interfaces with records]]).

- **Event:** `id, causes, recorded, occurred?, by { principal, on_behalf_of?, grant, via }, salt, action`. `causes` are the heads of what the action read.
- **Action:** `{ definition, args }`. `definition` is the id of the action's definition, which says its vocabulary and version. The basic vocabulary has one action, `assert`, with a fixed well-known id the kernel ships; a reserved `retracted` attribute marks retraction. Other definitions are opaque to the kernel for now.
- **Assertions:** one assert action carries one tuple `(identity?, attribute?, value?)`, so the event's id is the assertion's identity. `(,,)` instantiates an entity. A value is a literal (string or integer) or a reference to an identity, and says which. Attributes are identities; `retracted` and `label` are well-known ones. Which partial tuples mean what is left open.
- **Id:** `sha256:<hex>` of the event's canonical JSON (RFC 8785) without the id itself. Values are strings, integers and ids.
- **Clock:** a hybrid logical clock: wall milliseconds and a counter packed into one long. `now()` never goes backwards; `after(causes)` is later than every cause.
- **Frontier:** a set of head event ids, standing for the heads and all their causes. Operations: compare (before, after, equal, concurrent), combine (union, reduced to heads), and a closure check against an event graph that refuses a frontier whose events or causes are unknown. Replaces K4's `Frontier` placeholder; the event id replaces K4's `EventId` placeholder.
- **Event graph:** an in-memory store of events by id, used by the closure check and the tests. Appending an event whose causes are absent, or whose recorded time is not after its causes, is refused.
- **Round-trip:** an event written to canonical JSON and read back is equal, and its id recomputes to the same value.

Left to execution: the argument shapes of asserts, who defines a non-basic action (the principal's kind, the target's kind, or neither), and whether observations need anything beyond an assert by an adapter with a source.

Builds on the Issue #21 branch: this branch is rebased onto it, so the placeholders can be replaced.

## Plan

> **For agentic workers:** use superpowers:executing-plans or superpowers:subagent-driven-development to carry out this plan task by task. Steps use checkboxes.

**Goal:** events as accepted actions, their salted content-hash ids, the hybrid logical clock, and frontiers as sets of events closed under causes, as Java types with an in-memory event graph.

**Architecture:** package `khora.kernel.json` holds a minimal JSON value type with a canonical writer (RFC 8785, restricted to objects, arrays, strings and integers) and a parser. Package `khora.kernel.event` holds `Event`, `Action`, `By`, the basic vocabulary, `Hlc`, `EventGraph` and `Frontier`. `event` does not depend on `surface`; `surface` depends on `event`. Task 6 replaces K4's placeholders once the Issue #21 run has closed.

**Tech stack:** as Issue #21: Java 25, Maven through `src/kernel/mvnw`, JUnit 6.1.3. No new dependencies (agent's call: a JSON subset of four value types does not need Jackson).

**Spec:** the Design section above, and the Decisions linked above.

### Global constraints

- Java 25, `-Xlint:all` with no warnings. No dependencies beyond JUnit.
- Every type carries `Provisional:` in its Javadoc, saying what is provisional ([[Kernel types taken from ADRs are provisional]]).
- A union that carries data is a sealed interface with record variants; a choice without data is an enum. Refusals are returned as sealed results, not thrown, except for malformed input to constructors and parsers (`IllegalArgumentException`).
- JSON values are only objects, arrays, strings and 64-bit integers.
- Ids are `sha256:` followed by 64 lowercase hex digits.
- Run tests with `./mvnw -q test` from `src/kernel/`.

### Review focus

1. Non-ASCII strings (`é`, an emoji as a surrogate pair) in keys and values: keys sort by UTF-16 code units, hashing uses UTF-8. Test in Task 1.
2. Integers at `Long.MIN_VALUE` and `Long.MAX_VALUE` round-trip. Test in Task 1.
3. The first event has no causes; the empty frontier is valid and is before every other frontier. Tests in Tasks 2 and 5.
4. The wall clock going backwards, or many events in one millisecond: recorded time still increases. Test in Task 3.
5. A cause chain of 10 000 events: closure and comparison do not overflow the stack. Test in Task 5.

---

### Task 1: Canonical JSON

**Files:**
- Create: `src/main/java/khora/kernel/json/Json.java`, `src/main/java/khora/kernel/json/JsonParser.java`
- Test: `src/test/java/khora/kernel/json/JsonTest.java`

**Interfaces:**
- Produces: `sealed interface Json permits Json.Obj, Json.Arr, Json.Str, Json.Int`; `record Obj(SortedMap<String, Json> members)` (sorted by UTF-16 code units, i.e. `String.compareTo`; the constructor copies); `record Arr(List<Json> items)`; `record Str(String value)`; `record Int(long value)`; `String Json.canonical()`; `static Json Json.parse(String text)` (delegates to `JsonParser`); helpers `Obj.get(String) -> Optional<Json>`, `Obj.str(String) -> String`, `Obj.integer(String) -> long`, `Obj.obj(String) -> Obj`, `Obj.arr(String) -> Arr`, each throwing `IllegalArgumentException` when the member is missing or of another type.

- [ ] **Step 1: Write the failing tests**
  - `canonicalSortsKeysAndHasNoWhitespace`: `{"b":1,"a":[2,"x"]}` built from values → `canonical()` is `{"a":[2,"x"],"b":1}`.
  - `canonicalEscapesAsRfc8785`: a string with `"`, `\`, `
`, `	`, U+0001 and `é` → `"\"\\
	\u0001é"` (control characters other than `\b \f 
 \r 	` as `\u00xx` lowercase; everything else literal).
  - `keysSortByUtf16CodeUnits`: keys `"é"`, `"z"`, `"😀"` → order `z`, `é`, `😀`.
  - `integersAtLimitsRoundTrip`: `Long.MIN_VALUE` and `Long.MAX_VALUE` survive `parse(canonical())`.
  - `parseRoundTripsCanonicalText`: for a nested value `v`, `Json.parse(v.canonical()).equals(v)`.
  - `parseRefusesOtherValues`: `1.5`, `1e3`, `true`, `null`, a duplicate key, trailing text, an integer beyond `long` → `IllegalArgumentException`.
- [ ] **Step 2: Run** `./mvnw -q test -Dtest=JsonTest` → fails to compile.
- [ ] **Step 3: Implement** `Json` and a recursive-descent `JsonParser` (whitespace allowed between tokens on input; `\uXXXX` escapes, including surrogate pairs, decoded).
- [ ] **Step 4: Run** `./mvnw -q test -Dtest=JsonTest` → passes.
- [ ] **Step 5: Commit** `Add canonical JSON for event ids`.

### Task 2: Events, ids and the basic vocabulary

**Files:**
- Create: `src/main/java/khora/kernel/event/Event.java`, `By.java`, `Action.java`, `Basic.java`, `EventJson.java`
- Modify: `src/main/java/khora/kernel/event/EventId.java` (add `static EventId hashOf(String canonicalJson)`; keep the constructor as it is until Task 6)
- Test: `src/test/java/khora/kernel/event/EventTest.java`, `BasicTest.java`

**Interfaces:**
- Consumes: `Json` from Task 1.
- Produces:
  - `record By(String principal, Optional<String> onBehalfOf, String grant, String via)`.
  - `record Action(EventId definition, Json.Obj args)`.
  - `record Event(EventId id, SortedSet<EventId> causes, long recorded, Optional<Long> occurred, By by, String salt, Action action)`: the constructor recomputes the id from the other fields and throws `IllegalArgumentException("id does not match content")` on a mismatch. `static Event create(Collection<EventId> causes, long recorded, Optional<Long> occurred, By by, Action action)` draws a salt of 16 bytes from `SecureRandom`, written as 32 lowercase hex digits; `static Event create(..., String salt)` takes it for tests.
  - `EventJson`: `static Json.Obj toJson(Event e, boolean withId)`; `static Event fromJson(Json.Obj o)`; `static String write(Event e)` (canonical, with id); `static Event read(String text)`.
  - `Basic`: `static final EventId ASSERT` and `static final EventId RETRACTED`, the hashes of the strings `khora/basic/assert` and `khora/basic/retracted`. Asserts (argument shapes provisional): `static Action entity()` → args `{}`; `property(EventId thing, EventId attribute, Json value)` → `{"attribute","thing","value"}`; `relationship(EventId from, EventId to, EventId relator)` → `{"from","relator","to"}`; `binding(EventId behavior, EventId thing)` → `{"behavior","thing"}`; `retraction(EventId assertion)` = `property(assertion, RETRACTED, new Json.Str("true"))`. Ids in args are `Json.Str`.
- The event's JSON, pinned: `{"action":{"args":{…},"definition":"sha256:…"},"by":{"grant":…,"on_behalf_of"?:…,"principal":…,"via":…},"causes":[sorted ids],"id"?:…,"occurred"?:<int>,"recorded":<int>,"salt":"<32 hex>"}`. The id is `EventId.hashOf` of the canonical JSON without `id`: `sha256:` + hex of SHA-256 over its UTF-8 bytes.

- [ ] **Step 1: Write the failing tests**
  - `eventRoundTrips`: `EventJson.read(EventJson.write(e)).equals(e)` and the id is unchanged.
  - `idIsSha256OfCanonicalJsonWithoutId`: with salt `"00"×16`, the id equals `EventId.hashOf(EventJson.toJson(e, false).canonical())` and matches `sha256:[0-9a-f]{64}`.
  - `tamperedEventIsRefused`: change `recorded` in the written text → `read` throws `IllegalArgumentException`.
  - `sameContentGetsDifferentIdsWithDifferentSalts`: two `create` calls with identical fields → different ids.
  - `causesOrderDoesNotMatter`: causes `[b, a]` and `[a, b]`, same salt → same id; duplicates collapse.
  - `firstEventHasNoCauses`: `create(List.of(), …)` round-trips; JSON has `"causes":[]`.
  - `absentOptionalsAreOmitted`: no `occurred` or `on_behalf_of` keys when empty.
  - `BasicTest.assertsBuildPinnedArgs`: each helper's `args().canonical()` equals the pinned shape; `retraction(x)` has `attribute` = `RETRACTED`.
- [ ] **Step 2: Run** `./mvnw -q test -Dtest='EventTest,BasicTest'` → fails to compile.
- [ ] **Step 3: Implement** the types above.
- [ ] **Step 4: Run** the same command → passes.
- [ ] **Step 5: Commit** `Add events with salted content-hash ids and the basic asserts`.

### Task 3: Hybrid logical clock

**Files:**
- Create: `src/main/java/khora/kernel/event/Hlc.java`
- Test: `src/test/java/khora/kernel/event/HlcTest.java`

**Interfaces:**
- Produces: `final class Hlc`, constructed with `LongSupplier wallMillis` (`Hlc.system()` uses `System::currentTimeMillis`); `synchronized long next(long after)` returns a value greater than `after` and than every value it returned before; `long next()` = `next(Long.MIN_VALUE)`; `static long pack(long millis, int counter)` = `millis << 16 | counter`; `static long millis(long t)` = `t >>> 16`; `static int counter(long t)` = `(int) (t & 0xFFFF)`.
- Algorithm (pinned): `candidate = max(pack(wall, 0), last + 1, after + 1)`; `last = candidate`; return it. A counter overflow carries into the milliseconds.

- [ ] **Step 1: Write the failing tests**
  - `followsWallClock`: wall 1000 → `millis(next()) == 1000`, `counter == 0`.
  - `neverGoesBackwards`: wall 1000, then 900 → the second value is greater than the first.
  - `manyInOneMillisecondIncrease`: 70 000 calls at wall 1000 → strictly increasing.
  - `nextAfterCauseIsLater`: `next(pack(5000, 3))` at wall 1000 → greater than `pack(5000, 3)`.
- [ ] **Step 2: Run** `./mvnw -q test -Dtest=HlcTest` → fails to compile.
- [ ] **Step 3: Implement** `Hlc`.
- [ ] **Step 4: Run** → passes.
- [ ] **Step 5: Commit** `Add the hybrid logical clock`.

### Task 4: In-memory event graph

**Files:**
- Create: `src/main/java/khora/kernel/event/EventSource.java`, `EventGraph.java`, `AppendResult.java`
- Test: `src/test/java/khora/kernel/event/EventGraphTest.java`

**Interfaces:**
- Consumes: `Event` (Task 2).
- Produces: `interface EventSource { Optional<Event> get(EventId id); }`; `final class EventGraph implements EventSource` with `synchronized AppendResult append(Event e)`; `sealed interface AppendResult permits Appended, Refused`, `record Appended(EventId id)`, `record Refused(EventId id, Reason reason)`, `enum Reason { MISSING_CAUSE, NOT_AFTER_CAUSES, DUPLICATE }`.

- [ ] **Step 1: Write the failing tests**
  - `appendsEventWithKnownCauses`: e1 with no causes, then e2 caused by e1 with a later `recorded` → both `Appended`; `get(e2.id())` is present.
  - `refusesMissingCause`: e2 whose cause is not in the graph → `Refused(MISSING_CAUSE)`, and the graph does not contain e2.
  - `refusesTimeNotAfterCauses`: e2 with `recorded` equal to e1's → `Refused(NOT_AFTER_CAUSES)`.
  - `refusesDuplicate`: appending e1 twice → second is `Refused(DUPLICATE)`.
- [ ] **Step 2: Run** `./mvnw -q test -Dtest=EventGraphTest` → fails to compile.
- [ ] **Step 3: Implement** with a `HashMap<EventId, Event>`.
- [ ] **Step 4: Run** → passes.
- [ ] **Step 5: Commit** `Add the in-memory event graph`.

### Task 5: Frontiers

**Files:**
- Create: `src/main/java/khora/kernel/event/Frontier.java`, `FrontierCheck.java`, `Order.java`
- Test: `src/test/java/khora/kernel/event/FrontierTest.java`

**Interfaces:**
- Consumes: `EventSource`, `EventGraph` (Task 4).
- Produces:
  - `record Frontier(SortedSet<EventId> heads)`: it stands for the heads and all their causes. `static Frontier empty()`; `static Frontier of(Collection<EventId> ids, EventSource source)` reduces the ids to heads (drops any id that is a cause, direct or indirect, of another); throws `IllegalArgumentException` if an id is unknown.
  - `FrontierCheck`: `static Validity validate(Frontier f, EventSource source)`, where `sealed interface Validity permits Valid, Invalid` and `record Invalid(EventId missing)` names the first missing event found; `static Order compare(Frontier a, Frontier b, EventSource source)`; `static Frontier combine(Frontier a, Frontier b, EventSource source)` = the heads of the union.
  - `enum Order { BEFORE, AFTER, EQUAL, CONCURRENT }`: `a BEFORE b` when a's events are a strict subset of b's.
- Closure walks are iterative (a work list), never recursive.

- [ ] **Step 1: Write the failing tests**, over e1 ← e2 ← e3 and e1 ← e4 (e3 and e4 concurrent):
  - `frontierLackingACauseIsRefused`: a source holding e2 but not e1; `validate(new Frontier({e2}))` → `Invalid(e1)`. (Done-when of #18.)
  - `unknownHeadIsInvalid`: a head the source lacks → `Invalid(head)`.
  - `ofReducesToHeads`: `of({e1, e2, e3})` → heads `{e3}`.
  - `comparesByCausalOrder`: `{e2}` BEFORE `{e3}`; `{e3}` AFTER `{e2}`; `{e3}` EQUAL `{e3}`; `{e3}` CONCURRENT `{e4}`; `empty()` BEFORE `{e1}`.
  - `combineKeepsHeads`: `combine({e3}, {e4})` → `{e3, e4}`; `combine({e2}, {e3})` → `{e3}`.
  - `longChainDoesNotOverflow`: a chain of 10 000 events; `validate` is `Valid` and `compare(empty, {last})` is BEFORE.
- [ ] **Step 2: Run** `./mvnw -q test -Dtest=FrontierTest` → fails to compile.
- [ ] **Step 3: Implement** the types above.
- [ ] **Step 4: Run** `./mvnw -q test` (whole suite) → passes.
- [ ] **Step 5: Commit** `Add frontiers as sets of events closed under causes`.

### Task 6: Replace K4's placeholders

Start only after the Issue #21 run has closed; rebase onto its final branch first.

**Files:**
- Modify: `src/main/java/khora/kernel/event/EventId.java` (the constructor accepts only `sha256:[0-9a-f]{64}`; drop "placeholder" from its Javadoc)
- Delete: `src/main/java/khora/kernel/surface/Frontier.java`, `src/test/java/khora/kernel/surface/FrontierTest.java` (the blank-frontier test goes; `latestReadsAtNoFixedFrontier` moves to a `ReadTimeTest`)
- Modify: every `surface` and `stub` file that imports `khora.kernel.surface.Frontier` → `khora.kernel.event.Frontier`; K4's tests that build `new EventId("a1")` or `new Frontier("f1")` → a test helper `TestIds.id(int n)` (`EventId.hashOf("test/" + n)`) and `new Frontier(new TreeSet<>(Set.of(TestIds.id(n))))`.
- Create: `src/test/java/khora/kernel/TestIds.java`

**Interfaces:**
- Consumes: `Frontier` (Task 5), `EventId.hashOf` (Task 2).
- The stub surface keeps comparing frontiers by equality; it builds its frontier from the ids its `Actions` returns, without a graph.

- [ ] **Step 1:** In `ContextTest`, change `blankEventIdIsRefused` to `malformedEventIdIsRefused`: `" "`, `"a1"` and `"sha256:XYZ"` → `IllegalArgumentException`. Run `./mvnw -q test -Dtest=ContextTest` → fails.
- [ ] **Step 2:** Validate the format in `EventId`; replace the placeholders as listed.
- [ ] **Step 3:** Run `./mvnw -q test` → the whole suite passes, with no `-Xlint` warnings.
- [ ] **Step 4: Commit** `Replace the EventId and Frontier placeholders`.