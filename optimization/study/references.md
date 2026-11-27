# references.md

Every entry below was checked on 2026-11-21 by the method stated in its "Verified" line: a DOI resolved against Crossref (title, year, pages compared), an arXiv abstract read from the arXiv API, or a web page fetched with HTTP 200 and its text read. **Every figure quoted anywhere in this study's documents is listed under its source with the exact claim; each listed figure was matched by a script against the fetched source text.** Entries that could not be verified are in the last section and are not used. Citation keys in 01-related-work.md and 02-taxonomy.md are the bracketed keys here.

Source classes: **P** peer-reviewed paper or arXiv preprint (preprints are marked), **I** industry/vendor report (not peer-reviewed), **S** standard or official documentation, **M** statistical method, **T** tool.

## A. Papers and preprints (P)

### [Pearce2022]
Pearce, H., Ahmad, B., Tan, B., Dolan-Gavitt, B. et al. "Asleep at the Keyboard? Assessing the Security of GitHub Copilot’s Code Contributions." IEEE Symposium on Security and Privacy (S&P) 2022, pp. 754–768.
- Link: https://doi.org/10.1109/SP46214.2022.9833571 ; https://arxiv.org/abs/2108.09293
- Verified: DOI resolved by Crossref (title, year, pages); abstract read on arXiv v3
- Figure used: 89 scenarios producing 1,689 programs
- Figure used: approximately 40% of those programs vulnerable

### [Perry2023]
Perry, N., Srivastava, M., Kumar, D., Boneh, D. "Do Users Write More Insecure Code with AI Assistants?" ACM CCS 2023, pp. 2785–2799.
- Link: https://doi.org/10.1145/3576915.3623157 ; https://arxiv.org/abs/2211.03622
- Verified: DOI resolved by Crossref; abstract read on arXiv v3
- Figure used: participants with an AI assistant (codex-davinci-002) wrote significantly less secure code and were more likely to believe it secure (qualitative)

### [Fahl2012]
Fahl, S., Harbach, M., Muders, T., Smith, M., Baumgärtner, L., Freisleben, B. "Why Eve and Mallory Love Android: An Analysis of Android SSL (In)Security." ACM CCS 2012, pp. 50–61.
- Link: https://doi.org/10.1145/2382196.2382205
- Verified: DOI resolved by Crossref and Semantic Scholar (title, year, pages). Full text and abstract could not be retrieved automatically (publisher and mirrors returned HTTP 403/405); NO figure from this paper is used
- Figure used: none

### [Georgiev2012]
Georgiev, M., Iyengar, S., Jana, S., Anubhai, R., Boneh, D., Shmatikov, V. "The Most Dangerous Code in the World: Validating SSL Certificates in Non-Browser Software." ACM CCS 2012, pp. 38–49.
- Link: https://doi.org/10.1145/2382196.2382204
- Verified: DOI resolved by Crossref; full text read (hosted PDF)
- Figure used: certificate validation broken in the Chase mobile banking app on Android (named example)

### [Fischer2017]
Fischer, F., Böttinger, K., Xiao, H., Stransky, C., Acar, Y., Backes, M., Fahl, S. "Stack Overflow Considered Harmful? The Impact of Copy&Paste on Android Application Security." IEEE S&P 2017, pp. 121–136.
- Link: https://doi.org/10.1109/SP.2017.31 ; https://arxiv.org/abs/1710.03135
- Verified: DOI resolved by Crossref; abstract read on arXiv
- Figure used: 15.4% of 1.3 million analysed Android apps contained Stack Overflow security snippets
- Figure used: 97.9% of those contained at least one insecure snippet

### [Oltrogge2021]
Oltrogge, M., Huaman, N., Klivan, S., Acar, Y., Backes, M., Fahl, S. "Why Eve and Mallory Still Love Android: Revisiting TLS (In)Security in Android Applications." USENIX Security 2021.
- Link: https://www.usenix.org/conference/usenixsecurity21/presentation/oltrogge
- Verified: USENIX page fetched (HTTP 200), abstract read
- Figure used: static analysis of 15,000 apps found 5,511 recently published apps with vulnerable certificate validation code

### [Liu2014]
Liu, Y., Xu, C., Cheung, S.C. "Characterizing and Detecting Performance Bugs for Smartphone Applications." ICSE 2014, pp. 1013–1024.
- Link: https://doi.org/10.1145/2568225.2568229
- Verified: DOI resolved by Crossref; full text read (author-hosted PDF)
- Figure used: study of 70 real-world performance bugs from eight Android apps; GUI lagging 53 of 70 (75.7%) and memory bloat 8 of 70 (11.4%); GUI lagging, energy leak and memory bloat are the three dominant types

