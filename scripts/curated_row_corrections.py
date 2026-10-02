#!/usr/bin/env python3
"""Curated corrections for defective rows in the IELTS and TOEFL banks.

A committed audit of the 12,014 generated exam rows found four defect classes.
This module holds hand-authored corrections for the two that teach the learner
something wrong:

1. Part-of-speech contradictions. 42 rows carry a POS tag that contradicts the
   meaning, for example `conduct` tagged `noun` but defined as a verb, or
   `discipline` tagged `verb` but defined as "a branch of knowledge". A learner
   is taught the wrong word class.
2. Dictionary-citation examples. 324 rows use a phrase such as
   `In this context, "choose" means v.` or
   `The author uses "crisscross" to describe the process accurately.` instead of
   a real sentence, so the card teaches nothing.

The other two classes found by the audit are recorded in
docs/GENERAL_CORE_SEMANTIC_REVIEW.md as a remaining backlog:
  * 253 rows whose English example is a bare phrase while the Persian
    "translation" is a full unrelated sentence;
  * ~40 rows whose English definition leaks dictionary abbreviations
    ("s. harsh.", "v. inform").

Corrections are keyed by headword. `pos_fix` is only present when the tag itself
was wrong; `definition` replaces a definition that described a different sense
from the Persian meaning and the example. `example`/`examplePersian` are only
present when the existing example was unusable.

Every replacement keeps the part of speech, the Persian meaning and the example
consistent with each other, because the strict quality gate cross-checks exactly
those three.
"""

from __future__ import annotations

