# TORUVA HUB

TORUVA HUB is a landscape-first Android launcher for organizing and opening games already installed on your device. It combines an immersive home screen with a searchable game library, per-game artwork, and configurable sound effects.

**Version 0.3.0 was functionally validated on a Xiaomi Redmi Note 13 Pro 4G.** The device checks covered cold startup, sound, returning from games, carousel navigation, artwork, and game selection.

## Features

- **Quick access:** A centered carousel prioritizes favorites and recently opened games. Play and Details follow the centered game.
- **Game library:** Android game metadata helps discover installed games. Add or remove entries manually, search by original or custom name, and manage favorites. Known emulators are excluded from automatic game detection.
- **Game details:** Open a game, rename it for display, and customize its cover and background. Fill, Fit, and Custom framing support pinch, drag, and accessible sliders.
- **Personalization:** Six visual themes, a particle animation on cold startup, independent startup and navigation sound toggles, and a 0–100% volume control.
- **Local storage:** Room stores the library, history, and framing. DataStore stores preferences. Selected artwork is copied into app-private storage.

## Technology and requirements

| Component | Requirement |
| --- | --- |
| Target device | Android 10 or later (API 29+) |
| Build SDK | Android SDK Platform 34 |
| Java | JDK 17, Eclipse Temurin for the configured Gradle daemon |
| Build tools | Gradle Wrapper 8.9 and Android Gradle Plugin 8.7.2 |
| App stack | Kotlin 2.1.21, Jetpack Compose, Hilt, Room, DataStore, and Coil |

Use Android Studio to install the SDK and Platform Tools. Set `ANDROID_HOME` to your SDK directory or create an untracked `local.properties` in the project root. On Windows, use forward slashes in the SDK path, for example `sdk.dir=C:/Android/Sdk`.

## Build and install

Clone the repository, then build the debug APK:

```powershell
git clone https://github.com/Ju4nmar/toruva-hub.git
cd toruva-hub
.\gradlew.bat assembleDebug
```

On macOS or Linux, run `./gradlew assembleDebug`. The APK is generated at `app/build/outputs/apk/debug/app-debug.apk`.

To install it on a connected device, enable USB debugging and authorize the computer:

```powershell
adb devices
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

TORUVA HUB retains the application ID `com.nexus.gamelauncher`. An in-place update preserves app data when Android accepts the existing and new APKs as the same signing identity.

## Tests

```powershell
.\gradlew.bat testDebugUnitTest
.\gradlew.bat connectedDebugAndroidTest
```

Run instrumented tests with an Android test device or emulator connected.

## Project notes

- [Changelog](CHANGELOG.md)
- [Audio assets and playback](AUDIO.md)
- [Startup and carousel performance](PERFORMANCE.md)
- [Backlog](BACKLOG.md)