### [Liao2024]
Liao, D., Pan, S., Yang, S., Zhao, Y. et al. "A Comparative Study of Android Performance Issues in Real-world Applications and Literature." arXiv:2407.05090 (preprint).
- Link: https://arxiv.org/abs/2407.05090
- Verified: abstract read on arXiv v3; preprint, venue not verified
- Figure used: 57.14% of identified factors not examined in academic research; 76.39% unaddressed by existing tools

### [Spracklen2025]
Spracklen, J., Wijewickrama, R., Sakib, A.H.M.N., Maiti, A., Viswanath, B., Jadliwala, M. "We Have a Package for You! A Comprehensive Analysis of Package Hallucinations by Code Generating LLMs." USENIX Security 2025, pp. 3687–3706.
- Link: https://arxiv.org/abs/2406.10279 ; https://www.usenix.org/conference/usenixsecurity25/presentation/spracklen
- Verified: USENIX page fetched (HTTP 200: venue, authors); abstract read on arXiv v3; body read in the arXiv PDF
- Figure used: 16 LLMs, 576,000 code samples, two languages
- Figure used: average hallucinated packages at least 5.2% (commercial) and 21.7% (open-source)
- Figure used: 205,474 unique hallucinated package names
- Figure used: 2.23 million packages, of which 440,445 (19.7%) hallucinations (Python+JavaScript)

### [Wang2025]
Wang, C., Huang, K., Zhang, J., Feng, Y. et al. "LLMs Meet Library Evolution: Evaluating Deprecated API Usage in LLM-based Code Completion." ICSE 2025. DOI 10.1109/ICSE55347.2025.00245.
- Link: https://doi.org/10.1109/ICSE55347.2025.00245 ; https://arxiv.org/abs/2406.09834
- Verified: DOI resolved by Crossref; ICSE 2025 research-track page fetched (HTTP 200); abstract read on arXiv v3
- Figure used: seven LLMs, 145 API mappings from eight Python libraries, 28,125 completion prompts; described as the first evaluation study of deprecated API usage in LLM-based code completion

### [Mahmud2024]
Mahmud, T., Duan, B., Che, M., Yasmin, A. et al. "Automated Update of Android Deprecated API Usages with Large Language Models" (GUPPY). arXiv:2411.04387 (preprint).
- Link: https://arxiv.org/abs/2411.04387
- Verified: abstract read on arXiv v1; preprint, venue not verified
- Figure used: evaluation on 360 benchmark API usages from 20 deprecated APIs plus 156 usages at API levels 33 and 34

### [Mahmud2026]
Mahmud, T., Che, M., Yang, G. "A Systematic Study of Deprecated Android API Usage Updates." arXiv:2610.19150 (preprint, 2026).
- Link: https://arxiv.org/abs/2610.19150
- Verified: abstract read on arXiv v1; preprint
- Figure used: 1,000 deprecated Android APIs and 4,336 migration examples; pattern-guided prompt 139 to 147 correct updates on 156 tasks

### [Deng2026]
Deng, J., Fan, Z., Meng, R. "Understanding the (In)Security of Vibe-Coded Applications." arXiv:2606.23130 v4 (preprint, 2026).
- Link: https://arxiv.org/abs/2606.23130
- Verified: abstract read on arXiv v4 (the version cited)
- Figure used: 9,041 open-source apps collected; 200 deployed apps audited; 1,186 vulnerabilities
- Figure used: 91.0% of audited apps contain at least one vulnerability; 65.77% of vulnerabilities rated Critical or High

### [Shukla2025]
Shukla, S., Joshi, H., Syed, R. "Security Degradation in Iterative AI Code Generation: A Systematic Analysis of the Paradox." arXiv:2506.11022 (preprint, 2025).
- Link: https://arxiv.org/abs/2506.11022
- Verified: abstract read on arXiv v2; preprint
- Figure used: 400 code samples, 40 rounds of "improvements", four prompting strategies; 37.6% increase in critical vulnerabilities after five iterations

### [Liao2026]
Liao, D., Pan, S., Xing, Z., Sun, X. "Many a Little Makes a Mickle: A Code-Centric Empirical Study of Data Minimization Principle in Android App Development." arXiv:2606.02960 (preprint, 2026).
- Link: https://arxiv.org/abs/2606.02960
- Verified: abstract read on arXiv v1; preprint
- Figure used: formative study of 1,114 open-source Android apps; analysis of 9,875 APKs; 31 coding guidelines
- Figure used: LLMs reproduce risky practices; adding the guidelines eliminates the issues across all evaluated models

### [Androcec2026]
Andročec, D. "Vibe Coding and Web Application Security: A Twin-Prompt Study." arXiv:2608.20963 (preprint, 2026).
- Link: https://arxiv.org/abs/2608.20963
- Verified: abstract read on arXiv v1; preprint, authors state it is preliminary (one generation per variant)
- Figure used: six web applications, twelve programs; security-aware variant 24 confirmed findings versus 51; descriptive only

