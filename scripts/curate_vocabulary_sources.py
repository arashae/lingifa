#!/usr/bin/env python3
"""Curate the bundled vocabulary around open, pedagogically useful sources.

This pass does not pretend that an exam provider has a finite official word list. It
adds an explicit General Core pack built from NGSL 1.2 and NAWL 1.2, uses CEFR-J as
an independent level signal, and carries source/licence provenance into each card.
Existing exam packs are enriched in place when a source has a stronger frequency,
level or learner-definition signal. New cards are created only when a Persian
meaning is available from the already-approved EnglishToPersianDictionaries source.

The generated assets are deterministic and can be rebuilt with:
    python scripts/curate_vocabulary_sources.py
"""

from __future__ import annotations

import csv
import io
import json
import re
import shutil
import subprocess
import tempfile
import time
import urllib.request
from collections import defaultdict
from pathlib import Path

from openpyxl import load_workbook

ROOT = Path(__file__).resolve().parents[1]
ASSET_ROOT = ROOT / "app" / "src" / "main" / "assets" / "vocabulary"
CHUNK_SIZE = 500
DATASET_VERSION = "4.0.0"
CATALOG_VERSION = "2026.09.25.1"

NGSL_STATS_URL = "https://www.newgeneralservicelist.com/s/NGSL_12_stats.csv"
NGSL_DEFINITIONS_URL = "https://www.newgeneralservicelist.com/s/NGSL_12_with_English_definitions.xlsx"
NAWL_DEFINITIONS_URL = "https://www.newgeneralservicelist.com/s/NAWL_12_with_en_definitions.csv"
CEFRJ_URL = "https://raw.githubusercontent.com/openlanguageprofiles/olp-en-cefrj/master/cefrj-vocabulary-profile-1.5.csv"
OPENJAM_WORDS_URL = "https://raw.githubusercontent.com/amirj4m/openjam/main/data/json/words_en.json"
OPENJAM_PHONETICS_URL = "https://raw.githubusercontent.com/amirj4m/openjam/main/data/json/phonetics.json"
PERSIAN_REPO = "https://github.com/VahidN/EnglishToPersianDictionaries.git"

GENERAL_PACK_ID = "pack_general_core"
GENERAL_DIR = "general"
GENERAL_PREFIX = "general_core"

POS_MAP = {
    "n": "noun", "v": "verb", "a": "adjective", "adj": "adjective",
    "r": "adverb", "adv": "adverb", "prep": "preposition",
    "conj": "conjunction", "pron": "pronoun", "det": "determiner",
}

# These are deliberately small, hand-reviewed phrases. Empty collocations remain
# preferable to invented adjective+noun combinations.
CURATED_COLLOCATIONS: dict[str, list[str]] = {
    "achieve": ["achieve a goal", "achieve success", "achieve an objective"],
    "acquire": ["acquire knowledge", "acquire a skill", "acquire experience"],
    "adapt": ["adapt to change", "adapt to new conditions", "adapt quickly"],
    "address": ["address an issue", "address a problem", "address concerns"],
    "affect": ["adversely affect", "significantly affect", "directly affect"],
    "alternative": ["alternative approach", "alternative explanation", "viable alternative"],
    "analyze": ["analyze data", "analyze the evidence", "critically analyze"],
    "assess": ["assess the impact", "assess the risk", "assess the validity"],
    "assume": ["assume responsibility", "assume that", "make an assumption"],
    "benefit": ["significant benefit", "mutual benefit", "long-term benefit"],
    "challenge": ["face a challenge", "major challenge", "overcome a challenge"],
    "claim": ["make a claim", "support a claim", "unsubstantiated claim"],
    "coherent": ["coherent argument", "coherent explanation", "coherent strategy"],
    "complex": ["complex process", "complex relationship", "increasingly complex"],
    "conclusion": ["draw a conclusion", "reach a conclusion", "tentative conclusion"],
    "conduct": ["conduct research", "conduct a survey", "conduct an experiment"],
    "contribute": ["contribute to society", "contribute to growth", "make a contribution"],
    "crucial": ["crucial role", "crucial factor", "crucial decision"],
    "decline": ["sharp decline", "decline in demand", "decline rapidly"],
    "demonstrate": ["demonstrate ability", "demonstrate competence", "clearly demonstrate"],
    "determine": ["determine the cause", "determine the outcome", "help determine"],
    "distinguish": ["distinguish between", "distinguish a difference", "clearly distinguish"],
    "diverse": ["culturally diverse", "diverse range", "diverse population"],
    "effect": ["adverse effect", "long-term effect", "profound effect"],
    "emerge": ["emerge as", "emerging trend", "new evidence emerges"],
    "establish": ["establish a relationship", "establish a precedent", "establish credibility"],
    "evaluate": ["evaluate the effectiveness", "critically evaluate", "evaluate evidence"],
    "evidence": ["compelling evidence", "empirical evidence", "substantial evidence"],
    "factor": ["key factor", "contributing factor", "risk factor"],
    "facilitate": ["facilitate learning", "facilitate communication", "facilitate growth"],
    "feature": ["key feature", "distinctive feature", "feature prominently"],
    "generate": ["generate income", "generate evidence", "generate ideas"],
    "impact": ["assess the impact", "positive impact", "negative impact"],
    "implement": ["implement a policy", "implement changes", "effectively implement"],
    "indicate": ["clearly indicate", "strongly indicate", "findings indicate"],
    "influence": ["exert influence", "political influence", "influence behavior"],
    "issue": ["address an issue", "contentious issue", "underlying issue"],
    "maintain": ["maintain standards", "maintain stability", "maintain consistency"],
    "method": ["research method", "effective method", "alternative method"],
    "occur": ["frequently occur", "occur naturally", "an error occurs"],
    "obtain": ["obtain information", "obtain permission", "obtain results"],
    "perspective": ["from the perspective of", "broader perspective", "alternative perspective"],
    "policy": ["public policy", "implement a policy", "policy decision"],
    "provide": ["provide evidence", "provide insight", "provide guidance"],
    "reduce": ["reduce the risk", "reduce costs", "reduce inequality"],
    "require": ["require attention", "require further research", "require evidence"],
    "research": ["conduct research", "empirical research", "research findings"],
    "resource": ["allocate resources", "natural resources", "valuable resource"],
    "result": ["significant result", "yield results", "preliminary result"],
    "role": ["play a role", "crucial role", "pivotal role"],
    "significant": ["significant difference", "significant increase", "statistically significant"],
    "similar": ["similar to", "similar pattern", "broadly similar"],
    "strategy": ["effective strategy", "long-term strategy", "adopt a strategy"],
    "substantial": ["substantial evidence", "substantial increase", "substantial progress"],
    "suggest": ["evidence suggests", "strongly suggest", "suggest that"],
    "trend": ["emerging trend", "upward trend", "downward trend"],
    "vary": ["vary considerably", "vary according to", "vary widely"],
}

