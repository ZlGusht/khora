---
identity__uuid: 169f71e3-e73a-4f7c-8c84-48c808a80432
identity__type: particular
identity__created_at: 2026-11-18
identity__tags: []
relationships__is_a:
  - "[[Process Instance v1.0.0]]"
  - "[[Conversation Ingestion v1.0.0]]"
relationships__in_context_of:
  - "[[Kit]]"
properties__status: open
properties__started_at: 2026-11-18
properties__current_phase: Step 10
properties__author: agent:claude-opus-5-5
---
One run of [[Conversation Ingestion v1.0.0]] over one source: the Claude Code conversation of 2026-11-18 on loop levels for development. The conversation was not saved as a transcript, by the participant's choice; quotes come from the conversation itself.

## Steps

0. Branch `ingest/2026-11-18`.
1. Source: one session, in English, with reliable speaker labels (one team member and the agent). No human digest.
2. One Conversation: [[Conversation 2026-11-18 Loop levels for development]].
3. No Persons existed. Created [[Simeon Petkov]], the only human participant. The agent is not a Person.
4. Wrote the Conversation (medium chat): internal, in_context_of [[Kit]], no transcript, no earlier Conversation to follow.
5. 18 Decisions, all active, decided_by [[Simeon Petkov]], each with its quote. No Decision entities existed to supersede; [[Roadmaps and milestones live in loops, issues in GitHub]] names kit decision 9 in its Rationale. Agent proposals that were not taken up are not Decisions.
6. 3 Questions. Six work items listed as proposed issues in the pull request. Open issues (#11, #18 to #29): the call completes or changes none.
7. [[Simeon Petkov]] holds only the facts known: team member, GitHub name.
8. All written entities pass khora-validate.
9. Reported to the participant.
10. Not done: dashboards not re-rendered. Obsidian is in Restricted Mode on this machine, so its plugins don't load.
11. Pull request #33, labelled `ingestion`.
12. Retyped Call to Conversation in the graph, per [[Conversations replace Calls in the graph]].
13. The conversation's second part: 6 more Decisions, on the loop design; one answers [[What are the formats of loop runs and digests]].

## Frictions

- **No transcript.** The process assumed a transcript file. Conversation Ingestion now says how to ingest a live session: `transcript` stays empty and quotes come from the session. Those quotes cannot later be checked against a source.
- **A run that stops early.** The process has no rule for a run that cannot finish its last steps. This one stays open at Step 10.
- **One Letter per step.** Execute Process deposits a state Letter after every step: twelve entities for this run. They were folded into this body, at the participant's request.