### [LiuChrome2025]
Liu, Y., Xing, Z., Pan, S., Tantithamthavorn, C. "When AI Takes the Wheel: Security Analysis of Framework-Constrained Program Generation." arXiv:2510.16823 (preprint, 2025).
- Link: https://arxiv.org/abs/2510.16823
- Verified: abstract read on arXiv v2; preprint
- Figure used: 140 prompts, nine LLMs, Chrome extensions; vulnerable programs at 18%-50%

### [Hasan2026]
Hasan, A.A., Biswas, S. "What Breaks When LLMs Code? Characterizing Operational Safety Failures of Agentic Code Assistants." arXiv:2605.30777 v3 (2026), DOI 10.1145/3832783.3834393.
- Link: https://doi.org/10.1145/3832783.3834393 ; https://arxiv.org/abs/2605.30777
- Verified: abstract read on arXiv v3 (arXiv record lists this DOI; the DOI was not resolved separately)
- Figure used: 16,586 GitHub issues mined, 547 confirmed safety failures, 326 rated high or critical, over 65% in bug fixing and setup/configuration

### [Mousavi2026]
Mousavi, Z., Islam, C., Babar, M.A., Abuadbba, A. "Understanding the Impact of AI Code Assistants on Security API Usage: An Empirical Study." arXiv:2607.11348 (preprint, 2026).
- Link: https://arxiv.org/abs/2607.11348
- Verified: abstract read on arXiv v1; preprint
- Figure used: 44 developers; Copilot does not significantly improve secure API usage

### [Huang2024]
Huang, D., Qing, Y., Shang, W., Cui, H. "EffiBench: Benchmarking the Efficiency of Automatically Generated Code." arXiv:2402.02037 v6 (preprint).
- Link: https://arxiv.org/abs/2402.02037
- Verified: abstract read on arXiv v6; preprint, venue not verified
- Figure used: 42 LLMs; GPT-4 code averages 3.12 times the canonical solution’s execution time; extremes 13.89x time and 43.92x memory

### [Zheng2026]
Zheng, J., Guan, H., Liu, Y. "When LLMs Invent Rust Crates: An Empirical Study of Hallucination Patterns and Mitigation." arXiv:2606.08444 (preprint, 2026).
- Link: https://arxiv.org/abs/2606.08444
- Verified: abstract read on arXiv v2; preprint
- Figure used: first large-scale study of crate hallucination in Rust; pattern differs from Python and JavaScript (qualitative)

### [Freitas2025]
"Image-to-UI Accessibility: Assessing ChatGPT’s Effectiveness in Generating Accessible Android Screens from Figma Templates." Proceedings of the 31st Brazilian Symposium on Multimedia and the Web (WebMedia 2025). (Author list not extracted; cite by title.)
- Link: https://doi.org/10.5753/webmedia.2025.15298 ; https://sol.sbc.org.br/index.php/webmedia/article/view/37974
- Verified: DOI resolved by Crossref search (title, year, venue); landing page fetched (HTTP 200) and abstract read
- Figure used: ChatGPT-4, Jetpack Compose from 60 screens in 8 Figma templates; 302 accessibility violations flagged by Accessibility Scanner; more than 50% reduction after iterative prompting

## B. Industry and vendor reports, announcements (I), not peer-reviewed

### [Veracode2025]
Veracode. "AI-Generated Code Poses Major Security Risks in Nearly Half of All Development Tasks, Veracode Research Reveals" (2025 GenAI Code Security Report), press release, 30 July 2025. Vendor report, not peer-reviewed.
- Link: https://www.veracode.com/press-release/ai-generated-code-poses-major-security-risks-in-nearly-half-of-all-development-tasks-veracode-research-reveals/
- Verified: page fetched (HTTP 200), text read
- Figure used: 80 curated coding tasks, more than 100 LLMs, vulnerabilities introduced in 45 percent of cases

### [GitClear2025]
Anderson, T. "AI is eroding code quality, states new in-depth report." DevClass, 20 February 2025, reporting GitClear’s AI Copilot Code Quality 2025 research. SECONDARY report of a vendor white paper (the primary page blocks automated retrieval); not peer-reviewed.
- Link: https://www.devclass.com/ai-ml/2025/02/20/ai-is-eroding-code-quality-states-new-in-depth-report/1626250
- Verified: DevClass page fetched (HTTP 200), text read; primary GitClear page returned HTTP 403
- Figure used: analysis of 211 million lines of code; blocks of 5 or more duplicated lines increased 8 times during 2024 (as reported)

### [ChatGPT2022]
OpenAI. "Introducing ChatGPT." 30 November 2022.
- Link: https://openai.com/index/chatgpt/
- Verified: page fetched (HTTP 200)
- Figure used: dated 30 November 2022

