# 01-related-work.md — related work, gap analysis, contribution claims

Written 2026-11-21 by beta-f2-1. Every paper, page and figure below is in references.md with the way it was verified; a figure that could not be matched against its source text is not used (references.md section F lists what was dropped). Citation keys in brackets are the keys of references.md. Preprints (arXiv) are marked as such: they are not peer-reviewed, and several are 2026 preprints whose later versions may change the counts (the version cited is stated in references.md).

## 1. Scope and method

The study asks what AI-assisted first drafts cost an Android app in UI, memory and security, and which of the costs the known techniques do not remove because their cause is in the generation process (00-protocol §1). This review covers six strands: (2) security and quality of AI-generated code, (3) stale knowledge and hallucinated dependencies, (4) the Android baseline: performance bugs, TLS, accessibility, standards, tools, (5) the search log and what was not found, (6) the contribution claims that follow, (7) threats to this review.

Method: targeted web searches (listed with date and outcome in section 5), arXiv API and Crossref lookups for every paper cited, and fetching the official documentation pages. Claims about the **absence** of studies are limited to what the searches in section 5 returned; absence in a search tool's index is not proof of absence in the literature, and the searches should be re-run before submission.

## 2. Security and quality of AI-generated code

### 2.1 Completion assistants and user studies
- [Pearce2022] prompted GitHub Copilot with 89 scenarios built around high-risk weakness classes, producing 1,689 programs, and found approximately 40% of them vulnerable. The tool studied is the 2021 Copilot; the figure is not a statement about current assistants.
- [Perry2023] ran a user study: participants with access to an AI assistant (codex-davinci-002) wrote significantly less secure code than those without, and were more likely to believe their code secure. A 2026 preprint with 44 developers finds that Copilot did not significantly improve secure API usage [Mousavi2026]. Both are human-in-the-loop studies; ours is a no-human, first-draft design (00-protocol §1).

### 2.2 Generated applications at scale
- A vendor report ran 80 curated coding tasks across more than 100 LLMs and states that vulnerabilities were introduced in 45 percent of cases [Veracode2025]. It is a press release of a vendor study, not peer-reviewed, and is cited only as an industry data point.
- For framework-constrained generation (Chrome extensions), 140 prompts and nine LLMs gave vulnerable programs at rates between 18% and 50% [LiuChrome2025] (preprint). Chrome extensions are the closest published analogue of "a framework with its own security model", which is the Android situation, but the study is not about Android.
- For applications built with AI agents in the wild, a 2026 preprint collected 9,041 open-source applications, audited 200 deployed ones, found 1,186 vulnerabilities, and reports that 91.0% of the audited applications contain at least one vulnerability and 65.77% of the vulnerabilities are rated Critical or High [Deng2026]. The agents named in the abstract are Claude Code and Lovable; the figures are those of the version recorded in references.md, and preprint counts can change between versions.

### 2.3 Prompting, iteration and agent behaviour (relevant to H2 and to the guardrail)
- Iteration: 400 code samples across 40 rounds of "improvements" with four prompting strategies showed a 37.6% increase in critical vulnerabilities after five iterations [Shukla2025] (preprint). This is evidence that asking a model to improve code can make it worse; it is not an error-fix loop and not Android.
- Security-aware prompting: twelve programs (six web apps, each in a baseline and a security-aware variant) gave 24 confirmed findings in the security-aware variant against 51 in the baseline, with every variant generated once, which the author calls preliminary and descriptive [Androcec2026] (preprint).
- Guidelines on Android: a 2026 preprint studies data-minimisation practices in Android apps (1,114 open-source apps in a formative study, 9,875 APKs analysed, 31 coding guidelines derived), finds that LLMs reproduce risky practices, and reports that adding its guidelines eliminates those issues across all evaluated models [Liao2026]. This is the closest prior work to our guardrail arm: guidelines injected into an Android generation task. It covers one topic (data minimisation) and, as far as its abstract states, has no held-out rules; our guardrail spans storage, network, logging, performance and accessibility rules and keeps seven rules out of the file to separate instruction-following from generalisation (00-protocol §9).
- Agents and bug fixing: an incident study mined 16,586 GitHub issues of LLM coding tools, confirmed 547 operational safety failures, rated 326 of them high or critical, and reports that over 65% of incidents arise in bug fixing and setup or configuration [Hasan2026] (preprint). This is the nearest published support for studying the error-fix situation, but its failures are operational (constraint violations, destructive operations), not "weakened an Android security setting".

