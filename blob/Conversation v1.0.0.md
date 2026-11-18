---
identity__uuid: 722b327c-2543-4f30-b212-a93a1e5778b5
identity__type: universal
identity__created_at: 2026-11-18
identity__tags: []
aliases:
  - Conversation
relationships__is_a:
  - "[[BaseEntity v1.0.0]]"
relationships__in_context_of:
  - "[[Kit]]"
properties__author: agent:claude-opus-5-5
---
```toml
Description = """
One conversation: a call, a meeting, or a chat session with an agent, internal or external. The main entry point of knowledge into the graph; produced by [[Conversation Ingestion v1.0.0]].

A particular is_a [[Conversation v1.0.0]], named `Conversation YYYY-MM-DD <short topic>`. One recording or export that holds several sessions with different participants becomes several Conversations. The transcript never enters this repository, which is public: it stays in the team's transcripts folder outside it (KHORA_TRANSCRIPTS, by default a khora-transcripts folder beside the repository). The Conversation names the file; nothing from it is copied in except Decision quotes. A live session that was not saved has no transcript; its quotes come from the session itself.

What was decided and what must be done do NOT live in the Conversation body: Decisions are extracted into [[Decision v1.0.0]] entities that point back via originates_in; work items become GitHub issues. The body keeps the narrative: summary, what was discussed, open questions.
"""

[Metadata.Relationships]
participants = { type="list<connection>", description="Persons who took part." }
about = { type="list<connection>", description="Persons or other entities substantially discussed but not present.", optional=true }
follows = { type="connection", description="The previous Conversation in the same thread (e.g. the last conversation with the same person).", optional=true }
transcript = { type="text", description="The transcript's file name in the transcripts folder outside the repository. Read, never committed.", optional=true }
digest = { type="list<connection>", description="Human-written notes or summaries of this conversation that predate or accompany the entity.", optional=true }

[Metadata.Properties]
date = { type="date", description="When the conversation took place." }
medium = { type="text", description="How it was held.", values=["call", "meeting", "chat"], optional=true }
kind = { type="text", description="internal = team only; external = anyone else present.", values=["internal", "external"], optional=true }

[Data]
Summary = { type="text", description="3 to 6 lines: why the conversation happened and what came out of it." }
Discussion = { type="text", description="Compact account by topic, with attributions only where the source is reliable.", optional=true }
OpenQuestions = { type="text", description="Questions raised and left unresolved.", optional=true }
```