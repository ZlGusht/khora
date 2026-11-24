---
identity__uuid: 5ff4832d-125f-47b7-aa91-e5ad895eb08e
identity__type: particular
identity__created_at: 2026-11-24
identity__tags: []
relationships__is_a:
  - "[[Digest v1.0.0]]"
relationships__digest_of:
  - "[[Issue #25 Query as a data structure]]"
relationships__in_context_of:
  - "[[Milestone Stage 1 kernel core]]"
properties__type: "digest"
properties__priority: 2
properties__resolved: false
properties__author: agent:claude-opus-5-5
---
## Outcome

A search is a `Query` in the new `query` module: an envelope (frontier, set or multiset, returned columns, order, page) around a body of `Scan`, `Filter`, `Not` and `Aggregate`. A query is checked when it is built, a malformed one is refused, and it is stored as its canonical JSON in a statement of `khora/query`. A `Datastore` answers queries; `EventsDatastore` works answers out from a log's events, and `DatastoreContract` holds the 27 tests every datastore passes. 144 tests pass in `events`, `query` and `surfaces`. The `log-nats` tests did not run: Docker was not reachable.

Not done: the graph datastore (K6) is not built, so the issue's second box rests on `EventsDatastore` until K6 extends the contract. The third box needs B2.

## Findings

- Retracting a thing's creation leaves its other statements, so a scan of an attribute still finds it ([[Retracting a thing's creation leaves the statements about it]]).
- `Evaluator` answers over a `Graph` of three lookups (things, statements with an attribute, a thing's values of an attribute). K6 can give one from materialized state and reuse the evaluator.

## Frictions

- The run's design choices are the agent's, made without a team member: the run was one go on request. They are listed in the run's Design section for review in the pull request.

## Considerations

- Review the design choices before K6 builds on them: rows of columns, `Scan` of things versus statements, negation only as `Not`, the value order (integers, strings, references), and `set` semantics applied after every operator.
- [[Query answers have no bounds]] matters once queries come through the API (K12).
- [[A stored query has no version and its time is a frontier only]]: add a version before the JSON first changes.
- [[EventsDatastore works every answer out from all events]]: K6 replaces it for anything but checking.