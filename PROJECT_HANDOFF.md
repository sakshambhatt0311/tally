# Tally — Project Handoff

> Onboarding document for the next assistant. Read fully before writing code. Reflects the current state of the codebase. Package root: `com.tally.app`.

---

## 1. Project Overview & Philosophy

**Tally** is an offline-first Android app for tracking game-night scores, sessions, and leaderboards across **Circles** (groups of players). Users create circles, add members, track games from a catalog, log play sessions, and view a live Feed, a Board (leaderboard), per-game stats, and Head-to-Head rivalries.

**Philosophy:**
- **Automated** — the user logs a session; everything else (Feed flavor text, leaderboard standings, win%, records, head-to-head) is *derived* from that session history. There is exactly one source of truth (the session rows); no stat is ever stored twice.
- **Smooth** — no jank, no flicker. Loading states crossfade in; tab switches are instant; the theme never flashes on cold boot.
- **Dynamic** — flavor text, scoring units, and per-game rules adapt to the specific game being played.
- **Tactile** — a chunky, flat, high-contrast Material 3 design language (big rounded cards, zero shadows, large tap targets).

**Build note:** there is no build/emulator in the dev shell. Code is verified by careful review; always tell the user to "sync/rebuild in Android Studio" to confirm.

---

## 2. Tech Stack & Architecture

**Stack**
- Language: Kotlin 2.0.20 (KSP `2.0.20-1.0.25`)
- UI: Jetpack Compose, Material 3 (`androidx.compose.material3`), `material-icons-extended`
- Navigation: Navigation-Compose 2.8 type-safe routes (`@Serializable` route objects)
- Persistence: Room 2.6.1 (KSP compiler); Preferences DataStore 1.1.1
- DI: Dagger-Hilt 2.52 (KSP) + `androidx.hilt:hilt-navigation-compose` 1.2.0
- Async: Coroutines + Flow / StateFlow / SharedFlow
- Serialization: `kotlinx.serialization` (Room JSON converters + nav routes)
- Splash: `androidx.core:core-splashscreen` 1.0.1
- minSdk 26, target/compileSdk 34. Version catalog: `gradle/libs.versions.toml`.

**Architecture — MVVM + Clean + Unidirectional Data Flow (UDF)**

Layers:
- `data/` — domain models (`MockData.kt`, `Analytics.kt`, `SessionSummary.kt`, `LeaderboardEntry.kt`), game catalog (`MockGameData.kt` = `GameTemplate` library, `GameCatalog.kt` = injectable wrapper), `FeedMessageGenerator.kt`, `data/local/` (Room), `data/mapper/Mappers.kt`, `data/repository/` (impls + `KeyedFlowCache`), `data/analytics/SessionAggregator.kt`, `data/preferences/` (`ThemeConfig`, `UserPreferencesRepository`, `AppThemeController`).
- `domain/repository/` — repository **interfaces** (`CircleRepository`, `PlayerRepository`, `GameRepository`, `SessionRepository`).
- `di/` — `DatabaseModule`, `RepositoryModule` (`@Binds`), `CoroutineModule` (+ `@ApplicationScope` qualifier), `DataStoreModule`.
- `ui/viewmodel/`, `ui/screens/`, `ui/dashboard/`, `ui/session/`, `ui/components/`, `ui/theme/`.
- `navigation/` — `Routes.kt`, `TallyApp.kt`.

**Data flow:** Room DAO (`Flow`) → RepositoryImpl (`.map { it.toDomain() }`, cached) → ViewModel (`.stateIn(...)`) → stateless Compose screen (`collectAsStateWithLifecycle()`) → user action → ViewModel function → Repository (`suspend`) → Room. **No manual StateFlow mutation of list state** — Room emissions redraw the UI reactively.

### Repositories as single source of truth + global cache

All read queries flow through a small helper, `KeyedFlowCache`, that keeps each per-circle query **hot** even after the UI/ViewModel is destroyed:

```kotlin
internal class KeyedFlowCache<T>(scope: CoroutineScope, source: (String) -> Flow<T>) {
    private val cache = ConcurrentHashMap<String, Flow<T>>()
    operator fun get(key: String): Flow<T> = cache.getOrPut(key) {
        source(key)
            .flowOn(Dispatchers.Default)                                   // heavy mapping off main
            .shareIn(scope, SharingStarted.WhileSubscribed(10_000), replay = 1)
    }
}
```

