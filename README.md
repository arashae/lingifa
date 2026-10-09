# LinguaFa AI

Persian-first Android English-learning app for IELTS, TOEFL and GRE, built with Kotlin, Jetpack Compose and Room.

## Vocabulary master banks

LinguaFa 1.1 uses one deduplicated vocabulary database with many-to-many pack membership. A word can belong to IELTS, TOEFL and GRE at the same time without creating duplicate vocabulary rows.

| Master bank | Bundled offline rows |
|---|---:|
| IELTS | 5,335 words |
| TOEFL | 7,269 words |
| GRE | 7,504 words |

These are the counts actually shipped in `app/src/main/assets/vocabulary`. They are **LinguaFa study-coverage figures**, not claims that the exam organizations publish official closed lists of exactly those sizes.

Each exam bank includes curated phrasal verbs, phrasal prepositions and academic discourse expressions alongside single-word headwords. These are original LinguaFa editorial content: no upstream source supplies them, because ECDICT tags almost no multiword lemmas and Openjam contains no multiword lemma at all.

Every card carries syllabus topics, a target band and skill focus, so the app can group cards by topic or target a specific band.

### How a master bank is built

1. Built-in seed/chunk vocabulary is available immediately offline.
2. The app imports the exam-focused Openjam list first.
3. It expands the bank using Openjam CEFR/frequency data.
4. If a target still has a gap, it uses the Apache-2.0 supplemental Persian dictionary with exam-specific filtering and deterministic ranking.
5. Words are deduplicated by normalized English headword.
6. Actual `vocabulary_pack_items` membership count drives the installed/target progress bar.
7. When the target count is reached, the pack is marked complete and remains available offline in Room.

Downloads are resumable. Successfully stored words are not lost if the connection stops. To avoid upstream rate limits, the UI prevents multiple large master-bank downloads from running in parallel.

See [`docs/VOCABULARY_DATA_SOURCES.md`](docs/VOCABULARY_DATA_SOURCES.md) and [`THIRD_PARTY_NOTICES.md`](THIRD_PARTY_NOTICES.md) for source methodology and licenses.

## Exam Tracks

IELTS, TOEFL and GRE Exam Tracks now read from the actual Room master packs rather than only the original static demo seed. The original hand-curated cards remain as rich high-yield cards; downloaded vocabulary fills the rest of each track.

Each track is divided into four stages. A study session loads only the user's daily goal worth of not-yet-mastered words, so a 9,000-word bank does not become one enormous session.

## Learning and Review

Vocabulary Review uses pinned FSRS-6 memory states for each sense and skill: meaning, English retrieval, definition, context and lexical usage. Successful early practice preserves the spaced schedule; an observed lapse updates memory and brings relearning forward. One skill cannot postpone another due skill.

Hints are optional and recorded as guided practice. Valid alternatives leave the intended headword ungraded. The study and review pages share a curated 36-pair / 72-headword bank with original English definitions, contextual examples and Persian usage distinctions; production Review only pairs already-learned words.

The retention panel offers 85/90/95% targets and workload estimates, plus delayed independent recall and lexical confusion measurements as genuine history accumulates. These are targets and observations, not a guarantee of improved retention. See [learning engine and validation](docs/learning-engine.md).

## Data model

- Room database version: 12
- `vocabulary_items`: one canonical row per vocabulary item, including syllabus topic, target band, skill focus and pedagogical learning order
- `vocabulary_packs`: pack metadata and installed/target counts
- `vocabulary_pack_items`: many-to-many membership
- `vocabulary_dataset_chunks`: versioned bundled-import history
- `vocabulary_senses`: per-sense breakdown for multi-meaning words
- `vocabulary_skill_progress`: typed per-sense, per-skill FSRS memory and independent-success evidence
- `vocabulary_review_events`: immutable answer snapshots and pre-answer predictions
- `vocabulary_review_settings`: persisted desired retention
- SRS, progress, favorites and review histories remain stored locally

The v10 -> v11 migration adds the topic, band and skill-focus columns. The v11 -> v12 migration preserves existing study/pack data and dates, marks old heuristic states as legacy, and initializes FSRS from the next real assessment without inventing history.

## Build

The repository currently carries the Gradle wrapper properties but not the wrapper launcher/JAR, so a fresh clone should use an installed Gradle **9.3.1** rather than `./gradlew`.

Requirements used by CI:

- JDK 21 (required for Robolectric tests against Android API 36)
- Gradle 9.3.1
- Android SDK Platform 36.1
- Android Build Tools 36.0.0

Run the same quality gate used by CI:

```bash
gradle --no-daemon testDebugUnitTest lintDebug assembleDebug
```

Expected APK:

```text
app/build/outputs/apk/debug/app-debug.apk
```

The GitHub Actions workflow at `.github/workflows/android-ci.yml` runs content checks, unit tests, Android lint, stable signer/version checks and API 35 emulator review tests on a 360×640 dp display. It uploads the APK, UI reports and four verification screenshots.

For release signing and environment details see [`docs/BUILD_AND_RELEASE.md`](docs/BUILD_AND_RELEASE.md).

## Version

CI builds use **1.1.<workflow run number>**, with the same monotonically increasing version code and a stable debug signer, so a new APK can update the previous installation.