## C. Standards and official documentation (S)
All fetched 2026-11-21, HTTP 200, page text read; the "Fact used" lines were matched against the page text.

### [MASVS]
OWASP MASVS (Mobile Application Security Verification Standard), v2 control groups STORAGE, CRYPTO, AUTH, NETWORK, PLATFORM, CODE, RESILIENCE, PRIVACY (re-read 2026-11-21 for ARTICLE.md).
- Link: https://mas.owasp.org/MASVS/
- Fact used: the eight control groups MASVS-STORAGE, -CRYPTO, -AUTH, -NETWORK, -PLATFORM, -CODE, -RESILIENCE, -PRIVACY, each with a one-line scope

### [MASWE]
OWASP MASWE (Mobile Application Security Weakness Enumeration), weakness list MASWE-0001 to MASWE-0078 as published on 2026-11-21.
- Link: https://mas.owasp.org/MASWE/
- Fact used: page identity only (title, scope)

### [MASTG]
OWASP MASTG (Mobile Application Security Testing Guide).
- Link: https://mas.owasp.org/MASTG/
- Fact used: page identity only (title, scope)

### [MASWE-0001]
MASWE-0001 Sensitive Data Stored Unencrypted in Private Storage.
- Link: https://mas.owasp.org/MASWE/MASVS-STORAGE/MASWE-0001/
- Fact used: page identity only (title, scope)

### [MASWE-0004]
MASWE-0004 Sensitive Data Hardcoded in the App Package.
- Link: https://mas.owasp.org/MASWE/MASVS-STORAGE/MASWE-0004/
- Fact used: secrets can sit in source code, assets and resources, string resources

### [MASWE-0005]
MASWE-0005 Insertion of Sensitive Data into Logs.
- Link: https://mas.owasp.org/MASWE/MASVS-STORAGE/MASWE-0005/
- Fact used: page identity only (title, scope)

### [MASWE-0006]
MASWE-0006 Sensitive Data Not Excluded From Backup.
- Link: https://mas.owasp.org/MASWE/MASVS-STORAGE/MASWE-0006/
- Fact used: page identity only (title, scope)

### [MASWE-0007]
MASWE-0007 Improper Encryption.
- Link: https://mas.owasp.org/MASWE/MASVS-CRYPTO/MASWE-0007/
- Fact used: NIST disallows AES-ECB (deterministic)
- Fact used: predictable, hardcoded or reused IVs listed as a weakness

### [MASWE-0008]
MASWE-0008 Improper Hashing.
- Link: https://mas.owasp.org/MASWE/MASVS-CRYPTO/MASWE-0008/
- Fact used: MD5 and SHA-1 must not be used in a security-sensitive context

### [MASWE-0026]
MASWE-0026 Network Traffic Not Encrypted.
- Link: https://mas.owasp.org/MASWE/MASVS-NETWORK/MASWE-0026/
- Fact used: page identity only (title, scope)

### [MASWE-0027]
MASWE-0027 Insecure Certificate Validation.
- Link: https://mas.owasp.org/MASWE/MASVS-NETWORK/MASWE-0027/
- Fact used: page identity only (title, scope)

### [MASWE-0032]
MASWE-0032 Insecure Intents.
- Link: https://mas.owasp.org/MASWE/MASVS-PLATFORM/MASWE-0032/
- Fact used: page identity only (title, scope)

### [MASWE-0038]
MASWE-0038 Insufficient Protection of Sensitive Data from Screenshots or Screen Recordings.
- Link: https://mas.owasp.org/MASWE/MASVS-PLATFORM/MASWE-0038/
- Fact used: page identity only (title, scope)

### [MASWE-0044]
MASWE-0044 Dependencies with Known Vulnerabilities.
- Link: https://mas.owasp.org/MASWE/MASVS-CODE/MASWE-0044/
- Fact used: page identity only (title, scope)

### [MASWE-0046]
MASWE-0046 Use of Deprecated APIs or Functionality.
- Link: https://mas.owasp.org/MASWE/MASVS-CODE/MASWE-0046/
- Fact used: page identity only (title, scope)

### [MASWE-0050]
MASWE-0050 Unsafe Handling of Untrusted Data.
- Link: https://mas.owasp.org/MASWE/MASVS-CODE/MASWE-0050/
- Fact used: page identity only (title, scope)

### [MASWE-0063]
MASWE-0063 Debug Mechanisms Not Disabled.
- Link: https://mas.owasp.org/MASWE/MASVS-RESILIENCE/MASWE-0063/
- Fact used: page identity only (title, scope)

### [Android-NSC]
Network security configuration, Android Developers.
- Link: https://developer.android.com/privacy-and-security/security-config
- Fact used: cleartext disabled by default from Android 9 (API 28); enabled up to API 27
- Fact used: debug-overrides trusts debug CAs only when android:debuggable is true

