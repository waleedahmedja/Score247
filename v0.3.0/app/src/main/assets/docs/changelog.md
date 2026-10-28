# Changelog

All notable changes are listed here. The format follows Keep a Changelog, and the project uses
semantic versioning.

## Unreleased

## 1.1.0 - 2026-10-25

### Added
- Match history: every finished match is saved and can be reopened, shared or deleted.
- Player statistics: batting and bowling leaderboards across all saved matches.
- Share as image: a story-sized scorecard picture with a QR code to the project page.
- Share as text now ends with the project link.
- One-tap Wide and No ball buttons on the scoring screen.
- Setup remembers the last match's team names and house rules; squads remember player names.
- About and legal section inside the app (privacy, terms, licence and more).
- The screen stays awake while scoring.
- The app icon (stumps over a scoreboard, with a cricket ball) is now the logo on the home
  screen, the About screen and the shared scorecard picture.
- Security hardening: player and team names are cleaned and limited to 30 characters; links
  open only as http(s); a weekly dependency check and a build-and-test workflow were added.

### Fixed
- Undo restored both batters to the same position and re-credited every ball to the current
  bowler.
- A wicket on the last ball of an over lost the surviving batter.
- "Won by 0 wickets" in results.
- Back from the summary bounced into the scoring screen.
- Low-contrast text and icons in both themes (now WCAG AA).
- Typed squad names were lost when the phone was rotated.

## 1.0.0

- First version: setup, toss, live scoring with house rules, innings break, summary and
  Player of the Match.
