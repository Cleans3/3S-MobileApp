# 02-taxonomy.md — the frozen problem catalogue

IDs are frozen (contract §3): the detector, the bench, the triage sheets and the article all use them. A proposed addition goes to alpha as a Q# with sources and takes the prefix X until accepted. Citation keys in brackets are in references.md; every figure and doc statement below is listed there with its check.

How to read the columns:
- **Cause**: "evidenced" = the mechanism is stated by official documentation or a verified paper; "hypothesised" = the claim that an assistant tends to emit the pattern, or that the cause lies in the generation process, is a hypothesis of this study unless an experiment arm tests it (R13).
- **Known fix**: the baseline technique, from the cited documentation.
- **Measured in**: E1 = prevalence in generated code and human control; E2 = A/B cost in the bench (factors fixed by R9); E3 = static-tool and manual triage; E4 = pressure trial; **cited-only** = ID kept, not measured (R14). "Held-out" = omitted from the guardrail (00-protocol §9).

## 1. Performance and memory (P)

| ID | Problem | Cause | Known fix and source | MASVS/MASWE | Measured in |
|---|---|---|---|---|---|
| P01 | LazyColumn/LazyRow/LazyGrid items without `key` | evidenced: item state is keyed by position by default and keys help Compose handle reorderings [Compose-Lists]; hypothesised: `key` is an optional parameter an assistant omits | `items(list, key = { it.id })`, a stable unique key [Compose-Lists] | none (not a security rule) | E1, E2 (with P09) |
| P02 | No `contentType` on lists with different row layouts | evidenced: documented since Compose 1.2 to maximise lazy-layout performance [Compose-Lists]; hypothesised: omitted because optional | set `contentType` per item [Compose-Lists] | none | E1, E2 |
| P03 | Unstable parameters (List/MutableList/var data class) to composables | evidenced: a type is unstable when Compose cannot know whether its value changed [Compose-Stability]; **conditional impact**: from Kotlin 2.0.20 strong skipping is on by default and composables with unstable parameters become skippable [Compose-StrongSkipping], so cost depends on toolchain version (a generator that picks an older Kotlin, or unstable captures, still pays); hypothesised: assistants use List and var freely | immutable types or stable wrappers [Compose-Stability]; strong skipping [Compose-StrongSkipping] | none | E1 (held-out), E2 (with P04) |
| P04 | Heavy work in composition (sort/filter/format/regex without `remember`) | evidenced: composables can run as often as every animation frame, so calculation in the body repeats [Compose-BestPractices] | `remember` for calculations, move work to ViewModel [Compose-BestPractices] | none | E1, E2 |
| P05 | State read too high / fast-changing state without `derivedStateOf` | evidenced: rapidly changing state recomposes more than needed; derivedStateOf and deferred reads limit it [Compose-BestPractices] | `derivedStateOf`, defer reads [Compose-BestPractices] | none | E1 (held-out) only; no E2 factor (R9) |
| P06 | Image decoded at full resolution, or without size/cache | evidenced: decoding can easily raise OutOfMemory, check dimensions first [Load-Bitmaps]; `rememberAsyncImagePainter` loads original dimensions while `AsyncImage` sizes from constraints [Coil-Compose]; Coil provides memory and disk caching and downsampling [Coil]; hypothesised: assistants pick the simpler painter API | `AsyncImage`, request size = display size, caching [Coil-Compose] [Coil] [Load-Bitmaps] | none | E1, E2 |
| P07 | IO/DB/network on the main thread (`runBlocking`, no dispatcher) | evidenced: suspend functions should be main-safe, dispatchers injected, `withContext` for blocking work [Coroutines-BP] | `withContext(dispatcher)`, injected dispatchers [Coroutines-BP] | none | E1, E4 (NetworkOnMainThreadException case) |
| P08 | Visual-effect tax in list items (blur, RenderEffect, shadow/elevation > 2dp, gradient brush, graphicsLayer alpha, infinite animation) | evidenced only for the frame budget: frames must render in under 16 ms for 60 fps, slow frames are jank [Android-Jank]; `graphicsLayer` draws content into a layer [Compose-Graphics]; **hypothesised**: the cause of the "AI look" is the generation process (decorative defaults) and its cost is paid on low-end GPUs; no controlled study of this cost was found (01 section 5), practitioner posts only | flatten effects per item, cache or pre-render, drop per-item effects; the fix is measured in E2 | none | E1 (decorative-effect density, R13), E2 (C5); held-out |
| P09 | `Column` + `verticalScroll` or `forEach` for a long list | evidenced: a non-lazy layout composes and lays out all items whether visible or not [Compose-Lists] | lazy layouts [Compose-Lists] | none | E1, E2 (with P01) |
| P10 | Release build without R8/minify/shrinkResources | evidenced: enabling code and resource optimisation is recommended, both settings [R8] | `isMinifyEnabled`+`isShrinkResources`, or `optimization { enable = true }` on AGP 9.3+ [R8] | MASWE-0059 relates (obfuscation) but this ID is about size and speed, not resilience | E1, E2, E4 (R8 keep-rule crash case) |
| P11 | No Baseline Profile | evidenced: Baseline Profiles improve code execution speed by about 30% from first launch [Baseline-Overview] (Google's figure, not ours) | baselineprofile module and plugin [Baseline-Config] | none | E1, E2 (startup, relative) |
| P12 | Leaks and unscoped work (`GlobalScope`, Activity context held, listeners not removed) | evidenced for GlobalScope: it launches top-level coroutines not limited by structured concurrency [GlobalScope]; memory bloat is one of the performance-bug types studied in [Liu2014] (the study's 70 bugs came from eight apps) | structured scopes | none | **cited-only** (R14) |
| P13 | Search = linear filter on every keystroke, no debounce or index | evidenced: Room maps an FTS4 entity to a SQLite FTS4 table [Room-Fts4]; Paging loads pages from a larger dataset [Paging]; `debounce` operator [Flow-Debounce]; hypothesised: the in-memory filter is what a first draft does | debounce + indexed local store (FTS4) + paging | none | E1, E2 (100k-book search latency) |

## 2. Security and privacy (S)

| ID | Problem | Cause | Known fix and source | MASVS/MASWE | Measured in |
|---|---|---|---|---|---|
| S01 | Token or PII in plaintext SharedPreferences/DataStore/file | hypothesised: the shortest "stay signed in" code writes the token as is; evidenced: Keystore keeps key material non-exportable [Android-Keystore] | key in Keystore, encrypt before writing [Android-Keystore] | MASVS-STORAGE-1; MASWE-0001, MASWE-0002, MASWE-0003 [MASWE-0001] | E1, E3, E4 (Keystore auth case) |
| S02 | Hardcoded secret or API key (source, resources, assets) | evidenced: MASWE-0004 lists source code, assets and resources, string resources as introduction modes [MASWE-0004] | no secrets in the package; per-user tokens at runtime | MASVS-STORAGE-1; MASWE-0004 | E1, E3 |
| S03 | Cleartext traffic allowed | evidenced: cleartext is disabled by default from Android 9 (API 28) and enabled up to API 27, so opting in is an explicit weakening [Android-NSC] | HTTPS; `cleartextTrafficPermitted="false"`; debug-only scope | MASVS-NETWORK-1; MASWE-0026 [MASWE-0026] | E1, E3, E4 (cleartext case) |
| S04 | TLS validation disabled (trust-all TrustManager, ALLOW_ALL verifier) | evidenced: Android documentation calls the do-nothing TrustManager a poor alternative and says not to do it, "even temporarily" [Android-TLS]; broken validation was found in non-browser software including an Android banking app [Georgiev2012]; the problem persists in recent Android apps (static analysis of 15,000 apps found 5,511 recently published apps with vulnerable certificate validation code) [Oltrogge2021] | CA via network security config, `debug-overrides` for debuggable builds only [Android-NSC] | MASVS-NETWORK-1; MASWE-0027 [MASWE-0027] | E1, E3, E4 (self-signed case) |
| S05 | Sensitive logging (Log.*, HttpLoggingInterceptor BODY/HEADERS in release) | evidenced: HEADERS and BODY levels can leak Authorization/Cookie headers and bodies [OkHttp-Logging] | level NONE in release; no tokens/PII in logs | MASVS-STORAGE-2; MASWE-0005 [MASWE-0005] | E1, E3 (H6) |
| S06 | `allowBackup` without exclusion rules | evidenced: the default of `android:allowBackup` is true [AutoBackup] | set explicitly, exclude sensitive data | MASVS-STORAGE-2; MASWE-0006 [MASWE-0006] | E1 (held-out), E3 |
| S07 | Exported component without permission, or `exported` undeclared | evidenced: not setting `android:exported` explicitly risks differing defaults [Android-Exported]; components with intent filters must declare it when targeting Android 12 [Android12] | explicit `exported="false"` unless needed | MASVS-PLATFORM-1; MASWE-0032 [MASWE-0032] | E1, E3 |
| S08 | Weak or deprecated crypto or security library (MD5/SHA-1 for secrets, ECB, static IV, EncryptedSharedPreferences) | evidenced: MASWE-0007 lists AES-ECB and hardcoded/predictable/reused IVs [MASWE-0007]; MASWE-0008 says MD5 and SHA-1 must not be used in security-sensitive contexts [MASWE-0008]; Android recommends AES in CBC or GCM with 256-bit keys and SHA-2 [Android-Crypto]; security-crypto had all APIs deprecated in 1.1.0-alpha07 (9 April 2025) in favour of platform APIs and direct Keystore use [AndroidX-Security]; hypothesised: older code in the training data still shows these | AES-GCM + Keystore, SHA-2 | MASVS-CRYPTO-1; MASWE-0007, MASWE-0008, MASWE-0046 | E1, E3 |
| S09 | Missing input validation | hypothesised | validate and sanitise untrusted input | MASVS-CODE-4; MASWE-0050 [MASWE-0050] | E1 (reserve held-out; counted only if detector validation passes) |
| S10 | No FLAG_SECURE / screenshot protection on sensitive screens | evidenced: `FLAG_SECURE` prevents appearance in screenshots and on non-secure displays [FLAG_SECURE]; judgement call (reading history is PII) | FLAG_SECURE on token/PII screens | MASVS-PLATFORM-3; MASWE-0038 [MASWE-0038] | **cited-only** (R14) |
| S11 | Nonexistent, hallucinated or known-vulnerable dependency | evidenced: package hallucination measured at scale for Python and JavaScript [Spracklen2025] and Rust [Zheng2026]; MASWE-0044 covers known-vulnerable components, not nonexistent ones [MASWE-0044]; no study of Maven coordinates was found (01 section 5) | declared repositories only, verify coordinates | MASVS-CODE-3; MASWE-0044 (partial) | E1 (resolution check, H4) |
| S12 | Debuggable or test configuration in release | evidenced: MASWE-0063 [MASWE-0063] | release build types without debug flags | MASVS-RESILIENCE-4; MASWE-0063 | **cited-only** (R14) |
| X01 | Auth-bound Keystore key downgraded to make an authentication error go away (user authentication requirement turned off, biometric binding removed, invalidation on biometric enrollment turned off). Accepted addition (alpha-approved during the loop); distinct from S01 (plaintext fallback) | evidenced: the Keystore lets you restrict when and how keys can be used, such as requiring user authentication for key use [Android-Keystore]; hypothesised: an assistant relaxes the restriction because that is the shortest way to silence the error | keep the authentication binding; fix the cause of the authentication error | closest MASWE entries: MASWE-0016 Cryptographic Key Access Not Restricted and MASWE-0022 Crypto Keys Not Invalidated on New Biometric Enrollment (MASVS-CRYPTO-2, MASVS-AUTH-2) [MASWE] | E4 (Keystore user-not-authenticated case) |

## 3. UI, UX, accessibility (U)

| ID | Problem | Cause | Known fix and source | Mapping | Measured in |
|---|---|---|---|---|---|
| U01 | Icon-only control without contentDescription | evidenced: `Icon` carries a `contentDescription` property [Compose-Semantics]; Accessibility Scanner checks content labels [A11y-Scanner]; a study of ChatGPT-4 generating Compose screens from 60 screens found 302 violations flagged by Accessibility Scanner [Freitas2025] | `contentDescription` from string resources | existing tool covers it (Accessibility Scanner), see 00-protocol §5(c) | E1 (H5) |
| U02 | Touch target < 48dp | evidenced: minimum size 48dp [Compose-A11y-Defaults]; Accessibility Scanner checks touch target size [A11y-Scanner] | `minimumInteractiveComponentSize`, `sizeIn` | existing tool covers it | E1 |
| U03 | Text contrast < 4.5:1 | evidenced: Accessibility Scanner checks text and image contrast [A11y-Scanner]; Android Studio UI Check Mode flags low colour contrast [A11y-Testing]; the 4.5:1 threshold is the WCAG 2.2 value, not quoted from a verified source here (references.md section F) | contrast-checked palette | existing tool covers it | E1 (reserve held-out; literal colours only) |
| U04 | Hardcoded colours/dimens bypassing the theme (design drift) | hypothesised; source is the study outline's "Visual consistency / Design system drift" row (internal document) | theme tokens | none | E1 (held-out) |
| U05 | Missing loading, empty or error state | hypothesised; source is the outline's "UI states missing" row | explicit UI state model | none | E1 (held-out), H5 |
| U06 | Layout breaks at fontScale 2.0 or small width | hypothesised; outline rows "Responsive layout" and "Typography" | constraints, scroll, test with font scale | none | **cited-only** (R14) |

## 4. Staleness (D)

| ID | Problem | Cause | Known fix and source | Mapping | Measured in |
|---|---|---|---|---|---|
| D01 | Deprecated Android/Jetpack/Kotlin API | evidenced: LLM code completion uses deprecated APIs, measured on Python libraries (7 LLMs, 145 API mappings, 8 libraries, 28,125 prompts) [Wang2025]; Android-specific migration is studied with LLM-based updaters [Mahmud2024] [Mahmud2026]; examples: `AsyncTask` deprecated in API 30 [AsyncTask]; security-crypto deprecated [AndroidX-Security] | documented replacement | MASWE-0046 | E1 (H1) |
| D02 | Dependency >= 1 major or > 12 months behind the latest stable at generation date | hypothesised from the library-evolution mechanism of [Wang2025]; operational definition in 00-protocol §3 (H1) using dated Maven snapshots (R7) | newer stable version | MASWE-0044 applies only when the stale version is known-vulnerable | E1 (held-out) |

## 5. Coverage of the outline's catalogue

The outline (Cuong, internal, sections 3-6) lists problem families. Mapping to frozen IDs, so nothing in the outline is silently dropped. "Outside" = not in the frozen set; covered by citation in the article if at all, never measured here.

| Outline family | ID(s) |
|---|---|
| UI: Visual consistency / design system drift | U04 |
| UI: Responsive layout / adaptive UI; Typography and readability | U06 |
| UI: Color contrast | U03 |
| UI: Touch target / hit area | U02 |
| UI: UI states missing | U05 |
| UI: Accessibility semantics | U01 |
| UI: Visual regression | outside |
| UX: task flow, cognitive load, onboarding, feedback, mental model, usability evidence | outside (UX studies are out of scope, 00-protocol §13) |
| UX: perceived performance | frame and startup metrics in E2 (P-class) |
| Performance: jank, dropped frames | metric of E2 (P01 to P09 are causes) |
| Performance: unnecessary recomposition | P03, P04, P05 |
| Performance: overdraw, heavy images/blur/shadow | P08, P06 |
| Performance: slow startup | P10, P11 (E2 startup metric) |
| Performance: memory leaks | P12 (cited-only) |
| Performance: high memory, bitmap pressure | P06 |
| Performance: main-thread blocking / ANR | P07 |
| Performance: inefficient lists | P01, P02, P09, P13 |
| Performance: API chatter, battery, crash/instability | outside |
| Performance: build size / dependency bloat | P10 (shrinking), D02, S11 |
| Security: STORAGE | S01, S02, S06 |
| Security: CRYPTO | S08 |
| Security: AUTH (authentication, authorization, session) | outside (server side); token handling is S01, S05 |
| Security: NETWORK TLS | S03, S04 |
| Security: NETWORK API boundary | outside (server side) |
| Security: PLATFORM permissions, WebView | outside |
| Security: PLATFORM deep links / IPC | S07 |
| Security: CODE input/output validation | S09 |
| Security: CODE dependencies / supply chain | S11, D02 |
| Security: CODE secrets / logging | S02, S05 |
| Security: RESILIENCE | S12 (cited-only); other resilience controls outside |
| Security: PRIVACY (data minimisation, leakage surfaces) | outside as a rule; PII in logs is S05, and Android data-minimisation findings are in [Liao2026] |
| Security: misconfiguration | S03, S06, S07, S12 |

## 6. Additions and candidates for new IDs

Accepted: **X01** (row in the security table), proposed by beta-f2-4 for E4 and approved by the orchestrator; it is not part of the frozen E1 detector set and is counted only in E4. Not accepted, candidates seen while reading the literature, not added (IDs are frozen): hallucinated Gradle versions (counted inside S11 as `hallucinated-version`, protocol H4); agent-introduced destructive or constraint-violating actions during bug fixing (outside the code-pattern frame, relevant to E4 framing [Hasan2026]). Open coding in E1 (protocol §5(d)) may propose X-prefixed IDs.
