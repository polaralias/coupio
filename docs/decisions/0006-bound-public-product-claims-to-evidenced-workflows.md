# Decision 0006: Bound public product claims to evidenced workflows

- Date: 2026-05-25
- Status: accepted

## Context

The repository is approaching public-ready status, but some workflow details still have uneven evidence strength.

In particular:

- Android media persistence seams are now verified on-device
- chooser launch for share is verified
- third-party picker and camera-app interoperability are still only partly evidenced on the available emulator surfaces

Without an explicit rule, public-facing docs would drift toward naming workflow variants more confidently than the repository can currently prove.

## Decision

Public-facing docs such as `README.md` must describe the current product contract at the level the repository can actually evidence.

That means:

- current contract docs should describe bounded local import/capture flows without widening platform claims prematurely
- specific picker, PDF, camera, or receiver-compatibility claims belong in `docs/verification-matrix.md` unless they are directly evidenced at the claimed support level
- dated evidence notes should capture new runtime proof before broader product copy is widened

## Consequences

### Positive

- Public docs stay honest even when implementation detail outruns verification.
- The verification matrix remains the place to read support strength rather than inferred marketing copy.
- Future publish passes can promote workflow detail safely as new evidence lands.

### Costs

- Public-facing copy will sometimes be less specific than the code structure alone suggests.
- Contributors must update evidence and contract docs together when support claims improve.

## Follow-on implications

- `README.md` should stay generic where interoperability is not yet directly proven.
- `docs/final-product-state.md` should phrase media expectations in terms of publicly claimed workflows rather than unevidenced variants.
- Future device passes that verify third-party picker or camera interoperability should update the verification matrix first, then widen any public claim in the same tranche.
