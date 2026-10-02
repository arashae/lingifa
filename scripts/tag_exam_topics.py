#!/usr/bin/env python3
"""Tag exam banks with syllabus topics, target band and skill focus.

The bundled IELTS/TOEFL banks carried no topic or band metadata, so the app
could not group cards by syllabus topic, target a band, or focus practice on a
test skill. Only 27 hand-written flashcards had any of this information.

Topics are assigned by scanning each row's own English definition and
collocations against a keyword lexicon. That keeps tagging deterministic,
reproducible and derived from text already in the dataset, rather than from an
external word list that could disagree with the definition shown to the learner.

Target band and skill focus are derived from metadata that already exists:
recalibrated CEFR level, pedagogical stage, and part of speech.
"""

from __future__ import annotations

import json
import re
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
ASSET_ROOT = ROOT / "app" / "src" / "main" / "assets" / "vocabulary"

BANKS = ("ielts", "toefl")

# Keyword lexicon. Each entry is a topic plus the surface forms that signal it in
# a definition or collocation. Matched on word boundaries over lowercase text.
TOPIC_LEXICON: dict[str, tuple[str, ...]] = {
    "Education": ("school", "student", "teacher", "pupil", "university", "college",
                  "curriculum", "classroom", "tuition", "undergraduate", "degree",
                  "exam", "qualification", "literacy", "homework", "lecture", "campus"),
    "Environment": ("environment", "pollution", "emission", "ecosystem", "biodiversity",
                    "conservation", "climate", "carbon", "recycl", "landfill", "habitat",
                    "species", "sustainability", "greenhouse", "contaminat", "renewable"),
    "Health": ("health", "patient", "hospital", "clinic", "disease", "symptom", "doctor",
               "nurse", "therapy", "treatment", "vaccine", "nutrition", "obesity", "wellbeing",
               "mental", "epidemic", "infection", "medicine", "surgery", "illness",
               "malaria", "infectious", "mosquito", "blood", "bone", "muscle", "brain",
               "pregnan", "dental", "anxiety", "depression", "addiction", "allergy",
               "wellness", "dietary", "sedentary"),
    "Technology": ("technology", "software", "digital", "internet", "algorithm", "device",
                   "automation", "computing", "online", "database", "innovation", "robot",
                   "network", "platform", "smartphone", "engineering"),
    "Work": ("employment", "workplace", "employee", "employer", "salary", "wage", "career",
             "recruit", "workforce", "redundan", "promotion", "staff", "profession", "labour",
             "internship", "productivity", "remote work"),
    "Urbanisation": ("urban", "city", "town", "housing", "population", "infrastructure",
                     "commute", "congestion", "residential", "metropolitan", "rural", "village",
                     "suburb", "density"),
    "Globalisation": ("global", "international", "globalisation", "trade", "import", "export",
                      "multinational", "cross-border", "worldwide", "outsourcing", "tariff"),
    "Crime": ("crime", "criminal", "offence", "offender", "prison", "police", "court",
              "guilty", "fraud", "theft", "victim", "justice", "legal", "law", "sentence",
              "deterrent", "smuggling", "punishment"),
    "Media & Advertising": ("media", "advertis", "broadcast", "journalis", "newspaper",
                            "audience", "coverage", "campaign", "sponsor", "publish",
                            "subscriber", "press", "marketing"),
    "Family & Society": ("family", "parent", "child", "household", "community", "generation",
                         "marriage", "gender", "social", "demographic", "ageing", "elderly",
                         "youth", "neighbour"),
    "Government": ("government", "policy", "regulat", "legislat", "council", "authority",
                   "public sector", "minister", "parliament", "tax", "law", "official"),
    "Economics": ("economy", "economic", "inflation", "market", "price", "cost", "investment",
                  "growth", "income", "poverty", "financial", "budget", "consumer",
                  "unemployment", "trade"),
    "Science": ("research", "study", "experiment", "hypothesis", "data", "sample",
                "laboratory", "scientific", "evidence", "theory", "analysis", "earth",
                "planet", "gravity", "physics", "chemistry", "force", "motion", "atom",
                "molecule", "universe", "cell", "genetic", "biological", "measurement",
                "observation", "variable"),
    "Culture": ("culture", "cultural", "tradition", "heritage", "society", "custom",
                "identity", "art", "music", "language", "dialect", "historical"),
    "Energy": ("energy", "fuel", "power", "electric", "solar", "oil", "gas", "coal",
               "battery", "grid", "nuclear"),
    "Transport": ("transport", "vehicle", "traffic", "road", "rail", "airport", "commute",
                  "cycle", "pedestrian", "journey", "freight"),
    "Tourism": ("tourism", "tourist", "destination", "holiday", "travel", "visitor",
                "hospitality", "sightseeing"),
    "Food & Agriculture": ("food", "agricultur", "farm", "crop", "livestock", "diet",
                           "harvest", "nutrition", "soil", "irrigation", "fishing"),
}

# CEFR level -> the IELTS band a learner typically needs it for.
BAND_BY_CEFR = {"A1": "6.0", "A2": "6.0", "B1": "6.5", "B2": "7.0",
                "C1": "8.0", "C2": "8.0+"}

