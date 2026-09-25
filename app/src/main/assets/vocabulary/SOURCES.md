# Bundled vocabulary sources

LinguaFa bundles versioned JSONL vocabulary directly in the Android APK. The app does not need network access to review these cards.

## Primary open sources

- **ECDICT** (`skywind3000/ECDICT`) — MIT. Exam membership, definitions, pronunciation, POS and frequency metadata.
- **Openjam** (`amirj4m/openjam`) — MIT. Persian meanings, examples, IPA and CEFR metadata.
- **EnglishToPersianDictionaries** (`VahidN/EnglishToPersianDictionaries`) — Apache-2.0. Persian fallback meanings.
- **NGSL 1.2** (`newgeneralservicelist.com`) — CC BY-SA 4.0. 2,809 high-frequency general-English lemmas and learner definitions.
- **NAWL 1.2** (`newgeneralservicelist.com`) — CC BY-SA 4.0. 957 academic-English lemmas and learner definitions.
- **CEFR-J Vocabulary Profile 1.5** (`openlanguageprofiles/olp-en-cefrj`) — permitted for research and commercial use with proper citation; used as an independent CEFR signal.

The General Core pack is the union of NGSL and NAWL. A source word is bundled only when the card has a Persian meaning; this keeps the app from showing empty flashcards. Exam packs remain exam-tagged ECDICT banks, enriched with the open general/academic metadata when a word overlaps.

The IELTS, TOEFL and GRE providers do not publish one official finite master vocabulary list. Counts in the app therefore describe the pinned open datasets, not an official guarantee of test coverage.
