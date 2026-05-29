# SuvMusic — Architecture Review

> A plain-English explanation of the current architecture, its flaws, the concepts
> behind them, and what a robust version looks like.
> Written for the app author to *understand the issues*, not just patch them.

---

## 0. TL;DR (read this first)

Your app is **not a messy monolith** — it's a thoughtfully structured, multi-module
Kotlin/Compose app that is **partway through a modernization migration**. The technology
choices are good. The real problems are:

1. **Three "God classes"** that do too many jobs each (`YouTubeRepository` 3,471 lines,
   `SessionManager` 3,137 lines, `MusicPlayer` 2,953 lines).
2. **Half-finished migrations running in parallel** (Hilt **+** Koin, Room **+** SQLDelight,
   Retrofit **+** Ktor). Half-migrated is riskier than either end-state.
3. **"Swallow the error" handling** — failures return empty/null instead of being modeled,
   so the UI can't tell the user *why* something broke.

Everything else (DI, Compose, Media3, multi-module layering, domain UseCases) is already
in decent shape. **The single most important idea for you: finish the migrations you
start, and break up the God classes the surrounding architecture already supports.**

---

## 1. What the app currently HAS (the good foundation)

These are genuinely modern, correct choices:

| Area | What you have | Why it's good |
|------|---------------|---------------|
| **DI** | Hilt (migrating to Koin) | No scattered global singletons |
| **UI** | 100% Jetpack Compose + Material 3, adaptive layouts | Modern, foldable/tablet-ready |
| **Playback** | Media3 / ExoPlayer in a `MediaLibraryService` | Android Auto + Wear support, the right framework |
| **State** | StateFlow + Coroutines, `collectAsStateWithLifecycle`, `stateIn(WhileSubscribed)` | Reactive, lifecycle-aware |
| **Persistence** | Room (→ migrating to SQLDelight), DataStore, EncryptedSharedPreferences | Correct tools per job |
| **Structure** | Multi-module: `core:model`, `core:domain`, `core:data`, `core:db`, `core:ui` | Real separation of concerns |
| **Domain** | UseCases (`PlaybackUseCases`, `LibraryUseCases`, `SearchUseCases`) + repository interfaces | Clean Architecture skeleton already exists |
| **Native** | C++23 audio engine (spatial audio, limiter, EQ) via JNI | High-fidelity audio done properly |
| **Multiplatform** | Active KMP migration with a desktop target (`composeApp`) | Future-proofing |

**Reframe:** The *bricks* are fine. The issue is how a few *rooms* are laid out — and
that some renovation is half-done.

---

## 2. The flaws — explained as concepts

### 🔴 Flaw 1 — "God Objects" (the #1 real problem)

| File | Lines |
|------|-------|
| `app/.../data/repository/YouTubeRepository.kt` | **3,471** |
| `app/.../data/SessionManager.kt` | **3,137** |
| `app/.../player/MusicPlayer.kt` | **2,953** |
| `app/.../service/MusicPlayerService.kt` | **1,821** |

**The concept: Single Responsibility Principle (SRP)** — *a class should have only one
reason to change.*

Your `MusicPlayer` alone does ~8 unrelated jobs: it controls playback transport, resolves
stream URLs, handles retry logic, picks audio quality, writes listening history, computes
recommendations, drives Discord Rich Presence, **and** talks to 10+ repositories directly.

> **Analogy:** one employee being the chef, the waiter, the cashier, *and* the accountant.
> When the kitchen catches fire, you can't tell which job started it — and training a
> replacement is impossible because everything lives in one head.

**Why it hurts you concretely:**
- Every bug fix risks breaking an unrelated feature (this is why you have comments like
  `// BUG FIX (Skip)` and the `Silent Handshake` / `audioSinkKickstartDone` fade-in hack —
  patches stacked on patches).
- You can't unit-test "stream resolution" without spinning up the whole player.
- Two people can't safely work on the file at once.

### 🔴 Flaw 2 — Half-finished parallel migrations

You are currently running **two of everything**:

| Old (legacy) | New (target) | Bridge that exists |
|--------------|--------------|--------------------|
| Hilt | Koin | `HiltKoinBridge.kt` |
| Room | SQLDelight | both schemas maintained |
| Retrofit | Ktor | both clients in deps |
| Android-only | Kotlin Multiplatform | `composeApp` desktop target |

**The concept: a migration is a *temporary* state, not a *resting* state.**

A half-done migration is **riskier than either the start or the end**, because:
- You maintain **both** systems (double the surface area, double the bugs).
- New code has to decide which system to use, so conventions drift.
- A bridge layer (`HiltKoinBridge`) is pure overhead that exists only to glue two worlds.

This isn't a flaw in *direction* — the direction is good. It's a flaw in *momentum*. If a
migration stalls, you pay the cost forever without getting the benefit.

### 🔴 Flaw 3 — Tight coupling to concrete classes

`MusicPlayer` / `MusicPlayerService` inject the **concrete** `YouTubeRepository` (all 3,471
lines) directly — even though clean interfaces already exist in `core/domain/`.

**The concept: Dependency Inversion Principle** — *high-level code (playback) should depend
on a small abstraction, not on a giant concrete class.*