- `replay = 1` → the latest DB list stays in memory; a new ViewModel re-collects and replays it **instantly** (0 ms, no null/empty flash).
- `WhileSubscribed(10_000)` → stays hot for 10s after the last collector leaves, then stops the query so idle circles don't hold live cursors forever. Bounded by circle count — not a leak.
- `scope` is the injected **`@ApplicationScope CoroutineScope`** (`SupervisorJob() + Dispatchers.IO`, from `CoroutineModule`).
- Applied in every `@Singleton` repo: sessions/feed, games (counted), players, circles (`CircleRepositoryImpl` uses a single un-keyed `shareIn` for the My Circles list + `KeyedFlowCache` for per-circle detail).

**ViewModel layer:**
- Tab ViewModels expose state as `.stateIn(viewModelScope, SharingStarted.Lazily, null)`. `Lazily` keeps the flow warm for the whole Circle session (no re-query on tab return).
- Heavy aggregation (`combine` in Leaderboard/Head-to-Head/Game Detail, `FeedMessageGenerator` mapping) runs with `.flowOn(Dispatchers.Default)` so list rendering never stutters.
- Writes run on `Dispatchers.IO`. **Exception:** writes immediately followed by a nav-pop (`createCircle`, `saveSession`) run on the injected `@ApplicationScope` scope so the coroutine survives the ViewModel being cleared.

**ViewModel scoping (anti-lag):** The four Circle tabs (Feed / Board / Games / Members) are **not** separate nav destinations — they are index-switched inside one `CircleDashboardScreen` using `rememberSaveableStateHolder` + `SaveableStateProvider` (preserves per-tab scroll). Their ViewModels are hoisted at the top of `CircleDashboardScreen` via `hiltViewModel()`, which scopes them to the `CircleDashboard` `NavBackStackEntry` (the parent route). One instance per Circle session survives every tab switch.

### Theme persistence + OS sync

- **`ThemeConfig`** enum: `FOLLOW_SYSTEM`, `LIGHT`, `DARK`.
- **`UserPreferencesRepository`** wraps a Preferences `DataStore` (key `theme_config`), exposes `themeConfig: Flow<ThemeConfig>` (defaults `FOLLOW_SYSTEM`, `catch(IOException)` → `emptyPreferences()`), and `suspend updateThemeConfig(config)`.
- **`DataStoreModule`** provides the `DataStore<Preferences>` singleton (top-level `Context.preferencesDataStore` delegate).
- **`SettingsViewModel`** (`@HiltViewModel`) — `setDarkMode(enabled)` writes the config **and** calls the framework sync.
- **`AppThemeController`** — pushes the choice to the OS via `UiModeManager.setApplicationNightMode(MODE_NIGHT_YES/NO/AUTO)` on **API 31+** (guarded). The OS caches this per-app, so a saved Light preference no longer draws the system's Dark splash on the next cold boot. Pre-31 is a graceful no-op (we deliberately don't pull in AppCompat; the held splash frame + Compose theme prevent any in-app flash there).
- **`MainViewModel`** exposes `MainActivityUiState` (`Loading` → `Success(theme)`) via `stateIn(..., SharingStarted.Eagerly, Loading)`. **Eagerly is required** — the splash's `setKeepOnScreenCondition` reads `.value` synchronously, which is not a Flow subscriber, so `WhileSubscribed` would hang the splash forever.

---

## 3. UI/UX Design System (CRITICAL — do not violate)

**Typography:** Plus Jakarta Sans everywhere. `PlusJakartaSans` in `ui/theme/Type.kt`; bind `fontFamily = PlusJakartaSans` on text/labels/inputs. Never hardcode Roboto.

**Elevation:** `0.dp` on ALL Cards / Surfaces / FABs / Sheets. Depth comes from **color contrast, never shadow.** FABs use `FloatingActionButtonDefaults.elevation(0.dp, 0.dp, 0.dp, 0.dp)`.

**Shapes (chunky & rounded):** cards/sheets/fields `RoundedCornerShape(24.dp)` (or `16.dp` for smaller elements). Pills/FABs `CircleShape` / `RoundedCornerShape(50)`.

