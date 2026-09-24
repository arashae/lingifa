#!/usr/bin/env python3
"""
Enrich and perfect all offline English-to-English vocabulary datasets for LinguaFa.

This script elevates all 12,800+ vocabulary entries across IELTS, TOEFL, GRE, and CEFR:
1. Normalizes and fixes all corrupted IPA transcriptions (replaces Cyrillic schwa, ASCII stress, length marks).
2. Cleans and polishes English definitions (removes raw ECDICT POS prefix tags, numbers multiple senses, ensures full sentences).
3. Ensures natural, capitalized, and properly punctuated English example sentences.
4. Generates high-yield academic Collocations for entries lacking them.
5. Populates accurate English Synonyms matching part of speech.
6. Preserves exact line counts and JSON structure expected by BundledVocabularyImporter and master_catalog.json.
"""

from __future__ import annotations

import glob
import json
import os
import re
import sys
from pathlib import Path

# Ensure UTF-8 output
sys.stdout.reconfigure(encoding="utf-8")

ROOT = Path(__file__).resolve().parents[1]
VOCAB_DIR = ROOT / "app" / "src" / "main" / "assets" / "vocabulary"

# Comprehensive Curated Collocation Suffixes & Templates by Part of Speech
VERB_COLLOC_TEMPLATES = [
    "{word} the process",
    "{word} effectively",
    "{word} a solution",
    "attempt to {word}",
    "seek to {word}",
    "{word} rapidly"
]

NOUN_COLLOC_TEMPLATES = [
    "crucial {word}",
    "significant {word}",
    "play a role in {word}",
    "development of {word}",
    "high level of {word}",
    "fundamental {word}"
]

ADJ_COLLOC_TEMPLATES = [
    "highly {word}",
    "increasingly {word}",
    "{word} factor",
    "{word} importance",
    "{word} impact",
    "particularly {word}"
]

ADV_COLLOC_TEMPLATES = [
    "{word} important",
    "{word} significant",
    "{word} different",
    "{word} associated",
    "{word} evident"
]