# A small set of high-frequency polysemes is curated at the sense level. These
# entries prevent a dictionary's least useful sense from becoming the first card
# for everyday learners.
CURATED_CARD_OVERRIDES: dict[str, dict[str, object]] = {
    "run": {
        "partOfSpeech": "verb",
        "persianMeaning": "دویدن؛ اجرا کردن؛ اداره کردن؛ جریان داشتن",
        "englishDefinition": "To move quickly on foot; to operate or manage something; or to flow.",
        "example": "She runs a small design studio from home.",
        "examplePersian": "او یک استودیوی طراحی کوچک را از خانه اداره می‌کند.",
        "collocations": ["run a business", "run a program", "run out of time"],
    },
    "bank": {
        "partOfSpeech": "noun",
        "persianMeaning": "بانک؛ کناره رودخانه",
        "englishDefinition": "A financial institution, or the land alongside a river or other body of water.",
        "example": "I deposited the money at the bank before walking along the river bank.",
        "examplePersian": "پول را در بانک واریز کردم و بعد کنار رودخانه قدم زدم.",
        "collocations": ["open a bank account", "river bank", "bank loan"],
    },
    "address": {
        "partOfSpeech": "verb",
        "persianMeaning": "رسیدگی کردن به؛ خطاب کردن؛ آدرس",
        "englishDefinition": "To deal with a problem or speak to someone; also, the details used to locate a person or place.",
        "example": "The report addresses the main causes of air pollution.",
        "examplePersian": "این گزارش به علت‌های اصلی آلودگی هوا می‌پردازد.",
        "collocations": ["address an issue", "address concerns", "home address"],
    },
    "charge": {
        "partOfSpeech": "verb",
        "persianMeaning": "هزینه گرفتن؛ شارژ کردن؛ متهم کردن؛ حمله کردن",
        "englishDefinition": "To ask a price, put electricity into a battery, accuse someone, or rush forward.",
        "example": "The hotel does not charge guests for Wi-Fi.",
        "examplePersian": "هتل بابت وای‌فای از مهمانان هزینه‌ای دریافت نمی‌کند.",
        "collocations": ["charge a fee", "charge a battery", "face a charge"],
    },
    "issue": {
        "partOfSpeech": "noun",
        "persianMeaning": "مسئله؛ موضوع مورد اختلاف؛ شماره یا نسخه نشریه",
        "englishDefinition": "An important problem or topic for discussion; also, a particular edition of a magazine or journal.",
        "example": "The committee discussed the issue of affordable housing.",
        "examplePersian": "کمیته درباره مسئله مسکن مقرون‌به‌صرفه بحث کرد.",
        "collocations": ["address an issue", "controversial issue", "current issue"],
    },
    "novel": {
        "partOfSpeech": "adjective",
        "persianMeaning": "نو و بدیع؛ رمان",
        "englishDefinition": "New and unusual; also, a long fictional book.",
        "example": "The researchers proposed a novel approach to treating the disease.",
        "examplePersian": "پژوهشگران رویکردی نو برای درمان بیماری پیشنهاد کردند.",
        "collocations": ["novel approach", "novel idea", "historical novel"],
    },
    "effect": {
        "partOfSpeech": "noun",
        "persianMeaning": "اثر؛ پیامد؛ تأثیر",
        "englishDefinition": "A change or result caused by an action or event.",
        "example": "The new policy had a positive effect on small businesses.",
        "examplePersian": "سیاست جدید تأثیر مثبتی بر کسب‌وکارهای کوچک داشت.",
        "collocations": ["have an effect", "positive effect", "long-term effect"],
    },
    "affect": {
        "partOfSpeech": "verb",
        "persianMeaning": "تأثیر گذاشتن بر",
        "englishDefinition": "To produce a change in someone or something.",
        "example": "Lack of sleep can affect your concentration.",
        "examplePersian": "کمبود خواب می‌تواند بر تمرکز شما تأثیر بگذارد.",
        "collocations": ["adversely affect", "directly affect", "affect the outcome"],
    },
}

