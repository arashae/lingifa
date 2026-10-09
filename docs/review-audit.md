# Review integrity audit

Baseline: `5e77e8325fbd42c574cd26cbc22fceca8acb8298` (PR #14). Scope: queue, task generation, grading, SRS persistence, daily activity and library status queries.

## Confirmed findings and repairs

| Priority | Finding | Repair |
| --- | --- | --- |
| P1 | Non-due success advanced stability, scheduled dates, correct count and mastery; refreshing could repeatedly inflate them. | Early practice changes the exercised skill only. Due reviews advance SRS. Early failure brings the next review forward to at most 30 minutes, never postpones a nearer review. |
| P1 | Vocabulary could commit while mistake/activity saving failed, leaving a blocked submission and making retries unsafe. | One Room transaction reads fresh vocabulary and writes vocabulary, mistake and daily activity. A failure rolls everything back and exposes retry. |
| P2 | Finishing a session was required to record XP and activity. | Persist each submitted card. Minutes are incremental within the session. |
| P2 | Cooldown was only a preference; a fallback immediately reused cards. | Remove fallback. Persist an independent practice timestamp so SRS elapsed time remains intact. Genuine due cards retain priority. Ungraded due targets rest without changing their due date. |
| P2 | Exact headword matching treated valid alternative words as errors. | Provide first-letter/length target cue; use neutral mismatch feedback. Learner-declared alternative answers skip target grading and leave skill/SRS unchanged. This does not automatically certify synonyms. |
| P2 | Repeated headwords leaked through cloze prompts; punctuation deletion awarded false spelling matches. | Hide every exact occurrence; normalize typography and harmless terminal punctuation while preserving internal spelling errors. |
| P2 | Learning/New filters disagreed with the stricter mastered gate. | New means no attempts. Learning means attempted and not mastered. Apply in both library and pack queries. |
| P2 | Optional candidates were limited to the latest 2,000 studied words. | Sample up to 2,000 across all studied words before weighting weak/recent/rotating choices. All due words remain eligible through a separate query. |
| P2 | No recovery for loading/storage errors; concurrent restart and stale/deleted cards could be graded. | Loading/submitting guards, explicit error/restart UI, fresh reads and content validation. Rating requires revealed feedback. |
| P2 | Tied skill scores chose the same mode forever once scores saturated. | A persisted per-word exercise sequence rotates tied modes, including after scores reach 100. |
| P3 | Keyboard/focus and scroll state carried into feedback/next card; no blank-answer route. | Clear focus on check, key scroll by task, add “I don’t know”. |
| P3 | Empty-state and scheduler comments overstated behavior/scientific validation. | Correct text. Describe the scheduler as a custom heuristic and mastery as a progress score. |

## Regression coverage

Added 19 tests covering early/due review differences, cooldown refresh, independent practice time, future-due input, cloze leakage, typography/spelling, transactional rollback and retry, per-card activity, alternative answers, stale content, preservation of unrelated edits, and actual Room library/pack classification. A simulated SQLite trigger fails activity saving after the word write to exercise rollback.

CI gate: `testDebugUnitTest lintDebug assembleDebug`, plus the existing vocabulary content suites and APK signer/version checks. Physical-device keyboard/layout behavior still needs a device check; a JVM build is not a visual test.

## Learning-method limits

- The scheduler is custom, not FSRS. FSRS has specified equations and parameters; adopting it should include persisted review events, versioned migration, fixed algorithm vectors, and retention/workload evaluation. Changing the formula alone would not provide that evidence.
- Meaning, recall, definition, context and word-distinction scores remain heuristics stored as tags, with one SRS schedule per word. This does not demonstrate separate long-term productive and receptive mastery.
- The 12 curated pairs cover confusing/related words, not a general synonym dictionary. True synonyms require sense, register, collocations and counterexamples. Broad automatic synonym acceptance is not reliable enough to grade target recall.
- Target hints make these exercises cued recall; do not present their scores as an unaided recall test. An alternative answer receives participation credit, not a target mastery gain.
- The 30-minute rest and 50-card cap are product policies, not universally optimal learning constants. Dashboard targets of 30 due/10 weak words are recommendations, not exact session counts.
- The randomized optional pool is bounded at 2,000 for performance. It avoids permanent recency exclusion but does not guarantee every non-due word appears in each session.

## Evidence informing the audit

- Karpicke & Roediger (2008), *The Critical Importance of Retrieval for Learning*: repeated retrieval improved delayed foreign-language vocabulary retention. This supports effortful retrieval and delayed evaluation; it does not validate this app’s scheduler, score increments or timing constants. <https://doi.org/10.1126/science.1152408>
- FSRS algorithm specification: <https://github.com/open-spaced-repetition/awesome-fsrs/wiki/The-Algorithm>. Comparing the actual equations with this repository confirms that the repository currently uses a separate heuristic.

No existing word content, database columns, or public import/export format was changed. Practice timestamps use reserved tags; existing study/pack data is retained.
