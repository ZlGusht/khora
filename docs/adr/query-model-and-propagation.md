# ADR-0XX: Query model and propagation

> **Status:** Proposed
> **Date:** 06.11.2026
> **Issue:** #1 DataStore: base and advanced capabilities (task: decide the query's shape and record it as an ADR)
> **Related:** ADR-0XY Entity sources, time and schema (how queries are planned and executed); ADR-014 (views)
> **Number:** to be assigned when added to the repository

## Context

Issue #1 asks for graph queries and semantic search through a small custom model rather than raw Cypher, so backends stay swappable. The model was called `GraphQuery`; it is renamed `Query`, since it covers more than graphs.

The sample system specs (built-environment review, lifelong learning, company brain, application development) need, among others:

- **Traversal and paths:** network tracing, requirement-to-test traceability, blast radius, nested work, prerequisite paths.
- **Aggregation and rollups:** metrics from seats to collectives to the organization; costs from quantities.
- **Search and ranking:** context assembly, concept identity across contexts, duplicate detection.
- **Uncertainty:** beliefs, claims, findings and extractions with confidence and age, which must combine correctly as results are joined and propagated.
- **Simulation and logic:** values and rules spreading over networks and models.

Most of these are traversals or propagations of values over relations. Treating them as one primitive, which can be optimized per pattern later, keeps the model small and gives every system the same semantics. This ADR fixes the shape of `Query` and that primitive. How queries are planned and run across sources is the subject of ADR-0XY.

## Terms

| Term | Meaning |
| --- | --- |
| **Query** | A request: an envelope plus a body. |
| **Envelope** | What describes the request: snapshot, annotations, return, order, page. |
| **Body** | What to compute: a tree of operators. |
| **Operator** | One building block of a body: takes sets of entities or relations in, gives a set out. |
| **Propagate** | The general operator: values spread over relations, combined by an algebra, until a stop condition. |
| **Algebra** | A pair of operations (⊕ combine, ⊗ extend) that defines how values combine across joins, aggregation and propagation. |
| **Annotation** | A value carried with each result row (confidence, age, provenance), combined by the algebra. |
| **Planner** | Turns a query into a plan and distributes it across sources (ADR-0XY). |

## Decisions

**1. `Query` is khora's own logical model.** Engine plan formats (Substrait, an engine's internal plans) are used only below the planner, never by callers.

**2. `Query` is a data structure, not a text language.** Queries are data: they can be stored as entities, versioned, and used as definitions of computed entities and of an algorithm's input. A text syntax may be added on top later. Path modes take their names and definitions from ISO GQL (walk, trail, acyclic, simple); walk is the default.

**3. A `Query` is an envelope plus a body.**

```
Query {
  body:        Operator
  time:        { asOf: LogPosition, validAt? }
  annotations: [confidence, age, ...]
  semantics:   set | multiset
  return, order, page: { limit, cursor? }
}
```

`time` is defined in ADR-0XY. `semantics` chooses whether duplicates are removed (set) or kept (multiset).

**4. The body is a tree of operators.**

```
Operator =
  | Scan(kind | relation)                    // from whoever claims it
  | Filter(Operator, predicate)
  | Propagate(seed: Operator, over: Operator, algebra, until)
  | Aggregate(Operator, groupBy, algebra)
  | Not(Operator, Operator)
  | Search(...) | Spatial(...) | ...         // capability operators
```

Match patterns with steps, hop ranges and directions are shorthand that the planner expands into `Propagate`. Named forms such as joins and aggregation are kept as shorthand for readability and to give the planner obvious optimization hooks; their meaning is defined by `Propagate`.

**5. `Propagate` is the general operator of the query body.** It propagates values from a seed over relations, combining them with an algebra, until a stop condition:

```
Propagate {
  seed:    Operator                         // starting nodes with initial values
  over:    Operator                         // relations, direction, edge predicate
  algebra: Named | { combine ⊕, extend ⊗, zero, one, properties }
  update?: Expression | Process             // default: v ← v ⊕ incoming
  until:   steps(n) | hops{min, max} | fixpoint | converged(ε)
  emit:    final | per-step | paths
}
```

- A join is `steps(1)`; a fixed-length pattern is `steps(n)`; a hop range is `hops{min, max}`; reachability, rollups and blast radius run to `fixpoint`; PageRank-like computations run until `converged(ε)`.
- Aggregation is the ⊕ half of a step.
- Updates come in three levels the planner can see into to different degrees: a declarative algebra, rule-guarded updates (predicates over steps or node state, as expressions), and opaque updates (a process the planner knows only by what it reads).
- Operations that are not monotone (negation, top-k, ordering by score) stay separate operators. Negation inside recursion is layered: the inner part is computed completely before it is negated.
- Algorithms that propagation cannot express (Louvain and Leiden, random-walk embeddings, max flow, matching) live in a small library outside `Propagate`. PageRank, BFS, shortest paths, connected components, label propagation and belief propagation are presets of `Propagate`.
- Running `Propagate` as a long-lived process, including simulations, is deferred to the process engine.

