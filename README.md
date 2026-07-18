# Tally

Tally is a modern, beautifully designed Android application for tracking board game sessions, managing player groups ("Circles"), and analyzing deep competitive statistics. Built fully natively with Kotlin and Jetpack Compose, Tally leverages Clean Architecture principles to deliver a seamless experience both offline and online.

## Features

- **Circles**: Organize players into groups. Support for both offline (device-only) circles and online, cloud-synced circles.
- **Session Tracking**: Log games, scores, and winners. Supports games where lower scores are better (e.g., Golf, Wordle) and standard high-score games.
- **Leaderboards**: Dynamic, animated leaderboards showing win rates, games played, and rankings.
- **Head-to-Head Analytics**: Compare any two players to see their lifetime rivalry, win/loss records, and recent matchups.
- **Record Book**: Detailed per-game superlative tracking (e.g., Most Wins, Highest Score).
- **Live Feed**: A social-style feed automatically generating human-readable summaries of logged sessions based on the game's scoring rules.

## Architecture & Tech Stack

Tally is built using modern Android development best practices:

- **UI**: Jetpack Compose (Material 3 design system, flat UI, tactile components)
- **Architecture**: MVVM (Model-View-ViewModel) + Clean Architecture
- **Dependency Injection**: Hilt
- **Local Storage**: Room Database (SQLite)
- **Cloud/Backend**: Firebase Firestore (for Online Circles) & Google Authentication
- **Asynchronous Programming**: Kotlin Coroutines & Flow

## Setup Instructions

To run Tally locally:

1. Clone this repository.
2. Open the project in Android Studio.
3. To enable Online Circles and Google Sign-In, you will need to connect the app to your own Firebase project:
   - Create a project in the [Firebase Console](https://console.firebase.google.com/).
   - Add an Android app with the package name `com.tally.app`.
   - Download the `google-services.json` file and place it in the `app/` directory.
   - Enable Authentication (Google Sign-In) and Firestore Database.
4. Sync Gradle and run the app on an emulator or physical device.