# Small closed-class words and technical NAWL lemmas absent from the original
# exam-tagged dump. These translations are editorial seed data, not generated
# placeholders, and are included so the General Core is genuinely broader.
MANUAL_GENERAL_MEANINGS = {
    "the": "حرف تعریف معین؛ آن، این",
    "and": "و",
    "of": "از؛ متعلق به؛ دربارهٔ",
    "to": "به؛ برای؛ نشانۀ مصدر",
    "a": "یک؛ حرف تعریف نامعین",
    "you": "تو؛ شما",
    "for": "برای؛ به‌مدت؛ به‌خاطر",
    "they": "آن‌ها",
    "that": "آن؛ که؛ اینکه",
    "we": "ما",
    "with": "با؛ همراهِ",
    "this": "این",
    "i": "من",
    "at": "در؛ نزد؛ در ساعتِ",
    "she": "او (مونث)",
    "from": "از؛ از طرفِ",
    "by": "به‌وسیلهٔ؛ کنار؛ تا (مهلت)",
    "if": "اگر",
    "would": "می‌خواست؛ می‌کرد؛ (برای بیان حالت فرضی)",
    "which": "کدام؛ که",
    "there": "آنجا؛ وجود دارد",
    "who": "چه کسی؛ کسی که",
    "when": "چه زمانی؛ وقتی که",
    "what": "چه؛ آنچه",
    "up": "بالا؛ به سمت بالا؛ بیدار",
    "some": "مقداری؛ برخی",
    "because": "زیرا؛ چون",
    "could": "می‌توانست؛ ممکن بود",
    "than": "از (در مقایسه)",
    "into": "به داخلِ؛ درونِ",
    "where": "کجا؛ جایی که",
    "should": "باید؛ بهتر است",
    "how": "چگونه؛ چقدر",
    "through": "از میان؛ از طریق؛ در سراسر",
    "something": "چیزی؛ موضوعی",
    "why": "چرا؛ دلیل اینکه",
    "while": "درحالی‌که؛ مدتی که",
    "during": "در طولِ؛ هنگامِ",
    "since": "از زمانی که؛ از آنجا که",
    "under": "زیر؛ تحتِ",
    "without": "بدونِ",
    "against": "برضدِ؛ در برابرِ؛ تکیه‌داده به",
    "though": "اگرچه؛ بااین‌حال",
    "yes": "بله",
    "until": "تا؛ تا زمانی که",
    "whether": "آیا؛ خواه ... خواه",
    "although": "اگرچه؛ با اینکه",
    "within": "درونِ؛ ظرفِ (مدت)",
    "anything": "هر چیزی؛ چیزی (در جمله پرسشی/منفی)",
    "among": "در میانِ؛ بینِ چند نفر یا چیز",
    "toward": "به‌سوی؛ نسبت به",
    "everything": "همه‌چیز",
    "himself": "خودش (مذکر)",
    "else": "دیگر؛ غیر از این",
    "themselves": "خودشان",
    "itself": "خودِ آن؛ خودش",
    "upon": "بر؛ روی؛ به‌محضِ",
    "everyone": "همه؛ هرکس",
    "myself": "خودم",
    "anyone": "هرکس؛ کسی (در پرسش/منفی)",
    "shall": "خواهد؛ باید (رسمی)",
    "yourself": "خودت؛ خودتان",
    "everybody": "همه؛ هرکس",
    "nor": "و نه؛ همچنین نه",
    "herself": "خودش (مونث)",
    "software": "نرم‌افزار",
    "onto": "به رویِ؛ بر رویِ",
    "anybody": "هرکسی؛ کسی (در پرسش/منفی)",
    "ought": "باید؛ شایسته است",
    "ad": "آگهی؛ تبلیغ",
    "ourselves": "خودمان",
    "unlike": "برخلافِ؛ متفاوت از",
    "hi": "سلام",
    "disappoint": "ناامید کردن",
    "bike": "دوچرخه؛ دوچرخه‌سواری کردن",
    "beside": "کنارِ؛ در کنارِ",
    "terrorist": "تروریست؛ فرد تروریست",
    "whenever": "هر وقت که؛ هر زمان که",
    "dialog": "گفت‌وگو؛ دیالوگ",
    "firstly": "نخست؛ اولاً",
    "situate": "قرار دادن؛ واقع شدن",
    "server": "سرور؛ خدمت‌دهنده؛ گارسون",
    "alright": "خوب؛ قابل‌قبول؛ بسیار خوب",
    "blog": "وبلاگ؛ وبلاگ‌نویسی کردن",
    "amongst": "در میانِ",
    "subset": "زیرمجموعه",
    "whichever": "هرکدام که؛ هرچه",
    "whoever": "هرکس که؛ هر کسی",
    "cheers": "به‌سلامتی؛ ممنون؛ تشویق‌ها",
    "connector": "رابط؛ اتصال‌دهنده",
    "industrialize": "صنعتی کردن؛ صنعتی شدن",
    "reactive": "واکنشی؛ واکنش‌پذیر",
    "cyclic": "چرخه‌ای؛ دوره‌ای",
    "demonstrator": "نمایش‌دهنده؛ مدرس عملی؛ معترض",
    "founds": "بنیان می‌گذارد؛ تأسیس می‌کند",
    "globalization": "جهانی‌شدن؛ جهانی‌سازی",
    "randomize": "تصادفی کردن",
    "sustainable": "پایدار؛ قابل‌دوام؛ سازگار با محیط‌زیست",
    "theorist": "نظریه‌پرداز",
    "clone": "شبیه‌سازی کردن؛ شبیه‌سازی‌شده؛ همسانه",
    "nonlinear": "غیرخطی",
    "plural": "جمع؛ چندگانه",
    "convergence": "همگرایی؛ نزدیک‌شدن به یک نقطه",
    "multinational": "چندملیتی",
    "node": "گره؛ گره شبکه؛ گره زیستی",
    "overview": "نمای کلی؛ مرور اجمالی",
    "binary": "دودویی؛ باینری",
    "deflection": "انحراف؛ منحرف‌کردن",
    "encode": "رمزگذاری کردن؛ کدگذاری کردن",
    "epidemiology": "همه‌گیرشناسی",
    "sperm": "اسپرم؛ یاخته نر",
    "chemotherapy": "شیمی‌درمانی",
    "fetal": "جنینی؛ مربوط به جنین",
    "primer": "مقدماتی؛ آستر؛ آغازگر",
    "biodiversity": "تنوع زیستی",
    "circa": "حدودِ؛ تقریباً (در تاریخ)",
    "multi": "چند؛ چندگانه",
    "phonological": "واج‌شناختی؛ مربوط به واج‌ها",
    "sneeze": "عطسه کردن؛ عطسه",
    "denominator": "مخرج (کسر)",
    "helix": "مارپیچ؛ مارپیچ زیستی",
    "semi": "نیمه؛ نیمه‌نهایی",
    "descriptor": "توصیفگر؛ واژه توصیفی",
    "parenthesis": "پرانتز؛ عبارت توضیحی",
    "trans": "ترانس؛ فراتر/آن‌سوی (پیشوند)",
    "factorial": "فاکتوریل؛ مربوط به عامل‌ها",
    "solute": "حل‌شونده؛ ماده حل‌شده",
    "artwork": "اثر هنری",
    "anti": "ضد؛ مخالف (پیشوند)",
    "ex": "سابق؛ همسر یا شریک سابق",
    "micro": "ریز؛ کوچک (پیشوند)",
    "neo": "نو؛ جدید (پیشوند)",
    "non": "غیر؛ ناـ (پیشوند)",
    "pre": "پیش؛ قبل از (پیشوند)",
    "headquarter": "مقر اصلی داشتن؛ در مقر مستقر کردن",
    "descendent": "نسل؛ فرزند؛ از نسلِ",
}


