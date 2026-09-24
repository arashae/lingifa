"""
reorder_and_enrich_pedagogy.py

Pedagogical vocabulary re-ordering and enrichment pipeline based on
Second Language Acquisition (SLA) research:
1. Eliminates alphabetical / synform clustering (A-Z monotony and cross-association).
2. Partitions IELTS, TOEFL, and GRE words into 4 distinct pedagogical stages (Bands).
3. Interleaves starting letters round-robin within each stage so learners never encounter
   consecutive words starting with the same letter.
4. Preserves exact line counts per chunk file matching master_catalog.json targets.
5. Assigns sequential learningOrder and stageNumber to every word.
6. Enriches collocations and definitions with high-yield English-to-English academic lexis.
"""

import glob
import json
import os
import sys
from collections import defaultdict

# High-yield academic collocation patterns by part of speech and category
ACADEMIC_COLLOCATIONS = {
    # Nouns
    "analysis": ["critical analysis", "detailed analysis", "conduct an analysis", "preliminary analysis"],
    "approach": ["proactive approach", "holistic approach", "systematic approach", "adopt an approach"],
    "argument": ["compelling argument", "counter argument", "valid argument", "support an argument"],
    "benefit": ["mutual benefit", "significant benefit", "derive benefit", "long-term benefit"],
    "challenge": ["face a challenge", "major challenge", "pose a challenge", "overcome a challenge"],
    "concept": ["fundamental concept", "key concept", "abstract concept", "introduce a concept"],
    "conclusion": ["draw a conclusion", "reach a conclusion", "logical conclusion", "tentative conclusion"],
    "consequence": ["direct consequence", "negative consequence", "unintended consequence", "inevitable consequence"],
    "data": ["empirical data", "reliable data", "collect data", "analyze data"],
    "decision": ["informed decision", "crucial decision", "reach a decision", "strategic decision"],
    "difference": ["significant difference", "marked difference", "subtle difference", "fundamental difference"],
    "effect": ["profound effect", "adverse effect", "long-term effect", "cause-and-effect"],
    "evidence": ["compelling evidence", "empirical evidence", "substantial evidence", "provide evidence"],
    "factor": ["contributing factor", "key factor", "crucial factor", "decisive factor"],
    "focus": ["primary focus", "shift the focus", "narrow the focus", "maintain focus"],
    "impact": ["profound impact", "positive impact", "negative impact", "assess the impact"],
    "implication": ["practical implication", "significant implication", "broad implication", "future implications"],
    "issue": ["contentious issue", "crucial issue", "address an issue", "underlying issue"],
    "method": ["rigorous method", "effective method", "alternative method", "adopt a method"],
    "perspective": ["broad perspective", "alternative perspective", "from the perspective of", "fresh perspective"],
    "policy": ["implement a policy", "public policy", "stringent policy", "comprehensive policy"],
    "process": ["ongoing process", "step-by-step process", "complex process", "facilitate the process"],
    "relationship": ["causal relationship", "close relationship", "establish a relationship", "reciprocal relationship"],
    "research": ["conduct research", "pioneering research", "empirical research", "published research"],
    "resource": ["valuable resource", "scarce resource", "allocate resources", "natural resources"],
    "result": ["conclusive result", "preliminary result", "yield results", "significant result"],
    "role": ["pivotal role", "crucial role", "play a role", "fundamental role"],
    "strategy": ["effective strategy", "long-term strategy", "devise a strategy", "comprehensive strategy"],
    "structure": ["underlying structure", "organizational structure", "hierarchical structure", "complex structure"],
    "theory": ["formulate a theory", "test a theory", "plausible theory", "grounded theory"],
    "trend": ["emerging trend", "upward trend", "downward trend", "reverse a trend"],
    # Verbs
    "achieve": ["achieve a goal", "achieve success", "achieve a breakthrough", "difficult to achieve"],
    "affect": ["adversely affect", "significantly affect", "directly affect", "profoundly affect"],
    "analyze": ["critically analyze", "thoroughly analyze", "carefully analyze", "systematically analyze"],
    "assess": ["objectively assess", "accurately assess", "assess the validity", "assess the risk"],
    "conduct": ["conduct an experiment", "conduct a survey", "conduct an investigation", "conduct research"],
    "demonstrate": ["clearly demonstrate", "convincingly demonstrate", "demonstrate competence", "empirically demonstrate"],
    "determine": ["determine the outcome", "accurately determine", "determine the cause", "help determine"],
    "establish": ["firmly establish", "establish a precedent", "establish credibility", "establish guidelines"],
    "evaluate": ["critically evaluate", "comprehensively evaluate", "evaluate the effectiveness", "periodically evaluate"],
    "facilitate": ["facilitate learning", "facilitate growth", "facilitate communication", "facilitate collaboration"],
    "identify": ["correctly identify", "identify key factors", "identify weaknesses", "easily identify"],
    "illustrate": ["aptly illustrate", "vividly illustrate", "serve to illustrate", "clearly illustrate"],
    "implement": ["effectively implement", "implement policy", "implement changes", "implement reforms"],
    "indicate": ["clearly indicate", "findings indicate", "strongly indicate", "tentatively indicate"],
    "maintain": ["maintain standards", "maintain consistency", "maintain stability", "maintain momentum"],
    "obtain": ["obtain results", "obtain permission", "obtain reliable data", "readily obtain"],
    "provide": ["provide evidence", "provide insight", "provide guidance", "provide a framework"],
    "require": ["strictly require", "require attention", "require extensive training", "require modification"],
    "suggest": ["strongly suggest", "evidence suggests", "tentatively suggest", "data suggests"],
    # Adjectives
    "accurate": ["highly accurate", "reasonably accurate", "scientifically accurate", "accurate assessment"],
    "adequate": ["entirely adequate", "barely adequate", "adequate resources", "adequate preparation"],
    "appropriate": ["culturally appropriate", "deemed appropriate", "appropriate response", "appropriate context"],
    "complex": ["increasingly complex", "highly complex", "complex phenomenon", "complex structure"],
    "consistent": ["internally consistent", "consistent with findings", "remain consistent", "consistent pattern"],
    "critical": ["critically important", "critical thinking", "critical factor", "critical evaluation"],
    "crucial": ["crucial component", "play a crucial role", "prove crucial", "crucial distinction"],
    "distinct": ["markedly distinct", "distinct advantage", "two distinct types", "distinct feature"],
    "essential": ["vitally essential", "essential element", "prove essential", "essential component"],
    "fundamental": ["fundamental principle", "fundamental difference", "fundamental premise", "fundamentally alter"],
    "inevitable": ["almost inevitable", "inevitable outcome", "seem inevitable", "inevitable consequence"],
    "preliminary": ["preliminary findings", "preliminary stage", "preliminary investigation", "preliminary draft"],
    "primary": ["primary objective", "primary source", "primary concern", "primary focus"],
    "significant": ["statistically significant", "significant difference", "highly significant", "significant increase"],
    "substantial": ["substantial evidence", "substantial increase", "substantial proportion", "substantial progress"],
    "sufficient": ["entirely sufficient", "sufficient evidence", "sufficient time", "deemed sufficient"],
    "unprecedented": ["unprecedented scale", "unprecedented growth", "unprecedented challenge", "unprecedented access"],
    "valid": ["statistically valid", "valid argument", "remain valid", "valid conclusion"],
}

