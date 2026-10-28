# EXPLAIN.md: Score247, explained from A to Z

This guide explains **everything** that happens in the Score247 code, in plain English, so you can
read the project, learn from it, and talk about it confidently in an interview. You do not need
to know Kotlin or Android already. Every new word is explained the first time it appears, and
there is a glossary at the end.

**How to use this guide**

1. Read Parts 1 to 3 once, straight through. They give you the big picture.
2. Then read Part 4 (the files) next to the real source code, one file at a time.
3. Read Part 5 (a ball's journey) slowly. It ties everything together.
4. Before an interview, practise with Part 8 (questions and answers).

> Line numbers change as the code changes, so this guide quotes short pieces of real code and
> names the function they come from. Search for the function name to find it.

---

## Contents

1. [What the app does](#1-what-the-app-does)
2. [The project map](#2-the-project-map)
3. [The big ideas](#3-the-big-ideas)
4. [The files, one by one](#4-the-files-one-by-one)
5. [One ball's journey: from your thumb to the saved file](#5-one-balls-journey)
6. [Undo, step by step](#6-undo-step-by-step)
7. [Design decisions and trade-offs](#7-design-decisions-and-trade-offs)
8. [Interview questions and answers](#8-interview-questions-and-answers)
9. [Bugs that were found and fixed](#9-bugs-that-were-found-and-fixed)
10. [Security and privacy](#10-security-and-privacy)
11. [Glossary](#11-glossary)

---

## 1. What the app does

Score247 is an Android app for scoring **street (gully) cricket**. In street cricket every group
has its own rules, so before the match you switch rules on or off. Then you tap one button per
ball and the app keeps the score, the batters' and bowlers' figures, the over, the target and the
result.

### The journey of a match, screen by screen

| # | Screen | What the user does | File |
|---|---|---|---|
| 1 | **Home** | Starts a new match, resumes one in progress, opens history or "About". | `HomeScreen.kt` |
| 2 | **Setup** | Types the two team names, chooses overs and players per side, switches the house rules. | `SetupScreen.kt` |
| 3 | **Toss** | Taps a coin that flips; the winner picks bat or bowl. | `TossScreen.kt` |
| 4 | **Squads** | Types the player names for both teams (once). | `PlayersScreen.kt` |
| 5 | **Scoring** (innings 1) | Taps 0 to 6, Wicket, Wide, No ball, Undo for each ball. | `ScoringScreen.kt` |
| 6 | **Innings break** | Sees the first score and the target. | `InningsBreakScreen.kt` |
| 7 | **Scoring** (innings 2) | Same screen again; now there is a target to chase. | `ScoringScreen.kt` |
| 8 | **Scorecard** | Sees the result, Player of the Match, full tables; shares a picture. | `SummaryScreen.kt` |
| 9 | **History** | Browses finished matches and player leaderboards. | `HistoryScreen.kt` |
| 10 | **Match detail** | Reopens one old match; shares or deletes it. | `MatchDetailScreen.kt` |
| 11 | **About and legal** | Reads the privacy policy, terms, licence. | `AboutScreen.kt`, `DocumentScreen.kt` |

### The house rules

These live in the `MatchRules` class. Each one is a true/false switch (or a number):

| Rule | Meaning |
|---|---|
| `totalOvers` | How many overs each side bats (1 to 50). An over is 6 legal balls. |
| `playersPerTeam` | Squad size (2 to 11). |
| `wideGivesRun` | A wide costs the bowling side 1 extra run. |
| `wideIsLegalBall` | A wide uses up one of the 6 balls in the over (a street rule; real cricket re-bowls it). |
| `noBallGivesRun` | A no-ball costs 1 extra run. |
| `lastBatsmanAlone` | When only one batter is left, they keep batting alone instead of the innings ending. |
| `freeHitOnNoBall` | The ball after a no-ball is a "free hit": only a run-out can dismiss the batter. |

---

## 2. The project map

```
Score247/
├── app/
│   ├── build.gradle.kts            the build recipe: libraries and settings
│   ├── proguard-rules.pro          shrinking rules for the release build
│   └── src/
│       ├── main/
│       │   ├── AndroidManifest.xml     tells Android what the app is
│       │   ├── assets/docs/            the legal documents shown inside the app
│       │   ├── res/                    pictures, colours, the app name, file_paths.xml
│       │   └── java/com/waleedahmedja/score247/
│       │       ├── MainActivity.kt         the front door
│       │       ├── navigation/AppNavigation.kt   which screen leads to which
│       │       ├── viewmodel/MatchViewModel.kt   the brain that holds the match
│       │       ├── engine/                 the cricket rules (pure Kotlin)
│       │       │   ├── ScoringEngine.kt
│       │       │   └── CareerStats.kt
│       │       ├── data/
│       │       │   ├── model/Models.kt         the "vocabulary" of the app
│       │       │   └── datastore/MatchDataStore.kt   saving to the phone
│       │       ├── ui/
│       │       │   ├── theme/Theme.kt          colours, fonts, shapes
│       │       │   ├── components/             reusable buttons, cards, run pad
│       │       │   └── screens/                one file per screen
│       │       └── util/                   formatting, sharing, markdown, links
│       └── test/                       the automatic tests
├── docs/                           banner, icon, setup notes
├── README.md  EXPLAIN.md  CONTRIBUTING.md  LICENSE  PRIVACY.md  TERMS.md ...
└── .github/                        build checks and templates for GitHub
```

### The layers (and why there are layers)

The app is split into layers. Each layer has **one job** and only talks to the layer next to it.

```
 ┌──────────────────────────────────────────────────────────────┐
 │  UI (ui/)           draws screens, reports taps               │
 ├──────────────────────────────────────────────────────────────┤
 │  ViewModel          holds the live match, applies actions     │
 ├──────────────────────────────────────────────────────────────┤
 │  Engine (engine/)   ALL cricket rules, pure Kotlin            │
 ├──────────────────────────────────────────────────────────────┤
 │  Data (data/)       the models and saving to the phone        │
 └──────────────────────────────────────────────────────────────┘
```

Why? **Because you can change or test one layer without touching the others.** The engine has no
Android code at all, so its tests run in milliseconds on a laptop. The screens know nothing about
cricket: they only show what the ViewModel gives them.

---

## 3. The big ideas

Read these slowly. Once you understand them, the code stops being mysterious.

### 3.1 Immutability: never edit, always make a new copy

An **immutable** object can never be changed after it is created. To "change" it, you create a
new copy with the difference. Kotlin makes this easy with `data class` and `copy()`:

```kotlin
val a = Player(id = 0, name = "Sam", runsScored = 10)
val b = a.copy(runsScored = 14)   // a is untouched; b is a new Player with 14 runs
```

**Why it matters here:** the scoring engine returns a *new* `Innings` for every ball. The old
one still exists unchanged. That removes a whole family of bugs ("something changed this object
when I wasn't looking") and makes undo and testing simple.

### 3.2 Event sourcing: store what happened, not the totals

A normal design stores `runs = 34, wickets = 2` and updates them. Score247 stores a **list of
events**:

```
[ Runs(1), Runs(4), Wide(0), Runs(0), Wicket(caught), Runs(6) ... ]
```

The totals are never stored by hand. They are **worked out by replaying the list from the
beginning**. The class `Innings` does hold the totals (so screens can read them quickly), but
every one of them was produced by `applyEvent` folding the events one after another.

Think of a bank statement: the balance is not a number someone types in; it is the result of
adding up every transaction.

### 3.3 Undo for free

If the totals come from replaying events, then **undo is just "replay everything except the last
event"**. There is no special undo logic that could fall out of step. Here is the entire undo:

```kotlin
fun withoutLastBall(innings: Innings, rules: MatchRules): Innings? =
    if (innings.events.isEmpty()) null
    else rebuild(innings, innings.events.dropLast(1), rules)
```

### 3.4 One-way data flow ("state down, events up")

```
        ┌─────────── state (the data to show) ───────────┐
        ▼                                                 │
    Screen  ──── events (the user tapped something) ────► ViewModel
```

- The ViewModel owns the **state** (the current `MatchState`).
- Screens **only read** it and draw it.
- When the user taps, the screen sends an **event** up ("scoreBall with 4 runs").
- The ViewModel computes a new state; screens automatically redraw.

Screens never change the data themselves. There is exactly one place where the match can change,
which makes the app predictable.

### 3.5 Reactive UI: the screen is a function of the state

In Jetpack Compose you do not say "now change this text to 35". You write a function that says
"given this state, this is what the screen looks like". When the state changes, Compose calls the
function again and updates only what differs. This is called **recomposition**.

### 3.6 Coroutines and Flow: doing slow things without freezing the screen

Saving to the phone and drawing a big picture are slow. If they ran on the thread that draws the
screen, the app would freeze. A **coroutine** is a lightweight way to say "do this in the
background and carry on". A **Flow** is a stream of values over time (for example, "every time
the match changes, here is the new match"). The ViewModel exposes the match as a `StateFlow`: a
Flow that always remembers its latest value.

### 3.7 Persistence: surviving the app being closed

Android may kill the app at any time to free memory (this is called **process death**). So every
change to the match is also saved to the phone as text (JSON). When the app starts again it reads
the file and puts you back exactly where you were.

---

## 4. The files, one by one

Open each file next to this guide. Every section starts with **what the file is for**, then walks
through the important parts.

### 4.1 `data/model/Models.kt`: the app's vocabulary

This file does nothing by itself. It only **describes the things a cricket match is made of**.
Every other file uses these definitions.

**`Routes`** is a list of screen addresses (`"home"`, `"setup"`, `"scoring/{innings}"` ...). They
are written once here so a typo can never send the user to a screen that does not exist.
`Routes.scoring(2)` fills in the blank and gives `"scoring/2"`.

**`Limits`** caps what users can type. `cleanName` removes invisible control characters (tabs, line
breaks), trims spaces and cuts the name to 30 characters. This stops a huge or strange name from
bloating the saved file or breaking the shared picture.

**`MatchRules`** is the house-rules box described in section 1. One interesting line:

```kotlin
val maxWickets: Int get() = if (lastBatsmanAlone) playersPerTeam else playersPerTeam - 1
```

A team of 6 is normally all out after 5 wickets (one batter is left with no partner). If the
last batter may bat alone, all 6 must get out. This *computed property* keeps that logic in one
place.

**`DismissalKind`** lists the ways to get out. Each has a label and a flag `creditsBowler`. Only
`RUN_OUT` has `false`, because a run-out is not the bowler's wicket.

**`Player`** is one box that holds both batting and bowling numbers (runs, balls faced, fours,
sixes, whether out, how, plus balls bowled, runs conceded, wickets). It also has three
**computed values**: `strikeRate`, `economy` and `oversBowled`. Note how `oversBowled` turns balls
into cricket notation: `13` balls is `13 / 6 = 2` whole overs and `13 % 6 = 1` extra ball, so
`"2.1"`. (`%` is the remainder after dividing.)

> A player's `id` is always their position (index) in their team's list. The engine relies on
> this, which is why `newInnings` creates players with `mapIndexed`.

**`BallEvent`** is the heart of the data model. It is a **sealed class**: a closed family with
exactly six members:

| Event | Meaning |
|---|---|
| `Runs(runs, batter, bowler)` | A normal legal ball; all runs came off the bat |
| `Wide(extras, bowler)` | Wide; `extras` are runs run *on top of* the penalty |
| `NoBall(extras, batter, bowler)` | No-ball; `extras` are runs the batter hit |
| `Wicket(batter, bowler, runsOnBall, kind, nextBatter)` | A dismissal |
| `Bye(runs, bowler)` | Ball missed everyone, batters ran |
| `LegBye(runs, bowler)` | Ball hit the batter's body, batters ran |

Why *sealed*? Because when a `when (event)` covers every member, Kotlin checks you did not forget
one. If you add a seventh event later and forget to handle it, **the code will not compile**. The
compiler becomes your safety net.

Each member has `@SerialName("runs")` etc. That fixes its name in the saved JSON, so renaming the
Kotlin class later will not break matches already saved on someone's phone.

**`Innings`** is the live scoreboard of one team's turn to bat: the two player lists, runs,
wickets, legal balls, extras, who is striker / non-striker / bowling, a flag
`awaitingNextBatter` (after a wicket, until the scorer picks the new batter), the **event
diary**, and whether the innings is complete and why.

Two computed values to notice: `currentOver = balls / 6` and `currentBall = balls % 6`, and
`oversDisplay` glues them as `"3.4"`.

**`Toss`, `TossChoice`, `MatchPhase`**: the toss result and the stage of the match. `MatchPhase`
(`SETUP`, `TOSS`, `SETUP_INNINGS_1`, `INNINGS_1`, `INNINGS_BREAK`, `INNINGS_2`, `COMPLETED`, plus a
leftover `SETUP_INNINGS_2`) is saved so the app knows which screen to reopen after being killed.

**`MatchState`** is the one object that holds the whole match: team names, rules, toss,
`innings1`, `innings2`, phase, result text and Player of the Match. Its helper functions:

- `targetFor(2)` = first innings runs **+ 1** (a tie is not a win).
- `battingTeamFor(n)` works out who bats from the toss: if the toss winner chose BAT they bat
  first, otherwise the other team does. For innings 2 it is whoever did not bat first.

**`MatchRecord`** wraps a finished `MatchState` with an id and a `completedAt` time. History is a
list of these.

### 4.2 `engine/ScoringEngine.kt`: all the cricket rules

This is the most important file. It is a Kotlin `object` (one single instance), so you call
`ScoringEngine.applyEvent(...)` directly. It has **no Android code**, so it is tested without a
phone.

#### `newInnings`

Builds a fresh innings. Batter 0 is the striker, batter 1 is the non-striker (if the team has
only one player, that one opens alone). Players are created with `mapIndexed` so each one's `id`
equals its position.

#### `isLegalDelivery`

```kotlin
is BallEvent.Runs, is BallEvent.Wicket, is BallEvent.Bye, is BallEvent.LegBye -> true
is BallEvent.Wide    -> rules.wideIsLegalBall
is BallEvent.NoBall  -> false
```

A *legal* ball uses up one of the six in the over. A no-ball never does. A wide only does if the
street rule says so.

#### `isFreeHit`

Looks at the most recent event **that is not a wide** and asks "was that a no-ball?". Skipping
wides is deliberate: a wide bowled during a free hit does not use the free hit up.

#### `applyEvent`: the most important function in the app

It takes `(innings, event, rules)` and returns a **new** innings. It never edits the old one.
The shape is always:

1. Copy the numbers into local `var`s (`runs`, `wickets`, `balls`, `striker` ...).
2. A big `when (event)` changes them according to the kind of ball.
3. After the `when`: if the ball was legal and the over just ended, the batters swap ends.
4. `return innings.copy(...)` with the new numbers and the event added to the diary
   (`events = innings.events + event`).

What each branch does:

- **`Runs`**: `balls++`; the striker `facedBall(runs)` (which also counts fours and sixes); the
  bowler's balls and runs go up; team runs go up; **odd runs swap the batters** (they crossed).
- **`Wide`**: penalty run (if the rule is on) plus any extra runs all go to *extras*. The bowler
  is charged. The over only advances if `wideIsLegalBall` is on. Batters swap only if the *extra*
  runs are odd.
- **`NoBall`**: the batter **did face it**, so runs they hit are theirs. Only the penalty run is an
  extra. The over does not advance.
- **`Wicket`**: the longest branch. It marks the dismissed batter out (the striker, or for a
  run-out possibly the non-striker), credits the bowler only if `kind.creditsBowler`, adds any
  runs completed before the dismissal, then decides who comes in:
  - someone is waiting → if the event already knows `nextBatter` (the **replay path**), put them
    in; otherwise set `awaitingNextBatter = true` (the **live path**) and the scorer is asked;
  - nobody waiting but `lastBatsmanAlone` is on → the survivor stands at both ends;
  - otherwise the team is all out.
- **`Bye` / `LegBye`**: the ball counts, runs go to extras and the team total, but **not to the
  bowler's runs conceded**.

> Notice that the bowler comes from the **event** (`event.bowlerIndex`), not from "who is bowling
> now". That is what keeps undo exact after a bowling change.

#### `selectNextBatter`

After a wicket the scorer taps the new batter. This function places them in the empty end **and
also writes the choice onto the wicket event** (`e.copy(nextBatter = batterIndex)`). Because the
choice is stored in the diary, replaying the diary later (undo) puts the same person in
automatically. Invalid choices are ignored.

#### `checkCompletion`

The innings ends when **any** of these is true, checked in this order:

1. `"Target chased!"`: a target exists and runs have reached it.
2. `"Overs complete"`: balls bowled reached `totalOvers * 6`.
3. `"All out"`: fewer batters remain than the minimum (1 if last batter may bat alone, else 2).

#### `calcResult`

Chasing side wins "by N wickets" (wickets in hand = `maxWickets - wickets`), first side wins
"by N runs", equal scores give "Match tied!". It also handles the singular ("1 run", "1 wicket").

#### `rebuild` and `withoutLastBall` (undo)

```kotlin
return events.fold(fresh) { acc, ev -> applyEvent(acc, ev, rules) }
```

`fold` means: start with `fresh`; feed in event 1 to get a new innings; feed event 2 into *that*;
and so on. The last result is the innings after all the events. Undo is
`rebuild(innings, innings.events.dropLast(1), rules)`: replay everything except the last ball.

#### `currentOver`

Builds the little dots under "This over". It handles a subtle moment: straight after the sixth
legal ball it keeps showing the **finished** over (so the scorer can check it) instead of an
empty new one. Wides and no-balls are filed under the over that was current when they happened.

#### `suggestPotm`

Gives every player points and picks the top scorer:

- Batting: 1 point per run, plus `strikeRate / 10` if they faced at least 3 balls (so one lucky
  slog does not win).
- Bowling: 25 points per wicket, plus `12 - economy` (never below 0) if they bowled at least one
  over.
- Only the **winning** team is eligible (either team on a tie).

#### Private helpers

- `BallEvent.bowlerIndex`: lets any event answer "who bowled you?" without checking its type.
- `updatedAt(index) { ... }`: returns a list in which only that one player changed.
- `facedBall(batRuns)`: one ball faced, runs added, four/six counted.

---

### 4.3 `engine/CareerStats.kt`: totals across every saved match

`CareerStats.compute(records)` turns the history list into one `PlayerCareer` per real player.

- **Matching by name.** The app has no accounts, so "Sam" in match 1 and "sam " in match 2 are the
  same person: the key is `name.trim().lowercase()`.
- **Placeholders skipped.** Names like "Player 3" are ignored (`isRealName`, using a *regex*, a
  text pattern). Otherwise two unrelated people called "Player 3" would be merged into one fake star.
- **A private `Tally` class** is a mutable scratch-pad used only while adding up. The finished
  result is converted to the immutable `PlayerCareer`.
- **`addBatting`** counts an innings only if the player really batted (`hasBatted`). It tracks
  not-outs, runs, balls, fours, sixes and the high score (on a tie, a *not-out* score is better).
- **`addBowling`** adds wickets, balls and runs, and tracks best figures (most wickets, then fewest runs).
- Computed values on `PlayerCareer`: batting average = runs ÷ **dismissals** (`null` until the
  player has been out once, because dividing by zero is meaningless), strike rate, economy.
- **Leaderboards**: `battingLeaders` sorts by runs then strike rate; `bowlingLeaders` by wickets
  then lower economy (`compareByDescending ... thenBy`).

### 4.4 `data/datastore/MatchDataStore.kt`: the filing cabinet

**DataStore** is Android's tool for saving small amounts of data. Score247 saves **two** things
under two keys: `"state"` (the match in progress) and `"history"` (finished matches). Each is one
JSON string.

```kotlin
private val json = Json {
    ignoreUnknownKeys = true   // an older/newer version may have written extra fields
    encodeDefaults    = true
}
```

- `ignoreUnknownKeys` means a file written by a different app version does not crash the reader.
- Reading uses a `Flow`. `.catch { ... }` turns a disk-read error into "empty" instead of a crash,
  and a `try/catch` around decoding treats corrupt JSON as "nothing saved".
- Writing is a `suspend fun` (it may take time and runs in the background). The
  `CancellationException` is deliberately re-thrown: cancellation is Kotlin's polite way of saying
  "stop this task", and swallowing it would break coroutines.
- Saving is **best-effort**. If a write fails, the live match in memory is still correct, and the
  next change tries again. A scoring app must never freeze because a disk write failed.

### 4.5 `viewmodel/MatchViewModel.kt`: the brain that holds the match

A **ViewModel** is an object Android keeps alive while screens come and go (for example when the
phone rotates). It holds the one live `MatchState` and is **the only place that changes it**.

**State holders.** These are `StateFlow`s: observable boxes that always hold a current value.

| Name | Holds | Used for |
|---|---|---|
| `state` | the current `MatchState` | every screen |
| `history` | finished matches, newest first | History, stats |
| `ready` | `false` until the saved match is loaded | navigation waits for it |
| `toast` | one-shot messages | "Last ball undone" etc. |

The pattern `private val _state = MutableStateFlow(...)` with a public `val state = _state.asStateFlow()`
is deliberate: only the ViewModel can *write*; everyone else can only *read*.

**`init { ... }`** runs when the ViewModel is created. In a coroutine it:

1. loads the saved match (and gives an old save a `id` if it lacks one),
2. loads the history,
3. sets `ready = true`,
4. starts two collectors: `_state.drop(1).collectLatest { store.save(it) }` and the same for history.

`drop(1)` skips the initial value (no point re-saving what we just loaded). `collectLatest`
cancels an in-flight save if a newer state arrives, so rapid scoring never builds a queue of writes.

**The match flow functions**, in order:

- `saveSetup`: cleans and validates names (both required, must differ) and **replaces** the whole
  state with a fresh `MatchState` in phase `TOSS`. Replacing, not editing, means nothing from a
  previous match can leak into the new one.
- `performToss`: stores the toss, moves to `SETUP_INNINGS_1`.
- `startFirstInnings`: builds innings 1 with `ScoringEngine.newInnings`. Blank player names become
  "Player 1", "Player 2" ... (`cleanNames`).
- `proceedToInnings2`: builds innings 2 by **swapping the squads** (`i1.bowlers` now bat), so nobody
  retypes names.
- `scoreBall(inningsNum, event)`: see Part 5; it is the main event.
- `selectNextBatter`, `setBowler`: small updates that go through the engine or `copy`.
- `undoLastBall`: asks the engine for the innings without its last ball, **rolls the phase back**
  and clears the result and Player of the Match. If the match had already finished, it also removes it from history.
- `recordInHistory`: puts the finished match at the front of the list, removing any earlier copy
  with the same id, and keeps at most 50 (`take(MAX_HISTORY)`).
- `newMatch`: resets to an empty `MatchState()` synchronously. (An old version cleared storage
  first and reset afterwards, so the next screen briefly saw the old match. That was a bug.)

### 4.6 `navigation/AppNavigation.kt`: the map of screens

- A **route** is a screen's text address. A **NavHost** shows whichever route is on top of the
  **back stack** (the pile of visited screens; Back removes the top one).
- `resumeRoute(phase)` converts a saved `MatchPhase` into a route. This is what reopens the right
  screen after the app was killed.
- The map **always starts at HOME**, so HOME is guaranteed to be at the bottom of the stack. Then a
  `LaunchedEffect(Unit)` (run once when first shown) navigates to the resumed screen if needed.
- `popUpTo(...) { inclusive = true }` removes screens from the stack so Back never returns to
  a finished step (for example, from the toss back to setup).
- A subtle bug that was fixed: `popUpTo` must use the route **pattern** `"scoring/{innings}"`, not a
  concrete `"scoring/1"`, otherwise it matches nothing and Back from the summary bounced into
  the scoring screen.
- Arguments: `navArgument("innings") { type = NavType.IntType }` makes Navigation parse the number
  from the route for you.

### 4.7 `MainActivity.kt`: the front door

An **Activity** is one screen-sized window of an Android app. `MainActivity` does only four things:

1. installs the splash screen (shown while the saved match loads),
2. creates the ViewModel with `by viewModels()` (Android keeps it across rotation),
3. turns on edge-to-edge drawing (the app draws behind the status and navigation bars),
4. calls `setContent { Score247Theme { AppNavigation(vm) } }`, which hands the screen to Compose.

---

### 4.8 `ui/theme/Theme.kt`: colours, fonts and shapes

- **Semantic tokens.** Screens never write a colour like `#23744A`. They write `color = Pitch` or
  `color = Ink`. Each token has a **light** and a **dark** value inside `LightPalette` /
  `DarkPalette`. Switching theme swaps the whole palette, and no screen needs to know.
- **`S247Palette`** is the class holding the tokens: neutrals (`ink`...`ink50`, `paper`), brand
  (`pitch` = field green), accent (`amber`), danger (`red`) and the always-dark "scoreboard"
  header colours.
- **`LocalPalette`** is a *CompositionLocal*: a value made available to every composable below a
  certain point without passing it as a parameter. `Score247Theme` provides it.
- The getters (`val Ink: Color @Composable get() = LocalPalette.current.ink`) make call sites short.
- **Contrast.** Every text/background pair is chosen to be at least 4.5:1 (WCAG AA), so text is
  readable by people with weak eyesight.
- **Typography.** Score digits use `fontFeatureSettings = "tnum"` (*tabular numbers*: every digit
  has the same width), so the score does not wiggle sideways as it counts up.
- `toColorScheme()` also feeds the same colours into Material 3, so stock widgets (switches, text
  fields) match the brand.

### 4.9 `ui/components/`: reusable building blocks

Small pieces used by many screens, so the app looks consistent and each look is written once.

| Component | What it is |
|---|---|
| `S247Button`, `S247ButtonSecondary`, `S247ButtonDanger` | The three button styles (green, outlined, red) |
| `RunTile` | The big 0 to 6 button on the run pad; gives a small vibration |
| `S247Card` | A rounded white (or dark) box that groups content |
| `RuleRow` | A rule name, a description and a switch |
| `Stepper` | A "minus / number / plus" control (overs, players) |
| `OptionChip` | A selectable pill, used for choices |
| `BallDot` | One circle in the "This over" strip (colour and symbol depend on the event) |
| `S247Screen` | The standard screen frame: title bar, back arrow, content, system-bar handling |
| `SystemBarIcons` | Makes status/navigation icons light or dark so they stay visible |
| `ToastHost` | Shows the one-shot messages from the ViewModel for a moment |
| `EmptyState` | The friendly "nothing here yet" message |
| `AppLogo` | Shows your real app icon (`res/drawable-nodpi/app_logo.png`), rounded |
| `ShareScorecardActions` (in `ShareActions.kt`) | The Share buttons and the picture preview dialog |

### 4.10 The screens (`ui/screens/`)

Every screen follows the same recipe:

```kotlin
val state by vm.state.collectAsStateWithLifecycle()   // 1. watch the state
// 2. draw it with composables
// 3. on a tap, call a ViewModel function (never change data here)
```

**`HomeScreen`**: has three faces depending on the match: *fresh* (just "Start match"), *in
progress* (a card with the live score plus "Resume" and "New"), *done* (a card with the result and
"View scorecard"). It also shows the logo, plus History and About entries.

**`SetupScreen`**: team-name fields with inline error messages, steppers for overs and squad size,
and a `RuleRow` for each house rule. It pre-fills from your last match. When the form is valid it
calls `vm.saveSetup(...)`.

**`TossScreen`**: a coin that flips (an animation). The result is held with `rememberSaveable`, so
rotating the phone mid-flip does not re-toss. Then the winner picks Bat or Bowl, and
`vm.performToss(...)` is called.

**`PlayersScreen`**: one card per squad with a text field per player. `rosterFor` suggests names
from a team's last match so regulars are pre-filled. `nameListSaver` is a custom *Saver* telling
`rememberSaveable` how to store a list of names across rotation and process death. Blank names
become "Player N" in the ViewModel.

**`ScoringScreen`**: the busiest screen (read it in this order):

1. **`SystemBarIcons` and keep-screen-on**: the header is dark in both themes, so status icons must
   be light. `DisposableEffect` sets `view.keepScreenOn = true` and **undoes it on dispose**, so the
   phone does not sleep mid-match but goes back to normal on leaving.
2. **`LaunchedEffect(innings?.complete)`**: the moment the innings is complete, call `onComplete()`
   which navigates away. This also covers "app restarted on an already-finished innings".
3. **`LaunchedEffect(balls, awaiting)`**: after every 6th legal ball it opens the "Select bowler" dialog.
4. **Derived values**: `striker`, `bowler`, `target`, `ballsLeft`, `freeHit`, `over` (the last is
   wrapped in `remember(innings.events, rules)` so it is only recomputed when events change).
5. **`ScoreHeader`**: the big score. `animateIntAsState` makes the number *count up* to the new
   total over 300 ms. It shows the chase progress bar, CRR (current run rate), and "Need X off Y,
   RRR" (required run rate, from `requiredRate`).
6. **The run pad**: `RunTile` for 0 to 6 plus "Wicket". Each tap just calls
   `score(BallEvent.Runs(r, innings.strikerIndex, innings.bowlerIndex))`. The screen builds the
   event; the ViewModel and engine do the rest.
7. **Wide / No ball / More...**: one-tap buttons for the two most common extras; "More..." opens
   `ExtrasDialog` for byes, leg-byes and extras with runs. The dialog's confirm button shows the
   exact total it will add ("Add 3 runs").
8. **`WicketDialog`**: choose how out; for run-out also choose who is out and runs completed. On a
   **free hit** the list is reduced to `RUN_OUT` only.
9. **The "Who bats next?" dialog** cannot be dismissed (`onDismissRequest = {}`): scoring is locked
   until a choice is made (or the wicket is undone). While locked, all run tiles are disabled.
10. **`ToastHost`** shows messages such as "Last ball undone".

**`InningsBreakScreen`**: shows the first score and target; "Start chase" calls
`vm.proceedToInnings2()`; "Undo" returns into innings 1.

**`SummaryScreen`**: result, Player of the Match, and `ScorecardBody` (innings tiles plus
`BattingCard` and `BowlingCard` tables). `ScorecardBody` is its own function so
`MatchDetailScreen` can reuse it for old matches. It also calls `vm.autoSuggestPotm()` as a safety
net for matches saved by older versions.

**`HistoryScreen`**: two tabs. *Matches* is a `LazyColumn` (a list that only draws visible rows)
keyed by match id. *Players* computes `CareerStats.compute(history)` inside
`remember(history)` so the maths is redone only when history changes, not on every redraw.

**`MatchDetailScreen`**: finds the record by id, shows `ScorecardBody`, lets you share it, and
delete it after a confirmation dialog.

**`AboutScreen` / `DocumentScreen`**: About shows the version, the GitHub link and a list of
documents from `LegalDocs`. `DocumentScreen` loads one `.md` file from `assets/docs/`, runs
`MarkdownParser.parse`, and draws each block. Links open only if they are `https` (see Part 10).

### 4.11 Sharing code (`util/`)

**`ScorecardFormatter.kt`**: helper functions: `oneDecimal()`, `requiredRate()`, `dismissalText()`
(builds "c Sam b Ali"), `potmLine()` (finds the Player of the Match and a stats string), and
`shareScorecard()` (the plain-text scorecard).

**`DateFormatting.kt`**: `formatMatchDate` turns a number of milliseconds into a readable date.

**`ShareSummary.kt`**: decides **what** to print on the picture, with no Android code:
`ShareSummaryBuilder.from(state)` returns the title, result, one `InningsLine` per innings (score,
overs, run rate, top batter, top bowler) and the Player of the Match. `topBatter` picks most runs
(fewer balls on a tie) and puts a `*` for not out. `caption()` builds the message text that
carries the GitHub link.

**`ScorecardImage.kt`**: draws the 1080 x 1920 picture (Instagram-story shape) with Android's
`Canvas`: dark gradient, your icon, result, two score cards with their stars, Player of the Match,
and a **QR code** of the GitHub URL (made with the ZXing library) in the corner.

> **Why a QR code?** Instagram stories ignore captions and an image cannot hide a clickable link.
> A QR code can be scanned straight off a screen, and the caption link works on WhatsApp.

**`ShareHelper.kt`**: draws the image in the background, saves it to the app's private cache
folder, then shares it through a **FileProvider**. Android does not let you hand another app a raw
file path. A FileProvider creates a temporary `content://` address with read permission for just
that one file (`FLAG_GRANT_READ_URI_PERMISSION`). The provider must be declared in
`AndroidManifest.xml` (see `docs/SETUP_SHARING.md`) and `res/xml/file_paths.xml` says which folder
may be shared (only `cache/shared/`).

**`MarkdownParser.kt`**: a small hand-written parser. `parse` walks the text line by line and groups
lines into blocks (heading, paragraph, bullet, numbered, code, rule). `spans` uses one regular
expression to split a line into plain, **bold**, *italic*, `code` and link pieces.

**`LegalDocs.kt`**: the list of documents (id, title, one-line blurb, file name) that About shows.

### 4.12 The tests (`app/src/test/`)

Tests are small programs that check the code, so you notice at once if a change breaks something.
They cover only code with no Android in it, so they run on a laptop in milliseconds.

| File | What it proves |
|---|---|
| `ScoringEngineTest` | Every rule: runs, wides, no-balls, wickets, run-outs, free hit, over end, completion, result, Player of the Match, **and that undo rebuilds exactly** (the `undo_*` tests) |
| `CareerStatsTest` | Name matching, skipping "Player N", averages, best bowling |
| `ShareSummaryTest` | Top batter/bowler choice and the caption |
| `MarkdownParserTest` | Headings, lists, code blocks, links |
| `ScorecardFormatterTest` | Number formatting and required rate |
| `LimitsTest` | Name cleaning and length cap |

A good interview line: *"The most valuable test plays 400 random innings under random house rules and, after every ball, checks that replaying the event log rebuilds exactly the same innings; the undo tests do the same for removing the last ball
ball one by one, and checks the result equals the innings before that ball."*

### 4.13 Build files and resources

- **`app/build.gradle.kts`**: `minSdk 26` (Android 8+), `targetSdk 35`, version `0.4.0`. Libraries:
  Compose + Material 3, Navigation, DataStore, kotlinx.serialization, and `com.google.zxing:core`
  (QR codes).
- **`AndroidManifest.xml`**: declares the app and `MainActivity`; you add the `FileProvider` entry from `docs/SETUP_SHARING.md`.
- **`res/`**: `drawable-nodpi/app_logo.png` (your icon), `xml/file_paths.xml`, `values/` (app name,
  colours, themes, the splash screen colour).
- **`.github/`**: a workflow that builds and tests on every push, Dependabot for library updates,
  and issue/PR templates.

---

## 5. One ball's journey

Follow a single tap: the scorer hits **4** during innings 1. Every step names the real code.

1. **Tap.** `RunTile(4)` fires its `onClick`: a small vibration, then
   `score(BallEvent.Runs(4, innings.strikerIndex, innings.bowlerIndex))` in `ScoringScreen`.
2. **Up to the ViewModel.** `score` calls `vm.scoreBall(1, event)`.
3. **Guards.** `scoreBall` returns immediately if the innings is complete or waiting for a next
   batter, and rejects a non-run-out wicket on a free hit (with a toast).
4. **Engine.** `ScoringEngine.applyEvent(innings, event, rules)` returns a **new** `Innings`:
   `balls + 1`, `runs + 4`, striker's runs +4 and fours +1, bowler's runs +4. 4 is even, so no swap.
   If this was the 6th legal ball, the batters swap ends. The event is appended to `events`.
5. **Completion check.** `checkCompletion` asks: target reached? overs finished? all out? If so
   it marks `complete = true` with a reason.
6. **Phase change.** If it completed: innings 1 sets phase `INNINGS_BREAK`; innings 2 sets
   `COMPLETED` and computes the result text, then picks Player of the Match and files the match in
   history.
7. **Publish.** `_state.value = next`. Every screen watching `state` is told.
8. **Redraw.** Compose re-runs only what depends on the changed values: the score counts up to the
   new number, the batter's row updates, the over strip gets a new dot.
9. **Save.** The collector in `init` sees the new state and runs `store.save(it)`: the state is
   turned into JSON and written to DataStore in the background. If another ball arrives first,
   `collectLatest` cancels the stale write and saves the newer one.
10. **Navigate.** If the innings completed, `LaunchedEffect(innings?.complete)` calls
    `onComplete()` and the back stack is updated so Back does not return to the finished innings.

What the saved JSON roughly looks like (shortened):

```json
{"id":1760000000000,"teamA":"Tigers","teamB":"Lions","phase":"INNINGS_1",
 "innings1":{"battingTeam":"Tigers","runs":4,"balls":1,
   "events":[{"type":"runs","runs":4,"batter":0,"bowler":0}] }}
```

## 6. Undo, step by step

1. Scorer taps **Undo last ball** → `vm.undoLastBall(1)`.
2. `ScoringEngine.withoutLastBall` returns `null` if there are no events ("Nothing to undo" toast);
   otherwise `rebuild(innings, events.dropLast(1), rules)`.
3. `rebuild` creates a **fresh** innings with the same names and replays the remaining events
   with `fold`. Every number (runs, wickets, who is on strike, each player's figures) is
   recomputed from scratch, so nothing can be "slightly off".
4. Back in the ViewModel: the phase is reset to `INNINGS_1`/`INNINGS_2`, the result and Player of
   the Match are cleared, and if the match had already been **completed** it is removed from history.
5. The new state is published → screens redraw → it is saved.

**Why a wicket replays correctly:** when the scorer picked the replacement batter,
`selectNextBatter` wrote `nextBatter` *into the wicket event*. On replay, `applyEvent` sees
`event.nextBatter in waiting` and places that same batter without asking again.

**Why a bowling change replays correctly:** each event stores its own `bowler` index, so changing
the current bowler later does not re-credit earlier balls.

---

## 7. Design decisions and trade-offs

Good engineers can explain **why**, and also what they gave up.

| Decision | Why | What it costs |
|---|---|---|
| **Event log instead of stored totals** | Undo is exact and free; saved data is a true history; bugs in totals vanish | Replay costs a little CPU (an innings has at most a few hundred events, so it is instant) |
| **Immutable data (`copy`)** | No hidden changes; old states stay valid; easy to test | A new object per ball (cheap at this size) |
| **Engine has no Android code** | Tests run in milliseconds on a laptop; rules are reusable | One extra layer to learn |
| **Single `MatchState` saved as one JSON string** | Atomic: you can never save half a match; very simple code | Whole match rewritten each ball (a few KB, fine); not queryable |
| **History capped at 50 matches** | Keeps the one JSON string a sensible size | Old matches are dropped |
| **DataStore, not Room (a database)** | The data is small and always loaded whole | If you wanted per-player queries over thousands of matches, a database would be better |
| **Players matched by name** | No accounts needed; works offline | "Sam" and "sam" merge; two real Sams merge too |
| **Bowler stored on each event** | Undo stays exact after a bowler change | Slightly bigger events |
| **Next batter stored on the wicket event** | Replay needs no question to the scorer | The event is edited once after creation (by `selectNextBatter`) |
| **Compose, single Activity** | Less code than XML views; one `NavHost` | Newer library with a learning curve |
| **No internet permission, no analytics** | Privacy by design; nothing to leak | No cloud backup, no live sharing |
| **Share as an image with a QR code** | Works on Instagram, which strips links | The QR must be scanned from another screen |
| **Docs bundled as assets** | Readable offline inside the app and on GitHub | Two copies must be kept in sync (`scripts/sync-docs.sh`) |

---

## 8. Interview questions and answers

Short, honest answers you can say out loud. Practise them in your own words.

**Basics**

1. **What is Score247?** An Android app for scoring street cricket. You switch house rules on or off, tap one button per ball, and it keeps the score, stats, target and result. It saves everything offline, keeps match history and career stats, and can share a scorecard as an image.
2. **What is it built with?** Kotlin and Jetpack Compose with Material 3, a ViewModel with StateFlow, DataStore for saving, kotlinx.serialization for JSON, and Navigation-Compose.
3. **What is the architecture?** Layered: UI, ViewModel, engine, data. The UI only draws state and sends events. The ViewModel is the single place state changes. The engine holds pure cricket rules.
4. **What is a ViewModel and why use one?** An object that survives screen rotation and holds the data for the screens, so the match is not lost when the phone rotates.
5. **What is a `StateFlow`?** An observable holder that always has a current value. The UI collects it and redraws when it changes.

**The core idea**

6. **What is event sourcing and why use it?** Instead of storing totals, store the list of things that happened and compute totals by replaying. It gives exact undo and an honest history.
7. **How does undo work?** Replay all events except the last one into a fresh innings (`dropLast(1)`, `fold`). No special reverse code.
8. **Why immutable data classes?** They cannot be changed by accident; each ball produces a new innings, so old versions stay valid. That is what makes undo and testing simple.
9. **What is `fold`?** It combines a list into one value by feeding items one at a time into a running result. Here: start with an empty innings and apply each event.
10. **Why is `BallEvent` a sealed class?** The compiler knows every possible kind, so a `when` that misses one will not compile. Adding a new event type forces me to handle it everywhere.
11. **How do you undo a wicket exactly?** The chosen replacement batter is stored on the wicket event (`nextBatter`), so replay places the same person without asking again.
12. **Why store the bowler on each event?** Otherwise changing the bowler and then pressing undo would re-credit earlier balls to the wrong bowler.

**Cricket rules**

13. **How do you decide a ball is legal?** `isLegalDelivery`: runs, wickets, byes and leg-byes are legal; no-balls never; wides only when the house rule says so.
14. **How does the free hit work?** Find the latest event that is not a wide; if it was a no-ball, the free hit is active. Only a run-out may be recorded then.
15. **What is the target and why +1?** First innings runs plus one, because equal scores are a tie, not a win.
16. **How is "won by N wickets" worked out?** `maxWickets - wickets lost`. With "last batsman alone" on, the final batter counts as a wicket in hand.
17. **How is Player of the Match chosen?** A points system: runs plus a strike-rate bonus (min 3 balls), 25 per wicket plus an economy bonus (min one over), only from the winning side.

**State and saving**

18. **How does the app survive being killed?** Every state change is saved as JSON to DataStore. On start, it loads the file and `resumeRoute(phase)` reopens the right screen.
19. **Why `collectLatest` when saving?** If scoring is fast, an older write in progress is cancelled when a newer state arrives, so writes never queue up.
20. **Why `drop(1)`?** The first emitted value is what we just loaded; saving it again is pointless.
21. **What if the saved file is corrupt?** Decoding is wrapped in `try/catch` and treated as "no saved match", so the app starts fresh instead of crashing.
22. **What does `ignoreUnknownKeys` do?** It lets older or newer app versions read each other's saved files without crashing.
23. **Why `@SerialName` on events?** The saved name stays stable even if I rename the Kotlin class.

**Compose**

24. **What is recomposition?** Compose re-runs only the composables whose inputs changed, so the screen is a function of the state.
25. **`remember` vs `rememberSaveable`?** `remember` survives redraws; `rememberSaveable` also survives rotation and process death.
26. **What is `LaunchedEffect`?** A way to run side effects (like navigating) from a composable, tied to a key, and cancelled when it leaves the screen.
27. **What is `DisposableEffect` used for here?** Keeping the screen on during scoring, and *always* restoring it on exit.
28. **What is state hoisting?** Screens do not own the match data; they receive state and callbacks. It makes them easy to reuse and reason about.
29. **Why `LazyColumn`?** It only draws the rows that are visible, which matters for long lists.
30. **What are semantic colour tokens?** Colours named by role (`Ink`, `Pitch`) with a light and dark value, so screens never hard-code colours and dark mode is automatic.

**Sharing and security**

31. **How does image sharing work?** Draw a Bitmap with Canvas, save it to the private cache, expose it through a FileProvider with a temporary read permission, and open the system share sheet.
32. **Why a FileProvider?** Android forbids sharing raw file paths between apps. A FileProvider gives a temporary `content://` address for one file.
33. **Why a QR code?** Instagram removes captions and images cannot hide links, so a QR code lets viewers reach the GitHub page.
34. **How do you stop malicious markdown links?** The viewer only opens `https` links; other schemes are ignored.
35. **What privacy decisions did you make?** No internet permission, no accounts, no analytics. Data stays on the phone.

**Testing and quality**

36. **How did you test?** Unit tests for the engine, stats, formatter, parser and limits; a randomised test plays 400 innings under random rules and checks invariants and exact replay after every ball.
37. **Why can the engine be tested without a phone?** It has no Android imports; it is plain Kotlin.
38. **What is a regression?** A bug that breaks something that used to work. Tests exist to catch them.

**Reflection**

39. **What was the hardest bug?** Pick one from section 9. A good one: `popUpTo` with a concrete route matched nothing, so Back bounced users into a finished innings.
40. **What would you do next?** Animated (GIF/video) share, per-match charts (worm and manhattan), export to CSV, tablet layout, real screenshots and a Play Store listing.
41. **What would you change if you had many users?** Move history to Room (a database) and add optional cloud backup with encryption.
42. **What did you learn?** Make illegal states impossible, keep rules out of the UI, and make the data model do the hard work (event log).

---

## 9. Bugs that were found and fixed

These are great interview stories.

| Bug | Cause | Fix |
|---|---|---|
| Back from the summary jumped into the scoring screen | `popUpTo("scoring/1")` matched no route, so the screen stayed on the back stack | Use the route **pattern** `Routes.SCORING` |
| Next screen briefly showed the old match after "New match" | Storage was cleared first and the state reset afterwards | Reset the state synchronously; the collector saves it |
| Undo after a bowler change credited the wrong bowler | Bowler was taken from "current bowler" during replay | Each event stores its own `bowler` |
| Undo of a wicket asked for a new batter again | Replacement was not recorded on the event | `nextBatter` stored on the wicket event |
| Double vibration on run tiles | `RunTile` vibrated, and `score()` vibrated again | Removed the extra call |
| Match results could leak between matches | Setup edited the old state | `saveSetup` creates a brand-new `MatchState` |
| Duplicate history entries | Same match saved twice | History is keyed by match `id` and any older copy is removed first |
| Placeholder players merged into fake careers | "Player 3" in two matches looked like one person | `isRealName` skips placeholders |
| README overclaimed test coverage | Wording said "thousands" of innings | Corrected to match the real test (400) |
| Build errors: "conflicting RunTile / unresolved White" | Old duplicate `Components.kt` / `OtherScreens.kt` left in `ui/screens/` | Delete the old files |

---

## 10. Security and privacy

What the code does, and what it does **not** claim.

- **No network.** There is no code that talks to the internet. (Your manifest should not request
  the INTERNET permission. If it does, remove it.)
- **No accounts, no analytics, no ads.** Data lives only on the phone.
- **Input limits.** Names go through `Limits.cleanName` (control characters removed, 30 characters
  max) before they are saved, shown or drawn onto a picture.
- **Safe links.** The document viewer opens only `https` links, so a document cannot launch an
  unexpected app with a strange link.
- **Narrow file sharing.** The FileProvider exposes only `cache/shared/`, only one picture at a
  time, with a temporary read grant. Old pictures are deleted before a new one is made.
- **Defensive loading.** Corrupt or foreign saved data cannot crash the app (`try/catch`).
- **Backups.** Set `android:allowBackup="false"` in the manifest if you do not want Android to
  copy match data to cloud backups.
- **Dependencies.** Dependabot proposes library updates; CI builds and tests every push.

> **Honest limits.** No app can promise "no breaches". What this app does is reduce the attack
> surface to almost nothing: no server, no login, no network. Privacy and Terms documents are
> templates, not legal advice; have a lawyer review them before a commercial release.

---

## 11. Glossary

| Word | Plain-English meaning |
|---|---|
| **Activity** | One screen-sized window of an Android app. Here there is just one: `MainActivity`. |
| **Asset** | A file packed inside the app (the `.md` documents). |
| **Back stack** | The pile of screens you have visited; Back removes the top one. |
| **Bitmap** | A picture made of pixels in memory. |
| **Canvas** | A surface you draw shapes and text on in code. |
| **Collect** | Start listening to a Flow and react to each value. |
| **Composable** | A function marked `@Composable` that draws part of the screen. |
| **Composition / Recomposition** | Compose building the screen / rebuilding the parts whose data changed. |
| **Coroutine** | A lightweight background task that can pause without freezing the app. |
| **CI** | Continuous integration: a robot that builds and tests every push. |
| **DataStore** | Android's tool for saving small data on the phone. |
| **Data class** | A box of named values with free `copy()`, `equals` and `toString`. |
| **Edge-to-edge** | Drawing behind the status and navigation bars. |
| **Enum** | A fixed menu of allowed values. |
| **Event sourcing** | Storing what happened (events) and deriving the totals by replaying them. |
| **FileProvider** | A safe way to give another app temporary access to one file. |
| **Flow / StateFlow** | A stream of values over time / one that always remembers the latest. |
| **Fold** | Combine a list into a single value by feeding items one at a time. |
| **Gradle** | The build tool that compiles the app and downloads libraries. |
| **Immutable** | Cannot be changed after creation; you make a new copy instead. |
| **Index** | A position in a list, counting from 0. |
| **Intent** | A message asking Android to do something (open the share sheet). |
| **JSON** | Plain-text format for data, e.g. `{"runs":4}`. |
| **Lambda** | A small unnamed function, written `{ x -> ... }`. |
| **LazyColumn** | A scrolling list that only draws visible rows. |
| **Manifest** | `AndroidManifest.xml`, the app's ID card for Android. |
| **Material 3** | Google's design system and widget library. |
| **Modifier** | A chain of instructions (size, padding, click) attached to a composable. |
| **Null** | "There is nothing here." `String?` means it might be null. |
| **NavHost** | The container that shows the screen for the current route. |
| **Process death** | Android killing the app in the background to free memory. |
| **Regex** | A text-matching pattern. |
| **Route** | The text address of a screen, e.g. `scoring/1`. |
| **Sealed class** | A closed family of types so the compiler can check you handled every one. |
| **Serialization** | Turning an object into text (and back) so it can be saved. |
| **State** | The data that decides what the screen looks like. |
| **State hoisting** | Moving state up so a screen only receives data and callbacks. |
| **Striker / non-striker** | The batter facing the ball / the one waiting at the other end. |
| **Suspend function** | A function that can pause (for example while writing to disk). |
| **Token (design)** | A named design value (colour, shape) used everywhere instead of a raw number. |
| **Unit test** | A tiny program that checks one behaviour of the code. |
| **ViewModel** | An object that keeps screen data alive across rotation. |
| **WCAG AA** | An accessibility standard for text contrast (at least 4.5:1). |
| **ZXing** | A library that makes QR codes. |

---

*You do not need to memorise any of this. Understand the three big ideas (immutable data, event
log, one-way data flow) and you can derive the rest. Good luck.*
