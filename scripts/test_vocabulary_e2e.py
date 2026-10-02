#!/usr/bin/env python3
"""Automated E2E Test Suite for LinguaFa Vocabulary Datasets & Quality Validation Gate.

Multi-tier testing architecture:
  Tier 1: Feature Coverage (schema validity, mandatory fields, POS whitelist, valid CEFR, catalog integrity)
  Tier 2: Boundary & Corner Cases (empty/whitespace handling, Hangul/Hanzi, replacement chars, Urdu glyphs, ZWNJ, range parsing)
  Tier 3: Cross-Feature Combinations (POS vs definition sense alignment, CEFR vs exam priority sanity, proper noun downranking, duplicate detection)
  Tier 4: Real-World Application Scenarios (learner flashcard usability, example lemma presence with inflections, faithful Persian translations, E2E CLI execution)
"""

from __future__ import annotations

import json
import re
import subprocess
import sys
import unittest
from pathlib import Path

# Ensure UTF-8 stdout on Windows
if hasattr(sys.stdout, "reconfigure"):
    sys.stdout.reconfigure(encoding="utf-8")
if hasattr(sys.stderr, "reconfigure"):
    sys.stderr.reconfigure(encoding="utf-8")

REPO_ROOT = Path(__file__).resolve().parents[1]
SCRIPTS_DIR = REPO_ROOT / "scripts"
if str(SCRIPTS_DIR) not in sys.path:
    sys.path.insert(0, str(SCRIPTS_DIR))

from sanitize_vocabulary_assets import looks_like_c2_proper_noun

from validate_vocabulary_quality import (
    POS_OK,
    collocation_target_risk,
    REPORT_PATH,
    RISKY_SENSES,
    URDU_NON_PERSIAN_GLYPHS,
    VALID_CEFR,
    VOCAB_ROOT,
    definition_meaning_alignment_risk,
    detect_script_or_encoding_defect,
    example_translation_alignment_risk,
    generate_inflections,
    is_generated_family,
    metadata_outlier_risks,
    parse_args,
    placeholder_definition_risk,
    priority_sense_review_risk,
    parse_range,
    resolve_file,
    run_validation,
    target_present,
)


class Tier1FeatureCoverageTests(unittest.TestCase):
    """Tier 1: Feature Coverage (schema validity, mandatory fields, POS whitelist, valid CEFR, catalog)."""

    def setUp(self) -> None:
        self.sample_valid_card = {
            "id": "gen_001_001",
            "word": "abandon",
            "partOfSpeech": "verb",
            "cefrLevel": "B2",
            "persianMeaning": "ترک کردن، رها کردن",
            "englishDefinition": "To leave someone or something behind completely.",
            "example": "He had to abandon the car in the snow.",
            "examplePersian": "او مجبور شد ماشین را در برف رها کند.",
            "ipa": "/əˈbændən/",
            "collocations": ["abandon hope", "abandon ship"],
            "tags": ["core-vocabulary"],
            "source": "NGSL",
            "sourceLicense": "CC-BY",
            "frequencyRank": 1200,
            "examPriority": 1,
            "learningOrder": 1200,
            "difficultyScore": 4.5,
            "register": "neutral",
            "synonyms": ["desert", "leave"],
            "antonyms": ["keep", "maintain"],
            "morphology": {"past": "abandoned", "participle": "abandoned"},
            "etymology": "From Old French abandoner",
        }

    def test_schema_validity_mandatory_keys(self) -> None:
        """Verify that card schema contains all mandatory keys and correct data types."""
        card = self.sample_valid_card
        self.assertIsInstance(card["word"], str)
        self.assertIsInstance(card["partOfSpeech"], str)
        self.assertIsInstance(card["cefrLevel"], str)
        self.assertIsInstance(card["persianMeaning"], str)
        self.assertIsInstance(card["englishDefinition"], str)
        self.assertIsInstance(card["example"], str)
        self.assertIsInstance(card["examplePersian"], str)
        self.assertIsInstance(card["collocations"], list)
        self.assertIsInstance(card["tags"], list)
        self.assertIsInstance(card["frequencyRank"], int)

    def test_part_of_speech_in_whitelist(self) -> None:
        """Verify all standard POS values belong to POS_OK whitelist and invalid ones are detected."""
        for valid_pos in ("noun", "verb", "adjective", "adverb", "pronoun", "determiner",
                          "conjunction", "preposition", "modal verb", "article", "interjection"):
            self.assertIn(valid_pos, POS_OK)

        for invalid_pos in ("undefined", "word", "unknown", "invalid_pos", ""):
            self.assertNotIn(invalid_pos, POS_OK)

    def test_cefr_levels_whitelist(self) -> None:
        """Verify CEFR scale strictly permits A1, A2, B1, B2, C1, C2 and rejects invalid codes."""
        for level in ("A1", "A2", "B1", "B2", "C1", "C2"):
            self.assertIn(level, VALID_CEFR)

        for invalid_level in ("A0", "B3", "C3", "D1", "C2+", ""):
            self.assertNotIn(invalid_level, VALID_CEFR)

    def test_master_catalog_consistency(self) -> None:
        """Verify master_catalog.json exists and every referenced chunk exists on disk."""
        catalog_path = VOCAB_ROOT / "master_catalog.json"
        self.assertTrue(catalog_path.is_file(), f"Catalog not found: {catalog_path}")

        catalog = json.loads(catalog_path.read_text(encoding="utf-8"))
        self.assertIn("targets", catalog)
        self.assertIn("chunks", catalog)

        targets = catalog["targets"]
        for expected_pack in ("pack_general_core", "pack_ielts_master", "pack_toefl_master", "pack_gre_master"):
            self.assertIn(expected_pack, targets)
            self.assertGreater(targets[expected_pack], 0)

        chunks = catalog["chunks"]
        # Derived rather than hard-coded: growing a bank legitimately adds a
        # chunk. Compared by file name because the catalog is not internally
        # consistent about the path prefix it stores (some entries carry
        # "vocabulary/<bank>/<file>", others only "<file>").
        on_disk = {
            path.name
            for pattern in ("*/*.jsonl", "cefr/*/*.jsonl")
            for path in VOCAB_ROOT.glob(pattern)
        }
        self.assertEqual({Path(c["asset"]).name for c in chunks}, on_disk)
        self.assertEqual(
            {c["asset"] for c in chunks}.__len__(), len(chunks),
            "the catalog must not list the same chunk twice",
        )

        # Check all chunk files exist on disk
        for chunk in chunks:
            asset_rel = chunk["asset"]
            chunk_file = VOCAB_ROOT.parent / asset_rel
            self.assertTrue(chunk_file.is_file(), f"Chunk file missing: {chunk_file}")
            self.assertGreater(chunk.get("expectedItems", 0), 0)

    def test_cli_argument_parsing(self) -> None:
        """Verify CLI argument parser correctly parses --file, --range, --strict, and --report."""
        args = parse_args(["--file", "general_core_001.jsonl", "--range", "1:100", "--strict"])
        self.assertEqual(args.file, "general_core_001.jsonl")
        self.assertEqual(args.range_spec, "1:100")
        self.assertTrue(args.strict)

        # Default run with no arguments
        default_args = parse_args([])
        self.assertIsNone(default_args.file)
        self.assertIsNone(default_args.range_spec)
        self.assertFalse(default_args.strict)
        self.assertIsNone(default_args.report)