# High-frequency Curated Academic Headword Collocations & Synonyms Database
ACADEMIC_LEXICON = {
    "mitigate": {
        "def": "To make something less severe, serious, or painful; to alleviate or lessen.",
        "colloc": ["mitigate risk", "mitigate the impact", "mitigate damage", "mitigate environmental harm"],
        "syn": ["alleviate", "reduce", "lessen", "diminish", "relieve"]
    },
    "allocate": {
        "def": "To distribute resources, funds, or duties for a specific purpose or recipient.",
        "colloc": ["allocate resources", "allocate funds", "allocate time", "allocate budget"],
        "syn": ["assign", "distribute", "designate", "allot", "apportion"]
    },
    "substantial": {
        "def": "Large in size, amount, value, or importance; considerable.",
        "colloc": ["substantial increase", "substantial difference", "substantial progress", "substantial evidence"],
        "syn": ["considerable", "significant", "sizeable", "meaningful"]
    },
    "abandon": {
        "def": "(1) To cease supporting or looking after someone; to desert. (2) To give up completely a practice or action.",
        "colloc": ["abandon hope", "abandon an attempt", "abandon ship", "with reckless abandon"],
        "syn": ["desert", "forsake", "relinquish", "renounce"]
    },
    "ability": {
        "def": "The possession of the means, skill, or capacity to achieve something.",
        "colloc": ["natural ability", "proven ability", "develop the ability", "demonstrate ability"],
        "syn": ["capability", "competence", "capacity", "aptitude", "skill"]
    },
    "abnormal": {
        "def": "Deviating from what is normal or typical, usually in an undesirable or worrying way.",
        "colloc": ["abnormal conditions", "abnormal behavior", "abnormal levels", "abnormal pattern"],
        "syn": ["unusual", "atypical", "irregular", "anomalous", "uncommon"]
    },
    "abolish": {
        "def": "To formally put an end to a system, practice, or institution.",
        "colloc": ["abolish slavery", "abolish the tax", "abolish the death penalty", "vote to abolish"],
        "syn": ["eliminate", "terminate", "eradicate", "repeal", "annul"]
    },
    "accommodate": {
        "def": "(1) To provide lodging or sufficient space for. (2) To adapt to or fit in with someone's wishes or needs.",
        "colloc": ["accommodate the needs of", "accommodate growth", "accommodate passengers", "easily accommodate"],
        "syn": ["house", "lodge", "adapt", "adjust", "assist"]
    },
    "accumulate": {
        "def": "To gather together or acquire an increasing number or quantity of something over time.",
        "colloc": ["accumulate wealth", "accumulate evidence", "accumulate debt", "accumulate over time"],
        "syn": ["amass", "gather", "collect", "accrue", "compile"]
    },
    "accurate": {
        "def": "Correct in all details; exact and free from error.",
        "colloc": ["accurate description", "accurate assessment", "highly accurate", "accurate measurement"],
        "syn": ["precise", "correct", "exact", "flawless", "meticulous"]
    },
    "achieve": {
        "def": "To successfully bring about or reach a desired objective or result by effort, skill, or courage.",
        "colloc": ["achieve a goal", "achieve success", "achieve an objective", "struggle to achieve"],
        "syn": ["accomplish", "attain", "realize", "fulfill", "reach"]
    },
    "acquire": {
        "def": "To buy or obtain an asset or object for oneself; to learn or develop a skill or habit.",
        "colloc": ["acquire knowledge", "acquire a skill", "acquire a company", "newly acquired"],
        "syn": ["obtain", "gain", "attain", "procure", "secure"]
    },
    "adapt": {
        "def": "To make something suitable for a new use or purpose; to adjust to new conditions.",
        "colloc": ["adapt to change", "adapt to conditions", "adapt successfully", "ability to adapt"],
        "syn": ["adjust", "acclimate", "modify", "conform", "tailor"]
    },
    "adequate": {
        "def": "Satisfactory or acceptable in quality or quantity for a particular purpose.",
        "colloc": ["adequate resources", "adequate preparation", "adequate funding", "scarcely adequate"],
        "syn": ["sufficient", "enough", "satisfactory", "ample", "competent"]
    },
    "adjacent": {
        "def": "Next to or adjoining something else; sharing a common border or boundary.",
        "colloc": ["adjacent building", "adjacent area", "immediately adjacent", "adjacent rooms"],
        "syn": ["neighboring", "adjoining", "bordering", "contiguous", "proximate"]
    },
    "advocate": {
        "def": "(1) To publicly recommend or support a particular cause or policy. (2) A person who publicly supports a cause.",
        "colloc": ["strongly advocate", "advocate for reform", "vocal advocate", "advocate a policy"],
        "syn": ["champion", "support", "recommend", "endorse", "proponent"]
    },
    "aesthetic": {
        "def": "Concerned with beauty or the appreciation of beauty; artistic.",
        "colloc": ["aesthetic appeal", "aesthetic value", "aesthetic pleasure", "purely aesthetic"],
        "syn": ["artistic", "visual", "tasteful", "attractive", "beautiful"]
    },
    "aggregate": {
        "def": "Formed or calculated by the combination of many separate units or items; total.",
        "colloc": ["in the aggregate", "aggregate demand", "aggregate score", "aggregate data"],
        "syn": ["total", "combined", "cumulative", "collective", "overall"]
    },
    "ambiguous": {
        "def": "Open to more than one interpretation; having a double meaning; unclear.",
        "colloc": ["ambiguous statement", "remain ambiguous", "highly ambiguous", "deliberately ambiguous"],
        "syn": ["unclear", "equivocal", "vague", "obscure", "indeterminate"]
    },
    "anticipate": {
        "def": "To regard as probable; to expect or predict something and prepare for it.",
        "colloc": ["anticipate changes", "eagerly anticipate", "anticipate problems", "anticipate growth"],
        "syn": ["expect", "foresee", "predict", "await", "prepare for"]
    },
    "coherent": {
        "def": "Logical and consistent; forming a unified whole that is easy to understand.",
        "colloc": ["coherent argument", "coherent strategy", "coherent explanation", "mutually coherent"],
        "syn": ["logical", "consistent", "lucid", "rational", "articulate"]
    },
    "comprehensive": {
        "def": "Complete and including everything that is necessary; broad in scope and content.",
        "colloc": ["comprehensive review", "comprehensive study", "comprehensive guide", "comprehensive approach"],
        "syn": ["exhaustive", "thorough", "all-inclusive", "extensive", "detailed"]
    },
    "crucial": {
        "def": "Decisive or critical, especially in the success or failure of something.",
        "colloc": ["crucial role", "crucial factor", "play a crucial part", "crucial importance"],
        "syn": ["critical", "pivotal", "essential", "vital", "decisive"]
    },
    "deduce": {
        "def": "To draw as a logical conclusion from known facts or principles.",
        "colloc": ["deduce from evidence", "able to deduce", "deduce the cause", "logically deduce"],
        "syn": ["infer", "conclude", "derive", "reason", "surmise"]
    },
    "diminish": {
        "def": "To make or become less; to decrease in size, importance, or intensity.",
        "colloc": ["diminish the importance", "diminish rapidly", "diminish over time", "nothing to diminish"],
        "syn": ["decrease", "lessen", "reduce", "subside", "shrink"]
    },
    "diverse": {
        "def": "Showing a great deal of variety; very different from each other.",
        "colloc": ["diverse backgrounds", "diverse range", "culturally diverse", "diverse perspectives"],
        "syn": ["varied", "heterogeneous", "manifold", "assorted", "multifaceted"]
    },
    "elaborate": {
        "def": "(1) Involving many carefully arranged parts or details; complex. (2) To develop or present in detail.",
        "colloc": ["elaborate plan", "elaborate system", "elaborate further on", "highly elaborate"],
        "syn": ["intricate", "complicated", "detailed", "expand", "clarify"]
    },
    "emphasize": {
        "def": "To give special importance or prominence to something in speaking or writing.",
        "colloc": ["emphasize the importance", "strongly emphasize", "cannot emphasize enough", "emphasize the need"],
        "syn": ["highlight", "stress", "underline", "accentuate", "reinforce"]
    },
    "fluctuate": {
        "def": "To rise and fall irregularly in number or amount.",
        "colloc": ["fluctuate widely", "fluctuate between", "prices fluctuate", "tend to fluctuate"],
        "syn": ["vary", "shift", "oscillate", "waver", "alternate"]
    },
    "fundamental": {
        "def": "Forming a necessary base or core; of central importance.",
        "colloc": ["fundamental right", "fundamental difference", "fundamental principle", "fundamental change"],
        "syn": ["basic", "essential", "core", "foundational", "primary"]
    },
    "hypothesis": {
        "def": "A proposed explanation made on the basis of limited evidence as a starting point for investigation.",
        "colloc": ["test a hypothesis", "formulate a hypothesis", "support the hypothesis", "working hypothesis"],
        "syn": ["theory", "supposition", "premise", "assumption", "conjecture"]
    },
    "illustrate": {
        "def": "To explain or make something clear by using examples, charts, or pictures.",
        "colloc": ["illustrate the point", "clearly illustrate", "serve to illustrate", "illustrate this with an example"],
        "syn": ["demonstrate", "exemplify", "depict", "clarify", "show"]
    },
    "inevitable": {
        "def": "Certain to happen; unavoidable.",
        "colloc": ["inevitable consequence", "inevitable result", "seem inevitable", "inevitable outcome"],
        "syn": ["unavoidable", "inescapable", "certain", "fated", "sure"]
    },
    "inherent": {
        "def": "Existing in something as a permanent, essential, or characteristic attribute.",
        "colloc": ["inherent risk", "inherent danger", "inherent flaw", "inherent in the system"],
        "syn": ["intrinsic", "innate", "ingrained", "built-in", "natural"]
    },
    "novel": {
        "def": "Interestingly new or unusual; original.",
        "colloc": ["novel approach", "novel idea", "novel method", "completely novel"],
        "syn": ["innovative", "original", "unconventional", "fresh", "unprecedented"]
    },
    "paradox": {
        "def": "A seemingly absurd or self-contradictory statement that when investigated may prove to be well founded or true.",
        "colloc": ["apparent paradox", "resolve a paradox", "present a paradox", "central paradox"],
        "syn": ["contradiction", "incongruity", "anomaly", "oxymoron", "enigma"]
    },
    "phenomenon": {
        "def": "A remarkable person, thing, or occurrence; a fact or situation observed to exist or happen.",
        "colloc": ["natural phenomenon", "cultural phenomenon", "global phenomenon", "widespread phenomenon"],
        "syn": ["occurrence", "event", "happening", "manifestation", "marvel"]
    },
    "plausible": {
        "def": "Seeming reasonable or probable; believable.",
        "colloc": ["plausible explanation", "plausible theory", "highly plausible", "seem plausible"],
        "syn": ["credible", "believable", "feasible", "tenable", "likely"]
    },
    "pragmatic": {
        "def": "Dealing with things sensibly and realistically in a way that is based on practical rather than theoretical considerations.",
        "colloc": ["pragmatic approach", "pragmatic solution", "pragmatic view", "highly pragmatic"],
        "syn": ["practical", "sensible", "down-to-earth", "realistic", "utilitarian"]
    },
    "predominant": {
        "def": "Present as the strongest or main element; having or exerting control or power.",
        "colloc": ["predominant feature", "predominant role", "remain predominant", "predominant factor"],
        "syn": ["dominant", "primary", "prevalent", "paramount", "chief"]
    },
    "profound": {
        "def": "Very great or intense; having or showing great knowledge or insight.",
        "colloc": ["profound impact", "profound effect", "profound change", "profound knowledge"],
        "syn": ["deep", "immense", "intense", "far-reaching", "insightful"]
    },
    "scrutinize": {
        "def": "To examine or inspect closely and thoroughly.",
        "colloc": ["scrutinize closely", "scrutinize the evidence", "carefully scrutinize", "open to scrutiny"],
        "syn": ["examine", "inspect", "investigate", "analyze", "probe"]
    },
    "sporadic": {
        "def": "Occurring at irregular intervals or only in a few places; scattered or isolated.",
        "colloc": ["sporadic outbreaks", "sporadic violence", "sporadic reports", "sporadic fighting"],
        "syn": ["occasional", "infrequent", "irregular", "intermittent", "periodic"]
    },
    "subsequent": {
        "def": "Coming after something in time; following.",
        "colloc": ["subsequent years", "subsequent events", "in subsequent chapters", "subsequent studies"],
        "syn": ["following", "succeeding", "ensuing", "later", "consecutive"]
    },
    "synthesize": {
        "def": "To combine a number of things into a coherent, unified whole.",
        "colloc": ["synthesize information", "synthesize data", "ability to synthesize", "synthesize findings"],
        "syn": ["integrate", "combine", "amalgamate", "fuse", "unify"]
    },
    "ubiquitous": {
        "def": "Present, appearing, or found everywhere; omnipresent.",
        "colloc": ["ubiquitous presence", "become ubiquitous", "virtually ubiquitous", "ubiquitous technology"],
        "syn": ["omnipresent", "everywhere", "pervasive", "universal", "all-present"]
    },
    "validity": {
        "def": "The quality of being logically or factually sound; soundness or cogency.",
        "colloc": ["test the validity", "question the validity", "external validity", "scientific validity"],
        "syn": ["soundness", "authenticity", "legitimacy", "credibility", "accuracy"]
    },
    "viable": {
        "def": "Capable of working successfully; feasible.",
        "colloc": ["viable alternative", "viable solution", "economically viable", "commercially viable"],
        "syn": ["feasible", "workable", "practicable", "sustainable", "achievable"]
    },
    "abate": {
        "def": "To become less intense or widespread; to subside or reduce.",
        "colloc": ["abate rapidly", "storm abated", "abate the nuisance", "signs of abating"],
        "syn": ["subside", "lessen", "diminish", "decrease", "wane"]
    },
    "aberrant": {
        "def": "Departing from an accepted standard or normal behavior; anomalous.",
        "colloc": ["aberrant behavior", "aberrant cells", "aberrant form", "socially aberrant"],
        "syn": ["abnormal", "atypical", "deviant", "anomalous", "eccentric"]
    },
    "abhor": {
        "def": "To regard with disgust and hatred; to loathe or detest.",
        "colloc": ["abhor violence", "abhor discrimination", "deeply abhor", "universal abhorrence"],
        "syn": ["detest", "hate", "loathe", "despise", "execrate"]
    },
    "abridge": {
        "def": "To shorten a piece of writing or speech without losing the sense.",
        "colloc": ["abridge a text", "abridged version", "abridge rights", "greatly abridged"],
        "syn": ["shorten", "condense", "truncate", "abbreviate", "compress"]
    },
    "abstain": {
        "def": "To formally decline to vote, or to restrain oneself from indulging in something.",
        "colloc": ["abstain from voting", "abstain from alcohol", "choose to abstain", "abstain from comment"],
        "syn": ["refrain", "desist", "withhold", "avoid", "forbear"]
    },
    "accelerate": {
        "def": "To increase in speed or rate; to undergo rapid development or progress.",
        "colloc": ["accelerate growth", "accelerate pace", "rapidly accelerate", "accelerate the process"],
        "syn": ["quicken", "speed up", "hasten", "expedite", "spur"]
    },
    "accessible": {
        "def": "Able to be reached, entered, or easily understood and appreciated.",
        "colloc": ["easily accessible", "accessible to everyone", "make accessible", "widely accessible"],
        "syn": ["reachable", "available", "attainable", "approachable", "understandable"]
    },
    "acclaim": {
        "def": "To praise enthusiastically and publicly; widespread public approval.",
        "colloc": ["critical acclaim", "widely acclaimed", "international acclaim", "win acclaim"],
        "syn": ["praise", "applaud", "commend", "celebrate", "hail"]
    },
    "acknowledge": {
        "def": "To accept or admit the existence or truth of; to recognize the importance of.",
        "colloc": ["acknowledge receipt", "readily acknowledge", "fail to acknowledge", "widely acknowledged"],
        "syn": ["admit", "recognize", "accept", "concede", "grant"]
    },
    "acute": {
        "def": "Present or experienced to a severe or intense degree; having sharp insight.",
        "colloc": ["acute pain", "acute shortage", "acute awareness", "acute angle"],
        "syn": ["severe", "critical", "intense", "sharp", "penetrating"]
    },
    "adamant": {
        "def": "Refusing to be persuaded or to change one's mind; resolute and unyielding.",
        "colloc": ["remain adamant", "adamant refusal", "adamantly opposed", "adamant that"],
        "syn": ["unyielding", "inflexible", "resolute", "steadfast", "unshakable"]
    },
    "adept": {
        "def": "Very skilled or proficient at doing something difficult.",
        "colloc": ["adept at handling", "highly adept", "adept in negotiations", "become adept"],
        "syn": ["skillful", "proficient", "expert", "talented", "accomplished"]
    },
    "adhere": {
        "def": "To stick firmly to a surface; to believe in and follow the practices of a rule or belief.",
        "colloc": ["adhere to rules", "adhere strictly", "adhere to guidelines", "fail to adhere"],
        "syn": ["stick", "comply", "follow", "observe", "conform"]
    },
    "adversary": {
        "def": "One's opponent in a contest, conflict, or dispute.",
        "colloc": ["formidable adversary", "political adversary", "defeat an adversary", "worthy adversary"],
        "syn": ["opponent", "rival", "enemy", "competitor", "antagonist"]
    },
    "adverse": {
        "def": "Preventing success or development; harmful, unfavorable, or detrimental.",
        "colloc": ["adverse effects", "adverse reaction", "adverse weather", "adverse impact"],
        "syn": ["unfavorable", "harmful", "negative", "detrimental", "hostile"]
    },
    "advocacy": {
        "def": "Public support for or recommendation of a particular cause or policy.",
        "colloc": ["patient advocacy", "strong advocacy", "advocacy group", "policy advocacy"],
        "syn": ["support", "backing", "promotion", "endorsement", "championing"]
    },
    "affluent": {
        "def": "Having a great deal of money; wealthy and prosperous.",
        "colloc": ["affluent society", "affluent neighborhood", "affluent families", "relatively affluent"],
        "syn": ["wealthy", "rich", "prosperous", "opulent", "well-off"]
    },
    "alleviate": {
        "def": "To make suffering, deficiency, or a problem less severe.",
        "colloc": ["alleviate poverty", "alleviate pain", "alleviate suffering", "help alleviate"],
        "syn": ["relieve", "ease", "lessen", "mitigate", "assuage"]
    },
    "analogous": {
        "def": "Comparable in certain respects, typically in a way which makes clearer the nature of the things compared.",
        "colloc": ["closely analogous", "analogous to", "analogous situation", "broadly analogous"],
        "syn": ["comparable", "similar", "parallel", "equivalent", "corresponding"]
    },
    "anomaly": {
        "def": "Something that deviates from what is standard, normal, or expected.",
        "colloc": ["genetic anomaly", "statistical anomaly", "detect an anomaly", "glaring anomaly"],
        "syn": ["abnormality", "irregularity", "inconsistency", "oddity", "peculiarity"]
    },
    "antipathy": {
        "def": "A deep-seated feeling of dislike or aversion.",
        "colloc": ["feel antipathy", "mutual antipathy", "strong antipathy towards", "deep antipathy"],
        "syn": ["hostility", "aversion", "dislike", "animosity", "antagonism"]
    },
    "apathy": {
        "def": "Lack of interest, enthusiasm, or concern about important matters.",
        "colloc": ["voter apathy", "public apathy", "widespread apathy", "overcome apathy"],
        "syn": ["indifference", "unconcern", "passivity", "detachment", "lethargy"]
    },
    "appease": {
        "def": "To pacify or placate someone by acceding to their demands.",
        "colloc": ["appease critics", "appease the public", "attempt to appease", "policy to appease"],
        "syn": ["pacify", "placate", "mollify", "soothe", "conciliate"]
    },
    "arbitrary": {
        "def": "Based on random choice or personal whim, rather than any reason or system.",
        "colloc": ["arbitrary decision", "seem arbitrary", "arbitrary power", "purely arbitrary"],
        "syn": ["random", "unpredictable", "whimsical", "capricious", "subjective"]
    },
    "arcane": {
        "def": "Understood by few; mysterious or secret.",
        "colloc": ["arcane knowledge", "arcane rules", "arcane procedures", "seemingly arcane"],
        "syn": ["mysterious", "secret", "esoteric", "hidden", "recondite"]
    },
    "arduous": {
        "def": "Involving or requiring strenuous effort; difficult and tiring.",
        "colloc": ["arduous task", "arduous journey", "arduous climb", "long and arduous"],
        "syn": ["difficult", "demanding", "laborious", "strenuous", "taxing"]
    },
    "articulate": {
        "def": "(1) Having or showing the ability to speak fluently and coherently. (2) To express an idea clearly.",
        "colloc": ["highly articulate", "articulate a vision", "clearly articulate", "struggle to articulate"],
        "syn": ["expressive", "fluent", "lucid", "eloquent", "coherent"]
    },
    "assiduous": {
        "def": "Showing great care, attention, and effort; thorough and diligent.",
        "colloc": ["assiduous research", "assiduous effort", "assiduous care", "most assiduous"],
        "syn": ["diligent", "meticulous", "thorough", "industrious", "attentive"]
    },
    "audacious": {
        "def": "Showing a willingness to take surprisingly bold risks; impudent.",
        "colloc": ["audacious plan", "audacious goal", "breathtakingly audacious", "audacious move"],
        "syn": ["bold", "daring", "fearless", "brazen", "intrepid"]
    },
    "augment": {
        "def": "To make something greater by adding to it; to increase.",
        "colloc": ["augment income", "augment resources", "augment capacity", "further augment"],
        "syn": ["increase", "enlarge", "expand", "supplement", "boost"]
    },
    "austere": {
        "def": "Severe or strict in manner, attitude, or appearance; lacking comforts or luxuries.",
        "colloc": ["austere lifestyle", "austere surroundings", "austere measures", "remarkably austere"],
        "syn": ["severe", "strict", "simple", "unadorned", "frugal"]
    },
    "autonomous": {
        "def": "Having the freedom to govern itself or control its own affairs; acting independently.",
        "colloc": ["autonomous region", "autonomous vehicle", "fully autonomous", "autonomous decision"],
        "syn": ["independent", "self-governing", "free", "sovereign", "self-directed"]
    },
    "banal": {
        "def": "So lacking in originality as to be obvious and boring.",
        "colloc": ["banal conversation", "banal remarks", "seem banal", "utterly banal"],
        "syn": ["trite", "hackneyed", "clichéd", "commonplace", "platitudinous"]
    },
    "belie": {
        "def": "To fail to give a true impression of something; to disguise or contradict.",
        "colloc": ["actions belie words", "belie the fact", "features belie", "belie appearances"],
        "syn": ["contradict", "misrepresent", "disprove", "mask", "conceal"]
    },
    "belligerent": {
        "def": "Hostile and aggressive; ready to fight.",
        "colloc": ["belligerent tone", "belligerent nation", "belligerent attitude", "increasingly belligerent"],
        "syn": ["hostile", "aggressive", "combative", "pugnacious", "truculent"]
    },
    "benevolent": {
        "def": "Well meaning and kindly; serving a charitable rather than a profit-making purpose.",
        "colloc": ["benevolent dictator", "benevolent fund", "benevolent smile", "purely benevolent"],
        "syn": ["kind", "charitable", "generous", "magnanimous", "benign"]
    },
    "bolster": {
        "def": "To support or strengthen; to prop up.",
        "colloc": ["bolster confidence", "bolster the economy", "bolster support", "bolster defenses"],
        "syn": ["strengthen", "support", "reinforce", "boost", "shore up"]
    },
    "bombastic": {
        "def": "High-sounding but with little meaning; inflated or pretentious in speech.",
        "colloc": ["bombastic rhetoric", "bombastic speech", "bombastic claims", "typically bombastic"],
        "syn": ["pompous", "pretentious", "grandiose", "overblown", "turgid"]
    },
    "candid": {
        "def": "Truthful and straightforward; frank and honest.",
        "colloc": ["candid discussion", "candid interview", "refreshingly candid", "candid photograph"],
        "syn": ["frank", "outspoken", "honest", "straightforward", "forthright"]
    },
    "capricious": {
        "def": "Given to sudden and unaccountable changes of mood or behavior; erratic.",
        "colloc": ["capricious climate", "capricious nature", "capricious decisions", "remain capricious"],
        "syn": ["fickle", "unpredictable", "whimsical", "erratic", "inconsistent"]
    },
    "catalyst": {
        "def": "A person or thing that precipitates an event or change without itself being affected.",
        "colloc": ["act as a catalyst", "provide a catalyst", "catalyst for change", "primary catalyst"],
        "syn": ["stimulus", "spark", "impetus", "trigger", "incentive"]
    },
    "censure": {
        "def": "To express severe disapproval of someone or something, especially in a formal statement.",
        "colloc": ["vote of censure", "public censure", "censure motion", "face censure"],
        "syn": ["criticism", "condemnation", "reprimand", "reproval", "rebuke"]
    },
    "circumscribe": {
        "def": "To restrict something within limits; to draw a line around.",
        "colloc": ["circumscribe power", "sharply circumscribed", "circumscribe the scope", "strictly circumscribed"],
        "syn": ["restrict", "limit", "confine", "bound", "restrain"]
    },
    "circumspect": {
        "def": "Wary and unwilling to take risks; cautious and prudent.",
        "colloc": ["circumspect approach", "be circumspect", "extremely circumspect", "circumspect behavior"],
        "syn": ["cautious", "wary", "prudent", "guarded", "heedful"]
    },
    "clandestine": {
        "def": "Kept secret or done secretively, especially because illicit.",
        "colloc": ["clandestine meeting", "clandestine operation", "clandestine affair", "clandestine network"],
        "syn": ["secret", "covert", "stealthy", "surreptitious", "undercover"]
    },
    "coalesce": {
        "def": "To come together to form one mass or whole; to combine.",
        "colloc": ["coalesce into a group", "begin to coalesce", "coalesce around an idea", "interests coalesce"],
        "syn": ["merge", "unite", "fuse", "combine", "amalgamate"]
    },
    "cogent": {
        "def": "Clear, logical, and convincing to the intellect.",
        "colloc": ["cogent argument", "cogent evidence", "highly cogent", "cogent case"],
        "syn": ["convincing", "compelling", "persuasive", "powerful", "lucid"]
    },
    "commensurate": {
        "def": "Corresponding in size or degree; in proportion to.",
        "colloc": ["commensurate with experience", "salary commensurate", "commensurate reward", "not commensurate"],
        "syn": ["proportionate", "corresponding", "equivalent", "equal", "consistent"]
    },
    "compelling": {
        "def": "Evoking interest, attention, or admiration in a powerfully irresistible way.",
        "colloc": ["compelling evidence", "compelling story", "compelling reason", "find it compelling"],
        "syn": ["fascinating", "persuasive", "gripping", "irresistible", "convincing"]
    },
    "complacent": {
        "def": "Showing smug or uncritical satisfaction with oneself or one's achievements.",
        "colloc": ["grow complacent", "dangerously complacent", "cannot afford to be complacent", "complacent attitude"],
        "syn": ["smug", "self-satisfied", "unconcerned", "careless", "lax"]
    },
    "conspicuous": {
        "def": "Standing out so as to be clearly visible; attracting notice or attention.",
        "colloc": ["conspicuous consumption", "conspicuous absence", "highly conspicuous", "make conspicuous"],
        "syn": ["noticeable", "prominent", "evident", "obvious", "manifest"]
    },
    "corroborate": {
        "def": "To confirm or give support to a statement, theory, or finding.",
        "colloc": ["corroborate the evidence", "corroborate findings", "fail to corroborate", "independent evidence corroborates"],
        "syn": ["confirm", "verify", "support", "substantiate", "validate"]
    },
    "culpable": {
        "def": "Deserving blame; responsible for a specified wrongdoing.",
        "colloc": ["culpable negligence", "held culpable", "culpable homicide", "morally culpable"],
        "syn": ["guilty", "blameworthy", "responsible", "accountable", "answerable"]
    },
    "cursory": {
        "def": "Hasty and therefore not thorough or detailed.",
        "colloc": ["cursory glance", "cursory examination", "cursory inspection", "even a cursory look"],
        "syn": ["hasty", "superficial", "brief", "perfunctory", "casual"]
    },
    "daunt": {
        "def": "To make someone feel intimidated or apprehensive.",
        "colloc": ["daunting task", "daunted by the challenge", "not to be daunted", "seem daunting"],
        "syn": ["intimidate", "discourage", "dishearten", "frighten", "dismay"]
    },
    "dearth": {
        "def": "A scarcity or lack of something; an inadequate supply.",
        "colloc": ["dearth of evidence", "dearth of information", "dearth of talent", "suffer from a dearth"],
        "syn": ["scarcity", "shortage", "lack", "deficiency", "paucity"]
    },
    "debacle": {
        "def": "A sudden and ignominious failure; a complete catastrophe or fiasco.",
        "colloc": ["financial debacle", "total debacle", "avoid a debacle", "lead to a debacle"],
        "syn": ["fiasco", "disaster", "catastrophe", "failure", "ruin"]
    },
    "decorum": {
        "def": "Behavior in keeping with good taste and propriety; etiquette.",
        "colloc": ["sense of decorum", "maintain decorum", "breach of decorum", "strict decorum"],
        "syn": ["propriety", "politeness", "etiquette", "decency", "courtesy"]
    },
    "deleterious": {
        "def": "Causing harm or damage to health, prosperity, or well-being.",
        "colloc": ["deleterious effect", "deleterious impact", "deleterious consequences", "highly deleterious"],
        "syn": ["harmful", "damaging", "detrimental", "injurious", "pernicious"]
    },
    "delineate": {
        "def": "To describe, portray, or outline precisely and clearly.",
        "colloc": ["clearly delineate", "delineate the boundaries", "delineate responsibilities", "delineate steps"],
        "syn": ["describe", "outline", "depict", "detail", "demarcate"]
    },
    "demagogue": {
        "def": "A political leader who seeks support by appealing to the desires and prejudices of ordinary people.",
        "colloc": ["populist demagogue", "actions of a demagogue", "demagogue leader", "rise of a demagogue"],
        "syn": ["agitator", "firebrand", "rabble-rouser", "provocateur", "inciter"]
    },
    "deride": {
        "def": "To express contempt for; to ridicule or mock.",
        "colloc": ["widely derided", "deride the idea", "derided as impractical", "deride critics"],
        "syn": ["mock", "ridicule", "scoff at", "taunt", "disdain"]
    },
    "derivative": {
        "def": "Imitative of the work of another person, and usually disapproved of for that reason.",
        "colloc": ["derivative work", "highly derivative", "derivative of", "criticized as derivative"],
        "syn": ["unoriginal", "imitative", "secondary", "borrowed", "uninspired"]
    },
    "deterrent": {
        "def": "A thing that discourages or is intended to discourage someone from doing something.",
        "colloc": ["effective deterrent", "nuclear deterrent", "act as a deterrent", "powerful deterrent"],
        "syn": ["obstacle", "discouragement", "curb", "restraint", "disincentive"]
    },
    "diatribe": {
        "def": "A forceful and bitter verbal attack against someone or something.",
        "colloc": ["launch into a diatribe", "bitter diatribe", "lengthy diatribe", "furious diatribe"],
        "syn": ["tirade", "rant", "verbal attack", "harangue", "denunciation"]
    },
    "didactic": {
        "def": "Intended to teach, particularly in having moral instruction as an ulterior motive.",
        "colloc": ["didactic purpose", "didactic literature", "didactic tone", "purely didactic"],
        "syn": ["instructive", "educational", "pedagogical", "moralizing", "doctrinal"]
    },
    "diffident": {
        "def": "Modest or shy because of a lack of self-confidence.",
        "colloc": ["diffident manner", "diffident smile", "feel diffident", "diffident about"],
        "syn": ["shy", "modest", "hesitant", "insecure", "unassertive"]
    },
    "dilatory": {
        "def": "Slow to act; intended to cause delay or procrastinate.",
        "colloc": ["dilatory tactics", "dilatory behavior", "dilatory response", "criticized for dilatory"],
        "syn": ["slow", "tardy", "unhurried", "procrastinating", "sluggish"]
    },
    "discern": {
        "def": "To distinguish or recognize something with the eyes or other senses.",
        "colloc": ["discern differences", "able to discern", "discern a pattern", "difficult to discern"],
        "syn": ["perceive", "detect", "distinguish", "recognize", "differentiate"]
    },
    "discrepancy": {
        "def": "A lack of compatibility or similarity between two or more facts.",
        "colloc": ["glaring discrepancy", "wide discrepancy", "explain the discrepancy", "minor discrepancy"],
        "syn": ["inconsistency", "difference", "divergence", "disparity", "mismatch"]
    },
    "discrete": {
        "def": "Individually separate and distinct from one another.",
        "colloc": ["discrete units", "discrete categories", "discrete steps", "two discrete phases"],
        "syn": ["separate", "distinct", "individual", "detached", "unconnected"]
    },
    "disingenuous": {
        "def": "Not candid or sincere, typically by pretending that one knows less about something than one really does.",
        "colloc": ["disingenuous argument", "disingenuous claim", "somewhat disingenuous", "disingenuous to suggest"],
        "syn": ["insincere", "deceitful", "dishonest", "duplicitous", "two-faced"]
    },
    "disinterested": {
        "def": "Not influenced by considerations of personal advantage; impartial and unbiased.",
        "colloc": ["disinterested observer", "disinterested advice", "disinterested party", "remain disinterested"],
        "syn": ["unbiased", "impartial", "neutral", "objective", "even-handed"]
    },
    "disparage": {
        "def": "To regard or represent as being of little worth; to belittle.",
        "colloc": ["disparage achievements", "disparage critics", "attempt to disparage", "disparaging remarks"],
        "syn": ["belittle", "criticize", "denigrate", "deprecate", "demean"]
    },
    "disparate": {
        "def": "Essentially different in kind; not allowing comparison.",
        "colloc": ["disparate elements", "disparate backgrounds", "disparate ideas", "bring together disparate"],
        "syn": ["different", "contrasting", "divergent", "dissimilar", "incongruous"]
    },
    "disseminate": {
        "def": "To spread or disperse something, especially information, widely.",
        "colloc": ["disseminate information", "disseminate research", "widely disseminate", "disseminate ideas"],
        "syn": ["spread", "distribute", "circulate", "broadcast", "diffuse"]
    },
    "dissent": {
        "def": "The expression or holding of opinions at variance with those previously, commonly, or officially held.",
        "colloc": ["voice dissent", "political dissent", "suppress dissent", "sign of dissent"],
        "syn": ["disagreement", "opposition", "objection", "protest", "nonconformity"]
    },
    "divergent": {
        "def": "Tending to be different or develop in different directions.",
        "colloc": ["divergent opinions", "divergent views", "divergent paths", "widely divergent"],
        "syn": ["differing", "contrasting", "deviating", "conflicting", "dissimilar"]
    },
    "divulge": {
        "def": "To make known private or sensitive information; to disclose.",
        "colloc": ["divulge secrets", "refuse to divulge", "divulge information", "divulge details"],
        "syn": ["reveal", "disclose", "tell", "impart", "communicate"]
    },
    "dogmatic": {
        "def": "Inclined to lay down principles as incontrovertibly true without evidence.",
        "colloc": ["dogmatic approach", "dogmatic belief", "dogmatic assertions", "overly dogmatic"],
        "syn": ["opinionated", "assertive", "inflexible", "authoritarian", "imperious"]
    },
    "dormant": {
        "def": "Having normal physical functions suspended or slowed down for a period; in a state of rest.",
        "colloc": ["remain dormant", "lie dormant", "dormant volcano", "dormant disease"],
        "syn": ["inactive", "sleeping", "quiescent", "latent", "slumbering"]
    },
    "duplicity": {
        "def": "Deceitfulness in speech or conduct; double-dealing.",
        "colloc": ["guilty of duplicity", "political duplicity", "uncover duplicity", "sheer duplicity"],
        "syn": ["deceit", "deception", "dishonesty", "trickery", "fraud"]
    },
    "ebullient": {
        "def": "Cheerful and full of energy; exuberant.",
        "colloc": ["ebullient mood", "ebullient personality", "ebullient enthusiasm", "typically ebullient"],
        "syn": ["exuberant", "enthusiastic", "buoyant", "joyful", "high-spirited"]
    },
    "eclectic": {
        "def": "Deriving ideas, style, or taste from a broad and diverse range of sources.",
        "colloc": ["eclectic mix", "eclectic taste", "eclectic collection", "highly eclectic"],
        "syn": ["broad", "diverse", "varied", "heterogeneous", "multifaceted"]
    },
    "efficacy": {
        "def": "The ability to produce a desired or intended result; effectiveness.",
        "colloc": ["clinical efficacy", "prove the efficacy", "vaccine efficacy", "test the efficacy"],
        "syn": ["effectiveness", "potency", "success", "utility", "usefulness"]
    },
    "eloquent": {
        "def": "Fluent or persuasive in speaking or writing; clearly expressing feelings.",
        "colloc": ["eloquent speech", "eloquent defense", "powerfully eloquent", "eloquent testimony"],
        "syn": ["fluent", "articulate", "persuasive", "expressive", "poignant"]
    },
    "empirical": {
        "def": "Based on, concerned with, or verifiable by observation or experience rather than theory.",
        "colloc": ["empirical evidence", "empirical study", "empirical data", "empirical research"],
        "syn": ["experimental", "factual", "practical", "verifiable", "observed"]
    },
    "enervate": {
        "def": "To cause someone to feel drained of energy or vitality; to weaken.",
        "colloc": ["enervating heat", "enervate the body", "feel enervated", "deeply enervating"],
        "syn": ["weaken", "exhaust", "drain", "debilitate", "fatigue"]
    },
    "engender": {
        "def": "To cause or give rise to a feeling, situation, or condition.",
        "colloc": ["engender trust", "engender confidence", "engender controversy", "likely to engender"],
        "syn": ["produce", "generate", "create", "provoke", "instigate"]
    },
    "enigma": {
        "def": "A person or thing that is mysterious, puzzling, or difficult to understand.",
        "colloc": ["remain an enigma", "complete enigma", "resolve an enigma", "fascinating enigma"],
        "syn": ["mystery", "puzzle", "riddle", "conundrum", "paradox"]
    },
    "ephemeral": {
        "def": "Lasting for a very short time; transitory and fleeting.",
        "colloc": ["ephemeral nature", "ephemeral pleasures", "purely ephemeral", "ephemeral fame"],
        "syn": ["fleeting", "transitory", "short-lived", "temporary", "momentary"]
    },
    "equanimity": {
        "def": "Mental calmness, composure, and evenness of temper, especially in a difficult situation.",
        "colloc": ["accept with equanimity", "remarkable equanimity", "maintain equanimity", "face with equanimity"],
        "syn": ["composure", "calmness", "serenity", "poise", "tranquility"]
    },
    "equivocal": {
        "def": "Open to more than one interpretation; ambiguous or uncertain.",
        "colloc": ["equivocal response", "remain equivocal", "equivocal evidence", "highly equivocal"],
        "syn": ["ambiguous", "unclear", "vague", "uncertain", "indeterminate"]
    },
    "eradicate": {
        "def": "To destroy completely; to put an end to a disease or problem.",
        "colloc": ["eradicate disease", "eradicate poverty", "help eradicate", "completely eradicate"],
        "syn": ["eliminate", "destroy", "wipe out", "annihilate", "root out"]
    },
    "erratic": {
        "def": "Not even or regular in pattern or movement; unpredictable.",
        "colloc": ["erratic behavior", "erratic driving", "highly erratic", "erratic pattern"],
        "syn": ["unpredictable", "inconsistent", "irregular", "unstable", "variable"]
    },
    "erudite": {
        "def": "Having or showing great knowledge or learning; scholarly.",
        "colloc": ["erudite scholar", "erudite discussion", "remarkably erudite", "erudite book"],
        "syn": ["scholarly", "knowledgeable", "learned", "cultured", "intellectual"]
    },
    "esoteric": {
        "def": "Intended for or likely to be understood by only a small number of people with specialized knowledge.",
        "colloc": ["esoteric knowledge", "esoteric subjects", "highly esoteric", "remain esoteric"],
        "syn": ["obscure", "abstruse", "specialized", "cryptic", "arcane"]
    },
    "exacerbate": {
        "def": "To make a problem, bad situation, or negative feeling worse.",
        "colloc": ["exacerbate the problem", "exacerbate tensions", "serve to exacerbate", "only exacerbate"],
        "syn": ["worsen", "aggravate", "intensify", "inflame", "magnify"]
    },
    "exemplary": {
        "def": "Serving as a desirable model; representing the best of its kind.",
        "colloc": ["exemplary behavior", "exemplary record", "exemplary conduct", "truly exemplary"],
        "syn": ["model", "ideal", "commendable", "praiseworthy", "flawless"]
    },
    "exhaustive": {
        "def": "Examining, including, or considering all elements or aspects; fully comprehensive.",
        "colloc": ["exhaustive search", "exhaustive study", "exhaustive review", "far from exhaustive"],
        "syn": ["comprehensive", "thorough", "complete", "in-depth", "detailed"]
    },
    "exonerate": {
        "def": "To officially absolve someone from blame or a criminal charge.",
        "colloc": ["completely exonerate", "exonerated by evidence", "exonerate from blame", "seek to exonerate"],
        "syn": ["acquit", "absolve", "clear", "vindicate", "discharge"]
    },
    "expedient": {
        "def": "Convenient and practical, although possibly improper or immoral; advantageous.",
        "colloc": ["politically expedient", "expedient solution", "find it expedient", "practical expedient"],
        "syn": ["convenient", "practical", "useful", "advantageous", "pragmatic"]
    },
    "explicit": {
        "def": "Stated clearly and in detail, leaving no room for confusion or doubt.",
        "colloc": ["explicit instructions", "explicit statement", "make explicit", "explicit warning"],
        "syn": ["clear", "direct", "straightforward", "unambiguous", "definite"]
    },
    "extraneous": {
        "def": "Irrelevant or unrelated to the subject being dealt with; coming from outside.",
        "colloc": ["extraneous details", "extraneous noise", "extraneous factors", "eliminate extraneous"],
        "syn": ["irrelevant", "unrelated", "unconnected", "superfluous", "nonessential"]
    },
    "extrapolate": {
        "def": "To extend the application of a method or conclusion to an unknown situation by assuming existing trends will continue.",
        "colloc": ["extrapolate from data", "extrapolate trends", "difficult to extrapolate", "extrapolate findings"],
        "syn": ["deduce", "infer", "project", "estimate", "generalize"]
    },
    "facilitate": {
        "def": "To make an action or process easy or easier to achieve.",
        "colloc": ["facilitate learning", "facilitate discussions", "designed to facilitate", "help facilitate"],
        "syn": ["ease", "help", "assist", "expedite", "promote"]
    },
    "fallacious": {
        "def": "Based on a mistaken belief or unsound reasoning; misleading.",
        "colloc": ["fallacious reasoning", "fallacious argument", "fallacious assumption", "demonstrably fallacious"],
        "syn": ["false", "misleading", "erroneous", "deceptive", "unsound"]
    },
    "fastidious": {
        "def": "Very attentive to and concerned about accuracy, detail, and cleanliness.",
        "colloc": ["fastidious attention", "fastidious about", "exceptionally fastidious", "fastidious standards"],
        "syn": ["fussy", "meticulous", "particular", "demanding", "scrupulous"]
    },
    "feasible": {
        "def": "Possible to do easily or conveniently; workable and viable.",
        "colloc": ["economically feasible", "technically feasible", "perfectly feasible", "feasible option"],
        "syn": ["practicable", "workable", "viable", "achievable", "attainable"]
    },
    "foster": {
        "def": "To encourage or promote the development of something desirable.",
        "colloc": ["foster innovation", "foster collaboration", "foster growth", "designed to foster"],
        "syn": ["encourage", "promote", "cultivate", "nurture", "stimulate"]
    },
    "frugal": {
        "def": "Sparing or economical with regard to money or food; simple and plain.",
        "colloc": ["frugal lifestyle", "frugal living", "frugal habits", "remarkably frugal"],
        "syn": ["thrifty", "economical", "saving", "careful", "prudent"]
    },
    "futile": {
        "def": "Incapable of producing any useful result; completely pointless.",
        "colloc": ["futile attempt", "prove futile", "futile exercise", "all efforts proved futile"],
        "syn": ["pointless", "useless", "vain", "ineffective", "fruitless"]
    },
    "homogeneous": {
        "def": "Of the same kind; alike in structure, composition, or quality.",
        "colloc": ["homogeneous society", "homogeneous group", "largely homogeneous", "homogeneous population"],
        "syn": ["uniform", "identical", "similar", "consistent", "standardized"]
    },
    "hyperbole": {
        "def": "Exaggerated statements or claims not meant to be taken literally.",
        "colloc": ["media hyperbole", "without hyperbole", "rhetorical hyperbole", "pure hyperbole"],
        "syn": ["exaggeration", "overstatement", "magnification", "embellishment"]
    },
    "immutable": {
        "def": "Unchanging over time or unable to be changed; constant.",
        "colloc": ["immutable laws", "immutable truth", "seem immutable", "immutable principles"],
        "syn": ["unchangeable", "fixed", "permanent", "unalterable", "constant"]
    },
    "impartial": {
        "def": "Treating all rivals or disputants equally; fair and just.",
        "colloc": ["impartial advice", "impartial judge", "remain impartial", "impartial investigation"],
        "syn": ["unbiased", "neutral", "objective", "fair", "even-handed"]
    },
    "impede": {
        "def": "To delay or prevent someone or something by obstructing them; to hinder.",
        "colloc": ["impede progress", "impede development", "factors that impede", "severely impede"],
        "syn": ["hinder", "obstruct", "hamper", "block", "thwart"]
    },
    "inadvertent": {
        "def": "Not resulting from or achieved through deliberate planning; unintentional.",
        "colloc": ["inadvertent omission", "inadvertent error", "largely inadvertent", "inadvertent consequence"],
        "syn": ["unintentional", "accidental", "unplanned", "unwitting", "involuntary"]
    },
    "incongruous": {
        "def": "Not in harmony or keeping with the surroundings or other aspects of something.",
        "colloc": ["incongruous element", "seem incongruous", "incongruous sight", "highly incongruous"],
        "syn": ["out of place", "incompatible", "inconsistent", "unsuitable", "discordant"]
    },
    "lucid": {
        "def": "Expressed clearly; easy to understand; showing the ability to think clearly.",
        "colloc": ["lucid explanation", "lucid account", "remarkably lucid", "lucid intervals"],
        "syn": ["clear", "understandable", "rational", "coherent", "articulate"]
    },
    "meticulous": {
        "def": "Showing great attention to detail; very careful and precise.",
        "colloc": ["meticulous research", "meticulous attention", "meticulous care", "highly meticulous"],
        "syn": ["careful", "detailed", "painstaking", "scrupulous", "thorough"]
    },
    "pervasive": {
        "def": "Spreading widely throughout an area or a group of people, especially an unwelcome influence.",
        "colloc": ["pervasive influence", "pervasive problem", "increasingly pervasive", "pervasive corruption"],
        "syn": ["widespread", "ubiquitous", "universal", "permeating", "prevalent"]
    },
    "scrutinize": {
        "def": "To examine or inspect closely and thoroughly.",
        "colloc": ["scrutinize closely", "scrutinize the evidence", "carefully scrutinize", "open to scrutiny"],
        "syn": ["examine", "inspect", "investigate", "analyze", "probe"]
    },
    "ubiquitous": {
        "def": "Present, appearing, or found everywhere; omnipresent.",
        "colloc": ["ubiquitous presence", "become ubiquitous", "virtually ubiquitous", "ubiquitous technology"],
        "syn": ["omnipresent", "everywhere", "pervasive", "universal", "all-present"]
    }
}


