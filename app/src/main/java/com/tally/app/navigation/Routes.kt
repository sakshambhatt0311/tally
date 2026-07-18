package com.tally.app.navigation

import kotlinx.serialization.Serializable

/**
 * Type-safe destinations for Navigation Compose 2.8+. Each object/class is serialized
 * by kotlinx.serialization instead of hand-built string paths, so arguments (like a
 * circle's id) are compile-time checked rather than pulled out of a Bundle by key.
 */
sealed interface TallyRoute {

    /** The Get Started fork: Continue Solo vs. Sign in to Sync. Always the start destination. */
    @Serializable
    data object Welcome : TallyRoute

    /** Landing screen after onboarding — every circle this device knows about. */
    @Serializable
    data object MyCircles : TallyRoute

    /** "New Circle" — the single circle-name field. */
    @Serializable
    data object CreateCircle : TallyRoute

    /** "Join a Circle" via share code — the one flow that requires connectivity up front. */
    @Serializable
    data object JoinCircle : TallyRoute

    /** Share-code screen shown right after an ONLINE circle is created; opt-in, its own step. */
    @Serializable
    data class CircleCreated(val circleId: String, val circleName: String) : TallyRoute

    /** Success screen for a LOCAL (device-only) circle — no share code, leads into adding players. */
    @Serializable
    data class LocalCircleCreated(val circleId: String, val circleName: String) : TallyRoute

    /**
     * Hosts the persistent Feed / Board / Games / Members tabs for one circle. [initialTabIndex]
     * selects the tab to open on entry (0=Feed … 3=Members); defaults to Feed for normal navigation.
     */
    @Serializable
    data class CircleDashboard(
        val circleId: String,
        val circleName: String,
        val initialTabIndex: Int = 0,
    ) : TallyRoute

    /** Full-screen searchable game library picker, reached from the Games tab. */
    @Serializable
    data class AddGame(val circleId: String) : TallyRoute

    /** Per-game detail — standings + record book, reached by tapping a card on the Games tab. */
    @Serializable
    data class GameDetail(val circleId: String, val circleName: String, val gameId: String) : TallyRoute

    /** The 3-step log-a-session wizard (Pick a Game / Who's Playing / Enter Results). */
    @Serializable
    data class LogSession(val circleId: String) : TallyRoute

    /** Rivalry comparison between exactly two members of a circle. */
    @Serializable
    data class HeadToHead(val circleId: String, val playerAId: String, val playerBId: String) : TallyRoute

    /** Account + circle management, reached from the gear icon on My Circles. */
    @Serializable
    data object Settings : TallyRoute

    /** Profile screen showing Google account info or local login. */
    @Serializable
    data object Profile : TallyRoute

    /** Flat list of every circle you're in, with per-membership Delete/Leave actions. */
    @Serializable
    data object ManageCircles : TallyRoute
}
