# Decision 0002: Add a repository query surface for agent-led development

- Date: 2026-05-23
- Status: accepted

## Context

The repository now has an evidence-first knowledge base, but the remaining uncertainty is still scattered across broad narrative docs.

That makes future agent work slower and less reliable because agents must rediscover:

- which product questions are already answered by repository evidence
- which contradictions still need user judgment
- which open questions are important enough to block support or workflow claims

## Decision

The repository will maintain a dedicated `docs/query-surface.md` document.

That document will capture:

- repository-answered truths that should not be repeatedly re-asked
- active contradictions that still need human judgment
- the current dense question batch to resolve next
- where each resolved answer should be written back into the canonical knowledge base

## Consequences

### Positive

- Future agents can rebuild the true decision surface faster.
- Repository-answerable questions are separated from user-judgment questions.
- Product-meaning ambiguity is easier to resolve without reopening the full repo review each time.

### Costs

- The query surface becomes another maintained knowledge artifact.
- Resolved questions must be removed or rewritten promptly or the document will become stale.

## Follow-on implications

- `AGENTS.md` and `README.md` should include the query surface in the reading order for substantive work.
- The query surface should stay narrow: only active ambiguity, not general design ideas.
