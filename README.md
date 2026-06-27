# Aura

Aura is a native Android app that records audio and uploads each recording to a
**Drive folder named "Aura"** in your own Google Drive. There is no hidden
behavior: the foreground notification, the Quick Settings tile, and the main
screen all say plainly when recording is active.

- Tap the orb on the main screen, or the **Aura** tile in Quick Settings, to
  start/stop recording.
- While recording, a persistent notification reads "Aura — Recording audio"
  and the main screen shows a live elapsed-time counter.
- Recordings are queued for upload and retried automatically (with backoff) if
  the network is unavailable, and on app launch as well.
- You can pause/resume a recording in progress, from the app, the
  notification, or automatically while a phone call is active.
- The **Recordings** screen lists local recordings with upload status, lets
  you play them back, and retry any that failed.
- Local storage for not-yet-uploaded recordings is capped; if uploads keep
  failing, the oldest pending recordings are dropped rather than filling the
  device.
- You authenticate with your own Google account; the app only requests the
  narrow `drive.file` scope, which lets it see/create files *it* uploads — not
  your existing Drive contents.

## Architecture

- Kotlin only, single `Activity` hosting Jetpack Compose (Material3, dynamic
  color on Android 12+).
- `ViewModel` → `UseCase` (domain) → `Repository` (data) → data source
  (`MediaRecorder`, Retrofit/OkHttp, `EncryptedSharedPreferences`).
- Hilt for dependency injection.
- `MediaRecorder` records Opus-in-OGG at ~64 kbps on Android 10+ (`AudioEncoder.OPUS`
  requires API 29); on Android 8.0–9.0 it falls back to AAC-in-M4A, since Opus/OGG
  output isn't available on those API levels.
- A foreground `Service` (`FOREGROUND_SERVICE_TYPE_MICROPHONE`) owns the
  recorder so recording can continue while the app is backgrounded.
- Upload goes straight to the Drive REST v3 API (multipart/related, not the
  Drive Android SDK) via Retrofit/OkHttp.
- A `WorkManager`-backed queue retries failed uploads with backoff
  15s → 30s → 1m → 5m → 15m, capped at 5 attempts.
- Tokens, the cached Drive folder ID, and similar secrets live in
  `EncryptedSharedPreferences` (AES256-GCM master key).

## 1. Create a Google Cloud project

1. Go to the [Google Cloud Console](https://console.cloud.google.com/) and
   create a new project (or pick an existing one).
2. In **APIs & Services → Library**, search for **Google Drive API** and
   click **Enable**.

## 2. Configure the OAuth consent screen

1. Go to **APIs & Services → OAuth consent screen**.
2. Choose **External** user type (unless you have a Google Workspace
   organization and want **Internal**).
3. Fill in the required app information (name, support email).
4. Under **Scopes**, add `.../auth/drive.file`.
5. Under **Test users**, add the Google account(s) you'll sign in with on the
   device. While the app is unpublished, only accounts listed here can sign
   in — this also avoids Google's "unverified app" warning screen for your
   own testing.

## 3. Create an OAuth client and download `google-services.json`

1. Go to **APIs & Services → Credentials → Create Credentials → OAuth client ID**.
2. Application type: **Android**.
3. Package name: `com.aura.app`.
4. SHA-1 certificate fingerprint: get yours with:
   ```
   keytool -list -v -keystore ~/.android/debug.keystore -alias androiddebugkey -storepass android -keypass android
   ```
   (use your release keystore's SHA-1 instead if you're building a release
   APK).
5. Go to [Firebase Console](https://console.firebase.google.com/), create/select
   a project pointing at the **same** Google Cloud project, add an Android app
   with package name `com.aura.app`, and download `google-services.json`.

   (Firebase is only used here as a convenient way to generate
   `google-services.json` for Google Sign-In's client-ID lookup — Aura does
   not use any other Firebase service.)
6. Place the downloaded file at `app/google-services.json`. This file is
   `.gitignore`d — never commit your real one.

## 4. Generate the Gradle wrapper jar

This repo includes `gradlew`, `gradlew.bat`, and
`gradle/wrapper/gradle-wrapper.properties` (pinned to Gradle 8.9), but not the
binary `gradle-wrapper.jar` (binaries shouldn't be committed by hand). Generate
it once, from a machine with normal internet access to `dl.google.com` /
`services.gradle.org`:

```
gradle wrapper --gradle-version 8.9
```

Run this from the project root — it'll populate
`gradle/wrapper/gradle-wrapper.jar` using the `gradle-wrapper.properties`
already in the repo. If you don't have a system-wide `gradle`, opening the
project in Android Studio and letting it sync will generate the same file
automatically.

## 5. Build and sideload

```
./gradlew assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

Or open the project in Android Studio (Iguana+) and run it on a device/emulator
directly — Studio will handle SDK/Gradle setup for you.

On first launch, Aura asks for the **Record Audio**, **Phone State**, and
**Notifications** permissions with a plain-language explanation of why each is
needed. Sign in with a Google account from the **Settings** screen before
recording, so uploads can succeed (recordings made while signed out are
queued and uploaded once you sign in).

## Permissions requested

| Permission | Why |
|---|---|
| `RECORD_AUDIO` | Capture audio from the microphone. |
| `READ_PHONE_STATE` | Detect incoming/active calls so recording can auto-pause and resume around them. Optional — if denied, recording just continues through calls. |
| `POST_NOTIFICATIONS` | Show the "Recording audio" foreground-service notification. |
| `FOREGROUND_SERVICE`, `FOREGROUND_SERVICE_MICROPHONE` | Keep recording running reliably while the app is backgrounded. |
| `INTERNET` | Upload recordings to Google Drive. |

## Note on this build environment

This project was assembled in a sandboxed environment whose network policy
blocks `dl.google.com` (Google's Maven repository), so a full `./gradlew
assembleDebug` could not be executed here to verify compilation end-to-end.
All Gradle/Kotlin/manifest/resource files were written and manually
cross-checked instead. Please run a full build locally (or in CI) before
relying on this as a finished artifact, and file/fix anything that surfaces.