**Tap targets:** buttons ~52–56.dp min height, `fillMaxWidth` for primary actions; selectable list rows `defaultMinSize(minHeight = 72.dp)`. Reusable `TallyPrimaryButton` / `TallySecondaryButton` in `ui/components/Button.kt`.

**Color — Material 3 tokens only (recurring bug class):** In screens, use **only** `MaterialTheme.colorScheme.*`. Never hardcode `Tally*` color tokens in screens — they render invisible in dark mode.
- Primary text `onSurface`; muted `onSurfaceVariant`; borders `outline`/`outlineVariant`.
- Selected / toggle states → `secondaryContainer` / `primaryContainer`.
- Destructive actions → `error` / `errorContainer`.
- The **"Head-to-Head"** FAB and the **"Edit Sessions"** FAB both use `secondaryContainer` / `onSecondaryContainer` (they must match).
- Icons on colored badges/medals = `Color.White`.

**Theme system:** `TallyTheme(darkTheme)` in `ui/theme/Theme.kt` (flat palettes — all `surfaceContainer*` collapse to one surface tone in dark). `MainActivity` runs a root-level `Crossfade` (200ms) over `isDark` so light↔dark switches are a hardware-accelerated fade of the whole tree, not per-token recomposition. `isDark` is resolved from the persisted `ThemeConfig` (`DARK`→true, `LIGHT`→false, `FOLLOW_SYSTEM`/null→`isSystemInDarkTheme()`).

**Splash Screen (Core Splashscreen API):**
- Theme `Theme.Tally.Splash` (parent `Theme.SplashScreen`): `windowSplashScreenBackground = @color/tally_window_background` (day/night-qualified color), `windowSplashScreenAnimatedIcon = @drawable/ic_launcher_foreground`, `postSplashScreenTheme = @style/Theme.Tally`. `MainActivity` sets this theme in the manifest.
- `MainActivity`: `installSplashScreen()` **before** `super.onCreate`; `setKeepOnScreenCondition { mainViewModel.uiState.value is Loading }`; `setOnExitAnimationListener { ... }` runs a **gentle cross-fade** — icon `alpha 1→0` + subtle `scale 1→0.95`, whole splash view `alpha 1→0`, `AccelerateDecelerateInterpolator`, **280ms**, `doOnEnd { splashProvider.remove() }`. (An earlier X/Instagram zoom was deliberately toned down to this soft fade.)

**Colorful game logos (not monochrome):**
- Game catalog glyphs are Material vector icons (monochrome). "Full color" for them = each game's `template.brandColor` (used in the Games tab card, the Log Session picker, and the Add-a-Game list). Note: `tint = Color.Unspecified` would leave these *uncolored* — for the brand-color look use `tint = template.brandColor`.
- The Welcome-screen logo uses the real drawable: `Image(painterResource(R.drawable.ic_launcher_foreground), contentScale = ContentScale.Fit, modifier = Modifier.size(120.dp))` — `Image` (not `Icon`) so its baked-in green/gold colors aren't flattened to a single tint.

**Loading / empty / content (STRICT):** List-backing state is `StateFlow<List<T>?>` initialized to `null`. Three-way, now wrapped in a `Crossfade` over a stable `LoadPhase` enum:
- `null` → **Loading** (subtle centered `CircularProgressIndicator`, `color = primary`). Never the empty message.
- non-null `&& isEmpty()` → styled **Empty** state.
- else → **Content** list.
`Crossfade(targetState = loadPhase(list), animationSpec = tween(300))` fades load→data over 300ms instead of snapping. The phase enum (not the list) is the target, so it does **not** re-fade on every data change.

---

## 4. Domain Models & Core Logic

### Game Catalog
- `GameTemplate` (in `MockGameData.kt`): `name` (unique — `val id get() = name`), `icon: ImageVector`, `brandColor: Color`, metadata strings, `scoringType: ScoringType`, `startsAdded: Boolean`, and **`scoringUnit: String = "points"`**.
- `MockGameData.allGamesCategorized` = the categorized library; `templateById(id)` resolves one. `GameCatalog` is the injectable `@Singleton` wrapper.
- `TrackedGame` (domain) = `(templateId, displayName, scoringType, sessionCount, scoringUnit)`. Stored as `GameEntity`. `scoringUnit` is **not** persisted — the mapper resolves it from the catalog on read (`MockGameData.templateById(templateId)?.scoringUnit ?: "points"`).

