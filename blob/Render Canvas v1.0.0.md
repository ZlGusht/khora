---
identity__uuid: 244d507d-7dbd-49f8-96e3-81df1baa574a
identity__type: universal
identity__created_at: 2026-11-16
identity__tags: []
aliases:
- Render Canvas
relationships__is_a:
- '[[Process(n) v1.0.0]]'
relationships__in_context_of:
- '[[Kit]]'
relationships__input:
- The graph, plus the canvas's own Spec card
relationships__output:
- A .canvas file in dashboards/
properties__author: agent:claude-opus-5-5
---
```toml
Description = """
How an agent produces or refreshes a canvas. The kit's dashboards are dashboards/Areas.canvas, dashboards/Loops.canvas and dashboards/Stale.canvas; canvases in canvas/ are drawn by people and never rendered: a view over the graph that humans look at and any agent can regenerate.

Every canvas carries its own Spec card (canvas node id `spec`, purple, top-left): the canvas's purpose, what it must show, how to gather context, what judgement to apply, layout, limits and triggers. The Spec card is the canvas's source of truth and is owned by the humans: they edit it directly on the canvas to change what the canvas shows. The renderer never modifies it. Print it with `bash .claude/scripts/khora-canvas spec dashboards/<Canvas>.canvas`.

Division of labour. The agent is the intelligence: it gathers context, decides what is salient, summarises, judges state, and writes every card. The layout engine (`bash .claude/scripts/khora-canvas render <layout.json> dashboards/<Canvas>.canvas`) is only geometry: the agent describes the canvas as nested blocks (stack, row, grid, tree, hub) and the engine places them to the right of the Spec card without overlaps. Agents never write canvas coordinates by hand. The layout grammar is documented at the top of `.claude/scripts/khora-canvas.py`. Generated cards have ids starting with `g-` and are replaced on every render; any other card or edge a human adds is kept.

Card types:
- Entity cards: first line a bold wikilink to the entity, then one italic line of metadata and at most two lines of gist. Every fact on a canvas comes from an entity it links to. Card id = entity name, so ids stay stable between renders.
- Reading cards: the agent's synthesis and judgement (state, change since the last render, what is stuck, what needs a human). Titled `Reading · YYYY-MM-DD`, orange, at most about eight lines. Never invent; say "unknown" or "not in the vault yet".
- Live cards: `{"live": "<view>", "input": {...}}` renders dashboards/views/<view>.js via dataviewjs, for anything that changes faster than re-renders: counts, phases, and lists such as decisions, open questions and calls. Live views and agent judgement are complementary: views keep lists current, agents curate, summarise and judge. Views: loop-card, open-loops, closed-loops, decisions, declared-state, type-count, type-links, calls, questions (inputs documented at the top of each file).

Colour grammar (same on every canvas): 1 red = overdue, blocked, friction, needs a human now; 2 orange = header and Reading cards; 3 yellow = live cards; 4 green = open or active loops; 5 cyan = people and organizations; 6 purple = Spec card, types and processes; no colour = ordinary entity cards.

Edges: draw an edge only between cards that sit next to each other, never across groups or rows; long edges are untraceable once rendered. Express relationships by placement (inside the group or hub side they belong to) and by inline wikilinks in card text instead.

Quality bar: show gaps (undeclared, stale, overdue, not yet ingested) rather than aiming for completeness; respect the list limits in the Spec and summarise the rest in one card; past roughly 70 cards, split the canvas or summarise.

New canvas: write its Spec card first (agree it with a team member), pass it as the layout's `spec` field on the first render, then follow the steps below.
"""

ContextAssembly = """
Defined by each canvas's Spec card. Common tools: `bash .claude/scripts/khora-canvas graph` dumps every entity's frontmatter as JSON in one call; khora-query-entities for filtered lists; obsidian read for bodies; khora-search-entities for themes. Also look at the current canvas (its previous Reading cards and entity cards) to state what changed since the last render.
"""

Subprocesses = [
  "Step 1: Read the canvas's Spec card (`khora-canvas spec dashboards/<Canvas>.canvas`).",
  "Step 2: Assemble context as the Spec says, and note what the current canvas shows.",
  "Step 3: Decide salience and write the Reading cards: what is going on, what changed since the last render, what is stuck, what needs a human decision.",
  "Step 4: Write the layout JSON to a temporary file. Header card: canvas name, one line of purpose, the colour legend, and `Rendered YYYY-MM-DD by <author>`.",
  "Step 5: Render: `bash .claude/scripts/khora-canvas render <layout.json> dashboards/<Canvas>.canvas`. Fix any skipped edges it reports.",
  "Step 6: Tell the human in two or three lines what changed on the canvas and what it now flags.",
]
```
