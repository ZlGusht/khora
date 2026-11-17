---
identity__uuid: c2e9b0e0-3f4d-4c5a-8b1e-2f0a9d8c7b6a
identity__type: universal
identity__created_at: 30.10.2025
identity__tags: []
aliases:
  - BaseEntity
relationships__is_a:
  - "[[BaseEntity v1.0.0]]"
relationships__in_context_of:
  - "[[Khora]]"
---
```toml
Description = "A foundational structure defining the minimal required data for any entity within the system."

[Metadata]
# Used for systemic purposes and navigation

[Metadata.Identity]
uuid = { type="text", description="Unique identifier (GUID)" }
type = { type="text", description="System type of the entity (e.g. universal, template, etc)" }
created_at = { type="datetime", description="Timestamp indicating when the entity was created" }
tags = { type="list<text>", description="Used to enhance searching and organization capabilities without over imposing rigid structures", optional = true }

[Metadata.Relationships]
# Structured data defining basic connections to other entities, crucial for structuring, navigation, and emergence purposes.
is_a = { type="list<connection>", description="hierarchical inheritance" }
in_context_of = { type="list<connection>", description="used for creation and navigation of topological structures within the system", optional = true }
relates_to = { type="connection", description="a general-purpose lateral connection between entities; use when no more specific relationship key applies", optional = true }

[Metadata.Properties]
# Entity-specific properties that carry structured information, often dynamic or calculated (e.g., status, state, focus).
author = { type="text", description="Who wrote this entity. A human writes their own name (e.g. 'Jane Doe'). An AI agent writes 'agent:' followed by its model or agent name (e.g. 'agent:claude-opus-5-5'); the 'agent:' prefix is what marks an entity as agent-written, queryable with startswith(properties__author, 'agent:'). When a human dictates and an agent only transcribes, use the human's name.", optional=true }

[Metadata.Scratch]
  # Free-form accumulator for unstructured/temporal information - description fragments, agent notes, exploration. Not a property bag for runtime flags. Use `scratch__<key>` frontmatter keys. No schema enforced.
  
[Data]
# Properties that contain characteristic information and descriptions about the entity in different data formats, like rich text, images, files, etc. Examples include process instructions, notes, code, documents, etc. Used for semantic navigation and higher system functions. 
```
