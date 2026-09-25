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

## Batch 5: cards 401–450

- Reviewed every card in source order and selected common learner senses over rare dictionary, technical, or text-reference senses.
- Corrected POS/sense conflicts, WordNet-style fragments, empty closed-class cards, Persian gloss drift, and non-aligned bilingual examples.
- Added only high-confidence collocations and removed stale `needs-collocation` tags.
- Preserved inherited frequency, learning-order, and upstream provenance fields; revised lexical wording is editorial and is not attributed to those sources.

## IELTS priority review: cards 1–2,000

- Reviewed `ielts_core_001.jsonl` through `ielts_core_004.jsonl` card by card for one deliberate IELTS-relevant sense per entry.
- Corrected POS/sense conflicts, rare WordNet senses, malformed definitions, Persian encoding and gloss defects, missing examples, and non-aligned English/Persian examples.
- Added corpus-natural collocations; strict validation reports zero findings and zero errors across all 2,000 cards with 100% example coverage.
- Existing source, license, and frequency fields were preserved. Revised definitions, examples, Persian translations, and collocations are editorial and are not attributed to Cambridge, Oxford, NGSL, NAWL, or upstream datasets.

## IELTS priority review: cards 2,001–4,000

- Reviewed `ielts_core_005.jsonl` through `ielts_core_008.jsonl` card by card and restored the required `novice` lemma with the correct beginner sense.
- Corrected POS/sense conflicts, malformed definitions, Persian defects, missing examples, and non-aligned bilingual examples.
- Added sense-specific collocations while leaving only reviewed closed-class or low-value exceptions; strict validation reports zero findings and zero errors across all 2,000 cards with 100% example coverage.
- Existing source, license, and frequency fields were preserved. Revised learner content is editorial and is not attributed to external dictionaries or upstream datasets.

## IELTS priority review: cards 4,001–5,040

- Reviewed `ielts_core_009.jsonl` through `ielts_core_011.jsonl` card by card, including correction of the malformed lemma `wreathe` to `wreath`.
- Corrected POS/sense conflicts, definitions, Persian content, bilingual examples, collocations, relevance, tags, and metadata while preserving inherited provenance.
- Strict validation reports zero findings and zero errors across the final 1,040 IELTS cards with 100% example coverage.

## Batch 7: `general_core_002` cards 1–50

- Reviewed every card in source order and replaced rare, technical, or mismatched senses with one deliberate general or academic sense per card.
- Corrected POS conflicts, dictionary fragments, empty closed-class cards, Persian gloss drift, and non-aligned bilingual examples.
- Added only high-confidence collocations and removed stale `needs-collocation` tags.
- Preserved inherited frequency, learning-order, and upstream provenance fields; revised lexical wording is editorial and is not attributed to those sources.

## Batch 6: cards 451–500

- Reviewed every card in source order and selected dominant learner senses over rare gaming, anatomy, technical, or positional senses.
- Corrected POS/sense conflicts, empty reflexive-pronoun cards, dictionary fragments, Persian gloss drift, and non-aligned bilingual examples.
- Added only high-confidence collocations and removed stale `needs-collocation` tags.
- Preserved inherited frequency, learning-order, and upstream provenance fields; revised lexical wording is editorial and is not attributed to those sources.
