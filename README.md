<p align="center">
  <img src="app/src/main/res/mipmap-xxxhdpi/ic_launcher_round.png" alt="Coupio app icon" width="160" />
</p>

# Coupio

Coupio is an Android app for storing, organizing, and manually issuing coupons from a single device.

At its current documented contract, the app lets an admin load coupon media into the local vault, attach optional metadata, and expose those coupons in a user-facing list. Sharing a coupon through the Android share sheet is the intended issuance action, such as sending the coupon to a partner. After a share, the app moves the coupon into a one-hour pending window by default; an admin can then confirm it, roll it back, or later reissue it depending on the coupon's reuse policy.

This repository has completed its audit-and-verification cleanup pass. The goal of that work was to turn an inherited, under-documented project into a public, portfolio-grade codebase. Where behavior remains only partly verified, code and UI copy should still be treated as implementation attempts rather than authoritative product truth.

The documentation harness now separates:

- current contract
- desired final product state
- current verification evidence
- current implementation map

## Current observed scope

- Single Android app module
- Local-only and single-device by product intent
- Kotlin + Jetpack Compose UI
- Room database for coupon records
- DataStore for admin PIN and lightweight app preferences
- Local file storage for coupon media
- `FileProvider` share flow for coupon export
- WorkManager for delayed pending-finalization

There is no backend, authentication service, sync layer, or network API in this repository. This local-only and single-device shape is the current documented product contract, not just an implementation constraint.

## Core behavior observed in code

The core domain model is a coupon lifecycle with three states:

- `AVAILABLE`
- `PENDING`
- `LOCKED`

Observed flow:

1. Admin imports a coupon from file picker or camera.
2. Admin optionally sets title, description, category, expiry date, and reuse policy.
3. User browses available coupons.
4. User shares a coupon through Android share to issue it to another person.
5. The coupon enters a one-hour pending window by default.
6. Admin either confirms, rolls back, or later reissues the coupon depending on policy.

Observed reuse policies:

- `SINGLE_USE`
- `DAILY`
- `WEEKLY`
- `MONTHLY`
- `ALWAYS`

## Verification status

What has been confirmed in this environment:

- `./gradlew test` succeeds on 2026-05-25
- `./gradlew connectedDebugAndroidTest` succeeds on 2026-05-25
- `./gradlew --gradle-user-home .gradle-codex --no-daemon --console plain assembleDebug` succeeds on 2026-05-25
- the debug APK installs and launches on the local `Medium_Phone_API_36` emulator through direct `adb`/`emulator` SDK paths
- the repository is a single-module Android app
- the local persistence and lifecycle architecture are coherent enough to map
- lifecycle and repository rule coverage now exists for share, confirm, rollback, reissue, reconciliation, import metadata, metadata updates, and confirmation-anchored cooldown timing
- on-device Android integration coverage now exists for temp-file image import, `FileProvider` PDF import, and camera capture target generation

What has not yet been fully verified:

- third-party picker interoperability on a device that exposes `OPEN_DOCUMENT`
- third-party camera-app interoperability end to end
- share receiver compatibility beyond chooser launch
- WorkManager timing under background/process-death conditions
- full admin workflow behavior on-device beyond PIN unlock and share-to-pending
- configured pending-window behavior beyond the current one-hour default

Read next:

- `docs/final-product-state.md`
- `docs/verification-matrix.md`
- `docs/codebase-map.md`
- `GLOSSARY.md`
- `docs/query-surface.md`
- `docs/decisions/`
- `AGENTS.md`

## Project structure

- `app/`
  - Android application module
- `AGENTS.md`
  - repository operating guide and reading order for future agents
- `GLOSSARY.md`
  - current domain language and concept boundaries
- `docs/final-product-state.md`
  - desired end-state product and repository shape
- `docs/query-surface.md`
  - repository-level ambiguity and resolved question surface
- `docs/evidence/`
  - dated validation notes and supporting artifacts
- `docs/codebase-map.md`
  - current architecture and product-intent map
- `docs/decisions/`
  - durable repository decisions
- `docs/verification-matrix.md`
  - what is verified, inferred, blocked, or still unknown

## Notable current risks

- UI, workflow, and presentation logic are concentrated in a small number of large files
- automated coverage is still weighted toward domain and repository rules, with a smaller Android integration surface layered on top
- final-state quality expectations are now documented, but most are not yet verified working

## Build notes

Primary validation commands:

```powershell
./gradlew test
./gradlew connectedDebugAndroidTest
./gradlew --gradle-user-home .gradle-codex --no-daemon --console plain assembleDebug
```

Notes:

- the repository now has a verified working debug assembly path from this shell
- the workstation's default Gradle cache can still hit concurrent transform-cache locks when other Java/Gradle processes are active
- device checks on this workstation currently work best when the Android SDK tools are called directly or added to `PATH`

## Documentation stance

This repository intentionally separates:

- current verified behavior
- current observed implementation
- desired final product state
- resolved product decisions and any new question surface

If those categories collapse back together, the repo will become hard to trust again.
