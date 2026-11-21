# optimization/study — AI slop in Android apps

**EN.** An empirical study of what AI-assisted first drafts cost an Android app in UI, memory and security, and which of those costs the known techniques do not remove. Demo domain: ShelfTap (NFC library app). Shape: problem, cause, solution, every claim backed by a citation or by our own measurement. Baseline techniques (Compose lists, images, threading, R8, Baseline Profiles, Keystore, TLS, logging) are measured, not only listed; the novel part is the set of problems that come from the generation process.

**VI.** Nghiên cứu thực nghiệm: mã do trợ lý AI sinh ra ở bản nháp đầu gây ra những chi phí gì cho ứng dụng Android về giao diện, bộ nhớ và bảo mật, và phần nào các kỹ thuật tối ưu quen thuộc không xử lý được. Ứng dụng demo: ShelfTap (thư viện NFC). Mỗi nhận định đều có trích dẫn đã kiểm chứng hoặc số đo của chính nghiên cứu; bài viết cuối (ARTICLE.md) viết bằng tiếng Việt.

Source outline: [De_cuong_AI_Coding_UI_UX_Hieu_nang_Bao_mat_ban_mo_rong.docx](De_cuong_AI_Coding_UI_UX_Hieu_nang_Bao_mat_ban_mo_rong.docx) (Cuong). The binding agreement between the parts is the contract kept outside the repo by the orchestrator; its taxonomy IDs and CSV formats are reproduced in 02-taxonomy.md and in each experiment's REPORT.md.

## Read in this order

| File | What it is | State |
|---|---|---|
| [00-protocol.md](00-protocol.md) | pre-registered protocol: research questions, hypotheses with decision rules, five confirmatory tests (Holm), briefs B1-B3 (exact texts), held-out rules, detector validation gates, human control, threats, amendments | frozen v1.0 (commit a5969de); amended in section 15 only; current sha256 in [00-protocol.sha256](00-protocol.sha256), history in [00-protocol.sha256.log](00-protocol.sha256.log) |
| [guardrail/ANDROID-GUARDRAIL.md](guardrail/ANDROID-GUARDRAIL.md) | the guardrail context file given to assistants in the guardrailed arm; written from OWASP and Android documentation only, without reading the detector; seven taxonomy rules deliberately omitted | frozen, sha256 in [guardrail/ANDROID-GUARDRAIL.sha256](guardrail/ANDROID-GUARDRAIL.sha256) |
| [01-related-work.md](01-related-work.md) | related work, search log, explicit contribution claims (primary: H3 decorative-effect cost, H2 in an Android error-fix loop; replication and extension: H1, H4) | written |
| [02-taxonomy.md](02-taxonomy.md) | the frozen problem catalogue: 13 performance, 12 security, 6 UI, 2 staleness IDs (plus the accepted addition X01, used in E4 only), each with cause (evidenced or hypothesised), known fix, source, MASVS/MASWE mapping, where it is measured; mapping of the outline's catalogue | written |
| [references.md](references.md) | every cited source with how it was verified and every figure used; list of candidates dropped as unverifiable | written |

## Experiments (owners and outputs)

| Folder | Experiment | Owner | Read first |
|---|---|---|---|
| [e1-corpus/](e1-corpus/) | E1: first-draft corpus (3 briefs x 3 prompt styles x tool bundles x repetitions), slop detector, human control corpus | beta-f2-2 | REPORT.md when present; results/*.csv |
| [e2-bench/](e2-bench/) | E2: paired A (slop) / B (tuned) benchmarks in the optlab app (android/optlab, android/benchmark), Macrobenchmark and device-independent probes | beta-f2-3 | [e2-bench/REPORT.md](e2-bench/REPORT.md) |
| [e3-security/](e3-security/) | E3: static tools (MobSF, JADX, Lint) plus manual triage of A/B and corpus APKs | beta-f2-4 | REPORT.md when present |
| [e4-pressure/](e4-pressure/) | E4: assistants asked to fix induced Android errors; frozen rubric, two blind raters | beta-f2-4 | [e4-pressure/RUBRIC.md](e4-pressure/RUBRIC.md) |
| [e5-optimizer/](e5-optimizer/) | E5: an assistant as optimiser on a known-ground-truth app | beta-f2-4 | REPORT.md when present |

Heavy state (generated sources, APKs, videos, traces, gradle homes, tool venvs) lives outside the repo in the study work directory chosen by the orchestrator; REPORTs state the paths.

## Rules that make the numbers trustworthy

- No number appears in prose that is not in a results CSV or in [references.md](references.md). The orchestrator checks the article's numbers against the CSVs.
- Claims are worded "assistants emit X at rate r against a human base rate b", never "AI causes X". Causes are labelled hypothesised unless an experiment arm tests them.
- Only the five confirmatory tests of protocol section 3.1 carry interpreted p-values; everything else is an estimate with a confidence interval. Inconclusive and negative results are reported with the same prominence as positive ones.
- Emulator numbers are relative A/B evidence only. There is no physical device, no UX study and no human group in E5; protocol section 13 lists the threats.
- An assistant never certifies its own fix: detectors, tests and tools judge.

## Pipeline

Protocol and guardrail frozen, then E1 generation, E2 runs, E3 on APKs, E4 trials, then the reports, then the article (Vietnamese), the slide outline, the presentation script (outline section VII timing) and the demo video (adb screenrecord of the A/B scroll and search clips from E2).
