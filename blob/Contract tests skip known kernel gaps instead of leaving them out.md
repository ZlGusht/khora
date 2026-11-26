---
identity__uuid: 1574de01-f4eb-4cec-a73f-83e7d67bf87d
identity__type: particular
identity__created_at: 2026-11-25
identity__tags: []
relationships__is_a:
  - "[[Decision v1.0.0]]"
relationships__in_context_of:
  - "[[Surfaces]]"
relationships__originates_in:
  - "[[Issue #22 Contract tests for surfaces and authorities]]"
relationships__decided_by:
  - "[[Hristiyan Ganchov]]"
properties__decided_on: 2026-11-25
properties__status: "reversed"
properties__kind: "design"
properties__author: agent:claude-opus-5-5
---
## Statement

Where the kernel of #36 breaks a contract rule today, the test is written, run and skipped with a reason naming the gap, instead of being left out. A known gap that no longer fails makes its test fail, so a fix is noticed and the gap is taken off the list.

## Rationale

Leaving the rule out would hide the gap; failing the suite would block every implementation for a kernel fault. Today this covers a surface's own emissions not being causally ordered, and a surface whose interests throw failing the submit (found in the review of #36).

Reversed before review: the gaps did their work (#36's fixes in 6845d75 turned five of them to "appears fixed"), and #36 now tests those fixes itself, so K5 need not carry them. `KnownGaps` was removed.