# Android Integration Check

- Date: 2026-05-25
- Environment: local emulator `Medium_Phone_API_35_Play`
- Scope: on-device Android integration coverage for media import/storage seams and camera capture target generation

## Commands

```powershell
./gradlew test
./gradlew connectedDebugAndroidTest
./gradlew --gradle-user-home .gradle-codex --no-daemon --console plain assembleDebug
```

## Result

- `./gradlew test`: `verified working`
- `./gradlew connectedDebugAndroidTest`: `verified working`
- `./gradlew --gradle-user-home .gradle-codex --no-daemon --console plain assembleDebug`: `verified working`

## Android integration coverage exercised

The connected test suite now verifies three device-level seams:

1. image import from a temp file into app-managed coupon storage
2. PDF import from a `FileProvider` URI into app-managed coupon storage
3. camera capture target generation through `createCameraCaptureTarget()`

Covered test file:

- `app/src/androidTest/java/com/polaralias/coupio/data/AndroidMediaImportIntegrationTest.kt`

Observed outcomes:

- imported image media is copied into `filesDir/coupons`
- imported PDF media is copied into `filesDir/coupons` and keeps a `.pdf` extension
- temp-file imports delete the source temp file after persistence
- `FileProvider` URI imports leave the source file in place
- generated camera capture targets point at `cacheDir/captures` with the app `FileProvider` authority

## Remaining limits

- This pass does not prove third-party picker interoperability on arbitrary emulator images or physical devices.
- This pass does not prove third-party camera-app interoperability end to end.
- This pass does not prove Android share receiver compatibility beyond chooser launch evidence from 2026-05-24.
- This pass does not prove WorkManager behavior through reboot or process death.