def norm(value: str) -> str:
    value = str(value or "").strip().lower().replace("’", "'")
    return re.sub(r"\s+", " ", value)


def clean(value: object) -> str:
    return re.sub(r"\s+", " ", str(value or "")).strip()


def download(url: str, path: Path) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    last = None
    for attempt in range(4):
        try:
            req = urllib.request.Request(url, headers={"User-Agent": "LinguaFa-vocabulary-curator/1.0"})
            with urllib.request.urlopen(req, timeout=180) as src, path.open("wb") as dst:
                shutil.copyfileobj(src, dst, length=1024 * 1024)
            return
        except Exception as exc:  # noqa: BLE001
            last = exc
            if path.exists():
                path.unlink()
            time.sleep(2 ** attempt)
    raise RuntimeError(f"Could not download {url}: {last}")


def load_persian_dictionary(tmp: Path) -> dict[str, str]:
    repo = tmp / "persian-dictionaries"
    subprocess.run(["git", "clone", "--depth", "1", "--filter=blob:none", PERSIAN_REPO, str(repo)], check=True)
    mapping: dict[str, str] = {}
    # Earlier learner-oriented dictionaries take precedence over generic ones.
    dirs = ["essential-english-words-2", "learn-english", "generic-13", "generic-12", "generic-4", "generic-2"]
    for dirname in dirs:
        folder = repo / "Dictionaries" / dirname
        if not folder.exists():
            continue
        for path in sorted(folder.glob("*.json")):
            try:
                payload = json.loads(path.read_text(encoding="utf-8"))
            except Exception:  # noqa: BLE001
                continue
            for item in payload.get("Words", []):
                word = norm(item.get("EnglishWord"))
                meanings = [clean(x) for x in item.get("Meanings", []) if clean(x)]
                if word and meanings and word not in mapping:
                    mapping[word] = "، ".join(meanings[:4])
    return mapping


