#!/usr/bin/env python3
"""Build complete offline IELTS/TOEFL/GRE assets for LinguaFa.

Primary exam membership comes from ECDICT's ielts/toefl/gre tags. English
metadata comes from ECDICT. Persian meanings are enriched from Openjam first
and VahidN/EnglishToPersianDictionaries second. The generated JSONL chunks are
committed to app/src/main/assets, therefore they are physically bundled in the
APK and need no network connection at runtime.
"""

from __future__ import annotations

import csv
import json
import re
import shutil
import subprocess
import tempfile
import time
import urllib.request
from collections import defaultdict
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
ASSET_ROOT = ROOT / "app" / "src" / "main" / "assets" / "vocabulary"
CHUNK_SIZE = 500
DATASET_VERSION = "3.0.0"
CATALOG_VERSION = "2026.09.24.3"

ECDICT_CSV = "https://raw.githubusercontent.com/skywind3000/ECDICT/master/ecdict.csv"
OPENJAM_RAW = "https://raw.githubusercontent.com/amirj4m/openjam/main"
PERSIAN_REPO = "https://github.com/VahidN/EnglishToPersianDictionaries.git"

EXAMS = {
    "IELTS": {"tag": "ielts", "pack": "pack_ielts_master", "dir": "ielts", "prefix": "ielts_core", "default_cefr": "B2"},
    "TOEFL": {"tag": "toefl", "pack": "pack_toefl_master", "dir": "toefl", "prefix": "toefl_core", "default_cefr": "B2"},
    "GRE": {"tag": "gre", "pack": "pack_gre_master", "dir": "gre", "prefix": "gre_core", "default_cefr": "C1"},
}

POS_MAP = {
    "n": "noun", "v": "verb", "a": "adjective", "adj": "adjective",
    "r": "adverb", "adv": "adverb", "prep": "preposition",
    "conj": "conjunction", "pron": "pronoun", "num": "numeral",
    "int": "interjection", "det": "determiner",
}


def download(url: str, destination: Path, retries: int = 4) -> None:
    destination.parent.mkdir(parents=True, exist_ok=True)
    last_error = None
    for attempt in range(retries):
        try:
            req = urllib.request.Request(url, headers={"User-Agent": "LinguaFa-vocabulary-builder/1.0"})
            with urllib.request.urlopen(req, timeout=120) as src, destination.open("wb") as dst:
                shutil.copyfileobj(src, dst, length=1024 * 1024)
            return
        except Exception as exc:  # noqa: BLE001
            last_error = exc
            if destination.exists():
                destination.unlink()
            time.sleep(2 ** attempt)
    raise RuntimeError(f"Failed to download {url}: {last_error}")


def load_json(path: Path):
    return json.loads(path.read_text(encoding="utf-8-sig"))


def norm(word: str) -> str:
    return re.sub(r"\s+", " ", (word or "").strip().lower().replace("’", "'"))


def clean_text(value: str | None) -> str:
    if not value:
        return ""
    return re.sub(r"\s+", " ", str(value)).strip()


def parse_int(value) -> int:
    try:
        value = int(str(value).strip())
        return value if value > 0 else 0
    except (TypeError, ValueError):
        return 0


def tags_of(raw: str) -> set[str]:
    return set(re.findall(r"[a-z0-9-]+", (raw or "").lower()))


def parse_pos(raw: str) -> str:
    raw = clean_text(raw).lower()
    if not raw:
        return "word"
    # ECDICT commonly stores POS as forms such as n:63/v:37.
    candidates = re.findall(r"([a-z]+)(?::\d+)?", raw)
    for candidate in candidates:
        if candidate in POS_MAP:
            return POS_MAP[candidate]
    return POS_MAP.get(raw, "word")