### 2.4 General quality and efficiency
- GitClear's 2025 report, here only through a trade-press account (the primary page blocked automated retrieval), analysed 211 million lines of code and reports that blocks of five or more duplicated lines increased 8 times during 2024 [GitClear2025]. It is a vendor white paper, not peer-reviewed; it motivates "AI-generated code quality" but is not used for any claim of ours.
- On efficiency, EffiBench with 42 LLMs reports that GPT-4-generated code takes on average 3.12 times the execution time of the canonical human solution, and up to 13.89 times the time and 43.92 times the memory in the extreme cases [Huang2024] (preprint, algorithmic problems). It supports the general premise that generated code can be less efficient; it says nothing about UI cost.

## 3. Stale knowledge and hallucinated dependencies

### 3.1 Deprecated APIs and stale versions (H1)
- [Wang2025] (ICSE 2025) is the first evaluation of deprecated API usage in LLM-based code completion: seven LLMs, 145 API mappings from eight Python libraries, 28,125 completion prompts. It is a replication target for H1: our setting is Android/Kotlin/Gradle first drafts and our measure is the share of runs with at least one deprecated-API or stale-version finding, against dated Maven snapshots (00-protocol §3).
- For Android, the literature is about updating deprecated API usages with LLMs: an automated updater evaluated on 360 benchmark usages from 20 deprecated APIs plus 156 usages at API levels 33 and 34 [Mahmud2024] (preprint), and a 2026 study of 1,000 deprecated Android APIs and 4,336 migration examples in which a pattern-guided prompt raises correct updates from 139 to 147 of 156 tasks [Mahmud2026] (preprint). These measure fixing, not what a first draft emits.
- Concrete cases in the Android documentation: AsyncTask is deprecated in API level 30 [AsyncTask]; every API of the androidx security-crypto library (EncryptedSharedPreferences, MasterKey) was deprecated in version 1.1.0-alpha07 of 9 April 2025, in favour of platform APIs and direct use of Android Keystore [AndroidX-Security]. The study contract's wording "deprecated since 1.1.0" should read "since 1.1.0-alpha07".

### 3.2 Hallucinated dependencies (H4)
- [Spracklen2025] (USENIX Security 2025) generated 576,000 code samples with 16 LLMs for Python and JavaScript: the average share of hallucinated packages is at least 5.2% for commercial models and 21.7% for open-source models, with 205,474 unique hallucinated names; in the body, 440,445 (19.7%) of 2.23 million generated packages were hallucinations. A 2026 preprint finds that in Rust the hallucination pattern differs from Python and JavaScript [Zheng2026].
- No study of hallucinated Maven or Gradle coordinates was found (section 5). Ecosystem matters for the risk, not only for the rate: Maven Central requires proof of ownership for a namespace, by a simple verification for personal group IDs on code-hosting services and by a DNS TXT record for domain namespaces [Central-Namespace]; JitPack dependencies take the form com.github.User:Repo:Tag, with coordinates derived from the Git repository [JitPack]. That squatting is therefore harder on Central than via JitPack is our inference from those two pages, labelled hypothesised, not a finding. H4 measures resolution against the repositories a generated project declares.

## 4. The Android baseline layer

