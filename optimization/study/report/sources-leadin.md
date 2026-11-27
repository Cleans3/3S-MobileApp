# Sources for the report lead-in (hype, one-shot promise, reality, variance)

Owner: beta-fg-3, REQ#adhoc-1127-study-report:3. Accessed 2026-11-27 (all entries). Written for alpha to merge; nothing else in the repo was edited.

**How verified.** Each page was fetched (HTTP 200) and every quote below was matched by script as an exact substring of the fetched page text. Typographic quotes/apostrophes in the originals are written here as straight quotes; nothing else was altered. Entries marked *secondary* quote a publisher whose primary page blocks automated clients (HTTP 403 Cloudflare challenge); the challenge was not bypassed. Entries with an id in `references.md` reuse that id.

Lead-in points: **hype** (AI coding moves fast, everyone chases it), **one-shot** (the "one prompt, same result" promise), **reality** (results are worse or more costly than promised, slop), **variance** (same prompt, different results).

---

## L1. Karpathy coins "vibe coding" (hype)
- **Claim:** In Feb 2025 Andrej Karpathy named a way of coding where you accept AI output without reading it, and called it fine for throwaway projects.
- **Quote:** "There's a new kind of coding I call "vibe coding", where you fully give in to the vibes, embrace exponentials, and forget that the code even exists."
- **Quote:** "I "Accept All" always, I don't read the diffs anymore."
- **Quote:** "It's not too bad for throwaway weekend projects, but still quite amusing."
- **URL:** https://x.com/karpathy/status/1886192184808149383
- **Publisher / date:** Andrej Karpathy on X, 2 Feb 2025 (timestamp from the fxtwitter API mirror of the same status id; the x.com page text carries the first quote above).
- **Supports:** hype. The origin text itself scopes the method to throwaway projects, which the social-media version drops.
- **Note:** first quote verified on x.com page; the other two verified on the API mirror https://api.fxtwitter.com/karpathy/status/1886192184808149383.