### [Android-TLS]
Security with network protocols, Android Developers.
- Link: https://developer.android.com/privacy-and-security/security-ssl
- Fact used: a TrustManager that does nothing: "don't do this, even temporarily"

### [Android-Keystore]
Android Keystore system, Android Developers.
- Link: https://developer.android.com/privacy-and-security/keystore
- Fact used: key material remains non-exportable
- Fact used: the Keystore lets you restrict when and how keys can be used, such as requiring user authentication for key use

### [Android-Crypto]
Cryptography, Android Developers.
- Link: https://developer.android.com/privacy-and-security/cryptography
- Fact used: recommended: AES in CBC or GCM mode with 256-bit keys; SHA-2 family digests

### [Android-Exported]
android:exported, Android Developers (OWASP category MASVS-PLATFORM).
- Link: https://developer.android.com/privacy-and-security/risks/android-exported
- Fact used: not setting exported explicitly risks differing defaults

### [Android12]
Behavior changes: Apps targeting Android 12, Android Developers.
- Link: https://developer.android.com/about/versions/12/behavior-changes-12
- Fact used: intent-filter components must declare android:exported explicitly when targeting Android 12

### [AndroidX-Security]
Security release notes (androidx.security), Android Developers.
- Link: https://developer.android.com/jetpack/androidx/releases/security
- Fact used: security-crypto 1.1.0-alpha07, 9 April 2025: all APIs deprecated in favour of platform APIs and direct Android Keystore use

### [AsyncTask]
AsyncTask, Android API reference.
- Link: https://developer.android.com/reference/android/os/AsyncTask
- Fact used: deprecated in API level 30

### [AutoBackup]
Back up user data with Auto Backup, Android Developers.
- Link: https://developer.android.com/identity/data/autobackup
- Fact used: android:allowBackup default is true

### [FLAG_SECURE]
WindowManager.LayoutParams.FLAG_SECURE, Android API reference.
- Link: https://developer.android.com/reference/android/view/WindowManager.LayoutParams#FLAG_SECURE
- Fact used: FLAG_SECURE prevents screenshots and non-secure displays

### [OkHttp-Logging]
OkHttp logging-interceptor README (square/okhttp).
- Link: https://raw.githubusercontent.com/square/okhttp/master/okhttp-logging-interceptor/README.md
- Fact used: HEADERS/BODY levels can leak Authorization/Cookie and bodies

### [Compose-Lists]
Lazy lists and lazy grids, Jetpack Compose, Android Developers.
- Link: https://developer.android.com/develop/ui/compose/lists
- Fact used: non-lazy Column composes all items whether visible or not
- Fact used: item keys: stable and unique key per item
- Fact used: lazy components only compose and lay out the items visible in the viewport (added 2026-11-21 for ARTICLE.md)
- Fact used: contentType since Compose 1.2
- Fact used: by default item state is keyed by position; keys help Compose handle reorderings correctly

### [Compose-BestPractices]
Follow best practices, Jetpack Compose performance, Android Developers.
- Link: https://developer.android.com/develop/ui/compose/performance/bestpractices
- Fact used: remember to minimise expensive calculations
- Fact used: derivedStateOf to limit recompositions
- Fact used: defer state reads
- Fact used: composable functions can run very frequently, as often as for every frame of an animation, so do little calculation in the body (added 2026-11-21 for ARTICLE.md)
- Fact used: deferring state reads makes Compose re-run the minimum possible code on recomposition (added 2026-11-21 for ARTICLE.md)

### [Compose-Stability]
Stability in Compose, Android Developers.
- Link: https://developer.android.com/develop/ui/compose/performance/stability
- Fact used: a type is stable if it is immutable or Compose can know whether its value changed between recompositions; unstable if Compose cannot know

### [Compose-StrongSkipping]
Strong skipping mode, Android Developers.
- Link: https://developer.android.com/develop/ui/compose/performance/stability/strongskipping
- Fact used: strong skipping is enabled by default from Kotlin 2.0.20
- Fact used: with strong skipping, composables with unstable parameters become skippable
- Fact used: unstable parameters are compared by instance equality (===), stable ones by Object.equals() (added 2026-11-21 for ARTICLE.md)

### [Compose-Graphics]
Graphics modifiers, Jetpack Compose, Android Developers.
- Link: https://developer.android.com/develop/ui/compose/graphics/draw/modifiers
- Fact used: Modifier.graphicsLayer draws content into a layer

### [Compose-A11y-Defaults]
API defaults (touch target sizes), Jetpack Compose accessibility, Android Developers.
- Link: https://developer.android.com/develop/ui/compose/accessibility/api-defaults
- Fact used: minimum touch target 48dp

### [Compose-Semantics]
Semantics, Jetpack Compose accessibility, Android Developers.
- Link: https://developer.android.com/develop/ui/compose/accessibility/semantics
- Fact used: Icon carries a contentDescription semantic property

