package com.example.data.seed

import com.example.data.model.VocabularyItem

object VocabSeedExtended3 {
    fun getItems(): List<VocabularyItem> {
        val words = listOf(
            "hypothetical|/ˌhaɪpəˈθetɪkl/|adjective|C1|فرضی، تئوریک، مبتنی بر فرض|based on possible situations or events rather than ones that you know exist|Let us examine a hypothetical scenario where interest rates double.|بیایید سناریویی فرضی را بررسی کنیم که در آن نرخ بهره دو برابر می‌شود.",
            "indispensable|/ˌɪndɪˈspensəbl/|adjective|C1|ضروری، واجب، غیرقابل چشم‌پوشی|someone or something that is so good or important that you could not manage without them|Clean drinking water is an indispensable prerequisite for public health.|آب آشامیدنی پاک پیش‌نیازی غیرقابل چشم‌پوشی برای بهداشت عمومی است.",
            "induce|/ɪnˈdjuːs/|verb|C1|تحریک کردن، موجب شدن، واداشتن|to cause something to happen; to persuade someone to do something|Prolonged solitary confinement can induce acute psychiatric symptoms.|حبس انفرادی طولانی‌مدت می‌تواند علائم حاد روان‌پزشکی ایجاد کند.",
            "infer|/ɪnˈfɜː/|verb|B2|استنباط کردن، پی بردن از شواهد|to form an opinion or guess that something is true because of the information that you have|Readers can infer the protagonist's motives from subtle clues.|خوانندگان می‌توانند انگیزه‌های قهرمان داستان را از سرنخ‌های ظریف استنباط کنند.",
            "ingenious|/ɪnˈdʒiːniəs/|adjective|C1|باهوش، مبتکرانه، خلاقانه|very intelligent and skilful, or skilfully made|Nomadic builders devised ingenious thermal cooling towers.|معماران کوچ‌نشین بادگیرهای سرمایشی مبتکرانه‌ای ابداع کردند.",
            "inhibit|/ɪnˈhɪbɪt/|verb|C1|مهار کردن، بازداشتن، مانع شدن از رشد|to prevent someone from doing something by making them feel nervous or embarrassed|Fear of making grammatical errors should not inhibit your speaking fluency.|ترس از اشتباهات گرامری نباید مانع از روانی کلام شما شود.",
            "innate|/ɪˈneɪt/|adjective|C1|ذاتی، مادرزادی، نهادینه‌شده در طبیعت|an innate quality or ability is one that you were born with, not one you have learned|Chomsky posited an innate human faculty for language acquisition.|چامسکی قائل به یک موهبت ذاتی بشری برای فراگیری زبان بود.",
            "insight|/ˈɪnsaɪt/|noun|B2|بینش عمیق، درک شهودی، فهم موشکافانه|a clear, deep, and sometimes sudden understanding of a complicated problem|Her dissertation offers valuable insights into Persian linguistics.|رساله او بینش‌های ارزشمندی درباره زبان‌شناسی فارسی ارائه می‌دهد.",
            "integral|/ˈɪntɪɡrəl/|adjective|C1|جدایی‌ناپذیر، اساسی، جزء لاینفک|necessary and important as a part of a whole|Critical thinking is an integral component of university scholarship.|تفکر نقادانه جزء لاینفک تحصیلات دانشگاهی است.",
            "integrity|/ɪnˈteɡrəti/|noun|B2|درستکاری اخلاقی، یکپارچگی ساختار|the quality of being honest and having strong moral principles|Academic integrity requires truthful attribution of all citations.|درستکاری دانشگاهی مستلزم ارجاع‌دهی صادقانه به تمامی نقل‌قول‌هاست.",
            "intervene|/ˌɪntəˈviːn/|verb|B2|مداخله کردن، پادرمیانی کردن در منازعه|to intentionally become involved in a difficult situation in order to improve it|Central banks intervene in foreign exchange markets during panic.|بانک‌های مرکزی هنگام هراس و بحران در بازارهای ارز مداخله می‌کنند.",
            "intrinsic|/ɪnˈtrɪnzɪk/|adjective|C1|ذاتی، درونی، جزء ماهیت چیزی|being an extremely important and basic characteristic of a person or thing|Curiosity is an intrinsic driving force in human exploration.|کنجکاوی یک نیروی محرکه ذاتی در کاوش‌های بشری است.",
            "invoke|/ɪnˈvəʊk/|verb|C1|استناد جستن به قانون، فراخواندن|to mention or use a law, rule, etc. as a reason for doing something|The governor invoked emergency powers during the hurricane.|فرماندار اختیارات اضطراری را در طول طوفان فراخواند و به کار بست.",
            "juxtapose|/ˌdʒʌkstəˈpəʊz/|verb|C2|کنار هم نهادن برای مقایسه، همجوار ساختن|to put things that are not similar next to each other in order to compare them|The exhibition juxtaposes classical miniatures with modern abstracts.|نمایشگاه نگارگری‌های سنتی را در کنار آثار انتزاعی مدرن قرار می‌دهد.",
            "legitimate|/lɪˈdʒɪtɪmət/|adjective|B2|مشروع، قانونی، موجه و منطقی|allowed by law, or reasonable and acceptable|Citizens raised legitimate concerns regarding data privacy violations.|شهروندان نگرانی‌های کاملاً موجهی در رابطه با نقض حریم خصوصی داده‌ها مطرح ساختند.",
            "magnitude|/ˈmæɡnɪtjuːd/|noun|C1|بزرگی، عظمت، مقیاس زلزله یا رویداد|the large size or importance of something|Few understood the true magnitude of the impending ecological shift.|اندک‌کسانی بزرگی و عظمت واقعی دگرگونی زیست‌محیطی پیش‌رو را درک کردند.",
            "manifest|/ˈmænɪfest/|verb|C1|آشکار ساختن، متجلی شدن نشانه بیماری|to show something clearly, through signs or actions|Anxiety disorders often manifest as physical palpitations and insomnia.|اختلالات اضطرابی اغلب خود را در قالب تپش قلب و بی‌خوابی متجلی می‌سازند.",
            "manipulate|/məˈnɪpjuleɪt/|verb|B2|دستکاری کردن داده‌ها، تحت‌تاثیر قرار دادن غیرمنصفانه|to control something or someone to your advantage, often unfairly or dishonestly|Unethical actors manipulate online voting algorithms to sway elections.|عوامل غیراخلاقی الگوریتم‌های رای‌گیری را برای اثرگذاری بر انتخابات دستکاری می‌کنند.",
            "momentous|/məˈmentəs/|adjective|C1|بسیار پراهمیت، خطیر، سرنوشت‌ساز در تاریخ|very important because of effects on future events|The signing of the declaration was a momentous milestone.|امضای بیانیه یک نقطه عطف تاریخی و بسیار پراهمیت بود.",
            "monotonous|/məˈnɒtənəs/|adjective|B2|کسل‌کننده، یکنواخت و ملال‌آور|not changing and therefore boring|Assembly line workers endure monotonous, repetitive tasks daily.|کارگران خط مونتاژ روزانه کارهای یکنواخت و کسل‌کننده‌ای را تاب می‌آورند.",
            "mutate|/mjuːˈteɪt/|verb|B2|جهش ژنتیکی یافتن، دگرگون شدن شکل|to develop new physical characteristics because of a permanent change in genes|Viruses can mutate rapidly, outpacing antibody defenses.|ویروس‌ها می‌توانند به سرعت جهش یافته و از پادتن‌های دفاعی پیشی گیرند.",
            "navigate|/ˈnævɪɡeɪt/|verb|B1|مسیریابی کردن، از پس پیچیدگی‌ها برآمدن|to direct the way that a ship, aircraft, etc. will travel, or find a way through a problem|Immigrants must navigate complex municipal legal frameworks.|مهاجران ناچارند از پس پیچیدگی‌های چارچوب‌های حقوقی شهری برآیند.",
            "negate|/nɪˈɡeɪt/|verb|C1|خنثی کردن، باطل ساختن اثر مثبت پیشین|to cause something to have no effect or to be only a negative value|Poor sleep hygiene can negate the physiological benefits of gym training.|بهداشت نامناسب خواب می‌تواند فواید فیزیولوژیک ورزش را خنثی سازد.",
            "norm|/nɔːm/|noun|B2|هنجار، عرف پذیرفته‌شده اجتماعی|an accepted standard or a way of behaving or doing things that is usual|Remote employment has become the prevailing norm in software firms.|دورکاری در شرکت‌های نرم‌افزاری به هنجار غالب تبدیل شده است.",
            "notable|/ˈnəʊtəbl/|adjective|B2|شایان ذکر، چشمگیر، درخور توجه|important and deserving attention, because of being very good or unusual|There was a notable improvement in speaking accuracy this term.|بهبود درخور توجهی در دقت گفتار در این ترم حاصل شد.",
            "obscure|/əbˈskjʊə/|adjective|C1|گمنام، مبهم، ناشناخته و پنهان|not known to many people, or not clear and difficult to understand|The poet remained relatively obscure during his turbulent lifetime.|شاعر در طول دوران پرتلاطم حیات خود نسبتاً گمنام باقی ماند.",
            "ominous|/ˈɒmɪnəs/|adjective|C1|شوم، نگران‌کننده، حاکی از رخدادی ناگوار|suggesting that something unpleasant is likely to happen|Dark ominous clouds gathered above the coastal village.|ابرهای تیره و شوم بر فراز دهکده ساحلی گرد آمدند.",
            "ongoing|/ˈɒnɡəʊɪŋ/|adjective|B2|مداوم، ادامه‌دار، در حال انجام|continuing to exist or develop, or happening at the present moment|There is an ongoing public debate regarding artificial general intelligence.|بحثی ادامه‌دار و مداوم درباره هوش جامع مصنوعی در جریان است.",
            "opaque|/əʊˈpeɪk/|adjective|C1|کدر، مات، ناواضح در شفافیت مالی یا ساختار|not able to be seen through; not transparent, or hard to understand|Corporate tax havens utilize opaque financial mechanisms.|پناهگاه‌های مالیاتی شرکت‌ها از سازوکارهای مالی مبهم و کدر بهره می‌جویند.",
            "paramount|/ˈpærəmaʊnt/|adjective|C1|دارای بالاترین درجه اهمیت، مافوق همه چیز|more important than anything else|Passenger safety is of paramount concern to commercial airlines.|ایمنی مسافران برای خطوط هوایی بالاترین درجه اهمیت را دارد.",
            "perennial|/pəˈreniəl/|adjective|C1|همیشگی، دیرپا، چندساله در گیاه‌شناسی|lasting a very long time, or happening again and again|Traffic congestion is a perennial grievance for suburban commuters.|ترافیک گره‌خورده شکایتی همیشگی برای حاشیه‌نشینان شهرها است.",
            "peripheral|/pəˈrɪfərəl/|adjective|C1|پیرامونی، حاشیه‌ای، غیراصلی|not as important as other things, or at the edge of an area|Do not waste precious exam time on peripheral minor details.|وقت گرانبهای آزمون را برای جزئیات حاشیه‌ای و کم‌اهمیت تلف نکنید.",
            "permeate|/ˈpɜːmieɪt/|verb|C1|نفوذ کردن، رسوخ نمودن در تمام اجزا|to spread through something and be present in every part of it|Digitalization has permeated every tier of modern education.|فناوری دیجیتال در تمام سطوح آموزش مدرن نفوذ و رسوخ یافته است.",
            "pervasive|/pəˈveɪsɪv/|adjective|C1|فراگیر، همه‌گیر، جاری در تمامی ارکان|present or noticeable in every part of a thing or place|Corruption had a pervasive corroding effect on municipal institutions.|فساد اثری فراگیر و فرساینده بر نهادهای شهری بر جای گذاشت.",
            "phenomenon|/fəˈnɒmɪnən/|noun|B2|پدیده، رویداد طبیعی یا اجتماعی (جمع phenomena)|something that exists and can be seen, felt, tasted, etc., especially something unusual|Global warming is not a localized anomaly but a planetary phenomenon.|گرمایش زمین یک ناهنجاری محلی نبوده بلکه پدیده‌ای در مقیاس کل سیاره است.",
            "pioneer|/ˌpaɪəˈnɪə/|noun|B2|پیشگام، پیشتاز، طلایه‌دار یک عرصه|a person who is one of the first people to do something|Marie Curie was a pioneer in radiological scientific discovery.|ماری کوری پیشگامی در اکتشافات علمی پرتوشناسی بود.",
            "plausible|/ˈplɔːzəbl/|adjective|B2|باورپذیر، محتمل و منطقی|seeming likely to be true, or able to be believed|The detective formulated a plausible reconstruction of the crime.|کارآگاه بازسازی منطقی و باورپذیری از صحنه جنایت صورت‌بندی کرد.",
            "polarize|/ˈpəʊləraɪz/|verb|C1|دوقطبی کردن، منشعب ساختن جامعه به دو جناح متضاد|to cause something, especially something that contains different people or opinions, to divide into two completely opposing groups|Controversial cultural issues can deeply polarize the electorate.|مباحث مناقشه‌برانگیز فرهنگی می‌توانند رای‌دهندگان را عمیقاً دوقطبی سازند.",
            "ponder|/ˈpɒndə/|verb|C1|ژرف اندیشیدن، سبک‌سنگین کردن تصمیم|to think carefully about something, especially for a noticeable length of time|He spent the weekend pondering the university's tenure offer.|او آخر هفته را صرف سبک‌سنگین کردن و تامل بر سر پیشنهاد استادی کرد.",
            "precedent|/ˈpresɪdənt/|noun|C1|سابقه، پیشینه حقوقی، نمونه قبلی|an action, situation, or decision that has already happened and can be used as an example|This court verdict establishes a binding precedent for future cases.|این حکم دادگاه پیشینه‌ای الزام‌آور برای پرونده‌های آتی پایه‌ریزی می‌کند.",
            "precise|/prɪˈsaɪs/|adjective|B1|دقیق، بی‌نقص در ارقام و کلمات|exact and accurate in form, detail, or execution|Scientists need precise instruments to record subtle seismic tremors.|دانشمندان به ابزارهایی دقیق برای ثبت ارتعاشات ظریف لرزه‌ای نیازمندند.",
            "predominate|/prɪˈdɒmɪneɪt/|verb|C1|غلبه داشتن، اکثریت قاطع بودن|to be the largest in number or have the most importance or power|Cereal crops predominate across the vast fertile plains.|محصولات غلات بر دشت‌های پهناور و حاصلخیز غلبه دارند.",
            "profound|/prəˈfaʊnd/|adjective|C1|عمیق، ژرف، دارای اثر ماندگار|felt or experienced very strongly or in an extreme way|Philosophical treatises delve into profound questions of human existence.|رساله‌های فلسفی به پرسش‌های ژرف هستی انسان می‌پردازند.",
            "prohibit|/prəˈhɪbɪt/|verb|B2|ممنوع اعلام کردن رسمی، قدغن ساختن|to officially forbid something by law or rule|City regulations strictly prohibit open campfires in dry pine forests.|مقررات شهری برپایی آتش در جنگل‌های خشک کاج را اکیداً ممنوع اعلام کرده‌اند.",
            "prolific|/prəˈlɪfɪk/|adjective|C1|پرکار، پربار، زایا در هنر یا علم|producing a great number or amount of something|Mozart was an astonishingly prolific classical composer.|موتسارت آهنگسازی شگفت‌انگیز و فوق‌العاده پرکار بود.",
            "propagate|/ˈprɒpəɡeɪt/|verb|C1|تکثیر کردن، اشاعه دادن باورها|to produce a new plant using a parent plant; to spread opinions|Botanists propagate rare orchids in controlled greenhouse nurseries.|گیاه‌شناسان ارکیده‌های کمیاب را در گلخانه‌های پایش‌شده تکثیر می‌نمایند.",
            "prototype|/ˈprəʊtətaɪp/|noun|B2|نمونه اولیه، پیش‌الگوی تولیدی|the first example of something, such as a machine or other industrial product|Engineers built a working prototype of the hydrogen engine.|مهندسان نمونه اولیه کاربردی از موتور هیدروژنی ساختند.",
            "prudent|/ˈpruːdnt/|adjective|C1|محتاط، عاقلانه، دوراندیش در عمل|careful and avoiding risks; showing good judgment|It is prudent to diversify investment portfolios against downturns.|دوراندیشانه است که سبد سرمایه‌گذاری را در برابر رکودها متنوع ساخت.",
            "qualitative|/ˈkwɒlɪtətɪv/|adjective|B2|کیفی، مبتنی بر توصیف و عمق|relating to how good or bad something is, rather than how much of it there is|Researchers conducted qualitative interviews with cancer survivors.|پژوهشگران مصاحبه‌های کیفی با نجات‌یافتگان از سرطان به عمل آوردند.",
            "quantitative|/ˈkwɒntɪtətɪv/|adjective|B2|کمی، مبتنی بر ارقام و آمار|relating to numbers or amounts|The economist based her forecasts on quantitative regression models.|اقتصاددان پیش‌بینی‌های خود را بر مدل‌های رگرسیون کمی استوار ساخت.",
            "quarantine|/ˈkwɒrəntiːn/|noun|B2|قرنطینه، جداسازی برای پیشگیری از سرایت|a period of time during which an animal or person that might have a disease is kept away|International travelers were placed under quarantine during the pandemic.|مسافران بین‌المللی در طول همه‌گیری تحت قرنطینه بهداشتی قرار گرفتند.",
            "rational|/ˈræʃnəl/|adjective|B2|عقلانی، منطقی، به دور از احساسات کور|based on clear thought and reason rather than on emotions|Consumers do not always act as entirely rational economic agents.|مصرف‌کنندگان همواره به عنوان عاملان اقتصادی کاملاً عقلانی رفتار نمی‌کنند.",
            "reconcile|/ˈrekənsaɪl/|verb|C1|آشتی دادن، سازگار ساختن دو واقعیت متناقض|to find a way in which two situations or beliefs that are opposed can agree|It is hard to reconcile industrial expansion with carbon neutrality goals.|آشتی دادن و سازگار ساختن گسترش صنعتی با اهداف کربن صفر کاری بس دشوار است.",
            "redundant|/rɪˈdʌndənt/|adjective|B2|اضافی، تعدیل‌شده از کار|unnecessary because there is more than is needed|Many jobs were made redundant after the factory implemented robotics.|شغل‌های فراوانی پس از اجرای رباتیک در کارخانه مازاد و تعدیل گردیدند.",
            "refine|/rɪˈfaɪn/|verb|B2|پالایش کردن، تصفیه نفت، صیقل دادن مهارت|to make something pure or improve something, especially by removing unwanted material|Writers refine their prose through repeated revision and editing.|نویسندگان با بازبینی و ویرایش‌های مکرر، نثر خویش را صیقل می‌دهند.",
            "reinforce|/ˌriːɪnˈfɔːs/|verb|B2|مستحکم ساختن، پشتیبانی کردن از فرضیه|to make something stronger or more effective|Empirical survey data reinforces the validity of our conclusion.|داده‌های تجربی نظرسنجی، روایی نتیجه‌گیری ما را مستحکم می‌سازد.",
            "reluctant|/rɪˈlʌktənt/|adjective|B2|بی‌میل، اکراه داشتن در انجام کار|not willing to do something and therefore slow to do it|He was reluctant to admit his methodological mistakes.|او در پذیرش خطاهای روش‌شناختی خود بی‌میل و بی‌رغبت بود.",
            "remedy|/ˈremədi/|noun|B2|راه چاره، درمان دارویی، جبران حقوقی|a successful way of curing an illness or dealing with a problem|Fiscal stimulus proved an effective remedy for economic stagnation.|محرک‌های مالی راه چاره‌ای اثربخش برای رکود اقتصادی از کار درآمدند.",
            "replenish|/rɪˈplenɪʃ/|verb|C1|دوباره پر کردن ذخایر، تجدید منابع|to fill something up again with supplies or liquid|Rainfall helped replenish depleted municipal reservoirs.|بارش باران به پر شدن دوباره مخازن تهی‌شده شهری یاری رساند.",
            "replicate|/ˈreplɪkeɪt/|verb|C1|تکرار کردن آزمایش، نسخه‌برداری دقیق|to make or do something again in exactly the same way|Independent labs failed to replicate the original experimental results.|آزمایشگاه‌های مستقل در تکرار نتایج تجربی اولیه توفیقی نیافتند.",
            "resilient|/rɪˈzɪliənt/|adjective|B2|تاب‌آور، مقاوم در برابر تندباد حوادث|able to be happy, successful, etc. again after something difficult|Supply chains must become more resilient against geopolitical shocks.|زنجیره‌های تامین باید در برابر شوک‌های ژئوپلیتیک تاب‌آورتر گردند.",
            "retrospect|/ˈretrəspekt/|noun|C1|نگاه به گذشته، بازنگری پیشینه|thinking now about something in the past|In retrospect, postponing the acquisition was a judicious choice.|در نگاه به گذشته و بازنگری، به تعویق انداختن ادغام تصمیمی خردمندانه بود.",
            "rigorous|/ˈrɪɡərəs/|adjective|C1|دقیق و موشکافانه، سخت‌گیرانه در استاندارد|careful to look at or consider every part of something to make sure it is correct|Peer reviewers subject clinical trials to rigorous evaluation.|داوران مقالات علمی، کارآزمایی‌های بالینی را تحت ارزیابی موشکافانه و سخت‌گیرانه‌ای قرار می‌دهند.",
            "robust|/rəʊˈbʌst/|adjective|B2|نیرومند، تنومند، تاب‌آور در برابر خطا|strong and unlikely to break or fail|The server infrastructure is robust enough to manage peak traffic.|زیرساخت سرور آنچنان نیرومند و استوار است که ترافیک اوج را مدیریت کند.",
            "rudimentary|/ˌruːdɪˈmentri/|adjective|C1|ابتدایی، بدوی، دارای کمترین پیچیدگی|basic and not well developed|Early hominids fashioned rudimentary flint scraping blades.|انسان‌تباران اولیه تیغه‌های خراشنده سنگ‌چخماق بسیار ابتدایی ساختند.",
            "scrutinize|/ˈskruːtənaɪz/|verb|C1|وارسی دقیق کردن، با ذره‌بین نگریستن|to examine something very carefully in order to discover information|Auditors will scrutinize the company's financial balance sheets.|حسابرسان ترازنامه‌های مالی شرکت را با موشکافی تمام وارسی خواهند نمود.",
            "simulate|/ˈsɪmjuleɪt/|verb|B2|شبیه‌سازی کردن، تقلید شرایط با رایانه|to do or make something that looks real but is not real|Computer models simulate climate patterns over centuries.|مدل‌های رایانه‌ای الگوهای اقلیمی را در طول قرن‌ها شبیه‌سازی می‌نمایند.",
            "simultaneous|/ˌsɪmlˈteɪniəs/|adjective|B2|هم‌زمان، در یک لحظه رخ‌داده|happening or being done at exactly the same time|There were simultaneous protests in twenty major world capitals.|تظاهرات هم‌زمانی در بیست پایتخت مهم جهان برپا گردید.",
            "speculate|/ˈspekjuleɪt/|verb|B2|حدس و گمان زدن، سفته‌بازی مالی|to guess possible answers to a question without having enough information|Journalists continue to speculate about the cabinet reshuffle.|روزنامه‌نگاران همچنان به حدس و گمان درباره ترمیم کابینه ادامه می‌دهند.",
            "spontaneous|/spɒnˈteɪniəs/|adjective|B2|خودجوش، بی‌اختیار، بدون برنامه‌ریزی قبلی|happening naturally, without being planned or thought about in advance|The crowd erupted in spontaneous applause as the soloist concluded.|با پایان تکنوازی، جمعیت غریو تشویقی خودجوش سر دادند.",
            "sporadic|/spəˈrædɪk/|adjective|C1|گاه‌وبیگاه، پراکنده، با فواصل نامنظم|happening sometimes; not regular or continuous|Fighting has subsided into sporadic border skirmishes.|درگیری‌ها فروکش کرده و به زد‌وخوردهای مرزی پراکنده و گاه‌وبیگاه بدل گشته است.",
            "stagnant|/ˈstæɡnənt/|adjective|C1|راکد، بی‌حرکت، ایستا در اقتصاد یا آب|not growing or developing; not moving or flowing|Real wages have remained virtually stagnant over the past decade.|دستمزدهای واقعی طی دهه گذشته عملاً راکد و ایستا باقی مانده‌اند.",
            "stipulate|/ˈstɪpjuleɪt/|verb|C1|شرط کردن، در قرارداد تصریح نمودن|to state exactly what must be done|The lease stipulates that tenants must not sublet the premises.|اجاره‌نامه تصریح می‌کند که مستاجران نباید ملک را به دیگری واگذار کنند.",
            "stringent|/ˈstrɪndʒənt/|adjective|C1|سخت‌گیرانه، شدید در مقررات و ضوابط|having a very severe effect, or being extremely limiting and strict|Automakers must satisfy stringent new vehicle emission thresholds.|خودروسازان باید ضوابط سخت‌گیرانه جدید انتشار آلاینده‌ها را تامین کنند.",
            "subtle|/ˈsʌtl/|adjective|C1|ظریف، باریک‌بینانه، نامحسوس|not loud, bright, noticeable, or obvious in any way|Subtle changes in body language often betray underlying anxiety.|دگرگونی‌های ظریف در زبان بدن اغلب اضطراب درونی را آشکار می‌سازند.",
            "succinct|/səkˈsɪŋkt/|adjective|C1|موجز، مختصر و مفید، خلاصه|said in a clear and short way; expressing what needs to be said without unnecessary words|Provide a succinct overview of your previous academic achievements.|خلاصه‌ای موجز و مفید از دستاوردهای تحصیلی پیشین خود ارائه دهید.",
            "supplement|/ˈsʌplɪment/|verb|B2|تکمیل کردن، مکمل افزودن|to add something to make it larger or better|She works part-time to supplement her university scholarship.|او به صورت پاره‌وقت کار می‌کند تا کمک‌هزینه تحصیلی دانشگاهی خود را تکمیل نماید.",
            "suppress|/səˈpres/|verb|C1|سرکوب کردن، پنهان نگه داشتن مدرک|to prevent something from being seen or expressed|Authoritarian regimes routinely suppress independent journalists.|رژیم‌های اقتدارگرا پیوسته روزنامه‌نگاران مستقل را سرکوب می‌نمایند.",
            "surplus|/ˈsɜːpləs/|noun|B2|مازاد، فزونی تولید بر مصرف، تراز مثبت|an amount that is more than is needed|The agricultural cooperative exported its grain surplus abroad.|تعاونی کشاورزی مازاد غلات خود را به خارج از کشور صادر کرد.",
            "synthesize|/ˈsɪnθəsaɪz/|verb|C1|ترکیب و سنتز کردن، تلفیق ایده‌ها|to produce a substance by a chemical reaction, or combine different ideas|The literature review synthesizes findings from forty published studies.|مرور پیشینه پژوهش، یافته‌های برگرفته از چهل مقاله منتشرشده را تلفیق و سنتز می‌نماید.",
            "tangible|/ˈtændʒəbl/|adjective|C1|ملموس، عینی، قابل لمس و اثبات|real and not imaginary; able to be shown or touched|The training resulted in tangible improvements in customer retention.|این آموزش به پیشرفت‌های ملموس و عینی در حفظ مشتریان منجر گشت.",
            "tedious|/ˈtiːdiəs/|adjective|B2|خسته‌کننده، کسالت‌بار، طولانی و پرزحمت|boring and tiring, continuing for a long time|Checking thousands of spreadsheet entries is tedious work.|بررسی هزاران ردیف در صفحات گسترده، کاری بسیار خسته‌کننده و کسالت‌بار است.",
            "terminate|/ˈtɜːmɪneɪt/|verb|B2|خاتمه دادن به قرارداد، پایان یافتن خط سیر|to cause something to end, or to end|The university reserved the right to terminate the fellowship.|دانشگاه حق خاتمه دادن و لغو بورسیه پژوهشی را برای خود محفوظ داشت.",
            "tolerate|/ˈtɒləreɪt/|verb|B1|تحمل کردن، مدارا کردن با عقاید دیگران|to accept behaviour and beliefs that are different from your own, or deal with pain|Civilized communities tolerate diverse viewpoints with respect.|جوامع متمدن با دیدگاه‌های گوناگون با مدارا و احترام برخورد می‌کنند.",
            "tranquil|/ˈtræŋkwɪl/|adjective|B2|آرامش‌بخش، آرام، به دور از هیاهو|calm and peaceful and without noise or excitement|The mountain sanctuary offered a tranquil escape from urban frenzy.|پناهگاه کوهستانی پناهگاهی آرام و دلپذیر به دور از هیاهوی شهری فراهم آورد.",
            "transient|/ˈtrænziənt/|adjective|C1|گذرا، ناپایدار، کوتاه‌مدت|lasting for only a short time; temporary|Fame on social media is notoriously transient and fickle.|شهرت در شبکه‌های اجتماعی مشهور به گذرا و ناپایدار بودن است.",
            "transparent|/trænˈspærənt/|adjective|B2|شفاف، روشن، بلورین و بدون پنهان‌کاری|open and honest, without secrets; clear to see through|Government agencies should adopt fully transparent budget accounts.|نهادهای دولتی باید حساب‌های بودجه‌ای کاملاً شفاف و روشنی اتخاذ نمایند.",
            "ubiquitous|/juːˈbɪkwɪtəs/|adjective|C1|همه‌جا حاضر، فراگیر در جامعه|present or found everywhere|Wi-Fi access has become ubiquitous in modern international airports.|دسترسی به وای‌فای در فرودگاه‌های بین‌المللی مدرن همه‌جا حاضر و فراگیر گشته است.",
            "ultimate|/ˈʌltɪmət/|adjective|B2|نهایی، غایی، بالاترین درجه چیزی|most important, highest, last, or final|Our ultimate goal is attaining native-like conversational fluency.|هدف غایی و نهایی ما دستیابی به روانی کلام همانند گویشوران بومی است.",
            "unanimous|/juˈnænɪməs/|adjective|C1|به اتفاق آرا، متفق‌القول|in complete agreement, or showing that everyone agrees|The jury reached a unanimous verdict after two hours of deliberation.|هیئت منصفه پس از دو ساعت رایزنی به حکمی به اتفاق آرا دست یافتند.",
            "unprecedented|/ʌnˈpresɪdentɪd/|adjective|C1|بی‌سابقه، بی‌مانند در تاریخ|never having happened or existed in the past|The pandemic triggered unprecedented economic volatility worldwide.|همه‌گیری نوسانات اقتصادی بی‌سابقه‌ای را در سراسر جهان کلید زد.",
            "utilize|/ˈjuːtəlaɪz/|verb|B2|به کار بردن، بهره‌برداری مفید از منابع|to use something in an effective way|Surgeons utilize robotic arms for minimally invasive procedures.|جراحان از بازوهای رباتیک برای عمل‌های با حداقل تهاجم بهره می‌برند.",
            "valid|/ˈvælɪd/|adjective|B1|معتبر، دارای اعتبار قانونی یا علمی|based on truth or reason; accepted officially|You must present a valid passport at the border crossing.|شما باید گذرنامه‌ای معتبر در گذرگاه مرزی ارائه دهید.",
            "variable|/ˈveəriəbl/|noun|B2|متغیر آماری، عامل تغییرپذیر|a number, amount, or situation which can change|Temperature is the independent variable in this experiment.|دما در این آزمایش متغیر مستقل به شمار می‌رود.",
            "viable|/ˈvaɪəbl/|adjective|B2|دوام‌پذیر، شدنی، عملی و اقتصادی|able to work as intended or succeed|Geothermal heating is a viable energy solution for northern homes.|گرمایش زمین‌گرمایی راهکاری شدنی و دوام‌پذیر برای خانه‌های شمالی است.",
            "vibrant|/ˈvaɪbrənt/|adjective|B2|پرشور، پرجنب‌وجوش، سرزنده|energetic, exciting, and full of enthusiasm|Isfahan boasts a vibrant artistic and architectural heritage.|اصفهان از میراث هنری و معماری پرشور و سرزنده‌ای برخوردار است.",
            "vindicate|/ˈvɪndɪkeɪt/|verb|C2|تبرئه کردن، اثبات حقانیت با شواهد بعدی|to prove that what someone said or did was right after doubt|Subsequent DNA testing served to vindicate the wrongly convicted man.|آزمایش بعدی دی‌ان‌ای به اثبات حقانیت و تبرئه مرد محکوم‌شده انجامید.",
            "vital|/ˈvaɪtl/|adjective|B1|حیاتی، مبرم، برای بقا واجب|necessary for the success or continued existence of something|Vaccination plays a vital role in curbing infectious mortality.|واکسیناسیون نقشی حیاتی در مهار مرگ‌ومیر ناشی از بیماری‌های عفونی ایفا می‌کند.",
            "vivid|/ˈvɪvɪd/|adjective|B2|زنده، روشن در ذهن، پر از رنگ و جلا|vivid descriptions or memories produce clear, powerful mental images|The memoir paints a vivid portrait of post-war reconstruction.|کتاب خاطرات پرتره‌ای زنده و روشن از بازسازی پس از جنگ ترسیم می‌کند.",
            "volatile|/ˈvɒlətaɪl/|adjective|C1|بی‌ثبات، نوسان‌پذیر، زود خشمگین|likely to change suddenly and unexpectedly|Cryptocurrency markets remain notoriously volatile for retail savers.|بازارهای رمزارز برای پس‌اندازکنندگان خرد همچنان به شکل بدنامی پرنوسان و بی‌ثبات هستند.",
            "vulnerable|/ˈvʌlnərəbl/|adjective|B2|آسیب‌پذیر، شکننده در برابر گزند|able to be easily hurt, influenced, or attacked|Infants and the elderly are especially vulnerable to respiratory infections.|نوزادان و سالمندان به ویژه در برابر عفونت‌های تنفسی آسیب‌پذیرند.",
            "warrant|/ˈwɒrənt/|verb|C1|ایجاب کردن، توجیه داشتن برای اقدام|to make a particular activity necessary or acceptable|The seriousness of the crisis warrants immediate parliamentary review.|وخامت بحران، بررسی فوری پارلمانی را ایجاب می‌نماید.",
            "widespread|/ˈwaɪdspred/|adjective|B2|گسترده، فراگیر در سطح جامعه|existing or happening in many places and among many people|The government proposal encountered widespread public opposition.|پیشنهاد دولت با مخالفت عمومی گسترده‌ای مواجه گردید.",
            "yield|/jiːld/|verb|B2|تسلیم شدن، بازدهی داشتن، ثمر دادن|to produce a result, answer, or profit; to give way to pressure|Careful crop rotation can yield substantially higher grain harvests.|تناوب هوشمندانه محصولات می‌تواند برداشت غلات به مراتب بالاتری ثمر دهد."
        )

        return words.mapIndexed { index, raw ->
            val parts = raw.split("|")
            val word = parts.getOrNull(0) ?: "word"
            val ipa = parts.getOrNull(1) ?: ""
            val pos = parts.getOrNull(2) ?: "noun"
            val level = parts.getOrNull(3) ?: "B2"
            val meaningFa = parts.getOrNull(4) ?: ""
            val defEn = parts.getOrNull(5) ?: ""
            val exEn = parts.getOrNull(6) ?: ""
            val exFa = parts.getOrNull(7) ?: ""

            VocabularyItem(
                id = (800 + index).toLong(),
                word = word,
                normalizedWord = word.lowercase().trim(),
                ipa = ipa,
                persianMeaning = meaningFa,
                englishDefinition = defEn,
                partOfSpeech = pos,
                example = exEn,
                examplePersian = exFa,
                cefrLevel = level,
                synonyms = emptyList(),
                antonyms = emptyList(),
                collocations = emptyList(),
                wordFamily = emptyList(),
                commonMistakes = "به کاربرد رسمی و آکادمیک در متن توجه شود.",
                ieltsRelevance = if (level in listOf("B2", "C1", "C2")) "High" else "Medium",
                toeflRelevance = if (level in listOf("B2", "C1", "C2")) "High" else "Medium",
                tags = listOf(level, "IELTS", "TOEFL", "ExamCore"),
                packName = when (level) {
                    "C1", "C2" -> "pack_c1_advanced"
                    "B2" -> "pack_ielts_academic"
                    else -> "pack_oxford_essential"
                }
            )
        }
    }
}
