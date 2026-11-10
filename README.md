# khora

khora is a unification layer for prototyping, developing, scaling and integrating systems. Everything is data, the system grows out instead of being built, scale compounds value instead of adding cost, and meaning is stored and manipulated at its core.

There is no implementation code yet. The design lives in [`docs/`](docs/):

- [`docs/architecture.canvas`](docs/architecture.canvas): the projected runtime architecture as an Obsidian canvas: components and the read, write, reactive and external flows between them, as the ADRs describe them. Derived from the ADRs, not a source of truth.
- [`docs/PRD.md`](docs/PRD.md): product requirements (draft).
- [`docs/adr/`](docs/adr/): architecture decision records (proposed).
  - [Query model and propagation](docs/adr/query-model-and-propagation.md) (ADR-0XX)
  - [Entity sources, time and schema](docs/adr/entity-sources-time-and-schema.md) (ADR-0XY)
  - [Scopes, khoras and rights](docs/adr/scopes-khoras-and-rights.md) (ADR-0XZ)
  - [Entity behavior and effects](docs/adr/entity-behavior-and-effects.md) (ADR-0YA)
- [`docs/systems/`](docs/systems/): sample domain system specs, written with the [specification template](docs/systems/template.md).
  - [Nutrition optimization](docs/systems/nutrition-optimization.md)
  - [Built-environment review](docs/systems/built-environment-review.md)
  - [Lifelong learning](docs/systems/lifelong-learning.md)
  - [Company brain](docs/systems/company-brain.md)
  - [Application development](docs/systems/application-development.md)
  - [Furniture workshop](docs/systems/furniture-workshop.md)