> **Analogy:** a lamp should plug into a standard wall socket, not be soldered directly to
> the power station. Right now your player is soldered to the power station — you can't
> swap, mock, or test it in isolation.

You already have the sockets (`PlaybackRepository`, `SearchRepository`, `LibraryRepository`
interfaces). The flaw is that the big classes don't consistently *use* them.

### 🔴 Flaw 4 — Business logic in the wrong layer (leaky layering)

- Stream-retry logic lives **inside** `MusicPlayer` (`resolveHybridRemoteStream`).
- Lyrics-provider selection lives in `PlayerViewModel` instead of a UseCase/service.
- Audio-focus handling is implemented **twice** (once in the player, once in the service).

**The concept: Separation of Concerns + layering.** The intended flow is:

```
Compose UI → ViewModel → UseCase (domain rules) → Repository (interface) → DataSource
```

Each rule should live in **exactly one layer, exactly once.** When the same rule lives in
two places (like audio focus), the two copies drift apart over time and create bugs that
only appear in one code path.

### 🔴 Flaw 5 — "Swallow and return empty" error handling

The pattern repeated across repositories:

```kotlin
try {
    val response = apiService.searchSongs(query)
    songs
} catch (e: Exception) {
    Log.e("RemoteAudio", "search() FAIL", e)
    emptyList()   // ← failure becomes "no results"
}
```

**The concept: make failure a *value*.**

Returning `emptyList()`/`null` throws away *why* it failed — no internet? bad parse?
logged out? expired cookie? The UI literally cannot distinguish "no songs found" from
"the network is down." The robust pattern is a `Result<T>` / sealed `AppError` type:

```kotlin
sealed interface AppError { object NoNetwork; object AuthExpired; data class Parse(...) }
suspend fun search(q: String): Result<List<Song>>   // success OR a typed failure
```

Now the UI can show *"You're offline"* vs *"Session expired — sign in again"* instead of an
empty screen.

### 🟡 Flaw 6 — Scattered config & cache state

Hardcoded API endpoints, device IDs, and cache keys live inside repositories; Android-Auto
pagination caches are spread across `MusicPlayerService`. This is a *symptom* of the above —
when there's no clear home for something, it lands wherever was convenient.

---

## 3. What ROBUST looks like (the target)

You're already aiming here — this is the shape to *finish* converging on:

```
        ┌──────────────┐
        │  Compose UI  │   draw pixels, emit events
        └──────┬───────┘
               │ observes StateFlow
        ┌──────▼───────┐
        │  ViewModel   │   hold screen state, orchestrate (NO business rules)
        └──────┬───────┘
               │ calls
        ┌──────▼───────┐
        │   UseCase    │   ONE business rule each (ResolveStream, ToggleLike…)
        └──────┬───────┘
               │ depends on interface
        ┌──────▼───────┐
        │ Repository   │   "get me data" — hides WHERE it comes from
        │ (interface)  │
        └──────┬───────┘
               │ Hilt/Koin binds the concrete impl
        ┌──────▼───────┐
        │  DataSource  │   YouTube / Remote / Local / DB
        └──────────────┘
```

**Concretely, the God-class fix:** split `MusicPlayer` into focused collaborators, each
< ~300 lines, each with one job:

- `PlaybackController` — transport only (play/pause/seek/next)
- `StreamResolver` — URL resolution + retry policy (behind an interface)
- `HistoryTracker` — listening history
- `RecommendationEngine` — queue/recommendations (you already have a `RecommendationEngine`!)
- `DiscordPresence` — Rich Presence

Same idea for `YouTubeRepository` → `YouTubeSearchDataSource` / `YouTubeLibraryDataSource` /
`YouTubeBrowseDataSource` behind one facade, and `SessionManager` → grouped settings
holders (audio settings, theme settings, cache settings).

---

## 4. Priority order (if/when you act)

Lowest risk → highest. **Never big-bang rewrite a working app.** Ship in slices, each with
identical behavior:

1. **Finish ONE migration.** Pick the most-progressed of Hilt→Koin / Room→SQLDelight /
   Retrofit→Ktor and drive it to 100%, then delete the legacy side + the bridge. This
   removes whole categories of duplicate maintenance.
2. **Add a `Result` / `AppError` type** and convert the noisiest repository methods to it.
   Surface real messages in the UI.
3. **Extract `StreamResolver` out of `MusicPlayer`** behind an interface; the player depends
   on the interface, Hilt/Koin binds the impl.
4. **Extract `HistoryTracker` and `RecommendationEngine`** wiring out of the player.
5. **Split `YouTubeRepository`** into datasource classes behind the existing `core/domain`
   interfaces.
6. **Pull remaining business rules into UseCases** (lyrics selection, retry policy, like/dislike).
7. **Cleanup:** unify the two audio-focus implementations; centralize endpoints/keys/caches.

Each step is independently shippable, reviewable, and testable.

---

## 5. The one thing to remember

> **Your architecture isn't broken — it's mid-renovation.**
> The danger isn't the design (it's good); it's *leaving renovations half-done* and
> *letting a few classes keep absorbing every new responsibility.*
> Finish the migrations. Break up the God classes. Model your errors. That's the whole game.

---

*Generated as an architecture review. No code was changed.*
