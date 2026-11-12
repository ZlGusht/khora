# ADR-0XX: Surface logic

> **Status:** Proposed
> **Date:** 12.11.2026
> **Issue:** #1 DataStore: base and advanced capabilities (task: decide the query's shape)
> **Related:** ADR-0XY (surfaces); ADR-0YC (the runtime and sandbox)
> **Number:** to be assigned when added to the repository

## Context

Surfaces and behaviors carry logic. Declarative logic can be analyzed, pushed down into datastores and kept live incrementally; opaque logic can only be run. This ADR fixes the declarative language. Opaque logic runs in the sandbox (ADR-0YC).

## Terms

| Term | Meaning |
| --- | --- |
| **Query** | Declarative read logic: an envelope and a body. |
| **Operator** | One building block of a body: sets of entities or relations in, a set out. |
| **Propagate** | The general operator: values spread over relations, combined by an algebra, until a stop condition. |
| **Algebra** | A pair of operations (⊕ combine, ⊗ extend) with declared properties. |
| **Annotation** | A value carried with each result row: confidence, age, provenance. |
| **Operation** | A write a surface or behavior offers: its signature, conditions, and the effects it translates to. |

## Decisions

**1. Declarative logic is data, not text.** A query is a structure that can be stored, versioned and analyzed. A text syntax may come later. Engine plan formats such as Substrait are used only inside the runtime, never by callers.

**2. A query is an envelope plus a body.**

```
Query {
  body:        Operator
  time:        { frontier, by: recorded | occurred, meaning: then | now }   // default meaning: then
  annotations: [confidence, age, ...]
  semantics:   set | multiset
  return, order, page
}
```

**3. The body is a tree of operators:** `Scan`, `Filter`, `Propagate`, `Aggregate`, `Not`, and capability operators such as `Search` and `Spatial`. Joins and match patterns are shorthand that expands into `Propagate`. Path modes follow ISO GQL; walk is the default.

**4. `Propagate` is the general operator.**

```
Propagate { seed, over, algebra, update?,
            until: steps(n) | hops{min,max} | fixpoint | converged(ε),
            emit:  final | per-step | paths }
```

- A join is `steps(1)`. Reachability, rollups and blast radius run to `fixpoint`. Iterative scores run until `converged(ε)`.
- An update is declarative, or opaque and run in the sandbox.
- Negation, top-k and ordering by score stay separate operators.
- Algorithms propagation cannot express (Leiden, embeddings, max flow, matching) live in a small library.

**5. Algebras are named and declare their properties.** The runtime applies an optimization only when the algebra declares the property it needs.

| (⊕, ⊗) | Computes |
| --- | --- |
| (or, and) | Reachability |
| (min, +) | Shortest path |
| (max, ×) | Most probable path |
| (max, min) | Bottleneck |
| (+, ×) | Counts, weighted sums |
| (min, id) | Components |
| Provenance polynomials | Lineage |

**6. Each algebra declares how it is maintained incrementally.**

| Algebra | Insertions | Deletions |
| --- | --- | --- |
| Rings | Incremental | Incremental |
| Semirings without inverses | Incremental | Count derivations, or re-derive |
| Non-monotone operators | Re-evaluate the stratum | Same |

Only declarative logic is kept live incrementally. Opaque logic is recomputed on request or after a pause in changes, and its results say so.

**7. Merge rules are algebras applied to concurrent writes:** counters add, sets unite, maxima take the larger. An attribute without a merge rule conflicts.

**8. Surfaces and behaviors declare the operations they offer.** A write through stacked surfaces translates downward until it reaches effects. A surface that offers no operations is read-only.

**9. Annotations are carried as columns and combined by the algebra.** Confidence and age are carried by default. They stay cheap: kinds without uncertainty have a constant confidence of 1, which drops out.

## Costs and risks

- A fixpoint terminates only for well-behaved algebras. Everything else needs a step, hop or convergence bound.
- Declared algebra properties are trusted. A false declaration makes optimizations return wrong results.
- Probabilistic algebras assume independent inputs.
- Unrecognized propagation patterns run in a generic loop: correct, but slow until specialized.
- Propagation across several datastores moves data to wherever the iteration runs.

## Alternatives considered

- **Cypher or another text language as the model:** ties khora to one backend family.
- **Substrait as the model:** no traversal, no time, no kinds.
- **Separate mechanisms for patterns, graph algorithms and traversals:** three meanings to keep consistent.

## Open questions

| Question | Lean |
| --- | --- |
| Incremental engine | Adopt rather than build; evaluate DBSP/Feldera and Differential Dataflow |
| Syntax for operations | Open |
| First library algorithms | Community detection and embeddings, which the specs need |
| Text syntax | Later, as a layer that produces the structure |

## MVP

Decisions 1–5 and 7–9. Incremental maintenance (6) for rings first; the library comes with the first spec that needs it.
