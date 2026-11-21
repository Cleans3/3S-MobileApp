# Blind precision rating (protocol 5b)

You rate findings from a static detector. You are blind to which tool, prompt style or app produced each one: rows carry a salted-hash sample_id only.

Input: blind_sample.csv (sample_id, rule_id, rule_definition, file_tail, line, evidence, context).
Output: rating_sheet.csv, one row per sample_id you rate: sample_id, rater (your name, same on every row), label, note.

Labels
- tp: the flagged code does exhibit the pattern in rule_definition. Judge the pattern, not whether it matters in practice.
- fp: the code does not exhibit it (the rule matched text that means something else, a guarded case, dead or test-only code, a comment).
- unclear: the context window is not enough to decide. Use sparingly and write why in note.

Rules of the game
- Rate from the row alone. Do not open other files from the repository, do not search for the sample, do not try to infer the app or the tool.
- Rate every row; do not skip the ones that look odd.
- One line in note for every fp and every unclear.
- Do not discuss ratings with the other rater until both sheets are written. Disagreements go to a third reading with the rule definition.

The two sheets are merged by harness/rate_merge.py (Cohen's kappa; a rule needs precision >= 0.80 on at least 5 rated findings to stay in the primary analysis).