class Tier2BoundaryAndCornerCasesTests(unittest.TestCase):
    """Tier 2: Boundary & Corner Cases (empty/whitespace handling, Hangul/Hanzi, replacement chars, Urdu, ZWNJ)."""

    def test_empty_and_whitespace_only_handling(self) -> None:
        """Verify that detect_script_or_encoding_defect handles empty and whitespace strings gracefully."""
        self.assertIsNone(detect_script_or_encoding_defect(""))
        self.assertIsNone(detect_script_or_encoding_defect("   "))
        self.assertIsNone(detect_script_or_encoding_defect("\n\t"))

    def test_hangul_and_cjk_rejection(self) -> None:
        """Verify rejection of Korean Hangul syllables/Jamo and CJK Hanzi ideographs."""
        # Korean Hangul (e.g. Card 352 'bit': '조각')
        self.assertEqual(detect_script_or_encoding_defect("تکه کوچک، 조각"), "Hangul script detected")
        self.assertEqual(detect_script_or_encoding_defect("عمل کشیدن،당겨"), "Hangul script detected")

        # CJK Ideographs (Chinese)
        self.assertEqual(detect_script_or_encoding_defect("کتاب 苹果"), "CJK or Japanese script detected")
        self.assertEqual(detect_script_or_encoding_defect("آزمون 学习"), "CJK or Japanese script detected")

        # Japanese Kana
        self.assertEqual(detect_script_or_encoding_defect("سلام こんにちは"), "CJK or Japanese script detected")

    def test_replacement_character_rejection(self) -> None:
        """Verify rejection of Unicode replacement character \ufffd resulting from broken UTF-8."""
        self.assertEqual(detect_script_or_encoding_defect("خطای\ufffdسیستم"), "replacement character \ufffd detected")
        self.assertEqual(detect_script_or_encoding_defect("\ufffd"), "replacement character \ufffd detected")

    def test_urdu_and_non_persian_glyph_detection(self) -> None:
        """Verify detection of Urdu-specific characters and non-Persian glyphs."""
        # Urdu Yeh Barree (\u06d2) and Noon Ghunna (\u06ba)
        self.assertEqual(detect_script_or_encoding_defect("اپنے"), "Urdu-specific or non-Persian glyph detected")
        self.assertEqual(detect_script_or_encoding_defect("ماں"), "Urdu-specific or non-Persian glyph detected")

        # Urdu Rreh (\u0691) and Dal with small tah (\u0688)
        self.assertEqual(detect_script_or_encoding_defect("گاڑی"), "Urdu-specific or non-Persian glyph detected")
        self.assertEqual(detect_script_or_encoding_defect("ڈاکٹر"), "Urdu-specific or non-Persian glyph detected")

        # Urdu Heh Doachashmee (\u06be) and Urdu Heh Goal (\u06c1)
        self.assertEqual(detect_script_or_encoding_defect("کھانا"), "Urdu-specific or non-Persian glyph detected")
        self.assertEqual(detect_script_or_encoding_defect("یہ\u06c1"), "Urdu-specific or non-Persian glyph detected")

    def test_zwnj_normalization_boundary_cases(self) -> None:
        """Verify Persian ZWNJ normalization: accept valid ZWNJ, reject whitespace/boundary/duplicate violations."""
        # Valid Persian ZWNJ (نیم‌فاصله)
        self.assertIsNone(detect_script_or_encoding_defect("دانش‌آموز"))
        self.assertIsNone(detect_script_or_encoding_defect("می‌خواهم"))
        self.assertIsNone(detect_script_or_encoding_defect("روبه‌رو"))

        # ZWNJ adjacent to whitespace
        self.assertEqual(detect_script_or_encoding_defect("مقابل‌ آمدن"), "ZWNJ adjacent to whitespace detected")
        self.assertEqual(detect_script_or_encoding_defect("توسعه \u200cیافته"), "ZWNJ adjacent to whitespace detected")

        # Consecutive duplicate ZWNJ
        self.assertEqual(detect_script_or_encoding_defect("کتاب\u200c\u200cها"), "consecutive ZWNJ detected")

        # Boundary ZWNJ (leading or trailing)
        self.assertEqual(detect_script_or_encoding_defect("\u200cکتاب"), "boundary ZWNJ detected")
        self.assertEqual(detect_script_or_encoding_defect("کتاب\u200c"), "boundary ZWNJ detected")

        # Invisible formatting chars
        self.assertEqual(detect_script_or_encoding_defect("کتاب\u200bها"), "unexpected zero-width/invisible character detected")
        self.assertEqual(detect_script_or_encoding_defect("کتاب\ufeffها"), "unexpected zero-width/invisible character detected")

    def test_range_parser_boundary_cases(self) -> None:
        """Verify parse_range correctly handles valid ranges and rejects boundary errors."""
        self.assertEqual(parse_range("1:100"), (1, 100))
        self.assertEqual(parse_range("301-400"), (301, 400))
        self.assertEqual(parse_range("50:50"), (50, 50))

        with self.assertRaises(ValueError):
            parse_range("0:50")  # start < 1

        with self.assertRaises(ValueError):
            parse_range("100:50")  # end < start

        with self.assertRaises(ValueError):
            parse_range("abc:100")  # non-integer

        with self.assertRaises(ValueError):
            parse_range("single_number")  # missing delimiter