def load_sources(tmp: Path) -> tuple[dict[str, dict], dict[str, dict], dict[str, dict], dict[str, str]]:
    ngsl_stats_path = tmp / "ngsl_stats.csv"
    ngsl_defs_path = tmp / "ngsl_definitions.xlsx"
    nawl_path = tmp / "nawl_definitions.csv"
    cefrj_path = tmp / "cefrj.csv"
    openjam_words_path = tmp / "openjam_words.json"
    openjam_phonetics_path = tmp / "openjam_phonetics.json"
    for url, path in [
        (NGSL_STATS_URL, ngsl_stats_path),
        (NGSL_DEFINITIONS_URL, ngsl_defs_path),
        (NAWL_DEFINITIONS_URL, nawl_path),
        (CEFRJ_URL, cefrj_path),
        (OPENJAM_WORDS_URL, openjam_words_path),
        (OPENJAM_PHONETICS_URL, openjam_phonetics_path),
    ]:
        download(url, path)

    ngsl: dict[str, dict] = {}
    for row in csv.DictReader(ngsl_stats_path.read_text(encoding="utf-8-sig").splitlines()):
        word = norm(row.get("Lemma"))
        if word:
            ngsl[word] = {"rank": int(row["SFI Rank"]), "definition": ""}
    workbook = load_workbook(ngsl_defs_path, read_only=True, data_only=True)
    for values in workbook.active.iter_rows(min_row=2, values_only=True):
        word = norm(values[0])
        if word and word in ngsl:
            ngsl[word]["definition"] = clean(values[1])

    nawl: dict[str, dict] = {}
    for rank, row in enumerate(csv.DictReader(nawl_path.read_text(encoding="utf-8-sig").splitlines()), start=1):
        word = norm(row.get("Meanings"))
        if word:
            nawl[word] = {
                "rank": rank,
                "definition": clean(row.get("English Definition")),
                "pos": POS_MAP.get(norm(row.get("POS")), clean(row.get("POS"))),
            }

    cefrj: dict[str, dict] = {}
    for row in csv.DictReader(cefrj_path.read_text(encoding="utf-8-sig").splitlines()):
        raw_word = norm(row.get("headword"))
        word = raw_word.split("/")[0].strip()
        level = clean(row.get("CEFR")).upper()
        if word and level in {"A1", "A2", "B1", "B2", "C1", "C2"} and word not in cefrj:
            cefrj[word] = {"level": level, "pos": POS_MAP.get(norm(row.get("pos")), clean(row.get("pos")))}
    try:
        words_payload = json.loads(openjam_words_path.read_text(encoding="utf-8"))
        word_by_id = {str(item.get("id")): norm(item.get("english")) for item in words_payload if item.get("id")}
        phonetics_payload = json.loads(openjam_phonetics_path.read_text(encoding="utf-8"))
        by_word: dict[str, list[dict]] = defaultdict(list)
        for item in phonetics_payload:
            word = word_by_id.get(str(item.get("word_id")))
            if word and clean(item.get("ipa")):
                by_word[word].append(item)
        openjam_ipa = {}
        for word, values in by_word.items():
            selected = next((x for variant in ("us", "general", "uk") for x in values if x.get("variant") == variant), values[0])
            openjam_ipa[word] = clean(selected.get("ipa"))
    except Exception:  # noqa: BLE001
        openjam_ipa = {}
    return ngsl, nawl, cefrj, openjam_ipa


