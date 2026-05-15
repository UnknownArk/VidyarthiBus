# Vidyarthi Bus Android Project Notes

This folder now contains an Android Studio project scaffolded from the existing SOP in `README.md`.

## What Is Built

- Native Android app in Kotlin.
- Minimum SDK 26.
- Route selector with search.
- Live dashboard for the selected bus route.
- Crowd reporting dialog.
- Analytics screen using local prototype data.
- Firebase Auth signs users in anonymously and Firebase Realtime Database stores live routes/reports. Local data remains as fallback.

## How To Open

1. Open Android Studio.
2. Choose `Open`.
3. Select this folder: `C:\Users\krish\AndroidProjects\Vidyarthi-Bus Project`.
4. Let Android Studio sync Gradle.
5. If prompted, install the requested Android SDK / build tools.
6. Run the `app` configuration on an emulator or Android phone.

## Current Status

Solved in the local prototype:

- Reports persist locally on the device with SharedPreferences.
- Location permission and sample geofence validation run before report submission.
- Basic unit tests cover report persistence and geofence validation.

Still needs external setup/data:

- AI prediction is mocked from sample data.
- Route geofence coordinates are sample values and should be replaced with real college route data.

## Suggested Improvements Before Production

- Fix the README text encoding. Several symbols are currently garbled.
- Add route, stop, and college-specific real data.
- Add a proper launcher icon and Play Store assets.
- Consider moving to Jetpack Compose or AndroidX Material 3 once dependencies can be synced reliably.




