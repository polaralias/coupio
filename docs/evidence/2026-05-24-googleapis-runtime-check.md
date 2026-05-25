# Google APIs Runtime Check - 2026-05-24

## Scope

Bounded runtime verification on the local `Medium_Phone_API_36_GoogleApis` AVD, focused on the remaining picker and camera questions after the ATD lifecycle pass.

This note records only what was directly observed on this image.

## Environment

- Host date: 2026-05-24
- AVD: `Medium_Phone_API_36_GoogleApis`
- Device serial: `emulator-5554`
- APK installed through `./gradlew --gradle-user-home .gradle-codex --no-daemon --console plain installDebug`

## Runtime conditions observed

- the image booted but remained noisy during startup
- a `System UI isn't responding` dialog appeared during app launch
- tapping `Wait` cleared the dialog and allowed the app to continue

Interpretation:

- this image is usable for bounded checks
- it is not a clean enough runtime surface to overstate emulator stability

## Picker and camera findings

### 1. Import file from the app UI

Observed:

- the Admin area was opened with a fresh PIN setup
- tapping `Import file` from the live app UI showed snackbar text:
  - `No file picker available on this device.`
- the app stayed in the foreground
- `adb logcat -d -b crash` remained empty

Supporting context:

- this image has packages including:
  - `com.google.android.documentsui`
  - `com.google.android.apps.docs`
  - `com.google.android.apps.photos`
- despite that, Coupio's `ACTION_OPEN_DOCUMENT` availability gate still treated the picker as unavailable on this image

Status:

- unsupported-picker fallback: `verified working`
- real picker-backed image or PDF import: `blocked`

### 2. PDF import readiness

Observed:

- a real PDF test file was pushed to `/sdcard/Download/coupio-import-doc.pdf`
- the app could not reach a picker surface to select it because the same `OPEN_DOCUMENT` gate path stayed unavailable

Status:

- `blocked`

### 3. Camera handler availability

Observed:

- platform resolution succeeded for `android.media.action.IMAGE_CAPTURE`
- direct shell launch succeeded:
  - resolved activity: `com.android.camera2/com.android.camera.CaptureActivity`
  - `am start -W -a android.media.action.IMAGE_CAPTURE` returned `Status: ok`
- repeated `adb input tap` attempts on Coupio's `Camera capture` button did not transition away from `MainActivity`, so the app-driven capture path was not completed through the live UI in this pass

Interpretation:

- this emulator image has a usable camera handler at the platform level
- the missing proof is app-to-camera end-to-end interaction on this unstable emulator surface, not absence of a camera activity

Status:

- camera activity availability: `verified limited`
- app-driven camera capture flow: `blocked`

## Net result

- admin lifecycle verification is strongest on the ATD image
- picker-backed import remains blocked on this workstation's available emulator images
- camera capability exists on the Google APIs image, but app-level completion still needs a cleaner device or more reliable UI automation
