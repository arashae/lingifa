#!/usr/bin/env python3
"""Curated multiword exam vocabulary: phrasal verbs, phrasal prepositions, idioms.

These entries are original editorial content. No upstream source supplies them:
ECDICT tags only 2 IELTS and 4 TOEFL multiword lemmas, and Openjam contains no
multiword lemma at all, so every Persian meaning, example and translation below
is hand-authored rather than harvested.

Each card is stored compactly here and materialised into a full schema row by
scripts/apply_curated_additions.py. Keeping the content in one readable place
means the pipeline can rebuild the banks without losing the curation.

Authoring rules that satisfy the strict vocabulary quality gate:
  * `pos` is one of the multiword values the validator accepts, which also keeps
    multiword entries out of the noun/adjective Persian-alignment heuristic.
  * `ex` contains the phrase verbatim, because the validator resolves a
    multi-part target by exact normalised subsequence match.
  * `exFa` keeps the same grammatical person and any numerals as `ex`.
  * `coll` contains at least one phrase that includes the headwords themselves.
  * `fa` is a real translation; placeholder meanings are a hard failure.
"""

from __future__ import annotations

from validate_vocabulary_quality import target_present

CARDS: list[dict] = []

from multiword_cards_phrasal_verbs import PHRASAL_VERBS  # noqa: E402
from multiword_cards_prepositions import PREPOSITIONS  # noqa: E402
from multiword_cards_discourse import DISCOURSE  # noqa: E402
from multiword_cards_applied import APPLIED  # noqa: E402

CARDS.extend(PHRASAL_VERBS)
CARDS.extend(PREPOSITIONS)
CARDS.extend(DISCOURSE)
CARDS.extend(APPLIED)

REQUIRED = ("word", "pos", "fa", "en", "ex", "exFa", "cefr", "coll")


def validate_cards(cards: list[dict] | None = None) -> list[str]:
    """Return a list of authoring problems; empty means the catalog is sound."""
    problems: list[str] = []
    seen: set[str] = set()
    for index, card in enumerate(cards if cards is not None else CARDS, start=1):
        label = f"card {index} ({card.get('word', '<no word>')!r})"
        for field in REQUIRED:
            if not card.get(field):
                problems.append(f"{label}: missing '{field}'")
        word = str(card.get("word", "")).strip().lower()
        if word != card.get("word"):
            problems.append(f"{label}: word must be lowercase and trimmed")
        if " " not in word:
            problems.append(f"{label}: not a multiword expression")
        if word in seen:
            problems.append(f"{label}: duplicate headword")
        seen.add(word)
        pos = card.get("pos")
        if pos not in {"phrasal verb", "prepositional phrase", "idiom"}:
            problems.append(f"{label}: unsupported pos '{pos}'")
        if card.get("cefr") not in {"A1", "A2", "B1", "B2", "C1", "C2"}:
            problems.append(f"{label}: invalid cefr '{card.get('cefr')}'")
        collocations = card.get("coll") or []
        if collocations and not any(target_present(word, c) for c in collocations):
            problems.append(f"{label}: no collocation contains the phrase")
        if not target_present(word, str(card.get("ex", ""))):
            problems.append(f"{label}: example does not contain the phrase")
    return problems


def cards_for(exam: str) -> list[dict]:
    """Cards admitted to a bank. Both exams test these expressions."""
    return list(CARDS)


if __name__ == "__main__":
    issues = validate_cards()
    print(f"{len(CARDS)} curated multiword card(s)")
    if issues:
        print(f"{len(issues)} authoring problem(s):")
        for issue in issues:
            print(f"  - {issue}")
        raise SystemExit(1)
    print("all cards satisfy the authoring rules")
