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

The kernel speaks only in events. 135 tests pass on Java 25, the NATS ones against NATS 2.12.15 in Docker.
- **Events** (`events`): an event is something that happened, kept with what it knew of; an accepted action is its one kind so far. An accepted assert carries tuples `(identity, attribute, value)`; `(,,)` brings a thing into being, and a tuple is known by its event and place, `e/n`. Ids are salted hashes of canonical JSON, recorded time is a hybrid logical clock, frontiers are sets of events closed under causes, and named links point into an event's causal past.
- **The log** (`events`, `log-nats`): `EventLog` stores the events themselves, each once, filed under its id on NATS.
- **Surfaces** (`surfaces`): a surface receives the events its interests match and emits actions in response; an authority accepts or rejects; only accepted actions become events, delivered in log order.
- K4's request-and-response contract and K2's records with action, outcome, effect and observation types are replaced; the first plan's six tasks were done before.

Not done: asynchronous delivery, access checks at delivery, an index of what events are about, attribute definitions, link names per kind of action, behaviors, keeping rejections. #18's done-when on effects no longer applies.

## Findings

- The event model came out of the conversation, not the ADRs: [[An accepted action is one kind of event]], [[Assertions are tuples with identity]], [[An event carries several assertions]], [[Event ids are salted hashes of canonical JSON]], [[Surfaces receive and emit events by interest]], [[Reads return statements and writes follow a read]], [[Effects are not a notion of their own]], [[The log stores events themselves, each filed under its id]]; with K4's [[An event's causes are what it knew]] and [[Reads are events]].
- Building it showed what the conversation had not: tuples of one event must name each other, as `/n`, since the id is known only after hashing.
- Free emission by interest needs nothing beyond events except interests and the authority for a thing; streams and connections went.
- Delivery order is the subtle part: a review found surfaces receiving events before their causes, fixed with one queue in log order.
- K1, K2 and K4 built the same things in parallel under different names (event and record, graph and store, attach and subscribe). Folding them together cut the surface code from 23 types to 6.
- A content hash pays off only across trust boundaries; the salt keeps ids from revealing content.

## Frictions

- Two sessions shared one vault: a second Obsidian took over the CLI socket; CLI calls without the config home started stray Obsidians that removed it again; entities had to be carried between checkouts, until a permission rule blocked copies and the vault checkout itself moved to this branch.
- The design was rebuilt several times in Operate: asserts three times, surfaces twice. Laydown did not reach the tuple or emission models.
- Docker on Linux: the user was not in the `docker` group, the Gradle daemon kept its old groups, and the NATS test container wrote root-owned folders the tests could not delete.
- K4's Digest attributes the duplicate Decision "Events are the unit that gives identity and connects surfaces" to this run, which did not write it; it is still untracked.

## Ideas

- One genesis event per log, the only one without causes, identifying the deployment.
- A form of their own for named identities, so a reference says whether it names something or points to an event.
- Connections between surfaces as a restriction on top of interests, if the topology needs governing.
- `dashboards/Event Model.canvas` shows the model by meaning; its Spec is a draft to agree.

## Considerations

- Merge #36 with a merge commit: it carries main's merge of #35 and #37.
- Proposed issues:
  - Update View from this run's Decisions: ADR-0YA, ADR-0YB, ADR-0XY, the implementation plan, and the root README's "action, outcome, effects" picture are stale ([[Documents lag K1's Decisions]]).
  - Large integers and RFC 8785: strings, or a recorded deviation ([[Large integers are not canonical JSON as RFC 8785 writes them]]).
  - An index of statements and of what events are about, beside the log (K6, B1): [[Reading what a thing is about scans the whole log]], [[Every checked write reads the whole log]].
  - Delivery: asynchronous, with access checks ([[Delivery is synchronous under one lock]], [[Delivery checks no access]]).
  - Link names per kind of action, with behaviors and their triggers ([[Link names are free text]]).
- The decisions from the 18.11 call listed in #37 are not recorded; "reads are not logged" contradicts [[Reads are events]].
- The milestone gate speaks of "an outcome and effects"; with no effect events it needs restating.
- Unresolved Letters carried here: security ([[Who acted is claimed, not proven]], [[Salts are not yet shredded with content]], [[Parsed JSON has no size limits]], [[Named identities and event ids share one form]], [[Any caller may retract any assertion]], [[Anyone who can publish to the NATS stream can add events]]); change later ([[Reads, translations and reactions have no named links yet]], [[The clock serves one writer]], [[Retraction needs attribute definitions]], [[One authority per kernel, and an unused budget]], [[The old log stream is left behind]]); performance ([[Frontier operations walk whole histories]], [[Events are hashed twice and JSON copied often]], [[Reads as events multiply the log]], [[Attached surfaces remember every event they handled]]); kit ([[A second Obsidian takes over the CLI socket]]).
- Outside this run: `dev/observe-flags` (Issue Loop and Milestone Loop v1.1.0) is committed but not pushed.