---
identity__uuid: 25ebf0a4-72a2-4ae2-a062-8596a92a212f
identity__type: universal
identity__created_at: 2026-03-10
identity__tags: []
aliases:
  - Operate
relationships__is_a:
  - "[[Process(n) v1.0.0]]"
relationships__input:
  - "[[Vector]]"
  - "[[Constraint Space]]"
relationships__output:
  - "[[Reservoir]]"
  - Output
relationships__in_context_of:
  - "[[Looping v2]]"
---
```toml
Description = "Domain-aware execution guided by the Attractor and bounded by the Repulsor. Updates the Reservoir asynchronously to match impedance with flow."

Subprocesses = """
	1.  **Execute aligned to what Laydown produced:**
		*   *Plan/Tactics:* Move relentlessly through the steps. Reject the Repulsor. Do not deviate.
		*   *Waypoints:* Navigate toward the current waypoint. The route between waypoints is yours to choose — adapt freely within the Vector's corridor.
		*   *Hypothesis:* Run the probe. Stay open to what it reveals — the goal is contact with reality, not completion. If the probe surfaces something unexpected, following that signal *is* the work.
	2.  **Impedance Match (Fill the Reservoir):** Do not break flow to write detailed logs. "Drop" major friction points, unexpected findings, or spontaneous brilliant ideas into the `Reservoir` (a scratchpad). In exploratory work, unexpected findings are the *primary output*, not a side effect — treat them accordingly.
		*   **Capture Rhythm:** Rather than relying on noticing passively (unreliable under stress), carry a lightweight periodic prompt: after each meaningful action or at a regular interval, ask *"Anything to drop?"* — not analysis, just a sweep. The rhythm scales with domain: tighter in Complex (findings are the work), looser in Clear (execution is predictable).
	3.  **Circuit Breaker:** Continuously monitor the `Constraint Space`. The moment a constraint is triggered (e.g., time is up, or reservoir friction hits maximum), **stop work and continue to next step**. For Hypothesis Vectors, probe resolution (the question was answered, positively or negatively) is a natural close point even if no constraint was hit.
	"""
```
