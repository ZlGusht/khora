---
identity__uuid: 4430a916-2561-4231-a253-0cd853953203
identity__type: universal
identity__created_at: 2026-03-09
identity__tags: []
aliases:
  - Loop
relationships__is_a:
  - "[[Process(n) v1.0.0]]"
relationships__input:
  - Environment
relationships__output:
  - "[[Reservoir]]"
relationships__in_context_of:
  - "[[Looping v2]]"
---
**Represents:** The fundamental, adaptive unit for action, sense-making, and progress in complex environments.
**Usages:**
- Navigate uncertainty, pioneer ventures, and explore high-ambiguity domains.
- Establish a "Safety Net" that enables rapid pivoting without losing momentum.
- Break down complex, multi-layered objectives into executable, context-aware cycles.
**Motivation:** Move from rigid, top-down scripting (which creates cognitive tax and flow dissociation) to a fluid, capacity-driven system. Align action with the natural domain of the problem (Cynefin) and harvest insights organically through a core "Reservoir."

## Core Concepts
-   **The Dual Vector**: It defines the direction to move in and the method to move in that direction. More concrete (smaller) loops would call for more concrete actions and more abstract (bigger) would call for more abstract ones. Execution is guided by a unified Dual Vector (Direction + Anti-Direction). Choose from the following:
	-   **Plan/Tactics**: when focusing on performance and action environment is clear. 
	-   **Waypoints and resource allocation**: when facilitating divergent work
	-   **Hypotheses**: used for exploration
*   **The Reservoir:** A central, living container that holds the loop's state, tracking data, friction, and unexpected insights collected during operation. It is divorced from the loop execution and can be fed by external or internal events. The "substance" in the reservoir dictates the physical state of the loop.
	*   **Viscous / High-Friction:** Full of errors, unanswered dependencies, and confusion. *Effect:* Restricts the "Budget" constraint. Forces an early close, or triggers a vertical scale down (spawning a micro-loop to deal with the friction). #TODO Define friction better.
	*   **Fluid / Laminar:** Smooth execution, ticking off waypoints rapidly, high flow. *Effect:* Suppresses the "Time Budget" constraint, allowing the loop to stretch longer and capitalize on flow-state.
	*   **Saturated / Dense:** Massive amounts of new data or anomalies gathered (typical in Pioneer/Complex work). *Effect:* Triggers a "Heavy Metabolism" process. You cannot proceed until this data is digested into new mental models.
*   **Capacity-Driven Scaling:** Loops have a characteristic scale defined by their *Reservoir Capacity* and *Scope of Authority*. A loop closes when its container is "full" (based on friction, budget, or completion) and does not overstep its authority. 
	* High authority and low capacity calls for free exploration and cost-effective iteration
	* Low authority and high capacity calls for optimized execution
	* High authority and high capacity calls for high-level vision setting and abstract reasoning
	* Low authority and low capacity calls for specialized exploration and cost-effective iteration
*   **Ceremony Scaling:** The ceremony level of a loop — how much structure each subprocess carries — is derived, not chosen. Capacity and Scope set the *starting* ceremony level: narrow scope and small capacity produce lightweight subprocesses (a Laydown might be one sentence, an Observe a momentary pause), while wide scope and large capacity earn full ceremony. The Reservoir state adjusts ceremony *during* execution: if a lightweight loop's Reservoir goes Viscous, the next cycle should increase rigor; if a heavy loop stays Fluid, ceremony can relax. All four subprocesses are always present — the skeleton survives at every scale — but their weight adapts.
*    **The Loop Config:** Stores the scale and domain distribution for the loop to use as a reference.

```toml
Description = """
Looping v2 is a non-linear, metabolic process designed to navigate uncertainty while preserving flow. 
It replaces rigid task-completion triggers with 'Reservoir Capacity' and 'Dual Vector' alignment. 
It consists of a Meta-Shell (Inception) and a cyclical execution phase (Lay Down, Operate, Observe, Process).
It actively mitigates the 'Optimization Trap' by scaling the cognitive depth of processing 
based on the loop's characteristic Horizon and Authority.
Created by "[[Looping v2 UPSF]]"."""

Subprocesses = [
	"[[Inception v1.0.0]]",
	"[[Laydown v2.0.0]]",
	"[[Operate v2.0.0]]",
	"[[Observe v2.0.0]]",
	"[[Process(v) v2.0.0]]"
]
```

<!-- khora:auto-related start -->
## Related
- [[Inception v1.0.0]]
- [[Laydown v2.0.0]]
- [[Looping v2 UPSF]]
- [[Observe v2.0.0]]
- [[Operate v2.0.0]]
- [[Process(v) v2.0.0]]
<!-- khora:auto-related end -->