class Tier3CrossFeatureCombinationsTests(unittest.TestCase):
    """Tier 3: Cross-Feature Combinations (POS vs definition sense, CEFR vs exam priority, synthetic collocations)."""

    def test_pos_vs_english_definition_alignment_risky_senses(self) -> None:
        """Verify that closed-class sense risks (e.g. 'it' as noun) are identified."""
        # Closed class checklist contains 'it': expected 'pronoun'
        self.assertIn("it", RISKY_SENSES)
        self.assertEqual(RISKY_SENSES["it"], "pronoun")
        self.assertIn("or", RISKY_SENSES)
        self.assertEqual(RISKY_SENSES["or"], "conjunction")
        self.assertIn("may", RISKY_SENSES)
        self.assertEqual(RISKY_SENSES["may"], "modal")

    def test_synthetic_collocation_rejection(self) -> None:
        """Verify is_generated_family detects synthetic template collocations."""
        synthetic_row = {
            "word": "analyze",
            "partOfSpeech": "verb",
            "collocations": [
                "analyze the process",
                "analyze effectively",
                "analyze a solution",
                "attempt to analyze",
            ],
        }
        self.assertTrue(is_generated_family(synthetic_row))

        curated_row = {
            "word": "analyze",
            "partOfSpeech": "verb",
            "collocations": [
                "analyze data carefully",
                "statistically analyze results",
            ],
        }
        self.assertFalse(is_generated_family(curated_row))

    def test_c2_proper_noun_rule_ignores_words_ending_in_place_fragment(self) -> None:
        # "electricity in ..." contains the substring "city in " but is not a place name.
        false_positive = {
            "cefrLevel": "C2",
            "tags": [],
            "englishDefinition": "A small semiconductor used to control electricity in a circuit.",
        }
        self.assertFalse(looks_like_c2_proper_noun(false_positive))
        genuine = {
            "cefrLevel": "C2",
            "tags": [],
            "englishDefinition": "A city in northern France known for its cathedral.",
        }
        self.assertTrue(looks_like_c2_proper_noun(genuine))

    def test_c2_proper_noun_downranking_rule(self) -> None:
        """Verify looks_like_c2_proper_noun correctly identifies proper names/places."""
        proper_row = {
            "word": "Yorkshire",
            "cefrLevel": "C2",
            "tags": ["place"],
            "englishDefinition": "A historic county of northern England.",
            "learningOrder": 105000,
        }
        self.assertTrue(looks_like_c2_proper_noun(proper_row))

        non_proper_row = {
            "word": "ephemeral",
            "cefrLevel": "C2",
            "tags": [],
            "englishDefinition": "Lasting for a very short time.",
            "learningOrder": 5000,
        }
        self.assertFalse(looks_like_c2_proper_noun(non_proper_row))

    def test_corrected_novice_sense_does_not_trigger_priority_flag(self) -> None:
        self.assertIsNone(priority_sense_review_risk(
            "novice",
            "a person who is new to an activity or has little experience",
            "تازه‌کار؛ فردی با تجربه کم",
        ))
        self.assertIsNotNone(priority_sense_review_risk(
            "novice",
            "a member of a religious order who has not taken final vows",
            "عضو تازهوارد صومعه",
        ))

    def test_corrected_can_modal_sense_does_not_trigger_priority_flag(self) -> None:
        self.assertIsNone(priority_sense_review_risk(
            "can",
            "be able to do something or be allowed to do something",
            "توانستن؛ اجازه داشتن",
            "modal verb",
        ))
        self.assertIsNotNone(priority_sense_review_risk(
            "can",
            "a metal container",
            "قوطی",
            "noun",
        ))

    def test_corrected_editorial_traps_do_not_trigger_priority_flag(self) -> None:
        cases = [
            ("metabolism", "the chemical processes that break down substances in the body", "متابولیسم؛ فرایندهای شیمیایی بدن", "noun"),
            ("orient", "to give someone directions or to make them aware of something", "جهت‌دهی؛ آگاه کردن", "verb"),
            ("replicate", "to produce an exact copy of something or to repeat a study", "تکثیر؛ بازتولید کردن", "verb"),
            ("chess", "a board game played by two people with pieces", "شطرنج", "noun"),
            ("corpus", "a large collection of written texts used for analysis", "مجموعه متون", "noun"),
            ("metabolism", "the sum of the chemical processes by which an organism builds up and breaks down substances", "مجموعه فرایندهای شیمیایی که موجود زنده نیاز دارد", "noun"),
            ("orient", "to make someone familiar with a new situation or place", "جهت‌یابی کردن، خود را با شرایط تازه آشنا کردن", "verb"),
        ]
        for word, definition, meaning, pos in cases:
            with self.subTest(word=word):
                self.assertIsNone(priority_sense_review_risk(word, definition, meaning, pos))
        self.assertIsNotNone(priority_sense_review_risk(
            "metabolism",
            "the process by which an insect changes form",
            "دگردیسی",
            "noun",
        ))

    def test_pos_whitelist_covers_multiword_and_phrase_classes(self) -> None:
        # Multiword/phrase entries must be classifiable instead of falling back to "word".
        for pos in (
            "noun", "verb", "adjective", "adverb", "preposition", "conjunction",
            "pronoun", "determiner", "modal", "modal verb",
            "phrase", "phrasal verb", "idiom", "prepositional phrase",
        ):
            with self.subTest(pos=pos):
                self.assertIn(pos, POS_OK)
        self.assertNotIn("word", POS_OK)

    def test_corrected_closed_class_senses_do_not_trigger_priority_flag(self) -> None:
        # A correctly curated modal/function word must not be flagged just for its lemma.
        passing = [
            ("may", "Used to say something is possible or allowed.", "ممکن است؛ اجازه داشتن", "modal"),
            ("might", "Used to say something is possible but not certain.", "ممکن است؛ شاید", "modal"),
            ("must", "Used to say something is necessary or required.", "باید؛ لازم است", "modal"),
            ("it", "Used to refer to a single thing already mentioned.", "آن؛ ضمیر شخص غیرشخصی", "pronoun"),
            ("or", "Used to introduce an alternative choice.", "یا؛ یا اینکه", "conjunction"),
        ]
        for word, definition, meaning, pos in passing:
            with self.subTest(word=word):
                self.assertIsNone(priority_sense_review_risk(word, definition, meaning, pos))
        # The wrong senses must still be flagged.
        self.assertIsNotNone(priority_sense_review_risk("may", "the fifth month of the year", "ماه مه", "noun"))
        self.assertIsNotNone(priority_sense_review_risk("or", "a North American animal", "سگ گرگ", "noun"))

    def test_definition_persian_pos_alignment_heuristic(self) -> None:
        self.assertEqual(
            definition_meaning_alignment_risk("noun", "رسیدن به؛ به دست آوردن", "to arrive at a place or goal"),
            "Persian meaning is verbal but partOfSpeech is noun",
        )
        self.assertIsNone(
            definition_meaning_alignment_risk("verb", "ماندن؛ باقی ماندن", "to remain in a place or state")
        )

    def test_example_translation_pronoun_and_number_heuristic(self) -> None:
        self.assertEqual(
            example_translation_alignment_risk(
                "She has already seen that film.",
                "من قبلاً آن فیلم را دیده‌ام.",
            ),
            "English third-person subject conflicts with Persian first-person subject",
        )
        self.assertEqual(
            example_translation_alignment_risk(
                "Figure 2 shows how the system works.",
                "شکل ۳ نشان می‌دهد سیستم چگونه کار می‌کند.",
            ),
            "numeric content differs between English and Persian examples",
        )
        self.assertIsNone(
            example_translation_alignment_risk(
                "She has blue eyes.",
                "او چشمان آبی دارد.",
            )
        )
        self.assertIsNone(
            example_translation_alignment_risk(
                "Only 30 percent of students passed the exam.",
                "سی درصد از دانش‌آموزان در آزمون قبول شدند.",
            )
        )

    def test_collocation_definition_placeholder_and_metadata_heuristics(self) -> None:
        self.assertIsNone(collocation_target_risk("evidence", ["strong evidence", "provide evidence"]))
        self.assertEqual(
            collocation_target_risk("evidence", ["provide data", "gather facts"]),
            "no collocation contains the target lemma",
        )
        self.assertIsNone(placeholder_definition_risk("a short written message sent to someone"))
        self.assertIsNotNone(placeholder_definition_risk("A word used in everyday or academic English: real."))
        self.assertIsNotNone(placeholder_definition_risk("not illusory; ; ; - Longfellow"))
        self.assertTrue(metadata_outlier_risks("general", {"cefrLevel": "C2", "frequencyRank": 42, "tags": []}))
        self.assertFalse(metadata_outlier_risks("general", {"cefrLevel": "A2", "frequencyRank": 42, "tags": []}))
        self.assertTrue(metadata_outlier_risks("general", {
            "tags": ["NGSL"],
            "ieltsRelevance": "High",
            "toeflRelevance": "High",
            "greRelevance": "High",
        }))
        self.assertFalse(metadata_outlier_risks("general", {
            "tags": ["NAWL"],
            "ieltsRelevance": "High",
            "toeflRelevance": "High",
            "greRelevance": "High",
        }))

    def test_file_resolver(self) -> None:
        """Verify resolve_file correctly resolves filename, relative path, and absolute path."""
        resolved = resolve_file("general_core_001.jsonl")
        self.assertTrue(resolved.is_file())
        self.assertEqual(resolved.name, "general_core_001.jsonl")

        resolved_rel = resolve_file("general/general_core_001.jsonl")
        self.assertTrue(resolved_rel.is_file())
        self.assertEqual(resolved, resolved_rel)

        with self.assertRaises(FileNotFoundError):
            resolve_file("non_existent_file_xyz.jsonl")


