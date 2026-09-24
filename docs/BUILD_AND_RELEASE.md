# LinguaFa AI — Build and Release

This repository is configured so a debug APK can be built without committing any private API key or signing key.

## Requirements

- JDK 17
- Android SDK platform 36
- Android Build Tools 36.0.0
- Gradle wrapper from this repository

## Debug APK (installable)

```bash
./gradlew testDebugUnitTest assembleDebug
```

Output:

```text
app/build/outputs/apk/debug/app-debug.apk
```

Android's standard debug signing key is used automatically. Do not add a repository-local `debug.keystore`.

## GitHub manual APK workflow

Open **Actions → Android APK Build → Run workflow**.

The workflow runs unit tests, builds the debug APK, and uploads an artifact named:

```text
linguafa-debug-apk
```

If GitHub does not allocate a runner, this is an account/repository Actions execution issue rather than a Gradle test result. The workflow intentionally remains manual until runner execution is available.

## Gemini API key

Copy `.env.example` to `.env` locally and replace the placeholder:

```text
GEMINI_API_KEY=YOUR_KEY
```

`.env` is ignored by Git and must never be committed. The app is designed to fail gracefully or use offline content when a valid Gemini key is unavailable.

## Signed release APK / AAB

Release signing is provided through environment variables only:

```text
KEYSTORE_PATH=/absolute/path/to/my-upload-key.jks
STORE_PASSWORD=...
KEY_PASSWORD=...
```

The configured key alias is:

```text
upload
```

Build a signed release APK:

```bash
./gradlew assembleRelease
```

Build a signed Android App Bundle:

```bash
./gradlew bundleRelease
```

Outputs normally appear under:

```text
app/build/outputs/apk/release/
app/build/outputs/bundle/release/
```

Never commit `.jks`, `.keystore`, `.env`, passwords, API keys, or generated signing credentials.

## Vocabulary dataset checks

Bundled master vocabulary data is stored as versioned JSONL chunks under:

```text
app/src/main/assets/vocabulary/
```

The manifest is:

```text
app/src/main/assets/vocabulary/master_catalog.json
```

`BundledVocabularyCatalogTest` checks that each registered chunk matches its declared item count, contains non-empty English words and Persian meanings, and has no duplicate word inside the same chunk.

The master-bank targets (IELTS 9000, TOEFL 7000, GRE 5000) are targets, not claims that every word is already installed. UI progress is based on actual pack memberships stored in Room.
