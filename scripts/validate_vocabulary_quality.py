#!/usr/bin/env python3
"""Quality gate for bundled LinguaFa vocabulary assets.

Hard failures are limited to defects that should never ship: malformed rows, missing required
bilingual/English data, invalid CEFR values, duplicate headwords inside a study bank, leftover
synthetic-collocation families, and obvious C2 names/places that were not down-ranked.
Soft coverage metrics (examples, IPA, collocations) and semantic review heuristics are
written to quality_report.json.

CLI Options:
  --file <path>        Validate specific JSONL file (relative to repo root, vocabulary root, or absolute).
  --range <start>:<end> 1-based card line range filter (e.g. 1:100 or 301:400).
  --strict             Elevate heuristic warning flags to fatal errors (exit code 1).
  --report <path>      Custom report output path (defaults to app/src/main/assets/vocabulary/quality_report.json).
"""

from __future__ import annotations

import argparse
import json
import re
import sys
from collections import Counter, defaultdict
from pathlib import Path

# Ensure UTF-8 stdout/stderr on Windows consoles to prevent encoding errors with Persian text
if hasattr(sys.stdout, "reconfigure"):
    sys.stdout.reconfigure(encoding="utf-8")
if hasattr(sys.stderr, "reconfigure"):
    sys.stderr.reconfigure(encoding="utf-8")

# Ensure scripts directory is on sys.path for importing sister modules
scripts_dir = Path(__file__).resolve().parent
if str(scripts_dir) not in sys.path:
    sys.path.insert(0, str(scripts_dir))

from sanitize_vocabulary_assets import (
    VOCAB_ROOT,
    generated_templates,
    looks_like_c2_proper_noun,
    norm,
)

VALID_CEFR = {"A1", "A2", "B1", "B2", "C1", "C2"}
REPORT_PATH = VOCAB_ROOT / "quality_report.json"
POS_OK = {
    "noun", "verb", "adjective", "adverb", "pronoun", "determiner", "conjunction", "preposition",
    "modal", "phrase", "phrasal verb", "idiom", "prepositional phrase",
    "modal verb", "modal auxiliary", "auxiliary verb", "article", "interjection", "prefix", "suffix",
    "be-verb", "have-verb", "do-verb", "infinitive-to"
}
RISKY_SENSES = {
    "it": "pronoun", "or": "conjunction", "may": "modal", "can": "modal",
    "might": "modal", "must": "modal", "he": "pronoun"
}
PERSIAN_VERB_MARKERS = (
    "آمدن", "ایستادن", "باقی ماندن", "بودن", "توانستن", "جستن", "خواستن", "خوردن",
    "دادن", "داشتن", "دیدن", "رفتن", "رسیدن", "رویدادن", "زدن", "شدن", "گرفتن", "گفتن",
    "ماندن", "نشستن", "نوشیدن", "یافتن", "کردن",
)
ENGLISH_PRONOUNS = {
    "first": {"i", "me", "my", "mine", "myself", "we", "us", "our", "ours", "ourselves"},
    "second": {"you", "your", "yours", "yourself", "yourselves"},
    "third": {
        "he", "him", "his", "himself", "she", "her", "hers", "herself", "they", "them",
        "their", "theirs", "themselves",
    },
}
PERSIAN_PRONOUNS = {
    "first": {"من", "ما", "خودم", "خودمان"},
    "second": {"تو", "شما", "خودت", "خودتان"},
    "third": {"او", "ایشان", "آنها", "خودش", "خودشان"},
}
ENGLISH_NUMBERS = {
    "zero": 0, "one": 1, "two": 2, "three": 3, "four": 4, "five": 5, "six": 6,
    "seven": 7, "eight": 8, "nine": 9, "ten": 10, "eleven": 11, "twelve": 12,
    "thirteen": 13, "fourteen": 14, "fifteen": 15, "sixteen": 16, "seventeen": 17,
    "eighteen": 18, "nineteen": 19, "twenty": 20, "thirty": 30, "forty": 40,
    "fifty": 50, "sixty": 60,
}
PERSIAN_NUMBERS = {
    "صفر": 0, "یک": 1, "دو": 2, "سه": 3, "چهار": 4, "پنج": 5, "شش": 6,
    "هفت": 7, "هشت": 8, "نه": 9, "ده": 10, "یازده": 11, "دوازده": 12,
    "سیزده": 13, "چهارده": 14, "پانزده": 15, "شانزده": 16, "هفده": 17,
    "هجده": 18, "نوزده": 19, "بیست": 20, "سی": 30, "چهل": 40, "پنجاه": 50, "شصت": 60,
}