### 4.1 Performance
- A study of 70 real-world performance bugs from eight Android apps found three dominant types: GUI lagging (53 of 70, 75.7%), energy leak, and memory bloat (8 of 70, 11.4%) [Liu2014]. A 2024 preprint comparing real-world Android performance issues with the research literature reports that 57.14% of the identified factors have not been examined in academic research and 76.39% remain unaddressed by existing tools [Liao2024]. Together: UI lag and memory are the dominant, long-known Android performance problems, and the research-tool coverage of the real causes is thin; neither study concerns generated code or Jetpack Compose.
- The Compose documentation gives the baseline techniques this study measures: stable unique item keys and contentType [Compose-Lists]; a non-lazy Column composes and lays out all items whether visible or not [Compose-Lists]; `remember` for expensive calculations, derivedStateOf and deferred state reads [Compose-BestPractices]; stability of parameter types [Compose-Stability], with strong skipping enabled by default from Kotlin 2.0.20, which makes composables with unstable parameters skippable [Compose-StrongSkipping] (so P03's cost depends on the toolchain); R8 minification and resource shrinking [R8]; Baseline Profiles, which Google states improve code execution speed by about 30% from the first launch [Baseline-Overview] (Google's figure, not ours); the 16 ms frame budget for 60 fps [Android-Jank]; and the Macrobenchmark metrics StartupTimingMetric and FrameTimingMetric [Macrobenchmark].
- The Compose-specific research literature is thin (section 5). Practitioner material on recomposition and effects exists but was not used as evidence.

### 4.2 Security
- TLS validation has a long, human-written history of failure: broken certificate validation in non-browser software, including an Android banking app [Georgiev2012]; an analysis of SSL (in)security in Android apps [Fahl2012] (cited without figures: its text was not retrievable, references.md section F); and, nine years later, a static analysis of 15,000 apps in which 5,511 recently published apps still contained vulnerable certificate validation code [Oltrogge2021]. Android's own documentation names the do-nothing TrustManager as a poor alternative and says "don't do this, even temporarily" [Android-TLS], and recommends debug-only trust via debug-overrides [Android-NSC]. This is the human base rate behind H2.
- Copy-paste is the human mechanism behind insecure fixes: 15.4% of 1.3 million analysed Android apps contained security-related snippets from Stack Overflow, and 97.9% of those contained at least one insecure snippet [Fischer2017]. E4 uses the top Stack Overflow answers for each error string as the human baseline (00-protocol §4.4).
- Defaults and platform facts used by the taxonomy: cleartext traffic is disabled by default from Android 9 (API 28) and enabled up to API 27 [Android-NSC]; Keystore key material is non-exportable [Android-Keystore]; `android:exported` must be declared explicitly for components with intent filters when targeting Android 12 [Android12] and leaving it unset risks differing defaults [Android-Exported]; HttpLoggingInterceptor's HEADERS and BODY levels can leak Authorization and Cookie headers and bodies [OkHttp-Logging].
- Standards: OWASP MASVS (controls), MASWE (weakness enumeration, MASWE-0001 to MASWE-0078) and MASTG (tests) [MASVS] [MASWE] [MASTG]; the mapping of each taxonomy ID is in 02-taxonomy.md.

### 4.3 Accessibility
- One study had ChatGPT-4 generate Jetpack Compose code from 60 screens in eight Figma templates; Google's Accessibility Scanner flagged 302 violations, and iterative prompting with the scanner's feedback reduced the reported issues by more than 50% [Freitas2025]. Accessibility Scanner checks content labels, touch target size, clickable items, and text and image contrast [A11y-Scanner]; the minimum touch target is 48dp [Compose-A11y-Defaults]. Because existing tools cover U01 to U03, our U-class findings are cross-checked against them rather than claimed as a new detector (00-protocol §5(c)).

### 4.4 Tools used or compared
Android Lint [Android-Lint], detekt [detekt] with compose-rules [compose-rules] and Slack's compose-lints [compose-lints] are the existing static checks against which the study's detector rules are compared; MobSF [MobSF] and JADX [JADX] are used in E3. A custom rule is kept only where no existing tool covers it.

## 5. Search log and what was not found

All searches were run on 2026-11-21 through a web-search tool; "none found" means none in that tool's returned results, and the tool's index covers MSR/ICSE proceedings poorly (its own report). Re-run before submission.

