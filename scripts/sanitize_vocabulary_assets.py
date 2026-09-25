#!/usr/bin/env python3
"""Conservative post-processing for bundled vocabulary JSONL assets.

The source banks are intentionally broad. This pass removes clearly template-generated
collocations and pushes obvious C2 proper nouns/places to the back of the learning order.
It never invents lexical data: when a collocation is doubtful, an empty list is preferred.
"""

from __future__ import annotations

import json
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
VOCAB_ROOT = ROOT / "app" / "src" / "main" / "assets" / "vocabulary"


def norm(value: str) -> str:
    return " ".join(str(value or "").strip().lower().split())


def generated_templates(word: str, part_of_speech: str) -> set[str]:
    w = norm(word)
    pos = norm(part_of_speech)
    if "verb" in pos:
        values = {
            f"{w} the process",
            f"{w} effectively",
            f"{w} a solution",
            f"attempt to {w}",
            f"seek to {w}",
            f"{w} rapidly",
            f"effectively {w}",
            f"fail to {w}",
            f"ability to {w}",
        }
    elif "adj" in pos:
        values = {
            f"highly {w}",
            f"increasingly {w}",
            f"{w} factor",
            f"{w} importance",
            f"particularly {w}",
            f"remain {w}",
            f"become {w}",
        }
    elif "adverb" in pos:
        values = {
            f"{w} important",
            f"{w} significant",
            f"{w} different",
            f"{w} evident",
        }
    else:
        values = {
            f"crucial {w}",
            f"significant {w}",
            f"key {w}",
            f"underlying {w}",
            f"high level of {w}",
            f"role of {w}",
            f"importance of {w}",
        }
    return {norm(value) for value in values}


def sanitize_collocations(row: dict) -> tuple[list[str], int]:
    raw = [str(value).strip() for value in row.get("collocations", []) if str(value).strip()]
    if not raw:
        return [], 0

    templates = generated_templates(row.get("word", ""), row.get("partOfSpeech", ""))
    matches = [value for value in raw if norm(value) in templates]

    # Only treat a family as generated when it dominates the row. This avoids deleting a lone
    # genuinely useful phrase such as "highly significant" from a curated source.
    generated_family = len(matches) >= 2 and len(matches) * 2 >= len(raw)
    if not generated_family:
        return raw[:8], 0

    cleaned = [value for value in raw if norm(value) not in templates]
    return cleaned[:8], len(matches)


PROPER_NOUN_DEFINITION_PATTERNS = (
    "county of ",
    "city in ",
    "town in ",
    "island in ",
    "island of ",
    "capital of ",
    "province of ",
    "state in ",
    "surname",
    "given name",
    "family name",
)


def looks_like_c2_proper_noun(row: dict) -> bool:
    if norm(row.get("cefrLevel", "")) != "c2":
        return False
    tags = [norm(tag) for tag in row.get("tags", [])]
    if any(any(marker in tag for marker in ("proper", "place", "name")) for tag in tags):
        return True
    definition = norm(row.get("englishDefinition", ""))
    return any(pattern in definition for pattern in PROPER_NOUN_DEFINITION_PATTERNS)


def sanitize_row(row: dict) -> tuple[dict, int, bool]:
    collocations, removed = sanitize_collocations(row)
    row["collocations"] = collocations

    tags = [str(tag).strip() for tag in row.get("tags", []) if str(tag).strip()]
    tags = [tag for tag in tags if tag != "synthetic-collocation"]
    if not collocations and "needs-collocation" not in tags:
        tags.append("needs-collocation")

    downranked = False
    if looks_like_c2_proper_noun(row):
        downranked = True
        if "low-study-priority" not in tags:
            tags.append("low-study-priority")
        order = int(row.get("learningOrder") or 0)
        if order < 100_000:
            row["learningOrder"] = 100_000 + max(order, 1)
        row["examPriority"] = min(int(row.get("examPriority") or 0), 0)

    row["tags"] = tags
    return row, removed, downranked


def process_file(path: Path) -> tuple[int, int, int]:
    rows: list[str] = []
    total = 0
    removed = 0
    downranked = 0

    for raw_line in path.read_text(encoding="utf-8").splitlines():
        line = raw_line.strip()
        if not line or line.startswith("#"):
            if raw_line:
                rows.append(raw_line)
            continue
        row = json.loads(line)
        row, removed_here, downranked_here = sanitize_row(row)
        rows.append(json.dumps(row, ensure_ascii=False, separators=(",", ":")))
        total += 1
        removed += removed_here
        downranked += int(downranked_here)

    path.write_text("\n".join(rows) + "\n", encoding="utf-8")
    return total, removed, downranked


def main() -> None:
    total_rows = 0
    total_removed = 0
    total_downranked = 0
    files = sorted(VOCAB_ROOT.rglob("*.jsonl"))

    for path in files:
        rows, removed, downranked = process_file(path)
        total_rows += rows
        total_removed += removed
        total_downranked += downranked

    print(
        f"Sanitized {total_rows:,} rows across {len(files)} files; "
        f"removed {total_removed:,} template collocations; "
        f"down-ranked {total_downranked:,} obvious C2 names/places."
    )


if __name__ == "__main__":
    main()
