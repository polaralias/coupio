# Coupio Codebase Map

## Status of this document

- Built from code first, not from product claims.
- Existing docs and UI copy should be treated as intent, not proof.
- Code should be treated as the current implementation attempt, not as verified business truth.
- Verification performed for this pass:
  - `./gradlew test` succeeds on 2026-05-25.
  - `./gradlew connectedDebugAndroidTest` succeeds on 2026-05-25.
  - `./gradlew --gradle-user-home .gradle-codex --no-daemon --console plain assembleDebug` succeeds on 2026-05-25.
  - Lifecycle, repository, Android media import, and camera-target integration tests now exist.
  - The debug APK installs and launches on the local emulator surfaces used during the 2026-05-24 and 2026-05-25 evidence passes.

## Current documented product model

Coupio is currently documented as a small Android app for managing and sharing reusable or single-use coupons from one device.

The product model looks like this:

1. An admin imports a coupon asset, either an image or a PDF.
2. The admin attaches lightweight metadata:
   - title
   - description
   - category
   - expiry date
   - reuse policy
3. End users browse currently available coupons in a read-only view.
4. When a user shares a coupon through Android share, the app issues it to another person and moves it into a pending window.
5. During that window, the admin can either:
   - confirm/issue it
   - roll it back
6. After confirmation, the coupon either:
   - locks permanently
   - locks until a cooldown ends
   - immediately becomes available again

The current default pending window is one hour, but this is now understood as a default that may later become configurable.

This is best understood as a lightweight coupon vault with a manual issuance workflow for a single-device local setup, not a marketplace, scanning system, or remote service.

## Top-level structure

- `app/`
  - Entire Android application.
- `app/src/main/java/com/polaralias/coupio/`
  - Main code location on disk.
- `app/src/main/res/`
  - Android resources.
- `app/src/test/`
  - Focused lifecycle and repository unit test surface.
- `app/src/androidTest/`
  - On-device Android integration test surface for media import/storage seams.
- `app/schemas/`
  - Room schema export for database version 1.

## Runtime map

### 1. App bootstrap

- `CouponApp` creates a simple service container on application startup.
- `MainActivity` is the only Activity and mounts the Compose UI.
- There is no navigation framework and no multi-screen architecture; the app is effectively one composed screen with tab switching.

Relevant files:

- `app/src/main/java/com/polaralias/coupio/CouponApp.kt`
- `app/src/main/java/com/polaralias/coupio/MainActivity.kt`
- `app/src/main/AndroidManifest.xml`

### 2. Dependency container

`AppContainer` wires together:

- Room database
- coupon repository
- admin PIN repository
- app preferences repository
- lifecycle manager
- WorkManager-backed scheduler

There is no DI framework. The dependency graph is small and manual.

Relevant file:

- `app/src/main/java/com/polaralias/coupio/data/AppContainer.kt`

### 3. UI surface

The UI is split into two main tabs:

- `Coupons`
  - public/user-facing browsing and sharing
- `Admin`
  - protected import/edit/issue workflow

The UI is entirely Compose-based and mostly implemented in one large file. That file contains:

- top-level screen composition
- tab rendering
- admin PIN gate
- editor sheet
- coupon card rendering
- media preview
- status messaging

This is the main app surface and currently the largest concentration of presentation logic.

Relevant file:

- `app/src/main/java/com/polaralias/coupio/ui/CouponAppScreen.kt`

### 4. View-model and state orchestration

`AppViewModel` is the central coordinator.

It owns:

- search query
- filter state
- admin unlocked state
- coupon editor state
- ticker-like current time state
- event stream for snackbars and share intents

It combines repository data and UI inputs into a single `AppUiState`.

Important behavior:

- Calls `reconcileDueCoupons()` on startup.
- Re-runs reconciliation every 15 seconds in-process.
- Opens editors for import and edit flows.
- Handles PIN creation/verification.
- Executes coupon share, confirm, revert, and reissue actions.

This means business state transition logic is split between:

- repository operations
- lifecycle manager rules
- periodic ViewModel reconciliation
- background WorkManager reconciliation

Relevant file:

