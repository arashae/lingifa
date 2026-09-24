"""
reorder_cefr.py
Interleaves CEFR level chunks so each CEFR level has:
1. Sequential learningOrder (1 to N within each level).
2. Interleaved starting letters A-Z (round robin) ordered by frequencyRank.
3. Preserves exact chunk file item counts.
"""

import glob
import json
import os
import sys
from collections import defaultdict

def interleave_words(words: list) -> list:
    by_letter = defaultdict(list)
    for w in words:
        char = w['word'][0].lower()
        if not ('a' <= char <= 'z'):
            char = 'z'
        by_letter[char].append(w)
        
    for letter in by_letter:
        by_letter[letter].sort(
            key=lambda item: (
                item.get('frequencyRank', 999999) if item.get('frequencyRank', 0) > 0 else 999999,
                item['word'].lower()
            )
        )
        
    interleaved = []
    active_letters = sorted(by_letter.keys())
    last_char = None
    
    while any(by_letter[l] for l in active_letters):
        added = False
        for l in active_letters:
            if by_letter[l] and l != last_char:
                interleaved.append(by_letter[l].pop(0))
                last_char = l
                added = True
        if not added:
            for l in active_letters:
                if by_letter[l]:
                    interleaved.append(by_letter[l].pop(0))
                    
    for i, w in enumerate(interleaved):
        w['learningOrder'] = i + 1
        
    return interleaved

def process_cefr_level(level_dir: str):
    files = sorted(glob.glob(f"{level_dir}/*.jsonl"))
    if not files:
        return
        
    all_words = []
    counts = []
    for f in files:
        with open(f, 'r', encoding='utf-8') as fh:
            c = 0
            for line in fh:
                line = line.strip()
                if line:
                    all_words.append(json.loads(line))
                    c += 1
            counts.append(c)
            
    print(f"Level {os.path.basename(level_dir)}: {len(all_words)} words across {len(files)} files.")
    interleaved = interleave_words(all_words)
    
    idx = 0
    for path, count in zip(files, counts):
        chunk = interleaved[idx:idx + count]
        idx += count
        with open(path, 'w', encoding='utf-8') as out:
            for item in chunk:
                out.write(json.dumps(item, ensure_ascii=False) + '\n')
        print(f"  Wrote {len(chunk)} words to {os.path.basename(path)}")

def main():
    sys.stdout.reconfigure(encoding='utf-8')
    for lvl in ['a1', 'a2', 'b1', 'b2', 'c1', 'c2']:
        process_cefr_level(f"app/src/main/assets/vocabulary/cefr/{lvl}")

if __name__ == '__main__':
    main()
