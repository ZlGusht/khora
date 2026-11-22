---
identity__uuid: 4b5bbb17-5ea2-40ab-9c69-5decb60c0bf1
identity__type: particular
identity__created_at: 2026-11-22
identity__tags: []
relationships__is_a:
  - "[[Digest v1.0.0]]"
relationships__digest_of:
  - "[[Issue #18 Event types ids and clock]]"
relationships__in_context_of:
  - "[[Milestone Stage 1 kernel core]]"
properties__type: "digest"
properties__priority: 2
properties__resolved: false
properties__author: agent:claude-opus-5-5
---
## Outcome

K1 is done as provisional types in `src/kernel/` (`khora.kernel.event`, `khora.kernel.json`); 80 tests pass on Java 25. What the kernel now means by an event:
- An event is something that happened, kept forever with what it knew of; an accepted action, with who acted, is its one kind so far.
- An accepted assert carries statements `(identity, attribute, value)`; each is identified by its event and its place, `e/n`, and `(,,)` brings a new thing into being.
- An event's id is a salted hash of its canonical JSON. Recorded time is a hybrid logical clock.
- A frontier is a set of events closed under their causes; it can be checked, compared (including "concurrent") and combined.
- K4's `EventId` and `Frontier` placeholders are replaced; `Basic` has the `read` definition.

Not done: named links for reads, translations and reactions; other kinds of event; keeping rejected actions; attribute definitions; atomic writes across events (K3); conflict checks (K9). Issue #18's done-when on effects no longer applies, as effects are not events.

## Findings

- The run's main result is the event model, decided with [[Simeon Petkov]]: [[An accepted action is one kind of event]], [[Assertions are tuples with identity]], [[An event carries several assertions]], [[Event ids are salted hashes of canonical JSON]]; with K4's [[An event's causes are what it knew]] and [[Reads are events]].
- One tuple shape lets every surface index and diff the same thing; it pays off once something reads assertions (K6, B2). Its costs are wide reads and attributes that must carry meaning.
- A content hash earns its place only across trust boundaries: integrity and history commitment matter for interoperability (PRD P0, ADR-0XZ decisions 21–22), not inside one deployment. The salt keeps ids from revealing content.
- A frontier must hold only what a read depends on: an unrelated thread's events must not make a read wait or a write conflict.
- Two reviews found eight Important issues, all fixed with tests first.

## Frictions

- Two sessions on one vault: a second Obsidian took over the shared CLI socket and cut the other session off; `khora-sync` does not run in a worktree; K1's entities sat untracked in K4's checkout; two rebases onto K4, one with conflicts.
- The model of asserts was rebuilt three times during Operate; the tuple model came out of the conversation, not the Laydown.
- K4's Digest attributes the duplicate Decision "Events are the unit that gives identity and connects surfaces" to the K1 session; this session did not write it, and it is still untracked in the shared checkout.

## Ideas

- One genesis event per log, the only event without causes, whose id identifies the deployment.
- Named identities with a form of their own, so a reference says whether it names something or points to an event.
- The canvas `dashboards/Event Model.canvas` shows the model by meaning; its Spec is a draft to agree.

## Considerations

- Merge order: #35 (K4), then #36 (K1), which GitHub retargets to main.
- Proposed issues:
  - Update View from K1's and K4's Decisions: ADR-0YA, ADR-0YB, ADR-0XY, the PRD, the implementation plan and the kit documents are stale ([[Documents lag K1's Decisions]]).
  - Large integers and RFC 8785: store them as strings, or record a deviation ([[Large integers are not canonical JSON as RFC 8785 writes them]]). It contradicts [[Event ids are salted hashes of canonical JSON]] as written and needs a decision.
  - Named links for the basic read, translation and behavior events, with K4's "The surface contract speaks events".
- The milestone gate speaks of "an outcome and effects"; with no effect events it needs restating.
- Unresolved Letters carried here: security ([[Who acted is claimed, not proven]], [[Salts are not yet shredded with content]], [[Parsed JSON has no size limits]], [[Named identities and event ids share one form]]); change later ([[Reads, translations and reactions have no named links yet]], [[The stub surface's frontiers are not real frontiers]], [[The clock serves one writer]]); performance ([[Frontier operations walk whole histories]], [[Events are hashed twice and JSON copied often]], [[Reads as events multiply the log]]); kit ([[A second Obsidian takes over the CLI socket]]).
- Pending outside this run: `dev/observe-flags` (Issue Loop and Milestone Loop v1.1.0, khora-sync across versions) is committed but not pushed.
- Deferred review minors: the stub relies on unique action ids; `Identity` accepts an index beyond int range; small tidy-ups in `Assertion`, `Change` and the stub.