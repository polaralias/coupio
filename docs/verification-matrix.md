# Coupio Verification Matrix

## Purpose

This document records the current verification state of the repository.

- `verified working`: directly exercised in this environment and behaved as expected for the exercised scope.
- `verified limited`: partly verified or verified only through a constrained surface such as config, code review, or a bounded runtime smoke check.
- `observed in code`: implemented in code, but not exercised end-to-end in this pass.
- `untested`: product-critical behavior that still needs explicit testing.
- `blocked`: could not be verified from this environment due to tooling or host constraints.
- `known broken`: directly exercised and shown to fail as implemented.

## Environment used for this pass

- Date: 2026-05-25
- Working directory: repository root
- Platform context available:
  - local source tree and Gradle
  - Android SDK with working `adb` and emulator access
  - local AVDs `Medium_Phone_API_36`, `Medium_Phone_API_36_GoogleApis`, `Medium_Phone_API_35_AOSP_ATD`, and `Medium_Phone_API_35_Play`
  - app-owned `androidTest` coverage for Android storage seams
- Platform context unavailable:
  - no broad third-party UI automation framework beyond `adb`, `uiautomator dump`, and app-owned `androidTest`

## Verification summary

### Build and test

| Area | Status | Evidence | Notes |
|---|---|---|---|
| Unit tests run | verified working | `./gradlew test` succeeded on 2026-05-25 | Coverage includes lifecycle and repository rule paths, including confirmation-anchored cooldown timing. |
| Connected Android integration tests run | verified working | `./gradlew connectedDebugAndroidTest` succeeded on 2026-05-25; see `docs/evidence/2026-05-25-android-integration-check.md` | Covers Android media persistence seams and camera capture target generation on-device. |
| Debug assembly with isolated Gradle home | verified working | `./gradlew --gradle-user-home .gradle-codex --no-daemon --console plain assembleDebug` succeeded on 2026-05-25 | Confirms the repository compiles into a debug APK from this shell. |
| Debug assembly with default Gradle home | verified working | `./gradlew assembleDebug` completed during the 2026-05-25 connected-test pass | Earlier lock contention was environmental rather than a repository defect. |
| Gradle environment | verified limited | Gradle 9.1.0 daemons and cache locks were active during the pass | Host is prone to overlapping Gradle work unless the build runs with an isolated cache or a drained daemon set. |

### App structure

| Area | Status | Evidence | Notes |
|---|---|---|---|
| Single Android app module | verified working | `settings.gradle.kts`, `app/build.gradle.kts` | No library modules or backend components. |
| One application entry point | verified working | `app/src/main/AndroidManifest.xml`, `CouponApp.kt`, `MainActivity.kt` | Single-activity Compose app. |
| Local-only persistence architecture | verified working | Room, DataStore, app files, `FileProvider` usage | No network or remote API code found. |

### Coupon lifecycle rules

| Area | Status | Evidence | Notes |
|---|---|---|---|
| Pending window constant | observed in code | `DEFAULT_PENDING_WINDOW_MILLIS` in `CouponModels.kt` | Set to one hour. |
| Share moves coupon to pending | verified working | `docs/evidence/2026-05-24-atd-runtime-check.md`, plus repository and lifecycle tests | Verified on the ATD AVD through the Android chooser path and pending-state UI. |
| Pending can be confirmed | verified working | `docs/evidence/2026-05-24-atd-runtime-check.md` plus repository and lifecycle tests | Verified through the admin `Issue now` path on the ATD AVD. |
| Pending can be rolled back | verified working | `docs/evidence/2026-05-24-atd-runtime-check.md` plus repository and lifecycle tests | Verified through the admin `Roll back` path on the ATD AVD. |
| Cooldown-based reissue rules | verified limited | lifecycle tests cover `SINGLE_USE`, `ALWAYS`, `DAILY`, `WEEKLY`, `MONTHLY` | Cooldown timing is now explicitly covered as confirmation-anchored, but runtime timing still needs device verification. |
| Time-based reconciliation | verified limited | repository and lifecycle tests on 2026-05-24 | WorkManager and process-death behavior still need fuller runtime verification. |

### Media pipeline

| Area | Status | Evidence | Notes |
|---|---|---|---|
| Document picker import path exists | verified limited | `docs/evidence/2026-05-24-atd-runtime-check.md`, `docs/evidence/2026-05-24-googleapis-runtime-check.md`, `docs/evidence/2026-05-25-android-integration-check.md`, and `OpenDocument()` in `CouponAppScreen.kt` | App-owned URI import is verified working on-device. Real third-party picker interoperability remains unverified on the available emulator images. |
| Camera capture path exists | verified limited | `docs/evidence/2026-05-24-googleapis-runtime-check.md`, `docs/evidence/2026-05-25-android-integration-check.md`, `TakePicture()`, and `createCameraCaptureTarget()` | Camera target generation is verified working on-device. Third-party camera-app interoperability is still not fully exercised end to end. |
| Imported media persisted to app files | verified working | `docs/evidence/2026-05-25-android-integration-check.md` and `AndroidMediaImportIntegrationTest.kt` | On-device integration now proves temp-file image import and `FileProvider` PDF import into app-managed coupon storage. |
| Share via Android intent | verified limited | `docs/evidence/2026-05-24-atd-runtime-check.md` and `Intent.ACTION_SEND` in `CouponAppScreen.kt` | Chooser launch is verified working. Receiver compatibility remains unverified. |
| FileProvider paths | verified limited | `app/src/main/res/xml/file_paths.xml` | Covers `files/coupons` and `cache/captures`. |
| PDF preview behavior | observed in code | `MediaPreview()` shows PDF placeholder card | Import persistence is verified, but PDF preview behavior is still UI-observed rather than directly exercised. |