### ScoringType
`enum class ScoringType { PLACEMENT, POINTS, WIN_LOSS }`. Note: **`WIN_LOSS` is what the task-history calls "WINNER"** — the "Who won?" step.
- **PLACEMENT** — drag-to-rank finishing order (e.g. Mario Kart, Ludo, UNO (Placements), Monopoly, Mario Party). Stored as a JSON `List<String>` of names in order.
- **POINTS** — a number per player (FIFA, Poker Cash, racket sports, Two Truths). Stored as JSON `Map<String,String>` (name→score string).
- **WIN_LOSS** — the "Who won?" multi-select. Stored as a JSON `List<String>` of winner names.

### Custom scoring units
`scoringUnit` gives games their own noun for a score: **FIFA = "goals"**, most others default `"points"` (ready for "runs", "laps", etc.). `TrackedGame.scoringSummary()` returns the unit for POINTS games, else the type label ("placement" / "win/loss") — used in the Games card and Game Detail hero.

### Session results (wizard state)
`SessionResults` sealed interface (in `ui/session/LogSessionState.kt`):
- `None`
- `Placement(orderedPlayers: List<RosterMember>)`
- `Points(pointsByPlayer: Map<String, String>)`
- `WinLoss(winners: Set<String>)` — **a Set**, supporting multiple winners.

`LogSessionViewModel.encodeResults`: Placement → JSON name list; Points → JSON name→score map; WinLoss → JSON list of winner names.

### Standard Competition Ranking, ties & team wins — `SessionAggregator`

`SessionAggregator` (object, `data/analytics/`) is the **single source of truth** for turning session rows into stats (used by Leaderboard, Game Detail, Head-to-Head, and the Feed). Winning is a **`Set<String>`** — a session can have several winners:

```kotlin
fun winnersOf(scoring, resultsJson): Set<String> = when (scoring) {
    PLACEMENT -> setOfNotNull(order.firstOrNull())                 // 1st place
    POINTS    -> points.filterValues { it == maxScore }.keys       // ALL top scorers = joint 1st (SCR)
    WIN_LOSS  -> decode<List<String>>().toSet()                    // every selected winner
    null      -> emptySet()
}
```

- **Ties (points):** if two players share the top score, `winnersOf` returns **both** → both are Placement 1 → `leaderboard` credits `wins` to each (`wins = outcomes.count { name in it.winners }`). This is Standard Competition Ranking (joint 1st).
- **Team / co-op wins ("Who won?"):** the Step 3 grid is **multi-select** (toggle) — tap several players, all become winners.
- `leaderboard(...)` sorts by wins desc, then win% desc, then games desc; drops players with 0 games. `pointsOf` and `placementOrder` support scores and pairwise (Head-to-Head) comparisons.

### Game-specific rules (matched by `templateId` in `LogSessionState`)
- **Chess** (`isChess`) — strictly **2 players** (Step 2 gate + helper text "Chess requires exactly 2 players"). Step 3 uses a custom `ChessResults` composable with three exclusive chunky cards: **Player A**, **Player B**, **Draw / Stalemate**. A → winners `{A}` (A=1st, B=2nd); B → `{B}`; Draw → `{A, B}` (both Placement 1, via the tie logic). Uses `setWinners(...)` (replace), not toggle.
- **Racket sports** (`isRacketSport` = Table Tennis / Badminton / Tennis) — **2 to 4 players** (Step 2 gate + hint "This game requires 2 to 4 players").
- **Billiards** (`isBilliards`) — Step 3 win/loss is **single-select** (radio: tapping replaces the winner). **Pool** and all other WIN_LOSS games stay **multi-select** (toggle) for team wins.
- Validation lives in `LogSessionState.canAdvance` (Step 2 player-count gate) and `EnterResultsStep` (Step 3 branch selection). `playerRequirementHint` supplies the Step-2 text. Keep this logic in the state/ViewModel layer, not the composables.

**Catalog history (applied over the project):** Mario Party `POINTS → PLACEMENT`; CoD/Halo `POINTS → WIN_LOSS`; Catan `POINTS → WIN_LOSS`; **UNO split** into "UNO (Single Winner)" (WIN_LOSS) + "UNO (Placements)" (PLACEMENT); **deleted** Ticket to Ride, Basketball, Spikeball; **Pool/Billiards split** into "Pool" (WIN_LOSS, multi) + "Billiards" (WIN_LOSS, single); Codenames & Mafia are WIN_LOSS; FIFA `scoringUnit = "goals"`.

