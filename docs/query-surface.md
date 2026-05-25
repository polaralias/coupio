# Coupio Query Surface

## Purpose

This document records the repository question surface for agent-led development.

Use it to separate:

- questions already answered by repository evidence
- contradictions that still need a human judgment
- question dependencies that should be resolved before wider refactor or support claims

This is not a product spec. It is the retained ambiguity map for contradictions, judgments, and their resolution state.

## Repository-answered truths

Future agents should not keep re-asking these unless new evidence appears:

- The current implementation is a single Android app module with no backend, sync layer, or network API.
- Coupon media is stored locally on-device and shared out through Android intents and a `FileProvider`.
- The core stored lifecycle states are `AVAILABLE`, `PENDING`, and `LOCKED`.
- The current share flow moves an `AVAILABLE` coupon into `PENDING`.
- The current implementation uses a one-hour pending window via `DEFAULT_PENDING_WINDOW_MILLIS`.
- Cooldown-based reuse windows start when a pending coupon is confirmed, not when share is first requested.
- State reconciliation is currently distributed across repository logic, a `WorkManager` worker, and 15-second in-app polling.
- The admin area is currently PIN-gated on the same device as the user-facing browsing surface.
- Current reuse policies in code are `SINGLE_USE`, `DAILY`, `WEEKLY`, `MONTHLY`, and `ALWAYS`.

## Resolved product judgments

These points are now resolved and should be treated as current repository contract until changed by a later decision.

- Coupio should be documented as local-only and single-device by product intent.
- Sharing through Android share is the intended issuance workflow, such as sharing a coupon to a partner.
- The pending window is currently one hour by default and may later become configurable.
- `SINGLE_USE` coupons may still be manually reissued by an admin after confirmation.
- Expired coupons may also be manually reissued by an admin.
- `DAILY`, `WEEKLY`, and `MONTHLY` cooldown windows start at confirmation time.

## Current question state

There are no equivalent product-intent blockers at the moment.

Future additions to this document should be limited to new contradictions or user-judgment decisions that materially affect product claims, workflow meaning, support boundaries, or the final product state.

## Capture rule

When these answers are provided:

- update `README.md` for the product contract
- update `GLOSSARY.md` for canonical terms
- add or update `docs/decisions/` if the answer is a durable policy or workflow choice
- update `docs/codebase-map.md` if the implementation map changes
