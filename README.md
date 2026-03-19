<div align="center">

# S C O R E 2 4 7

**The only cricket scoring app built for gully cricket.**
Kotlin + Jetpack Compose. Designed to end arguments before they start.

<br/>

[![Kotlin](https://img.shields.io/badge/Kotlin-2.0.0-7F52FF?style=flat-square&logo=kotlin&logoColor=white)](https://kotlinlang.org)
[![Compose](https://img.shields.io/badge/Jetpack_Compose-2023.10-4285F4?style=flat-square&logo=jetpackcompose&logoColor=white)](https://developer.android.com/jetpack/compose)
![API](https://img.shields.io/badge/Min_SDK-26-2E7D4F?style=flat-square)
![License](https://img.shields.io/badge/License-SCSL_v1.0-D97706?style=flat-square)
![Status](https://img.shields.io/badge/Status-Active-brightgreen?style=flat-square)
<br/>
</div>

---

## The Idea

Most cricket scoring apps were built for stadiums.

Score247 was built for the street.

No WiFi. No umpires. No fixed rules. Just two teams, a tape ball, and someone's phone on the boundary line.

Score247 lets you **define the rules before the match starts** — so there are no fights during it.

> *"Wide pe run hai ya nahi?"*  
> *"Last batsman akela khelega?"*  
> *"No ball dobara hogi?"*

Set it before the toss. Lock it in. Play.

---

## Philosophy

Score247 is built on a simple idea:

Clarity before action.

In street cricket, most problems don’t come from gameplay.  
They come from disagreement.

So instead of controlling the game, Score247 controls the ambiguity.

Define the rules once.  
Play without interruption.

---

## What It Does

| Feature | Description |
|---|---|
| **Custom Rules Engine** | Wide runs, no-ball re-bowls, last batsman rule — all configurable per match |
| **Fair Coin Toss** | Animated, randomised, transparent — ends all *"dubara karo"* |
| **Ball-by-Ball Scoring** | Runs 0–6, extras, wickets — one tap per ball |
| **Undo Last Ball** | Mistake on last ball? Rebuilt from event log — no state drift |
| **Session Persistence** | DataStore-backed — match survives app kill, rotation, reboot |
| **Full Scorecard** | Batting, bowling, RR, economy, Player of the Match |
| **Offline Only** | No internet. No accounts. No ads. |

---

## How It Works

```
Match Setup → Toss → Player Names
                          │
                    Live Scoring
                          │
             ┌────────────┴────────────┐
             │   Ball-by-ball input    │
             │   Undo / Extras / Wkt   │
             └────────────┬────────────┘
                          │
                 Over complete? → Rotate strike
                 Wicket? → Next batter
                 Target chased? → Innings over
                          │
                   Innings Break
                          │
                   2nd Innings
                          │
                   Final Scorecard
```

---

## Evolution

Score247 wasn’t built in one go.

It evolved the same way real systems do — through iteration, failure, and rebuilding when needed.

---

### v0.1 — Proving the Idea  
The first version answered a simple question:  
*Can defining rules before a match actually prevent arguments during it?*

Built quickly, with minimal structure.  
Functional, but rough.

It worked.

That was enough to continue.

---

### v0.2 — Refinement  
The second version focused on usability.

Improved layout, clearer flow, better interaction during live scoring.  
Less friction. More clarity.

Not a redesign — a correction.

---

### v0.3 — Rebuild  
At this point, the limitation wasn’t features.  
It was the foundation.

So the app was rebuilt entirely.

From Python/Kivy → Kotlin/Jetpack Compose.  
From mutable state → event-driven architecture.  
From working → reliable.

Undo became deterministic.  
State became predictable.  
The system became stable.

---

### What Changed Over Time

Not just the code.

The thinking behind it.

- From building features → designing systems  
- From making it work → making it reliable  
- From solving a problem → building something others can depend on  

---

## Architecture

Single ViewModel. Single StateFlow. No surprises.

```
app/
├── ui/
│   ├── screens/        # HomeScreen, SetupScreen, TossScreen,
│   │                   # BattingSetupScreen, ScoringScreen,
│   │                   # InningsBreakScreen, SummaryScreen
│   ├── components/     # PrimaryButton, RunButton, AppCard, NumberStepper...
│   └── theme/          # Color.kt, Type.kt, Theme.kt
├── viewmodel/
│   └── MatchViewModel  # Single VM, StateFlow-driven, undo by event replay
├── data/
│   ├── model/          # MatchState, Innings, Player, BallEvent (sealed)
│   └── datastore/      # MatchDataStore — JSON via kotlinx.serialization
└── navigation/
    └── AppNavigation   # Single-activity, composable nav
```

**Key design decisions:**

- **Undo by replay** — `undoLastBall` drops the last `BallEvent` and replays all prior events from scratch. No reverse mutations. No state drift. Crash-proof.
- **DataStore over Room** — match state is one JSON blob. One key. No schema migrations.
- **`runCatching` on all serialization** — a corrupt save never crashes the app. Silently ignored.
- **Reactive navigation** — screens navigate via `LaunchedEffect` watching `StateFlow`, not callbacks. Eliminates race conditions.

---

## Design

The UI is built around one principle: **clarity under pressure**.

- Warm off-white backgrounds (`#F9F9F7`) — easy on the eyes in sunlight
- Single green accent (`#2E7D4F`) — field green, desaturated, calm
- Dark score header (`#1A1A18`) — maximum contrast for the live score
- System fonts only — no downloads, crisp on every device
- Adaptive icon — field green background, white bat + ball

---

## Built With

- **Kotlin 2.0** + Coroutines + Flow
- **Jetpack Compose** (BOM 2023.10) + Material 3
- **Android DataStore** — async preferences
- **kotlinx.serialization** — JSON encoding with `@SerialName` discriminators
- **Compose Navigation** — single-activity
- **MVVM** — `AndroidViewModel` + `StateFlow`
- **SplashScreen API** — `core-splashscreen` for branded launch

---

## Installation

**From Releases:**

→ [github.com/waleedahmedja/Score247/releases](https://github.com/waleedahmedja/Score247/releases)

Download the `.apk`, enable "Install from unknown sources", install.

**Build from source:**

```bash
git clone https://github.com/waleedahmedja/Score247.git
cd Score247
./gradlew assembleDebug
```

Requires Android Studio Hedgehog or later, JDK 17+.

---

## Contributing

Read [CONTRIBUTING.md](CONTRIBUTING.md) before opening a PR.

Score247 has a clear design philosophy and a strong offline-first constraint. Contributions that add internet dependencies, analytics, or unnecessary complexity won't be merged. Keep it simple. Keep it fast. Keep it for the street.

---

## License

[Score247 Community Source License (SCSL) v1.0](LICENSE.md) — free to use, study, modify. Attribution required. Public changes stay public. No reselling or rebranding.

---

## Privacy

Score247 collects nothing. Stores nothing outside your device. Has no internet permission. See [PRIVACY_POLICY.md](PRIVACY_POLICY.md).

---

<div align="center">

*Built for that dusty pitch. That taped tennis ball. That one match everyone still argues about.*

**— waleedahmedja**

</div>
