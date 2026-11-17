---
name: khora-assemble-context
description: Assemble a compact, ranked context bundle of relevant vault entities for a downstream task — combines semantic search, structural query, and graph traversal. Use BEFORE invoking other skills or subagents that need awareness of relevant entities. Default hybrid recipe; process entities may declare their own.
---

# khora/assemble-context

Composition skill: orchestrates the retrieval atoms (search + query + frontmatter graph walks) into a single context bundle. Goal: give the downstream task enough awareness of relevant entities to act, without flooding it with the whole vault.

Think of it as an apprentice familiarizing themselves with a problem before doing it — look around, follow the relevant threads, build a working picture. The default recipe below is a starting point; adapt depth and breadth to the intent.

## Inputs

- `intent` (required) — what's the downstream task? what kind of awareness is needed? Ask if not provided.
- `seeds` (optional) — specific entity wikilinks to start from (besides whatever search/query surfaces)
- `recipe` (optional) — a recipe entity wikilink, or an inline spec, that overrides the default

## Default recipe

1. **Semantic axis.** Load and invoke `khora-search-entities` skill on `intent` with a generous limit (~10). Smart Connections returns block-level paths. Collapse to file-level for the bundle but remember the block paths — callers may want to drill in.

2. **Structural axis.** If `intent` suggests a structural filter (e.g. "Letter instances", "loop logs from this quarter", "everything is_a Process(n)"), compose a DQL and invoke `khora-query-entities`. Otherwise skip — don't fabricate a structural filter that doesn't follow from the intent.

3. **Graph axis.** Three sub-walks per seed entity (caller-provided + top hits from steps 1 & 2). Use `obsidian read` for outward frontmatter walks; `obsidian backlinks` for inbound.

   **3a. Outward `relationships__*` walk (excluding `is_a`).** Extract wikilinks under closed-vocabulary keys (per rule 4) and read each target one hop out. Walk **1–2 hops total** along this axis — beyond that, signal:noise collapses.

   **3b. `is_a` chain walk — always to root.** For each seed, follow `relationships__is_a` all the way to `[[BaseEntity]]`. The schema (allowed relationship keys, property types) is the **union of `[Metadata.Relationships]` and `[Metadata.Properties]` declarations across this chain** (rule 4). Cheap to walk — universals are small — and high-value for any downstream task that authors, validates, or queries by schema. Surface as `is_a_chain` on the entity entry (self → parents → … → `[[BaseEntity]]`). This walk is NOT capped at 1 hop; the chain length is whatever the ontology declares.

   **3c. Inbound backlinks.** Run `obsidian backlinks file=<seed>` **first hop only** — do NOT recursively backlink the backlinks (the bundle explodes). Load-bearing whenever the seed is used as an **anchor**: the wikilink relation is one-directional — only the depositor declares it, the anchor doesn't know about it. Backlinks are the only way to surface what's been pointed at a given entity. Without this sub-walk, intents that anchor on a named entity get only outward-declared neighbors and miss everything that's been "stuck on" it.

4. **Memory axis.** *(Skip this axis if the vault has no `Memory v1.0.0` universal — this starter bundle does not ship one.)* Memory entities (`is_a [[Memory v1.0.0]]`) are first-class — always check whether any are relevant to intent, even when the upstream axes didn't surface them.

   - **Semantic restriction.** From step-1 results, retain any entity whose `is_a` chain reaches `[[Memory]]` (alias-resolved). If the intent suggests a specific Memory type (user preferences → `type: user`; "how should I approach…" → `type: feedback`; project state → `type: project`; "where do we track…" → `type: reference`), prefer that type.
   - **Structural fallback.** If semantic surfaced none, run `khora-query-entities` for `is_a [[Memory v1.0.0]]`, filtered by `relates_to` / `in_context_of` overlap with seeds. Bound results to top 5–10 to avoid bundle bloat.

   Surface as a discrete `memories` category in the output bundle (separate from `entities`). Default top-5 memories. The point is that downstream callers can decide to weight memories without having to filter them out of a mixed list.

5. **Fuse.** Dedupe by file path (one entry per entity even if it appears via multiple axes). Score each entity by combining: semantic similarity (cosine, when present), structural-match (boolean), and inverse hop distance from a seed. Reciprocal Rank Fusion is one acceptable scheme; a weighted sum is another. Use judgment based on the intent. Memories are fused into the `memories` category independently of the `entities` fusion.

6. **Trim.** Relevance-thresholded, not count-capped. Include every entity and every memory whose fused score is above a relevance floor (default ~0.5 on the cosine-equivalent scale; per-recipe overridable). A runaway safety cap exists only as a guardrail — if more than ~50 entities or ~20 memories pass threshold, surface the count to the consumer and either tighten the threshold or return all with a flag. Don't truncate silently. The threshold is the mechanism; the cap is the alarm. `#TODO: revisit when the recipe gets more sub-axes — the trim rule is where bundle coherence will become a real design question.`

## Custom recipes

A process entity may declare a `ContextAssembly` field in its TOML body specifying its own retrieval pattern — which axes to use, hop depths per relationship key, weighting, axis-specific queries. If the caller passes a recipe (wikilink to a recipe entity, or an inline spec), follow it instead of the default.

Recipes themselves can be entities (`is_a [[Context Assembly Recipe]]`, future universal) so they're reusable across processes. `#TODO: create Context Assembly Recipe v1.0.0 universal once the first process needs to formally declare its recipe.`

## Output

Compact JSON bundle:

```json
{
  "intent": "<input>",
  "recipe": "default | <wikilink-or-inline-id>",
  "entities": [
    {
      "path": "<vault-path>",
      "block_paths": ["<block-level-path>", "..."],
      "sources": ["semantic", "structural", "graph-outward", "graph-is-a", "graph-backlink"],
      "score": 0.74,
      "hops_from_seed": 0,
      "is_a_chain": ["<self-wikilink>", "<parent>", "...", "[[BaseEntity v1.0.0]]"],
      "backlinks": ["<wikilink>", "..."]
    }
  ],
  "memories": [
    {
      "path": "<vault-path>",
      "type": "user | feedback | project | reference",
      "score": 0.81
    }
  ]
}
```

`block_paths` is the list of block-level addresses Smart Connections returned for that entity (may be multiple per file). `sources` records which axes surfaced the entity (one entity can appear via multiple) — the graph axis is split into `graph-outward` / `graph-is-a` / `graph-backlink` so consumers can tell schema-walks from anchor-deposits at a glance. `is_a_chain` enumerates the resolved classification chain to its versioned root (`[[BaseEntity v1.0.0]]` for current entities) — useful downstream for filtering, dispatching, and schema-aware authoring/validation. `backlinks` lists first-hop inbound links for the entity (anchor-pattern signal — what's been deposited against this entity). `memories` is a separate category for Memory entities — each entry includes its `type` so consumers can route by taxonomy without re-reading the file.

This skill does NOT fetch body content beyond frontmatter. The consumer (downstream skill or subagent) decides which entities to read in full via `obsidian read`.

## When NOT to use

If the caller knows exactly what they want — a specific DQL filter, a single semantic lookup, walking from one known entity — invoke the atoms directly. This skill is for **broad orientation**, not targeted retrieval.

## Out of scope (v1)

- Fetching body content beyond frontmatter
- Caching across invocations
- Re-ranking with an LLM
- Cross-recipe fusion (one recipe per invocation)
- Persisting bundles to the vault as entities `#TODO: revisit if assembled contexts become reusable artifacts`