# Pedagogical stage -> band floor. Stage 1 is the learner's entry point, so a
# B2 card encountered first is still a 7.0 target.
BAND_FLOOR = {1: "6.5", 2: "7.0", 3: "7.5", 4: "8.0"}
BAND_ORDER = ["6.0", "6.5", "7.0", "7.5", "8.0", "8.0+"]

# Skill focus, keyed by part of speech. Function-like expressions are what a
# learner reaches for when speaking, so they are routed to the speaking tasks.
SKILL_BY_POS = {
    "phrasal verb": ["Speaking", "Writing"],
    "idiom": ["Speaking", "Writing"],
    "phrase": ["Speaking", "Writing"],
    "prepositional phrase": ["Speaking", "Writing"],
    "verb": ["Writing", "Speaking"],
    "adjective": ["Reading Academic", "Writing"],
    "noun": ["Reading Academic", "Listening Lecture"],
    "adverb": ["Writing", "Listening Lecture"],
}

GENERAL_SKILLS = ["Listening Conversation", "Speaking"]

# Every card is given a topic so a topic filter never shows a blank group. A
# word whose definition names no syllabus field is academic vocabulary rather
# than topical vocabulary, and that is worth saying explicitly.
FALLBACK_TOPIC = "General Academic"


def norm_text(row: dict) -> str:
    parts = [str(row.get("englishDefinition", "")), str(row.get("example", ""))]
    parts.extend(str(value) for value in row.get("collocations", []) or [])
    parts.extend(str(value) for value in row.get("synonyms", []) or [])
    return " ".join(parts).lower()


def topics_for(row: dict) -> list[str]:
    text = norm_text(row)
    scored: list[tuple[int, str]] = []
    for topic, keywords in TOPIC_LEXICON.items():
        hits = sum(1 for keyword in keywords if re.search(rf"\b{re.escape(keyword)}", text))
        if hits:
            scored.append((hits, topic))
    # Stable ordering: strongest evidence first, then alphabetical for ties.
    scored.sort(key=lambda pair: (-pair[0], pair[1]))
    if not scored:
        return [FALLBACK_TOPIC]
    return [topic for _, topic in scored[:3]]


def band_for(row: dict) -> str:
    from_cefr = BAND_BY_CEFR.get(str(row.get("cefrLevel", "")).upper(), "7.0")
    try:
        stage = int(row.get("stageNumber") or 0)
    except (TypeError, ValueError):
        stage = 0
    floor = BAND_FLOOR.get(stage)
    if not floor:
        return from_cefr
    return from_cefr if BAND_ORDER.index(from_cefr) >= BAND_ORDER.index(floor) else floor


def skills_for(row: dict, tagged: list[str]) -> list[str]:
    pos = str(row.get("partOfSpeech", "")).strip().lower()
    skills = list(SKILL_BY_POS.get(pos, ["Reading Academic"]))
    # A card tied to a concrete syllabus topic is also worth meeting in
    # conversation, which is how the speaking test actually works. The fallback
    # topic is not concrete, so it does not earn speaking practice.
    if tagged and FALLBACK_TOPIC not in tagged and pos in {"noun", "adjective"} \
            and "Speaking" not in skills:
        skills.append("Speaking")
    if FALLBACK_TOPIC in tagged and pos in {"noun", "adverb"}:
        skills.extend(s for s in GENERAL_SKILLS if s not in skills)
    return skills[:3]


def process(bank: str, report: dict) -> None:
    files = sorted((ASSET_ROOT / bank).glob("*.jsonl"))
    topic_counter: dict[str, int] = {}
    for path in files:
        lines = path.read_text(encoding="utf-8").splitlines()
        output: list[str] = []
        for raw in lines:
            line = raw.strip()
            if not line or line.startswith("#"):
                output.append(raw)
                continue
            row = json.loads(line)
            curated = "curated-multiword" in {str(t) for t in row.get("tags", [])}
            if curated and row.get("examTopics"):
                # Hand-authored taxonomy beats keyword inference for curated rows.
                tagged = [str(t) for t in row["examTopics"]]
                band = str(row.get("targetBand") or "").strip() or band_for(row)
            else:
                tagged = topics_for(row)
                band = band_for(row)
            row["examTopics"] = tagged
            row["targetBand"] = band
            row["skillFocus"] = skills_for(row, tagged)
            for topic in tagged:
                topic_counter[topic] = topic_counter.get(topic, 0) + 1
            # Keep the flat tag list in step so existing tag-based filtering and
            # search see the topic without knowing about the new field.
            existing = {str(t) for t in row.get("tags", [])}
            for topic in tagged:
                slug = topic.lower().replace(" & ", "-").replace(" ", "-")
                existing.add(slug)
            row["tags"] = sorted(existing)
            output.append(json.dumps(row, ensure_ascii=False, separators=(",", ":")))
        path.write_text("\n".join(output) + "\n", encoding="utf-8")
    report[bank] = topic_counter


def main() -> None:
    report: dict[str, dict[str, int]] = {}
    for bank in BANKS:
        process(bank, report)
        total = sum(report[bank].values())
        print(f"{bank.upper()}: tagged {total:,} topic assignment(s) across "
              f"{len(report[bank])} distinct topics")
    top = sorted(report.get("ielts", {}).items(), key=lambda kv: -kv[1])[:6]
    print(f"  leading IELTS topics: {top}")


if __name__ == "__main__":
    main()