def load_rows() -> tuple[list[tuple[Path, dict]], dict[str, dict]]:
    entries: list[tuple[Path, dict]] = []
    best: dict[str, dict] = {}
    for path in sorted(ASSET_ROOT.rglob("*.jsonl")):
        if path.parent.name == GENERAL_DIR:
            continue
        lines = path.read_text(encoding="utf-8").splitlines()
        for index, raw in enumerate(lines):
            if not raw.strip() or raw.lstrip().startswith("#"):
                continue
            row = json.loads(raw)
            entries.append((path, row))
            word = norm(row.get("word"))
            if word and word not in best:
                best[word] = row
            elif word and len(clean(row.get("example"))) > len(clean(best[word].get("example"))):
                best[word] = row
    return entries, best


def source_tags(row: dict, ngsl: dict[str, dict], nawl: dict[str, dict], cefrj: dict[str, dict]) -> tuple[str, ...]:
    word = norm(row.get("word"))
    tags = []
    if word in ngsl:
        tags.append("NGSL")
    if word in nawl:
        tags.append("NAWL")
    if word in cefrj:
        tags.append("CEFR-J")
    return tuple(tags)


def learner_definition(row: dict, ngsl: dict[str, dict], nawl: dict[str, dict]) -> str:
    word = norm(row.get("word"))
    candidate = clean(ngsl.get(word, {}).get("definition")) or clean(nawl.get(word, {}).get("definition"))
    current = clean(row.get("englishDefinition"))
    if not candidate:
        return current
    if not current or current.startswith("A word designating") or "Vocabulary item in the" in current:
        return candidate.rstrip(".") + "."
    # ECDICT often contains long semicolon chains. A short learner definition is safer
    # when it is available from NGSL/NAWL.
    if len(current) > 180 and len(candidate) < len(current):
        return candidate.rstrip(".") + "."
    return current


def add_source_metadata(row: dict, ngsl: dict[str, dict], nawl: dict[str, dict], cefrj: dict[str, dict], general: bool = False) -> dict:
    word = norm(row.get("word"))
    tags = [clean(x) for x in row.get("tags", []) if clean(x)]
    for tag in source_tags(row, ngsl, nawl, cefrj):
        if tag not in tags:
            tags.append(tag)
    if general and "GeneralCore" not in tags:
        tags.append("GeneralCore")

    if word in ngsl:
        rank = ngsl[word]["rank"]
        current = int(row.get("frequencyRank") or 0)
        row["frequencyRank"] = min(x for x in (rank, current) if x > 0) if current > 0 else rank
    if word in nawl:
        row["examPriority"] = max(int(row.get("examPriority") or 0), 4)
    elif word in ngsl:
        row["examPriority"] = max(int(row.get("examPriority") or 0), 2)

    if word in cefrj and general:
        row["cefrLevel"] = cefrj[word]["level"]
        row["partOfSpeech"] = cefrj[word]["pos"] or row.get("partOfSpeech", "word")

    row["englishDefinition"] = learner_definition(row, ngsl, nawl)
    if word in CURATED_CARD_OVERRIDES:
        row.update(CURATED_CARD_OVERRIDES[word])
    existing = [clean(x) for x in row.get("collocations", []) if clean(x)]
    if not existing and word in CURATED_COLLOCATIONS:
        row["collocations"] = CURATED_COLLOCATIONS[word]
    elif existing:
        row["collocations"] = existing[:8]
    row["tags"] = tags
    def provenance(*values: str) -> str:
        parts: list[str] = []
        for value in values:
            for part in value.split(";"):
                part = clean(part)
                if part and part not in parts:
                    parts.append(part)
        return "; ".join(parts)

    row["source"] = provenance(clean(row.get("source")), "NGSL 1.2" if word in ngsl else "", "NAWL 1.2" if word in nawl else "", "CEFR-J 1.5" if word in cefrj else "")
    row["sourceLicense"] = provenance(clean(row.get("sourceLicense")), "NGSL/NAWL CC BY-SA 4.0" if word in ngsl or word in nawl else "", "CEFR-J commercial use with attribution" if word in cefrj else "")
    return row


