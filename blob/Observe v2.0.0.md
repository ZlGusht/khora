---
identity__uuid: 5d8d0b69-c354-4109-9f10-84dbe2726f40
identity__type: universal
identity__created_at: 2026-03-10
identity__tags: []
aliases:
  - Observe
relationships__is_a:
  - "[[Process(n) v1.0.0]]"
relationships__input:
  - "[[Reservoir]]"
  - "[[Loop Config]]"
  - Environment
relationships__output:
  - "[[Reservoir]]"
relationships__in_context_of:
  - "[[Looping v2]]"
---
```toml
Description = "A state-collection phase where the output of the Operate phase and the environment is ingested. It focuses on 'Metric Sampling' and 'Reservoir Intake' to capture the qualitative and quantitative state of the loop without performing analysis, acting as the bridge between execution and synthesis."

Subprocesses = """
	1.  **Reservoir Intake:** Deposit post-factum observations for the characteristic scale into the reservoir in accordance to the `Loop Configuration`. 
	2.  **Metric Sampling:** Apply the metrics defined during Inception (if any)."""
```
