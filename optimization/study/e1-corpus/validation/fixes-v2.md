# Detector fixes v2 (for protocol A13)

Scope: false-positive bug fixes only, found by the r1 pass over blind pack v1 (157 items, no arm labels). No rule added, retuned for prevalence or dropped. Mutation validation after the fixes (slop-scan/validate.py): recall 1.00 on all 27 rules, 0 benign false fires (results/validation/mutation.csv). Pre-fix scans are frozen in results/validation/v1/.

| rule | before | after | v1 evidence |
|---|---|---|---|
| P10 | any build script mentioning android.application, including root scripts with `apply false` and convention-plugin ids | module script must apply com.android.application or libs.plugins.android.application on a line without `apply false`; convention plugins are skipped (release type set outside the script) | root-script findings were false positives |
| S03 | cleartextTrafficPermitted="true" matched inside XML comments; http:// literals matched XML feature identifiers (xml.org/sax/...) | XML comments blanked before matching (line numbers kept); xml.org added to the identifier exclusions next to w3.org, apache.org, schemas.android | comment and SAX-feature findings were false positives |
| P05 | scroll position read inside a trailing lambda of a call on a Modifier chain counted as read in composition | such lambdas count as deferred (layout/draw time) | Modifier.libraryListEntrance(...) { index - listState.firstVisibleItemIndex } |
| P09 | repeat(n) with a non-literal n and loops in non-emitting code (onClick validation) counted | repeat() is never a collection iteration; a loop whose next 700 characters call no composable is skipped | star-rating repeat, header validation loop |
| P07 | blocking call flagged in Interceptor/Authenticator classes and in private helpers called only under withContext(IO)/thread | those two cases are exempt (same background-thread contract the runBlocking branch already used) | request() helper called inside withContext(Dispatchers.IO) |

Validation of each changed rule uses fresh items from the v2 findings, excluding every v1 item (alpha, A13 condition 4). Not done yet: the fresh-item draw, pack v2, and the ratings.
