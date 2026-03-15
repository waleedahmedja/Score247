# Contributing to Score247

First off — thank you. Score247 is built for street cricket players everywhere, and contributions from the community make it better for all of them.

---

## Before You Contribute

Please read the following:

- [LICENSE.md](LICENSE.md) — All contributions are subject to the Score247 Community Source License (SCSL v1.0)
- [CODE_OF_CONDUCT.md](CODE_OF_CONDUCT.md) — All contributors are expected to follow it

By submitting a contribution, you agree that your work will be licensed under SCSL v1.0 and that you have the right to submit it.

---

## What You Can Contribute

- 🐛 **Bug fixes** — especially around scoring logic, undo behavior, or persistence
- 🎨 **UI/UX improvements** — consistent with the app's Apple × Braun design philosophy
- 🧠 **Rule engine features** — new gully cricket rule variations
- 📱 **Performance improvements** — startup time, recomposition efficiency, memory
- 📄 **Documentation** — README, inline comments, architecture notes
- 🌐 **Localization** — translations (Urdu, Hindi, and other cricket-playing regions are a priority)

---

## What to Avoid

- Adding internet permissions, analytics, or tracking of any kind
- Third-party SDKs that conflict with the offline-first philosophy
- Features that are irrelevant to informal/street cricket
- Removing or altering the `waleedahmedja` credit in any screen or file

---

## Tech Stack

Score247 uses:

- **Kotlin 2.0** + **Jetpack Compose** + **Material 3**
- **MVVM** architecture with a single `StateFlow`-backed `MatchViewModel`
- **DataStore** for persistence (no Room, no database migrations)
- **kotlinx.serialization** for JSON encoding/decoding
- No internet, no ads, no accounts

All contributions should stay consistent with this stack. Do not introduce new dependencies without discussion.

---

## How to Submit a Contribution

1. **Fork** the repository on GitHub
2. **Create a branch** — use a descriptive name like `fix/undo-wide-ball` or `feature/urdu-localization`
3. **Make your changes** — keep commits small and focused
4. **Test on a real device** — especially scoring edge cases (last wicket, target chase, undo chains)
5. **Open a Pull Request** — describe what you changed and why

---

## Pull Request Guidelines

- Reference any related issue in the PR description
- Keep changes focused — one feature or fix per PR where possible
- Include a brief description of how you tested the change
- Do not modify `LICENSE.md` or remove author credits

---

## Reporting Bugs

Open a GitHub Issue and include:

- Android version and device (or emulator spec)
- Steps to reproduce the bug
- What you expected vs. what happened
- Any relevant screenshots or logs

---

## Feature Suggestions

Open a GitHub Issue with the label `enhancement`. Describe:

- What gully cricket scenario it addresses
- How it would work from the user's perspective
- Why existing behavior doesn't cover it

---

## Questions?

Open an issue or reach out via GitHub. All questions are welcome.

Street cricket belongs to everyone. So does this project.

— **waleedahmedja**
