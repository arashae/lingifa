#!/usr/bin/env python3
"""Apply curated row corrections to the IELTS and TOEFL banks.

Corrects learner-visible content that a downstream stage cannot fix on its own:
part-of-speech tags that contradict the meaning, definitions that describe a
different sense from the Persian meaning and example, and examples that were
generated as dictionary citations rather than real sentences.

Corrections are applied in place, so unlike apply_curated_additions.py this stage
does not re-chunk the bank or touch master_catalog.json: the row count never
changes.

Re-running is safe. Each correction is idempotent, and the stage reports how
many rows it actually rewrote so a second run shows zero.
"""

from __future__ import annotations

import json
from pathlib import Path

from curated_row_corrections import CORRECTIONS, validate_corrections

ROOT = Path(__file__).resolve().parents[1]
ASSET_ROOT = ROOT / "app" / "src" / "main" / "assets" / "vocabulary"
BANKS = ("ielts", "toefl")

# Marks a row as editorially corrected so later stages and reviewers can see it.
CORRECTION_TAG = "corrected-content"


def apply_to_row(row: dict, fix: dict) -> bool:
    """Apply one correction to a row in place. Returns True when it changed."""
    before = json.dumps(row, ensure_ascii=False, sort_keys=True)
    for field in ("definition", "example", "examplePersian", "pos_fix", "fa_fix"):
        value = fix.get(field)
        if not value:
            continue
        if field == "definition":
            row["englishDefinition"] = value
        elif field == "example":
            row["example"] = value
        elif field == "examplePersian":
            row["examplePersian"] = value
        elif field == "pos_fix":
            row["partOfSpeech"] = value
        elif field == "fa_fix":
            row["persianMeaning"] = value
    tags = {str(t) for t in row.get("tags", [])}
    if tags.difference({CORRECTION_TAG}):
        tags.add(CORRECTION_TAG)
        row["tags"] = sorted(tags)

    # Keep the primary sense in step with the card. Without this, a row that
    # already carried a `senses` array would keep the pre-correction wording in
    # its primary sense, and the app would show two different definitions for the
    # same word depending on which screen the learner opened.
    senses = row.get("senses")
    if isinstance(senses, list) and fix.get("definition"):
        for sense in senses:
            if not sense.get("isPrimary"):
                continue
            sense["englishDefinition"] = row["englishDefinition"]
            if fix.get("fa_fix"):
                sense["persianMeaning"] = row["persianMeaning"]
            if fix.get("example"):
                sense["exampleSentence"] = row["example"]
            if fix.get("examplePersian"):
                sense["exampleTranslation"] = row["examplePersian"]
            break

    return json.dumps(row, ensure_ascii=False, sort_keys=True) != before


def main() -> None:
    problems = validate_corrections()
    if problems:
        for problem in problems:
            print(f"  - {problem}")
        raise SystemExit(f"Refusing to apply {len(problems)} malformed correction(s).")

    total_applied = 0
    unmatched: list[str] = []
    for bank in BANKS:
        applied = 0
        seen: set[str] = set()
        for path in sorted((ASSET_ROOT / bank).glob("*.jsonl")):
            lines = path.read_text(encoding="utf-8").splitlines()
            output: list[str] = []
            dirty = False
            for raw in lines:
                line = raw.strip()
                if not line or line.startswith("#"):
                    output.append(raw)
                    continue
                row = json.loads(line)
                fix = CORRECTIONS.get(str(row.get("word", "")).strip().lower())
                if fix and apply_to_row(row, fix):
                    dirty = True
                    applied += 1
                    seen.add(str(row["word"]).strip().lower())
                output.append(json.dumps(row, ensure_ascii=False, separators=(",", ":")))
            if dirty:
                path.write_text("\n".join(output) + "\n", encoding="utf-8")
        missing = sorted(set(CORRECTIONS) - seen)
        unmatched.extend(f"{bank}:{word}" for word in missing)
        total_applied += applied
        print(f"  {bank.upper()}: {applied:,} row(s) corrected "
              f"({len(missing)} correction(s) with no matching row)")

    # A correction aimed at the other exam may legitimately be absent, so this
    # is a warning rather than a failure, but it must never be silent.
    if unmatched:
        print(f"  note: {len(unmatched)} correction(s) did not match a row in one bank: "
              f"{', '.join(unmatched[:12])}{'...' if len(unmatched) > 12 else ''}")
    print(f"{total_applied:,} correction(s) applied across {len(BANKS)} banks.")


if __name__ == "__main__":
    main()