- `app/src/main/java/com/polaralias/coupio/ui/AppViewModel.kt`

Observed implementation boundary:

- the repository now returns prepared share metadata
- the Compose/UI layer creates the Android `Uri` through `FileProvider` just before launching share

This keeps Android-specific sharing details out of repository tests.

## Domain model

### Coupon

The core entity is `CouponEntity`.

Observed fields:

- media location and mime info
- human metadata
- expiry date
- reuse policy
- lifecycle state
- pending and cooldown timestamps
- audit-ish created/updated timestamps

Observed states:

- `AVAILABLE`
- `PENDING`
- `LOCKED`

Observed reuse policies:

- `SINGLE_USE`
- `DAILY`
- `WEEKLY`
- `MONTHLY`
- `ALWAYS`

Relevant files:

- `app/src/main/java/com/polaralias/coupio/data/local/CouponEntity.kt`
- `app/src/main/java/com/polaralias/coupio/data/model/CouponModels.kt`

### State transitions

The main lifecycle rules live in `CouponLifecycleManager`.

Observed transition model:

- `AVAILABLE` -> `PENDING`
  - triggered by share
  - starts a one-hour pending window
- `PENDING` -> `AVAILABLE`
  - admin rollback
- `PENDING` -> `LOCKED` or `AVAILABLE`
  - admin confirmation or delayed auto-confirm
  - result depends on reuse policy
  - cooldown-based policies anchor their lock window to confirmation time
- `LOCKED` -> `AVAILABLE`
  - admin reissue or cooldown expiry

This is the clearest business domain in the repo and probably the real center of the app.

Important documented interpretation:

- `SINGLE_USE` does not mean permanently non-reissuable.
- Admins may manually reissue coupons after confirmation, including expired coupons.
- Expiry is therefore a user-facing availability signal, not an irreversible terminal state.

Relevant file:

- `app/src/main/java/com/polaralias/coupio/data/CouponLifecycleManager.kt`

## Persistence and local storage

### Database

Room database version `1`, single table:

- `coupons`

No migrations exist yet. The current schema is stable for the repository's first documented release, but there is still no demonstrated upgrade path for a later schema change.

Relevant files:

- `app/src/main/java/com/polaralias/coupio/data/local/AppDatabase.kt`
- `app/src/main/java/com/polaralias/coupio/data/local/CouponDao.kt`
- `app/schemas/com.polaralias.coupio.data.local.AppDatabase/1.json`

### File storage

Coupon media is persisted into app-internal files storage:

- `filesDir/coupons/`

Camera captures are staged in cache first:

- `cacheDir/captures/`

Sharing happens through an Android `FileProvider`.

Observed implication:

- The app is local-first.
- There is no remote sync or backend.
- Coupon assets live entirely on-device.

Relevant files:

- `app/src/main/java/com/polaralias/coupio/MainActivity.kt`
- `app/src/main/java/com/polaralias/coupio/data/CouponRepository.kt`
- `app/src/main/java/com/polaralias/coupio/ui/CouponAppScreen.kt`
- `app/src/main/res/xml/file_paths.xml`
- `app/src/androidTest/java/com/polaralias/coupio/data/AndroidMediaImportIntegrationTest.kt`

### Preferences

Two DataStore-backed preference areas exist:

- admin PIN storage
- issuer display name

The PIN is hashed with PBKDF2 and random salt before storage, which is materially stronger than plain-text storage.

Relevant files:

- `app/src/main/java/com/polaralias/coupio/data/AdminPinRepository.kt`
- `app/src/main/java/com/polaralias/coupio/data/AppPreferencesRepository.kt`

## Background and timing behavior

There are two independent mechanisms trying to keep coupon state current:

1. `WorkManager`
   - schedules one-time finalize work after a coupon is shared
2. `AppViewModel` polling
   - every 15 seconds, reconciles overdue pending/locked coupons while the app is alive

Interpretation:

- WorkManager covers delayed completion when the app is not foregrounded.
- ViewModel polling covers visible freshness while the app is open.

This is functional, but it also means timing behavior is distributed across multiple layers and should be treated as a verification hotspot.

Relevant files:

