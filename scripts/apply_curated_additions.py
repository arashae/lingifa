#!/usr/bin/env python3
"""Append curated multiword cards to the IELTS and TOEFL banks.

Reviewed learner-facing content is added here rather than in
apply_vocabulary_overrides.py because that script can only replace or drop
headwords that the generator already produced. Phrasal verbs and phrasal
prepositions carry no ECDICT exam tag and no Openjam lemma, so nothing upstream
will ever emit them and they have to be introduced into the bank explicitly.

Both exams test these expressions, so every curated card is admitted to both
banks. That is deliberate duplication across banks, which the validator allows:
duplicate detection is per-bank, and cross-bank membership is how one
VocabularyItem serves IELTS, TOEFL and GRE simultaneously.

After appending, the bank is re-chunked to the standard chunk size and
master_catalog.json is rewritten, because the importer asserts that every chunk
row count equals its catalog `expectedItems`.
"""

from __future__ import annotations

import csv
import json
import sys
from pathlib import Path

from curated_multiword_catalog import CARDS, validate_cards

ROOT = Path(__file__).resolve().parents[1]
ASSET_ROOT = ROOT / "app" / "src" / "main" / "assets" / "vocabulary"
CATALOG_PATH = ASSET_ROOT / "master_catalog.json"
ECDICT_CSV = Path(sys.argv[1]) if len(sys.argv) > 1 else None

CHUNK_SIZE = 500
BANKS = {"ielts": "pack_ielts_master", "toefl": "pack_toefl_master"}

SOURCE = "LinguaFa curated multiword editorial"
SOURCE_LICENSE = "Original LinguaFa editorial content"

# Tag vocabulary. `multiword` marks the row so the app can group or filter the
# expressions separately from single-word cards.
BASE_TAGS = ["offline", "curated-multiword", "curated-2026"]


def norm(word: str) -> str:
    return " ".join((word or "").strip().lower().replace("’", "'").split())


def load_ecdict_ranks() -> dict[str, int]:
    """Frequency ranks for multiword lemmas, when ECDICT actually carries them.

    Sourced rather than invented. Phrases absent from ECDICT keep rank 0, which
    the study policy already treats as unknown.
    """
    if not ECDICT_CSV or not ECDICT_CSV.is_file():
        return {}
    wanted = {norm(card["word"]) for card in CARDS}
    ranks: dict[str, int] = {}
    csv.field_size_limit(10**9)
    with ECDICT_CSV.open("r", encoding="utf-8-sig", newline="") as handle:
        for row in csv.DictReader(handle):
            key = norm(row.get("word", ""))
            if key not in wanted:
                continue
            try:
                rank = int(str(row.get("frq") or row.get("bnc") or 0).strip())
            except (TypeError, ValueError):
                rank = 0
            if rank > 0:
                ranks[key] = rank
            if len(ranks) == len(wanted):
                break
    return ranks


def build_row(card: dict, rank: int) -> dict:
    tags = list(BASE_TAGS) + [card["pos"].replace(" ", "-")]
    for topic in card.get("topic", []):
        tags.append(topic.lower())
    if card.get("mistake"):
        tags.append("usage-warning")
    return {
        "word": card["word"],
        "ipa": "",
        "persianMeaning": card["fa"],
        "englishDefinition": card["en"],
        "partOfSpeech": card["pos"],
        "example": card["ex"],
        "examplePersian": card["exFa"],
        "cefrLevel": card["cefr"],
        "synonyms": list(card.get("syn", [])),
        "antonyms": list(card.get("ant", [])),
        "collocations": list(card.get("coll", [])),
        "wordFamily": list(card.get("fam", [])),
        "commonMistakes": card.get("mistake", ""),
        "ieltsRelevance": "Essential",
        "toeflRelevance": "Essential",
        "greRelevance": "Low",        "tags": tags,
        "source": SOURCE,
        "sourceLicense": SOURCE_LICENSE,
        "frequencyRank": rank,
        "examPriority": 4,
        "examTopics": list(card.get("topic", [])),
        "targetBand": card.get("band", "7.0"),
        "skillFocus": list(card.get("skill", [])),
    }


