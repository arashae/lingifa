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

`scripts/validate_vocabulary_quality.py` now emits human-review flags for placeholder definitions, missing example translations, examples without the target lemma/likely inflection, unreviewed POS labels, selected closed-class sense risks, unexpected scripts/encoding, General-bank rank zero, and early C2 outliers. These are advisory findings; they do not synthesize replacements or count as semantic correctness proof.