### [A11y-Scanner]
Get started with Accessibility Scanner, Android Accessibility Help.
- Link: https://support.google.com/accessibility/android/answer/6376570
- Fact used: checks content labels, touch target size, clickable items, text and image contrast

### [A11y-Testing]
Test your app’s accessibility, Android Developers (UI Check Mode flags low contrast).
- Link: https://developer.android.com/guide/topics/ui/accessibility/testing
- Fact used: UI Check Mode flags low colour contrast

### [Baseline-Overview]
Baseline Profiles overview, Android Developers.
- Link: https://developer.android.com/topic/performance/baselineprofiles/overview
- Fact used: Baseline Profiles improve code execution speed by about 30% from first launch
- Fact used: with a Baseline Profile, ART can optimize specified code paths through ahead-of-time (AOT) compilation, from the first launch (added 2026-11-21 for ARTICLE.md)

### [Baseline-Config]
Configure Baseline Profile generation, Android Developers.
- Link: https://developer.android.com/topic/performance/baselineprofiles/configure-baselineprofiles
- Fact used: page identity only (title, scope)

### [R8]
Enable app optimization with R8, Android Developers.
- Link: https://developer.android.com/topic/performance/app-optimization/enable-app-optimization
- Fact used: enable both isMinifyEnabled and isShrinkResources; optimization { enable = true } on AGP 9.3+
- Fact used: the page lists faster startup time, reduced memory usage, improved rendering and runtime performance and fewer ANRs as benefits (added 2026-11-21 for ARTICLE.md)

### [Coroutines-BP]
Best practices for coroutines in Android, Android Developers.
- Link: https://developer.android.com/kotlin/coroutines/coroutines-best-practices
- Fact used: inject dispatchers; suspend functions should be main-safe

### [Load-Bitmaps]
Loading large bitmaps efficiently, Android Developers.
- Link: https://developer.android.com/topic/performance/graphics/load-bitmap
- Fact used: decoding can easily throw OutOfMemory; check dimensions first
- Fact used: with inSampleSize 4 a bitmap of about 512x384 uses 0.75 MB rather than 12 MB for the full image (assuming ARGB_8888) (added 2026-11-21 for ARTICLE.md)

### [Coil]
Coil image loading library (overview and Compose pages).
- Link: https://coil-kt.github.io/coil/
- Fact used: memory and disk caching, downsampling

### [Coil-Compose]
Coil Compose: AsyncImage sizing.
- Link: https://coil-kt.github.io/coil/compose/
- Fact used: AsyncImage determines load size from composable constraints; rememberAsyncImagePainter loads original dimensions

### [Paging]
Paging library overview, Android Developers.
- Link: https://developer.android.com/topic/libraries/architecture/paging/v3-overview
- Fact used: Paging loads and displays pages of data from a larger dataset

### [Room-Fts4]
Fts4 annotation, Room API reference.
- Link: https://developer.android.com/reference/androidx/room/Fts4
- Fact used: Room maps an FTS4 entity to a SQLite FTS4 virtual table

### [Flow-Debounce]
debounce, kotlinx.coroutines Flow API.
- Link: https://kotlinlang.org/api/kotlinx.coroutines/kotlinx-coroutines-core/kotlinx.coroutines.flow/debounce.html
- Fact used: page identity only (title, scope)

### [GlobalScope]
GlobalScope, kotlinx.coroutines API (@DelicateCoroutinesApi).
- Link: https://kotlinlang.org/api/kotlinx.coroutines/kotlinx-coroutines-core/kotlinx.coroutines/-global-scope/
- Fact used: GlobalScope launches top-level coroutines not limited by structured concurrency

### [Android-Jank]
Slow rendering (jank), Android Developers.
- Link: https://developer.android.com/topic/performance/vitals/render
- Fact used: frames must render in under 16 ms for 60 fps; slow frames are jank
- Fact used: frozen frames are UI frames that take longer than 700 ms to render (added 2026-11-21 for ARTICLE.md)
- Fact used: slow frames, frozen frames and ANRs are all different forms of jank (added 2026-11-21 for ARTICLE.md)
- Fact used: FrameTimeline in Perfetto can help in tracking slow or frozen frames (added 2026-11-21 for ARTICLE.md)

### [Macrobenchmark]
Macrobenchmark overview, Android Developers.
- Link: https://developer.android.com/topic/performance/benchmarking/macrobenchmark-overview
- Fact used: StartupTimingMetric and FrameTimingMetric

### [Android-Deps]
Add build dependencies, Android Developers.
- Link: https://developer.android.com/studio/build/dependencies
- Fact used: page identity only (title, scope)

### [Android-Lint]
Improve your code with lint checks, Android Studio docs.
- Link: https://developer.android.com/studio/write/lint
- Fact used: page identity only (title, scope)