**6. Algebras are named and declare their properties.** The standard set:

| Algebra (⊕, ⊗) | Computes |
| --- | --- |
| (or, and) | Reachability, hop-bounded traversal |
| (min, +) | Shortest or cheapest path |
| (max, ×) | Most reliable or most probable path |
| (max, min) | Widest path, bottleneck |
| (+, ×) | Path counts, weighted sums, iterated centrality |
| (min, id) on labels | Connected components |
| Provenance polynomials | Lineage: which inputs produced a result, and how |

Each algebra declares properties such as "⊕ picks one value" or "⊗ never decreases a value". The planner applies optimizations (Dijkstra, early termination, pushing aggregation through joins) only when the required properties are declared.

**7. Metadata exists at three levels.**

- **Query:** snapshot, deadline, required completeness, annotations to carry, budget.
- **Result:** completeness (complete, partial with a reason, unknown), applied log position, next cursor, cost, and for iterative runs the step count and residual.
- **Value annotations:** confidence, age, provenance and similar, carried as columns and combined by the algebra through joins and propagation.

Annotations are carried when a query asks for them. Confidence and age are carried by default. Defaults must stay cheap: kinds without uncertainty have a constant confidence of 1, which the planner drops; age is derived from timestamps sources already report.

## Caveats

On `Propagate` and algebras:

- **Termination.** A fixpoint is guaranteed only for well-behaved algebras. Everything else needs `steps`, `hops` or `converged`.
- **Declared properties are trusted.** An algebra that declares properties it does not have makes optimizations like Dijkstra return wrong results.
- **Probabilities assume independence.** The (max, ×) and sum-product algebras are exact only for independent inputs. Correlated inputs need provenance and a separate probability computation, which is expensive in general and approximated by sampling.
- **Walks are the natural path mode.** Trails, acyclic and simple paths do not fit the fixpoint and are restricted or expensive.
- **Not everything is propagation.** Louvain and Leiden, random-walk embeddings, max flow and matching stay library algorithms.
- **Generic execution is slow.** A propagation the planner does not recognize runs in a generic loop: correct, but slow until the pattern is recognized and specialized (BFS, Dijkstra, GraphBLAS, pushdown to a graph store).
- **Cross-source propagation moves data.** Relations spread across several sources pay the cost of moving batches to wherever the iteration runs.
- **Reproducibility.** Randomized and convergence-based runs record their seed and threshold.

On queries and metadata:

- **Set versus multiset.** Removing duplicates costs work; each query chooses.
- **Default annotations.** Confidence and age on every query are cheap only while they fold to constants or derive from existing timestamps. Kinds that carry real uncertainty pay for it in every join.
- **Performance is not restricted by the algebra.** The algebra defines meaning, not execution: queries without recursion never iterate, single steps are ordinary joins and fixed hops are unrolled. Distributivity of ⊗ over ⊕ also enables optimizations, such as pushing aggregation through joins.

## Consequences

**Positive**

- One meaning for traversal, joins, aggregation, rollups, uncertainty propagation and many graph algorithms; optimizations are added per pattern without changing the model.
- Queries are data, so they can be stored, versioned, inspected and reused across the khora.
- Uncertainty and provenance propagate the same way in every system, instead of being reimplemented per system.

**Negative**

- Until patterns are recognized, general propagation is slow.
- The planner must recognize patterns to be fast, which concentrates complexity there.
- Callers that expect a text query language have to wait for a syntax layer.

## Alternatives considered

- **Raw Cypher or another text language as the model:** ties khora to one backend family and makes queries harder to generate, inspect and store as data.
- **Substrait as the logical model:** covers the relational core well, but has no traversal, as-of reads or similarity search, and knows nothing about kinds. Kept as a candidate wire format below the planner (ADR-0XY).
- **Separate mechanisms for patterns, graph algorithms and custom traversals:** replaced by `Propagate` with three update levels plus a small algorithm library.
- **Folding aggregation and joins entirely into `Propagate`, without named forms:** same meaning, but the planner would have to recognize every common shape before optimizing it.

## Open questions

| Question | Lean |
| --- | --- |
| Which algorithms go into the library first | Those the sample specs need: community detection for the company brain, embeddings for search |
| Validity of materialized results (algorithm outputs) and who triggers recomputation | Open; part of the process engine work |
| Text syntax for people and agents | Later, as a layer that produces the data structure |

## Out of scope (TODO)

- The process engine, including simulations and running `Propagate` as a process.
- Scope mechanics, including whether scope can be expressed as an algebra.