- `app/src/main/java/com/polaralias/coupio/data/CouponRepository.kt`
- `app/src/main/java/com/polaralias/coupio/work/FinalizePendingCouponWorker.kt`
- `app/src/main/java/com/polaralias/coupio/ui/AppViewModel.kt`

## What appears implemented vs what does not

### Implemented or mostly implemented

- import coupon from document picker
- capture coupon from camera
- save image/PDF locally
- browse coupons
- search and filter coupons
- protect admin area with PIN
- edit coupon metadata
- share coupon file out to another app
- pending confirmation window
- manual confirm / rollback / reissue
- cooldown-based unlock behavior

### Not present in the codebase

- backend or cloud sync
- user accounts
- analytics
- redemption tracking beyond local state
- barcode/QR scanning
- network APIs
- explicit migration strategy
- formal architecture boundaries beyond package folders

## Initial quality/readiness assessment

### Strengths

- Scope is constrained and coherent.
- The product idea is legible from code.
- The lifecycle manager is separated enough to reason about.
- Local persistence choices are practical for the app concept.
- Unit tests now cover the main lifecycle rules and core repository transitions.
- Android integration tests now cover the real media-storage seams on-device.
- The app builds cleanly enough for unit tests, connected Android integration tests, and debug assembly to pass.

### Weaknesses and risk areas

- Large UI file with mixed concerns makes verification slower.
- Business logic is split across repository, lifecycle manager, worker, and ViewModel polling.
- WorkManager timing and external app interoperability still rely on limited evidence rather than complete end-to-end proof.
- No migration strategy beyond schema export.
- UI event wiring still depends more on manual/runtime confidence than on direct automated UI coverage.

## Remaining unknowns that need explicit verification

These are not proven by this pass:

- whether third-party picker interoperability is consistent across the Android versions and images worth claiming publicly
- whether third-party camera-app interoperability is consistent across the Android versions and images worth claiming publicly
- whether WorkManager finalization survives process death and device reboot as intended
- whether expiry semantics are correct for the target use case
- whether a future configurable pending window preserves lifecycle behavior cleanly
- how expired-yet-manually-reissued coupons should present in runtime UX

## Evidence touched in this pass

- `app/src/main/AndroidManifest.xml`
- `app/src/main/java/com/polaralias/coupio/CouponApp.kt`
- `app/src/main/java/com/polaralias/coupio/MainActivity.kt`
- `app/src/main/java/com/polaralias/coupio/data/AppContainer.kt`
- `app/src/main/java/com/polaralias/coupio/data/AppPreferencesRepository.kt`
- `app/src/main/java/com/polaralias/coupio/data/AdminPinRepository.kt`
- `app/src/main/java/com/polaralias/coupio/data/CouponLifecycleManager.kt`
- `app/src/main/java/com/polaralias/coupio/data/CouponRepository.kt`
- `app/src/main/java/com/polaralias/coupio/data/local/AppDatabase.kt`
- `app/src/main/java/com/polaralias/coupio/data/local/CouponDao.kt`
- `app/src/main/java/com/polaralias/coupio/data/local/CouponEntity.kt`
- `app/src/main/java/com/polaralias/coupio/data/model/CouponModels.kt`
- `app/src/main/java/com/polaralias/coupio/ui/AppViewModel.kt`
- `app/src/main/java/com/polaralias/coupio/ui/CouponAppScreen.kt`
- `app/src/main/java/com/polaralias/coupio/ui/theme/CouponGlassTheme.kt`
- `app/src/main/java/com/polaralias/coupio/work/FinalizePendingCouponWorker.kt`
- `app/src/androidTest/java/com/polaralias/coupio/data/AndroidMediaImportIntegrationTest.kt`
- `app/src/test/java/com/polaralias/coupio/data/CouponLifecycleManagerTest.kt`
- `app/src/test/java/com/polaralias/coupio/data/CouponRepositoryTest.kt`
- `app/schemas/com.polaralias.coupio.data.local.AppDatabase/1.json`
- `docs/evidence/2026-05-24-emulator-smoke-check.md`
- `docs/evidence/2026-05-25-android-integration-check.md`
