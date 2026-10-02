#!/usr/bin/env python3
"""Normalise raw dictionary sense dumps in the bundled definitions.

An audit of the 12,014 generated exam rows found 990 definitions that were raw
multi-sense dumps taken straight from the dictionary, for example:

    "n. a general officer of the highest rank\\nn. the head of a religious order"

and:

    "v. stroke soothingly\\ns. soft and mild; not harsh or stern"

Two defects: the `n.` / `v.` / `s.` abbreviation leaks into learner-facing text,
and the senses are flattened into one unreadable string. A single-word card
showing three or four senses at once teaches the learner nothing about which
sense the exam wants.

This stage does three things, all from data already in the row:

1. Splits the dump into individual senses, each keeping its own part of speech.
2. Promotes the sense whose part of speech matches the card's tag to be the
   primary definition, so `gentle` (an adjective) is defined as "soft and mild"
   rather than as the verb "to stroke soothingly". Falls back to the first sense
   when nothing matches the tag.
3. Writes the remaining senses into a `senses` array, which the importer loads
   into the existing `vocabulary_senses` table. Multi-sense support therefore
   arrives for the 390 genuinely polysemous rows rather than being hand-written.

Definitions that are already clean are left untouched, so the stage is safe to
run over the whole catalogue and idempotent on a second pass.
"""

from __future__ import annotations

import json
import re
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
ASSET_ROOT = ROOT / "app" / "src" / "main" / "assets" / "vocabulary"
BANKS = ("ielts", "toefl", "gre")

# Dictionary abbreviation -> normalised part of speech. `s` is an older
# abbreviation for adjective and appears alongside `adj` in this corpus.
POS_ABBREVIATIONS = {
    "n": "noun", "v": "verb", "vt": "verb", "vi": "verb", "s": "adjective",
    "adj": "adjective", "adv": "adverb", "prep": "preposition", "conj": "conjunction",
    "pron": "pronoun", "interj": "interjection", "num": "numeral", "det": "determiner",
}

# A dump begins with an abbreviation followed by a full stop, e.g. "n. " or "s. ".
SENSE_START = re.compile(r"^\s*([A-Za-z]{1,5})\s*[\.:]\s+")

LITERAL_NEWLINE = "\\n"


def split_senses(definition: str) -> list[str]:
    """Split a dump into raw sense strings."""
    text = definition.replace("\r\n", "\n")
    parts = text.split(LITERAL_NEWLINE) if LITERAL_NEWLINE in text else [text]
    senses: list[str] = []
    for part in parts:
        senses.extend(piece.strip() for piece in part.split("\n") if piece.strip())
    return senses


def strip_pos_prefix(sense: str) -> tuple[str | None, str]:
    """Return (part of speech, sense text) with any abbreviation removed."""
    match = SENSE_START.match(sense)
    if not match:
        return None, sense.strip()
    abbreviation = match.group(1).lower()
    if abbreviation not in POS_ABBREVIATIONS:
        return None, sense.strip()
    return POS_ABBREVIATIONS[abbreviation], sense[match.end():].strip()


def is_dump(definition: str) -> bool:
    """True when the definition is a raw sense dump rather than clean prose."""
    if LITERAL_NEWLINE in definition or "\n" in definition:
        return True
    return any(POS_ABBREVIATIONS.get(m.group(1).lower()) for m in [SENSE_START.match(definition)] if m)


def punctuate(definition: str) -> str:
    """Give a definition the terminal full stop its neighbours already have.

    42% of the shipped definitions ended without punctuation, so the same word
    rendered inconsistently depending on which source produced it. This only ever
    appends a full stop: it never rewrites wording.
    """
    text = definition.rstrip()
    if not text:
        return text
    stripped = text.rstrip("\"'”’ ")
    if stripped.endswith((".", "!", "?", "…")):
        return text
    if stripped.endswith("."):
        return stripped
    return stripped + "."


def tidy(text: str) -> str:
    """Tidy one sense into a single clean sentence-like fragment."""
    cleaned = re.sub(r"\s+", " ", text).strip()
    cleaned = cleaned.strip(" ;,")
    if not cleaned:
        return ""
    # Keep the first letter capitalised and the fragment terminated, so it reads
    # like the hand-written definitions it now sits beside.
    cleaned = cleaned[0].upper() + cleaned[1:]
    return cleaned if cleaned.endswith((".", "!", "?")) else cleaned + "."


def build_senses(definition: str, tagged_pos: str) -> tuple[str, list[dict]] | None:
    """Return (primary definition, sense rows) or None when nothing to do."""
    raw = split_senses(definition)
    if len(raw) < 1:
        return None
    parsed: list[tuple[str | None, str]] = []
    for sense in raw:
        pos, text = strip_pos_prefix(sense)
        if tidy(text):
            parsed.append((pos, tidy(text)))
    if not parsed:
        return None

    wanted = tagged_pos.strip().lower()
    primary_index = 0
    if wanted:
        for index, (pos, _) in enumerate(parsed):
            if pos == wanted:
                primary_index = index
                break

    senses = [
        {
            "senseIndex": index + 1,
            "partOfSpeech": pos or tagged_pos,
            "englishDefinition": text,
            "isPrimary": index == primary_index,
        }
        for index, (pos, text) in enumerate(parsed)
    ]
    return senses[primary_index]["englishDefinition"], senses


def process_bank(bank: str, report: dict) -> None:
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
            definition = str(row.get("englishDefinition", ""))
            senses = row.get("senses")
            if is_dump(definition):
                built = build_senses(definition, str(row.get("partOfSpeech", "")))
                if built:
                    primary, senses = built
                    if primary != definition:
                        row["englishDefinition"] = primary
                        dirty = True
                        report["definitions"] += 1
                    if len(senses) > 1:
                        row["senses"] = senses
                        dirty = True
                        report["multisense"] += 1
            elif isinstance(senses, list) and senses:
                # Second pass over an already-normalised row: this stage is the
                # only writer of `senses`, so it also owns their punctuation.
                changed = False
                for sense in senses:
                    tidied = punctuate(tidy(str(sense.get("englishDefinition", ""))))
                    if tidied and tidied != sense.get("englishDefinition"):
                        sense["englishDefinition"] = tidied
                        changed = True
                primary = next(
                    (s for s in senses if s.get("isPrimary")), senses[0]
                )
                tidied_primary = punctuate(tidy(str(primary.get("englishDefinition", ""))))
                if tidied_primary and tidied_primary != definition:
                    row["englishDefinition"] = tidied_primary
                    changed = True
                if changed:
                    dirty = True
                    report["definitions"] += 1
            elif definition:
                # Clean prose that only needs terminal punctuation.
                punctuated = punctuate(definition)
                if punctuated != definition:
                    row["englishDefinition"] = punctuated
                    dirty = True
                    report["definitions"] += 1
            output.append(json.dumps(row, ensure_ascii=False, separators=(",", ":")))
        if dirty:
            path.write_text("\n".join(output) + "\n", encoding="utf-8")
            report["files"] += 1


def main() -> None:
    report = {"definitions": 0, "multisense": 0, "files": 0}
    for bank in BANKS:
        before = dict(report)
        process_bank(bank, report)
        if report != before:
            print(f"  {bank.upper()}: {report['definitions'] - before['definitions']:,} "
                  f"definition(s) normalised, {report['multisense'] - before['multisense']:,} "
                  f"row(s) given multiple senses")
    total = report["definitions"] + report["multisense"]
    print(f"Normalised {total:,} field(s) across {report['files']} file(s).")
    if total == 0:
        print("  (already normalised - stage is idempotent)")


if __name__ == "__main__":
    main()
