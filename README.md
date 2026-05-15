# Vidyarthi Bus

Vidyarthi Bus is a lightweight, crowdsourced Android application designed for college students to track the real-time crowd status of BMTC buses. 

By empowering students to report live crowd levels on their buses, the community can help one another make informed travel decisions, avoiding missed lectures and transport-related stress.

## Features
- **Real-Time Crowd Reporting:** Students on board can report crowd levels (Empty, Seated, Full) with just a few taps.
- **Live Route Status:** Waiting students can instantly see the latest crowd reports for incoming buses.
- **Offline Fallback & Local Persistence:** Reports and route data gracefully handle poor network conditions using SharedPreferences for local caching.
- **Geofenced Reporting:** (In Development) Validates that a user is near a bus route before allowing a crowd report.

## Tech Stack
- **Language:** Kotlin
- **Architecture:** MVVM (Model-View-ViewModel) + Repository Pattern
- **UI:** Android Views (XML), Material Design
- **Backend:** Firebase (Authentication, Realtime Database, Cloud Messaging)
- **Minimum SDK:** 24 (Android 7.0)
- **Target SDK:** 35 (Android 15)

## Getting Started

1. **Clone the repository:**
   ```bash
   git clone https://github.com/UnknownArk/VidyarthiBus.git
   ```
2. **Open in Android Studio:**
   Select `Open` in Android Studio and navigate to the cloned project directory.
3. **Connect Firebase:**
   Ensure `google-services.json` is correctly set up in the `app` directory to enable Firebase functionality (Auth & Database).
4. **Build and Run:**
   Allow Gradle to sync, then run the app on an Android emulator or a physical device.

## Contributing
Contributions are welcome! Please feel free to submit a Pull Request.
