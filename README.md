<p align="center">
  <img src="logo-round.png" alt="Coupio app icon" width="180" />
</p>

# Coupio

Coupio is an Android app for creating, organizing, and issuing relationship-style coupons from a single device.

## What It Does

An admin can import coupon media, add optional metadata such as title, description, category, expiry, and reuse policy, and then expose those coupons in a user-facing list. Sharing a coupon through Android is the issuance action. After a share, the coupon moves into a pending window and can then be confirmed, rolled back, or later reissued depending on its reuse policy.

## Core Features

- local coupon vault stored on-device
- image and document coupon import
- optional metadata and category organization
- coupon lifecycle tracking across available, pending, and locked states
- reuse policies such as single-use, daily, weekly, monthly, and always
- Android share-sheet issuance flow

## How It Works

Coupio is intentionally local-first and single-device. The app uses:

- Room for coupon persistence
- DataStore for admin PIN and lightweight preferences
- local file storage for coupon media
- `FileProvider` for secure share/export
- WorkManager for delayed pending-state finalization

There is no backend, sync service, or remote account dependency in the current product shape.

## Build And Run

```bash
./gradlew assembleDebug
./gradlew test
./gradlew connectedDebugAndroidTest
```

## Project Structure

- `app/` Android app module
- `docs/` product docs, verification notes, decisions, and evidence
- `.github/workflows/` debug CI and release automation

## Documentation

Start with:

- [docs/final-product-state.md](docs/final-product-state.md)
- [docs/verification-matrix.md](docs/verification-matrix.md)
- [docs/codebase-map.md](docs/codebase-map.md)

For repository workflow and agent-focused context, read [AGENTS.md](AGENTS.md).
