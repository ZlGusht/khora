---
identity__uuid: 13b9d7e0-9ee1-406e-a76b-fe15b397f461
identity__type: particular
identity__created_at: 2026-11-18
identity__tags: []
relationships__is_a:
  - "[[Letter v1.0.0]]"
relationships__in_context_of:
  - "[[Kit]]"
  - "[[Call Ingestion 2026-11-18]]"
properties__type: insight
properties__priority: 3
properties__resolved: false
properties__author: agent:claude-opus-5-5
---
## Context

[[Call Ingestion v1.0.0]] assumes a transcript file in the transcripts folder. On 2026-11-18 the source was the live Claude Code conversation, which the participant chose not to save.

## Content

The process should say how to ingest a conversation that has no file: the Call leaves `transcript` empty and quotes come from the conversation. As it stands, a later reader cannot check the quotes against any source.