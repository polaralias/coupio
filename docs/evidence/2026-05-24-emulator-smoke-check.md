# Emulator Smoke Check - 2026-05-24

## Scope

Bounded emulator verification to confirm that this workstation can boot the local AVD, install the debug APK, and launch the app without an application crash being observed.

This is an evidence note, not a support claim for the full product workflow.

## Environment

- Host date: 2026-05-24
- AVD: `Medium_Phone_API_36`
- SDK root: `C:\Users\james.DESKTOP-Q8VOBFS\AppData\Local\Android\Sdk`
- APK installed: `app/build/outputs/apk/debug/app-debug.apk`

## Commands exercised

```powershell
./gradlew --gradle-user-home .gradle-codex --no-daemon --console plain assembleDebug
```

```powershell
C:\Users\james.DESKTOP-Q8VOBFS\AppData\Local\Android\Sdk\emulator\emulator.exe -avd "Medium_Phone_API_36" -no-window -no-snapshot-load -no-boot-anim -gpu swiftshader_indirect -no-audio
```

```powershell
C:\Users\james.DESKTOP-Q8VOBFS\AppData\Local\Android\Sdk\platform-tools\adb.exe install -r app\build\outputs\apk\debug\app-debug.apk
```

```powershell
C:\Users\james.DESKTOP-Q8VOBFS\AppData\Local\Android\Sdk\platform-tools\adb.exe shell am start -W -n com.polaralias.coupio/.MainActivity
```

## Observed results

- APK install returned `Success`.
- Activity launch returned `Status: ok` with `Activity: com.polaralias.coupio/.MainActivity`.
- `adb logcat -d -b crash` returned no application crash output before or after launch.
- Screenshot artifact:
  - `docs/evidence/2026-05-24-emulator-launch.png`

## Interpretation

- The repository has a usable emulator-based smoke path on this workstation.
- The app can be installed and launched on the local AVD.
- This is still `verified limited` rather than `verified working` for runtime behavior because the screenshot captured a `System UI isn't responding` overlay from the emulator environment while the app UI was visible behind it.
- Import, share, camera, admin, and background timing flows remain outside the scope of this evidence note.
