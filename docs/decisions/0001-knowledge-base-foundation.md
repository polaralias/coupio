# Decision 0001: Establish an evidence-first repository knowledge base

- Date: 2026-05-23
- Status: accepted

## Context

The repository is being prepared for eventual public presentation, but it was inherited in an under-documented and only partially verified state.

At the start of this decision:

- product intent was inferred more from code than from canonical docs
- there was no explicit reading order for future contributors or agents
- verification evidence and architectural understanding existed only in ad hoc notes
- root documentation still contained workstation-specific references unsuitable for a public repository

## Decision

The repository knowledge base will use an evidence-first structure with a small set of canonical artifacts:

- `README.md` for root reading order and current contract summary
- `docs/verification-matrix.md` for current proof state
- `docs/codebase-map.md` for architecture and implementation map
- `GLOSSARY.md` for domain language
- `docs/decisions/` for durable decisions
- `AGENTS.md` for repository operating guidance

Repository docs must distinguish clearly between:

- verified working behavior
- observed-in-code behavior
- untested assumptions
- blocked verification

Machine-local paths should not appear in public-facing docs unless the path itself is required as evidence.

## Consequences

### Positive

- Future agents and contributors have a stable reading order.
- Architectural understanding, evidence, and terminology are no longer trapped in chat history.
- Public-facing repo cleanup can proceed from documented truth rather than rediscovery.

### Costs

- Every meaningful behavior or support change now requires documentation alignment in the same slice.
- Evidence docs will need active maintenance as verification improves.

## Follow-on implications

- Product claims should remain conservative until device-level verification exists.
- Future refactors should update this knowledge base as they land.
- If product intent becomes clearer than the current inferred model, the glossary, README, and codebase map should be updated before broader implementation work depends on the old framing.
