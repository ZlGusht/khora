---
identity__uuid: 0293217f-499d-425b-a9fe-f2b84d279ffc
identity__type: particular
identity__created_at: 2026-11-24
identity__tags: []
relationships__is_a:
  - "[[Process Instance v1.0.0]]"
  - "[[Issue Loop v1.0.0]]"
relationships__in_process:
  - "[[Milestone Stage 1 kernel core]]"
relationships__in_context_of:
  - "[[Surface logic]]"
properties__status: closed
properties__started_at: 2026-11-24
properties__current_phase: Process(v)
properties__issue: 25
properties__author: agent:claude-opus-5-5
properties__ended_at: 2026-11-24
---
Issue #25 (plan task K8) of [[Milestone Stage 1 kernel core]]: the `Query` as a stored, validated data structure, the search a planner hands to datastores ([[Stage 1's tasks and gate are stated in events]], item 2). Run by an agent in one go, on Simeon Petkov's request, built on the Issue #18 branch.

## Loop Config

- **Domain:** Complicated. ADR-0XX decisions 1 to 3 fix the shape; the work is choosing the operators' exact meaning over statements, and the validation rules.
- **Scope of Authority:** a new `query` module under `src/`, small additions to `events` that it needs, `src/README.md`, `settings.gradle.kts`, and this run's entities in `blob/`. Nothing in `docs/`.
- **Reservoir capacity:** one pull request.

## Vector

- **Attractor:** a `Query` (envelope and body of `Scan`, `Filter`, `Aggregate`, `Not`) that is validated when built, refused when malformed, stored as canonical JSON in an assertion, and answered by a datastore at a frontier. A contract suite every datastore must pass.
- **Repulsor:** `Propagate`, joins and paths (stage 2); `Search` and other capability operators, pushdown and planning (P2); annotations; reads by recorded or occurred time (K7, not staged); the graph datastore itself (K6); the generic entity view (B2); changes to the kernel's read path.

## Design

The agent's calls, for review in the pull request; no team member agreed them during the run.

- **Rows.** An operator gives rows: each maps column names to values. A value is an assertion's value, a literal or a reference.
- **Scan.** `Scan(as)` gives one row per thing brought into being by an empty tuple that holds at the frontier. `Scan(as, attribute, value)` gives one row per holding statement with that attribute: the thing it is about, and its value.
- **Filter.** Keeps the rows a condition holds for. Conditions: `has` (the thing in a column has a holding statement with an attribute, and optionally a value), `compare` (`eq ne lt le gt ge` against a value), `and`, `or`. Negation is the `Not` operator, not a condition (ADR-0XX decision 4).
- **Not.** The rows of its input with no matching row in `minus`, matched on `minus`'s columns, which must all be columns of the input.
- **Aggregate.** Groups by columns and computes `count`, `sum`, `min` or `max` into a new column. Values order as integers, then strings, then references.
- **Envelope.** `at` (optional; absent means everything the datastore holds), `semantics` (`set` by default, applied after every operator), `return` (columns), `order`, `page`. A page needs an order. Annotations and time by recorded or occurred are left out.
- **Validation.** Every operator knows its columns. Building a query refuses a column that its input does not give, a duplicate column, a page without an order, and unknown or missing JSON members.
- **Stored.** A query's canonical JSON is a string literal of the `khora/query` attribute, so it is kept in the log like any statement and read back with `Query.fromJson`.
- **Datastores.** `Datastore.answer(Query)` gives an `Answer`: the frontier it answered at, the columns and the rows. `EventsDatastore` answers from the events of a log, working everything out on each call, as `Statements` does. `DatastoreContract`, in the module's test fixtures, holds the tests every datastore passes; K6's graph datastore extends it.

## Plan

1. `events`: `Statements.at`, every statement that holds at a frontier, which `Statements.about` now filters; `Assertion.valueJson` and `valueOf`, so other modules write values as assertions do.
2. `query` module: `Operator`, `Condition`, `Values`, and `Query` with its checks.
3. `QueryJson`: canonical JSON that refuses unknown and missing members; `Query.stored` and `Query.from` keep a query as a statement.
4. `Datastore`, `Answer`, `Graph` and `Evaluator`; `EventsDatastore` over a log.
5. `DatastoreContract` in the test fixtures; `EventsDatastoreTest` extends it.
6. `src/README.md` names the module.

## Observe

- **Done:** a query is built only if valid, and refused otherwise, in code and from JSON (the issue's first box). It is stored as a statement, read back and answered. `EventsDatastore` answers every operator, condition and envelope member at any frontier, and the contract suite holds that for any datastore.
- **Not done:** the graph datastore (K6) does not exist yet. The issue's second box is met by `EventsDatastore` and waits for K6 to extend `DatastoreContract`. The third box needs the generic entity view (B2).
- **Checked:** 78 tests in `events`, 40 in `query` and 26 in `surfaces` pass on Java 25, with no compiler warnings. The `log-nats` tests did not run: the machine's user may not reach Docker. `log-nats` compiles against the changed `events`. khora-validate passes on the run and its Letters. khora-stale reports nothing from this run. Nothing in `docs/` changed.
- **Flagged:** [[Query answers have no bounds]] (security), [[EventsDatastore works every answer out from all events]] (performance), [[A stored query has no version and its time is a frontier only]] (change-later), and the finding [[Retracting a thing's creation leaves the statements about it]].