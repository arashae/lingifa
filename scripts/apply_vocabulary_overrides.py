#!/usr/bin/env python3
"""Re-apply reviewed TOEFL cards after any generated vocabulary rebuild.

The generator is useful for membership/source refreshes, but reviewed learner-facing
TOEFL cards are editorial source-of-truth. This script overlays those reviewed rows
by headword so a later rebuild cannot silently replace them with raw dictionary
senses. GRE and IELTS are intentionally untouched.
"""

from __future__ import annotations

import json
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
TARGET_DIR = ROOT / "app" / "src" / "main" / "assets" / "vocabulary" / "toefl"
OVERRIDE_DIR = ROOT / "scripts" / "vocabulary_overrides" / "toefl"

# Small final corrections found while auditing the completed reviewed bank.
FINAL_FIXES = {
    "stationary": {
        "collocations": [
            "remain stationary",
            "a stationary vehicle",
            "stationary position",
            "stationary object",
        ],
    },
    "steep": {
        "persianMeaning": "تند؛ شیب‌دار؛ پرشیب",
        "example": "The steep path climbs to the mountain refuge above the valley.",
        "examplePersian": "مسیر پرشیب تا پناهگاه کوهستانی بالای دره بالا می‌رود.",
        "collocations": [
            "a steep climb",
            "steep slope",
            "a steep decline",
            "steep gradient",
        ],
    },
    "transpire": {
        "collocations": [
            "it transpired that",
            "what transpired",
            "events transpired",
        ],
    },
}


def load_jsonl(path: Path) -> list[dict]:
    rows: list[dict] = []
    for line_no, raw in enumerate(path.read_text(encoding="utf-8").splitlines(), start=1):
        line = raw.strip()
        if not line or line.startswith("#"):
            continue
        try:
            row = json.loads(line)
        except json.JSONDecodeError as exc:
            raise RuntimeError(f"{path}:{line_no}: malformed JSON: {exc}") from exc
        if not isinstance(row, dict):
            raise RuntimeError(f"{path}:{line_no}: expected object row")
        rows.append(row)
    return rows


def main() -> None:
    override_files = sorted(OVERRIDE_DIR.glob("toefl_core_*.jsonl"))
    target_files = sorted(TARGET_DIR.glob("toefl_core_*.jsonl"))
    if not override_files:
        raise SystemExit(f"No TOEFL overrides found in {OVERRIDE_DIR}")
    if not target_files:
        raise SystemExit(f"No generated TOEFL files found in {TARGET_DIR}")

    overrides: dict[str, dict] = {}
    for path in override_files:
        for row in load_jsonl(path):
            word = str(row.get("word", "")).strip().lower()
            if not word:
                raise RuntimeError(f"{path}: reviewed row has empty word")
            if word in overrides:
                raise RuntimeError(f"Duplicate reviewed TOEFL headword: {word}")
            overrides[word] = row

    replaced: set[str] = set()
    changed_files = 0
    for path in target_files:
        generated_rows = load_jsonl(path)
        output_rows: list[dict] = []
        changed = False
        for generated in generated_rows:
            word = str(generated.get("word", "")).strip().lower()
            reviewed = overrides.get(word)
            if reviewed is None:
                output = generated
            else:
                output = dict(reviewed)
                replaced.add(word)
                if output != generated:
                    changed = True

            final_fix = FINAL_FIXES.get(word)
            if final_fix:
                patched = dict(output)
                patched.update(final_fix)
                if patched != output:
                    changed = True
                output = patched
            output_rows.append(output)

        if changed:
            with path.open("w", encoding="utf-8", newline="\n") as handle:
                for row in output_rows:
                    handle.write(json.dumps(row, ensure_ascii=False, separators=(",", ":")) + "\n")
            changed_files += 1

    missing = sorted(set(overrides) - replaced)
    if missing:
        preview = ", ".join(missing[:20])
        raise SystemExit(
            f"Generated TOEFL membership lost {len(missing)} reviewed words; "
            f"refusing silent data loss. First entries: {preview}"
        )

    print(
        f"Applied {len(replaced):,} reviewed TOEFL cards from "
        f"{len(override_files)} override chunks; changed {changed_files} target files."
    )
    print("IELTS and GRE were not modified.")


if __name__ == "__main__":
    main()
