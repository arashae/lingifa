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

Current bundled counts:

| Pack | Bundled rows | Exam-focused core source |
|---|---:|---|
| IELTS Master | 5,335 | Openjam IELTS/AWL 570 |
| TOEFL Master | 7,269 | Openjam TOEFL 2,632 |
| GRE Master | 7,504 | Openjam GRE 3,036 |

Construction order is deliberately deterministic:

1. import the exam-focused Openjam list;
2. expand with selected Openjam CEFR/frequency bands;
3. fill only the remaining gap from the Apache-2.0 supplemental dictionary;
4. stop at the target count;
5. deduplicate globally so one `VocabularyItem` can belong to IELTS, TOEFL and GRE simultaneously.

A target is marked downloaded/complete only when the actual `vocabulary_pack_items` count reaches that target. UI progress is therefore based on installed database membership, not a hard-coded marketing number.

### 3a. Offline generation pipeline

`.github/workflows/vocabulary-bundle.yml` is the authoritative order. The stages
after `apply_vocabulary_overrides.py` are order-sensitive:

| Stage | Role |
|---|---|
| `build_vocabulary_assets.py` | regenerate chunks from ECDICT + Openjam + Persian fallback |
| `postprocess_vocabulary_meanings.py` | resolve placeholder Persian meanings; fails if any remain |
| `sanitize_vocabulary_assets.py` | drop template collocations, down-rank C2 names/places to `learningOrder >= 100000` |
| `curate_vocabulary_sources.py` | build the General Core pack and tag CEFR-J/NGSL/NAWL provenance |
| `apply_vocabulary_overrides.py` | overlay reviewed IELTS/TOEFL rows by headword |
| `normalize_definitions.py` | split raw sense dumps, promote the sense matching the card's POS, add missing terminal punctuation |
| `apply_curated_row_corrections.py` | apply hand-authored content fixes, keeping the primary sense in step |
| `recalibrate_cefr_levels.py` | cap over-inflated `C2` labels |
| `apply_curated_additions.py` | append curated multiword cards, re-chunk, update the catalog |
| `tag_exam_topics.py` | assign syllabus topic, target band and skill focus |
| `reorder_and_enrich_pedagogy.py` | 4 pedagogical stages plus `learningOrder` |
| `validate_vocabulary_quality.py` | strict gate on every IELTS and TOEFL chunk |

Why that order:

- Definition normalisation runs **before** the curated corrections, so a
  correction that rewrites a card's definition can also update that card's
  primary sense. The reverse order leaves the app showing two different
  definitions for the same word.
- Calibration runs **after** the overrides so a reviewed row cannot restore a
  stale CEFR label.
- Additions run **before** topic tagging and ordering so curated cards receive
  the same metadata as generated cards.
- Ordering runs **last** so every row, including additions, gets a
  `learningOrder`. It preserves the `learningOrder >= 100000` sentinel that the
  sanitizer assigns to C2 names and places; overwriting it would silently undo
  that down-ranking.

### 3b. Curated multiword content

The bundled exam banks ship 295 original multiword entries each: phrasal verbs
(`carry out`, `give rise to`), phrasal prepositions (`in the wake of`, `at the
expense of`) and academic discourse expressions (`there is growing evidence
that`).

These **cannot** be harvested from upstream sources. ECDICT tags only 2 IELTS
and 4 TOEFL multiword lemmas, and Openjam contains no multiword lemma at all.
Every Persian meaning, example sentence and translation is therefore original
LinguaFa editorial content, authored in `scripts/curated_multiword_catalog.py`
and its `multiword_cards_*.py` modules.

Because that content is original, it is **not** attributable to ECDICT,
Openjam, NGSL, NAWL, CEFR-J, Cambridge or Oxford. Rows created by this stage are
tagged `curated-multiword` and carry `sourceLicense = "Original LinguaFa
editorial content"` so the provenance stays separable from derived data.

### 3c. CEFR calibration

Openjam's `level` field is not calibrated: 16,321 of its 20,602 lemmas (79%)
carry `C2`, including 218 NAWL words and 39 NGSL words, which are by
construction below C2. Inherited unmodified, this labelled 44% of the TOEFL
bank as C2.

`recalibrate_cefr_levels.py` caps `C2` using two signals already attached to each
row: list membership (`NGSL` caps at B2, `NAWL` caps at C1) and the ECDICT
frequency rank. Levels A1-C1 are left untouched, because C2 is the only level
that is demonstrably saturated. The CEFR level packs are deliberately not
re-bucketed; that is a catalogue restructure rather than exam-bank quality work.

## 4. Content-quality audit

A committed audit of the 12,014 generated IELTS and TOEFL rows found four defect
classes. Three are now fixed in the pipeline; one is a tracked backlog.

| Defect | Before | After | Fix |
|---|---:|---:|---|
| Definitions leaking raw multi-sense dictionary dumps (`n. ...`, `v. ...`) | 990 | 0 | `normalize_definitions.py` |
| Definitions without terminal punctuation | 10,082 | 0 | `normalize_definitions.py` |
| Part-of-speech tag contradicting the meaning | 42 | 0 | `curated_row_corrections.py` |
| Examples that are dictionary citations, not sentences | 494 | 494 | `curated_row_corrections.py` |

Splitting the sense dumps also delivered multi-sense support: 4,056 rows across
all banks (489 in the exam banks) now carry a `senses` array, which the importer
loads into `vocabulary_senses`. The sense matching the card's part of speech is
marked primary, so an adjective card is no longer defined as a verb.

### Remaining backlog

Both remaining classes need hand-written bilingual sentences, because no upstream
source supplies them: Openjam's English and Persian example fields are not
sentence-aligned with each other, so they cannot be paired safely.

- **494 rows** whose example is a dictionary citation. Reported as an
  **advisory** finding in `quality_report.json` and deliberately excluded from
  `--strict`, so the pipeline is not blocked by work that is already counted and
  scheduled. The count is tracked so the backlog cannot silently grow.
- **247 rows** whose English example is a bare phrase while the Persian
  "translation" is a full unrelated sentence. The validator's alignment
  heuristics check grammatical person and numerals, not meaning, so this class is
  not machine-detectable and needs hand review.

## 5. Offline behavior

Downloaded vocabulary is stored in the local Room database. Network access is required only to install/update remote master-bank content. Study, filtering, favorites, SRS progress and review continue offline after download.

## 5. Attribution and redistribution

Keep this file with distributed source code and retain upstream license notices when redistributing source-derived data. Do not replace these sources with proprietary commercial word lists unless redistribution rights are explicitly obtained.