### [Compose-Phases]
Jetpack Compose phases, Android Developers. Added 2026-11-21 for ARTICLE.md.
- Link: https://developer.android.com/develop/ui/compose/phases
- Fact used: Compose has three main phases of a frame: Composition (what UI to show), Layout (where to place UI: measurement and placement), Drawing (how it renders)
- Fact used: Compose tracks state reads within the different phases and performs only the minimum amount of work required to update the UI

### [Android-ANR]
ANRs, Android Developers (App quality). Added 2026-11-21 for ARTICLE.md.
- Link: https://developer.android.com/topic/performance/vitals/anr
- Fact used: an ANR is triggered when the app has not responded to an input event within 5 seconds

### [Android-Launch]
App startup time, Android Developers. Added 2026-11-21 for ARTICLE.md.
- Link: https://developer.android.com/topic/performance/vitals/launch-time
- Fact used: three startup states (cold, warm, hot); TTID is the time to display the first frame and TTFD the time until the app is fully interactive

### [Android-Memory]
Overview of memory management, Android Developers. Added 2026-11-21 for ARTICLE.md.
- Link: https://developer.android.com/topic/performance/memory-overview
- Fact used: the system sets a hard heap size limit per app and exceeding it can raise OutOfMemoryError; the logical size of the heap is not the amount of physical memory the heap uses
- Fact used: memory an app modifies remains resident in RAM and cannot be paged out; the only way to release it is to release object references so that garbage collection can reclaim it

### [Central-Namespace]
Sonatype. "Register a Namespace", Maven Central (Central Portal) documentation.
- Link: https://central.sonatype.org/register/namespace/
- Fact used: personal group IDs on code-hosting services (io.github.<user>, io.gitlab.<user>) require a simple verification to prove ownership; domain namespaces are verified with a DNS TXT record

### [JitPack]
JitPack, package repository for Git (home page).
- Link: https://jitpack.io/
- Fact used: dependencies take the form com.github.User:Repo:Tag from the repository https://jitpack.io

## D. Statistical methods (M)
DOIs resolved by Crossref on 2026-11-21 (title, year, volume, pages compared); Holm 1979 has no DOI at Crossref, its bibliographic details were confirmed by two independent web sources (search result citing the hosted PDF and the BCO-DMO record), JSTOR 4615733.

### [Wilson1927]
Wilson, E.B. "Probable Inference, the Law of Succession, and Statistical Inference." Journal of the American Statistical Association 22(158):209–212, 1927.
- Link: https://doi.org/10.1080/01621459.1927.10502953

### [Holm1979]
Holm, S. "A Simple Sequentially Rejective Multiple Test Procedure." Scandinavian Journal of Statistics 6(2):65–70, 1979. JSTOR 4615733.
- Link: https://www.jstor.org/stable/4615733

### [Cohen1960]
Cohen, J. "A Coefficient of Agreement for Nominal Scales." Educational and Psychological Measurement 20(1):37–46, 1960.
- Link: https://doi.org/10.1177/001316446002000104

### [Landis1977]
Landis, J.R., Koch, G.G. "The Measurement of Observer Agreement for Categorical Data." Biometrics 33(1):159–174, 1977.
- Link: https://doi.org/10.2307/2529310

### [Cliff1993]
Cliff, N. "Dominance statistics: Ordinal analyses to answer ordinal questions." Psychological Bulletin 114(3):494–509, 1993.
- Link: https://doi.org/10.1037/0033-2909.114.3.494

### [Efron1979]
Efron, B. "Bootstrap Methods: Another Look at the Jackknife." The Annals of Statistics 7(1):1–26, 1979.
- Link: https://doi.org/10.1214/aos/1176344552

### [Hanley1983]
Hanley, J.A., Lippman-Hand, A. "If Nothing Goes Wrong, Is Everything All Right? Interpreting Zero Numerators." JAMA 249(13):1743–1745, 1983.
- Link: https://doi.org/10.1001/jama.1983.03330370053031

### [Mann1947]
Mann, H.B., Whitney, D.R. "On a Test of Whether one of Two Random Variables is Stochastically Larger than the Other." The Annals of Mathematical Statistics 18(1):50–60, 1947.
- Link: https://doi.org/10.1214/aoms/1177730491

## E. Tools (T)
Repository or project pages fetched with HTTP 200 on 2026-11-21; used only as named tools, no claims about their performance are made.

### [MobSF]
Mobile Security Framework (MobSF).
- Link: https://github.com/MobSF/Mobile-Security-Framework-MobSF

### [JADX]
JADX dex to Java decompiler.
- Link: https://github.com/skylot/jadx

### [detekt]
detekt static analysis for Kotlin.
- Link: https://detekt.dev/

