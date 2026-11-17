---
identity__uuid: bb8537f1-ff6f-4d95-a830-c4b70abf5cfb
identity__type: universal
identity__created_at: 2026-01-19
identity__tags: []
aliases:
  - Letter
relationships__is_a:
  - "[[BaseEntity v1.0.0]]"
---
```toml
Description = "This is something that needs attention either now or at some point. An item in the inbox that is going to serve for later processing. Used for collection of fleeting thoughts, tasks, ideas, etc."

[Metadata.Relationships]
relates_to = { type="list<connection>", description="Undirected adjacency to other entities — semantic relation not captured by hierarchical (is_a) or contextual (in_context_of) keys.", optional=true }
belongs_to = { type="list<connection>", description="Hierarchical containment within a project, area, layer, or comparable structural parent.", optional=true }

[Metadata.Properties]
Priority = { type="number", min=1, max=5 }
Due = { type="date", optional=true}
Resolved = { type="boolean", default=false }
Type = { type="text", description="How is the letter to be interpreted", examples=["task", "epic", "thought", "idea"]}

[Data]
Context = { type="text", description = "An explanation of what has led up to the ticket and the surrounding circumstances that it might be connected to."}
Content = { type="text" }
```
