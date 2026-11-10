# ADR-0XY: Entity sources, time and schema

> **Status:** Proposed
> **Date:** 06.11.2026
> **Issue:** #1 DataStore: base and advanced capabilities
> **Related:** ADR-0XX Query model and propagation (what queries look like); ADR-014 (views)
> **Number:** to be assigned when added to the repository

## Context

khora stores data in several typed stores, mapped from the schema store, so entities exist virtually: a billion time entries are entities even though they are not in a graph store. Entities also come from outside khora (external entities, synced to where their content is) and from computation (computed entities and aggregators).

Queries will be built and distributed across all of these by a planner (an aggregator service, e.g. Substrait-based or custom). The sample system specs add requirements on that layer:

- **Point-in-time reads:** log replay everywhere; valid time and planned future states in the built environment; checkpoints in application development.
- **Change propagation:** proxy sync, cache invalidation, drift detection, triggers.
- **Honest absence:** "cannot evaluate", never "pass"; "unknown", never "unaffected".
- **Specialized data:** spatial data, time series and similarity search, which only some backends handle well.

This ADR fixes the contract between the planner and the places entities come from, the clock all reads share, and where the schema and the location of data live. The shape of queries is the subject of ADR-0XX.

## Terms

| Term | Meaning |
| --- | --- |
| **Entity** | Anything in khora. Virtual: it exists through its kind, wherever its data is held. |
| **Kind** | What an entity is, defined in the schema. |
| **Source** | Anything that holds or produces entities: a datastore, an external source, a computed definition, an aggregator. |
| **Claim** | A source's registered statement of what it holds: kinds, attributes, relations. |
| **Schema store** | The pinned store holding the schema and claims. |
| **Action log** | The record of every action. Its positions are khora's clock. |
| **Planner** | Turns a query into a plan and distributes it across sources. |
| **Fragment** | The part of a plan sent to one source. |
| **Capability** | Something a source can execute itself. |

```
Caller ── Query{envelope, body} ──► Planner ◄── schema store (kinds, claims @ snapshot)
                                       │
                         plan = tree of operators
                                       │
                 ┌─────── fragments, negotiated per source ───────┐
                 ▼                     ▼                          ▼
            datastores          external sources          computed (inlined)
                 └──────────── batches + metadata ────────────────┘
                                       │
                     planner runs what wasn't pushed down
                                       ▼
                               Result + metadata
```

## Decisions

### Sources

**1. The planner plans over every entity source, not only datastores.** Datastores, external sources, computed entities and aggregators share one source contract (decision 2); `DataStore` is one implementation of it. Computed entities defined by a `Query` are expanded inline by the planner, like views; computed entities defined by an opaque process are executed as a whole.

**2. Every source implements one contract.**

```
interface EntitySource {
  capabilities(): Capabilities
  negotiate(fragment): Pushdown
  execute(fragment, ctx): Stream<Batch>, ResultMeta
  changes?(since: LogPosition): Stream<Change>    // capability
}

ctx = { principal, snapshot: LogPosition, deadline, annotations }
```

`ResultMeta` is the result metadata defined in ADR-0XX. This contract replaces `query` and `search` on the store itself; callers use `Query`.

**3. Pushdown is negotiated per operation.** For each operation in a fragment a source answers *exact* (fully applied), *inexact* (narrows the results; the planner re-checks) or *unsupported* (the planner runs it, or reports that it cannot). A source never silently drops part of a fragment.

**4. Advanced capabilities are named and declared,** each with its own contract test suite, run only for sources that declare it. Examples: traversal, search modes (fuzzy, semantic), grouped aggregation, valid time, spatial, time-series windows, change feeds, library algorithms. Every planner fallback for a capability is tested against a source that supports it natively.

**5. Entities have one global id.** The schema store maps it to each source's own key, so the planner can join across sources.

### Time

**6. Every read is pinned to a snapshot.** Reading as of a past record time is part of the base contract. A source may answer that it cannot serve a snapshot (typical for external entities); the planner then reconstructs from the action log where possible, or reports the gap. Valid time is an advanced capability.

**7. Record time is a position in the action log.** Each source reports the last log position it has applied. A query pinned to a later position waits or comes back partial. External sources report their last sync; computed entities report the oldest applied position among their inputs.

### Schema

**8. The kernel is a fixed schema; everything above it is data.** The firmware holds the kernel schema and the location of the schema store. The upper ontology (an open choice) and domain schemas are data in the pinned schema store, which implements the base source contract and can be queried like any other source. The planner keeps the whole schema in memory as its catalog.

**9. Location is data, registered as claims.** Sources register claims in the schema store: which kinds (all, or those matching a predicate), attributes and relations they hold, with which capabilities. Kinds say what something is; claims say who holds it. The planner looks claims up, and asks "who has this?" only on a miss, for new sources, and later across khoras.

**10. The schema and claims are versioned by log position.** A query at a past position uses the schema and claims in force then. Moving a kind to another source is an action with a cutover position.

## Caveats

- **Snapshot consistency depends on sources.** It holds only if every source reports its applied log position honestly and promptly.
- **External sources rarely keep history.** Reads of external entities in the past are only as good as what the action log recorded at sync time.
- **Fallbacks are slow.** A capability the planner emulates (e.g. traversal by expanding one hop at a time) is correct but can be far slower than a native source.
- **Claims can conflict.** Two sources may claim the same attribute of the same entity; until a rule is decided (see open questions), the planner must surface the conflict rather than pick silently.
- **The schema store is a single point of startup.** khora cannot plan anything until the schema store is reachable.

## Consequences

**Positive**

- Any backend that can answer filtered scans can be a source; richer backends declare more. Backends stay swappable.
- Datastores, external sources and computed entities are planned the same way.
- Missing data, unsupported operations and stale sources are always visible in results.
- Past states are reproducible: data, schema and claims all share the action log's clock.

**Negative**

- The planner carries most of the complexity: claim lookup, negotiation, fallbacks, cross-source joins.
- Every source has to track and report its applied log position.

## Alternatives considered

- **Each store exposes its own query language:** simple per store, but callers would need to know where data lives, which virtual entities rule out.
- **Discovering sources by asking on every query:** replaced by registered claims with lookup, falling back to asking on a miss.
- **Wall-clock timestamps as record time:** clocks differ between sources and cannot order actions reliably; the action log already gives one order.
- **Hard-coding the schema:** only the kernel is fixed; everything above it is data, as the self-containment requirement of the PRD asks.

## Open questions

| Question | Lean |
| --- | --- |
| Batch format for results | Arrow record batches; decide with the engine |
| Federation engine | DataFusion if Rust, Calcite if JVM, custom otherwise; depends on the language |
| Substrait as the wire format to engine-backed stores | Likely, for the relational parts |
| Shape of the action log; what is computed from it and what is reified | Open; decision 7 depends on it |
| Exact base contract of a source (scans over entity and relation tables?) | To settle while implementing decision 2 |
| Conflicting claims: two sources claim the same attribute of the same entity | Precedence rule, or both values with provenance and a contradiction flag |
| Validity of materialized computed entities and who triggers recomputation | Open |

## Out of scope (TODO)

- Scope mechanics, including whether scope filtering must be pushed down to sources.
- Mixins and the rest of the kernel schema.
- The process engine.
- Interoperability between khoras, including cross-khora claims.

## Done when (issue #1)

- The source contract (decisions 2–4) covers the collected cases from the sample specs.
- `DataStoreContractTest` covers the base contract; each named capability has its own suite; each fallback is tested against a native source.