class Tier4RealWorldApplicationScenariosTests(unittest.TestCase):
    """Tier 4: Real-World Application Scenarios (learner flashcards, lemma presence in examples, faithful translations)."""

    def test_example_contains_target_lemma_regular_inflections(self) -> None:
        """Verify target_present handles regular English inflections: -s, -es, -ed, -d, -ing, -ied, doubling."""
        # Regular -s / -es
        self.assertTrue(target_present("walk", "He walks to school every morning."))
        self.assertTrue(target_present("watch", "She watches documentaries."))

        # Past tense -ed / -d
        self.assertTrue(target_present("walk", "They walked five miles yesterday."))
        self.assertTrue(target_present("live", "We lived in Paris for three years."))

        # Present participle -ing
        self.assertTrue(target_present("walk", "Walking in nature improves mental health."))
        self.assertTrue(target_present("live", "She is living her best life."))

        # Consonant + y -> -ied (try -> tried)
        self.assertTrue(target_present("try", "He tried his best to solve the riddle."))
        self.assertTrue(target_present("study", "She studied biology at university."))

        # Consonant doubling (run -> running, stop -> stopped, fit -> fitted)
        self.assertTrue(target_present("run", "The athlete was running at top speed."))
        self.assertTrue(target_present("stop", "The train stopped at the station."))
        self.assertTrue(target_present("fit", "The jacket fitted him perfectly."))

    def test_example_contains_target_lemma_high_frequency_irregular_forms(self) -> None:
        """Verify target_present handles high-frequency irregular forms requested in specification."""
        test_pairs = [
            ("hold", "held", "She held the infant securely in her arms."),
            ("lose", "lost", "The team lost the championship match."),
            ("send", "sent", "He sent an urgent email to the director."),
            ("buy", "bought", "We bought fresh organic vegetables from the market."),
            ("sell", "sold", "They sold their family home last summer."),
            ("become", "became", "She became an influential researcher in robotics."),
            ("give", "gave", "The mentor gave invaluable feedback on the project."),
            ("take", "took", "They took the express shuttle to the terminal."),
            ("stand", "stood", "The monument stood resilient against harsh weather."),
            ("feel", "felt", "The audience felt inspired by the keynote address."),
            ("leave", "left", "The delegates left the conference hall at noon."),
            ("mean", "meant", "The gesture was meant as an apology."),
            ("tell", "told", "She told an enchanting bedtime story to the children."),
            ("keep", "kept", "He kept all his promises faithfully."),
            ("build", "built", "The architects built an energy-efficient library."),
            ("pay", "paid", "They paid the vendor promptly upon delivery."),
            ("meet", "met", "The committee met in the executive boardroom."),
            ("lead", "led", "She led the expedition across the mountain ridge."),
        ]
        for lemma, inflected_word, example_sentence in test_pairs:
            with self.subTest(lemma=lemma, inflected=inflected_word):
                self.assertTrue(
                    target_present(lemma, example_sentence),
                    f"target_present failed for lemma '{lemma}' (inflection: '{inflected_word}') in sentence: '{example_sentence}'"
                )

    def test_example_contains_target_lemma_hyphenated_and_multiword(self) -> None:
        """Verify target_present tokenizes hyphenated and multi-word lemmas for component matching."""
        self.assertTrue(target_present("avant-garde", "The avant-garde filmmakers challenged traditional narrative."))
        self.assertTrue(target_present("fail-safe", "The system's fail-safe activated when cooling failed."))
        self.assertTrue(target_present("point out", "The reviewer pointed out several crucial formatting errors."))
        self.assertTrue(target_present("ice cream", "We enjoyed delicious vanilla ice cream on the beach."))

    def test_example_negative_detection_genuine_defects(self) -> None:
        """Verify target_present correctly rejects examples where target lemma is missing (e.g. synset synonyms)."""
        # ECDICT / WordNet synset synonym defects
        self.assertFalse(target_present("product", "good business depends on having good merchandise"))
        self.assertFalse(target_present("buy", "The family purchased a new luxury vehicle"))
        self.assertFalse(target_present("full-bodied", "a rich ruby port"))
        self.assertFalse(target_present("half-baked", "a crazy scheme"))

    def test_learner_flashcard_usability_curated_batch_3(self) -> None:
        """Verify curated Batch 3 (cards 201-300 in general_core_001.jsonl) meets learner flashcard requirements."""
        pack_file = VOCAB_ROOT / "general" / "general_core_001.jsonl"
        lines = pack_file.read_text(encoding="utf-8").splitlines()[200:300]
        self.assertEqual(len(lines), 100)

        for idx, line in enumerate(lines, start=201):
            card = json.loads(line)
            word = card["word"]
            # Front of card essentials
            self.assertTrue(bool(word), f"Card {idx}: empty word")
            self.assertIn(card["partOfSpeech"], POS_OK, f"Card {idx} ({word}): invalid POS {card['partOfSpeech']}")

            # Back of card essentials
            self.assertTrue(bool(card["persianMeaning"]), f"Card {idx} ({word}): missing Persian meaning")
            self.assertTrue(bool(card["englishDefinition"]), f"Card {idx} ({word}): missing English definition")

            # Faithful translation coverage: whenever an example exists, Persian translation must be present
            if card.get("example"):
                self.assertTrue(
                    bool(card.get("examplePersian")),
                    f"Card {idx} ({word}): English example exists but missing Persian translation"
                )

            # No raw dictionary placeholder strings
            self.assertNotIn("معنی فارسی در منابع آزاد فعلی پیدا نشد", card["persianMeaning"])

        # High example coverage in curated Batch 3 (>= 95%)
        cards_with_example = sum(1 for line in lines if json.loads(line).get("example"))
        self.assertGreaterEqual(cards_with_example, 95)

        # High target lemma presence in examples (>= 90% of examples contain target or inflection)
        valid_targets = sum(
            1 for line in lines
            if json.loads(line).get("example") and target_present(json.loads(line)["word"], json.loads(line)["example"])
        )
        self.assertGreaterEqual(valid_targets, 90)

    def test_learner_flashcard_usability_curated_batch_4(self) -> None:
        pack_file = VOCAB_ROOT / "general" / "general_core_001.jsonl"
        lines = pack_file.read_text(encoding="utf-8").splitlines()[300:400]
        self.assertEqual(len(lines), 100)
        for index, line in enumerate(lines, start=301):
            card = json.loads(line)
            word = card["word"]
            self.assertIn(card["partOfSpeech"], POS_OK, f"Card {index} ({word}): invalid POS")
            self.assertTrue(card.get("example"), f"Card {index} ({word}): missing example")
            self.assertTrue(card.get("examplePersian"), f"Card {index} ({word}): missing example translation")
            self.assertTrue(target_present(word, card["example"]), f"Card {index} ({word}): target missing from example")
            self.assertIsNone(detect_script_or_encoding_defect(card["persianMeaning"]), f"Card {index} ({word}): bad Persian script")
            self.assertIsNone(detect_script_or_encoding_defect(card["examplePersian"]), f"Card {index} ({word}): bad translation script")
            self.assertNotIn("needs-collocation", card.get("tags", []), f"Card {index} ({word}): stale collocation tag")
            self.assertGreaterEqual(len(card.get("collocations", [])), 2, f"Card {index} ({word}): too few collocations")
            self.assertLessEqual(len(card.get("collocations", [])), 4, f"Card {index} ({word}): too many collocations")
            self.assertGreater(card.get("frequencyRank", 0), 0, f"Card {index} ({word}): invalid frequency rank")
            for field in ("ieltsRelevance", "toeflRelevance", "greRelevance"):
                self.assertIn(card[field], {"Low", "Medium", "High"}, f"Card {index} ({word}): invalid {field}")

    def test_learner_flashcard_usability_curated_batch_5(self) -> None:
        pack_file = VOCAB_ROOT / "general" / "general_core_001.jsonl"
        lines = pack_file.read_text(encoding="utf-8").splitlines()[400:450]
        self.assertEqual(len(lines), 50)
        for index, line in enumerate(lines, start=401):
            card = json.loads(line)
            word = card["word"]
            self.assertIn(card["partOfSpeech"], POS_OK, f"Card {index} ({word}): invalid POS")
            self.assertTrue(card.get("example"), f"Card {index} ({word}): missing example")
            self.assertTrue(card.get("examplePersian"), f"Card {index} ({word}): missing example translation")
            self.assertTrue(target_present(word, card["example"]), f"Card {index} ({word}): target missing from example")
            self.assertIsNone(detect_script_or_encoding_defect(card["persianMeaning"]), f"Card {index} ({word}): bad Persian script")
            self.assertIsNone(detect_script_or_encoding_defect(card["examplePersian"]), f"Card {index} ({word}): bad translation script")
            self.assertNotIn("needs-collocation", card.get("tags", []), f"Card {index} ({word}): stale collocation tag")
            self.assertGreaterEqual(len(card.get("collocations", [])), 2, f"Card {index} ({word}): too few collocations")
            self.assertLessEqual(len(card.get("collocations", [])), 4, f"Card {index} ({word}): too many collocations")
            self.assertGreater(card.get("frequencyRank", 0), 0, f"Card {index} ({word}): invalid frequency rank")

    def test_learner_flashcard_usability_curated_batch_6(self) -> None:
        pack_file = VOCAB_ROOT / "general" / "general_core_001.jsonl"
        lines = pack_file.read_text(encoding="utf-8").splitlines()[450:500]
        self.assertEqual(len(lines), 50)
        for index, line in enumerate(lines, start=451):
            card = json.loads(line)
            word = card["word"]
            self.assertIn(card["partOfSpeech"], POS_OK, f"Card {index} ({word}): invalid POS")
            self.assertTrue(card.get("example"), f"Card {index} ({word}): missing example")
            self.assertTrue(card.get("examplePersian"), f"Card {index} ({word}): missing example translation")
            self.assertTrue(target_present(word, card["example"]), f"Card {index} ({word}): target missing from example")
            self.assertIsNone(detect_script_or_encoding_defect(card["persianMeaning"]), f"Card {index} ({word}): bad Persian script")
            self.assertIsNone(detect_script_or_encoding_defect(card["examplePersian"]), f"Card {index} ({word}): bad translation script")
            self.assertNotIn("needs-collocation", card.get("tags", []), f"Card {index} ({word}): stale collocation tag")
            self.assertGreaterEqual(len(card.get("collocations", [])), 2, f"Card {index} ({word}): too few collocations")
            self.assertLessEqual(len(card.get("collocations", [])), 4, f"Card {index} ({word}): too many collocations")
            self.assertGreater(card.get("frequencyRank", 0), 0, f"Card {index} ({word}): invalid frequency rank")

    def test_learner_flashcard_usability_curated_chunk_2_batch_1(self) -> None:
        pack_file = VOCAB_ROOT / "general" / "general_core_002.jsonl"
        lines = pack_file.read_text(encoding="utf-8").splitlines()[:50]
        self.assertEqual(len(lines), 50)
        for index, line in enumerate(lines, start=501):
            card = json.loads(line)
            word = card["word"]
            self.assertIn(card["partOfSpeech"], POS_OK, f"Card {index} ({word}): invalid POS")
            self.assertTrue(card.get("example"), f"Card {index} ({word}): missing example")
            self.assertTrue(card.get("examplePersian"), f"Card {index} ({word}): missing example translation")
            self.assertTrue(target_present(word, card["example"]), f"Card {index} ({word}): target missing from example")
            self.assertIsNone(detect_script_or_encoding_defect(card["persianMeaning"]), f"Card {index} ({word}): bad Persian script")
            self.assertIsNone(detect_script_or_encoding_defect(card["examplePersian"]), f"Card {index} ({word}): bad translation script")
            self.assertNotIn("needs-collocation", card.get("tags", []), f"Card {index} ({word}): stale collocation tag")
            self.assertGreaterEqual(len(card.get("collocations", [])), 2, f"Card {index} ({word}): too few collocations")
            self.assertLessEqual(len(card.get("collocations", [])), 4, f"Card {index} ({word}): too many collocations")
            self.assertGreater(card.get("frequencyRank", 0), 0, f"Card {index} ({word}): invalid frequency rank")

    def test_e2e_cli_execution_via_subprocess(self) -> None:
        """Verify CLI execution of scripts/validate_vocabulary_quality.py via subprocess."""
        cmd = [
            sys.executable,
            str(SCRIPTS_DIR / "validate_vocabulary_quality.py"),
            "--file", "general/general_core_001.jsonl",
            "--range", "201:300",
        ]
        result = subprocess.run(
            cmd,
            cwd=str(REPO_ROOT),
            capture_output=True,
            text=True,
            encoding="utf-8",
        )
        self.assertEqual(result.returncode, 0, f"Validator CLI failed: {result.stderr}")
        report = json.loads(result.stdout)
        self.assertEqual(report["rows"], 100)
        self.assertEqual(report["errorCount"], 0)


