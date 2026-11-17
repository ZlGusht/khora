---
identity__uuid: c7529dcc-cfdd-42f8-a075-ec73aa24332c
identity__type: universal
identity__created_at: 2026-11-16
identity__tags: []
aliases:
  - Call Ingestion
relationships__is_a:
  - "[[Process(n) v1.0.0]]"
relationships__in_context_of:
  - "[[Kit]]"
relationships__input:
  - Transcript file and/or human-written call notes
relationships__output:
  - "[[Call v1.0.0]]"
  - "[[Decision v1.0.0]]"
  - "[[Question v1.0.0]]"
  - "[[Person v1.0.0]]"
properties__author: agent:claude-opus-5-5
---
```toml
Description = """
Turns a call transcript (and any human notes on it) into graph entities: Calls, Decisions, Questions, and new or updated Persons. Work items become proposed GitHub issues, not entities. This is the main way knowledge enters the vault, so its two duties are fidelity (nothing invented, uncertainty marked) and graph hygiene (no duplicates, no orphan entities, nothing extracted that nobody will act on).

Rules that hold throughout:
- Write entities in English regardless of the transcript's language; quote the original only when wording matters.
- Speaker labels in machine transcripts drift. Never attribute a statement without reading around it; when unsure, write it unattributed or mark it [?].
- Mark every inference or uncertain fact with [?] so a human can confirm it.
- A human-written digest of the same call outranks the agent's reading of the transcript; reconcile differences rather than silently picking one.
- Extract only what someone will act on or rely on later. Everything else stays narrative in the Call body.
- Transcripts, and exported conversations, are only read. They live outside the repository (KHORA_TRANSCRIPTS, by default a khora-transcripts folder beside it) and are never copied or committed. A Call names its transcript's file in `transcript`.
- Light mode, for older material the team chooses to summarise: one Call per meeting with Summary, Discussion and a "Decided then" list; no Decision entities or issues are extracted, and the run record says so. Thread links (`follows`) and participants are still set.
- Run under a Process Instance in_context_of the loop run that is ingesting (or [[Kit]] while the system is being built). Each Process Instance holds one batch of calls.
"""

ContextAssembly = """
Before extracting: (1) list every name of a person or organization appearing in the transcript and digest, and search the vault for each (exact filename, aliases, semantic search); (2) query active Decisions (is_a Decision v1.0.0, status = active) to detect ones the call supersedes; (3) list open GitHub issues (`gh issue list --state open`) to detect ones the call completes or changes; (4) find the most recent Call with overlapping participants, to set follows.
"""

Subprocesses = [
  "Step 0 — Work on a branch named `ingest/YYYY-MM-DD`.",
  "Step 1 — Read the sources: the transcript in full, and any human digest. Note the language, the speaker-label quality, and whether the recording holds several sessions.",
  "Step 2 — Split into Calls: one Call per session with a distinct participant set or purpose. Name each `Call YYYY-MM-DD <short topic>`.",
  "Step 3 — Resolve people: for each participant, and each person the call commits us to act on, reuse the existing Person (add a missing alias if they appeared under a new name) or create a new Person with role where known. Organizations are not entities in this vault: name them in plain text. Passing mentions stay plain text.",
  "Step 4 — Write each Call: participants, date, kind, transcript, digest, follows, about, and in_context_of the Areas it discussed. Body sections: Summary, Discussion (by topic, compact), Open questions. Link to Persons inline where they are named.",
  "Step 5 — Extract Decisions: explicit agreements, rules and choices only. Set decided_on, status = active, kind, decided_by, originates_in, and quote (the transcript line where it was decided, verbatim). This repository is public: a quote holds only the words of the decision itself, never names of people outside the team or anything personal. When the decision can't be quoted without that, paraphrase it and start with [paraphrased]. Set in_context_of to the Decision's Area. When one replaces an earlier active Decision, set supersedes on the new one and status = superseded (or reversed) on the old one. Body: Statement, then Rationale including disagreements. One-off choices already executed during the call can stay in the Call body instead.",
  "Step 6 — Extract Questions and work items. Open questions someone will need answered become Question entities (lean, originates_in, in_context_of their Area); a Decision that answers one sets answers. Work items are not entities: list each as a proposed issue in the pull request description (title, assignee, the Call and Decision it comes from). Do not create Task entities or issues. Note any open issue the call completes or changes.",
  "Step 7 — Update Persons: add durable facts learned about them to their body.",
  "Step 8 — Validate every created or modified entity with khora-validate.",
  "Step 9 — Report to the humans: what was created or changed, every [?] needing confirmation, and anything that did not fit the current types. Friction with this process itself is deposited as a Letter (type insight, with properties__priority (1 to 5) and properties__resolved (true or false), which Letter v1.0.0 requires) in_context_of [[Kit]] so the process improves.",
  "Step 10 — Re-render dashboards/Areas.canvas and dashboards/Loops.canvas via [[Render Canvas v1.0.0]].",
  "Step 11 — Open one pull request labelled `ingestion` with every created or changed entity, the proposed issues, and the [?] list. Merging it is acceptance; whoever merges creates the proposed issues.",
]
```
