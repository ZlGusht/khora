---
identity__uuid: 05515fe2-6721-4985-8162-9f2629a56811
identity__type: particular
identity__created_at: 2026-11-24
identity__tags: []
relationships__is_a:
  - "[[Decision v1.0.0]]"
relationships__in_context_of:
  - "[[Product]]"
relationships__originates_in: "[[Milestone Stage 1 kernel core]]"
relationships__decided_by:
  - "[[Simeon Petkov]]"
relationships__about:
  - "[[Milestone Stage 1 kernel core]]"
properties__decided_on: 2026-11-24
properties__status: active
properties__kind: design
properties__quote: "k3 is really not needed, k7 would move forwar [...] maybe we can push k13"
properties__author: agent:claude-opus-5-5
---
## Statement

Stage 1 keeps only what its base systems use.

1. K3, the append path, is done by the event kernel (#36): an accepted action is appended as one event, all or none, and a rejected one appends nothing.
2. K7 leaves stage 1 for the plan's *Not staged* list. Reads at a frontier and past reads of old data are done by #36. Waiting or partial reads come with the first adapter; reads by recorded or occurred time and old kinds in past reads come when something needs them.
3. K13, definitions in their own files, moves to stage 2, pulled by the first kinds (F2, C2).
4. ADR-0XY's MVP changes the same way: decision 4 keeps reads at a frontier without waiting, and decision 5 keeps past reads of old data only.

## Rationale

The stage 1 rows were copied from each ADR's MVP list, not from what a stage 1 system uses. A read waits for origins that have not caught up, and other origins come with adapters, which the MVP already defers. Nothing in stage 1 reads by time, and there are no kinds yet. K13 has nothing to move out today: attributes are bare names, and the only definitions in code are the kernel's own. It becomes real with the first kinds, in stage 2, not with the later move to firmware.