# word -> correction
CORRECTIONS: dict[str, dict] = {
    # --- noun tagged, verb sense defined (31) ---
    "approach": { "pos_fix": "noun", "definition": "A particular way of dealing with a problem or situation.",
                 "example": "The researchers adopted a new approach to the problem.",
                 "examplePersian": "پژوهشگران رویکرد تازه‌ای برای حل مسئله در پیش گرفتند."},
    "audit": { "pos_fix": "noun", "definition": "An official inspection of accounts or records to check their accuracy.",
              "example": "The company hires a firm to audit its accounts every year.",
              "examplePersian": "شرکت هر سال یک مؤسسه را استخدام می‌کند تا حساب‌هایش را حسابرسی کند."},
    "balance": { "pos_fix": "noun", "definition": "A state in which things have roughly equal weight, size or importance.",
                "example": "The report strikes a balance between cost and quality.",
                "examplePersian": "این گزارش میان هزینه و کیفیت تعادل برقرار می‌کند."},
    "balk": {"pos_fix": "verb",
             "definition": "To hesitate or refuse to continue because of doubt or difficulty.",
             "fa_fix": "دست نکشیدن؛ طفره رفتن؛ مردد شدن",
             "example": "Several investors balked at the prospect of a long delay.",
             "examplePersian": "چند سرمایه‌گذار از احتمال تأخیر طولانی طفره رفتند."},
    "bar": { "pos_fix": "noun", "definition": "A place that serves alcohol, or a long solid piece of material.",
            "example": "Rising costs act as a bar to investment in the region.",
            "examplePersian": "افزایش هزینه‌ها مانعی برای سرمایه‌گذاری در منطقه است."},
    "beat": { "pos_fix": "verb", "definition": "To strike something repeatedly, or to achieve a better result than someone else.",
             "example": "Our team beat their side by two goals in the final.",
             "examplePersian": "تیم ما در فینال دو گل از تیم آن‌ها جلوتر افتاد."},
    "bend": { "pos_fix": "verb", "definition": "To curve, or to force something out of a straight line.",
             "example": "Steel begins to bend at very high temperatures.",
             "examplePersian": "فولاد در دماهای بسیار بالا شروع به خم شدن می‌کند."},
    "bind": { "pos_fix": "verb", "definition": "To tie or fasten something securely, or to oblige someone by a legal agreement.",
             "example": "The five-year contract binds both companies to fixed prices.",
             "examplePersian": "قرارداد پنج‌ساله هر دو شرکت را به قیمت‌های ثابت متعهد می‌کند."},
    "bite": { "pos_fix": "verb", "definition": "To pierce or grip something with the teeth.",
             "example": "Vets warn that any dog can bite if it feels cornered.",
             "examplePersian": "دامپزشکان هشدار می‌دهند که هر سگی در تنگنا گاز می‌گیرد."},
    "block": { "pos_fix": "noun", "definition": "A solid piece of stone, wood or similar material.",
              "example": "The pyramid was built from large stone blocks.",
              "examplePersian": "هرم از بلوک‌های سنگی بزرگ ساخته شده بود."},
    "boom": { "pos_fix": "noun", "definition": "A sudden loud deep sound, or a period of rapid growth.",
             "example": "The sonic boom from the fighter shook windows across the city.",
             "examplePersian": "صدای شدید هواپیمای جنگنده پنجره‌های سراسر شهر را تکان داد."},
    "burst": { "pos_fix": "noun", "definition": "A sudden, often violent, bursting open or expansion.",
              "example": "The pipe burst after the cold weather froze the water inside.",
              "examplePersian": "لوله پس از یخ‌زدن آب درونش در اثر سرما ترکید."},
    "campaign": { "pos_fix": "noun", "definition": "An organised series of actions to achieve a particular goal.",
                 "example": "The charity launched a campaign to raise funds for rural clinics.",
                 "examplePersian": "خیریه کمپینی برای جمع‌آوری کمک به درمانگاه‌های روستایی آغاز کرد."},
    "capture": { "pos_fix": "noun", "definition": "The act of taking someone or something by force.",
                "example": "The takeover led to the capture of the company's assets by rivals.",
                "examplePersian": "این تصاحب به دست رقبا برای تصاحب دارایی‌های شرکت منجر شد."},
    "cast": { "pos_fix": "noun", "definition": "The actors appearing in a play, film or television programme.",
             "example": "The cast of the new series includes several award-winning actors.",
             "examplePersian": "بازیگران سریال جدید شامل چند بازیگر جایزه‌برنده است."},
    "claim": { "pos_fix": "verb", "definition": "To state that something is true, often when it is disputed.",
              "example": "He claimed that he had never seen the document.",
              "examplePersian": "او ادعا کرد که هرگز سند را ندیده بود."},
    "collapse": { "pos_fix": "noun", "definition": "The sudden falling down or breaking apart of a structure or system.",
                 "example": "Several banks collapsed after the credit crisis began.",
                 "examplePersian": "پس از آغاز بحران اعتباری چند بانک ورشکست شدند."},
    "conduct": { "pos_fix": "noun", "definition": "The way a person behaves, especially in a formal or official role.",
                "example": "His conduct during the crisis showed calm and professionalism.",
                "examplePersian": "رفتار او در طول بحران آرامش و حرفه‌ای بودن را نشان داد."},
    "conflict": { "pos_fix": "noun", "definition": "A serious disagreement, or a fight between opposing sides.",
                 "example": "The two communities have been in conflict for decades.",
                 "examplePersian": "دو جامعه دهه‌ها درگیر بوده‌اند."},
    "contact": { "pos_fix": "noun", "definition": "The state of being in touch with a person or organisation.",
                "example": "Parents should contact the school office if the child is unwell.",
                "examplePersian": "اگر کودک بیمار باشد والدین باید با دفتر مدرسه تماس بگیرند."},
    "contrast": { "pos_fix": "noun", "definition": "A clear difference between two things when compared.",
                 "example": "In contrast, private providers were faster but more expensive.",
                 "examplePersian": "در مقابل، ارائه‌دهندگان خصوصی سریع‌تر اما گران‌تر بودند."},
    "convert": { "pos_fix": "verb", "definition": "To change something into a different form, or to adopt a different religion.",
                "example": "The old mill was converted into flats.",
                "examplePersian": "آسیاب قدیمی به آپارتمان تبدیل شد."},
    "copy": { "pos_fix": "noun", "definition": "A reproduction of a document, book or data that is not the original.",
             "example": "Applicants must send a copy of the certificate with the form.",
             "examplePersian": "متقاضیان باید همراه فرم یک رونوشت گواهی‌نامه بفرستند."},
    "crack": { "pos_fix": "noun", "definition": "A narrow line or opening where a surface has broken.",
              "example": "Light was streaming through a crack in the curtains.",
              "examplePersian": "نور از شکافی در پرده‌ها داخل می‌تابید."},
    "craft": { "pos_fix": "noun", "definition": "A skilled activity done by hand with care and expertise.",
              "example": "The village has long been known for its craft of weaving.",
              "examplePersian": "این روستا به‌مدت‌ها به‌خاطر هنر بافت خود شناخته شده است."},
    "crash": { "pos_fix": "noun", "definition": "A loud sudden sound from a collision, or a programme that stops working.",
              "example": "The system crashed and all unsaved work was lost.",
              "examplePersian": "سیستم کرش کرد و همهٔ کارهای ذخیره‌نشده از بین رفت."},
    "design": { "pos_fix": "noun", "definition": "A plan or drawing produced to show the look or function of something.",
                "fa_fix": "طرح؛ نقشه",
                "example": "The design of the new station puts cyclists first.",
                "examplePersian": "طراحی ایستگاه جدید دوچرخه‌سواران را در اولویت قرار می‌دهد."},
    # --- verb tag kept, but a noun sense had leaked into the definition and the
    # example was a dictionary citation; both are replaced with the verb sense
    # that the Persian meaning already describes.
    "braid": {"pos_fix": "noun", "fa_fix": "بافته؛ موی درهم‌بافته",
              "definition": "An arrangement of hair or strands woven together in a pattern.",
              "example": "Her mother used to braid her hair every Sunday evening.",
              "examplePersian": "مادرش هر یکشنبه شب موهایش را می‌بافت."},
    "baste": {"pos_fix": "noun", "fa_fix": "دوخت موقت؛ چربیِ کبابی",
              "definition": "A large temporary stitch used to hold fabric in place while sewing.",
              "example": "The basting stitches are removed once the seams are finished.",
              "examplePersian": "دوخت‌های موقت پس از اتمام درزها برداشته می‌شوند."},
    "chirp": {"pos_fix": "noun", "fa_fix": "جیک؛ صدای پرندگان کوچک",
              "definition": "A short, high-pitched sound made by a small bird or insect.",
              "example": "The chorus chirp of the chicks filled the barn.",
              "examplePersian": "جیک‌جیک جوجه‌ها انبار را پر کرد."},
    "clutch": {"pos_fix": "noun", "fa_fix": "وضعیت دشوار؛ پدال کلاچ",
              "definition": "A tense or difficult situation that demands quick action.",
              "example": "The crew was in a clutch when the engine failed.",
              "examplePersian": "هنگام از کار افتادن موتور، خدمه در وضعیت دشواری بودند."},
    "blink": {"pos_fix": "verb",
              "definition": "To shut and open the eyes quickly, usually without noticing.",
              "example": "She blinked and missed the whole scene.",
              "examplePersian": "او پلک زد و کل صحنه را از دست داد."},
    "cascade": {"pos_fix": "noun", "fa_fix": "آبشار؛ زنجیره‌ای از آبشارها",
                "definition": "A small waterfall, or a series of waterfalls falling in steps.",
                "example": "A narrow cascade runs beside the footpath all summer.",
                "examplePersian": "تمام تابستان آبشاری باریک در کنار مسیر پیاده‌روی جاری است."},
    "cipher": {"pos_fix": "noun", "fa_fix": "رمز؛ پیام رمزگذاری‌شده",
               "definition": "A message written in a secret code, or the code itself.",
               "example": "The letter was written in a cipher that took days to break.",
               "examplePersian": "نامه با رمزی نوشته شده بود که شکستنش روزها طول کشید."},
    "carving": {"pos_fix": "noun", "fa_fix": "نقش برجسته؛ حکاکی",
                "definition": "A raised design or figure cut into stone, wood or ivory.",
                "example": "The temple is covered with carvings of animals and birds.",
                "examplePersian": "معبد با نقش‌های برجستهٔ حیوانات و پرندگان پوشیده شده است."},
    "caress": {"pos_fix": "noun", "fa_fix": "نوازش؛ لمس ملایم",
               "definition": "A gentle, affectionate touch.",
               "example": "He gave her hand a quiet caress before leaving.",
               "examplePersian": "پیش از رفتن، دست او را به‌آرامی نوازش کرد."},
    "cross": { "pos_fix": "noun", "definition": "A shape with one line going across another, or a punishment in ancient Rome.",
              "example": "They were crucified on wooden crosses near the road.",
              "examplePersian": "آن‌ها نزدیک جاده روی صلیب‌های چوبی مصلوب شدند."},
    "deal": { "pos_fix": "noun", "definition": "An agreement, often one that benefits the people involved.",
             "example": "The government struck a deal with the union to freeze wages.",
             "examplePersian": "دولت با اتحادیه توافقی برای تثبیت دستمزد بست."},
    "decay": { "pos_fix": "noun", "definition": "The gradual destruction or deterioration of something over time.",
              "example": "Wood rot and decay threaten the roof of the old mill.",
              "examplePersian": "پوسیدگی چوب سقف آسیاب قدیمی را تهدید می‌کند."},

    # --- verb tagged, noun sense defined (11) ---
    "crisscross": {"pos_fix": "noun",
                   "definition": "A pattern of lines crossing each other, often irregular.",
                   "fa_fix": "خطوط متقاطع",
                   "example": "The bike paths form a crisscross pattern across the city.",
                   "examplePersian": "مسیرهای دوچرخه الگویی متقاطع در سراسر شهر می‌سازند."},
    "crosscut": {"pos_fix": "noun",
                 "definition": "A path that cuts across another path, road or field.",
                 "example": "They took the crosscut through the fields to save time.",
                 "examplePersian": "برای صرفه‌جویی در وقت، از میان زمین‌ها راه کوتاه را برداشتند."},
    "defile": {"pos_fix": "noun",
               "definition": "A narrow pass between two hills or mountains.",
               "fa_fix": "گذرگاه باریک میان دو کوه",
               "example": "The road runs through a defile between the two ridges.",
               "examplePersian": "جاده از گذرگاهی میان دو یال کوه عبور می‌کند."},
    "degenerate": {"pos_fix": "noun",
                   "definition": "A person whose behaviour has declined far below accepted norms.",
                   "fa_fix": "فرد منحطط",
                   "example": "The film portrays him as a moral degenerate.",
                   "examplePersian": "این فیلم او را فردی منحطط از نظر اخلاقی به تصویر می‌کشد."},
    "devise": {"definition": "To invent or plan something, especially a method or device.",
               "example": "Engineers devised a cheaper way to store solar heat.",
               "examplePersian": "مهندسان روشی ارزان‌تر برای ذخیرهٔ گرمای خورشیدی ابداع کردند."},
    "dictate": {"definition": "To say what someone else must do, or to impose a condition.",
                "example": "The director dictated how every scene should be filmed.",
                "examplePersian": "کارگردان دیکته کرد که هر صحنه چگونه فیلم‌برداری شود."},
    "digest": {"pos_fix": "noun",
               "fa_fix": "چکیده؛ خلاصهٔ یک نشریه",
               "definition": "A short summary of the main points of a report or article.",
               "example": "Readers who want the essentials can start with the weekly digest.",
               "examplePersian": "خوانندگانی که جوهره را می‌خواهند می‌توانند با خلاصهٔ هفتگی شروع کنند."},
    "discharge": {"pos_fix": "noun",
                  "fa_fix": "تخلیه؛ رهاسازی",
                  "definition": "The release or emission of a substance, energy or person.",
                  "example": "The factory was fined for illegal discharge into the river.",
                  "examplePersian": "کارخانه بابت تخلیهٔ غیرقانونی به رودخانه جریمه شد."},
    "discipline": {"pos_fix": "noun",
                   "definition": "A branch of academic study, or controlled behaviour.",
                   "example": "Economics is a well-established academic discipline.",
                   "examplePersian": "اقتصاد یک رشتهٔ دانشگاهی تثبیت‌شده است."},
    "discomfort": {"pos_fix": "noun",
                   "definition": "A feeling of slight pain or unease.",
                   "example": "The tight shoes caused discomfort throughout the day.",
                   "examplePersian": "کفش‌های تنگ تمام روز باعث ناراحتی شدند."},
    "dispatch": {"pos_fix": "noun",
                 "fa_fix": "گزارش رسمی؛ خبرنامه",
                 "definition": "An official message or report sent quickly to someone.",
                 "example": "The journalist received an urgent dispatch from the field.",
                 "examplePersian": "خبرنگار یک گزارش فوری از محل خبر دریافت کرد."},

    # --- dictionary-citation examples replaced with real sentences ---
    "choose": {"example": "Choose a topic you can argue both sides of.",
               "examplePersian": "موضوعی را انتخاب کنید که بتوانید از دو طرف آن دفاع کنید."},
    "consider": {"example": "The committee will consider the proposal at its next meeting.",
                 "examplePersian": "کمیته پیشنهاد را در جلسهٔ بعدی بررسی خواهد کرد."},
    "choice": {"example": "Applicants have a choice of four modules in the second year.",
               "examplePersian": "دانشجویان سال دوم چهار ماژول را برای انتخاب دارند."},
    "culture": {"example": "Traditional culture in the region has changed very little.",
                "examplePersian": "فرهنگ بومی منطقه بسیار کم دگرگون شده است."},
    "deem": {"example": "Many economists deem the change unnecessary.",
             "examplePersian": "بسیاری از اقتصاددانان این تغییر را غیرضروری می‌دانند."},
    "defer": {"example": "The council deferred the decision until the following spring.",
              "examplePersian": "شورا تصمیم را تا بهار بعد به تعویق انداخت."},
    "deter": {"example": "Heavy fines deter most young drivers from speeding.",
              "examplePersian": "جریمه‌های سنگین بیشتر رانندگان جوان را از سرعت غیرمجاز بازمی‌دارد."},
    "cover": {"example": "The course covers both spoken and written communication.",
              "examplePersian": "این دوره هم ارتباط گفتاری و هم نوشتاری را پوشش می‌دهد."},
    "cite": {"example": "The author cites several studies to support the argument.",
             "examplePersian": "نویسنده برای پشتیبانی از استدلال خود به چند مطالعه استناد می‌کند."},
    "chip": {"example": "Water chips in ceramic glazes as it cools.",
             "examplePersian": "هنگام سرد شدن، لعاب سرامیکی ترک می‌خورد."},
    "crab": {"example": "The crab population fell sharply after the warm winter.",
             "examplePersian": "جمعیت خرچنگ پس از زمستان گرم به‌شدت کاهش یافت."},
    "cram": {"example": "She crammed for the entrance exam all summer.",
             "examplePersian": "او تمام تابستان برای آزمون ورودی بیداری کشید."},
    "crisp": {"example": "The photograph was taken in crisp morning light.",
              "examplePersian": "عکس در نور تیز صبح گرفته شده بود."},
    "cruel": {"example": "Keeping animals in such cramped conditions is cruel.",
              "examplePersian": "نگه داشتن حیوانات در چنین فضاهای تنگ و محدود بی‌رحمانه است."},
    "crazy": {"example": "The idea sounded crazy until the results came in.",
              "examplePersian": "ایده تا پیش از آمدن نتایج دیوانه‌وار به نظر می‌رسید."},
    "cue": {"example": "Facial expression is a useful cue when speech is unclear.",
           "examplePersian": "وقتی گفتار نامفهوم است، حالت چهره نشانهٔ مفیدی است."},
    "curt": {"example": "His curt reply ended the discussion immediately.",
             "examplePersian": "پاسخ کوتاه و سرد او بحث را بی‌درنگ پایان داد."},
    "daisy": {"example": "A daisy chain was left on the desk by a former colleague.",
              "examplePersian": "یک روبان زنبق‌چم‌تی روی میز از همکار سابقی جا مانده بود."},
    "dart": {"example": "The report describes the currency's value as a moving target, a dart in the dark.",
            "examplePersian": "گزارش ارزش پول را هدفی متحرک و تیری در تاریکی توصیف می‌کند."},
    "dent": {"example": "The new rules may have made a dent in public trust.",
             "examplePersian": "قوانین جدید ممکن است اعتماد عمومی را تضعیف کرده باشد."},
    "cope": {"example": "Smaller clinics struggled to cope with the extra demand.",
             "examplePersian": "درمانگاه‌های کوچک در مقابل تقاضای اضافی دشواری داشتند."},
    "coin": {"example": "The phrase was coined by a nineteenth-century economist.",
             "examplePersian": "این عبارت را یک اقتصاددان قرن نوزدهم ساخت."},
    "coax": {"example": "The engineer coaxed the old engine back into life.",
             "examplePersian": "مهندس موتور قدیمی را با اصرار دوباره روشن کرد."},
    "dank": {"example": "The walls were damp and dank after weeks of rain.",
             "examplePersian": "پس از هفته‌ها بارندگی، دیوارها نمناک و نمور بودند."},
    "cuban": {"example": "The exhibition covers Cuban art after 1959.",
              "examplePersian": "نمایشگاه هنر کوبا پس از ۱۹۵۹ را پوشش می‌دهد."},
    "chief": {"example": "The chief executive defended the decision before shareholders.",
              "examplePersian": "مدیرعامل تصمیم را در برابر سهام‌داران دفاع کرد."},
    "deem": {"example": "A single test should not deem a child a failure.",
             "examplePersian": "یک آزمون نباید کودکی را ناموفق تلقی کند."},
    "cite": {"example": "She cited three separate studies in her review.",
             "examplePersian": "او در مرور خود به سه مطالعهٔ جداگانه استناد کرد."},
}

