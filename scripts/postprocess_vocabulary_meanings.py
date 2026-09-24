#!/usr/bin/env python3
"""Replace placeholder Persian meanings in generated exam vocabulary assets.

The primary generator intentionally keeps every exam-tagged ECDICT entry, even
when no exact Persian translation exists in the enrichment datasets. This pass
resolves common inflections/derivations against meanings already present in the
bundled banks and applies a reviewed override map for compounds, irregular
forms, abbreviations and rare derivatives. It never removes a vocabulary item.
"""

from __future__ import annotations

import json
import re
from collections import defaultdict
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
ASSET_ROOT = ROOT / "app" / "src" / "main" / "assets" / "vocabulary"
PLACEHOLDER = "معنی فارسی در منابع آزاد فعلی پیدا نشد"

MANUAL = {
    "account for": "توضیح دادن؛ علتِ چیزی بودن؛ تشکیل دادن یا شامل شدن",
    "baby-sitter": "پرستار کودک",
    "closed-minded": "بسته‌ذهن؛ کوته‌فکر",
    "commonsense": "عقل سلیم؛ قضاوت منطقی روزمره",
    "craftspeople": "پیشه‌وران؛ صنعتگران",
    "drop-out": "ترک‌تحصیل‌کرده؛ فردی که دوره‌ای را نیمه‌کاره رها کرده",
    "eye-catching": "چشمگیر؛ جلب‌کنندهٔ توجه",
    "hardbitten": "سخت‌جان؛ باتجربه و واقع‌بین بر اثر تجربه‌های دشوار",
    "healthcare": "مراقبت سلامت؛ خدمات بهداشتی و درمانی",
    "heavy-handedness": "سخت‌گیری یا اعمال قدرت افراطی",
    "helpline": "خط تلفنی راهنما و پشتیبانی",
    "long-lasting": "بادوام؛ ماندگار",
    "low-risk": "کم‌خطر؛ با ریسک پایین",
    "midmorning": "اواسط صبح",
    "non-drinker": "فردی که نوشیدنی الکلی مصرف نمی‌کند",
    "nonflammable": "غیرقابل اشتعال",
    "nonhuman": "غیرانسانی؛ مربوط به موجودی غیر از انسان",
    "nonporous": "غیرمتخلخل؛ نفوذناپذیر",
    "nonprofessional": "غیرحرفه‌ای",
    "nonradioactive": "غیررادیواکتیو",
    "nontraditional": "غیرسنتی",
    "open-book": "کتاب‌باز؛ آزمونی که استفاده از کتاب در آن مجاز است",
    "phone-in": "تماس تلفنی با یک برنامه یا سازمان",
    "springwater": "آب چشمه",
    "thousand-fold": "هزار برابر",
    "trainload": "مقداری به اندازهٔ بار یک قطار",
    "unbridgeable": "غیرقابل پُر کردن یا رفع کردن؛ حل‌نشدنی (شکاف)",
    "undistorted": "بدون اعوجاج؛ تحریف‌نشده",
    "unobstructed": "بدون مانع؛ باز",
    "unpalatable": "بدمزه؛ ناخوشایند یا سخت‌پذیر",
    "unprepossessing": "در نگاه اول غیرجذاب یا نامطلوب",
    "unsubstantiated": "اثبات‌نشده؛ بدون شواهد کافی",
    "unthreatening": "غیرتهدیدآمیز",
    "water-clock": "ساعت آبی",
    "water-proof": "ضد آب؛ نفوذناپذیر در برابر آب",
    "wedge-shaped": "گوه‌ای‌شکل",
    "cacti": "کاکتوس‌ها",
    "larvae": "لاروها",
    "millennia": "هزاره‌ها",
    "stimuli": "محرک‌ها",
    "effluvia": "بخارات یا بوهای ناخوشایند؛ مواد خارج‌شده",
    "minutia": "جزئیات بسیار ریز",
    "muniments": "اسناد و مدارک حقوقی یا مالکیت",
    "lineaments": "خطوط و ویژگی‌های چهره؛ مشخصه‌های متمایز",
    "qualms": "تردیدها یا عذاب وجدان‌ها",
    "naysay": "مخالفت کردن؛ منفی‌بافی کردن",
    "versemonger": "شاعر ضعیف یا شعرساز کم‌مایه",

    # Reviewed exact fallbacks left after the morphology pass.
    "beds": "تخت‌ها؛ بسترها",
    "bedsit": "اتاق یا واحد کوچک یک‌نفره برای خواب و زندگی",
    "booklist": "فهرست کتاب‌ها",
    "cages": "قفس‌ها",
    "cheaper": "ارزان‌تر",
    "classmates": "همکلاسی‌ها",
    "contaminants": "آلاینده‌ها؛ مواد آلوده‌کننده",
    "crops": "محصولات زراعی؛ کشت‌ها",
    "fingers": "انگشتان دست",
    "floors": "کف‌ها؛ طبقات ساختمان",
    "forbes": "فوربز؛ نام خاص و نام مجلهٔ تجاری فوربز",
    "hens": "مرغ‌های ماده",
    "instructors": "مدرسان؛ مربیان",
    "investigators": "بازرسان؛ محققان",
    "kilograms": "کیلوگرم‌ها",
    "mathematic": "ریاضی؛ مربوط به ریاضیات",
    "memorise": "به خاطر سپردن؛ حفظ کردن",
    "ohp": "اورهد پروژکتور؛ دستگاه نمایش شفافیت‌ها",
    "passengers": "مسافران",
    "peasants": "دهقانان؛ کشاورزان روستایی",
    "readers": "خوانندگان",
    "rivers": "رودخانه‌ها",
    "robots": "ربات‌ها",
    "smokers": "افراد سیگاری",
    "tones": "لحن‌ها؛ تُن‌های صدا",
    "travelers": "مسافران؛ سفرکنندگان",
    "universities": "دانشگاه‌ها",
    "visitors": "بازدیدکنندگان؛ مهمانان",

    "bandleader": "رهبر گروه موسیقی",
    "crossbreeding": "دورگه‌گیری؛ آمیزش نژادها یا گونه‌ها",
    "embryological": "رویان‌شناختی؛ مربوط به جنین‌شناسی",
    "facelift": "کشیدن پوست صورت؛ نوسازی یا بهسازی ظاهری",
    "flagella": "تاژک‌ها؛ زائده‌های شلاق‌مانند سلولی",
    "handcrafted": "دست‌ساز؛ ساخته‌شده با دست",
    "iconographic": "شمایل‌نگارانه؛ مربوط به تصویرنگاری و نمادهای تصویری",
    "industrialized": "صنعتی‌شده",
    "interconnected": "به‌هم‌پیوسته؛ مرتبط با یکدیگر",
    "interconnecting": "به‌هم‌پیونددهنده؛ مرتبط‌کننده",
    "locomote": "حرکت کردن از جایی به جای دیگر",
    "multifaceted": "چندوجهی؛ دارای جنبه‌های متعدد",
    "multistory": "چندطبقه",
    "patroller": "مامور گشت؛ گشت‌زن",
    "seamen": "دریانوردان؛ ملوانان",
    "stereophotograph": "عکس استریوسکوپی؛ تصویر سه‌بعدنما",
    "stylized": "سبک‌پردازی‌شده؛ به‌صورت قراردادی بازنمایی‌شده",
    "supercontinent": "ابرقاره",
    "surpassingly": "به‌طور فوق‌العاده؛ بسیار",
    "telecommuter": "دورکار؛ کارمندی که از راه دور کار می‌کند",

    "brattish": "لوس و بچگانه؛ بدرفتار مانند کودک لوس",
    "circumlocutory": "حاشیه‌گو؛ غیرمستقیم و دور از صراحت",
    "daubster": "نقاش ناشی یا بدکار",
    "defoliator": "عامل برگ‌ریز؛ ماده یا جانداری که برگ‌ها را از بین می‌برد",
    "denunciate": "محکوم کردن؛ افشا یا تقبیح کردن",
    "desalinize": "نمک‌زدایی کردن",
    "distention": "اتساع؛ کشیدگی یا تورم",
    "dysfunctional": "ناکارآمد؛ دارای اختلال عملکرد",
    "freelancer": "آزادکار؛ فردی که مستقل و پروژه‌ای کار می‌کند",
    "impressed": "تحت تأثیر قرارگرفته؛ متاثر",
    "leisureliness": "آرامی و بی‌شتابی؛ حالت فراغت‌گونه",
    "oafishness": "زمختی و کودنی؛ رفتار دست‌وپاچلفتی",
    "palings": "نرده‌ها؛ حصارهای چوبی",
    "renunciate": "چشم‌پوشی یا انکار کردن؛ ترک کردن حق یا ادعا",
    "rollicking": "شاد و پرهیاهو؛ سرزنده",
    "scad": "مقدار بسیار زیاد؛ انبوه",
    "underutilized": "کم‌استفاده‌شده؛ کمتر از ظرفیت به‌کاررفته",
    "undisputable": "غیرقابل انکار؛ مسلم",
    "unenlightened": "ناآگاه؛ فاقد آگاهی یا روشنگری",
    "ungrudging": "بی‌دریغ؛ با رضایت و بدون اکراه",
    "unregulated": "تنظیم‌نشده؛ بدون مقررات یا نظارت",
    "unscented": "بی‌بو؛ بدون عطر افزوده",
    "unstinting": "بی‌دریغ؛ فراوان و سخاوتمندانه",
    "vaunting": "خودستایانه؛ لاف‌زنانه",
}