# High-frequency irregular forms and inflection dictionaries
IRREGULAR_FORMS: dict[str, set[str]] = {
    "hold": {"held", "holds", "holding"},
    "lose": {"lost", "loses", "losing"},
    "send": {"sent", "sends", "sending"},
    "buy": {"bought", "buys", "buying"},
    "sell": {"sold", "sells", "selling"},
    "become": {"became", "becomes", "becoming"},
    "give": {"gave", "given", "gives", "giving"},
    "take": {"took", "taken", "takes", "taking"},
    "stand": {"stood", "stands", "standing"},
    "feel": {"felt", "feels", "feeling"},
    "leave": {"left", "leaves", "leaving"},
    "mean": {"meant", "means", "meaning"},
    "tell": {"told", "tells", "telling"},
    "keep": {"kept", "keeps", "keeping"},
    "build": {"built", "builds", "building"},
    "pay": {"paid", "pays", "paying"},
    "meet": {"met", "meets", "meeting"},
    "lead": {"led", "leads", "leading"},
    "be": {"am", "is", "are", "was", "were", "been", "being"},
    "have": {"has", "had", "having"},
    "do": {"does", "did", "done", "doing"},
    "go": {"goes", "went", "gone", "going"},
    "see": {"saw", "seen", "sees", "seeing"},
    "come": {"came", "comes", "coming"},
    "say": {"said", "says", "saying"},
    "get": {"got", "gotten", "gets", "getting"},
    "make": {"made", "makes", "making"},
    "know": {"knew", "known", "knows", "knowing"},
    "think": {"thought", "thinks", "thinking"},
    "find": {"found", "finds", "finding"},
    "run": {"ran", "runs", "running"},
    "write": {"wrote", "written", "writes", "writing"},
    "read": {"reading", "reads"},
    "speak": {"spoke", "spoken", "speaks", "speaking"},
    "break": {"broke", "broken", "breaks", "breaking"},
    "choose": {"chose", "chosen", "chooses", "choosing"},
    "draw": {"drew", "drawn", "draws", "drawing"},
    "drive": {"drove", "driven", "drives", "driving"},
    "eat": {"ate", "eaten", "eats", "eating"},
    "fall": {"fell", "fallen", "falls", "falling"},
    "grow": {"grew", "grown", "grows", "growing"},
    "hear": {"heard", "hears", "hearing"},
    "hide": {"hid", "hidden", "hides", "hiding"},
    "rise": {"rose", "risen", "rises", "rising"},
    "seek": {"sought", "seeks", "seeking"},
    "show": {"showed", "shown", "shows", "showing"},
    "sit": {"sat", "sits", "sitting"},
    "spend": {"spent", "spends", "spending"},
    "teach": {"taught", "teaches", "teaching"},
    "wear": {"wore", "worn", "wears", "wearing"},
    "win": {"won", "wins", "winning"},
    "understand": {"understood", "understands", "understanding"},
    "bring": {"brought", "brings", "bringing"},
    "begin": {"began", "begun", "begins", "beginning"},
    # Irregular plurals
    "child": {"children"},
    "man": {"men"},
    "woman": {"women"},
    "person": {"people"},
    "foot": {"feet"},
    "tooth": {"teeth"},
    "mouse": {"mice"},
    "criterion": {"criteria"},
    "phenomenon": {"phenomena"},
    "datum": {"data"},
    "medium": {"media"},
    "analysis": {"analyses"},
    "basis": {"bases"},
    "crisis": {"crises"},
    "hypothesis": {"hypotheses"},
}