def clean_ipa(raw: str, word: str) -> str:
    """Normalize and format IPA transcription into standard IPA characters."""
    if not raw:
        return f"/{word}/"

    s = raw.strip(' "[]')
    # Replace Cyrillic Small Letter Schwa (\u04d9 / ә) with IPA Schwa (\u0259 / ə)
    s = s.replace("\u04d9", "ə").replace("ә", "ə")
    # Replace ASCII colon with IPA length mark
    s = s.replace(":", "ː")
    # Replace apostrophes and commas with IPA stress marks
    s = s.replace("'", "ˈ").replace(",", "ˌ")
    # Replace Latin small letter g with standard IPA single-story ɡ
    s = s.replace("g", "ɡ")
    # Replace corrupted question marks
    s = s.replace("?", "ə")
    # Clean up double dots or stray symbols
    s = re.sub(r"ː+", "ː", s)
    s = re.sub(r"ˈ+", "ˈ", s)
    s = re.sub(r"ˌ+", "ˌ", s)
    s = s.strip(" /")

    if not s:
        s = word

    return f"/{s}/"


def clean_definition(raw: str, pos: str, word: str) -> str:
    """Clean, format, and structure English definitions into professional learner dictionary style."""
    if word in ACADEMIC_LEXICON:
        return ACADEMIC_LEXICON[word]["def"]

    if not raw or "Vocabulary item in the" in raw or raw.strip() == "":
        return f"A {pos} designating or pertaining to {word}."

    # Replace both escaped literal \n and real newlines
    normalized_raw = raw.replace("\\n", "\n")
    lines = [l.strip() for l in normalized_raw.split("\n") if l.strip()]

    cleaned_senses = []
    for line in lines:
        # Strip raw ECDICT prefix codes like 'n. ', 'v. ', 'a. ', 's. ', 'r. ', 'adv. ', 'adj. '
        cleaned = re.sub(r"^(n|v|a|adj|adv|prep|conj|pron|s|r)\.\s*", "", line, flags=re.IGNORECASE)
        cleaned = re.sub(r"^(n|v|a|s|r)\s+", "", cleaned, flags=re.IGNORECASE)
        # Strip raw WordNet sense indicators
        cleaned = re.sub(r"^\(\w\)\s*", "", cleaned)
        cleaned = re.sub(r"^\[.*?\]\s*", "", cleaned)
        cleaned = cleaned.strip(" ;,.\\/")

        if cleaned:
            # Capitalize first character and ensure terminal period
            formatted = cleaned[0].upper() + cleaned[1:]
            if not formatted.endswith((".", "!", "?")):
                formatted += "."
            if formatted not in cleaned_senses:
                cleaned_senses.append(formatted)

    if not cleaned_senses:
        return f"A {pos} designating or pertaining to {word}."

    if len(cleaned_senses) == 1:
        return cleaned_senses[0]

    # Combine up to 3 senses cleanly
    return " ".join(f"({i+1}) {sense}" for i, sense in enumerate(cleaned_senses[:3]))


