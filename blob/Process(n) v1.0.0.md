---
identity__uuid: d64a821b-5f28-40c9-a9a4-5861f9157056
identity__type: universal
identity__created_at: 2025-12-31
identity__tags: []
aliases:
  - Process(n)
relationships__is_a:
  - "[[BaseEntity v1.0.0]]"
relationships__in_context_of:
  - "[[Khora]]"
---
```toml
Description = """

A foundational structure defining a process. 

### For agents
An agent equipped with atomic [[skills]] (read, create, property:set, validate, query, search, run) is firmware. A process entity is a program/disk. This entity is one disk. Future disks: digest process, grooming process, weekly-review process. 

Generated particulars from processes are [[Process Instance v1.0.0]]

Future generic skill: execute-process — knows how to read a process entity and run its steps.
"""

[Metadata.Relationships]
# The state variables that allow for interfacing and structuring of input and output. Could be an entity from the system or an unstructured placeholder
in_process = { type="connection", description="Names this entity as a subprocess of another Process. The parent Process discovers its named subprocesses via inbound in_process backlinks; subprocesses order themselves under the parent via properties.order.", optional=true }

[Metadata.Relationships.state]
input = { type="list", optional=true }
output = { type="list", optional=true }

[Metadata.Properties]
order = { type="number", description="Numeric ordering for named subprocesses under the same parent Process. Lower runs first. Combined with declared (anonymous) Subprocesses by the Execute Process v1.0.0 disc-reader.", optional=true }

[Data]
Description = { type = "text", description="A contextual description for the process that aims to assist its correct, intended usage", optional=true }
Subprocesses = { type = "list", description="Suprocesses or steps that compose the process. Allows for recursive definitions", optional=true }
ContextAssembly = { type = "text", description="A retrieval recipe (JSON or prose) passed to khora-assemble-context before the process executes. Describes which axes to use, queries, hop depth, and trim. When absent the executor uses its own judgment.", optional=true }
```

<!-- khora:auto-related start -->
## Related
- [[Process Instance v1.0.0]]
- [[skills]]
<!-- khora:auto-related end -->