def load_openjam(tmp: Path) -> dict[str, dict]:
    files = {
        "words": "data/json/words_en.json",
        "translations": "data/json/translations_fa.json",
        "phonetics": "data/json/phonetics.json",
    }
    local = {}
    for key, rel in files.items():
        path = tmp / f"openjam_{key}.json"
        download(f"{OPENJAM_RAW}/{rel}", path)
        local[key] = load_json(path)

    trans_by_sense: dict[str, list[dict]] = defaultdict(list)
    for item in local["translations"]:
        if item.get("language_code") == "fa" and clean_text(item.get("meaning")):
            trans_by_sense[str(item.get("sense_id"))].append(item)

    phonetic_by_word: dict[str, list[dict]] = defaultdict(list)
    for item in local["phonetics"]:
        phonetic_by_word[str(item.get("word_id"))].append(item)

    result: dict[str, dict] = {}
    for word in local["words"]:
        english = norm(word.get("english", ""))
        if not english:
            continue
        senses = word.get("senses") or []
        meanings: list[str] = []
        translated_sense = None
        translated_obj = None
        for sense in senses:
            translations = trans_by_sense.get(str(sense.get("id")), [])
            for t in translations:
                meaning = clean_text(t.get("meaning"))
                if meaning and meaning not in meanings:
                    meanings.append(meaning)
                if translated_sense is None:
                    translated_sense = sense
                    translated_obj = t
        primary = translated_sense or (senses[0] if senses else {})

        ipa = ""
        phonetics = phonetic_by_word.get(str(word.get("id")), [])
        for preferred in ("us", "general", "uk"):
            match = next((p for p in phonetics if p.get("variant") == preferred and clean_text(p.get("ipa"))), None)
            if match:
                ipa = clean_text(match.get("ipa"))
                break

        result[english] = {
            "persianMeaning": "، ".join(meanings[:4]),
            "englishDefinition": clean_text(primary.get("definition_en")),
            "partOfSpeech": clean_text(primary.get("part_of_speech")),
            "example": clean_text(primary.get("example_en")),
            "examplePersian": clean_text((translated_obj or {}).get("example")),
            "cefrLevel": clean_text(word.get("level")),
            "frequencyRank": parse_int(word.get("frequency_rank")),
            "ipa": ipa,
        }
    return result


def load_persian_dictionary(tmp: Path) -> dict[str, str]:
    repo = tmp / "fa-dicts"
    subprocess.run(
        ["git", "clone", "--depth", "1", "--filter=blob:none", PERSIAN_REPO, str(repo)],
        check=True,
    )

    # Earlier sources have precedence; generic dictionaries fill the gaps.
    dictionary_dirs = [
        "essential-english-words-2",
        "learn-english",
        "generic-13",
        "generic-12",
        "generic-4",
        "generic-2",
    ]
    mapping: dict[str, str] = {}
    for dirname in dictionary_dirs:
        folder = repo / "Dictionaries" / dirname
        if not folder.exists():
            continue
        for path in sorted(folder.glob("*.json")):
            try:
                payload = load_json(path)
            except Exception:  # noqa: BLE001
                continue
            for item in payload.get("Words", []):
                english = norm(item.get("EnglishWord", ""))
                if not english or english in mapping:
                    continue
                meanings = [clean_text(x) for x in (item.get("Meanings") or []) if clean_text(x)]
                if meanings:
                    mapping[english] = "، ".join(meanings[:3])
    return mapping


def load_ecdict(tmp: Path) -> dict[str, list[dict]]:
    csv_path = tmp / "ecdict.csv"
    download(ECDICT_CSV, csv_path)
    by_exam: dict[str, list[dict]] = {exam: [] for exam in EXAMS}
    seen: dict[str, set[str]] = {exam: set() for exam in EXAMS}

    with csv_path.open("r", encoding="utf-8-sig", newline="") as handle:
        reader = csv.DictReader(handle)
        for row in reader:
            english = norm(row.get("word", ""))
            if not english:
                continue
            tags = tags_of(row.get("tag", ""))
            matched = [exam for exam, cfg in EXAMS.items() if cfg["tag"] in tags]
            if not matched:
                continue
            base = {
                "word": english,
                "ipa": clean_text(row.get("phonetic")),
                "englishDefinition": clean_text(row.get("definition")),
                "partOfSpeech": parse_pos(row.get("pos", "")),
                "frequencyRank": parse_int(row.get("frq")) or parse_int(row.get("bnc")),
                "sourceTags": sorted(tags),
            }
            for exam in matched:
                if english not in seen[exam]:
                    seen[exam].add(english)
                    by_exam[exam].append(dict(base))
    return by_exam


