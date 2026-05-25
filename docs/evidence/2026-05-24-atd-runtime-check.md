# ATD Runtime Check - 2026-05-24

## Scope

Bounded runtime verification on the local Android Test Device AVD after emulator setup tuning and a small runtime guard fix.

This note records what was directly exercised on the `Medium_Phone_API_35_AOSP_ATD` emulator. It does not widen support claims beyond those exact paths.

## Environment

- Host date: 2026-05-24
- AVD: `Medium_Phone_API_35_AOSP_ATD`
- Device serial: `emulator-5554`
- APK installed: `app/build/outputs/apk/debug/app-debug.apk`

## Build and install

Commands exercised:

```powershell
./gradlew --gradle-user-home .gradle-codex --no-daemon --console plain testDebugUnitTest --tests com.polaralias.coupio.ui.ExternalActionGateTest
```

```powershell
./gradlew --gradle-user-home .gradle-codex --no-daemon --console plain assembleDebug installDebug
```

Observed:

- targeted unit test pass succeeded after adding the external-action guard
- debug APK assembled and installed successfully on the ATD AVD

## Runtime actions exercised

### 1. App launch on ATD

Observed:

- the app launched on the ATD AVD
- `uiautomator dump` showed the Compose UI hierarchy under `com.polaralias.coupio`
- black screencaps remained unreliable on this AVD, so UI-dump evidence remained the stronger proof surface

Status:

- `verified limited`

### 2. Admin PIN gate

Observed:

- the Admin tab opened
- first-run PIN setup worked earlier in the same workstation pass
- after relaunch, entering the configured PIN unlocked the backstage area
- the snackbar `Backstage unlocked.` appeared in the UI dump

Status:

- `verified working`

### 3. Share to pending transition

Observed:

- a seeded `DAILY` coupon rendered in the Coupons tab
- tapping `Share to redeem` launched the Android chooser
- returning to the app showed:
  - summary counts changed from `Available 1 / Pending 0 / Locked 0` to `Available 0 / Pending 1 / Locked 0`
  - the coupon badge changed to `Pending`
  - pending status copy showed the one-hour waiting window
- `adb logcat -d -b crash` stayed empty for this action

Status:

- `verified working`

### 4. Import file fallback on a device without a document picker

Observed before the fix:

- tapping `Import file` on this ATD image could throw `ActivityNotFoundException`
- failure text in crash log:
  - `No Activity found to handle Intent { act=android.intent.action.OPEN_DOCUMENT ... }`

Observed after the fix and reinstall:

- tapping `Import file` no longer crashed the app
- the UI showed snackbar text:
  - `No file picker available on this device.`
- `adb logcat -d -b crash` remained empty for that action after reinstall

Interpretation:

- this ATD image does not provide a usable document picker
- Coupio now degrades safely on that device class instead of crashing
- actual picker-based image or PDF import is still untested on a device that exposes `OPEN_DOCUMENT`

Status:

- unsupported-device fallback: `verified working`
- real picker import flow: `untested`

### 5. Admin pending and locked actions

Observed in a later ATD pass on the same date:

- a seeded `PENDING` coupon exposed both `Issue now` and `Roll back`
- tapping `Roll back`:
  - showed snackbar `Coupon rolled back.`
  - returned the coupon to `AVAILABLE` in the app database
  - left `adb logcat -d -b crash` empty
- reseeding the same coupon to `PENDING` and tapping `Issue now`:
  - showed snackbar `Coupon issued.`
  - moved the coupon to `LOCKED` in the app database
  - updated status copy to the locked-until cooldown message
  - left `adb logcat -d -b crash` empty
- with the coupon in `LOCKED`, tapping `Unlock again`:
  - returned the badge to `Ready`
  - removed the unlock button from the card
  - restored ready-state copy
  - left `adb logcat -d -b crash` empty

Status:

- `verified working`

## Remaining gaps after this pass

- real picker-backed image import remains blocked on this workstation's available emulator images
- PDF import and share remain blocked for the same picker reason
- camera capture was shown to be platform-resolvable on a Google APIs image, but not completed end-to-end through the app UI in this ATD pass
