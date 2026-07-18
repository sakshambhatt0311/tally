package com.tally.app

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

/**
 * Application entry point for Hilt's dependency graph. `@HiltAndroidApp` triggers code generation
 * for the app-wide component that every `@AndroidEntryPoint` (activities, etc.) and `@Module`
 * hangs off of, and injects this class's own `@Inject` fields.
 *
 * Named `TallyApplication` — not `TallyApp` — to avoid colliding with the existing
 * `com.tally.app.navigation.TallyApp` root composable.
 *
 * Mock-data seeding removed for QA: a fresh install now starts with an empty database, so all
 * data is built from scratch through the UI.
 */
@HiltAndroidApp
class TallyApplication : Application()
