# Wiki

Directory where all Foodie documentation lives. This is the entry point — start here.

## Contents

### Requirements
1. [Glossary](./glossary.md) — ubiquitous language (define each term once)
2. [Actors](./actors/actors.md)
3. [User Journeys](./user-journeys/user-journeys.md)
4. [Use Cases](./use-cases/use-cases.md) — TOC; each use case may embed its own flow & state diagrams

### Design
1. [Architecture](./architecture/architecture.md) — C4 context + containers
2. [Tech Stack](./tech-stack.md)
3. [ERD](./domain/erd.md) — data model

### Decisions
1. [Architecture Decision Records](./decisions/) — the "why we chose X" log

## Templates

| Template | Use it for |
| --- | --- |
| [`use-cases/_use-case-template.md`](./use-cases/_use-case-template.md) | a new use-case spec |
| [`_templates/epic.md`](./_templates/epic.md) | an umbrella GitHub issue |
| [`_templates/issue.md`](./_templates/issue.md) | a sub-issue under an epic |
| [`_templates/task.md`](./_templates/task.md) | a single-PR slice of an issue |
| [`decisions/_template.md`](./decisions/_template.md) | an architecture decision record |

## Conventions

1. **One folder per use case**, at `use-cases/<area>/<use-case>/`, with the spec named after the folder in kebab-case. [`add-cart-item`](./use-cases/cart-management/add-cart-item/add-cart-item.md) is the reference — match it.
2. **Diagrams go inline as Mermaid** so they diff in pull requests. Binary exports live in the use case's `images/`.
3. **The spec is the source of truth.** When code and spec disagree, fix the spec in the same PR.
4. `implementation-plan.md` beside a spec is gitignored working notes. Anything worth keeping belongs in the spec.

## Project Management
- Status / ownership / sprints live in GitHub Projects.

## Notes
1. Structure is subject to change.
2. Specs written before this structure was agreed are queued for reshaping to the template, not for deletion.
