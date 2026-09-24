# LinguaFa AI

Persian-first Android English-learning app for IELTS, TOEFL and GRE, built with Kotlin, Jetpack Compose and Room.

## Vocabulary master banks

LinguaFa 1.1 uses one deduplicated vocabulary database with many-to-many pack membership. A word can belong to IELTS, TOEFL and GRE at the same time without creating duplicate vocabulary rows.

| Master bank | LinguaFa coverage target |
|---|---:|
| IELTS | 9,000 words |
| TOEFL | 7,000 words |
| GRE | 5,000 words |

These are **LinguaFa study-coverage targets**, not claims that the exam organizations publish official closed lists of exactly those sizes.

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

## Data model

- Room database version: 6
- `vocabulary_items`: one canonical row per vocabulary item
- `vocabulary_packs`: pack metadata and installed/target counts
- `vocabulary_pack_items`: many-to-many membership
- `vocabulary_dataset_chunks`: versioned bundled-import history
- independent SRS/mastery/favorites/progress remain stored locally

The v5 -> v6 migration preserves existing vocabulary and progress.

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

The GitHub Actions workflow at `.github/workflows/android-ci.yml` runs unit tests, Android lint, builds the debug APK, and uploads it as an artifact.

For release signing and environment details see [`docs/BUILD_AND_RELEASE.md`](docs/BUILD_AND_RELEASE.md).

## Version

Current app version: **1.1.0** (`versionCode = 2`).