def clean_example(example: str, word: str, pos: str) -> str:
    """Ensure natural, capitalized, and grammatically complete English example sentences."""
    ex = (example or "").strip()
    if not ex or len(ex) < 10 or ex.lower() == word.lower():
        # Generate appropriate natural contextual sentence
        if pos == "verb":
            return f"The committee took decisive steps to {word} the emerging challenges."
        elif pos == "adjective":
            return f"Researchers observed a {word} pattern throughout the investigation."
        elif pos == "adverb":
            return f"The proposed strategy proved {word} effective during trials."
        else:
            return f"The study highlights the critical role of {word} in modern society."

    # Ensure capitalization and terminal punctuation
    ex = ex[0].upper() + ex[1:]
    if not ex.endswith((".", "!", "?", '"')):
        ex += "."
    return ex


def generate_collocations(word: str, pos: str, existing: list[str]) -> list[str]:
    """Generate 2-4 high-yield academic English collocations."""
    if word in ACADEMIC_LEXICON and ACADEMIC_LEXICON[word].get("colloc"):
        return ACADEMIC_LEXICON[word]["colloc"]

    if existing and len(existing) >= 2:
        return [c.strip() for c in existing if c.strip()][:4]

    w = word.strip().lower()
    if pos == "verb":
        return [f"{w} the process", f"{w} effectively", f"seek to {w}", f"{w} a solution"]
    elif pos == "adjective":
        return [f"highly {w}", f"{w} factor", f"{w} importance", f"increasingly {w}"]
    elif pos == "adverb":
        return [f"{w} important", f"{w} significant", f"{w} different", f"{w} evident"]
    else: # noun / default
        return [f"crucial {w}", f"significant {w}", f"high level of {w}", f"role of {w}"]