def make_missing_card(word: str, ngsl: dict[str, dict], nawl: dict[str, dict], cefrj: dict[str, dict], persian: dict[str, str], openjam_ipa: dict[str, str], order: int) -> dict | None:
    meaning = clean(persian.get(word)) or clean(MANUAL_GENERAL_MEANINGS.get(word))
    if not meaning:
        return None
    source = nawl.get(word) or ngsl.get(word) or {}
    definition = clean(source.get("definition")) or f"A word used in everyday or academic English: {word}."
    pos = source.get("pos") or cefrj.get(word, {}).get("pos") or "word"
    level = cefrj.get(word, {}).get("level") or "B2"
    row = {
        "word": word,
        "ipa": openjam_ipa.get(word, ""),
        "persianMeaning": meaning,
        "englishDefinition": definition.rstrip(".") + ".",
        "partOfSpeech": pos,
        "example": "",
        "examplePersian": "",
        "cefrLevel": level,
        "synonyms": [], "antonyms": [],
        "collocations": CURATED_COLLOCATIONS.get(word, []),
        "wordFamily": [], "commonMistakes": "",
        "ieltsRelevance": "Medium", "toeflRelevance": "Medium", "greRelevance": "Low",
        "tags": ["General", "GeneralCore", "offline", "NGSL" if word in ngsl else "NAWL", "CEFR-J" if word in cefrj else ""],
        "source": "NGSL 1.2 + NAWL 1.2 + CEFR-J + EnglishToPersianDictionaries",
        "sourceLicense": "NGSL/NAWL CC BY-SA 4.0; CEFR-J commercial use with attribution; EnglishToPersianDictionaries Apache-2.0",
        "frequencyRank": int(ngsl.get(word, {}).get("rank") or 0),
        "examPriority": 4 if word in nawl else 2,
        "learningOrder": order,
    }
    row["tags"] = [x for x in row["tags"] if x]
    return row


def load_existing_general_cards() -> dict[str, dict]:
    """Read existing cards from general core packs to preserve manual curation."""
    out_dir = ASSET_ROOT / GENERAL_DIR
    existing: dict[str, dict] = {}
    if not out_dir.exists():
        return existing
    for path in sorted(out_dir.glob(f"{GENERAL_PREFIX}_*.jsonl")):
        if not path.is_file():
            continue
        try:
            for line in path.read_text(encoding="utf-8").splitlines():
                line = line.strip()
                if not line or line.startswith("#"):
                    continue
                item = json.loads(line)
                word = norm(item.get("word"))
                if word and word not in existing:
                    existing[word] = item
        except Exception:
            continue
    return existing


def write_general_pack(rows: list[dict]) -> list[dict]:
    out_dir = ASSET_ROOT / GENERAL_DIR
    out_dir.mkdir(parents=True, exist_ok=True)
    existing_cards = load_existing_general_cards()

    # Safeguard: preserve existing manual curation edits in general core packs.
    # Existing curated cards take precedence over upstream raw/unreviewed cards.
    final_rows: list[dict] = []
    seen: set[str] = set()
    for row in rows:
        w = norm(row.get("word"))
        if w in existing_cards:
            final_rows.append(existing_cards[w])
        else:
            final_rows.append(row)
        seen.add(w)

    for w, card in existing_cards.items():
        if w not in seen:
            final_rows.append(card)

    final_rows.sort(key=lambda x: (int(x.get("learningOrder") or 99999), norm(x.get("word"))))

    for old in out_dir.glob(f"{GENERAL_PREFIX}_*.jsonl"):
        old.unlink()
    chunks = []
    for chunk_index, start in enumerate(range(0, len(final_rows), CHUNK_SIZE), start=1):
        part = final_rows[start:start + CHUNK_SIZE]
        path = out_dir / f"{GENERAL_PREFIX}_{chunk_index:03d}.jsonl"
        path.write_text("".join(json.dumps(x, ensure_ascii=False, separators=(",", ":")) + "\n" for x in part), encoding="utf-8")
        chunks.append({
            "id": f"general-core-{chunk_index:03d}",
            "version": DATASET_VERSION,
            "packId": GENERAL_PACK_ID,
            "asset": f"vocabulary/{GENERAL_DIR}/{path.name}",
            "expectedItems": len(part),
            "source": "NGSL 1.2 + NAWL 1.2 + CEFR-J + EnglishToPersianDictionaries",
        })
    return chunks