---

## 5. Utilities & Features

### FeedMessageGenerator
`object FeedMessageGenerator` (`data/`) turns a session into fun, game-specific flavor text for the Feed. `Map<String, List<String>>` keyed by templateId (= game name): **FIFA has 30 lines, every other game 15.**

Placeholders:
- `[W]` — winner(s), grammar-formatted (Oxford comma): "Alice" / "Alice and Bob" / "Alice, Bob, and Charlie".
- `[L]` — loser(s) (placement > 1), same grammar.
- `[S]` — **margin of victory**, NOT the raw total: `winnerScore − highestLoserScore`.

```kotlin
fun generate(templateId, winners, losers, scores: Map<String,Int>, seed: Int): String
// margin = winners.maxOf(scores) − losers.maxOf(scores)   (null when no scores or no losers)
```

Robustness / fallbacks (all `ifEmpty`-guarded, crash-proof):
- **No losers** (co-op / everyone tied 1st) → filter out `[L]` lines; if none remain, "A and B shared the victory!".
- **No margin** (placement games, no scores, no losers) → filter out `[S]` lines.
- **Chess draw** (both Placement 1) → select only from lines tagged `**[DRAW]**`; strip the tag before returning.
- **Stable per card:** the random pick is seeded by `session.id.hashCode()`, so a card always shows the same line across DB re-emissions (no reshuffle).

Integration: called inside `Mappers.toSummary()` (runs on `Dispatchers.Default`), producing `SessionSummary.recap`, which the Feed card renders (bodyLarge quote block).

### Edit Mode — multi-select delete (Feed)
`FeedViewModel` holds `isEditMode: StateFlow<Boolean>` and `selectedSessionIds: StateFlow<Set<String>>`, plus `toggleEditMode()`, `exitEditMode()` (clears selection), `toggleSelection(id)`, and `deleteSelected()` (→ `sessionRepository.deleteSessions(ids)` then exits). Bulk delete DAO: `@Query("DELETE FROM sessions WHERE id IN (:sessionIds)")`.

UI: the **same** `FeedViewModel` instance is shared between `CircleDashboardScreen`'s FAB slot and `FeedTab` (same nav entry). Normal mode shows **"Edit Sessions"** (secondaryContainer) directly above **"Log Session"**. Edit mode swaps the FABs for a chunky **"Delete (X)"** (errorContainer) + **"Cancel"**. In edit mode, tapping a Feed card toggles selection; a selected card shifts to `primaryContainer` with a 2.dp `primary` border. System back exits edit mode.

---

## 6. Room Schema, Ids & Gotchas

**Entities** (`data/local/entity/`), String UUID primary keys, FK by `circleId`, `onDelete = CASCADE`:
- `CircleEntity` — `id`, `name`, `memberCount` (legacy/static — real count is computed live), `activityLabel`, `membershipType` (OWNER/LINKED/LOCAL), `isDeviceOnly`.
- `PlayerEntity` — `id`, `circleId` (FK CASCADE, indexed), `name`, `initial`, `colorKey`, `membershipType`.
- `GameEntity` — `id`, `circleId` (FK CASCADE, indexed), `templateId`, `displayName`, `scoringType`, `sessionCount` (legacy — count is computed live).
- `SessionEntity` — `id`, `circleId` (FK CASCADE), `gameId` (FK → games, CASCADE), both indexed, `playedAt: Long`, `playerIds: List<String>` (JSON), `resultsJson: String`.

**Relations:** `GameWithSessions`; `CircleWithMemberCount` (correlated count aliased `memberCountDynamic`); `GameWithSessionCount` (correlated count aliased `dynamicSessionCount` — used so the Games list shows live counts); `SessionWithGame` (`@Embedded` session + nullable `@Relation` game — the Feed source).

**Deterministic ids:** player = `"$circleId:$name"`, game row id = `"$circleId:$templateId"`, session id = random UUID. Names/scores in `resultsJson`/`playerIds` are stripped back to display names via `removePrefix("$circleId:")`.

**TypeConverters:** enums ↔ `name`, Compose `Color` ↔ `Long`, `Instant` ↔ epoch millis, `List<String>` / `Map<String,Int>` ↔ kotlinx JSON.