| # | Query (abridged) | Outcome |
|---|---|---|
| 1 | empirical study of Jetpack Compose performance issues and recomposition anti-patterns | no peer-reviewed empirical study found; practitioner posts, a sample repository, a book, one unrelated Java paper |
| 2 | Compose mining study (recomposition, skippable, stability) at MSR/ICSE 2025-2026 | none found; index limitation stated |
| 3 | cost of blur, shadow, gradient, animation on low-end Android (frame time, overdraw) | no peer-reviewed measurement found; developer issue reports and blog posts only |
| 4 | "AI slop" / design homogeneity in AI-generated UIs (purple gradient) | practitioner posts; no controlled study of the cause or the cost found |
| 5 | LLM assistant weakens security (trust-all, cleartext) to fix a build error | no study of that scenario found; closest work is in sections 2.3 and 4.2 |
| 6 | LLM package hallucination for Maven/Gradle/Java/Kotlin coordinates | none found; Python, JavaScript, Rust studies exist [Spracklen2025] [Zheng2026] |
| 7 | LLM-generated Jetpack Compose UI code, benchmark, performance, accessibility | accessibility study found [Freitas2025]; no Compose performance benchmark |
| 8 | LLM and deprecated Android API usage | updater-style studies found [Mahmud2024] [Mahmud2026]; none measuring first-draft usage |
| 9 | AI coding agents, Android security, MASVS | no study of generated Android apps against MASVS found; closest [Liao2026] |
| 10 | vibe-coded applications security empirical | [Deng2026], [Androcec2026], [Shukla2025] |

## 6. Contribution claims (R4)

Primary contributions (novel, per the searches above):
1. **H3, decorative-effect cost in Compose.** We measure the frame and graphics-memory cost of the "AI look" in Compose lists, using A variants taken from code that assistants actually generated (E1) against flattened B variants (E2), and its prevalence (E1). Claim wording: "to our knowledge, and per the searches in section 5, no controlled measurement of this cost has been published; practitioner reports exist". The mechanism ("the generation process favours decorative defaults") is hypothesised; the measured cost is the contribution.
2. **H2 on Android, in an error-fix loop.** E4 puts an assistant in front of five induced Android errors and classifies the fix as secure, insecure workaround, performance-regressing, or none, with a frozen rubric, two blind raters, and a Stack Overflow baseline. Claim wording: "no study of this scenario was found; adjacent evidence is generic (iteration degrades security [Shukla2025], agent failures concentrate in bug fixing [Hasan2026])".

Secondary and partly novel:
3. **Guardrail with held-out rules** on Android (E1 arm): separates instruction-following from generalisation across storage, network, logging, performance and accessibility rules; position against [Liao2026] and [Androcec2026] as in section 2.3.

Replication and extension:
4. **H1** (deprecated APIs and stale versions), replication in the spirit of [Wang2025], extended to Android/Gradle first drafts.
5. **H4** (hallucinated dependencies), replication in the spirit of [Spracklen2025], extended to Maven coordinates resolved against Maven Central, Google Maven and declared extra repositories; JitPack is the exception that makes squatting plausible [JitPack].

Descriptive: **H5** (accessibility and state debt), **H6** (debug leakage).

Methodological: a pre-registered protocol (confirmatory family of five tests, Holm), a human control corpus with an assistant-free stratum, a validated detector (mutation recall, blind second rater, kappa), independence rules between generator, detector and fixer.

Not claimed: that assistants cause the problems (observational against a human base rate), absolute device numbers (emulator, relative evidence), UX effects, a ranking of vendors, results for assistants with retrieval or a human reviewer in the loop.

## 7. Threats to this review
- Search-tool coverage (section 5) limits every "not found" statement; it is a review of what was reachable, not a systematic review.
- Several 2025-2026 sources are preprints and one is a vendor report; numbers can change between versions. References.md records the version read.
- Fahl et al. 2012 and the WCAG criteria could not be retrieved; they are not used for any figure.
- The figures of different studies are not comparable (different models, tasks, languages, detectors); they are cited to place the study, not pooled.