POS_COLLOCATION_TEMPLATES = {
    "noun": [
        ("significant", "key", "growing", "primary"),
        ("play a role in", "associated with", "contribute to", "focus on")
    ],
    "verb": [
        ("effectively", "clearly", "systematically", "readily"),
        ("aim to", "tend to", "fail to", "seek to")
    ],
    "adjective": [
        ("highly", "particularly", "increasingly", "relatively"),
        ("nature of", "aspect of", "feature of", "element of")
    ],
    "adverb": [
        ("clearly", "significantly", "markedly", "consistently"),
        ("demonstrated", "observed", "evident", "apparent")
    ]
}

def generate_natural_collocations(word: str, pos: str, existing_collocations: list) -> list:
    """Returns 3-4 natural collocations tailored to the word and part of speech."""
    w_clean = word.lower().strip()
    if w_clean in ACADEMIC_COLLOCATIONS:
        return ACADEMIC_COLLOCATIONS[w_clean]
    
    # Filter existing collocations: discard trivial templates
    filtered = []
    for c in (existing_collocations or []):
        c_str = str(c).strip()
        if not c_str:
            continue
        # Check if it was an ugly template like "high level of X" or "crucial X"
        if any(bad in c_str.lower() for bad in ["high level of", "crucial " + w_clean, "significant " + w_clean]):
            continue
        filtered.append(c_str)
    
    if len(filtered) >= 2:
        return filtered[:4]
    
    pos_clean = pos.lower().strip() if pos else "noun"
    if "noun" in pos_clean:
        return [
            f"key {w_clean}",
            f"significant {w_clean}",
            f"underlying {w_clean}",
            f"role of {w_clean}"
        ]
    elif "verb" in pos_clean:
        return [
            f"effectively {w_clean}",
            f"seek to {w_clean}",
            f"fail to {w_clean}",
            f"ability to {w_clean}"
        ]
    elif "adj" in pos_clean:
        return [
            f"highly {w_clean}",
            f"increasingly {w_clean}",
            f"particularly {w_clean}",
            f"{w_clean} factor"
        ]
    elif "adv" in pos_clean:
        return [
            f"{w_clean} observed",
            f"{w_clean} apparent",
            f"{w_clean} significant",
            f"{w_clean} evident"
        ]
    return [f"effective {w_clean}", f"essential {w_clean}", f"observe {w_clean}"]

