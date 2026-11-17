---
identity__uuid: 7acad70c-0c24-4dc5-b7da-dc531264b711
identity__type: universal
identity__created_at: 2026-05-12
identity__tags: []
aliases:
  - Process Instance
relationships__is_a:
  - "[[BaseEntity v1.0.0]]"
relationships__in_context_of:
  - "[[Khora]]"
---
```toml
Description = """
A single run of a process. When a process is executed, one Process Instance is opened (status = open) and closed (status = closed) when the run reaches its natural stopping point or a terminal state.

Deposits anchored via in_context_of to this instance form the run's history. Reservoirs and similar views are Dataview projections over deposits, not stored fields. State that is genuinely scalar to the run lives in the deposit stream; structured content the run itself carries (e.g. a reflection's observations) lives in this entity's body, governed by the schema of its process-parent (see below).

## Dual-parent convention (replaces the former `of_process` relationship)

A run does NOT name its process via a dedicated relationship. Instead it declares BOTH parents in its is_a (wikilink brackets omitted here to avoid polluting the graph; use real brackets in actual entities):

    relationships__is_a:
      - Process Instance v1.0.0    # it IS a run  -> gains status / started_at / ended_at
      - <the process it ran>       # it is a run OF that process -> gains the process's relationship vocabulary

This fusion is load-bearing. By being is_a the process, a run inherits that process's relationship keys (e.g. a Loop run inherits in_process / in_loop and can therefore point at its PARENT run — nested instances, which a one-way of_process could never express) and that process's property/body schema (so the run may legally carry the structured fields the process defines). The former of_process relationship was retired on 2026-06-23 and is not part of this schema.

## Querying runs

Because a run is is_a its process, a plain is_a query for the process ALSO matches the process definition itself. Always disambiguate with identity__type:

  - runs of process X    ==  is_a contains X  AND  identity__type = "particular"
  - the definition of X  ==  identity__type = "universal"
"""

[Metadata.Properties]
status = { type = "text", description = "Lifecycle state of this run", values = ["open", "closed"], optional = false }
started_at = { type = "date", description = "Date this run was initiated", optional = false }
ended_at = { type = "date", description = "Date this run was closed; set when status transitions to closed", optional = true }
current_phase = { type = "text", description = "Free-text or wikilink-string the executor uses to mark progress within the run", optional = true }
```
