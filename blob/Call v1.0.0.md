---
identity__uuid: e896b353-6cb9-4fca-bd2b-c64d3c4b6909
identity__type: universal
identity__created_at: 2026-11-16
identity__tags: []
aliases:
  - Call
relationships__is_a:
  - "[[BaseEntity v1.0.0]]"
relationships__in_context_of:
  - "[[Kit]]"
properties__author: agent:claude-opus-5-5
---
```toml
Description = """
One conversation: a call, meeting or recorded session, internal or external. The main entry point of knowledge into the graph; produced by [[Call Ingestion v1.0.0]].

A particular is_a [[Call v1.0.0]], named `Call YYYY-MM-DD <short topic>`. One recording that holds several sessions with different participants becomes several Calls. The transcript never enters this repository, which is public: it stays in the team's transcripts folder outside it (KHORA_TRANSCRIPTS, by default a khora-transcripts folder beside the repository). The Call names the file; nothing from it is copied in except Decision quotes.

What was decided and what must be done do NOT live in the Call body: they are extracted into [[Decision v1.0.0]] and [[Task v1.0.0]] entities that point back via originates_in. The body keeps the narrative: summary, what was discussed, open questions.
"""

[Metadata.Relationships]
participants = { type="list<connection>", description="Persons who took part." }
about = { type="list<connection>", description="Organizations, Persons or other entities substantially discussed but not present.", optional=true }
follows = { type="connection", description="The previous Call in the same thread (e.g. the last call with the same person).", optional=true }
transcript = { type="text", description="The transcript's file name in the transcripts folder outside the repository. Read, never committed.", optional=true }
digest = { type="list<connection>", description="Human-written notes or summaries of this call that predate or accompany the entity.", optional=true }

[Metadata.Properties]
date = { type="date", description="When the call took place." }
kind = { type="text", description="internal = team only; external = anyone else present.", values=["internal", "external"], optional=true }

[Data]
Summary = { type="text", description="3 to 6 lines: why the call happened and what came out of it." }
Discussion = { type="text", description="Compact account by topic, with attributions only where the source is reliable.", optional=true }
OpenQuestions = { type="text", description="Questions raised and left unresolved.", optional=true }
```
