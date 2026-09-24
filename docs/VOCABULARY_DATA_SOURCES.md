# LinguaFa vocabulary data sources

LinguaFa separates **source lists** from **LinguaFa master-bank targets**.

The app does **not** claim that IELTS, TOEFL, or GRE publish an official closed vocabulary list of exactly 9,000 / 7,000 / 5,000 words. Those numbers are LinguaFa coverage targets used to build broad, practical study banks.

## 1. Openjam — primary exam/core and CEFR source

Repository: `amirj4m/openjam`

License: **MIT** (data and code).

LinguaFa uses the public, unauthenticated Openjam REST API and its book layer. Openjam provides English lemmas, CEFR levels, frequency ranks, Persian sense translations, examples and (where present) IPA/phonetics.

Exam-focused source lists exposed by Openjam at the time this integration was written:

- IELTS / Academic Word List: **570 headwords**. Openjam attributes this to Averil Coxhead's Academic Word List and states that definitions/Persian translations are original Openjam data.
- TOEFL Essential Vocabulary: **2,632 words**. Openjam attributes the source list to the public `JIHUNJO123/toefl-prep-vocabulary` repository and states that definitions/Persian translations are original Openjam data.
- GRE 3000 Vocabulary: **3,036 words**. Openjam attributes the source list to the public `Dramalf/GRE3000-cli` repository and states that definitions/Persian translations are original Openjam data.

The downloader imports those exam-focused lists first, then expands using Openjam's CEFR/frequency-ranked vocabulary. Imported rows carry `source`, `sourceLicense`, `datasetVersion`, `frequencyRank`, exam relevance and pack membership metadata.

Openjam API used by LinguaFa:

- `https://openjam.amirj4m.com/v1/books/{slug}/words?lang=fa&limit=500&offset=...`
- `https://openjam.amirj4m.com/v1/words?level={CEFR}&lang=fa&limit=500&offset=...`

Openjam's published API is rate-limited. LinguaFa imports pages incrementally and keeps all successful rows in Room, so interrupted downloads are resumable.

## 2. VahidN/EnglishToPersianDictionaries — supplemental Persian dictionary

Repository: `VahidN/EnglishToPersianDictionaries`

License: **Apache License 2.0**.

LinguaFa uses only the `essential-english-words-2` dictionary as a supplemental source when Openjam coverage is not enough to reach a master-bank target. The upstream README reports **17,117 entries** in this dictionary.

The app downloads the upstream A–Z JSON slices from `raw.githubusercontent.com`, then applies LinguaFa filtering before a word can enter a master exam bank:

- single English headword only;
- no proper-name capitalization;
- clean Latin headword pattern;
- minimum length and exam-specific filtering;
- deterministic exam-oriented scoring using academic/advanced morphology;
- duplicate elimination by normalized headword;
- stop-word exclusion;
- membership capped exactly at the configured master-bank target.

Supplemental rows are marked with `sourceLicense = Apache-2.0`.

## 3. Master-bank construction

Current LinguaFa targets:

| Pack | Target | Exam-focused core source |
|---|---:|---:|
| IELTS Master | 9,000 | Openjam IELTS/AWL 570 |
| TOEFL Master | 7,000 | Openjam TOEFL 2,632 |
| GRE Master | 5,000 | Openjam GRE 3,036 |

Construction order is deliberately deterministic:

1. import the exam-focused Openjam list;
2. expand with selected Openjam CEFR/frequency bands;
3. fill only the remaining gap from the Apache-2.0 supplemental dictionary;
4. stop at the target count;
5. deduplicate globally so one `VocabularyItem` can belong to IELTS, TOEFL and GRE simultaneously.

A target is marked downloaded/complete only when the actual `vocabulary_pack_items` count reaches that target. UI progress is therefore based on installed database membership, not a hard-coded marketing number.

## 4. Offline behavior

Downloaded vocabulary is stored in the local Room database. Network access is required only to install/update remote master-bank content. Study, filtering, favorites, SRS progress and review continue offline after download.

## 5. Attribution and redistribution

Keep this file with distributed source code and retain upstream license notices when redistributing source-derived data. Do not replace these sources with proprietary commercial word lists unless redistribution rights are explicitly obtained.