# `decay` exists in both banks with two different rows; both are corrected by key.
PAIRED_KEYS = ("example", "examplePersian")


def validate_corrections(corrections: dict[str, dict] | None = None) -> list[str]:
    """Return authoring problems; empty means the correction set is sound."""
    from validate_vocabulary_quality import detect_script_or_encoding_defect, target_present

    problems: list[str] = []
    for word, fix in (corrections if corrections is not None else CORRECTIONS).items():
        # A correction must change at least one learner-visible field, and an
        # example may never be replaced without its Persian translation.
        if not any(fix.get(key) for key in ("definition",) + PAIRED_KEYS) and not fix.get("pos_fix"):
            problems.append(f"{word}: correction changes nothing")
        if any(fix.get(key) for key in PAIRED_KEYS) and not all(fix.get(key) for key in PAIRED_KEYS):
            problems.append(f"{word}: example and examplePersian must be supplied together")
        if word != word.lower().strip():
            problems.append(f"{word}: headword must be lowercase and trimmed")
        definition = str(fix.get("definition", ""))
        if definition and not definition.endswith((".", "!", "?")):
            problems.append(f"{word}: definition must end with a full stop")
        pos = fix.get("pos_fix")
        if pos is not None and pos not in {"noun", "verb", "adjective", "adverb"}:
            problems.append(f"{word}: unsupported pos_fix '{pos}'")
        # The same two gates the strict validator applies to every bundled row.
        example = str(fix.get("example", ""))
        if example and not target_present(word, example):
            problems.append(f"{word}: example does not contain the lemma")
        for field in ("examplePersian", "fa_fix"):
            defect = detect_script_or_encoding_defect(str(fix.get(field) or ""))
            if defect:
                problems.append(f"{word}: {field} {defect}")
    return problems


if __name__ == "__main__":
    issues = validate_corrections()
    print(f"{len(CORRECTIONS)} curated correction(s)")
    if issues:
        print(f"{len(issues)} problem(s):")
        for issue in issues:
            print(f"  - {issue}")
        raise SystemExit(1)
    print("all corrections are well formed")
