# Decision 0004: Add a final product state surface to the knowledge base

- Date: 2026-05-23
- Status: accepted

## Context

The repository now has:

- a current contract summary
- an evidence surface
- an implementation map
- a glossary
- a decision history
- a reduced question surface

What it still lacked was a dedicated desired-state artifact describing what the finished product and repository should look like.

Without that separation, future contributors and agents would tend to blur:

- current observed truth
- current documented contract
- and the intended end state

## Decision

The repository will maintain `docs/final-product-state.md` as the canonical desired-state surface.

That document will describe:

- the intended finished product
- the repository qualities expected at the final state
- the verification bar required before the final state can be claimed
- explicit out-of-scope areas that should not quietly expand

## Consequences

### Positive

- Future agents can reason separately about current truth and target end state.
- The documentation harness now covers both proof and destination.
- Scope expansion is easier to resist because the intended finished shape is explicit.

### Costs

- The final-state surface must be kept aligned with durable decisions.
- Future changes to scope or quality bar must update both current-contract and desired-state docs when relevant.

## Follow-on implications

- `README.md` and `AGENTS.md` should include the final-state surface in the reading order.
- Evidence docs should stay conservative even when the final-state surface is ambitious.