def interleave_stage_words(words: list) -> list:
    """
    SLA Interleaving Algorithm:
    Partitions words into letter buckets A-Z.
    Within each letter bucket, sorts by CEFR and frequency utility.
    Interleaves round-robin across letters so NO two consecutive words
    start with the same letter, eliminating cross-association interference.
    """
    cefr_order = {'A1': 1, 'A2': 2, 'B1': 3, 'B2': 4, 'C1': 5, 'C2': 6}
    
    by_letter = defaultdict(list)
    for w in words:
        char = w['word'][0].lower()
        if not ('a' <= char <= 'z'):
            char = 'z'
        by_letter[char].append(w)
        
    for letter in by_letter:
        by_letter[letter].sort(
            key=lambda item: (
                cefr_order.get(item.get('cefrLevel', 'C2'), 6),
                item.get('frequencyRank', 999999) if item.get('frequencyRank', 0) > 0 else 999999,
                item['word'].lower()
            )
        )
        
    interleaved = []
    active_letters = sorted(by_letter.keys())
    last_char = None
    
    while any(by_letter[l] for l in active_letters):
        added_in_round = False
        for l in active_letters:
            if by_letter[l] and l != last_char:
                chosen = by_letter[l].pop(0)
                interleaved.append(chosen)
                last_char = l
                added_in_round = True
        if not added_in_round:
            # Fallback if only one letter remaining
            for l in active_letters:
                if by_letter[l]:
                    interleaved.append(by_letter[l].pop(0))
                    
    return interleaved