class Tier5CuratedMultiwordAndTaxonomyTests(unittest.TestCase):
    """Invariants for the curated multiword, calibration and taxonomy stages.

    Each test here pins a defect that was actually present in the shipped
    dataset, so the suite fails if any of them is reintroduced.
    """

    EXAM_BANKS = ("ielts", "toefl")

    def _rows(self, bank: str) -> list[dict]:
        rows: list[dict] = []
        for path in sorted((VOCAB_ROOT / bank).glob("*.jsonl")):
            for raw in path.read_text(encoding="utf-8").splitlines():
                line = raw.strip()
                if line and not line.startswith("#"):
                    rows.append(json.loads(line))
        return rows

    def test_curated_catalog_is_internally_valid(self) -> None:
        from curated_multiword_catalog import CARDS, validate_cards

        self.assertGreaterEqual(len(CARDS), 250, "curated multiword catalog regressed")
        self.assertEqual(validate_cards(CARDS), [])

    def test_every_curated_card_lands_in_both_exam_banks(self) -> None:
        from curated_multiword_catalog import CARDS

        expected = {card["word"] for card in CARDS}
        for bank in self.EXAM_BANKS:
            present = {row["word"] for row in self._rows(bank)}
            # Two cards already exist as generated rows, so require near-total
            # coverage rather than exact equality.
            missing = expected - present
            self.assertLessEqual(
                len(missing), 5, f"{bank.upper()} lost {len(missing)} curated card(s)"
            )

    def test_banks_actually_contain_multiword_expressions(self) -> None:
        """The shipped banks had zero phrasal verbs before curation."""
        for bank in self.EXAM_BANKS:
            multiword = [r for r in self._rows(bank) if " " in r["word"].strip()]
            self.assertGreater(len(multiword), 250, f"{bank.upper()} has too few multiword rows")

    def test_curated_rows_carry_full_bilingual_content(self) -> None:
        for bank in self.EXAM_BANKS:
            curated = [
                r for r in self._rows(bank)
                if "curated-multiword" in {str(t) for t in r.get("tags", [])}
            ]
            self.assertGreater(len(curated), 250)
            for row in curated:
                self.assertTrue(row["persianMeaning"].strip(), row["word"])
                self.assertTrue(row["englishDefinition"].strip(), row["word"])
                self.assertTrue(row["example"].strip(), row["word"])
                self.assertTrue(row["examplePersian"].strip(), row["word"])
                self.assertTrue(row["collocations"], row["word"])
                self.assertIn(row["cefrLevel"], VALID_CEFR, row["word"])

    def test_curated_rows_survive_the_strict_quality_gate(self) -> None:
        for bank in self.EXAM_BANKS:
            for row in self._rows(bank):
                if "curated-multiword" not in {str(t) for t in row.get("tags", [])}:
                    continue
                word = row["word"]
                self.assertIsNone(
                    placeholder_definition_risk(row["englishDefinition"]), word
                )
                self.assertIsNone(
                    definition_meaning_alignment_risk(
                        row["partOfSpeech"], row["persianMeaning"], row["englishDefinition"]
                    ),
                    word,
                )
                self.assertIsNone(
                    example_translation_alignment_risk(row["example"], row["examplePersian"]),
                    word,
                )
                self.assertTrue(target_present(word, row["example"]), word)
                self.assertFalse(is_generated_family(row), word)
                self.assertEqual(
                    collocation_target_risk(word, row["collocations"]), None, word
                )

    def test_cefr_calibration_caps_saturated_c2_labels(self) -> None:
        """C2 used to cover 44% of TOEFL; list membership and frequency now cap it."""
        from recalibrate_cefr_levels import calibrate

        listed_academic = {
            "word": "corpus", "cefrLevel": "C2", "tags": ["NAWL"],
            "frequencyRank": 40_000,
        }
        self.assertEqual(calibrate(listed_academic), "C1")
        listed_general = {
            "word": "however", "cefrLevel": "C2", "tags": ["NGSL"],
            "frequencyRank": 90,
        }
        self.assertEqual(calibrate(listed_general), "B2")
        common_word = {
            "word": "incur", "cefrLevel": "C2", "tags": ["ielts"], "frequencyRank": 12_994,
        }
        self.assertEqual(calibrate(common_word), "C1")
        rare_word = {
            "word": "connivance", "cefrLevel": "C2", "tags": ["gre"], "frequencyRank": 78_487,
        }
        self.assertIsNone(calibrate(rare_word), "a rare word must keep its published level")
        self.assertIsNone(
            calibrate({"word": "harmony", "cefrLevel": "B2", "tags": [], "frequencyRank": 900}),
            "only C2 labels are in scope for this stage",
        )

    def test_every_exam_row_has_learning_order_and_a_topic(self) -> None:
        """learningOrder was missing from every bundled exam row before the fix."""
        for bank in self.EXAM_BANKS:
            rows = self._rows(bank)
            orders = [int(r.get("learningOrder") or 0) for r in rows]
            self.assertTrue(
                all(value > 0 for value in orders),
                f"{bank.upper()} has rows without a positive learningOrder",
            )
            self.assertEqual(
                len({v for v in orders if v < 100_000}), len([v for v in orders if v < 100_000]),
                f"{bank.upper()} learningOrder values are not unique",
            )
            for row in rows:
                self.assertTrue(row.get("examTopics"), f"{row['word']} has no topic")
                self.assertTrue(row.get("targetBand"), f"{row['word']} has no target band")
                self.assertTrue(row.get("skillFocus"), f"{row['word']} has no skill focus")
                self.assertIn(row["targetBand"], {"6.0", "6.5", "7.0", "7.5", "8.0", "8.0+"})

    def test_low_study_priority_rows_keep_the_sanitizer_sentinel(self) -> None:
        """The pedagogy stage must not undo the C2 name/place down-ranking."""
        for bank in (*self.EXAM_BANKS, "gre"):
            for row in self._rows(bank):
                if "low-study-priority" not in {str(t) for t in row.get("tags", [])}:
                    continue
                if not looks_like_c2_proper_noun(row):
                    continue
                self.assertGreaterEqual(
                    int(row.get("learningOrder") or 0), 100_000,
                    f"{bank}: {row['word']} lost its down-ranking sentinel",
                )

    def test_topic_tagging_is_deterministic_and_idempotent(self) -> None:
        from tag_exam_topics import band_for, skills_for, topics_for

        row = {
            "englishDefinition": "A serious illness spread by mosquitoes in hot countries.",
            "collocations": ["malaria cases"],
            "example": "Malaria cases rose sharply.",
            "synonyms": [],
            "cefrLevel": "B1",
            "stageNumber": 2,
            "partOfSpeech": "noun",
        }
        self.assertEqual(topics_for(row), ["Health"])
        self.assertEqual(band_for(row), "7.0")
        self.assertEqual(skills_for(row, ["Health"])[0], "Reading Academic")
        untagged = {
            "englishDefinition": "Contact means the state of being in touch with someone.",
            "collocations": ["in contact with"],
            "example": "",
            "synonyms": [],
            "cefrLevel": "B2",
            "partOfSpeech": "noun",
        }
        self.assertEqual(topics_for(untagged), ["General Academic"])

    def test_catalog_target_counts_match_the_shipped_banks(self) -> None:
        catalog = json.loads(
            (VOCAB_ROOT / "master_catalog.json").read_text(encoding="utf-8-sig")
        )
        for bank, pack_id in (("ielts", "pack_ielts_master"), ("toefl", "pack_toefl_master")):
            rows = self._rows(bank)
            self.assertEqual(
                catalog["targets"][pack_id], len(rows),
                f"{bank.upper()} catalog target is stale",
            )
            for entry in (c for c in catalog["chunks"] if c["packId"] == pack_id):
                # Asset paths are relative to assets/, as the importer opens them.
                path = VOCAB_ROOT.parent / entry["asset"]
                self.assertTrue(path.is_file(), f"missing chunk asset {path.name}")
                actual = sum(
                    1 for raw in path.read_text(encoding="utf-8").splitlines() if raw.strip()
                )
                self.assertEqual(
                    actual, entry["expectedItems"],
                    f"{path.name} row count does not match the catalog",
                )