IRREGULAR = {
    "cacti": "cactus", "larvae": "larva", "stimuli": "stimulus",
    "millennia": "millennium", "seamen": "seaman", "craftspeople": "craftsperson",
    "pennies": "penny", "studies": "study", "countries": "country",
    "families": "family", "factories": "factory", "universities": "university",
    "opportunities": "opportunity", "priorities": "priority", "energies": "energy",
    "emergencies": "emergency", "inquiries": "inquiry", "activities": "activity",
}


def normalized(value: str) -> str:
    return re.sub(r"\s+", " ", (value or "").strip().lower().replace("’", "'"))


def candidate_bases(word: str) -> list[str]:
    w = normalized(word)
    out: list[str] = []

    def add(candidate: str) -> None:
        candidate = normalized(candidate)
        if candidate and candidate != w and candidate not in out:
            out.append(candidate)

    if w in IRREGULAR:
        add(IRREGULAR[w])

    if "-" in w:
        add(w.replace("-", " "))
        add(w.replace("-", ""))
        parts = w.split("-")
        if len(parts) > 1:
            add(parts[-1])
            add(parts[0])
    if " " in w:
        parts = w.split()
        if len(parts) > 1:
            add(parts[0])
            add(parts[-1])

    if w.endswith("ies") and len(w) > 4:
        add(w[:-3] + "y")
    if w.endswith("ves") and len(w) > 4:
        add(w[:-3] + "f")
        add(w[:-3] + "fe")
    if w.endswith("sses") or w.endswith("shes") or w.endswith("ches") or w.endswith("xes") or w.endswith("zes"):
        add(w[:-2])
    if w.endswith("es") and len(w) > 4:
        add(w[:-2])
        add(w[:-1])
    if w.endswith("s") and not w.endswith("ss") and len(w) > 3:
        add(w[:-1])

    if w.endswith("ying") and len(w) > 5:
        add(w[:-4] + "ie")
    if w.endswith("ing") and len(w) > 5:
        stem = w[:-3]
        add(stem)
        add(stem + "e")
        if len(stem) >= 2 and stem[-1] == stem[-2]:
            add(stem[:-1])

    if w.endswith("ied") and len(w) > 4:
        add(w[:-3] + "y")
    if w.endswith("ed") and len(w) > 4:
        stem = w[:-2]
        add(stem)
        add(stem + "e")
        if len(stem) >= 2 and stem[-1] == stem[-2]:
            add(stem[:-1])

    if w.endswith("iest") and len(w) > 5:
        add(w[:-4] + "y")
    if w.endswith("ier") and len(w) > 4:
        add(w[:-3] + "y")
    if w.endswith("est") and len(w) > 5:
        add(w[:-3])
        add(w[:-3] + "e")
    if w.endswith("er") and len(w) > 4:
        add(w[:-2])
        add(w[:-1])

    suffixes = (
        ("ically", "ic"), ("ally", "al"), ("ily", "y"), ("ly", ""),
        ("lessness", "less"), ("ness", ""), ("ments", ""), ("ment", ""),
        ("ation", "ate"), ("ization", "ize"), ("isation", "ise"),
        ("izer", "ize"), ("iser", "ise"), ("able", ""), ("ible", ""),
        ("al", ""), ("ical", ""), ("ous", ""), ("ish", ""),
    )
    for suffix, replacement in suffixes:
        if w.endswith(suffix) and len(w) > len(suffix) + 2:
            add(w[:-len(suffix)] + replacement)

    for prefix in ("un", "non", "re", "over", "under", "semi", "inter", "sub"):
        if w.startswith(prefix) and len(w) > len(prefix) + 3:
            add(w[len(prefix):])

    return out