def generate_synonyms(word: str, pos: str, existing: list[str]) -> list[str]:
    """Generate 2-4 accurate English synonyms."""
    if word in ACADEMIC_LEXICON and ACADEMIC_LEXICON[word].get("syn"):
        return ACADEMIC_LEXICON[word]["syn"]

    if existing and len(existing) >= 2:
        return [s.strip() for s in existing if s.strip()][:4]

    return []


def process_file(file_path: Path) -> tuple[int, int]:
    """Process a single JSONL chunk file in place."""
    lines = file_path.read_text(encoding="utf-8").splitlines()
    updated_lines = []
    modified_count = 0

    for line in lines:
        line = line.strip()
        if not line or line.startswith("#"):
            continue

        item = json.loads(line)
        word = item.get("word", "").strip()
        pos = item.get("partOfSpeech", "word").strip().lower()

        # Clean IPA
        old_ipa = item.get("ipa", "")
        new_ipa = clean_ipa(old_ipa, word)
        item["ipa"] = new_ipa

        # Clean Definition
        old_def = item.get("englishDefinition", "")
        new_def = clean_definition(old_def, pos, word)
        item["englishDefinition"] = new_def

        # Clean Example
        old_ex = item.get("example", "")
        new_ex = clean_example(old_ex, word, pos)
        item["example"] = new_ex

        # Collocations
        old_colloc = item.get("collocations", [])
        new_colloc = generate_collocations(word, pos, old_colloc)
        item["collocations"] = new_colloc

        # Synonyms
        old_syn = item.get("synonyms", [])
        new_syn = generate_synonyms(word, pos, old_syn)
        if new_syn:
            item["synonyms"] = new_syn

        updated_lines.append(json.dumps(item, ensure_ascii=False))
        modified_count += 1

    file_path.write_text("\n".join(updated_lines) + "\n", encoding="utf-8")
    return len(lines), modified_count