def main() -> None:
    entries, best = load_rows()
    with tempfile.TemporaryDirectory(prefix="linguafa-curate-") as tmpdir:
        tmp = Path(tmpdir)
        print("Downloading NGSL, NAWL and CEFR-J metadata…")
        ngsl, nawl, cefrj, openjam_ipa = load_sources(tmp)
        print("Loading Persian fallback meanings for new Core cards…")
        persian = load_persian_dictionary(tmp)

        touched = 0
        for path, row in entries:
            tags = source_tags(row, ngsl, nawl, cefrj)
            if tags:
                add_source_metadata(row, ngsl, nawl, cefrj)
                touched += 1
            elif norm(row.get("word")) in CURATED_CARD_OVERRIDES:
                # Apply editorial sense fixes to every duplicate occurrence,
                # including legacy exam/CEFR rows that have no source tag.
                add_source_metadata(row, ngsl, nawl, cefrj)

        # Persist the enriched rows back into their source packs. Without this
        # step the metadata would exist only in the generated General Core pack
        # and the installed exam/CEFR cards would keep their old definitions.
        rows_by_path: dict[Path, list[dict]] = defaultdict(list)
        for path, row in entries:
            rows_by_path[path].append(row)
        for path, rows in rows_by_path.items():
            path.write_text(
                "".join(json.dumps(row, ensure_ascii=False, separators=(",", ":")) + "\n" for row in rows),
                encoding="utf-8",
            )

        # Build a General Core pack from the union of NGSL and NAWL. Existing cards
        # are copied and re-leveled with CEFR-J; missing words are admitted only with
        # a real Persian dictionary entry. Existing manual curation edits in general
        # core packs are preserved.
        existing_general = load_existing_general_cards()
        words = sorted(set(ngsl) | set(nawl), key=lambda w: (0 if w in ngsl else 1, ngsl.get(w, {}).get("rank", 99999), nawl.get(w, {}).get("rank", 99999), w))
        general: list[dict] = []
        new_cards = 0
        for order, word in enumerate(words, start=1):
            if word in existing_general:
                card = json.loads(json.dumps(existing_general[word], ensure_ascii=False))
                if "learningOrder" not in card or not card["learningOrder"]:
                    card["learningOrder"] = order
            else:
                base = best.get(word)
                if base:
                    card = json.loads(json.dumps(base, ensure_ascii=False))
                    card = add_source_metadata(card, ngsl, nawl, cefrj, general=True)
                    card["learningOrder"] = order
                else:
                    card = make_missing_card(word, ngsl, nawl, cefrj, persian, openjam_ipa, order)
                    if card:
                        new_cards += 1
            if card:
                general.append(card)

        general.sort(key=lambda x: (int(x.get("learningOrder") or 99999), norm(x.get("word"))))
        chunks = write_general_pack(general)

        catalog_path = ASSET_ROOT / "master_catalog.json"
        catalog = json.loads(catalog_path.read_text(encoding="utf-8"))
        catalog["catalogVersion"] = CATALOG_VERSION
        existing_chunks = []
        for chunk in catalog.get("chunks", []):
            if chunk.get("packId") == GENERAL_PACK_ID:
                continue
            # Existing rows also receive the new source metadata, so their
            # version must change or an installed app would skip them forever.
            chunk["version"] = DATASET_VERSION
            existing_chunks.append(chunk)
        catalog["chunks"] = existing_chunks + chunks
        catalog.setdefault("targets", {})[GENERAL_PACK_ID] = len(general)
        catalog_path.write_text(json.dumps(catalog, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")

        report_path = ASSET_ROOT / "coverage_report.json"
        report = json.loads(report_path.read_text(encoding="utf-8")) if report_path.exists() else {}
        report["datasetVersion"] = DATASET_VERSION
        report["sourceAugmentation"] = {
            "ngslWords": len(ngsl),
            "nawlWords": len(nawl),
            "cefrjWords": len(cefrj),
            "existingRowsTagged": touched,
            "generalCoreCards": len(general),
            "newCardsWithPersianMeaning": new_cards,
            "generalCoreMissingPersian": len(words) - len(general),
        }
        report_path.write_text(json.dumps(report, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")

    sources = """# Bundled vocabulary sources

LinguaFa bundles versioned JSONL vocabulary directly in the Android APK. The app does not need network access to review these cards.

## Primary open sources

- **ECDICT** (`skywind3000/ECDICT`) — MIT. Exam membership, definitions, pronunciation, POS and frequency metadata.
- **Openjam** (`amirj4m/openjam`) — MIT. Persian meanings, examples, IPA and CEFR metadata.
- **EnglishToPersianDictionaries** (`VahidN/EnglishToPersianDictionaries`) — Apache-2.0. Persian fallback meanings.
- **NGSL 1.2** (`newgeneralservicelist.com`) — CC BY-SA 4.0. 2,809 high-frequency general-English lemmas and learner definitions.
- **NAWL 1.2** (`newgeneralservicelist.com`) — CC BY-SA 4.0. 957 academic-English lemmas and learner definitions.
- **CEFR-J Vocabulary Profile 1.5** (`openlanguageprofiles/olp-en-cefrj`) — permitted for research and commercial use with proper citation; used as an independent CEFR signal.

The General Core pack is the union of NGSL and NAWL. A source word is bundled only when the card has a Persian meaning; this keeps the app from showing empty flashcards. Exam packs remain exam-tagged ECDICT banks, enriched with the open general/academic metadata when a word overlaps.

The IELTS, TOEFL and GRE providers do not publish one official finite master vocabulary list. Counts in the app therefore describe the pinned open datasets, not an official guarantee of test coverage.
"""
    (ASSET_ROOT / "SOURCES.md").write_text(sources, encoding="utf-8")
    print(f"Tagged {touched:,} existing rows; wrote {len(general):,} General Core cards ({new_cards:,} new rows); {len(chunks)} chunks.")


if __name__ == "__main__":
    main()