class Tier6ContentQualityTests(unittest.TestCase):
    """Guards the content-quality defects found by the committed audit.

    Each assertion pins a defect that was actually present in the shipped banks:
    raw dictionary sense dumps leaking into definitions, part-of-speech tags that
    contradict the meaning, and examples that were dictionary citations rather
    than sentences.
    """

    EXAM_BANKS = ("ielts", "toefl")
    DICT_ABBREV = re.compile(r"^\s*(s|v|n|adj|adv|prep|con|vt|vi)\s*[\.:]")

    def _rows(self, bank: str) -> list[dict]:
        rows: list[dict] = []
        for path in sorted((VOCAB_ROOT / bank).glob("*.jsonl")):
            for raw in path.read_text(encoding="utf-8").splitlines():
                line = raw.strip()
                if line and not line.startswith("#"):
                    rows.append(json.loads(line))
        return rows

    def test_definitions_have_no_dictionary_sense_dumps(self) -> None:
        """990 rows once showed raw text such as 'n. a general officer of...'."""
        offenders = []
        for bank in (*self.EXAM_BANKS, "gre"):
            for row in self._rows(bank):
                definition = str(row.get("englishDefinition", ""))
                if "\\n" in definition or "\n" in definition or self.DICT_ABBREV.match(definition):
                    offenders.append(f"{bank}:{row['word']}")
        self.assertEqual(offenders[:10], [], f"{len(offenders)} definition(s) still leak a dump")

    def test_normalized_definitions_are_complete_sentences(self) -> None:
        for bank in (*self.EXAM_BANKS, "gre"):
            for row in self._rows(bank):
                definition = str(row.get("englishDefinition", "")).strip()
                if not definition:
                    continue
                self.assertTrue(
                    definition.endswith((".", "!", "?")),
                    f"{bank}:{row['word']} definition has no terminal punctuation",
                )

    def test_multisense_rows_expose_a_primary_sense(self) -> None:
        """Senses must be usable, so exactly one has to be primary."""
        checked = 0
        for bank in self.EXAM_BANKS:
            for row in self._rows(bank):
                senses = row.get("senses")
                if not isinstance(senses, list) or len(senses) < 2:
                    continue
                checked += 1
                primaries = [s for s in senses if s.get("isPrimary")]
                self.assertEqual(len(primaries), 1, f"{row['word']} primary sense count")
                indices = [int(s["senseIndex"]) for s in senses]
                self.assertEqual(indices, list(range(1, len(senses) + 1)), row["word"])
                for sense in senses:
                    self.assertTrue(str(sense.get("englishDefinition", "")).strip(), row["word"])
        self.assertGreater(checked, 200, "expected multi-sense rows in the exam banks")

    def test_primary_sense_matches_the_card_definition(self) -> None:
        for bank in self.EXAM_BANKS:
            for row in self._rows(bank):
                senses = row.get("senses")
                if not isinstance(senses, list) or len(senses) < 2:
                    continue
                primary = next(s for s in senses if s.get("isPrimary"))
                self.assertEqual(
                    primary["englishDefinition"], row["englishDefinition"],
                    f"{row['word']} primary sense disagrees with the card definition",
                )

    def test_part_of_speech_agrees_with_the_definition(self) -> None:
        def implied(definition: str) -> str | None:
            definition = definition.strip()
            if re.match(r"^(To|to)\s+[a-z]", definition):
                return "verb"
            if re.match(r"^(A|An|The|a|an|the)\s+[a-z]", definition):
                return "noun"
            return None

        offenders = []
        for bank in self.EXAM_BANKS:
            for row in self._rows(bank):
                guess = implied(str(row.get("englishDefinition", "")))
                tag = str(row.get("partOfSpeech", ""))
                if guess and tag in ("noun", "verb") and guess != tag:
                    offenders.append(f"{bank}:{row['word']}")
        self.assertEqual(offenders[:10], [], f"{len(offenders)} POS contradiction(s) remain")

    def test_dictionary_citation_examples_are_detected(self) -> None:
        from validate_vocabulary_quality import dictionary_citation_example_risk

        bad = [
            'In this context, "choose" means v.',
            'The author uses "crisscross" to describe the process accurately.',
            'The article explains the meaning of the term "daisy" in this context.',
            "Researchers carefully examined the term 'braid' in the report.",
        ]
        for example in bad:
            self.assertIsNotNone(
                dictionary_citation_example_risk(example), example
            )
        good = [
            "Her mother used to braid her hair every Sunday evening.",
            "Choose a topic you can argue both sides of.",
            "Water chips in ceramic glazes as it cools.",
        ]
        for example in good:
            self.assertIsNone(dictionary_citation_example_risk(example), example)

    def test_curated_corrections_are_well_formed_and_applied(self) -> None:
        from curated_row_corrections import CORRECTIONS, validate_corrections

        self.assertEqual(validate_corrections(CORRECTIONS), [])
        tagged = [
            row
            for bank in self.EXAM_BANKS
            for row in self._rows(bank)
            if "corrected-content" in {str(t) for t in row.get("tags", [])}
        ]
        self.assertGreater(len(tagged), 100, "curated corrections are not reaching the banks")

    def test_definition_normaliser_splits_senses(self) -> None:
        from normalize_definitions import build_senses, is_dump, strip_pos_prefix

        dump = "n. a general officer of the highest rank\\nn. the head of a religious order"
        self.assertTrue(is_dump(dump))
        built = build_senses(dump, "noun")
        self.assertIsNotNone(built)
        primary, senses = built
        self.assertEqual(len(senses), 2)
        self.assertTrue(primary.startswith("A general officer"))
        self.assertTrue(senses[0]["isPrimary"])
        self.assertFalse(senses[1]["isPrimary"])
        # The sense matching the card tag wins, so an adjective card is not
        # defined as a verb.
        prefer_adjective = build_senses(
            "v. stroke soothingly\\ns. soft and mild; not harsh or stern", "adjective"
        )
        self.assertTrue(prefer_adjective[0].startswith("Soft and mild"))
        self.assertEqual(strip_pos_prefix("s. gentle")[0], "adjective")
        self.assertFalse(is_dump("A gentle person."))


def main() -> None:
    unittest.main(verbosity=2)


if __name__ == "__main__":
    main()
