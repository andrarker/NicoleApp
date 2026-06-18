# NicoleApp

An Android chat application built with Kotlin and Jetpack Compose that integrates with the Gemini AI API.

## Tech Stack

- **Language**: Kotlin
- **UI**: Jetpack Compose + Material 3
- **Architecture**: MVVM
- **Database**: Room (local persistence)
- **Networking**: Retrofit + OkHttp + Moshi
- **AI**: Gemini API (via `GeminiService.kt`)
- **Image Loading**: Coil
- **Build**: Gradle 8.7 with Kotlin DSL

## Project Structure

- `app/src/main/java/com/example/` — main source code
  - `data/` — Room DB, DAOs, API services (Gemini/Retrofit)
  - `ui/` — Compose screens (ChatScreen), ViewModels, theme
  - `utils/` — helpers (CrashHandler)
  - `MainActivity.kt` — app entry point
- `gradle/libs.versions.toml` — centralized dependency versions
- `build.sh` — build script that sets up Android SDK and builds the debug APK

## Building

This is a native Android app. It cannot run in a browser — it produces an APK for installation on Android devices.

### Prerequisites

- Java (GraalVM 22.3 module installed)
- The `build.sh` script handles downloading the Android SDK automatically

### Build

Run the **Build APK** workflow, or:

```bash
bash build.sh
```

The debug APK will be output to:
```
app/build/outputs/apk/debug/app-debug.apk
```

## Environment Variables

Copy `.env.example` to `.env` and fill in your key:

```
GEMINI_API_KEY=your_key_here
```

Get a Gemini API key at: https://aistudio.google.com/

## Notes

- `local.properties` is generated automatically by `build.sh` with the correct Android SDK path
- `gradle/wrapper/gradle-wrapper.jar` is fetched by `build.sh` if missing (the GitHub repo stores a placeholder)
- Android SDK is installed to `/home/runner/android-sdk` on first build

## User Preferences
