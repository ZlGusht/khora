---
identity__uuid: 915e777f-762b-44ed-8c34-bfb564dba4bd
identity__type: particular
identity__created_at: 2026-11-18
identity__tags: []
relationships__is_a:
  - "[[Decision v1.0.0]]"
relationships__in_context_of:
  - "[[Kit]]"
relationships__originates_in:
  - "[[Conversation 2026-11-18 Loop levels for development]]"
relationships__decided_by:
  - "[[Simeon Petkov]]"
properties__decided_on: 2026-11-18
properties__status: "active"
properties__kind: "design"
properties__quote: "specific relational keys are fine"
properties__author: agent:claude-opus-5-5
---
## Statement

The loop structure may add specific relationship keys (for example, from a digest to the run it digests) rather than reuse the generic relates_to.

## Rationale

This is consent to an ontology change; each key is still added by amending the schema of the kind that hosts it.