# Bidirectional indexing: map irregular inflections back to headword lemma
_REVERSE_IRREGULAR: dict[str, set[str]] = {}
for _base, _forms in IRREGULAR_FORMS.items():
    for _form in _forms:
        _REVERSE_IRREGULAR.setdefault(_form, set()).add(_base)
for _form, _bases in _REVERSE_IRREGULAR.items():
    IRREGULAR_FORMS.setdefault(_form, set()).update(_bases)

URDU_NON_PERSIAN_GLYPHS = {
    "\u0688", "\u0691", "\u06ba", "\u06d2", "\u06d3",
    "\u0679", "\u06be", "\u06c1", "\u06c2", "\u06c3",
    "\u06bb", "\u068c", "\u068d", "\u067b", "\u0684",
    "\u0683", "\u06a6",
}


def rel_label(path: Path) -> str:
    """Path relative to the asset root when possible, else the path as given.

    Reviewed source-of-truth chunks live in scripts/vocabulary_overrides, so
    validation must also work for files outside the asset root.
    """
    try:
        return path.relative_to(VOCAB_ROOT).as_posix()
    except ValueError:
        return path.as_posix()


def bank_key(path: Path) -> str:
    try:
        rel = path.relative_to(VOCAB_ROOT)
    except ValueError:
        parts = path.parts
        if "vocabulary_overrides" in parts:
            index = parts.index("vocabulary_overrides")
            if len(parts) > index + 1:
                return parts[index + 1]
        return "unknown"
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


def generate_inflections(token: str) -> set[str]:
    t = token.lower()
    if not t:
        return set()
    forms = {t, t + "s", t + "es", t + "ed", t + "d", t + "ing"}

    # -e endings: live -> lived, living
    if t.endswith("e"):
        forms.update({t + "d", t[:-1] + "ed", t[:-1] + "ing"})
        if t.endswith("ee"):
            forms.update({t + "ing", t + "d"})
        elif t.endswith("ie"):
            forms.update({t[:-2] + "ying", t + "d"})

    # -y endings: try -> tried, tries
    if t.endswith("y"):
        if len(t) >= 2 and t[-2] not in "aeiou":
            stem = t[:-1]
            forms.update({stem + "ies", stem + "ied", stem + "ier", stem + "iest", stem + "iness", stem + "ily"})
        else:
            forms.update({t + "s", t + "ed", t + "ing"})

    # Consonant doubling: stop -> stopped, running, fitted
    vowels = "aeiou"
    consonants = "bcdfghjklmnpqrstvz"
    if len(t) >= 3 and t[-1] in consonants and t[-2] in vowels and t[-3] not in vowels:
        doubled = t + t[-1]
        forms.update({doubled + "ed", doubled + "ing", doubled + "er", doubled + "en", doubled + "es"})
    elif len(t) == 2 and t[-1] in consonants and t[-2] in vowels:
        doubled = t + t[-1]
        forms.update({doubled + "ed", doubled + "ing", doubled + "er", doubled + "en"})

    # Reverse stripping of suffixes to recover potential root
    if t.endswith("ed"):
        forms.update({t[:-2], t[:-1]})
    if t.endswith("ing"):
        forms.update({t[:-3], t[:-3] + "e"})
    if t.endswith("ies") or t.endswith("ied"):
        forms.add(t[:-3] + "y")
    if t.endswith("s") and not t.endswith("ss"):
        forms.add(t[:-1])

    # Irregular forms
    if t in IRREGULAR_FORMS:
        forms.update(IRREGULAR_FORMS[t])

    return forms


def target_present(word: str, example: str) -> bool:
    if not word:
        return True
    if not example:
        return False
    tokens = set(re.findall(r"[a-z0-9]+", example.lower()))
    parts = re.findall(r"[a-z0-9]+", word.lower())
    if not parts:
        return True
    if len(parts) == 1:
        forms = generate_inflections(parts[0])
        return bool(tokens.intersection(forms))

    # Multi-part / hyphenated / compound words
    norm_word = " ".join(parts)
    norm_ex = " ".join(re.findall(r"[a-z0-9]+", example.lower()))
    if norm_word in norm_ex or "".join(parts) in tokens:
        return True
    if all(bool(tokens.intersection(generate_inflections(p))) for p in parts):
        return True
    return False


