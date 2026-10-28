# Contributing to Score247

Thank you for wanting to help! Whether you fix a typo or add a feature, it is welcome.
Please read the Code of Conduct first.

## Ways to help

- **Report a bug.** Open an issue with what you did, what you expected and what happened.
  A screenshot, your phone model and Android version help a lot.
- **Suggest an idea.** Open an issue and describe the problem it would solve.
- **Fix something.** Pick an issue (the ones labelled "good first issue" are a gentle start),
  say you are working on it, then send a pull request.
- **Improve the docs.** Clearer words are as valuable as new code.

## Setting up

1. Fork the repository and clone your fork.
2. Open the folder in **Android Studio** (a recent stable version) and let Gradle sync.
3. Run the app on an emulator or a phone (minimum Android 8.0, API 26).
4. Run the tests before and after your change:

```
./gradlew test
```

## How the code is organised

- `engine/` holds ALL the cricket rules. It is plain Kotlin with no Android code, so it is
  fast to test. If you change a rule, change it here and add a test.
- `data/` holds the models and saving.
- `viewmodel/` connects the screens to the engine and saving.
- `ui/` holds the screens and reusable pieces, with the colours in `ui/theme/Theme.kt`.

The file EXPLAIN.md explains the whole codebase step by step.

## Style

- Write comments for a beginner. Explain **why**, not only what.
- Colours come from the theme (`Ink`, `Pitch`, ...), never as raw hex values in screens.
- Keep buttons at least 48 dp tall so they are easy to tap.
- Keep functions small and give things clear names.

## Pull request checklist

- The project builds and `./gradlew test` passes.
- New behaviour has a test where possible (rules and statistics especially).
- The change is described in the pull request: what, why, and how you checked it.
- Screenshots are included for any visual change (light and dark mode if possible).
- You have added a line to CHANGELOG.md under "Unreleased".

## Commit messages

Short and clear, in the present tense: "Fix undo after a wicket on the last ball".

## Licence

By contributing you agree that your work is released under the MIT License.
