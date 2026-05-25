# Decision 0003: Document the current product contract for agent-led development

- Date: 2026-05-23
- Status: accepted

## Context

The initial evidence pass established the repository structure and implementation map, but several workflow questions still required explicit user judgment before agents could treat the docs as a finished working contract.

Those questions included:

- whether the app is only currently local-only and single-device or intended to remain so
- whether Android share is the real issuance workflow
- whether the one-hour pending window is fixed or a default
- whether `SINGLE_USE` prevents manual reissue
- whether expired coupons can be manually reissued

## Decision

The current documented product contract is:

- Coupio is local-only and single-device by product intent.
- Sharing through Android share is the intended issuance workflow, including sharing a coupon to a partner.
- The pending window currently defaults to one hour and may later become configurable.
- `SINGLE_USE` coupons may still be manually reissued by an admin after confirmation.
- Expired coupons may also be manually reissued by an admin.

## Consequences

### Positive

- Future agents can work from a stable product contract instead of inference alone.
- Current UI and lifecycle behavior can now be judged against documented intent.
- The repository no longer needs to treat these workflow questions as unresolved ambiguity.

### Costs

- If implementation changes later, the canonical docs must be updated in the same tranche.
- Some user-facing labels such as `Share to redeem` may still need refinement even though the workflow meaning is now fixed.

## Follow-on implications

- Lifecycle tests should eventually cover the documented reissue semantics for `SINGLE_USE` and expired coupons.
- If pending-window customization is implemented, the glossary, README, and verification surfaces should be updated together.