**Critical DAO conflict strategies (learned the hard way):**
- `GameDao.insert` is `OnConflictStrategy.REPLACE`, but `upsertGame` in the repo uses **`insertIfAbsent` (IGNORE) + `update`** — NOT `REPLACE`. A plain `REPLACE` does `DELETE`+`INSERT`, and the games→sessions FK CASCADE would **wipe every logged session** for that game (the old "second session overwrites the first" bug). Never re-introduce `REPLACE` on the game upsert path.
- `SessionDao.insert` is `OnConflictStrategy.ABORT` — sessions are append-only history; a new row must never overwrite an existing one. Each session gets a fresh UUID at instantiation.

**Gotchas / debt:**
- **`.fallbackToDestructiveMigration()` is DEV-ONLY** (`DatabaseModule`, DB version 3). It silently drops all data on any version bump. **Must** be replaced with real `Migration`s before shipping real user data.
- `SavedStateHandle["circleId"] / ["gameId"]` in ViewModels rely on the type-safe nav route arg names. `checkNotNull` crashes loudly if a key name is wrong.
- The old `GameId` enum still lingers only in `ui/components/Branding.kt` and the orphan `ui/screens/GamePlacementScreen.kt` (superseded, not in the nav graph, safe to delete). Do **not** reintroduce it for game tracking.

---

## 7. Navigation

`TallyRoute` sealed interface (`navigation/Routes.kt`), wired in `navigation/TallyApp.kt`:
`Welcome`, `MyCircles`, `CreateCircle`, `JoinCircle`, `CircleCreated(circleId, circleName)`, `LocalCircleCreated(circleId, circleName)`, `CircleDashboard(circleId, circleName, initialTabIndex = 0)`, `AddGame(circleId)`, `GameDetail(circleId, circleName, gameId)`, `LogSession(circleId)`, `HeadToHead(circleId, playerAId, playerBId)`, `Settings`, `ManageCircles`.

- `CircleDashboard.initialTabIndex` opens a specific tab (Members = **3**). Both "Add players" CTAs (`LocalCircleCreatedScreen`, `CircleReadyScreen`) navigate with `initialTabIndex = 3`.
- `New Circle` branches: online → `CircleCreated` (share code); local → `LocalCircleCreated`. `createCircle` also seeds the creator as an OWNER member ("You").
- Nav transitions are instant (`EnterTransition.None`) so a tap right after a pop is never swallowed by an in-flight animation.
- Circle creation writes on `@ApplicationScope` (survives the immediate pop of the Create screen).

---

## 8. Current State & Known Behaviors

**Complete:** the full local Room wiring — Circles, Members, Games, Sessions/Feed, Board/Leaderboard, Head-to-Head, per-game stats — all reactive, all derived from the session history via `SessionAggregator`. Plus: dynamic per-game scoring units, ties/multi-winner logic, per-game player/winner rules (Chess, racket sports, Pool/Billiards), the `FeedMessageGenerator` (margin-of-victory flavor text), Feed multi-select delete, DataStore theme persistence + OS `UiModeManager` sync, and the Core Splashscreen with a soft cross-fade exit.

**Latest optimizations:**
- **Global repo cache** — `KeyedFlowCache` (`shareIn`, `replay=1`, `WhileSubscribed(10_000)`) keeps queries hot across ViewModel/nav teardown → instant re-entry into a Circle (the Back-button fix).
- **Parent-route ViewModel scoping** — tab VMs hoisted in `CircleDashboardScreen`, scoped to the `CircleDashboard` nav entry (one instance per Circle, survives tab switches).
- **`SharingStarted.Lazily` + `flowOn(Dispatchers.Default)`** on tab VMs — flows stay warm; aggregation/text-gen off the main thread.
- **`Crossfade` (300ms) over a `LoadPhase` enum** on every tab — smooths the load→data transition; the phase key means it doesn't re-fade on data changes.

**Verification workflow:** no build in the dev shell → verify by review, then ask the user to sync/rebuild in Android Studio and physically test (log sessions across scoring types, check Feed/Board/Head-to-Head consistency, tab-switch smoothness, cold-boot splash theme).

**Next phase (not started):** Firebase Auth → Firestore sync (likely via WorkManager) for the "online" circles. Before any real-data release: replace `.fallbackToDestructiveMigration()` with real Room migrations.
