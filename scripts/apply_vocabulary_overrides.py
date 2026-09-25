#!/usr/bin/env python3
"""Re-apply reviewed IELTS/TOEFL cards after generated vocabulary rebuilds.

Reviewed learner-facing cards are the editorial source of truth. This script overlays
those reviewed rows by headword after a regeneration so upstream dictionary data
cannot silently replace curated senses, examples, translations, or collocations.
GRE is intentionally untouched.
"""

from __future__ import annotations

import json
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
VOCAB_ROOT = ROOT / "app" / "src" / "main" / "assets" / "vocabulary"
OVERRIDE_ROOT = ROOT / "scripts" / "vocabulary_overrides"

TOEFL_FINAL_FIXES = {
    "stationary": {
        "collocations": ["remain stationary", "a stationary vehicle", "stationary position", "stationary object"],
    },
    "steep": {
        "persianMeaning": "تند؛ شیب‌دار؛ پرشیب",
        "example": "The steep path climbs to the mountain refuge above the valley.",
        "examplePersian": "مسیر پرشیب تا پناهگاه کوهستانی بالای دره بالا می‌رود.",
        "collocations": ["a steep climb", "steep slope", "a steep decline", "steep gradient"],
    },
    "transpire": {
        "collocations": ["it transpired that", "what transpired", "events transpired"],
    },
}

BANKS = {
    "ielts": {"prefix": "ielts_core", "expected": 5040, "final_fixes": {}},
    "toefl": {"prefix": "toefl_core", "expected": 6974, "final_fixes": TOEFL_FINAL_FIXES},
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


def load_drops(override_dir: Path) -> set[str]:
    """Headwords removed from a bank by explicit editorial decision.

    Inflected or redundant entries (e.g. a bare plural whose lemma is already a
    reviewed card) are dropped here instead of being silently re-added by
    regeneration. Removal must be deliberate and auditable, never inferred.
    """
    path = override_dir / "_dropped.json"
    if not path.is_file():
        return set()
    payload = json.loads(path.read_text(encoding="utf-8"))
    headwords = payload.get("headwords", [])
    if not isinstance(headwords, list):
        raise SystemExit(f"{path}: 'headwords' must be a list")
    return {str(word).strip().lower() for word in headwords if str(word).strip()}


def apply_bank(bank: str, cfg: dict) -> tuple[int, int]:
    prefix = cfg["prefix"]
    target_dir = VOCAB_ROOT / bank
    override_dir = OVERRIDE_ROOT / bank
    override_files = sorted(override_dir.glob(f"{prefix}_*.jsonl"))
    target_files = sorted(target_dir.glob(f"{prefix}_*.jsonl"))
    if not override_files:
        raise SystemExit(f"No {bank.upper()} overrides found in {override_dir}")
    if not target_files:
        raise SystemExit(f"No generated {bank.upper()} files found in {target_dir}")

    overrides: dict[str, dict] = {}
    for path in override_files:
        for row in load_jsonl(path):
            word = str(row.get("word", "")).strip().lower()
            if not word:
                raise RuntimeError(f"{path}: reviewed row has empty word")
            if word in overrides:
                raise RuntimeError(f"Duplicate reviewed {bank.upper()} headword: {word}")
            overrides[word] = row

    expected = int(cfg["expected"])
    dropped = load_drops(override_dir)
    if len(overrides) + len(dropped) != expected:
        raise SystemExit(
            f"Reviewed {bank.upper()} source-of-truth has {len(overrides)} rows "
            f"+ {len(dropped)} dropped headwords; expected {expected} in total. "
            "Update BANKS['expected'] deliberately when membership changes."
        )
    if dropped & set(overrides):
        both = sorted(dropped & set(overrides))
        raise SystemExit(
            f"Reviewed {bank.upper()} headwords are both kept and dropped: {both[:20]}"
        )

    replaced: set[str] = set()
    removed: set[str] = set()
    changed_files = 0
    final_fixes: dict[str, dict] = cfg.get("final_fixes", {})
    for path in target_files:
        generated_rows = load_jsonl(path)
        output_rows: list[dict] = []
        changed = False
        for generated in generated_rows:
            word = str(generated.get("word", "")).strip().lower()
            if word in dropped:
                removed.add(word)
                changed = True
                continue
            reviewed = overrides.get(word)
            output = dict(reviewed) if reviewed is not None else generated
            if reviewed is not None:
                replaced.add(word)
                if output != generated:
                    changed = True
            final_fix = final_fixes.get(word)
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
            f"Generated {bank.upper()} membership lost {len(missing)} reviewed words; "
            f"refusing silent data loss. First entries: {preview}"
        )
    unmatched_drops = sorted(dropped - removed)
    if unmatched_drops:
        preview = ", ".join(unmatched_drops[:20])
        raise SystemExit(
            f"{len(unmatched_drops)} dropped {bank.upper()} headword(s) were not present in the "
            f"generated data (typo or already removed?): {preview}"
        )
    return len(replaced), changed_files


def main() -> None:
    for bank, cfg in BANKS.items():
        replaced, changed_files = apply_bank(bank, cfg)
        print(
            f"Applied {replaced:,} reviewed {bank.upper()} cards from source-of-truth overrides; "
            f"changed {changed_files} target files."
        )
    print("GRE was not modified.")


if __name__ == "__main__":
    main()
