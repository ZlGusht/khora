---
identity__uuid: 8d773186-7820-4342-8b8c-f14590f4db88
identity__type: particular
identity__created_at: 2026-11-18
identity__tags: []
relationships__is_a:
  - "[[Conversation v1.0.0]]"
relationships__in_context_of:
  - "[[Kit]]"
relationships__participants:
  - "[[Simeon Petkov]]"
properties__date: 2026-11-18
properties__medium: "chat"
properties__kind: "internal"
properties__author: agent:claude-opus-5-5
---
## Summary

A working session between [[Simeon Petkov]] and a Claude Code agent on how work on the project is structured. It started as a request for workflow rules for agents, and became a design of loop levels for development: Roadmap, Milestone and Issue loops, how lower loops report up, and how the blob and GitHub relate. The source is the conversation itself; it was not saved as a transcript, by choice.

## Discussion

**Starting point.** Simeon asked for rules that make agents follow a set workflow, and for a process inheriting Loop v2.0.0 specialised for development. The agent drafted a single Development Loop and rules; Simeon stopped it as too fast and asked for a discussion first. The drafts were discarded.

**Levels.** Long term, milestone, and issue planning and execution, close to standard open-source practice. One loop at the issue level; large issues spawn child runs.

**Reporting up.** Lower loops compact frictions, results, findings, considerations and ideas into one digest per run for the parent's Observe. Raw Letters stay for provenance. Learnings and memories stay with the agent harness.

**Blob and GitHub.** Roadmaps and milestones live in the blob; issues live in GitHub and seed issue loops. The blob is the source, GitHub its reflection, synced both ways when a run opens, closes and merges. This replaces kit decision 9.

**Other.** Documents are wired to kit entities. Cadence loops stay separate for now. Written rules first, CI checks later. Only team members open roadmaps and milestones.

**Conversation, not call.** This session was a chat, not a call. The graph now records conversations: Conversation replaces Call, with a medium property.

**Designing the loops.** The three kinds were designed against what the repository already holds: the implementation plan's stages and the GitHub milestones and issues. Stage 0 is recorded as a closed Milestone run. The plan's Stages section becomes a view of the roadmap. Designer work is issues. An Issue run usually ends in one pull request. What a closed roadmap's Digest resolves into is left open. Simeon approved the design with "go ahead".

## Decisions

- [[Everyone works on the project inside loop runs]]
- [[Development runs in Roadmap, Milestone and Issue loops]]
- [[Agents compact their work to a minimal, complete record]]
- [[The blob stays navigable through meaningful entities and relations]]
- [[Lower loops feed findings up to the parent loop's Observe]]
- [[Each loop run writes one digest]]
- [[Raw Letters stay for provenance]]
- [[Roadmaps and milestones live in loops, issues in GitHub]]
- [[The blob is the source and GitHub its reflection, synced both ways]]
- [[Sync runs when a run opens, closes and merges]]
- [[Loop entities use specific relationship keys]]
- [[Learnings stay in the agent harness memory]]
- [[Only team members open Roadmap and Milestone runs]]
- [[The roadmap has an artifact in the repository]]
- [[Documents are wired to kit entities]]
- [[Cadence loops stay separate from development loops for now]]
- [[Workflow rules are written first, CI checks later]]
- [[Conversations replace Calls in the graph]]
- [[Stage 0 is recorded as a closed Milestone run]]
- [[The implementation plan's stages are a view of the roadmap]]
- [[Designer work is tracked as issues]]
- [[An Issue run usually ends in one pull request]]
- [[Digests of closed roadmap runs resolve into a level not yet defined]]
- [[The loops are built as designed on 2026-11-18]]

## Open questions

- [[What form does the roadmap artifact take]]
- [[What are the formats of loop runs and digests]]
- [[Which CI checks enforce the workflow]]