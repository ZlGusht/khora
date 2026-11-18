---
identity__uuid: 54890739-0ffc-448e-ae97-12d5e1d9091f
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
properties__kind: "rule"
properties__quote: "calls are replaced with conversations in the graph"
properties__author: agent:claude-opus-5-5
---
## Statement

The graph records conversations, not calls. Conversation v1.0.0 replaces Call v1.0.0 for calls, meetings and chat sessions alike, and its `medium` property says which. Call Ingestion becomes Conversation Ingestion.

## Rationale

The session this came from was a chat with an agent, not a call. Alternatives offered: keep Call as a kind of Conversation, or retype only that session's entity. Both declined.