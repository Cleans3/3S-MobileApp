
import difflib,pathlib,re,sys,json
REPO=pathlib.Path("/mnt/01DC198BE9192500/github/Schooldev/3S-MobileApp")
C=REPO/"optimization/study/e1-corpus/runs"; O=REPO/"android/optlab/src/main/kotlin/shelftap/optlab/corpus"
pairs=[
("P08","agy-g38flash-B1-naive-r1","app/src/main/java/com/example/librarycatalogue/ui/components/BookCover.kt","p08/BookCover.kt"),
("P08","agy-g38flash-B1-naive-r1","app/src/main/java/com/example/librarycatalogue/ui/components/BookCard.kt","p08/BookCard.kt"),
("P06","codex-luna-B3-detailed-functional-r1","app/src/main/java/vn/shelftap/app/MainActivity.kt","p06/CoverBitmap.kt"),
("P01","claude-haiku-B3-detailed-functional-r1","app/src/main/java/vn/shelftap/app/ui/history/HistoryScreen.kt","p01/HistoryList.kt"),
("P09/P03P04","agy-g38flash-B3-naive-r1","app/src/main/java/com/example/libraryloans/ui/components/BookDetailSheet.kt","p09/BookDetailSheet.kt"),
("P13","claude-haiku-B1-detailed-functional-r1","app/src/main/java/vn/shelftap/app/data/local/TitleDao.kt","p13/TitleSearch.kt"),
]
out=[]
for f,run,src,dst in pairs:
    p=C/run/"src"/src
    if not p.exists(): p=C/run/src
    a=p.read_text().splitlines(); b=(O/dst).read_text().splitlines() if (O/dst).exists() else None
    if b is None: print(f,'MISSING port',dst); continue
    d=[l for l in difflib.unified_diff(a,b,lineterm='',n=0) if l[:1] in '+-' and not l.startswith(('+++','---'))]
    mech=[];probe=[];ws=[];res=[]
    for l in d:
        t=l[1:].strip()
        if not t: ws.append(l)
        elif re.match(r'(package|import)\b',t): mech.append(l)
        elif re.search(r'Probe|probe',t): probe.append(l)
        else: res.append(l)
    print(f"== {f} {dst} (src lines {len(a)}, port lines {len(b)}): -/+ {len(d)} | pkg/import {len(mech)} | probe {len(probe)} | blank {len(ws)} | residual {len(res)}")
    out.append((f,dst,res))
json.dump(out,open('/tmp/claude-1000/e2res.json','w'))