### [compose-rules]
compose-rules: lint rules for ktlint/detekt for healthier Compose usage (mrmans0n).
- Link: https://github.com/mrmans0n/compose-rules

### [compose-lints]
compose-lints: Lint checks for Compose (Slack).
- Link: https://github.com/slackhq/compose-lints

### [MASTG-repo]
OWASP/mastg repository.
- Link: https://github.com/OWASP/mastg

## G. Lead-in sources for the plain-language report (added 2026-11-27)

Verified 2026-11-27 by beta-fg-3 (each quote matched by script as an exact substring of the fetched page; full quotes in report/sources-leadin.md); alpha re-fetched the Stack Overflow, Veracode and arXiv pages and re-matched five quotes. Used only in report/index.html section 1. *Secondary* = the publisher's own page returned HTTP 403 to automated clients, so a named news outlet quoting it is cited instead.

### [Karpathy2025]
Karpathy, A. Post on X introducing "vibe coding", 2 February 2025. Social media post (I).
- Link: https://x.com/karpathy/status/1886192184808149383
- Verified: x.com page text (first quote); full text via https://api.fxtwitter.com/karpathy/status/1886192184808149383
- Figure used: none; quoted phrases "vibe coding", "Accept All", "throwaway weekend projects"

### [Collins2025]
Collins Dictionary, Word of the Year 2025 "vibe coding", announced 6 November 2025. *Secondary*: Irish Examiner, https://www.irishexaminer.com/news/arid-41737758.html ; Gulf News.
- Verified: news pages fetched (HTTP 200); collinsdictionary.com/woty returned HTTP 403, not read
- Figure used: none

### [SOSurvey2025]
Stack Overflow, 2025 Developer Survey, section AI. https://survey.stackoverflow.co/2025/ai (no exact date on the page).
- Verified: page fetched (HTTP 200), quotes matched
- Figure used: 84% of respondents use or plan to use AI tools (76% the year before)
- Figure used: 46% distrust the accuracy of AI tools, 33% trust it
- Figure used: 66% cite "AI solutions that are almost right, but not quite" as the biggest frustration

### [MerriamWebster2025]
Merriam-Webster, Word of the Year 2025 "slop". *Secondary*: PBS NewsHour (AP), 15 December 2025, https://www.pbs.org/newshour/nation/merriam-websters-word-of-the-year-for-2025-is-ais-slop
- Verified: PBS page fetched (HTTP 200); merriam-webster.com returned HTTP 403, not read
- Figure used: none; definition quoted

### [Ouyang2023]
Ouyang, S., Zhang, J. M., Harman, M., Wang, M. "An Empirical Study of the Non-determinism of ChatGPT in Code Generation." arXiv:2308.02828 (2023, v2 2024). Preprint (P).
- Link: https://arxiv.org/abs/2308.02828
- Verified: abstract read on arXiv
- Figure used: none in the report (qualitative: the same prompt can return very different code; temperature 0 does not guarantee determinism)

[Veracode2025] (section B) is also cited in the report lead-in with its existing figure (45 percent).

## F. Candidates examined and NOT used (verification failed or incomplete)

| Candidate | Why it is not used |
|---|---|
| Fahl et al. 2012, figures (apps analysed, share vulnerable) | The paper itself and its abstract could not be retrieved (ACM, typeset.io and slide mirrors returned HTTP 403/405 or non-PDF). Only the bibliographic record is cited. Oltrogge et al. 2021 supplies the verified Android TLS figures instead |
| Georgiev et al. 2012, aggregate figures | Only the Android-specific named example is quoted (verified in the PDF text) |
| GitClear 2025, primary white paper | primary page blocked automated retrieval (HTTP 403); only the DevClass report is cited, as a secondary source, and only the two figures it states |
| Veracode 2025, per-language breakdown | seen only in a search summary, not in the fetched press release; only the three figures in the press release are used |
| WCAG 2.2 success criteria (contrast, target size, non-text content) | w3.org returned HTTP 403 and the Wayback copy HTTP 429; no WCAG figure is quoted. Android Accessibility Scanner and Compose docs (section C) carry the same checks |
| Peer-reviewed Compose component-performance experiment (reported in a search summary, JCSI 2025) | the host page returned HTTP 403; not read, not cited. The absence-of-studies statement in 01 section 5 is restricted to what the searches listed there returned |
| Wang et al. 2025, deprecated-usage-rate percentages | the rate values could not be tied to a single clearly labelled table from the extracted PDF text; only the study scale (verified) is quoted |
| "Spracklen: Java (Maven) results" | not visible in any retrieved text; not claimed |
| Contract wording "EncryptedSharedPreferences deprecated since security-crypto 1.1.0" | the release notes say all APIs were deprecated in 1.1.0-alpha07 (9 April 2025); 1.1.0-beta01 (4 June 2025) repeats it. Use that wording |
