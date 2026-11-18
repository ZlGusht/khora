---
identity__uuid: 1aae4688-e18b-4e1e-88f0-5746cf659bae
identity__type: universal
identity__created_at: 2026-11-18
identity__tags: []
aliases:
  - Digest
relationships__is_a:
  - "[[Letter v1.0.0]]"
relationships__in_context_of:
  - "[[Kit]]"
properties__author: agent:claude-opus-5-5
---
A **Digest** is what one loop run passes up to its parent run when it closes: a compacted account of the run, written once, at its Process(v).

A particular is_a [[Digest v1.0.0]], named `Digest <run name>`, digest_of the run, and in_context_of the parent run. A Digest of a [[Roadmap Loop v1.0.0]] run has no parent run yet; it is in_context_of the roadmap's Areas.

```toml
Description = """
One Digest per run. It carries the run's outcome, findings, frictions, ideas and considerations, so that the parent's Observe reads what matters without reading every Letter. The run's own Letters stay, as provenance.
A Digest is a Letter: properties__type is digest, with properties__priority and properties__resolved. It starts unresolved. The parent run's Process(v) resolves it once it has acted on it, or carries it into its own Digest.
"""

[Metadata.Relationships]
digest_of = { type = "connection", description = "The loop run this Digest digests." }

[Data]
Outcome = { type = "text", description = "What the run achieved, and what it did not." }
Findings = { type = "text", description = "What was learned about the work or the system.", optional = true }
Frictions = { type = "text", description = "What slowed the run or got in its way.", optional = true }
Ideas = { type = "text", description = "Ideas worth keeping, not acted on.", optional = true }
Considerations = { type = "text", description = "What the parent should weigh: risks, open points, choices it now faces.", optional = true }
```