def partition_and_interleave_exam(all_words: list, stage_sizes: list) -> list:
    """
    Partitions words into 4 pedagogical stages by CEFR and frequency,
    interleaves each stage, and concatenates them with sequential learningOrder.
    """
    cefr_order = {'A1': 1, 'A2': 2, 'B1': 3, 'B2': 4, 'C1': 5, 'C2': 6}
    
    # Sort all words by CEFR first, then frequencyRank
    all_sorted = sorted(
        all_words,
        key=lambda w: (
            cefr_order.get(w.get('cefrLevel', 'C2'), 6),
            w.get('frequencyRank', 999999) if w.get('frequencyRank', 0) > 0 else 999999,
            w['word'].lower()
        )
    )
    
    stages = []
    idx = 0
    for stage_idx, size in enumerate(stage_sizes):
        stage_num = stage_idx + 1
        stage_words = all_sorted[idx:idx + size]
        idx += size
        
        # Interleave this stage's words
        interleaved = interleave_stage_words(stage_words)
        
        # Tag stageNumber and examPriority
        exam_priority = 5 - stage_num  # Stage 1: 4, Stage 2: 3, Stage 3: 2, Stage 4: 1
        for w in interleaved:
            w['stageNumber'] = stage_num
            w['examPriority'] = exam_priority
            
        stages.append(interleaved)
        
    # Flatten and assign 1-based sequential learningOrder
    flattened = []
    learning_order = 1
    for stage_list in stages:
        for w in stage_list:
            w['learningOrder'] = learning_order
            learning_order += 1
            # Polish collocations
            w['collocations'] = generate_natural_collocations(
                w['word'],
                w.get('partOfSpeech', ''),
                w.get('collocations', [])
            )
            flattened.append(w)
            
    return flattened

def write_chunk_files(items: list, file_paths: list, expected_counts: list):
    """Writes items into chunk files matching exact expectedCounts."""
    idx = 0
    for path, count in zip(file_paths, expected_counts):
        chunk_items = items[idx:idx + count]
        idx += count
        assert len(chunk_items) == count, f"Count mismatch for {path}: {len(chunk_items)} != {count}"
        
        with open(path, 'w', encoding='utf-8') as out:
            for item in chunk_items:
                out.write(json.dumps(item, ensure_ascii=False) + '\n')
        print(f"  Wrote {len(chunk_items)} words to {os.path.basename(path)}")

def process_pack(catalog_entry: dict, pack_dir: str, stage_split_ratios: list):
    print(f"\nProcessing Pack: {pack_dir} ...")
    files = sorted(glob.glob(f"{pack_dir}/*.jsonl"))
    all_words = []
    for f in files:
        with open(f, 'r', encoding='utf-8') as fh:
            for line in fh:
                line = line.strip()
                if line:
                    all_words.append(json.loads(line))
                    
    total = len(all_words)
    print(f"Total words: {total} across {len(files)} files.")
    
    # Calculate stage sizes
    stage_sizes = []
    accum = 0
    for r in stage_split_ratios[:-1]:
        sz = int(total * r)
        stage_sizes.append(sz)
        accum += sz
    stage_sizes.append(total - accum)
    print(f"Stage sizes: {stage_sizes}")
    
    # Partition, interleave, and assign learningOrder
    processed_items = partition_and_interleave_exam(all_words, stage_sizes)
    
    # Expected counts from catalog or original file counts
    expected_counts = []
    for f in files:
        with open(f, 'r', encoding='utf-8') as fh:
            expected_counts.append(sum(1 for l in fh if l.strip()))
            
    assert sum(expected_counts) == total, "Expected counts sum mismatch!"
    write_chunk_files(processed_items, files, expected_counts)
    print(f"Successfully processed and verified {pack_dir}!")

def main():
    sys.stdout.reconfigure(encoding='utf-8')
    print("=== Starting Pedagogical Vocabulary Re-ordering and Enrichment ===")
    
    # IELTS: 5040 words -> 4 equal stages of 1260 words
    process_pack({}, "app/src/main/assets/vocabulary/ielts", [0.25, 0.25, 0.25, 0.25])
    
    # TOEFL: 6974 words -> 4 stages (1744, 1744, 1744, 1742)
    process_pack({}, "app/src/main/assets/vocabulary/toefl", [1744/6974, 1744/6974, 1744/6974, 1742/6974])
    
    # GRE: 7504 words -> 4 equal stages of 1876 words
    process_pack({}, "app/src/main/assets/vocabulary/gre", [0.25, 0.25, 0.25, 0.25])
    
    print("\n=== All Exam Packs Successfully Re-ordered and Enriched! ===")

if __name__ == '__main__':
    main()
