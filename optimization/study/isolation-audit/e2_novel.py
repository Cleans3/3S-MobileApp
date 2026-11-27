
import pathlib,re
REPO=pathlib.Path("/mnt/01DC198BE9192500/github/Schooldev/3S-MobileApp")
C=REPO/"optimization/study/e1-corpus/runs"; O=REPO/"android/optlab/src/main/kotlin/shelftap/optlab/corpus"
def src(run):
    p=C/run/"src"; return p if p.exists() else C/run
cases=[("P06","codex-luna-B3-detailed-functional-r1","p06/CoverBitmap.kt",["app/src/main/java/vn/shelftap/app/MainActivity.kt"]),
("P01","claude-haiku-B3-detailed-functional-r1","p01/HistoryList.kt",["app/src/main/java/vn/shelftap/app/ui/history/HistoryScreen.kt"]),
("P09","agy-g38flash-B3-naive-r1","p09/BookDetailSheet.kt",["app/src/main/java/com/example/libraryloans/ui/components/BookDetailSheet.kt"]),
("P13","claude-haiku-B1-detailed-functional-r1","p13/TitleSearch.kt",["app/src/main/java/vn/shelftap/app/data/local/TitleDao.kt","app/src/main/java/vn/shelftap/app/data/CatalogueRepository.kt","app/src/main/java/vn/shelftap/app/ui/SearchScreen.kt"])]
norm=lambda s:re.sub(r'\s+',' ',s.strip())
for f,run,dst,srcs in cases:
    corp=set()
    for s in srcs:
        p=src(run)/s
        if p.exists():
            for l in p.read_text().splitlines(): corp.add(norm(l))
        else: print('missing',p)
    port=(O/dst).read_text().splitlines()
    novel=[(i+1,l) for i,l in enumerate(port) if norm(l) and norm(l) not in corp and not re.match(r'\s*(package|import)\b',l) and norm(l) not in ('}','{','})',')')]
    print(f"== {f} {dst}: {len(port)} port lines, {len(novel)} lines not found verbatim in corpus source(s)")
    for i,l in novel: print(f"  {dst}:{i}: {l.strip()[:150]}")
