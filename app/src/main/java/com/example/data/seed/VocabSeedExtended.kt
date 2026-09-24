package com.example.data.seed

import com.example.data.model.VocabularyItem

object VocabSeedExtended {
    // Generates 250+ categorized high-yield English-Persian vocabulary items
    fun getItems(): List<VocabularyItem> {
        val rawData = listOf(
            // Format: Word | IPA | PartOfSpeech | CEFR | PersianMeaning | EnglishDefinition | ExampleEn | ExampleFa | Collocation | Synonyms | Pack
            "approach|/əˈprəʊtʃ/|noun|B1|رویکرد، شیوه، نزدیک شدن به موضوع|a way of considering or doing something|We need a novel approach to solve traffic congestion.|ما برای حل معضل ترافیک به رویکردی نوین نیاز داریم.|adopt an approach|method, strategy|pack_oxford_essential",
            "assess|/əˈses/|verb|B2|ارزیابی کردن، سنجیدن|to judge or decide the amount, value, quality, or importance of something|The committee will assess the environmental impact.|کمیته اثرات زیست‌محیطی را ارزیابی خواهد کرد.|assess the situation|evaluate, gauge|pack_ielts_academic",
            "assume|/əˈsjuːm/|verb|B2|فرض کردن، پنداشتن، به عهده گرفتن مسئولیت|to accept something to be true without question or proof|Many assume that technology always saves time.|بسیاری چنین می‌پندارند که فناوری همواره در وقت صرفه‌جویی می‌کند.|safely assume|presume, suppose|pack_oxford_essential",
            "authority|/ɔːˈθɒrəti/|noun|B2|مرجع صلاحیت‌دار، اقتدار، تسلط بر موضوع|the moral or legal right or power to give orders and make decisions|Local authorities imposed restrictions during the heatwave.|مقامات محلی در طول موج گرما محدودیت‌هایی وضع کردند.|exercise authority|power, command|pack_ielts_academic",
            "available|/əˈveɪləbl/|verb|A2|در دسترس، موجود، قابل تهیه|able to be bought or used|Renewable alternatives are now widely available.|جایگزین‌های تجدیدپذیر اکنون به طور گسترده در دسترس هستند.|readily available|accessible, obtainable|pack_oxford_essential",
            "benefit|/ˈbenɪfɪt/|noun|A2|منفعت، سود، فایده|a helpful or good effect, or something intended to help|Regular exercise provides immense cardiovascular benefits.|ورزش منظم فواید قلبی عروقی بی‌شماری به همراه دارد.|reap benefits|advantage, perk|pack_oxford_essential",
            "concept|/ˈkɒnsept/|noun|B2|مفهوم، ایده کلی، برداشت نظری|a principle or idea|The concept of sustainable development is now global.|مفهوم توسعه پایدار اکنون مفهومی جهانی است.|grasp a concept|notion, idea|pack_ielts_academic",
            "consist|/kənˈsɪst/|verb|B1|شامل بودن، متشکل بودن از|to be made of or formed from something|The committee consists of independent international experts.|کمیته متشکل از کارشناسان مستقل بین‌المللی است.|consist of|comprise, contain|pack_oxford_essential",
            "context|/ˈkɒntekst/|noun|B2|زمینه، بستر متن یا رویداد|the situation within which something exists or happens|Words must be studied within their authentic context.|کلمات باید در بستر اصیل کاربردشان مطالعه شوند.|in this context|framework, background|pack_ielts_academic",
            "contract|/ˈkɒntrækt/|noun|B1|قرارداد، پیمان، منقبض شدن|a legal agreement between two people or organizations|Both parties signed a binding three-year contract.|هر دو طرف قراردادی الزام‌آور و سه ساله امضا کردند.|breach a contract|agreement, covenant|pack_oxford_essential",
            "data|/ˈdeɪtə/|noun|A2|داده‌ها، اطلاعات آماری|information, especially facts or numbers collected to be examined|Empirical data confirms that the ozone layer is recovering.|داده‌های تجربی تایید می‌کنند که لایه اوزون در حال ترمیم است.|analyze data|statistics, evidence|pack_ielts_academic",
            "economy|/ɪˈkɒnəmi/|noun|B1|اقتصاد، صرفه‌جویی|the system of trade and industry by which the wealth of a country is made and used|The national economy is shifting toward knowledge-based sectors.|اقتصاد ملی در حال گرایش به سوی بخش‌های دانش‌بنیان است.|emerging economy|market, finances|pack_oxford_essential",
            "environment|/ɪnˈvaɪrənmənt/|noun|B1|محیط زیست، پیرامون، محیط کاری|the air, water, and land in or on which people, animals, and plants live|We must protect the natural environment from toxic waste.|ما باید از محیط زیست طبیعی در برابر پساب‌های سمی محافظت کنیم.|protect the environment|habitat, surroundings|pack_ielts_academic",
            "establish|/ɪˈstæblɪʃ/|verb|B2|بنا نهادن، تاسیس کردن، اثبات قطعی کردن|to start a company or organization that will continue for a long time|Researchers established a clear link between diet and health.|پژوهشگران ارتباطی شفاف میان رژیم غذایی و سلامت اثبات کردند.|establish a link|found, institute|pack_oxford_essential",
            "estimate|/ˈestɪmeɪt/|verb|B2|تخمین زدن، برآورد کردن مقادیر|to guess or calculate the cost, size, or value of something|Economists estimate that inflation will decline next year.|اقتصاددانان برآورد می‌کنند که تورم سال آینده کاهش خواهد یافت.|rough estimate|approximate, calculate|pack_ielts_academic",
            "evident|/ˈevɪdənt/|adjective|B2|بدیهی، آشکار، مبرهن|easily seen or understood|It became evident that previous policies were ineffective.|بدیهی شد که سیاست‌های قبلی ناکارآمد بوده‌اند.|self-evident|obvious, apparent|pack_oxford_essential",
            "factor|/ˈfæktə/|noun|B1|عامل، فاکتور، پارامتر اثرگذار|a fact or situation that influences the result of something|Dietary habits are a crucial factor in life expectancy.|عادت‌های غذایی عاملی تعیین‌کننده در امید به زندگی هستند.|contributing factor|element, determinant|pack_ielts_academic",
            "finance|/ˈfaɪnæns/|noun|B2|مالیه، تامین بودجه، سرمایه‌گذاری|the management of a large amount of money, especially by governments or large companies|He works in corporate finance and investment analysis.|او در حوزه امور مالی شرکت‌ها و تحلیل سرمایه‌گذاری کار می‌کند.|public finance|funds, capital|pack_oxford_essential",
            "function|/ˈfʌŋkʃn/|noun|B1|کارکرد، عملکرد، نقش کاری|the natural purpose of something or the duty of a person|The primary function of the lungs is gas exchange.|کارکرد اصلی ریه‌ها تبادل گازها است.|fulfill a function|role, purpose|pack_oxford_essential",
            "identify|/aɪˈdentɪfaɪ/|verb|B1|شناسایی کردن، تشخیص دادن|to recognize a problem, need, or fact and understand it|Engineers identified the mechanical fault promptly.|مهندسان نقص مکانیکی را بی‌درنگ شناسایی کردند.|identify a problem|recognize, pinpoint|pack_oxford_essential",
            "income|/ˈɪnkʌm/|noun|B1|درآمد، عایدی مالی|money that is earned from doing work or received from investments|Higher education typically correlates with higher lifetime income.|تحصیلات عالی معمولاً با درآمد مادام‌العمر بالاتر همبستگی دارد.|disposable income|earnings, salary|pack_oxford_essential",
            "indicate|/ˈɪndɪkeɪt/|verb|B2|نشان دادن، حاکی بودن از|to show, point, or make clear in another way|Survey findings indicate that customer satisfaction is growing.|یافته‌های نظرسنجی حاکی از آن است که رضایت مشتریان رو به رشد است.|indicate a trend|show, signify|pack_ielts_writing",
            "individual|/ˌɪndɪˈvɪdʒuəl/|noun|B1|فرد، شخص، انفرادی|a single person or thing, especially when compared to the whole group|Each individual is responsible for their ecological footprint.|هر فرد مسئول ردپای بوم‌شناختی خویش است.|respect individuals|person, entity|pack_oxford_essential",
            "interpret|/ɪnˈtɜːprɪt/|verb|B2|تفسیر کردن، تعبیر کردن داده‌ها یا هنر|to describe the meaning of something, or express one's own understanding|Scholars interpret the ancient text in varying manners.|دانشمندان این متن کهن را به شیوه‌های متفاوتی تفسیر می‌کنند.|interpret data|explain, decode|pack_toefl_academic",
            "involve|/ɪnˈvɒlv/|verb|B1|شامل شدن، درگیر کردن، مستلزم بودن|if an activity or situation involves something, that thing is a necessary part of it|The initiative involves collaboration between schools and parents.|این ابتکار مستلزم همکاری میان مدارس و والدین است.|involve risk|entail, encompass|pack_oxford_essential",
            "issue|/ˈɪʃuː/|noun|B1|مسئله، موضوع بحث، انتشار|a subject or problem that people are thinking and talking about|Air quality is a pressing public health issue.|کیفیت هوا یک مسئله مبرم و فوری در بهداشت عمومی است.|address an issue|matter, topic|pack_oxford_essential",
            "labor|/ˈleɪbə/|noun|B2|نیروی کار، زحمت، کارگری|practical work, especially when it involves hard physical effort|Automation has drastically reduced manual labor on farms.|اتوماسیون به شدت نیروی کار یدی را در مزارع کاهش داده است.|skilled labor|workforce, toil|pack_ielts_academic",
            "legal|/ˈliːɡl/|adjective|B1|قانونی، حقوقی|connected with the law or allowed by the law|Every citizen has a right to competent legal counsel.|هر شهروندی حق برخورداری از وکیل حقوقی کاردان را دارد.|legal framework|lawful, legitimate|pack_oxford_essential",
            "major|/ˈmeɪdʒə/|adjective|A2|عمده، اصلی، بزرگ|more important, bigger, or more serious than others of the same type|Water scarcity is a major threat to arid agricultural regions.|کمبود آب تهدیدی عمده برای مناطق کشاورزی خشک است.|major challenge|primary, significant|pack_oxford_essential",
            "method|/ˈmeθəd/|noun|A2|روش، شیوه، متدولوژی|a particular way of doing something|Quantitative research methods were employed throughout the study.|روش‌های پژوهش کمی در سراسر این مطالعه به کار گرفته شدند.|scientific method|technique, procedure|pack_oxford_essential",
            "occur|/əˈkɜː/|verb|B1|رخ دادن، اتفاق افتادن|to happen, especially of accidents and other unexpected events|Earthquakes occur frequently along tectonic fault lines.|زمین‌لرزه‌ها مکرراً در امتداد گسل‌های تکتونیکی رخ می‌دهند.|rarely occur|happen, transpire|pack_oxford_essential",
            "percent|/pəˈsent/|noun|A1|درصد|one part in every hundred|Over eighty percent of households now have high-speed internet.|بیش از هشتاد درصد از خانواده‌ها اکنون اینترنت پرسرعت دارند.|ninety percent|percentage, share|pack_oxford_essential",
            "period|/ˈpɪəriəd/|noun|A2|دوره زمانی، عصر|a length of time|The region experienced a sustained period of economic prosperity.|این منطقه یک دوره پایدار از شکوفایی اقتصادی را تجربه کرد.|prolonged period|era, interval|pack_oxford_essential",
            "policy|/ˈpɒləsi/|noun|B1|سیاست، خط‌مشی سازمانی یا دولتی|a set of ideas or a plan of what to do in particular situations|The university adopted a strict anti-plagiarism policy.|دانشگاه سیاست سخت‌گیرانه‌ای در برابر سرقت ادبی اتخاذ کرد.|implement a policy|strategy, guideline|pack_ielts_academic",
            "principle|/ˈprɪnsəpl/|noun|B2|اصل، قاعده اخلاقی یا علمی|a basic idea or rule that explains or controls how something happens or works|Democratic governance is founded on the principle of equality.|حکمرانی دموکراتیک بر اصل برابری استوار است.|moral principle|tenet, doctrine|pack_oxford_essential",
            "proceed|/prəˈsiːd/|verb|B2|پیش رفتن، ادامه دادن کار|to continue as planned or go forward|Despite the heavy downpour, the ceremony proceeded as scheduled.|علی‌رغم باران سیل‌آسا، مراسم طبق برنامه پیش رفت.|proceed with caution|continue, advance|pack_oxford_essential",
            "process|/ˈprəʊses/|noun|B1|فرایند، روند، مراحل انجام کار|a series of actions that you take in order to achieve a result|Language learning is a gradual process requiring patience.|یادگیری زبان روندی تدریجی است که نیازمند صبوری است.|lengthy process|procedure, operation|pack_oxford_essential",
            "require|/rɪˈkwaɪə/|verb|B1|نیاز داشتن، ملزم ساختن|to need something or make something necessary|The visa application requires official stamped transcripts.|درخواست ویزا مستلزم ریزنمرات ممهور رسمی است.|urgently require|demand, necessitate|pack_oxford_essential",
            "research|/rɪˈsɜːtʃ/|noun|B1|پژوهش، تحقیق علمی|a detailed study of a subject, especially in order to discover new information|Extensive research has linked sleep deprivation with mood disorders.|تحقیقات گسترده بی‌خوابی را با اختلالات خلقی مرتبط ساخته است.|conduct research|investigation, study|pack_ielts_academic",
            "respond|/rɪˈspɒnd/|verb|B1|پاسخ دادن، واکنش نشان دادن|to say or do something as a reaction to something that has been said or done|First responders responded swiftly to the emergency alert.|نیروهای امدادی به سرعت به هشدار اضطراری پاسخ دادند.|respond promptly|reply, react|pack_oxford_essential",
            "role|/rəʊl/|noun|B1|نقش، وظیفه، جایگاه|the position or purpose that someone or something has in a situation|Teachers play a transformative role in nurturing young minds.|معلمان نقشی تحول‌آفرین در پرورش ذهن جوانان ایفا می‌کنند.|play a vital role|function, part|pack_ielts_writing",
            "section|/ˈsekʃn/|noun|A2|بخش، قسمت، قطعه|one of the parts that something is divided into|The listening section of the test takes thirty minutes.|بخش شنیداری آزمون سی دقیقه طول می‌کشد.|introductory section|part, segment|pack_oxford_essential",
            "sector|/ˈsektə/|noun|B2|بخش اقتصادی یا اجتماعی|one of the areas into which the economic activity of a country is divided|The private sector is investing heavily in clean tech.|بخش خصوصی سرمایه‌گذاری سنگینی در فناوری‌های پاک انجام می‌دهد.|public sector|industry, domain|pack_ielts_academic",
            "significant|/sɪɡˈnɪfɪkənt/|adjective|B2|معنادار، چشمگیر، مهم|important or noticeable|There was a significant disparity between the two outcomes.|اختلاف معناداری میان این دو نتیجه وجود داشت.|significant difference|notable, major|pack_ielts_writing",
            "source|/sɔːs/|noun|B1|منبع، سرچشمه، خاستگاه|the place something comes from or starts at|Solar power is an inexhaustible energy source.|انرژی خورشیدی یک منبع انرژی تمام‌نشدنی است.|reliable source|origin, root|pack_oxford_essential",
            "specific|/spəˈsɪfɪk/|adjective|B1|مشخص، معین، خاص|relating to one particular thing and not to others|Candidates must give specific examples to support their claims.|داوطلبان باید مثال‌های مشخصی برای پشتیبانی از ادعاهای خود بیاورند.|specific details|particular, precise|pack_ielts_writing",
            "structure|/ˈstrʌktʃə/|noun|B1|ساختار، اسکلت، بنا|the way that parts of something are arranged or put together|The syntactic structure of complex sentences must be mastered.|ساختار نحوی جملات پیچیده باید به تسلط درآید.|organizational structure|framework, setup|pack_ielts_academic",
            "theory|/ˈθɪəri/|noun|B1|نظریه، تئوری علمی|a formal idea or set of ideas that is intended to explain something|Einstein formulated the general theory of relativity.|اینشتین نظریه نسبیت عام را صورت‌بندی کرد.|in theory|hypothesis, principle|pack_toefl_academic",
            "vary|/ˈveəri/|verb|B2|تنوع داشتن، تفاوت داشتن، متغیر بودن|to be different or to become different|Admission requirements vary significantly across universities.|شرایط پذیرش در میان دانشگاه‌ها تفاوت‌های چشمگیری دارد.|vary widely|differ, fluctuate|pack_ielts_academic",
            "ambiguous|/æmˈbɪɡjuəs/|adjective|C1|مبهم، چندپهلو، دارای تعابیر گوناگون|having or expressing more than one possible meaning|The contract language was dangerously ambiguous.|عبارت‌بندی قرارداد به شکلی خطرناک مبهم بود.|ambiguous wording|unclear, equivocal|pack_c1_advanced",
            "arbitrate|/ˈɑːbɪtreɪt/|verb|C2|داوری کردن، میانجی‌گری رسمی میان طرفین اختلاف|to judge a disagreement between two people or groups|An independent mediator was appointed to arbitrate the labor dispute.|یک میانجی‌گر مستقل برای داوری در مناقشه کارگری منصوب شد.|arbitrate a dispute|mediate, judge|pack_c1_advanced",
            "catalyst|/ˈkætəlɪst/|noun|C1|کاتالیزور، شتاب‌بخش تحول، عامل محرک|something that makes a chemical reaction happen faster, or an event that causes change|The crisis served as a catalyst for educational reform.|این بحران به عنوان کاتالیزوری برای اصلاحات آموزشی عمل کرد.|act as a catalyst|stimulus, trigger|pack_c1_advanced",
            "deduce|/dɪˈdjuːs/|verb|B2|استنتاج کردن، نتیجه‌گیری منطقی از شواهد|to reach an answer or a decision by thinking carefully about known facts|Detectives deduced that the suspect had fled the country.|کارآگاهان استنتاج کردند که مظنون از کشور گریخته است.|deduce from evidence|infer, conclude|pack_toefl_academic",
            "elicit|/ɪˈlɪsɪt/|verb|C1|برانگیختن، بیرون کشیدن واکنش یا پاسخ|to get or produce something, especially information or a reaction|The questionnaire was structured to elicit genuine user opinions.|پرسشنامه به شکلی تدوین شد تا نظرات صادقانه کاربران را بیرون بکشد.|elicit a response|evoke, draw out|pack_c1_advanced",
            "gauge|/ɡeɪdʒ/|verb|B2|سنجیدن، برآورد کردن مقادیر، فشارسنج|to calculate an amount, especially by using a measuring device; to judge feelings|Surveys help gauge customer sentiment before launching products.|نظرسنجی‌ها به سنجش احساسات مشتریان پیش از عرضه محصولات کمک می‌کنند.|gauge public opinion|measure, assess|pack_toefl_academic",
            "hierarchy|/ˈhaɪərɑːki/|noun|B2|سلسله‌مراتب، رده‌بندی طبقاتی|a system in which people or things are arranged according to their importance|Many startups favor flat structures over rigid hierarchies.|بسیاری از استارتاپ‌ها ساختارهای مسطح را به سلسله‌مراتب‌های صلب ترجیح می‌دهند.|rigid hierarchy|ranking, order|pack_c1_advanced",
            "intuitive|/ɪnˈtjuːɪtɪv/|adjective|B2|شهودی، فطری، کاربرپسند و آسان‌یاب|based on feelings rather than facts or proof; easy to understand without explanation|The mobile app features a remarkably intuitive user interface.|این اپلیکیشن موبایل از رابط کاربری فوق‌العاده شهودی و روانی برخوردار است.|intuitive interface|instinctive, user-friendly|pack_oxford_essential",
            "marginal|/ˈmɑːdʒɪnl/|adjective|C1|حاشیه‌ای، اندک، ناچیز|very small in amount or effect; situated on a border|The new medication demonstrated only marginal improvement.|داروی جدید تنها بهبودی ناچیز و حاشیه‌ای نشان داد.|marginal gain|minor, negligible|pack_c1_advanced",
            "novice|/ˈnɒvɪs/|noun|B2|تازه‌کار، مبتدی، بیابانی در یک تخصص|a person who is not experienced in a job or situation|Even a novice can create responsive layouts with modern tools.|حتی یک فرد تازه‌کار نیز می‌تواند با ابزارهای مدرن چیدمان‌های واکنش‌گرا بسازد.|complete novice|beginner, trainee|pack_oxford_essential",
            "override|/ˌəʊvəˈraɪd/|verb|C1|لغو کردن، باطل کردن تصمیم زیردست با اختیارات بالاتر|to decide against or refuse to accept a previous decision|The director can override recommendations made by the committee.|مدیر می‌تواند توصیه‌های مطرح‌شده توسط کمیته را وتو و لغو کند.|override a decision|overrule, supersede|pack_c1_advanced",
            "redundant|/rɪˈdʌndənt/|adjective|B2|اضافی، زائد، تعدیل‌شده از کار|unnecessary because there is more than is needed; without work|Automated workflows made manual data entry entirely redundant.|جریان‌های کاری خودکار، ورود دستی اطلاعات را کاملاً زائد و بی‌مصرف ساخت.|render redundant|superfluous, unneeded|pack_c1_advanced",
            "simulate|/ˈsɪmjuleɪt/|verb|B2|شبیه‌سازی کردن، بازآفرینی رایانه‌ای|to do or make something that looks real but is not real|Flight simulators allow pilots to train in emergency scenarios safely.|شبیه‌سازهای پرواز به خلبانان امکان می‌دهند در سناریوهای اضطراری به شکلی ایمن آموزش ببینند.|simulate conditions|replicate, emulate|pack_toefl_academic",
            "subsequent|/ˈsʌbsɪkwənt/|adjective|B2|متعاقب، بعدی، رخ‌داده در پی چیزی|happening after something else|The initial trial failed, but subsequent experiments proved successful.|آزمایش اولیه شکست خورد، اما آزمایش‌های بعدی و متعاقب موفقیت‌آمیز بودند.|subsequent years|following, succeeding|pack_ielts_writing",
            "denote|/dɪˈnəʊt/|verb|C1|دلالت داشتن بر، معنی لفظی داشتن|to represent or mean something|A red flag on the beach denotes hazardous swimming conditions.|پرچم قرمز در ساحل دلالت بر شرایط مخاطره‌آمیز شنا دارد.|clearly denote|indicate, represent|pack_toefl_academic",
            "distort|/dɪˈstɔːt/|verb|C1|تحریف کردن، دگرگون ساختن واقعیت|to change the shape, meaning, or sound of something so it is false or unnatural|Propaganda seeks to distort historical facts for political ends.|تبلیغات مغرضانه در صدد تحریف حقایق تاریخی برای اهداف سیاسی است.|distort the truth|warp, falsify|pack_c1_advanced",
            "formidable|/fɔːˈmɪdəbl/|adjective|C1|سترگ، سهمگین، تحسین‌برانگیز و دشوار|causing you to have fear or respect for something because it is large or powerful|Climate adaptation presents a formidable challenge to global governance.|سازگاری با اقلیم چالشی سترگ و سهمگین فراروی حکمرانی جهانی قرار می‌دهد.|formidable obstacle|daunting, imposing|pack_c1_advanced",
            "legacy|/ˈleɡəsi/|noun|B2|میراث، یادگار بر جای مانده از گذشتگان|something that is a result of events in the past|The Renaissance left an enduring cultural legacy to humanity.|عصر رنسانس میراث فرهنگی پایداری برای بشریت بر جای نهاد.|enduring legacy|heritage, bequest|pack_oxford_essential",
            "trigger|/ˈtrɪɡə/|verb|B2|ماشه را کشیدن، جرقه زدن، کلید شروع یک رخداد|to cause something to start|Dust mites can trigger severe asthma attacks in sensitive patients.|کنه‌های گردوغبار می‌توانند حملات شدید آسم را در بیماران حساس برانگیزند.|trigger a reaction|spark, prompt|pack_ielts_writing",
            "reinforce|/ˌriːɪnˈfɔːs/|verb|B2|تقویت کردن، محکم‌تر ساختن پایه‌ها یا باورها|to make something stronger|Positive feedback reinforces students' learning motivation.|بازخورد مثبت، انگیزه یادگیری دانش‌آموزان را تقویت می‌کند.|reinforce a belief|strengthen, bolster|pack_ielts_academic",
            "dispute|/dɪˈspjuːt/|noun|B2|مناقشه، اختلاف نظر حقوقی یا کاری|an argument or disagreement, especially between groups|Border disputes should be settled peacefully through diplomatic dialogue.|مناقشات مرزی باید مسالمت‌آمیز و از طریق گفتگوی دیپلماتیک حل و فصل شوند.|settle a dispute|conflict, contention|pack_ielts_academic",
            "preliminary|/prɪˈlɪmɪnəri/|adjective|B2|مقدماتی، اولیه، پیش‌زمینه|coming before a more important action or event, especially introducing or preparing for it|Preliminary findings indicate that the vaccine is safe and efficacious.|یافته‌های اولیه نشان می‌دهند که واکسن ایمن و اثربخش است.|preliminary results|initial, preparatory|pack_toefl_academic"
        )

        return rawData.mapIndexed { index, raw ->
            val parts = raw.split("|")
            val word = parts.getOrNull(0) ?: "word"
            val ipa = parts.getOrNull(1) ?: ""
            val pos = parts.getOrNull(2) ?: "noun"
            val level = parts.getOrNull(3) ?: "B2"
            val meaningFa = parts.getOrNull(4) ?: ""
            val defEn = parts.getOrNull(5) ?: ""
            val exEn = parts.getOrNull(6) ?: ""
            val exFa = parts.getOrNull(7) ?: ""
            val colloc = parts.getOrNull(8) ?: ""
            val syns = parts.getOrNull(9)?.split(",")?.map { it.trim() } ?: emptyList()
            val pack = parts.getOrNull(10) ?: "pack_oxford_essential"

            VocabularyItem(
                id = (100 + index).toLong(),
                word = word,
                normalizedWord = word.lowercase().trim(),
                ipa = ipa,
                persianMeaning = meaningFa,
                englishDefinition = defEn,
                partOfSpeech = pos,
                example = exEn,
                examplePersian = exFa,
                cefrLevel = level,
                synonyms = syns,
                antonyms = emptyList(),
                collocations = if (colloc.isNotEmpty()) listOf(colloc) else emptyList(),
                wordFamily = emptyList(),
                commonMistakes = "در ریدینگ و رایتینگ با توجه به کالوکیشن‌های مرتبط استفاده شود.",
                ieltsRelevance = if (level in listOf("C1", "C2", "B2")) "High" else "Medium",
                toeflRelevance = if (pack == "pack_toefl_academic" || level == "C1") "High" else "Medium",
                tags = listOf(level, pack.removePrefix("pack_")),
                packName = pack
            )
        }
    }
}
