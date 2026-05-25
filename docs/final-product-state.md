# Coupio Final Product State

## Purpose

This document describes the intended end goal for the repository and product.

It is a desired-state contract, not a proof surface.

Use it to answer:

- what the finished product should be
- what "done enough to present publicly" looks like
- which qualities must be true before the repository can be treated as a final product rather than an audited prototype

For current proof strength, read `docs/verification-matrix.md`.
For current implementation shape, read `docs/codebase-map.md`.

## Status of this document

- Desired end state, not current verification evidence.
- May describe target behavior that is not yet `verified working`.
- Must stay aligned with durable product decisions under `docs/decisions/`.

## Final product goal

Coupio should finish as a polished, local-first Android coupon vault for a single-device workflow with manual issuance through Android share.

The finished product should feel intentionally small and complete:

- one clear use case
- one coherent device model
- one understandable coupon lifecycle
- one trustworthy local data model

The goal is not to broaden the scope into a platform, marketplace, scanning system, or synced service.

## Target product contract

At the final product state, the repository should clearly support this contract:

1. An admin loads coupon media into the local vault through the app's bounded local import or capture flows.
2. The admin can add or edit lightweight metadata:
   - title
   - description
   - category
   - expiry date
   - reuse policy
3. A non-admin user can browse currently available coupons on the same device.
4. Sharing a coupon through Android share is the intended issuance workflow, such as sending it to a partner.
5. After share, the coupon enters a pending window.
6. The admin can confirm it, roll it back, or later reissue it according to the product rules.
7. Reuse behavior remains understandable across `SINGLE_USE`, `DAILY`, `WEEKLY`, `MONTHLY`, and `ALWAYS`.
8. Expired coupons remain visible and understandable, and admin override behavior is documented and intentional.

## Product qualities the final state must demonstrate

### 1. Lifecycle clarity

The final product should make the coupon lifecycle easy to understand in both code and UI:

- `AVAILABLE`
- `PENDING`
- `LOCKED`

Transitions should be documented, testable, and visible in the product copy.

### 2. Media workflow reliability

The final product should reliably support:

- the bounded media workflows the repository publicly claims in `README.md`
- Android share export

Specific picker, PDF, or camera interoperability should only be named in public-facing docs once the repository has direct evidence for that claimed support level.

### 3. Local-first trustworthiness

The final product should keep its local-only model as a strength, not a limitation hidden in the fine print:

- no backend dependency
- no account system
- no hidden sync story
- understandable local storage behavior
- explicit handling of missing media and device-local failure modes

### 4. Intentional admin and user workflow

The final product should clearly explain the same-device split:

- who the admin is
- who the user is
- why both surfaces exist on one device
- when admin intervention is expected

### 5. Public-repo credibility

The final repository should be understandable to a fresh engineer or agent without relying on chat history.

That means:

- accurate root reading order
- current contract docs that match the implementation
- durable decision history
- explicit glossary language
- bounded and honest evidence records

## End-state repository qualities

Before the repository should be treated as finished and ready to use for agent-led development, the documentation harness should support all of the following.

### Canonical reading order

A fresh agent should be able to find:

- current contract
- desired end state
- verification evidence
- architecture map
- glossary language
- active decisions
- remaining open work

without needing hidden context from prior sessions.

### Stable vocabulary

Terms such as `share`, `issue`, `pending`, `locked`, `expired`, `reissue`, `admin`, and `user` should mean one thing across:

- README
- glossary
- UI-facing product description
- architecture docs
- decision records

### Truth separation

The harness must preserve the distinction between:

- current verified state
- current observed implementation
- desired final state
- remaining gap

### Low-rediscovery maintenance

Future agents should not need to repeat repository archaeology to determine:

- what the product is
- what the repository currently proves
- what still blocks the final state

## Verification bar for the final state

The final product state should not be claimed until the repository can show proportionate evidence for these areas:

- `verified working` build and install path
- `verified working` bounded media import paths the repository chooses to claim publicly
- `verified working` Android share issuance flow
- `verified working` pending confirmation and rollback behavior
- `verified working` cooldown behavior across all reuse policies
- `verified working` expiry and manual reissue behavior
- `verified working` background reconciliation behavior at the claimed support level
- `verified limited` or better security posture explanation for admin PIN and local file exposure

## Architecture shape expected at the final state

The final product state does not require a large architecture, but it should require a clearer one than the current inherited layout.

The repository should end with:

- a coherent domain model for coupon lifecycle rules
- manageable UI boundaries instead of one oversized presentation file
- proportionate tests around lifecycle and repository behavior
- explicit handling of timing and reconciliation responsibilities
- documented reasoning for any remaining package-path drift or naming mismatches

## What should remain out of scope

The final product state should continue to exclude unless a later decision changes that scope:

- backend services
- cloud sync
- user accounts
- remote APIs
- analytics as a core product dependency
- marketplace behavior
- scanning or redemption infrastructure beyond local issuance

## Completion criteria

The documentation harness is ready for the final product state when a fresh agent can answer, from tracked docs alone:

1. What is Coupio?
2. What is the finished product meant to be?
3. What does the repository currently prove?
4. What still needs verification or implementation work?
5. Which documents should be updated when behavior or product intent changes?

If those answers still depend on chat history, the harness is not finished.
