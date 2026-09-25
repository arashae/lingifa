# General Core semantic review log

## Batch 1: cards 1–100

- File: `app/src/main/assets/vocabulary/general/general_core_001.jsonl`
- Reviewed in source order; retained existing lemma selection and frequency/order metadata unless a clear rank defect was visible.
- Corrected prominent sense, part-of-speech, definition, Persian gloss, example, and example-translation defects in high-frequency items.
- Examples and Persian translations revised in editorial wording; they are not copied from Cambridge, Oxford, or another dictionary.
- Existing `source` / `sourceLicense` fields were retained as legacy provenance for the upstream dataset and lemma selection. They do not claim that revised definitions, Persian glosses, or examples came from those sources.

## Batch 2: cards 101–200

- Same file and review order.
- Corrected prominent noun/verb/adjective confusions, rare-sense intrusions, and mismatched or unnatural examples/translations.
- Existing upstream provenance is retained only as inherited dataset metadata; revised lexical content is editorial.

## Semantic validator

`scripts/validate_vocabulary_quality.py` emits human-review flags for placeholder definitions, definition/POS conflicts, missing example translations, examples without the target lemma/likely inflection, obvious person or number conflicts between English and Persian examples, non-target collocations, unreviewed POS labels, selected closed-class sense risks, unexpected scripts/encoding, General-bank rank zero, high-frequency C2 outliers, and unsupported High exam relevance. These are advisory findings; they do not synthesize replacements or count as semantic correctness proof.

## Batch 3: cards 201–300

- Reviewed in source order and corrected high-confidence sense/POS errors and mismatched or awkward bilingual examples.
- Editorial definitions, glosses, examples, and translations remain distinct from carried-forward lemma/frequency provenance.

## Batch 4: cards 301–400

- Reviewed every card in source order and selected the highest-value general or academic sense for each entry.
- Corrected POS/sense conflicts, malformed definitions, foreign-script contamination, Persian gloss drift, and non-aligned bilingual examples.
- Added only high-confidence collocations and removed the stale `needs-collocation` tag from reviewed cards.
- Exam relevance remains editorial and conservative; no official IELTS, TOEFL, or GRE list membership was inferred.
- Lemma selection and frequency metadata remain sourced from NGSL 1.2 and inherited open datasets. CEFR-J 1.5 remains the carried level source, with selected sense-level levels cross-checked against Cambridge and Oxford learner references.
- Definitions, Persian glosses, examples, translations, and collocations in this batch were rewritten editorially and are not attributed to NGSL, NAWL, CEFR-J, Cambridge, Oxford, ECDICT, Openjam, or the Persian fallback dictionary.