def load_existing_seed_meanings() -> dict[str, str]:
    result: dict[str, str] = {}
    for folder in ("ielts", "toefl", "gre"):
        for path in (ASSET_ROOT / folder).glob("*.jsonl"):
            try:
                lines = path.read_text(encoding="utf-8").splitlines()
            except OSError:
                continue
            for line in lines:
                line = line.strip()
                if not line or line.startswith("#"):
                    continue
                try:
                    item = json.loads(line)
                except json.JSONDecodeError:
                    continue
                english = norm(item.get("word") or item.get("text") or "")
                meaning = clean_text(item.get("persianMeaning"))
                if english and meaning:
                    result.setdefault(english, meaning)
    return result


def relevance(tags: set[str], tag: str) -> str:
    return "High" if tag in tags else "Medium"


def enrich(exam: str, rows: list[dict], openjam: dict[str, dict], persian: dict[str, str], seeds: dict[str, str]):
    cfg = EXAMS[exam]
    missing_before_fallback: list[str] = []
    enriched: list[dict] = []

    for position, row in enumerate(rows, start=1):
        word = row["word"]
        oj = openjam.get(word, {})
        meaning = clean_text(oj.get("persianMeaning")) or clean_text(persian.get(word)) or clean_text(seeds.get(word))
        if not meaning:
            missing_before_fallback.append(word)
            meaning = "معنی فارسی در منابع آزاد فعلی پیدا نشد"

        source_tags = set(row.get("sourceTags") or [])
        definition = clean_text(row.get("englishDefinition")) or clean_text(oj.get("englishDefinition"))
        if not definition:
            definition = f"Vocabulary item in the {exam} preparation list."

        pos = row.get("partOfSpeech")
        if not pos or pos == "word":
            pos = clean_text(oj.get("partOfSpeech")) or "word"

        rank = parse_int(oj.get("frequencyRank")) or parse_int(row.get("frequencyRank")) or position
        cefr = clean_text(oj.get("cefrLevel")) or cfg["default_cefr"]
        ipa = clean_text(row.get("ipa")) or clean_text(oj.get("ipa"))

        enriched.append({
            "word": word,
            "ipa": ipa,
            "persianMeaning": meaning,
            "englishDefinition": definition,
            "partOfSpeech": pos,
            "example": clean_text(oj.get("example")),
            "examplePersian": clean_text(oj.get("examplePersian")),
            "cefrLevel": cefr,
            "synonyms": [],
            "antonyms": [],
            "collocations": [],
            "wordFamily": [],
            "commonMistakes": "",
            "ieltsRelevance": relevance(source_tags, "ielts"),
            "toeflRelevance": relevance(source_tags, "toefl"),
            "greRelevance": relevance(source_tags, "gre"),
            "tags": [exam, "offline", "ECDICT"] + sorted(source_tags),
            "source": "ECDICT + Openjam + EnglishToPersianDictionaries",
            "sourceLicense": "ECDICT MIT; Openjam MIT; EnglishToPersianDictionaries Apache-2.0",
            "frequencyRank": rank,
            "examPriority": 3,
        })
    return enriched, missing_before_fallback


