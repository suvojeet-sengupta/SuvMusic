# SuvMusic — How the System Works & Its System-Level Flaws

> This is **not** about code structure (classes, line counts, layering).
> This is about **how the app works as a live system at runtime** — where data comes
> from, how a song actually plays, what it depends on, and where the whole *system*
> is fragile regardless of how clean the code is.

---

## Part 1 — How the system works right now (the data & control flow)

### The big picture

SuvMusic is a **client-only app with no backend of its own.** Everything it shows and
plays comes from **external services it does not control**, resolved live on each device:

```
  ┌─────────────────────────────────────────────────────────────┐
  │                     YOUR PHONE (the only "server")           │
  │                                                              │
  │   UI ── asks for ──► metadata ──► YouTube Music PRIVATE API  │──► YouTube
  │                                   (NewPipe + OkHttp)         │
  │                                                              │
  │   Tap play ──► resolve stream URL at runtime ────────────────│──► YouTube CDN
  │                  │ fallback ►  RemoteAudio API (DES-encrypted)│──► 3rd-party server
  │                  ▼                                            │
  │              ExoPlayer ── merges audio+video streams ──► 🔊   │
  │                                                              │
  │   Lyrics ──► KuGou / LRCLIB / SimpMusic (try each in turn) ──│──► 3rd-party servers
  │   Listen Together ──► WebSocket ────────────────────────────│──► sync server
  │   Scrobble ──► Last.fm   │   Presence ──► Discord            │──► 3rd-party servers
  │                                                              │
  │   Local state: Room/SQLDelight DB + DataStore + in-mem cache │
  └─────────────────────────────────────────────────────────────┘
```

### What happens when you tap a song (step by step)

1. **Discovery / metadata** — the app calls **YouTube Music's *private, undocumented*
   internal API** (via the NewPipe extractor + custom OkHttp calls) to get search results,
   home feed, playlists, artist pages.
2. **Stream resolution (at play time)** — when you press play, the app resolves a **playable
   stream URL live, on the spot.** YouTube serves audio and video as **separate streams**,
   so the app picks an audio-only stream (or merges audio+video via `MergingMediaSource`).
3. **Fallback path** — if YouTube resolution fails, it falls back to a **third-party
   "RemoteAudio" API** (requests are DES-encrypted).
4. **Playback** — ExoPlayer (inside a foreground `MediaLibraryService`) streams the URL,
   applies normalization / spatial audio / EQ (your C++ engine), and plays.
5. **Side systems** — lyrics are fetched from whichever third-party provider answers first;
   plays are scrobbled to Last.fm; presence pushed to Discord; "Listen Together" syncs over
   a WebSocket to a sync server.
6. **State** — history/likes saved locally (Room/SQLDelight); settings in DataStore; search
   & stream results cached **in memory** (lost on cold start).

**The key fact:** there is **no SuvMusic server**. Every phone independently does its own
scraping, resolving, and caching against services owned by Google and others.

---

## Part 2 — The system-level flaws (how it's fragile)

### 🔴 1. The whole system depends on a private API you don't control
Metadata and streams come from **YouTube's internal endpoints**, which are **undocumented,
unversioned, and can change without warning.** YouTube periodically changes its stream
signature/cipher and endpoint shapes. When they do, **playback breaks for every user at
once**, and there's nothing you can ship fast enough to prevent the outage — you can only
react. This is the **single biggest systemic risk** and it has nothing to do with code
quality.

> **Concept: External single point of failure.** A system is only as reliable as the
> services it can't operate. You've built on a foundation that can move under you.

### 🔴 2. Stream URLs are ephemeral and resolved live
YouTube stream URLs are **time-limited and often IP-bound.** Because the app resolves them
**at the moment of play**:
- A slow resolution = a slow/janky "press play → sound" experience.
- A URL can **expire mid-session** (long pauses, long queues) → playback dies and must
  re-resolve.
- This is why retry logic and the "Silent Handshake" audio-sink workaround exist — they're
  band-aids over an inherently **fragile, just-in-time resolution model.**

> **Concept: Just-in-time vs pre-resolved.** Resolving the critical resource at the last
> possible moment makes the user-facing action (play) depend on a network round-trip that
> can fail.

### 🔴 3. Multiple fragile third-party dependencies, each a failure point
The system leans on several services it doesn't run:
- YouTube private API (metadata + streams)
- A third-party "RemoteAudio" fallback API
- 3 lyric providers (KuGou, LRCLIB, SimpMusic)
- Last.fm, Discord, the Listen-Together sync server

**Each one is an independent point of failure.** If any goes down, changes its format, or
rate-limits you, that feature degrades — and because errors are swallowed (return empty),
the user often just sees **a blank screen with no explanation**.

