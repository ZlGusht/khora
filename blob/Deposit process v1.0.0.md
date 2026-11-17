---
identity__uuid: b416cac8-123a-4dcd-8ff0-350f78af1f1e
identity__type: universal
identity__created_at: 2026-05-12
identity__tags: []
aliases:
  - Deposit process
relationships__is_a:
  - "[[Process(n) v1.0.0]]"
relationships__in_context_of:
  - "[[Khora]]"
---
```toml
Description = "Captures honest signals into the Khora substrate as well-anchored entities. Invoked any time an agent or human has something worth recording mid-work — a friction, a claim, a task, a signal — and wants it preserved so downstream structures and processes can consume it without further transformation. Functional lens: deposits earn their place by being consumed downstream; the substrate exists to enhance processes."

Subprocesses = """

### Step 1 — Recognize the consumption pattern

Ask: *'What should happen to this, and why?'*

The answer routes to one of four patterns (state-snapshot held for later):

- **Knowledge-claim** — feeds the persistent knowledge graph; read on demand by future agents assembling context.
- **Action-call** — surfaces in a work queue; read by selection processes.
- **Metabolism-fuel** — feeds a loop's Process phase; digested at the next metabolism cycle. Heterogeneous content (friction, signal, surprise, mid-flow idea); unified by the consumer (the loop), not by type.
- **Signal** — accumulates as aggregate data; consumed by [[Dynamos]] and trend-watchers.

### Step 2 — Apply the matching heuristic

Each heuristic answers WHAT (deposit shape), HOW (content form + anchors), and WHY (what the consumer needs).

#### Heuristic A — Knowledge-claim
- **Recognition:** A self-contained claim believed true. Not action, not state.
- **Shape:** [[Letter v1.0.0]] with properties__type: insight.
- **Content:** Self-contained sentence(s); readable without re-loading source.
- **Anchors:** At least one outward non-fleeting anchor (concept, area, process, structure). Source anchor (e.g. a chat) is optional; chats alone are too fleeting.
- **Wikilinks in content:** every concept the claim touches. Dangling links are stigmergic marks; they accumulate pressure for future concept creation.
- **Why:** Read on demand, possibly months later. Self-containment makes it portable. Wikilinks turn each claim into a passive vote for the concepts it touches; concepts accumulating supporting claims become promotion candidates without explicit curation.

#### Heuristic B — Action-call
- **Recognition:** Names something to be done. Imperative shape.
- **Shape:** [[Letter v1.0.0]] with properties__type: task.
- **Content:** Imperative form; enough context that an actor can act without re-reading source.
- **Anchors:** belongs_to the project/area where the action lives (required — surfaces it in the right queue). Source anchor optional.
- **Group-before-create:** Before depositing fresh, ask whether this is part of a larger architecture or cluster of related actions. If yes, attach to that cluster; don't proliferate parallel singletons. Anti-hoarding pressure comes from periodic grooming, not from refusing capture.
- **Why:** Action-calls surface via belongs_to. Without that anchor, the task lives in limbo.

#### Heuristic C — Metabolism-fuel
- **Recognition:** Something noticed mid-flow that the active loop's metabolism should digest — friction, signal, surprise, or mid-flow idea.
- **Shape:** [[Letter v1.0.0]]; properties__type reflects what was noticed (friction / signal / surprise / idea). Consumption pattern is metabolism-fuel — signaled by the anchor, not the type.
- **Content:** Concrete, narrative — what happened, what was attempted, what didn't work.
- **Anchors:** in_context_of the active loop (required; places it in the right [[Reservoir]]). Source anchor optional.
- **Why:** Active-loop anchor places it in the loop's Process-phase reading. Content-as-narrative is what metabolism needs; abstract complaints don't fuel digest.

#### Heuristic D — Signal
- **Recognition:** A measurable, aggregable thing about an entity the system tracks. Not narrative, not actionable.
- **Shape:** [[Letter v1.0.0]] with properties__type: signal. Body minimal.
- **Anchors:** The entity being signaled-about. Timestamp critical (already standard via created_at).
- **Why:** Signals get aggregated; individual items have little value. Under-structure content, over-structure anchor + timestamp. [[Dynamos]] watching for thresholds trigger when accumulation crosses.

### Step 3 — Cross-cutting capture rules

- **Sticky shape-claim.** Once declared, the entity commits. No fluid reclassification.
- **No orphans.** Every deposit carries at least one anchor — relationship or in-content wikilink.
- **'How does this relate?'** is a mandatory anchor-completing question.
- **Bounded scratch.** Permissive holding for what the heuristic couldn't structure; not a dumping ground. Scratch-pressure dynamics deferred.
- **Source-tagging.** Deferred. Free-text source plus later semantic search is plausible but uncommitted.

### Step 4 — Realization back-pointers (causal chain)

When a deposit's work resolves into a new entity (a Process, a Refactor, a [[Concept v1.0.0]], a universal), the new entity carries realized_as pointing at the source Letter. The Letter doesn't disappear; it gains a forward trail. This single typed back-pointer is the substrate's causal-history mechanism — walkable both directions via Obsidian backlinks. Decision-records, audit trails, and 'why does this exist' queries fall out of this rule. No separate causal-chain machinery is needed.

### Step 5 — Validation

After every deposit, run [[khora-validate-entity]] to confirm: is_a chain reaches [[BaseEntity v1.0.0]], every relationship key is schema-blessed, frontmatter parses, body TOML parses, no unresolved Templater placeholders.

### Future operationalization

This process will eventually run as an interactive flow — see [[Deposit process as questioning flow]]. Agent invokes a CLI or skill; the question tree encodes each heuristic's recognition step; required anchors are enforced by the flow structure.

### Open extensions

- [[Project]] / [[Area]] convention for Heuristic B's belongs_to target.
- Dangling-link clustering — periodic digestion pass that finds concepts named differently across deposits and proposes canonicalization.
- State-snapshot as a fifth consumption pattern — revisit when a temporal-series consumer demands it.
- relationships__relates_to declaration on [[BaseEntity v1.0.0]] (or [[Letter v1.0.0]]) — used in practice, not yet schema-blessed.
- Grouping mechanic for action-calls — balance 'group into abstract designs' vs 'naive 1:1 attack.'
"""
```

<!-- khora:auto-related start -->
## Related
- [[Area]]
- [[BaseEntity v1.0.0]]
- [[Concept v1.0.0]]
- [[Deposit process as questioning flow]]
- [[Dynamos]]
- [[Letter v1.0.0]]
- [[Project]]
- [[Reservoir]]
- [[khora-validate-entity]]
<!-- khora:auto-related end -->