def detect_script_or_encoding_defect(text: str) -> str | None:
    if not text:
        return None
    if "\ufffd" in text:
        return "replacement character \ufffd detected"
    if any(ch in URDU_NON_PERSIAN_GLYPHS for ch in text):
        return "Urdu-specific or non-Persian glyph detected"
    for ch in text:
        # Hangul (Syllables, Jamo, Compatibility Jamo)
        if ("\uac00" <= ch <= "\ud7af") or ("\u1100" <= ch <= "\u11ff") or ("\u3130" <= ch <= "\u318f"):
            return "Hangul script detected"
        # CJK Ideographs and Japanese Kana
        if ("\u4e00" <= ch <= "\u9fff") or ("\u3400" <= ch <= "\u4dbf") or ("\u3040" <= ch <= "\u30ff"):
            return "CJK or Japanese script detected"
        # Other unexpected foreign scripts (Cyrillic, Devanagari, Greek, Thai)
        if ("\u0400" <= ch <= "\u04ff") or ("\u0900" <= ch <= "\u097f") or ("\u0370" <= ch <= "\u03ff") or ("\u0e00" <= ch <= "\u0e7f"):
            return "unexpected foreign script detected"
        # Invisible characters
        if ch in ("\u200b", "\u200d", "\ufeff"):
            return "unexpected zero-width/invisible character detected"
    # Persian ZWNJ normalization checks
    if "\u200c\u200c" in text:
        return "consecutive ZWNJ detected"
    if re.search(r"\s\u200c|\u200c\s", text):
        return "ZWNJ adjacent to whitespace detected"
    if text.startswith("\u200c") or text.endswith("\u200c"):
        return "boundary ZWNJ detected"
    return None


def definition_meaning_alignment_risk(part_of_speech: str, persian_meaning: str, definition: str) -> str | None:
    pos = part_of_speech.strip().lower()
    if pos not in {"noun", "adjective", "adverb", "pronoun", "preposition", "conjunction"}:
        return None
    segments = [segment.strip() for segment in re.split(r"[؛,،/|]", persian_meaning)]
    verbal = [
        segment
        for segment in segments
        if any(segment == marker or segment.startswith(marker + " ") or segment.endswith(marker) for marker in PERSIAN_VERB_MARKERS)
    ]
    if verbal and definition:
        return f"Persian meaning is verbal but partOfSpeech is {pos}"
    return None


def _english_person_categories(tokens: set[str]) -> set[str]:
    return {category for category, words in ENGLISH_PRONOUNS.items() if tokens.intersection(words)}


def _persian_person_categories(text: str) -> set[str]:
    normalized = text.replace("\u200c", "")
    tokens = set(re.findall(r"[\u0600-\u06ff]+", normalized))
    return {category for category, words in PERSIAN_PRONOUNS.items() if tokens.intersection(words)}


def _number_values(text: str, persian: bool) -> set[int]:
    normalized = str(text).translate(str.maketrans("۰۱۲۳۴۵۶۷۸۹٠١٢٣٤٥٦٧٨٩", "01234567890123456789"))
    values = {int(value) for value in re.findall(r"\d+", normalized)}
    words = re.findall(r"[\u0600-\u06ff]+", normalized) if persian else re.findall(r"[a-z]+", normalized.lower())
    mapping = PERSIAN_NUMBERS if persian else ENGLISH_NUMBERS
    values.update(mapping[word] for word in words if word in mapping)
    return values


def example_translation_alignment_risk(example: str, example_persian: str) -> str | None:
    if not example or not example_persian:
        return None
    english_tokens = set(re.findall(r"[a-z]+", example.lower()))
    english_people = _english_person_categories(english_tokens)
    persian_people = _persian_person_categories(example_persian)
    if len(english_people) == 1 and len(persian_people) == 1 and english_people != persian_people:
        english_person = next(iter(english_people))
        persian_person = next(iter(persian_people))
        return f"English {english_person}-person subject conflicts with Persian {persian_person}-person subject"
    english_numbers = _number_values(example, False)
    persian_numbers = _number_values(example_persian, True)
    if english_numbers and persian_numbers and english_numbers.isdisjoint(persian_numbers):
        return "numeric content differs between English and Persian examples"
    return None


