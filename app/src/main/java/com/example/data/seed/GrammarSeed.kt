package com.example.data.seed

import com.example.data.model.GrammarQuizQuestion
import com.example.data.model.GrammarTopic

object GrammarSeed {
    fun getTopics(): List<GrammarTopic> = listOf(
        GrammarTopic(
            id = "topic_present_simple",
            titleFa = "زمان حال ساده (Present Simple)",
            titleEn = "Present Simple Tense",
            level = "A1-A2",
            descriptionFa = "بیان حقایق علمی، روتین‌های روزمره، عادات پایدار و برنامه‌های زمان‌بندی‌شده قطعی.",
            rulesFa = "فرمول: فاعل + شکل ساده فعل (برای فاعل‌های سوم شخص مفرد he/she/it پسوند -s یا -es اضافه می‌شود).\nمنفی: do not (don't) / does not (doesn't) + شکل ساده فعل.\nسوالی: Do / Does + فاعل + فعل ساده؟",
            examplesEn = listOf(
                "Water boils at 100 degrees Celsius under standard atmospheric pressure.",
                "Dr. Collins analyzes quarterly market statistics every Monday morning.",
                "The academic semester commences on September 23rd."
            ),
            examplesFa = listOf(
                "آب در فشار استاندارد جو در ۱۰۰ درجه سانتی‌گراد می‌جوشد.",
                "دکتر کالینز هر دوشنبه صبح آمارهای فصلی بازار را تحلیل می‌کند.",
                "ترم دانشگاهی در تاریخ ۲۳ سپتامبر آغاز می‌شود."
            ),
            iranianCommonMistakesFa = "۱. فراموش کردن -s سوم شخص مفرد (مثلاً گفتن He work به جای He works).\n۲. گفتن اشتباه 'I am agree' به جای 'I agree' (agree خود یک فعل است، نه صفت!).",
            quizQuestions = listOf(
                GrammarQuizQuestion(
                    questionEn = "Renewable solar energy _______ widely across desert regions.",
                    options = listOf("generate", "generates", "is generate", "generating"),
                    correctIndex = 1,
                    explanationFa = "فاعل Renewable solar energy مفرد غیرقابل شمارش است، بنابراین فعل باید پسوند -s بگیرد: generates."
                ),
                GrammarQuizQuestion(
                    questionEn = "I _______ with the proposal to build a bypass road.",
                    options = listOf("am agree", "agree", "agreeing", "am agreed"),
                    correctIndex = 1,
                    explanationFa = "فعل agree نیازی به am ندارد؛ صحیح آن I agree است."
                )
            )
        ),
        GrammarTopic(
            id = "topic_present_continuous",
            titleFa = "زمان حال استمراری (Present Continuous)",
            titleEn = "Present Continuous Tense",
            level = "A2-B1",
            descriptionFa = "بیان کارهایی که هم‌اکنون در حال وقوع هستند یا روندهای در حال تحول پیرامون ما.",
            rulesFa = "فرمول: فاعل + am/is/are + verb-ing.\nافعال حسی و حالتی (stative verbs مانند know, believe, understand) معمولاً در این زمان به کار نمی‌روند.",
            examplesEn = listOf(
                "Global surface temperatures are rising at an alarming trajectory.",
                "The research laboratory is currently developing a synthetic antibody.",
                "More consumers are transitioning to zero-emission electric vehicles."
            ),
            examplesFa = listOf(
                "دمای سطح کره زمین با روندی هشداردهنده در حال افزایش است.",
                "آزمایشگاه پژوهشی هم‌اکنون در حال توسعه یک پادتن سنتزی است.",
                "مصرف‌کنندگان بیشتری در حال مهاجرت به سوی خودروهای برقی صفر انتشار هستند."
            ),
            iranianCommonMistakesFa = "استفاده از افعال حالتی با -ing؛ مثلاً گفتن 'I am knowing' یا 'I am understanding' که اشتباه است و باید گفت I know / I understand.",
            quizQuestions = listOf(
                GrammarQuizQuestion(
                    questionEn = "The percentage of urban dwellers _______ steadily throughout this decade.",
                    options = listOf("is growing", "are growing", "grows", "grow"),
                    correctIndex = 0,
                    explanationFa = "فاعل The percentage مفرد است، بنابراین از is growing برای بیان روند استفاده می‌شود."
                )
            )
        ),
        GrammarTopic(
            id = "topic_past_simple",
            titleFa = "زمان گذشته ساده (Past Simple)",
            titleEn = "Past Simple Tense",
            level = "A2",
            descriptionFa = "رویدادهایی که در گذشته در یک زمان مشخص آغاز شده و پایان یافته‌اند.",
            rulesFa = "افعال باقاعده: verb + ed.\nافعال بی‌قاعده: باید از حفظ شوند (go -> went, take -> took, buy -> bought).\nمنفی: did not (didn't) + فعل ساده.",
            examplesEn = listOf(
                "The government allocated substantial subsidies to education in 2021.",
                "The investigative committee published its final findings yesterday.",
                "Ancient civilizations utilized irrigation to sustain agricultural yields."
            ),
            examplesFa = listOf(
                "دولت در سال ۲۰۲۱ یارانه‌های چشمگیری به آموزش اختصاص داد.",
                "کمیته تحقیق روز گذشته یافته‌های نهایی خود را منتشر کرد.",
                "تمدن‌های باستان برای تداوم بازدهی کشاورزی از آبیاری بهره می‌بردند."
            ),
            iranianCommonMistakesFa = "آوردن فعل در شکل گذشته پس از did؛ مثلاً گفتن 'Did you went?' که غلط است و باید گفت 'Did you go?'.",
            quizQuestions = listOf(
                GrammarQuizQuestion(
                    questionEn = "When _______ the archaeological team discover the inscription?",
                    options = listOf("did", "was", "does", "have"),
                    correctIndex = 0,
                    explanationFa = "برای سوالی کردن گذشته ساده با فعل اصلی discover از فعل کمکی did استفاده می‌شود."
                )
            )
        ),
        GrammarTopic(
            id = "topic_present_perfect",
            titleFa = "زمان حال کامل (Present Perfect)",
            titleEn = "Present Perfect Tense",
            level = "B1-B2",
            descriptionFa = "پیوند گذشته با اکنون: تجربیات زندگی، کارهایی در گذشته با اثر ملموس بر اکنون، کارهایی که از گذشته تا کنون ادامه دارند.",
            rulesFa = "فرمول: have / has + past participle (قسمت سوم فعل).\nکلمات کلیدی: already, yet, just, since, for, ever, never, so far.",
            examplesEn = listOf(
                "Scientists have observed significant changes in arctic ice sheets.",
                "The company has expanded its operations into five European markets since 2018.",
                "Have you ever presented research findings at an international symposium?"
            ),
            examplesFa = listOf(
                "دانشمندان دگرگونی‌های معناداری را در صفحات یخچالی قطب شمال مشاهده کرده‌اند.",
                "این شرکت از سال ۲۰۱۸ عملیات خود را به پنج بازار اروپایی گسترش داده است.",
                "آیا تاکنون یافته‌های پژوهشی را در یک سمپوزیوم بین‌المللی ارائه داده‌اید؟"
            ),
            iranianCommonMistakesFa = "استفاده از زمان گذشته ساده به جای حال کامل همراه با since یا for؛ مثلاً گفتن 'I live here since 2 years' که غلط است و باید گفت: 'I have lived here for 2 years'. همچنین تفاوت since (نقطه آغاز زمان) و for (طول مدت زمان).",
            quizQuestions = listOf(
                GrammarQuizQuestion(
                    questionEn = "The municipal council _______ three public parks since January.",
                    options = listOf("has renovated", "renovated", "renovates", "is renovating"),
                    correctIndex = 0,
                    explanationFa = "با وجود قید زمان since January، باید از زمان حال کامل has renovated استفاده کرد."
                )
            )
        ),
        GrammarTopic(
            id = "topic_conditionals",
            titleFa = "جملات شرطی (Conditionals: 0, 1, 2, 3 & Mixed)",
            titleEn = "Conditionals",
            level = "B2-C1",
            descriptionFa = "بیان احتمالات، فرضیات، حقایق علمی و حسرت‌های گذشته در رایتینگ و مکالمه.",
            rulesFa = "شرطی نوع صفر: If + present simple, present simple (حقایق علمی).\nشرطی نوع اول: If + present, will + verb (آینده محتمل).\nشرطی نوع دوم: If + past simple, would + verb (فرضیه حال و ناممکن).\nشرطی نوع سوم: If + past perfect, would have + p.p (حسرت یا فرضیه گذشته).",
            examplesEn = listOf(
                "If temperatures rise by 2 degrees, coastal wetlands will face inundation.",
                "If governments invested more in clean transit, urban emissions would decline rapidly.",
                "Had the authorities intervened earlier, the economic crisis could have been mitigated."
            ),
            examplesFa = listOf(
                "اگر دما ۲ درجه افزایش یابد، تالاب‌های ساحلی با خطر آب‌گرفتگی روبرو خواهند شد.",
                "اگر دولت‌ها بیشتر در حمل‌ونقل پاک سرمایه‌گذاری می‌کردند، انتشار آلاینده‌های شهری به سرعت کاهش می‌یافت.",
                "اگر مقامات زودتر مداخله کرده بودند، بحران اقتصادی می‌توانست تعدیل و مهار گردد."
            ),
            iranianCommonMistakesFa = "استفاده از will یا would در بخش if-clause (مثلاً گفتن 'If it will rain' که غلط است و باید گفت 'If it rains'). همچنین وارونگی ساختار شرطی سوم بدون if: Had they known... به جای If they had known...",
            quizQuestions = listOf(
                GrammarQuizQuestion(
                    questionEn = "If the regulatory agency _______ earlier, the disaster could have been prevented.",
                    options = listOf("had acted", "acted", "would act", "acts"),
                    correctIndex = 0,
                    explanationFa = "در شرطی نوع سوم، بخش شرط با If + past perfect (had acted) می‌آید تا نتیجه would have been prevented تکمیل شود."
                )
            )
        ),
        GrammarTopic(
            id = "topic_passive_voice",
            titleFa = "حالت مجهول در نگارش آکادمیک (Passive Voice)",
            titleEn = "Passive Voice in Academic Writing",
            level = "B2-C1",
            descriptionFa = "تمرکز بر عمل یا نتیجه به جای فاعل؛ لحن بی‌طرف، عینی و آکادمیک در گزارش‌های علمی و رایتینگ آیلتس.",
            rulesFa = "فرمول: فاعل + شکل مناسب فعل be + past participle (قسمت سوم فعل).\nمثال در حال کامل: has/have been + p.p.\nمجهول غیرشخصی: It is widely believed that... / The policy is considered to be...",
            examplesEn = listOf(
                "Over three thousand blood samples were analyzed during the initial trial.",
                "Substantial funding has been allocated to renewable energy initiatives.",
                "It is widely acknowledged that early bilingualism enhances cognitive flexibility."
            ),
            examplesFa = listOf(
                "بیش از سه هزار نمونه خون در طول مرحله کارآزمایی اولیه مورد تحلیل قرار گرفت.",
                "بودجه چشمگیری به ابتکارات انرژی‌های تجدیدپذیر اختصاص داده شده است.",
                "به طور گسترده اذعان می‌شود که دوزبانگی زودهنگام انعطاف‌پذیری شناختی را تقویت می‌کند."
            ),
            iranianCommonMistakesFa = "فراموش کردن شکل فعل be در ساختار مجهول (مثلاً گفتن 'The samples analyzed' به جای 'The samples were analyzed').",
            quizQuestions = listOf(
                GrammarQuizQuestion(
                    questionEn = "The historic manuscript _______ by conservation experts last winter.",
                    options = listOf("was restored", "restored", "is restore", "has restored"),
                    correctIndex = 0,
                    explanationFa = "جمله مجهول در زمان گذشته با فاعل مفرد manuscript نیازمند was restored است."
                )
            )
        ),
        GrammarTopic(
            id = "topic_relative_clauses",
            titleFa = "جملات موصولی (Relative Clauses: Defining & Non-defining)",
            titleEn = "Relative Clauses",
            level = "B1-B2",
            descriptionFa = "ترکیب جملات و ساخت ساختارهای دستوری پیچیده (Complex Sentences) برای کسب نمره گرامر بالای ۷ در آیلتس.",
            rulesFa = "Defining: اطلاعات ضروری برای شناسایی اسم (بدون کاما، می‌توان از that استفاده کرد).\nNon-defining: اطلاعات اضافی (حتماً بین دو کاما، هرگز از that استفاده نمی‌شود و فقط who/which).",
            examplesEn = listOf(
                "Candidates who score above Band 7 demonstrate proficient lexical flexibility.",
                "Wind energy, which is entirely renewable, provides a sustainable alternative to coal.",
                "The research center that published the report is situated in Geneva."
            ),
            examplesFa = listOf(
                "داوطلبانی که نمره بالاتر از ۷ می‌گیرند انعطاف‌پذیری واژگانی کارآمدی از خود نشان می‌دهند.",
                "انرژی بادی، که کاملاً تجدیدپذیر است، جایگزینی پایدار برای زغال‌سنگ فراهم می‌آورد.",
                "مرکز پژوهشی که این گزارش را منتشر ساخت در ژنو واقع شده است."
            ),
            iranianCommonMistakesFa = "تکرار ضمیر مفعولی در انتهای جمله موصولی (مثلاً گفتن 'The book that I read it' که غلط است؛ it باید حذف شود: 'The book that I read').",
            quizQuestions = listOf(
                GrammarQuizQuestion(
                    questionEn = "Dr. Alvarez, _______ founded the research lab, delivered the keynote address.",
                    options = listOf("who", "which", "that", "whom"),
                    correctIndex = 0,
                    explanationFa = "در جمله موصولی غیرمعرف (دارای کاما) که به انسان اشاره دارد، فقط از who استفاده می‌شود، نه that."
                )
            )
        ),
        GrammarTopic(
            id = "topic_inversion_emphasis",
            titleFa = "وارونگی و تاکید (Inversion for Emphasis in IELTS)",
            titleEn = "Inversion for Emphasis",
            level = "C1-C2",
            descriptionFa = "قرار دادن قیدهای منفی یا محدودکننده در ابتدای جمله و معکوس کردن جای فعل کمکی و فاعل جهت ایجاد تاثیر بلاغی قوی.",
            rulesFa = "قیدهای منفی: Seldom, Rarely, Never, Scarcely, Not only... but also, Under no circumstances.\nفرمول: قید منفی + فعل کمکی + فاعل + فعل اصلی.",
            examplesEn = listOf(
                "Not only did the project reduce carbon emissions, but it also stimulated local employment.",
                "Rarely have international economists witnessed such rapid technological adoption.",
                "Under no circumstances should safety verification procedures be compromised."
            ),
            examplesFa = listOf(
                "این پروژه نه تنها انتشار کربن را کاهش داد، بلکه اشتغال محلی را نیز تحریک و شکوفا ساخت.",
                "اقتصاددانان بین‌المللی به ندرت شاهد چنین پذیرش شتابانی در عرصه فناوری بوده‌اند.",
                "تحت هیچ شرایطی نباید رویه‌های راستی‌آزمایی ایمنی نادیده گرفته شوند."
            ),
            iranianCommonMistakesFa = "فراموش کردن معکوس کردن ترتیب فعل و فاعل پس از قید منفی؛ مثلاً گفتن 'Not only the project reduced' به جای 'Not only did the project reduce'.",
            quizQuestions = listOf(
                GrammarQuizQuestion(
                    questionEn = "Seldom _______ such decisive consensus among planetary scientists.",
                    options = listOf("has there been", "there has been", "is there being", "there was"),
                    correctIndex = 0,
                    explanationFa = "وقتی جمله با قید منفی Seldom شروع می‌شود، وارونگی انجام می‌شود: فعل کمکی has قبل از there قرار می‌گیرد."
                )
            )
        ),
        GrammarTopic(
            id = "topic_articles",
            titleFa = "حروف تعریف (Articles: A, An, The, Zero Article)",
            titleEn = "Definite and Indefinite Articles",
            level = "A2-B2",
            descriptionFa = "یکی از بزرگترین چالش‌های زبان‌آموزان ایرانی به دلیل تفاوت در کاربرد معرفه و نکره در فارسی و انگلیسی.",
            rulesFa = "a/an: برای اسم‌های قابل شمارش مفرد ناشناس.\nthe: برای اسامی شناخته‌شده، یگانه در جهان (the sun, the earth)، اقیانوس‌ها، و اسم‌های ذکرشده پیشین.\nZero Article (بدون حرف تعریف): اسامی جمع کلی و اسامی غیرقابل شمارش عمومی (مثلاً: Water is essential, not The water).",
            examplesEn = listOf(
                "Education plays a pivotal role in eradicating societal poverty.",
                "The research published yesterday challenges conventional theories.",
                "Renewable energy reduces pollution worldwide."
            ),
            examplesFa = listOf(
                "آموزش نقشی محوری در ریشه‌کن کردن فقر اجتماعی ایفا می‌کند (آموزش به صورت کلی بدون the).",
                "پژوهشی که دیروز منتشر شد نظریه‌های سنتی را به چالش می‌کشد (معرفه با the).",
                "انرژی‌های تجدیدپذیر آلودگی را در سراسر جهان کاهش می‌دهند."
            ),
            iranianCommonMistakesFa = "استفاده از the برای مفاهیم عام و انتزاعی؛ مثلاً گفتن 'The education is important' که اشتباه است و باید گفت 'Education is important'.",
            quizQuestions = listOf(
                GrammarQuizQuestion(
                    questionEn = "_______ economic inequality remains one of the greatest global challenges.",
                    options = listOf("No article", "The", "An", "A"),
                    correctIndex = 0,
                    explanationFa = "برای مفاهیم کلی و انتزاعی مثل economic inequality نیازی به حرف تعریف نیست (Zero Article)."
                )
            )
        ),
        GrammarTopic(
            id = "topic_academic_hedging",
            titleFa = "لحن محتاطانه در رایتینگ آکادمیک (Hedging & Modality)",
            titleEn = "Hedging and Academic Modality",
            level = "B2-C1",
            descriptionFa = "پرهیز از احکام مطلق (Overgeneralization) و استفاده از زبان محتاطانه علمی، که از معیارهای اصلی ارزیابی در آیلتس و تافل است.",
            rulesFa = "افعال معین: may, might, could, would.\nافعال احتیاطی: tend to, appear to, seem to, suggest that.\nقیدها: arguably, potentially, predominantly, substantially.",
            examplesEn = listOf(
                "The findings suggest that excessive social media usage may contribute to heightened anxiety.",
                "Urbanization tends to accelerate infrastructural challenges in developing nations.",
                "These policies are arguably among the most progressive environmental measures in the region."
            ),
            examplesFa = listOf(
                "یافته‌ها حاکی از آن است که استفاده مفرط از شبکه‌های اجتماعی ممکن است در تشدید اضطراب نقش داشته باشد.",
                "شهرنشینی تمایل دارد چالش‌های زیرساختی را در کشورهای در حال توسعه شتاب بخشد.",
                "این سیاست‌ها به احتمال قوی در زمره مترقی‌ترین تدابیر زیست‌محیطی در این منطقه به شمار می‌آیند."
            ),
            iranianCommonMistakesFa = "نوشتن احکام قطعی با will و always؛ مثلاً 'Cell phones always destroy concentration' که در آیلتس باعث کسر نمره است. صورت درست: 'Cell phones may impair concentration'.",
            quizQuestions = listOf(
                GrammarQuizQuestion(
                    questionEn = "Preliminary statistics _______ that hybrid work improves employee retention.",
                    options = listOf("indicate", "will prove definitely", "always guarantees", "must absolute prove"),
                    correctIndex = 0,
                    explanationFa = "فعل indicate بیانی علمی، معقول و بدون ادعای مطلق است و مناسب رایتینگ آکادمیک می‌باشد."
                )
            )
        )
    )
}