def load_rows():
    files = []
    for exam in ("ielts", "toefl", "gre"):
        files.extend(sorted((ASSET_ROOT / exam).glob("*_core_*.jsonl")))
    rows_by_file: dict[Path, list[dict]] = {}
    for path in files:
        rows = []
        for line in path.read_text(encoding="utf-8").splitlines():
            if line.strip():
                rows.append(json.loads(line))
        rows_by_file[path] = rows
    return rows_by_file


def main() -> None:
    rows_by_file = load_rows()
    meanings: dict[str, str] = {}
    placeholder_before = defaultdict(int)
    for path, rows in rows_by_file.items():
        exam = path.parent.name.upper()
        for item in rows:
            word = normalized(item.get("word", ""))
            meaning = (item.get("persianMeaning") or "").strip()
            if meaning == PLACEHOLDER:
                placeholder_before[exam] += 1
            elif word and meaning:
                meanings.setdefault(word, meaning)
    meanings.update(MANUAL)

    resolved = defaultdict(int)
    unresolved = defaultdict(list)
    changed_files = 0

    for path, rows in rows_by_file.items():
        exam = path.parent.name.upper()
        changed = False
        for item in rows:
            if (item.get("persianMeaning") or "").strip() != PLACEHOLDER:
                continue
            word = normalized(item.get("word", ""))
            meaning = MANUAL.get(word)
            source_word = word if meaning else None
            if not meaning:
                for candidate in candidate_bases(word):
                    candidate_meaning = meanings.get(candidate)
                    if candidate_meaning and candidate_meaning != PLACEHOLDER:
                        meaning = candidate_meaning
                        source_word = candidate
                        break
            if meaning:
                item["persianMeaning"] = meaning
                item["tags"] = list(dict.fromkeys((item.get("tags") or []) + ["persian-fallback-resolved"]))
                if source_word and source_word != word:
                    item["source"] = (item.get("source") or "") + f"; Persian fallback via lemma:{source_word}"
                resolved[exam] += 1
                meanings.setdefault(word, meaning)
                changed = True
            else:
                unresolved[exam].append(word)

        if changed:
            path.write_text(
                "".join(json.dumps(item, ensure_ascii=False, separators=(",", ":")) + "\n" for item in rows),
                encoding="utf-8",
            )
            changed_files += 1

    report_path = ASSET_ROOT / "coverage_report.json"
    report = json.loads(report_path.read_text(encoding="utf-8"))
    report["placeholderPersianBeforePostprocess"] = {exam: placeholder_before.get(exam, 0) for exam in ("IELTS", "TOEFL", "GRE")}
    report["persianFallbackResolved"] = {exam: resolved.get(exam, 0) for exam in ("IELTS", "TOEFL", "GRE")}
    report["placeholderPersianAfterPostprocess"] = {exam: len(unresolved.get(exam, [])) for exam in ("IELTS", "TOEFL", "GRE")}
    report["placeholderPersianSamplesAfterPostprocess"] = {exam: unresolved.get(exam, [])[:100] for exam in ("IELTS", "TOEFL", "GRE")}
    report_path.write_text(json.dumps(report, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")

    print(f"Updated {changed_files} chunk files")
    print("Placeholders before:", dict(placeholder_before))
    print("Resolved:", dict(resolved))
    print("Remaining placeholders:", {k: len(v) for k, v in unresolved.items()})
    if any(unresolved.values()):
        raise SystemExit("Unresolved Persian placeholders remain; add reviewed fallbacks before release")


if __name__ == "__main__":
    main()
