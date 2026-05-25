# Decision 0005: Anchor cooldown windows to confirmation time

- Date: 2026-05-24
- Status: accepted

## Context

The repository contract and UI copy describe cooldown-based coupons as resting after the pending window is finalized.

During the publish-readiness pass, the implementation was found to anchor `DAILY`, `WEEKLY`, and `MONTHLY` lock windows to the original share request time instead.

That created drift between:

- user-facing behavior descriptions
- glossary language
- repository expectations
- and the actual lifecycle implementation

## Decision

Cooldown-based reuse windows start when a pending coupon is confirmed.

That rule applies whether confirmation happens:

- manually through the admin flow
- or later through delayed reconciliation/finalization

The original share request time does not anchor the cooldown window.

## Consequences

### Positive

- Lifecycle timing now matches the documented product story.
- Unit tests can state the cooldown rule unambiguously.
- Future agents do not need to reinterpret whether the pending window consumes part of a cooldown.

### Costs

- Lifecycle and repository tests must keep this anchor explicit.
- Any future UI copy or configurability work must preserve this timing rule unless a later decision changes it.

## Follow-on implications

- `GLOSSARY.md` should define confirmation as the cooldown anchor for cooldown-based policies.
- `docs/query-surface.md` should treat this as a resolved repository truth.
- `docs/verification-matrix.md` should mention the confirmation-anchored coverage explicitly.
