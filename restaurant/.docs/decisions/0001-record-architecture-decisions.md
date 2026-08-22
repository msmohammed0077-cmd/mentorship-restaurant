# 0001 — Record architecture decisions

- **Status:** Accepted
- **Date:** 2026-07-10
- **Deciders:** TODO

## Context
We need a lightweight, durable way to capture *why* significant technical and product decisions were made, so future contributors don't re-litigate settled questions or lose the rationale.

## Decision
We will keep Architecture Decision Records as numbered Markdown files in `wiki/decisions/`, using the format described by Michael Nygard. Records are append-only: to reverse a decision, add a new ADR that supersedes the old one rather than editing history.

## Consequences
- Rationale lives next to the code and diffs in PRs.
- Small ongoing discipline: one short file per significant decision.
- The first candidate for a real ADR: whether **Cart** is its own epic/bounded context separate from Order.
