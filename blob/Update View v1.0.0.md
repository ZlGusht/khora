---
identity__uuid: fdd0d849-1ccc-4e64-91db-71d7eb2ef05f
identity__type: universal
identity__created_at: 2026-11-17
identity__tags: []
aliases:
  - Update View
relationships__is_a:
  - "[[Process(n) v1.0.0]]"
relationships__in_context_of:
  - "[[Kit]]"
properties__author: agent:claude-opus-5-5
---
```toml
Description = """
Writes a document in docs/ from accepted Decisions, and nothing else. This is how the kit keeps documents free of sentences nobody decided.

Trace markers tie a document to the graph. A sentence that decides something ends with `<!-- d:<decision uuid> -->`. A row in an open-questions table ends with `<!-- q:<question uuid> -->`. Markers are HTML comments, so they do not show on GitHub. The stale report reads them.

The document's own conventions win on form: an ADR keeps the ADR shape and the writing rules in CLAUDE.md. Decisions decide the content.
"""

Subprocesses = [
  "Step 1: Read the document and the Areas in its `areas` frontmatter.",
  "Step 2: Query the active Decisions in_context_of those Areas. Find the ones the document does not trace yet, and the traced ones that are now superseded or reversed.",
  "Step 3: For each untraced Decision, add or change only the sentences that state it, ending each with its d: marker. For each superseded or reversed one, replace its sentences and marker with those of the Decision that supersedes it, or remove them.",
  "Step 4: Add nothing a Decision does not state. A sentence that only connects two others is allowed if it decides nothing; it carries no marker.",
  "Step 5: Open questions: one row per open Question in_context_of the Areas, with its lean and q: marker. Remove rows whose Question an active Decision answers.",
  "Step 6: Open a pull request. The reviewer checks each new sentence against the Decision its marker names.",
]
```
