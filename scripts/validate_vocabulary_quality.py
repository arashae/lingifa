#!/usr/bin/env python3
"""Quality gate for bundled LinguaFa vocabulary assets.

Hard failures are limited to defects that should never ship: malformed rows, missing required
bilingual/English data, invalid CEFR values, duplicate headwords inside a study bank, leftover
synthetic-collocation families, and obvious C2 names/places that were not down-ranked.
Soft coverage metrics (examples, IPA, collocations) are written to quality_report.json.
"""

from __future__ import annotations

import json
from collections import Counter, defaultdict
from pathlib import Path

from sanitize_vocabulary_assets import (
    VOCAB_ROOT,
    generated_templates,
    looks_like_c2_proper_noun,
    norm,
)

VALID_CEFR = {"A1", "A2", "B1", "B2", "C1", "C2"}
REPORT_PATH = VOCAB_ROOT / "quality_report.json"


def bank_key(path: Path) -> str:
    rel = path.relative_to(VOCAB_ROOT)
    parts = rel.parts
    if not parts:
        return "unknown"
    if parts[0] == "cefr" and len(parts) >= 2:
        return f"cefr/{parts[1]}"
    return parts[0]


def is_generated_family(row: dict) -> bool:
    collocations = [str(value).strip() for value in row.get("collocations", []) if str(value).strip()]
    if not collocations:
        return False
    templates = generated_templates(row.get("word", ""), row.get("partOfSpeech", ""))
    matches = sum(norm(value) in templates for value in collocations)
    return matches >= 2 and matches * 2 >= len(collocations)


def main() -> None:
    errors: list[str] = []
    counts = Counter()
    per_bank_words: dict[str, set[str]] = defaultdict(set)
    duplicate_samples: list[str] = []
    example_missing_samples: list[str] = []
    definition_missing_samples: list[str] = []
    proper_noun_samples: list[str] = []

    files = sorted(VOCAB_ROOT.rglob("*.jsonl"))
    for path in files:
        bank = bank_key(path)
        for line_no, raw_line in enumerate(path.read_text(encoding="utf-8").splitlines(), start=1):
            line = raw_line.strip()
            if not line or line.startswith("#"):
                continue
            try:
                row = json.loads(line)
            except json.JSONDecodeError as exc:
                errors.append(f"{path}:{line_no}: malformed JSON: {exc}")
                continue

            counts["rows"] += 1
            word = str(row.get("word", "")).strip()
            normalized_word = norm(word)
            meaning = str(row.get("persianMeaning", "")).strip()
            definition = str(row.get("englishDefinition", "")).strip()
            example = str(row.get("example", "")).strip()
            cefr = str(row.get("cefrLevel", "")).strip().upper()

            if not word:
                errors.append(f"{path}:{line_no}: empty word")
                continue
            if not meaning:
                errors.append(f"{path}:{line_no}: {word}: missing Persian meaning")
            if not definition:
                errors.append(f"{path}:{line_no}: {word}: missing English definition")
                if len(definition_missing_samples) < 20:
                    definition_missing_samples.append(f"{bank}:{word}")
            if cefr not in VALID_CEFR:
                errors.append(f"{path}:{line_no}: {word}: invalid CEFR '{cefr}'")

            if normalized_word in per_bank_words[bank]:
                counts["duplicates"] += 1
                if len(duplicate_samples) < 20:
                    duplicate_samples.append(f"{bank}:{word}")
            else:
                per_bank_words[bank].add(normalized_word)

            if example:
                counts["with_example"] += 1
            elif len(example_missing_samples) < 20:
                example_missing_samples.append(f"{bank}:{word}")

            if str(row.get("ipa", "")).strip():
                counts["with_ipa"] += 1
            if row.get("collocations"):
                counts["with_collocations"] += 1

            if is_generated_family(row):
                errors.append(f"{path}:{line_no}: {word}: synthetic collocation family remains")

            if looks_like_c2_proper_noun(row):
                counts["c2_suspicious_names_places"] += 1
                tags = {str(tag).strip() for tag in row.get("tags", [])}
                order = int(row.get("learningOrder") or 0)
                if "low-study-priority" not in tags or order < 100_000:
                    errors.append(f"{path}:{line_no}: {word}: C2 name/place not down-ranked")
                elif len(proper_noun_samples) < 20:
                    proper_noun_samples.append(word)

    # Duplicates are a hard defect only within the same learning bank; cross-bank membership is valid.
    if counts["duplicates"]:
        errors.append(f"Found {counts['duplicates']} duplicate headword rows inside study banks")

    rows = counts["rows"]
    report = {
        "rows": rows,
        "files": len(files),
        "coverage": {
            "examples": round(counts["with_example"] / rows, 4) if rows else 0,
            "ipa": round(counts["with_ipa"] / rows, 4) if rows else 0,
            "collocations": round(counts["with_collocations"] / rows, 4) if rows else 0,
        },
        "c2SuspiciousNamesPlacesDownranked": counts["c2_suspicious_names_places"],
        "duplicateCount": counts["duplicates"],
        "samples": {
            "missingExamples": example_missing_samples,
            "missingDefinitions": definition_missing_samples,
            "duplicates": duplicate_samples,
            "c2NamesPlaces": proper_noun_samples,
        },
        "errorCount": len(errors),
        "errors": errors[:100],
    }
    REPORT_PATH.write_text(json.dumps(report, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    print(json.dumps(report, ensure_ascii=False, indent=2))

    if errors:
        raise SystemExit(f"Vocabulary quality gate failed with {len(errors)} error(s).")


if __name__ == "__main__":
    main()
