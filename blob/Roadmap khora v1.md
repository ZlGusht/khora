---
identity__uuid: 977101b3-1016-4007-9db3-26687fbc2d05
identity__type: particular
identity__created_at: 2026-11-18
identity__tags: []
relationships__is_a:
  - "[[Process Instance v1.0.0]]"
  - "[[Roadmap Loop v1.0.0]]"
relationships__in_context_of:
  - "[[Product]]"
properties__status: "open"
properties__started_at: 2026-11-02
properties__current_phase: "Operate"
properties__author: agent:claude-opus-5-5
---
The roadmap for khora v1: stages 0 to 3 of `docs/implementation-plan.md`. Opened by [[Simeon Petkov]] on 2026-11-18, after the fact: the stages already ran as GitHub milestones before the loop kinds existed. started_at is the date of the first milestone.

## Loop Config

- **Horizon:** khora v1, stages 0 to 3.
- **Scope of Authority:** the milestones, their order, goals and gates. Not the design: that is in the ADRs.
- **Areas:** [[Product]].
- **Source:** the Stages section of `docs/implementation-plan.md` (draft of 13.11.2026). Its stage table is marked there as a suggestion.

## Vector

- **Attractor:** stage 3 done: a furniture project is linked to a company brain order, and an assembled project opens in Kroika.
- **Repulsor:** not set yet.

## Milestones

| Stage | Goal | Gate | Run |
| --- | --- | --- | --- |
| 0. Decide | Language, log store, test format; how people see and work with a khora | The stage 0 decisions are made and the records are accepted | [[Milestone Definition]] (closed) |
| 1. Kernel core | The kernel, built together; the generic entity view and the history view | An action through the API yields an outcome and effects. State is read at a frontier. The history view shows who did what. The contract tests pass. | [[Milestone Stage 1 kernel core]] |
| 2. Two systems in parallel | Furniture workshop slice 1; company brain slice; both interfaces | The furniture slice answers its query suite on real Kroika projects. The company brain slice passes the access tests on the team's own work. | not opened |
| 3. In parallel | Furniture workshop slice 2; nutrition, or application development if a fourth person joins; the remaining interfaces | A furniture project is linked to a company brain order. An assembled project opens in Kroika. | not opened |