def write_assets(exam_data: dict[str, list[dict]], missing: dict[str, list[str]]) -> None:
    chunks = []
    targets = {}

    for exam, rows in exam_data.items():
        cfg = EXAMS[exam]
        out_dir = ASSET_ROOT / cfg["dir"]
        out_dir.mkdir(parents=True, exist_ok=True)
        for old in out_dir.glob(f"{cfg['prefix']}_*.jsonl"):
            old.unlink()

        targets[cfg["pack"]] = len(rows)
        for chunk_index, start in enumerate(range(0, len(rows), CHUNK_SIZE), start=1):
            chunk_rows = rows[start : start + CHUNK_SIZE]
            filename = f"{cfg['prefix']}_{chunk_index:03d}.jsonl"
            path = out_dir / filename
            with path.open("w", encoding="utf-8", newline="\n") as handle:
                for item in chunk_rows:
                    handle.write(json.dumps(item, ensure_ascii=False, separators=(",", ":")) + "\n")
            chunks.append({
                "id": f"{cfg['dir']}-core-{chunk_index:03d}",
                "version": DATASET_VERSION,
                "packId": cfg["pack"],
                "asset": f"vocabulary/{cfg['dir']}/{filename}",
                "expectedItems": len(chunk_rows),
                "source": "ECDICT exam tags + Openjam + EnglishToPersianDictionaries",
            })

    catalog = {
        "schemaVersion": 1,
        "catalogVersion": CATALOG_VERSION,
        "targets": targets,
        "chunks": chunks,
    }
    (ASSET_ROOT / "master_catalog.json").write_text(
        json.dumps(catalog, ensure_ascii=False, indent=2) + "\n", encoding="utf-8"
    )

    report = {
        "datasetVersion": DATASET_VERSION,
        "generatedFrom": {
            "examMembership": "skywind3000/ECDICT (MIT)",
            "primaryPersianEnrichment": "amirj4m/openjam (MIT)",
            "fallbackPersianEnrichment": "VahidN/EnglishToPersianDictionaries (Apache-2.0)",
        },
        "examCounts": {exam: len(rows) for exam, rows in exam_data.items()},
        "missingPersianBeforeFallback": {exam: len(words) for exam, words in missing.items()},
        "missingPersianSamples": {exam: words[:200] for exam, words in missing.items()},
        "chunkSize": CHUNK_SIZE,
    }
    (ASSET_ROOT / "coverage_report.json").write_text(
        json.dumps(report, ensure_ascii=False, indent=2) + "\n", encoding="utf-8"
    )

    sources = """# Bundled vocabulary sources\n\nLinguaFa bundles the generated IELTS, TOEFL and GRE JSONL files directly in the Android APK. Runtime network access is not required to import these packs.\n\n- **ECDICT** (`skywind3000/ECDICT`) — MIT License. Used for IELTS/TOEFL/GRE exam tags, English definitions, pronunciation and POS/frequency metadata.\n- **Openjam** (`amirj4m/openjam`) — MIT License. Used for high-quality Persian translations, examples, IPA and CEFR metadata when available.\n- **EnglishToPersianDictionaries** (`VahidN/EnglishToPersianDictionaries`) — Apache License 2.0. Used as a Persian-meaning fallback.\n\nThe exam providers do not publish one official finite list of every word that may appear on their tests. Therefore “complete” here means the complete set of entries carrying each exam tag in the pinned open datasets above, not an official ETS/IELTS master vocabulary.\n"""
    (ASSET_ROOT / "SOURCES.md").write_text(sources, encoding="utf-8")


def validate(exam_data: dict[str, list[dict]]) -> None:
    for exam, rows in exam_data.items():
        assert rows, f"{exam}: no words generated"
        words = [item["word"] for item in rows]
        assert len(words) == len(set(words)), f"{exam}: duplicate words"
        for item in rows:
            assert item["word"].strip(), f"{exam}: blank word"
            assert item["persianMeaning"].strip(), f"{exam}/{item['word']}: blank Persian meaning"
            assert item["englishDefinition"].strip(), f"{exam}/{item['word']}: blank English definition"


def main() -> None:
    ASSET_ROOT.mkdir(parents=True, exist_ok=True)
    seeds = load_existing_seed_meanings()
    with tempfile.TemporaryDirectory(prefix="linguafa-vocab-") as tmpdir:
        tmp = Path(tmpdir)
        print("Downloading and parsing ECDICT…")
        ecdict = load_ecdict(tmp)
        print("Downloading and parsing Openjam enrichment…")
        openjam = load_openjam(tmp)
        print("Cloning and parsing English→Persian dictionaries…")
        persian = load_persian_dictionary(tmp)

        final: dict[str, list[dict]] = {}
        missing: dict[str, list[str]] = {}
        for exam in EXAMS:
            final[exam], missing[exam] = enrich(exam, ecdict[exam], openjam, persian, seeds)
            print(f"{exam}: {len(final[exam]):,} words; Persian misses before explicit fallback: {len(missing[exam]):,}")

        validate(final)
        write_assets(final, missing)

    total = sum(len(v) for v in final.values())
    print(f"Generated {total:,} exam memberships across {len(EXAMS)} offline packs.")


if __name__ == "__main__":
    main()
