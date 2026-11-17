---
identity__uuid: 49f16a3f-4b24-408c-884c-f34b5ef2700e
identity__type: universal
identity__created_at: 2026-03-10
identity__tags: []
aliases:
  - Laydown
relationships__is_a:
  - "[[Process(n) v1.0.0]]"
relationships__input:
  - "[[Loop Config]]"
relationships__output:
  - "[[Vector]]"
  - "[[Constraint Space]]"
relationships__in_context_of:
  - "[[Looping v2]]"
---
```toml
Description = "Sets the 'Field' for execution by defining a Dual Vector, a first move, and an OR-Gate constraint space."

Subprocesses = """
1.  **Emit the Dual `Vector` aligned to the `Loop Configuration`:**
	    *   *Attractor:* The waypoint or hypothesis to move toward. (e.g., "Draft business logic").
	    *   *Repulsor:* The explicit anti-goal/distraction to avoid. (e.g., "Do not touch the UI/CSS").
2.  **Commit to a First Move aligned to the Vector type.** Only commit to what is visible from where you stand. The Vector holds direction — the first move just gets you into motion.
	    *   *Plan/Tactics Vector* (Clear): First move + a lightweight sketch of remaining steps. The path is visible, so a sketch is honest — but it is a sketch, not a commitment.
	    *   *Waypoints Vector* (Complicated): First move only. The territory has structure, but the sequence reveals itself through the work. (e.g., "Engage the first waypoint" or "Start the analysis").
	    *   *Hypothesis Vector* (Complex): First probe only. Design the smallest experiment that would tell you something about the hypothesis. You are not planning — you are designing one contact with reality.
3.  **Arm the `Constraint Space` aligned to the `Loop Configuration`:** Define specific triggers based on your Loop Configuration. Here are some examples. *The loop will hard-close the instant ONE of these is met:*
	    *   **Budget (mandatory)**: Time or Energy limit (e.g., "90 minutes elapsed").
	    *   **Reservoir**: Friction limit (e.g., "Stuck for 15 minutes without progress").
	    *    **Completion**: Only if in a Complicated domain (e.g., "Function compiles").   
	    *   **Emotional/Cognitive Limit:** "Close if I feel absolute frustration/boredom for 15 minutes." (Protects mental energy).
		*   **Diminishing Returns (Yield Constraint):** "Close if the last 3 actions produced less than 10% progress." (Directly fights the Optimization Trap).
		*   **External Interrupt/Invalidation:** "Close immediately if a newly discovered fact invalidates the Vector." (Prevents executing a dead plan).
	* **Vertical Scaling (Recursive Depth & Ascension):** Scaling vertically happens when the `Reservoir` dictates that the current `Scope of Authority` is mismatched to the reality of the work.
	    *   **Scale-Down (The Drill):** If the Reservoir fills tightly with a single, highly specific impasse, spawn a Sub-Loop. It inherits a *narrower* Scope, a tiny Capacity, and a strict time Budget to dissolve the local friction. Result feeds back up.
	    *   **Scale-Up (The Meta-Loop):** If the Reservoir detects a "Scope Violation" (the problem spans outside the current loop's authority) or systemic diminishing returns, *freeze* the current loop. Spawn a Meta-Loop. It assumes a *systemic* Scope. Its explicit **Repulsor** is to avoid tactical execution; its **Attractor** is to abstract the problem, map the dependencies, and rewrite the Parent Vector. Result restructures everything below it.
"""
```