def placeholder_definition_risk(definition: str) -> str | None:
    normalized = " ".join(definition.lower().split()).strip(" .")
    if normalized in {"", "word", "a word", "placeholder", "definition"}:
        return "empty or generic definition"
    if normalized.startswith("a word used in everyday or academic english:"):
        return "generated lemma-echo definition"
    if "vocabulary item in the" in normalized:
        return "dictionary category placeholder"
    if re.search(r";\s*;", definition) or re.search(r";\s*-\s*[A-Z][A-Za-z'-]+\s*$", definition):
        return "definition contains repeated delimiters or source attribution"
    return None


def priority_sense_review_risk(word: str, definition: str, persian_meaning: str, part_of_speech: str = "") -> str | None:
    normalized_word = word.lower()
    priority_words = {"it", "or", "may", "can", "might", "must", "chess", "metabolism", "replicate", "orient", "corpus", "novice"}
    if normalized_word not in priority_words:
        return None
    if normalized_word == "can" and "modal" in part_of_speech.lower():
        meaning_ok = any(marker in persian_meaning for marker in ("توانستن", "اجازه", "ممکن"))
        if meaning_ok:
            return None
    if normalized_word == "metabolism":
        if any(marker in definition.lower() for marker in ("chemical process", "break down substances")) and any(marker in persian_meaning for marker in ("متابولیسم", "متابولیک", "فرایندهای شیمیایی")):
            return None
    if normalized_word == "orient":
        if any(marker in definition.lower() for marker in ("give someone directions", "make them aware", "turn to face", "familiar with a new situation")) and any(marker in persian_meaning for marker in ("جهت‌دهی", "جهت دهی", "جهت‌یابی", "آشنا کردن", "روبه‌رو")):
            return None
    if normalized_word == "replicate":
        if any(marker in definition.lower() for marker in ("exact copy", "repeat a study", "produce a copy")) and any(marker in persian_meaning for marker in ("تکثیر", "بازتولید", "تکرار نتیجه")):
            return None
    if normalized_word == "chess":
        if "board game" in definition.lower() and "شطرنج" in persian_meaning:
            return None
    if normalized_word == "corpus":
        if any(marker in definition.lower() for marker in ("collection of written texts", "body of texts")) and any(marker in persian_meaning for marker in ("مجموعه متون", "پیکره متنی")):
            return None
    if normalized_word == "novice":
        definition_ok = any(marker in definition.lower() for marker in ("new to", "little experience", "beginner"))
        meaning_ok = any(marker in persian_meaning for marker in ("تازه‌کار", "مبتدی", "تجربه کم"))
        if definition_ok and meaning_ok:
            return None
    return "high-risk lemma from editorial checklist"


def collocation_target_risk(word: str, collocations: list[str]) -> str | None:
    values = [str(value).strip() for value in collocations if str(value).strip()]
    if values and not any(target_present(word, value) for value in values):
        return "no collocation contains the target lemma"
    return None


def metadata_outlier_risks(bank: str, row: dict) -> list[str]:
    risks: list[str] = []
    rank = int(row.get("frequencyRank") or 0)
    if bank == "general" and str(row.get("cefrLevel", "")).upper() == "C2" and 0 < rank < 1000:
        risks.append("high-frequency C2 outlier")
    tags = {str(tag).strip().lower() for tag in row.get("tags", [])}
    relevance = [str(row.get(field, "")).strip().lower() for field in ("ieltsRelevance", "toeflRelevance", "greRelevance")]
    if bank == "general" and "nawl" not in tags and any(value == "high" for value in relevance):
        risks.append("High exam relevance without NAWL evidence")
    return risks


def parse_range(range_spec: str) -> tuple[int, int]:
    delim = ":" if ":" in range_spec else "-"
    parts = range_spec.split(delim)
    if len(parts) != 2:
        raise ValueError(f"Invalid range specification '{range_spec}'. Expected format 'start:end' (e.g. '1:100').")
    try:
        start = int(parts[0].strip())
        end = int(parts[1].strip())
    except ValueError as exc:
        raise ValueError(f"Range values must be integers: '{range_spec}'") from exc
    if start < 1:
        raise ValueError(f"Range start must be >= 1, got {start}")
    if end < start:
        raise ValueError(f"Range end ({end}) cannot be less than start ({start})")
    return start, end