> **Concept: Fan-out dependency risk.** Reliability multiplies *downward* — five 99%-uptime
> dependencies give you ~95% combined. More external services = lower combined reliability.

### 🔴 4. Anti-bot / rate-limiting / account risk
Hitting YouTube's private API at scale **looks like scraping** to Google. The system risks:
- **IP-level rate limiting or blocking** (bursts of API calls).
- **Account flagging** if user cookies/auth are used (stored in EncryptedSharedPreferences).

Because every device scrapes independently (no backend to centralize/cache requests), the
**aggregate footprint across all users is large and uncoordinated**, which raises the odds
of Google clamping down.

> **Concept: No request coordination.** A backend could cache one response and serve
> thousands of users; here, thousands of users each hammer the source directly.

### 🔴 5. No graceful degradation — the system is online-first
The core experience (discovery, resolution, playback) **assumes the network works.** When
it doesn't:
- Errors are swallowed → empty UI, no "you're offline" message.
- In-memory caches vanish on cold start, so a fresh launch with bad network = a broken-
  looking app.
Downloads exist, but the **default path is online and brittle**, not offline-resilient.

> **Concept: Degrade, don't disappear.** Robust systems show *reduced* function on failure
> (cached data, clear status), not a blank screen.

### 🔴 6. The audio pipeline has device-dependent failure modes
The system **merges separate audio + video streams** and **toggles hardware audio offload**.
These add real-device variability:
- Stream-merging adds a synchronization/format risk.
- Audio-offload on/off behaves differently per chipset → the "Silent Handshake" fade-in
  hack exists because **audio sometimes doesn't start on certain devices.**
This means playback reliability **varies by phone model**, which is hard to test and
support.

### 🔴 7. No server = no central truth, observability, or control
With no backend:
- **No remote kill-switch / hotfix** when YouTube changes — you must ship an app update and
  wait for users to install it.
- **No central caching** (every device re-does the work).
- **Observability is uneven** — ACRA catches *crashes*, but **swallowed errors** (the
  empty-list failures) are **invisible**, so you can't even *measure* how often resolution
  fails in the wild.

> **Concept: Operability.** A system you can't observe or hotfix is a system you can only
> watch break.

### 🟡 8. Real-time features are latency- and uptime-fragile
"Listen Together" needs the sync server up and depends on network latency/clock alignment
between participants. If the server is down or laggy, the feature **desyncs or dies** — and
it's a feature whose entire premise is tight timing.

### 🟡 9. Half-finished internal migrations create runtime ambiguity
Because two DI systems (Hilt + Koin) and two databases (Room + SQLDelight) run side-by-side,
**runtime behavior can depend on which path initializes a component.** This is a *system*
flaw (operational), not just a code-tidiness one: it widens the matrix of "how the app can
behave on startup."

### 🟡 10. Legal / Terms-of-Service exposure (systemic, not technical)
Using YouTube's internal API and extracting streams very likely **violates YouTube's ToS.**
This is a *system-level* survival risk: the provider can deliberately break or block the
access pattern at any time, independent of any bug. No amount of clean code mitigates this.

---

## Part 3 — What a robust version of *the system* looks like

(Not code refactors — **system design** changes.)

| Flaw | Robust system design |
|------|----------------------|
| Private-API single point of failure | A thin **backend you control** that brokers/normalizes sources, so clients depend on *your* stable contract and you can fix server-side without app updates. |
| Live stream resolution | **Pre-resolve / prefetch** the next track's URL while the current one plays; refresh URLs before they expire. |
| Fan-out to many third parties | **Centralize + cache** on your backend; clients hit one endpoint. One cached lyric/stream serves many users. |
| Rate-limit / account risk | Backend with **pooled, rate-managed requests** and caching instead of every device scraping. |
| Online-first | **Offline-first**: cache last-known feed/metadata; show stale data + a clear status banner instead of blank. |
| No kill-switch / blind to failures | **Remote config + telemetry** (count resolution failures, not just crashes) so you can see and respond to breakage. |
| Swallowed errors | Model failures as typed results and **surface real status** ("offline", "session expired") to the user. |
| ToS exposure | A product/strategy decision — but a backend at least lets you **adapt the source quickly** when access changes. |

**The core systemic shift:** today the app is a **fat client talking directly to services
it can't control or observe.** The robust version puts a **thin layer you own between the
app and those services**, giving you a stable contract, caching, observability, and the
ability to fix things without shipping an app update.

---

## One-line summary

> **The flaw isn't how the code is written — it's that the entire system hangs off private,
> uncontrolled, ever-changing external services, resolved live on each device, with no
> backend to cache, coordinate, observe, or hotfix.** It works beautifully until Google (or
> any provider) changes something — and then it breaks for everyone at once, silently.

*Generated as a system-design review. No code was changed.*
