# Score247 — code review & improvement notes

## Bugs found (and fixed)

| # | Bug | Impact | Fix |
|---|-----|--------|-----|
| 1 | `rebuild()` (undo) created innings with `nonStrikerIndex = 0` | After any undo both batters were player 0 | Undo replays the event log from a correct opening pair |
| 2 | Engine ignored `event.bowler`; used the *current* bowler | Undo re-credited every earlier ball to the bowler selected at that moment | Bowler credit comes from the event |
| 3 | Next-batter choice was never stored in the log | Undo after a wicket lost who came in | Choice is recorded on the `Wicket` event |
| 4 | Wicket on 6th ball: over-end swap, then `selectNextBatter` overwrote the *striker* slot | Surviving batter vanished from the crease | New batter fills whichever slot holds the dismissed player |
| 5 | `calcResult` used `playersPerTeam - 1` | "Won by 0 wickets" when last-batsman-alone applied | `rules.maxWickets` |
| 6 | "New Match" from the *Last match* card didn't reset state | Old innings, result and Player of the Match leaked into the new match | `saveSetup` builds a fresh `MatchState` |
| 7 | `newMatch()` cleared storage first, reset state after | Starting a new match from the Toss phase bounced back to the Toss | State reset is synchronous |
| 8 | `popUpTo("scoring/1")` (concrete route) | Matches nothing → scoring screen stayed on the back stack; Back from the summary re-entered it | `popUpTo(Routes.SCORING)` (the pattern) |
| 9 | "This over" showed the last 6 *legal balls*, not the current over | Mid-over it mixed in the previous over | `ScoringEngine.currentOver()` |
| 10 | Wide counted as a legal ball but not for the bowler; no over-end strike change | Bowler stats / strike wrong under that rule | Handled for all legal-ball cases |
| 11 | Runs run on a wide never changed strike | Wrong striker after "wide + 1" | Odd extras swap ends |
| 12 | Fours/sixes off a no-ball weren't counted | Boundary tally low | `facedBall()` |
| 13 | Every wicket credited to the bowler | Run-outs inflated bowling figures | `DismissalKind.creditsBowler` |
| 14 | Lone last batter shown twice; `BatterRow` marked both | Cosmetic but confusing | Shown once |
| 15 | `battingTeamFor(2)` fell back to the toss *winner* | Wrong team if the winner chose to bowl and innings 1 was missing | Falls back using the toss choice |
| 16 | Player of the Match computed but never displayed; candidates from either team | Dead feature | Shown on the summary; winning side only |
| 17 | Squad names re-entered for innings 2 | 12 extra names; stats could mismatch by position | Squads carry over |
| 18 | Last ball of an innings/match could not be undone | A mis-tap on the final ball was permanent | Undo available on Innings-break and Summary |
| 19 | Targeting SDK 35 without edge-to-edge handling | Android 15 draws content under the status bar | `enableEdgeToEdge()` + insets |
| 20 | `Ink300`/amber/header-dim text at 2.6–2.9:1 contrast | Fails WCAG AA | New palette, all ≥ 4.5:1 |
| 21 | Dead/duplicate code: second `RunTile`, `BallDot`, `ScoringScreen`, `requiredRate` lambda (also mathematically wrong) | Won't compile if pasted together | Removed |
| 22 | `runCatching` around DataStore swallowed `CancellationException` | Breaks cooperative cancellation | Explicit try/catch that rethrows |

## Deliberate conventions (documented, easy to change)
- **Run-out crossing:** if the batters completed an odd number of runs they are treated as having crossed *before* the dismissal; the new batter fills the dismissed player's slot.
- **Free hit:** only a run-out can dismiss; wides in between don't cancel it.
- **Byes / leg-byes** are not charged to the bowler; **wides and no-balls** are.

## Not verified
- I could compile and run the pure-Kotlin layer (models, engine, formatter) with your Kotlin version and run all tests. I could **not** build the Android project (no Maven/Google repos reachable), so the Compose UI files have been syntax-checked but not compiled or run on a device.
- `gradle-wrapper.properties` pins Gradle 9.2.1 with AGP 8.7.0. If your build works, fine — but check Google's AGP/Gradle compatibility table before publishing the repo.
- Compose BOM `2024.06.00` is old; bump it (and re-test) when convenient.

## Roadmap — what actually moves the needle
1. **Match history + stats** (Room): the single biggest gap vs. real scoring apps.
2. **Screenshots / a short GIF in the README** — a recruiter decides in 10 seconds.
3. **CI** (GitHub Actions: `./gradlew test assembleDebug`) + a badge.
4. Fall-of-wickets, super over on a tie, retired batter, penalty runs.
5. Urdu localisation + RTL check; tablet/landscape layout.
6. A distinctive display typeface for the score digits (downloadable Google Font).
7. UI tests for the scoring flow (Compose test rule) + a couple of `@Preview`s.
