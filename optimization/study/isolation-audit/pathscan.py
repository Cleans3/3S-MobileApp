
import gzip,os,re,collections,pathlib,csv
STUDY=pathlib.Path(__file__).resolve().parent.parent
WORK=pathlib.Path("/mnt/01DC198BE9192500/github/Schooldev/3S-study-work")
rx=re.compile(r'(?:/home/cleanse|~|\$HOME|\$\{HOME\})/([A-Za-z0-9_.\-]+)(?:/([A-Za-z0-9_.\-]+))?')
rx2=re.compile(r'/(?:etc|root|usr/local/etc)/(?:claude-code|claude|codex|gemini)[A-Za-z0-9_./\-]*')
agg=collections.defaultdict(set)
for root in (STUDY,WORK):
  for dp,dn,fn in os.walk(root):
    if 'node_modules' in dp or '/.git' in dp: continue
    if 'transcript.jsonl.gz' not in fn and 'transcript_full.jsonl.gz' not in fn: continue
    rid=('R:' if root==STUDY else 'W:')+os.path.relpath(dp,root)
    for f in ('transcript.jsonl.gz','transcript_full.jsonl.gz','stderr.txt.gz'):
      p=os.path.join(dp,f)
      if not os.path.exists(p): continue
      t=gzip.open(p,'rt',errors='replace').read()
      t=re.sub(r'"plugins":\s*\[.*?\],\s*"capabilities"','"capabilities"',t,flags=re.S)
      for m in rx.finditer(t): agg['/home/cleanse/'+m.group(1)+('/'+m.group(2) if m.group(2) and m.group(1) in ('.agents','.claude','.claude-aas-pt','.codex','.gemini','.config','.local') else '')].add(rid)
      for m in rx2.finditer(t): agg[m.group(0)[:60]].add(rid)
for k,v in sorted(agg.items(), key=lambda kv:-len(kv[1])):
  ex=sorted(v)
  print(len(v),k,'|',ex[0][:70],'|',sorted(set(x.split('/')[0]+'/'+x.split('/')[1].split('/')[0] for x in ex))[:6])
