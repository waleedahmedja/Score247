# 🏏 Score247

**Score247** is a lightweight, offline-first cricket scoring app built *for gully cricket*.

Not stadium cricket. Not ICC-perfect rules.  
Just real street matches — custom overs, flexible rules, heated moments, and memories that last longer than the score itself.

Score247 exists because **no existing app truly understands how gully cricket works**.

---

## 🌍 Why Score247?

Most cricket scoring apps assume:

- Fixed overs
- Fixed players
- Official rules
- Proper umpires

Gully cricket assumes:

- *"2 overs each"*
- *"Last batsman akela khelega"*
- *"Wide pe run hai ya nahi?"*
- *"No ball dobara hogi ya nahi?"*

Score247 lets **players define the rules before the match starts** — so there are no fights *during* the match.

---

## ✨ Features

### 🧠 Fully Custom Match Rules
- Custom number of overs (1–50)
- Custom number of players per side (2–11)
- Wide rules — runs on wide, or wide as legal ball
- No-ball rules — extra run, re-bowl, both, or neither
- Last-batsman rule — play alone or all out

### ⚡ Built for Fast Matches
- Button-based scoring — no typing mid-over
- Instant response for tense moments
- Undo last ball at any time
- Fully offline — zero internet required

### 🎯 Fair Coin Toss
- Built-in animated digital toss
- Transparent and non-manipulable
- Ends all *"dubara karo"* arguments

### 📊 Match Results & Stats
- Full match scorecard
- Batting & bowling statistics per player
- Player of the Match (deterministic, explainable logic)
- Clean, dispute-proof result screen

### 🎨 Design Philosophy
- Apple × Braun aesthetic — minimal, functional, beautiful
- OLED-friendly dark score header
- Large touch targets for in-match pressure moments
- No ads. No tracking. No noise.

---

## 🛠 Tech Stack

| Layer | Technology |
|---|---|
| Language | Kotlin 2.0 |
| UI | Jetpack Compose + Material 3 |
| Architecture | MVVM + StateFlow |
| Persistence | DataStore (JSON via kotlinx.serialization) |
| Platform | Android (API 26+) |
| Mode | Fully offline |

---

## 📁 Project Structure

```
app/src/main/java/com/waleedahmedja/score247/
├── MainActivity.kt
├── data/
│   ├── model/Models.kt          — Match state, rules, player, ball events
│   └── datastore/MatchDataStore.kt
├── viewmodel/
│   └── MatchViewModel.kt        — All match logic, undo, scoring
├── navigation/
│   └── AppNavigation.kt
└── ui/
    ├── theme/                   — Color, typography, theme
    ├── components/              — Reusable composables
    └── screens/                 — Setup → Toss → Batting → Scoring → Summary
```

---

## 🚀 Getting Started

1. Clone the repo
2. Open in Android Studio (Hedgehog or newer)
3. Sync Gradle — no additional setup required
4. Run on device or emulator (API 26+)

> The app is fully offline. No API keys, no backend, no accounts.

---

## 🚦 Project Status

| Feature | Status |
|---|---|
| Core match engine | ✅ Complete |
| Ball-by-ball scoring + undo | ✅ Complete |
| Custom rules engine | ✅ Complete |
| Batting & bowling stats | ✅ Complete |
| DataStore persistence | ✅ Complete |
| Adaptive app icon | ✅ Complete |
| Splash screen | ✅ Complete |
| Play Store preparation | 🟡 Pending |

---

## 🤝 Contributing

Score247 is open for community contributions. See [CONTRIBUTING.md](CONTRIBUTING.md) for guidelines.

---

## 📄 License

Score247 is released under the **Score247 Community Source License (SCSL) v1.0**.  
See [LICENSE.md](LICENSE.md) for full terms.

Key points: free to use, study, and contribute — but not to sell or rebrand.

---

## 📋 Code of Conduct

This project follows a contributor code of conduct. See [CODE_OF_CONDUCT.md](CODE_OF_CONDUCT.md).

---

## 📥 Download

Latest APK: [GitHub Releases](https://github.com/waleedahmedja/Score247/releases)

---

❤️ Final Note

Score247 isn't just an app.

It's that dusty pitch.  
That taped tennis ball.  
That one match everyone still argues about.

Built for the streets. Built with heart.

— **waleedahmedja**
