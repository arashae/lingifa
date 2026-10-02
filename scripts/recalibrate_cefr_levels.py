#!/usr/bin/env python3
"""Recalibrate over-inflated CEFR levels in the exam vocabulary banks.

The bundled exam banks inherited CEFR labels from the Openjam `level` field.
That field is not calibrated: 16,321 of its 20,602 lemmas (79%) carry `C2`,
including 218 words from NAWL (the Nouns from the Academic Word List) and 39
words from NGSL (the New General Service List). Those two lists are, by
construction, below C2, so the labels are demonstrably wrong rather than
merely debatable.

This stage caps a `C2` label using two independent, already-attached signals:

1. list membership - `NGSL` caps at B2, `NAWL` caps at C1;
2. otherwise the ECDICT frequency rank, because the most frequent English
   words cannot be C2.

Levels A1-C1 are left untouched: they are plausible as published, and this
stage only corrects the single level that is provably saturated.

The CEFR level packs themselves are deliberately not re-bucketed. They are
organised by level, so moving a word between packs is a catalogue restructure
outside the scope of exam-bank quality work. Run this stage after
apply_vocabulary_overrides.py so reviewed overrides cannot restore stale
levels, and so the result is reproducible on regeneration.
"""

from __future__ import annotations

import json
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
ASSET_ROOT = ROOT / "app" / "src" / "main" / "assets" / "vocabulary"

# Only these banks are recalibrated. GRE is intentionally excluded: the user
# asked for IELTS and TOEFL, and GRE has no reviewed override source of truth.
BANKS = ("ielts", "toefl")

# Frequency-rank ceilings for a C2 label. ECDICT `frq` is a corpus rank topping
# out near 190,000, and the median non-C2 row in these banks sits at rank
# 9,543. A C2 word inside the top 20,000 is therefore better described as C1,
# while a rarer word keeps its published level rather than being guessed at.
FREQUENCY_CAPS = (
    (2_000, "B1"),
    (6_000, "B2"),
    (20_000, "C1"),
)

# Highest level a word may keep when list membership proves the C2 label wrong.
LIST_CAPS = {
    "ngsl": "B2",
    "nawl": "C1",
}

C2 = "C2"


def calibrate(row: dict) -> str | None:
    """Return the corrected CEFR level, or None when no change is warranted."""
    current = str(row.get("cefrLevel", "")).strip().upper()
    if current != C2:
        return None

    tags = {str(tag).strip().lower() for tag in row.get("tags", [])}
    for tag, ceiling in LIST_CAPS.items():
        if tag in tags:
            return ceiling

    try:
        rank = int(row.get("frequencyRank") or 0)
    except (TypeError, ValueError):
        rank = 0
    if rank <= 0:
        # No frequency evidence: keep the published label rather than guess.
        return None

    for ceiling, level in FREQUENCY_CAPS:
        if rank <= ceiling:
            return level
    return None


def process_file(path: Path, report: dict) -> None:
    lines = path.read_text(encoding="utf-8").splitlines()
    output: list[str] = []
    changed = False
    for raw in lines:
        line = raw.strip()
        if not line or line.startswith("#"):
            output.append(raw)
            continue
        row = json.loads(line)
        corrected = calibrate(row)
        if corrected:
            was = row["cefrLevel"]
            row["cefrLevel"] = corrected
            changed = True
            report["changes"][corrected] = report["changes"].get(corrected, 0) + 1
            report["samples"].append(
                {"word": row.get("word", ""), "from": was, "to": corrected,
                 "frequencyRank": row.get("frequencyRank", 0)}
            )
        output.append(json.dumps(row, ensure_ascii=False, separators=(",", ":")))
    if changed:
        path.write_text("\n".join(output) + "\n", encoding="utf-8")
        report["files_changed"] += 1


def main() -> None:
    report = {"banks": {}, "files_changed": 0, "changes": {}, "samples": []}
    for bank in BANKS:
        bank_dir = ASSET_ROOT / bank
        files = sorted(bank_dir.glob("*.jsonl"))
        if not files:
            print(f"No JSONL files found for bank '{bank}' in {bank_dir}", file=sys.stderr)
            raise SystemExit(1)
        before: dict[str, int] = {}
        for path in files:
            for raw in path.read_text(encoding="utf-8").splitlines():
                line = raw.strip()
                if line and not line.startswith("#"):
                    level = str(json.loads(line).get("cefrLevel", "?")).upper()
                    before[level] = before.get(level, 0) + 1
            process_file(path, report)
        after: dict[str, int] = {}
        for path in files:
            for raw in path.read_text(encoding="utf-8").splitlines():
                line = raw.strip()
                if line and not line.startswith("#"):
                    level = str(json.loads(line).get("cefrLevel", "?")).upper()
                    after[level] = after.get(level, 0) + 1
        report["banks"][bank] = {"before": before, "after": after}

    total = sum(report["changes"].values())
    print(f"Recalibrated {total:,} over-inflated C2 label(s) across "
          f"{report['files_changed']} file(s).")
    for bank, data in report["banks"].items():
        print(f"  {bank.upper()}: {data['before']} -> {data['after']}")
    if report["changes"]:
        print(f"  downgraded to: {report['changes']}")
    if total == 0:
        print("  (already calibrated - stage is idempotent)")


if __name__ == "__main__":
    main()
