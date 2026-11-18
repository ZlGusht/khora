---
identity__uuid: ac7c9c53-1bbf-4388-b92a-ea428547341f
identity__type: particular
identity__created_at: 2026-11-18
identity__tags: []
relationships__is_a:
  - "[[Process Instance v1.0.0]]"
  - "[[Milestone Loop v1.0.0]]"
relationships__in_process:
  - "[[Roadmap khora v1]]"
relationships__in_context_of:
  - "[[Product]]"
properties__status: "open"
properties__started_at: 2026-11-16
properties__current_phase: "Operate"
properties__milestone: 2
properties__author: agent:claude-opus-5-5
---
Stage 1 of [[Roadmap khora v1]], reflected by the GitHub Milestone "Stage 1: kernel core". Opened as a run by [[Simeon Petkov]] on 2026-11-18; the milestone started on 2026-11-16.

## Loop Config

- **Goal:** the kernel core from the implementation plan, and the designer's generic entity view and history view.
- **Gate:** an action through the API yields an outcome and effects. State is read at a frontier. The history view shows who did what. The contract tests pass.

## Vector

- **Attractor:** the gate.
- **Repulsor:** not set yet.

## Issues taken in

| Issue | Plan ID | Title |
| --- | --- | --- |
| #18 | K1 | Event types, ids and clock |
| #19 | K2 | Log store prototype on the candidates |
| #20 | K3 | Append path: action, then outcome and effects together |
| #21 | K4 | Surface contract, declaration and context as types |
| #22 | K5 | Contract test suite |
| #23 | K6 | State datastore materialized from effects |
| #24 | K7 | Frontiers and past reads |
| #25 | K8 | Query as a data structure, with the basic operators |
| #26 | K9 | Conditional writes |
| #27 | K13 | Definitions in their own files |
| #28 | B1 | Base system: history view of the log |
| #29 | B2 | Base system: generic entity view |

Not yet issues: K10 (reads as actions), K11 (authentication at the API boundary), K12 (API for people and outside agents). #11 (user experience initiation, designer work) has no milestone yet.