def resolve_file(file_arg: str, vocab_root: Path = VOCAB_ROOT) -> Path:
    p = Path(file_arg)
    if p.is_file():
        return p.resolve()
    p_vocab = vocab_root / file_arg
    if p_vocab.is_file():
        return p_vocab.resolve()
    matches = list(vocab_root.rglob(file_arg))
    if len(matches) == 1 and matches[0].is_file():
        return matches[0].resolve()
    elif len(matches) > 1:
        for m in matches:
            if m.name == file_arg:
                return m.resolve()
        return matches[0].resolve()
    raise FileNotFoundError(f"Vocabulary file not found: '{file_arg}'")


def parse_args(argv: list[str] | None = None) -> argparse.Namespace:
    parser = argparse.ArgumentParser(
        description="Quality gate for bundled LinguaFa vocabulary assets."
    )
    parser.add_argument(
        "--file",
        type=str,
        default=None,
        help="Validate specific JSONL file (path relative to repo root, vocabulary root, or absolute).",
    )
    parser.add_argument(
        "--range",
        dest="range_spec",
        type=str,
        default=None,
        help="1-based card line range filter in format 'start:end' (e.g. 1:100 or 301:400).",
    )
    parser.add_argument(
        "--strict",
        action="store_true",
        default=False,
        help="Elevate heuristic warning flags to fatal errors (exit code 1).",
    )
    parser.add_argument(
        "--report",
        type=str,
        default=None,
        help="Custom output path for JSON quality report. Defaults to app/src/main/assets/vocabulary/quality_report.json.",
    )
    return parser.parse_args(argv)


