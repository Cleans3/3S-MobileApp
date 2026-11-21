# E3 statistics (descriptive)

findings 103 on 13 targets, tp 30, fp 73, precision 0.291 (Wilson 95%: (0.212, 0.385))

## by tool x MASVS area

| tool | area | n | tp | fp | precision | lo | hi |
|---|---|---|---|---|---|---|---|
| jadx | MASVS-STORAGE | 1 | 0 | 1 | 0.0 | 0 | 0.793 |
| mobsf | MASVS-CODE | 14 | 11 | 3 | 0.786 | 0.524 | 0.924 |
| mobsf | MASVS-CRYPTO | 2 | 0 | 2 | 0.0 | 0 | 0.658 |
| mobsf | MASVS-NETWORK | 2 | 0 | 2 | 0.0 | 0 | 0.658 |
| mobsf | MASVS-PLATFORM | 22 | 0 | 22 | 0.0 | 0.0 | 0.149 |
| mobsf | MASVS-RESILIENCE | 25 | 13 | 12 | 0.52 | 0.335 | 0.7 |
| mobsf | MASVS-STORAGE | 37 | 6 | 31 | 0.162 | 0.077 | 0.311 |

## by target

| target | kind | tool | style | zero-touch | n | tp | fp | precision |
|---|---|---|---|---|---|---|---|---|
| agy-g38flash-B1-detailed-functional-r1 | debug-type (corpus survivor) | agy | detailed-functional | pass | 12 | 3 | 9 | 0.25 |
| agy-g38flash-B1-guardrailed-r2 | debug-type (corpus survivor) | agy | guardrailed | fail | 11 | 2 | 9 | 0.182 |
| agy-g38flash-B3-detailed-functional-r3 | debug-type (corpus survivor) | agy | detailed-functional | pass | 12 | 3 | 9 | 0.25 |
| agy-g38flash-B3-naive-r2 | debug-type (corpus survivor) | agy | naive | pass | 7 | 3 | 4 | 0.429 |
| claude-haiku-B2-guardrailed-r2 | debug-type (corpus survivor) | claude | guardrailed | pass | 9 | 2 | 7 | 0.222 |
| claude-haiku-B2-naive-r3 | debug-type (corpus survivor) | claude | naive | pass | 9 | 2 | 7 | 0.222 |
| claude-haiku-B3-naive-r3 | debug-type (corpus survivor) | claude | naive | pass | 6 | 3 | 3 | 0.5 |
| codex-luna-B1-naive-r2 | debug-type (corpus survivor) | codex | naive | fail | 5 | 3 | 2 | 0.6 |
| codex-luna-B2-guardrailed-r2 | debug-type (corpus survivor) | codex | guardrailed | pass | 7 | 2 | 5 | 0.286 |
| codex-luna-B3-naive-r1 | debug-type (corpus survivor) | codex | naive | pass | 5 | 4 | 1 | 0.8 |
| optlab-noR8 | release-type (study app) |  |  |  | 8 | 1 | 7 | 0.125 |
| optlab-release | release-type (study app) |  |  |  | 6 | 1 | 5 | 0.167 |
| shelftap-product-release | release-type (product) |  |  |  | 6 | 1 | 5 | 0.167 |

## by generator bundle (survivors)

| tool | scans | tp | fp | tp/scan | precision |
|---|---|---|---|---|---|
| agy | 4 | 11 | 31 | 2.75 | 0.262 |
| claude | 3 | 7 | 17 | 2.33 | 0.292 |
| codex | 3 | 9 | 8 | 3.0 | 0.529 |

## label-rule sensitivity

| reading | tp | fp | precision |
|---|---|---|---|
| as triaged (primary) | 30 | 73 | 0.291 |
| strict: library and debug-artifact fps counted tp (what a scan of the artifact reports as real) | 83 | 20 | 0.806 |
| release-relevant: findings that only describe the debug build type of a debug-type artifact removed | 20 | 62 | 0.244 |
| release-type artifacts only (3 targets) | 3 | 17 | 0.15 |
| debug-type survivors only (10 targets) | 27 | 56 | 0.325 |

fp categories: {'library': 42, 'other': 20, 'debug-artifact': 11}; build-type rows removed in the third reading: 10 tp, 11 fp

## slop-scan vs MobSF (survivors x classes S01-S06,S08,S09)

| comparison | items | both | slop only | mobsf only | neither | kappa | PABAK | agreement |
|---|---|---|---|---|---|---|---|---|
| MobSF raw (any finding of the class) | 80 | 7 | 2 | 6 | 65 | 0.581 | 0.8 | 0.9 |
| MobSF triaged tp | 80 | 6 | 3 | 0 | 71 | 0.78 | 0.925 | 0.963 |

## one tool only (MobSF triaged tp vs slop-scan)

| target | class | found by | evidence |
|---|---|---|---|
| agy-g38flash-B3-detailed-functional-r3 | S05 | slop-scan only | HttpLoggingInterceptor Level.BODY not guarded for release |
| agy-g38flash-B3-detailed-functional-r3 | S09 | slop-scan only | SQL built by interpolation: execSQL("DROP TABLE IF EXISTS $TABLE_LOANS") db.execSQL("DROP |
| claude-haiku-B2-naive-r3 | S01 | slop-scan only | Preferences DataStore key "access_token" stored without encryption |