## L2. Collins Dictionary Word of the Year 2025 is "vibe coding" (hype) — *secondary*
- **Claim:** Collins named "vibe coding" its Word of the Year 2025, after a large rise in usage since February 2025.
- **Quote:** ""It's programming by vibes, not variables," said Collins."
- **Quote:** "While tech experts debate whether it's revolutionary or reckless, the term has resonated far beyond Silicon Valley"
- **Quote (Irish Examiner):** "They chose vibe coding as Word of the Year after observing a huge increase in usage since its first appearance in February."
- **URL:** https://gulfnews.com/world/europe/vibe-coding-named-word-of-the-year-by-collins-dictionary-2-1.500337462 ; https://www.irishexaminer.com/news/arid-41737758.html
- **Publisher / date:** Gulf News; Irish Examiner (both quoting Collins). Announcement 6 Nov 2025 (Akashvani/AIR item dated 6 Nov 2025: https://www.newsonair.gov.in/vibe-coding-named-collins-dictionarys-word-of-the-year-2025/).
- **Primary:** https://www.collinsdictionary.com/woty returned HTTP 403 (challenge), so the Collins page itself was not read.
- **Supports:** hype.

## L3. Merriam-Webster Word of the Year 2025 is "slop" (reality) — *secondary*
- **Claim:** Merriam-Webster chose "slop", defined as low-quality AI-made content, as its 2025 word of the year.
- **Quote:** "digital content of low quality that is produced usually in quantity by means of artificial intelligence."
- **Quote:** "It's almost a defiant word when it comes to AI." (Merriam-Webster president Greg Barlow)
- **URL:** https://www.pbs.org/newshour/nation/merriam-websters-word-of-the-year-for-2025-is-ais-slop
- **Publisher / date:** PBS NewsHour (AP story), 15 Dec 2025.
- **Primary:** https://www.merriam-webster.com/wordplay/word-of-the-year returned HTTP 403 (challenge), not read.
- **Supports:** reality; the report's term "slop" is now a dictionary word.

## L4. Developer AI adoption (hype) — Stack Overflow 2025
- **Claim:** In the 2025 Stack Overflow survey 84% of respondents use or plan to use AI tools, up from 76%; 51% of professional developers use them daily.
- **Quote:** "84% of respondents are using or planning to use AI tools in their development process, an increase over last year (76%)."
- **Quote:** "This year we can see 51% of professional developers use AI tools daily."
- **URL:** https://survey.stackoverflow.co/2025/ai
- **Publisher / date:** Stack Overflow, 2025 Developer Survey (page carries no exact publication date).
- **Supports:** hype (everyone is on it).

## L5. Distrust and "almost right" (reality) — Stack Overflow 2025
- **Claim:** More developers distrust AI accuracy than trust it, and the top frustration is output that is almost right but not quite.
- **Quote:** "More developers actively distrust the accuracy of AI tools (46%) than trust it (33%), and only a fraction (3%) report "highly trusting" the output."
- **Quote:** "The biggest single frustration, cited by 66% of developers, is dealing with "AI solutions that are almost right, but not quite,""
- **URL:** https://survey.stackoverflow.co/2025/ai
- **Publisher / date:** Stack Overflow, 2025 Developer Survey (undated page).
- **Supports:** reality. Pairs with L4: adoption is high, trust is not.

## L6. Security flaws in AI code; bigger models not safer (reality + variance) — id [Veracode2025]
- **Claim:** Veracode tested code from 100+ LLMs in four languages; 45% of tests produced a security flaw, and larger/newer models did not improve that.
- **Quote:** "AI-generated code introduced risky security flaws in 45% of tests."
- **Quote:** "Larger, newer AI models didn't improve security."
- **Quote:** "analyzes the security of code generated by over 100 large language models across Java, JavaScript, Python, and C#"
- **URL:** https://www.veracode.com/resources/analyst-reports/2025-genai-code-security-report/
- **Publisher / date:** Veracode, 2025 GenAI Code Security Report (landing page; no date on page). Already in references.md as [Veracode2025]; re-fetched 2026-11-27, HTTP 200.
- **Supports:** reality; also variance (model size/newness does not predict the outcome).

## L7. METR RCT: experienced developers 19% slower, but felt faster (reality) — with mandatory later caveat
- **Claim:** In a randomized trial (16 developers, 246 tasks, Feb-Jun 2025) allowing AI made tasks take 19% longer, while developers believed it made them faster.
- **Quote:** "Surprisingly, we find that allowing AI actually increases completion time by 19%--AI tooling slowed developers down."
- **Quote:** "Before starting tasks, developers forecast that allowing AI will reduce completion time by 24%. After completing the study, developers estimate that allowing AI reduced completion time by 20%."
- **Quote:** "16 developers with moderate AI experience complete 246 tasks in mature projects"
- **URL:** https://arxiv.org/abs/2507.09089 ; blog https://metr.org/blog/2025-07-10-early-2025-ai-experienced-os-dev-study/
- **Publisher / date:** METR (Becker, Rush, Barnes, Rein), arXiv 12 Jul 2025 (v2 25 Jul 2025); blog 10 Jul 2025.
- **CAVEAT, do not cite L7 without it.** METR itself updated on 24 Feb 2026 (https://metr.org/blog/2026-02-24-uplift-update/): "we believe that the data from our new experiment gives us an unreliable signal of the current productivity effect of AI tools." and "Based on conversations with study participants, we believe it is likely that developers are more sped up from AI tools now — in early 2026 — compared to our estimates from early 2025." Their later raw estimate for returning developers was a speedup (-18%, CI -38% to +9%). So the defensible claim is the perception gap in early 2025, not "AI is slower" today.
- **Supports:** reality (felt vs measured gap); the gap, not the sign, is the lasting point.

## L8. Duplicated code up 8x in 2024 (reality) — id [GitClear2025], *secondary*
- **Claim:** GitClear's analysis of 211 million changed lines found duplicated-code blocks rose 8x in 2024 and refactoring fell.
- **Quote:** "The researchers found that the number of code blocks with 5 or more duplicated lines increased by 8 times during 2024."
- **Quote:** "suggest that AI coding assistants are eroding code quality by increasing duplicated and copy/pasted code and decreasing refactoring."
- **URL:** https://www.devclass.com/ai-ml/2025/02/20/ai-is-eroding-code-quality-states-new-in-depth-report/1626250
- **Publisher / date:** DevClass (Tim Anderson), 20 Feb 2025, reporting GitClear's Jan 2025 report.
- **Primary:** https://www.gitclear.com/ai_assistant_code_quality_2025_research returned HTTP 403 (challenge); same limitation already noted in references.md. GitClear sells code-analysis tooling (vendor report, not peer-reviewed).
- **Supports:** reality (slop shows up as maintainability debt).

## L9. DORA: AI amplifies, stability fell in 2024, throughput recovered in 2025 (reality, balanced)
- **Claim (2024):** Google's DORA report found AI raised individual productivity but hurt delivery stability and throughput.
- **Quote:** "AI adoption significantly increases individual productivity, flow, and job satisfaction. However, it also negatively impacts software delivery stability and throughput"
- **URL:** https://dora.dev/research/2024/dora-report/ (DORA, Accelerate State of DevOps Report 2024; page undated).
- **Claim (2025):** AI use reached 90% and its main effect is amplification of what a team already is; throughput now rises with adoption.
- **Quote:** "AI's primary role in software development is that of an amplifier. It magnifies the strengths of high-performing organizations and the dysfunctions of struggling ones."
- **URL:** https://research.google/pubs/dora-2025-state-of-ai-assisted-software-development-report/ (DORA, Google, 2025)
- **Quote:** "AI adoption among software development professionals has surged to 90%, marking a 14% increase from last year."
- **Quote:** "This year's research shows that AI adoption is now linked to higher software delivery throughput"
- **URL:** https://blog.google/technology/developers/dora-report-2025/ (Google, 2025; page undated)
- **Supports:** reality and variance (same tool, different outcome by team setup). Keep the 2024 and 2025 findings together; the 2025 report reverses the throughput finding.

## L10. Same prompt, very different code (variance)
- **Claim:** Asking ChatGPT the same coding task repeatedly often yields different outputs; temperature 0 does not remove it.
- **Quote:** "results from LLMs can be highly unstable; nondeterministically returning very different codes for the same prompt."
- **Quote:** "the ratio of coding tasks with zero equal test output across different requests is 75.76%, 51.00%, and 47.56% for CodeContests, APPS, and HumanEval, respectively."
- **Quote:** "setting the temperature to 0 does not guarantee determinism in code generation"
- **URL:** https://arxiv.org/abs/2308.02828
- **Publisher / date:** Ouyang et al., "An Empirical Study of the Non-determinism of ChatGPT in Code Generation", arXiv 5 Aug 2023 (v2 17 Oct 2024). Older model generation (ChatGPT, 2023); cite as evidence the effect exists, not its current size.
- **Supports:** variance (one prompt is not one result).

## L11. Different models, different hallucination rates (variance) — id [Spracklen2025]
- **Claim:** Across 16 LLMs and 576,000 code samples, hallucinated packages averaged at least 5.2% for commercial and 21.7% for open-source models.
- **Quote:** "Using 16 popular LLMs for code generation and two unique prompt datasets, we generate 576,000 code samples"
- **Quote:** "the average percentage of hallucinated packages is at least 5.2% for commercial models and 21.7% for open-source models"
- **URL:** https://arxiv.org/abs/2406.10279
- **Publisher / date:** Spracklen et al., USENIX Security 2025 (arXiv preprint). Already in references.md; abstract re-read 2026-11-27.
- **Supports:** variance between models on a comparable task.

---

## Dropped / not found
- **Study measuring variance between different models on the same one-shot UI prompt:** none found that I could verify. L6, L10, L11 cover variance on security, determinism and hallucination, not UI. The E6 one-shot data is the evidence for UI variance and should be presented as this study's own result, not as a cited fact.
- **Collins, Merriam-Webster, GitClear primary pages:** HTTP 403 bot challenge; not bypassed. Kept only as *secondary* entries (L2, L3, L8) quoting the named publisher; downgrade or drop if the report must cite primary pages only.
- **Exact publication dates for the Stack Overflow 2025 survey, Veracode report, DORA 2024 page, DORA 2025 blog:** pages show none; only the year is stated. Not guessed.
- **The claim "METR: AI makes developers slower" stated bare:** dropped as an unqualified claim, superseded by METR's own Feb 2026 update; kept only with the L7 caveat.
- **Media/social "one-shot prompt" promise itself** (the influencer pattern from the user's brief): no citable primary source collected; it is an observation, not a sourced fact. If cited, pick concrete posts and archive them.