def run_validation(
    files: list[Path] | None = None,
    line_range: tuple[int, int] | None = None,
    strict: bool = False,
    report_path: Path | None = None,
) -> tuple[dict, list[str]]:
    if files is None:
        files = sorted(VOCAB_ROOT.rglob("*.jsonl"))
    if report_path is None:
        report_path = REPORT_PATH

    errors: list[str] = []
    counts = Counter()
    per_bank_words: dict[str, set[str]] = defaultdict(set)
    duplicate_samples: list[str] = []
    example_missing_samples: list[str] = []
    definition_missing_samples: list[str] = []
    proper_noun_samples: list[str] = []
    semantic_findings: list[dict] = []
    semantic_counts = Counter()

    def flag(kind: str, path: Path, line_no: int, word: str, detail: str) -> None:
        semantic_counts[kind] += 1
        rel_posix = rel_label(path)
        if len(semantic_findings) < 500:
            semantic_findings.append({
                "kind": kind,
                "file": rel_posix,
                "line": line_no,
                "word": word,
                "detail": detail,
            })
        if strict:
            errors.append(f"{rel_posix}:{line_no}: {word}: [{kind}] {detail}")

    for path in files:
        bank = bank_key(path)
        rel_posix = rel_label(path)
        for line_no, raw_line in enumerate(path.read_text(encoding="utf-8").splitlines(), start=1):
            if line_range is not None:
                start_l, end_l = line_range
                if line_no < start_l or line_no > end_l:
                    continue

            line = raw_line.strip()
            if not line or line.startswith("#"):
                continue
            try:
                row = json.loads(line)
            except json.JSONDecodeError as exc:
                errors.append(f"{rel_posix}:{line_no}: malformed JSON: {exc}")
                continue

            counts["rows"] += 1
            word = str(row.get("word", "")).strip()
            normalized_word = norm(word)
            meaning = str(row.get("persianMeaning", "")).strip()
            definition = str(row.get("englishDefinition", "")).strip()
            example = str(row.get("example", "")).strip()
            cefr = str(row.get("cefrLevel", "")).strip().upper()

            if not word:
                errors.append(f"{rel_posix}:{line_no}: empty word")
                continue
            if not meaning:
                errors.append(f"{rel_posix}:{line_no}: {word}: missing Persian meaning")
            if not definition:
                errors.append(f"{rel_posix}:{line_no}: {word}: missing English definition")
                if len(definition_missing_samples) < 20:
                    definition_missing_samples.append(f"{bank}:{word}")
            if cefr not in VALID_CEFR:
                errors.append(f"{rel_posix}:{line_no}: {word}: invalid CEFR '{cefr}'")

            pos = str(row.get("partOfSpeech", "")).strip().lower()
            if not pos or pos in {"undefined", "word", "unknown"} or pos not in POS_OK:
                flag("pos_review", path, line_no, word, pos or "(empty)")
            expected_pos = RISKY_SENSES.get(word.lower())
            if expected_pos and expected_pos not in pos:
                flag("closed_class_sense_review", path, line_no, word, "expected %s; got %s" % (expected_pos, pos))
            placeholder = placeholder_definition_risk(definition)
            if placeholder:
                flag("placeholder_definition", path, line_no, word, placeholder)
            meaning_alignment = definition_meaning_alignment_risk(pos, meaning, definition)
            if meaning_alignment:
                flag("definition_meaning_alignment_review", path, line_no, word, meaning_alignment)
            if bank == "general" and row.get("frequencyRank") == 0:
                flag("zero_frequency_rank", path, line_no, word, "frequencyRank is 0")
            for metadata_risk in metadata_outlier_risks(bank, row):
                flag("metadata_outlier_review", path, line_no, word, metadata_risk)

            example_persian = str(row.get("examplePersian", "")).strip()
            # Enhanced foreign script, encoding, and ZWNJ detection
            for field_name, value in (("persianMeaning", meaning), ("examplePersian", example_persian)):
                defect = detect_script_or_encoding_defect(value)
                if defect:
                    flag("unexpected_script_or_encoding", path, line_no, word, f"{field_name}: {defect}")
                    break

            if example and not example_persian:
                flag("missing_example_translation", path, line_no, word, "English example has no translation")
            if example and not target_present(word, example):
                flag("target_word_missing_from_example", path, line_no, word, "review lemma/inflection and sense")
            alignment_risk = example_translation_alignment_risk(example, example_persian)
            if alignment_risk:
                flag("example_translation_alignment_review", path, line_no, word, alignment_risk)
            collocation_risk = collocation_target_risk(word, [str(value) for value in row.get("collocations", [])])
            if collocation_risk:
                flag("collocation_target_review", path, line_no, word, collocation_risk)
            priority_risk = priority_sense_review_risk(word, definition, meaning, pos)
            if priority_risk:
                flag("priority_sense_review", path, line_no, word, priority_risk)

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
                errors.append(f"{rel_posix}:{line_no}: {word}: synthetic collocation family remains")

            if looks_like_c2_proper_noun(row):
                counts["c2_suspicious_names_places"] += 1
                tags = {str(tag).strip() for tag in row.get("tags", [])}
                order = int(row.get("learningOrder") or 0)
                if "low-study-priority" not in tags or order < 100_000:
                    errors.append(f"{rel_posix}:{line_no}: {word}: C2 name/place not down-ranked")
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
        "semanticReview": {
            "findingCounts": dict(semantic_counts),
            "findingsTruncatedAt": 500,
            "findings": semantic_findings,
            "note": "Heuristic flags require editorial review and never invent meanings.",
        },
        "errorCount": len(errors),
        "errors": errors[:100],
    }

    report_path.parent.mkdir(parents=True, exist_ok=True)
    report_path.write_text(json.dumps(report, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    return report, errors


def main(argv: list[str] | None = None) -> None:
    args = parse_args(argv)

    files = None
    if args.file:
        files = [resolve_file(args.file)]

    line_range = None
    if args.range_spec:
        line_range = parse_range(args.range_spec)

    report_path = Path(args.report) if args.report else REPORT_PATH

    report, errors = run_validation(
        files=files,
        line_range=line_range,
        strict=args.strict,
        report_path=report_path,
    )

    print(json.dumps(report, ensure_ascii=False, indent=2))

    if errors:
        raise SystemExit(f"Vocabulary quality gate failed with {len(errors)} error(s).")


if __name__ == "__main__":
    main()
