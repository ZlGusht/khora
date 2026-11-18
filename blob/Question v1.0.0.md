---
identity__uuid: b8f5bc30-d518-4d4c-aff1-bd5bb0be4e97
identity__type: universal
identity__created_at: 2026-11-17
identity__tags: []
aliases:
  - Question
relationships__is_a:
  - "[[BaseEntity v1.0.0]]"
relationships__in_context_of:
  - "[[Kit]]"
properties__author: agent:claude-opus-5-5
---
```toml
Description = """
An open question someone will need answered, with the team's current lean. It is answered when an active Decision sets `answers` to it; that is derived from the inbound link, never stored here.

Questions are written by Conversation Ingestion or during a loop, in_context_of their Area. A document lists an open Question as one row ending with its trace marker `<!-- q:<uuid> -->`.
"""

[Metadata.Relationships]
originates_in = { type="connection", description="Where it was raised: a Conversation or a loop run.", optional=true }

[Metadata.Properties]
lean = { type="text", description="The current lean, in one line. Empty when there is none yet.", optional=true }
```