### Admin and access model

| Area | Status | Evidence | Notes |
|---|---|---|---|
| Admin area is PIN-gated | verified working | `docs/evidence/2026-05-24-atd-runtime-check.md` | Verified on the ATD AVD for configured PIN unlock. |
| PIN hashing | verified limited | `AdminPinRepository.kt` uses PBKDF2 + random salt | Better than plaintext, but the threat model is still lightly documented. |
| Issuer name preference | observed in code | `AppPreferencesRepository.kt` | Used in status copy for locked and expired guidance. |
| Locked coupons can be manually reissued | verified working | `docs/evidence/2026-05-24-atd-runtime-check.md` | Verified through the admin `Unlock again` action on the ATD AVD. |

### UI behavior

| Area | Status | Evidence | Notes |
|---|---|---|---|
| App installs and launches on local emulator | verified limited | `docs/evidence/2026-05-24-emulator-smoke-check.md`, `docs/evidence/2026-05-24-atd-runtime-check.md` | Launch proved on both the original emulator path and the cleaner ATD path. |
| Coupons tab renders on first launch | verified limited | ATD `uiautomator dump` evidence in `docs/evidence/2026-05-24-atd-runtime-check.md` | The ATD path provided cleaner runtime proof than the earlier screenshot-only surface. |
| Search and filter | observed in code | `matchesQuery()`, `matchesFilter()` | Needs manual verification with real data. |
| Admin coupon editing | observed in code | `CouponEditorSheet()` and `saveEditor()` | Needs manual verification. |
| Snackbars and share events | verified limited | `docs/evidence/2026-05-24-atd-runtime-check.md`, `docs/evidence/2026-05-24-googleapis-runtime-check.md` | Verified through UI-dump evidence for unlock, rollback, issue, and unsupported-picker snackbars plus chooser launch on share. |

## Existing automated coverage

Current unit test files:

- `app/src/test/java/com/polaralias/coupio/data/CouponLifecycleManagerTest.kt`
- `app/src/test/java/com/polaralias/coupio/data/CouponRepositoryTest.kt`
- `app/src/test/java/com/polaralias/coupio/ui/ExternalActionGateTest.kt`

Current Android integration test files:

- `app/src/androidTest/java/com/polaralias/coupio/data/AndroidMediaImportIntegrationTest.kt`

Covered:

- single-use confirmation locks until manual reissue
- always-available confirmation returns to available
- daily, weekly, and monthly cooldown reissues after time passes
- cooldown windows start at confirmation time rather than initial share request
- rollback behavior
- share preparation for available, expired, and stale locked coupons
- repository confirm, revert, manual reissue, and reconciliation flows
- import metadata normalization and repository metadata updates
- Android temp-file image import into app storage
- Android `FileProvider` PDF import into app storage
- Android camera capture target generation

Not covered:

- expiry behavior
- worker behavior
- DataStore-backed admin preferences
- Compose/UI event wiring
- real third-party picker interoperability on a device/image where `ACTION_OPEN_DOCUMENT` resolves for Coupio
- app-driven camera interoperability with a third-party camera app

## Host-specific caveats encountered

### 1. SDK tools are available outside `PATH`

- `adb` and `emulator` are not on `PATH`.
- Direct SDK binaries worked successfully.
- Result:
  - emulator-based smoke verification is available on this workstation
  - future passes should either call the SDK binaries directly or add the SDK tool directories to `PATH`

### 2. Shared Gradle cache contention is intermittent rather than a repository defect

- an earlier `assembleDebug` run using the default Gradle home hit a transform-cache lock
- `assembleDebug` later succeeded during the 2026-05-25 connected-test pass
- the isolated Gradle home path remains the most repeatable option from this shell

Interpretation:

- the repository build itself is `verified working`
- any remaining default-cache failures should be treated as workstation/process-state issues first

### 3. Emulator environment stability is still limited, but the ATD path is better

- The local AVD booted, installed the app, and launched `MainActivity`.
- A screenshot taken after launch showed the app UI rendered behind a `System UI isn't responding` overlay from the emulator environment.
- A later pass on `Medium_Phone_API_35_AOSP_ATD` provided a cleaner runtime surface for launch, PIN unlock, share-to-pending, and unsupported-picker fallback.

Interpretation:

- the app launch path is stronger than before
- emulator-based manual verification is now feasible on this machine, especially on the ATD image
- the AVD environment itself is still noisy enough that publish claims should remain conservative
