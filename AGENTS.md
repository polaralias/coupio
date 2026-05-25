# AGENTS.md

## Purpose

This repository is in an evidence-first cleanup phase.

Future work should prefer verified repository truth over inherited assumptions, UI copy, or optimistic interpretation of code.

## Reading order

Read these files in order before making substantive changes:

1. `README.md`
2. `docs/final-product-state.md`
3. `docs/verification-matrix.md`
4. `docs/codebase-map.md`
5. `GLOSSARY.md`
6. `docs/query-surface.md`
7. `docs/decisions/`

Then read the relevant code paths.

## Canonical knowledge surfaces

- `README.md`
  - root reading order and current contract summary
- `docs/final-product-state.md`
  - desired end-state product and repository contract
- `docs/verification-matrix.md`
  - current evidence state
- `docs/codebase-map.md`
  - architecture and observed runtime map
- `GLOSSARY.md`
  - domain language and concept boundaries
- `docs/query-surface.md`
  - active question surface and irreducible user judgments
- `docs/decisions/`
  - durable decisions and rationale

## Status language

Use these terms consistently in docs:

- `verified working`
- `verified limited`
- `observed in code`
- `untested`
- `blocked`
- `known broken`

Do not blur these categories.

## Documentation rules

- Treat code as current implementation, not automatic truth.
- Treat docs as claims that need evidence.
- Preserve dated evidence instead of silently rewriting history.
- Do not widen product or support claims based on manifests or code structure alone.
- Keep root docs free of workstation-specific paths unless the path itself is the evidence.
- When behavior changes, update the canonical docs in the same tranche.

## Project-specific guidance

- The main product behavior currently appears to be a local-first coupon vault with manual issuance.
- The intended end state is still intentionally small: a polished single-device Android coupon vault, not a broader platform.
- The highest-risk surfaces are lifecycle timing, media import/share, and admin/user workflow assumptions.
- Filesystem package paths and Kotlin package names currently drift; document this carefully before changing it.

## When to ask questions

Ask the user when:

- the repository cannot resolve product intent
- docs and code conflict on an important behavior
- a support claim would change publicly
- a durable naming or workflow decision must be made

Prefer one dense question batch over a long sequence of small clarification turns.

## Shared Git Workflow

- work from a short-lived branch created from `main`
- do not commit directly to `main`
- use branch names prefixed with `feat/`, `fix/`, `docs/`, `chore/`, `refactor/`, or `test/`
- keep one logical change per branch and pull request
- open a pull request before merging to `main`, including for solo work
- prefer squash merge unless multiple commits carry durable review value
- delete the merged or closed feature branch after the work is finished; never delete `main`
- use tags in `vX.Y.Z` format for releases and do not move published tags