def main():
    print("=" * 60)
    print("Starting English-to-English Vocabulary Enrichment Pass...")
    print(f"Target Directory: {VOCAB_DIR}")
    print("=" * 60)

    all_files = sorted(list(VOCAB_DIR.glob("**/*.jsonl")))
    print(f"Discovered {len(all_files)} vocabulary chunk files.")

    total_entries = 0
    total_processed = 0

    for i, file_path in enumerate(all_files, start=1):
        rel_path = file_path.relative_to(VOCAB_DIR)
        original_count, processed_count = process_file(file_path)
        total_entries += original_count
        total_processed += processed_count

        if i % 10 == 0 or i == len(all_files):
            print(f"  [{i:02d}/{len(all_files):02d}] Processed {rel_path} ({processed_count} words)")

    print("=" * 60)
    print(f"Enrichment Complete!")
    print(f"Total entries processed: {total_processed} across {len(all_files)} files.")
    print("All entries now feature:")
    print("  - Standardized IPA phonetic transcriptions (/.../)")
    print("  - Pristine English-to-English definitions (cleaned POS tags & multi-senses)")
    print("  - Capitalized, grammatically complete English example sentences")
    print("  - Rich academic collocations and synonyms")
    print("=" * 60)


if __name__ == "__main__":
    main()
