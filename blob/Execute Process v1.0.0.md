---
identity__uuid: 9f46f8e3-c2d2-4535-b8fb-b6bc7a6aab1c
identity__type: universal
identity__created_at: 2026-05-12
identity__tags: []
aliases:
  - Execute Process
relationships__is_a:
  - "[[Process(n) v1.0.0]]"
relationships__in_context_of:
  - "[[Khora]]"
---
```toml
Description = """
This is the disc-reader's missing capability, expressed as a process itself. Any agent capable of reading and following process definitions can execute any other process by following these steps. Recursion bottoms out when a target process has no Subprocesses or its Subprocesses are pure prose with no further wikilinks to follow.
"""

Subprocesses = [
  "Step 1: Read the target process entity in full via the obsidian CLI.",
  "Step 2: If no Process Instance was provided, author one — is_a BOTH [[Process Instance v1.0.0]] AND the target process (dual-parent convention, which replaced the retired of_process relationship on 2026-06-23), with status = open and started_at = today. Otherwise read the provided instance.",
  "Step 3: If the target declares a ContextAssembly recipe in its body, invoke khora-assemble-context with that recipe. Otherwise gather context using your own judgment — read the target's referenced entities, query for related ones if needed.",
  "Step 4: Compute the total subprocess set for the target as the union of: (a) items in the target's Subprocesses field, when present (anonymous subprocesses, ordered by list position); and (b) entities whose relationships__in_process points at the target (named subprocesses, ordered by their properties__order ascending). Walk this combined list in order. For each item: (i) if it is or resolves to a Process entity, recursively follow Execute Process v1.0.0 against it, passing the same Process Instance from step 2 for state continuity; (ii) if it is prose, interpret it with the assembled context and perform the work it describes; (iii) After the step completes, deposit a state Letter: properties__type = state, properties__priority (1 to 5) and properties__resolved (true or false), which Letter v1.0.0 requires, relationships__in_context_of pointing at the Process Instance from step 2, content describing what was completed and any concrete outputs.",
  "Step 5: When all subprocesses complete (or the run hits a natural stopping point — termination predicate met, user signal, error blocking continuation), update the Process Instance: status = closed, ended_at = today.",
  "Step 6: Run khora-validate-entity against every entity created or modified during the run. Surface violations.",
]
```

<!-- khora:auto-related start -->
## Related
- [[Process Instance v1.0.0]]
<!-- khora:auto-related end -->