def load_bank_rows(bank: str, prefix: str) -> tuple[list[dict], list[Path]]:
    files = sorted((ASSET_ROOT / bank).glob(f"{prefix}_*.jsonl"))
    if not files:
        raise SystemExit(f"No chunk files found for bank '{bank}'")
    rows: list[dict] = []
    for path in files:
        for raw in path.read_text(encoding="utf-8").splitlines():
            line = raw.strip()
            if line and not line.startswith("#"):
                rows.append(json.loads(line))
    return rows, files


def write_bank(bank: str, prefix: str, rows: list[dict], files: list[Path]) -> None:
    """Re-chunk a bank into `files`, sized CHUNK_SIZE with a short final chunk."""
    for stale in files:
        stale.unlink()
    chunks = [rows[i:i + CHUNK_SIZE] for i in range(0, len(rows), CHUNK_SIZE)]
    bank_dir = ASSET_ROOT / bank
    written: list[tuple[str, int]] = []
    for index, chunk in enumerate(chunks, start=1):
        path = bank_dir / f"{prefix}_{index:03d}.jsonl"
        with path.open("w", encoding="utf-8", newline="\n") as handle:
            for row in chunk:
                handle.write(json.dumps(row, ensure_ascii=False, separators=(",", ":")) + "\n")
        written.append((path.name, len(chunk)))
    _update_catalog(bank, written)


def _update_catalog(bank: str, written: list[tuple[str, int]]) -> None:
    catalog = json.loads(CATALOG_PATH.read_text(encoding="utf-8-sig"))
    pack_id = BANKS[bank]
    chunks = [c for c in catalog.get("chunks", []) if c.get("packId") == pack_id]
    if not chunks:
        raise SystemExit(f"Catalog has no chunks for pack {pack_id}")
    # Growing a bank can need one more chunk than before; that is the expected
    # consequence of adding cards, not a bookkeeping error.
    while len(chunks) < len(written):
        template = dict(chunks[-1])
        template["id"] = f"{template.get('id', 'chunk').rsplit('-', 1)[0]}-{len(chunks) + 1:03d}"
        chunks.append(template)
        catalog["chunks"].append(template)
    if len(chunks) > len(written):
        raise SystemExit(
            f"Catalog lists {len(chunks)} {bank.upper()} chunk(s) but the bank only "
            f"needs {len(written)}. Shedding a chunk is never automatic; reduce the "
            f"bank deliberately in vocabulary_overrides if that is intended."
        )
    for entry, (name, count) in zip(chunks, written):
        # Asset paths are relative to the app's assets/ directory, so they must
        # keep the "vocabulary/<bank>/" prefix the importer opens them with.
        entry["asset"] = f"vocabulary/{bank}/{name}"
        entry["expectedItems"] = count
        entry["source"] = SOURCE
    targets = catalog.setdefault("targets", {})
    if pack_id in targets:
        targets[pack_id] = sum(count for _, count in written)
    CATALOG_PATH.write_text(
        json.dumps(catalog, ensure_ascii=False, indent=2) + "\n", encoding="utf-8"
    )


def main() -> None:
    problems = validate_cards()
    if problems:
        for problem in problems:
            print(f"  - {problem}", file=sys.stderr)
        raise SystemExit(f"Refusing to add {len(problems)} malformed curated card(s).")

    ranks = load_ecdict_ranks()
    print(f"Curated catalog: {len(CARDS)} card(s); ECDICT supplied {len(ranks)} frequency rank(s).")

    for bank, pack_id in BANKS.items():
        prefix = f"{bank}_core"
        rows, files = load_bank_rows(bank, prefix)
        index = {norm(row.get("word", "")): pos for pos, row in enumerate(rows)}
        added = 0
        refreshed = 0
        skipped = 0
        for card in CARDS:
            key = norm(card["word"])
            row = build_row(card, ranks.get(key, 0))
            if key in index:
                existing = rows[index[key]]
                # Refresh content we previously authored, but never overwrite a
                # row the generator produced: that is a genuine duplicate headword.
                if "curated-multiword" in {str(t) for t in existing.get("tags", [])}:
                    existing.update(row)
                    refreshed += 1
                else:
                    skipped += 1
                continue
            index[key] = len(rows)
            rows.append(row)
            added += 1
        before = len(rows) - added
        write_bank(bank, prefix, rows, files)
        print(f"  {bank.upper()}: {before:,} + {added:,} curated = {len(rows):,} "
              f"({refreshed} refreshed, {skipped} generated duplicates left alone)")


if __name__ == "__main__":
    main()
