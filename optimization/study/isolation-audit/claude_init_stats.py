
import gzip,json,os,collections,pathlib,statistics as S
STUDY=pathlib.Path(__file__).resolve().parent.parent
WORK=pathlib.Path("/mnt/01DC198BE9192500/github/Schooldev/3S-study-work")
rows=[]; subt=collections.Counter(); keys=collections.Counter()
for root,tag in ((STUDY,'R'),(WORK,'W')):
  for dp,dn,fn in os.walk(root):
    if 'node_modules' in dp or '/.git' in dp or 'transcript.jsonl.gz' not in fn: continue
    rel=os.path.relpath(dp,root)
    try: meta=json.load(open(dp+'/meta.json'))
    except: meta={}
    if meta.get('tool')!='claude' and 'claude' not in rel: continue
    first=None; plug=None; sysn=collections.Counter(); ver=None
    for ln in gzip.open(dp+'/transcript.jsonl.gz','rt',errors='replace'):
      try:e=json.loads(ln)
      except: continue
      t=e.get('type'); 
      if t=='system':
        sysn[e.get('subtype')]+=1
        if e.get('subtype')=='init':
          plug='|'.join(f"{p['name']}@{p.get('version','builtin')}" for p in e.get('plugins',[])); ver=e.get('claude_code_version')
      if t=='assistant' and first is None:
        u=(e.get('message') or {}).get('usage') or {}
        first=(u.get('input_tokens') or 0)+(u.get('cache_read_input_tokens') or 0)+(u.get('cache_creation_input_tokens') or 0)
    for k,v in sysn.items(): subt[(tag+':'+rel.split('/')[0]+('/pilot' if 'pilot' in rel else ''),k)]+=v
    rows.append((tag+':'+rel,meta.get('real_home'),first,plug,ver,meta.get('cli_version')))
by=collections.defaultdict(list)
for r in rows: by[(r[1], r[0].split('/')[0] if r[0].startswith('R') else r[0][:24])].append(r)
for k,v in sorted(by.items(),key=str):
  f=[x[2] for x in v if x[2]]
  print(k,len(v),'first-turn input tokens min/med/max',(min(f),S.median(f),max(f)) if f else None,'| plugin sets',len(set(x[3] for x in v)),'| cli',sorted(set(str(x[4]) for x in v)))
print('model-gate 0.4.0 runs:',[x[0] for x in rows if x[3] and 'model-gate@0.4.0' in x[3]])
print('plugin version sets:'); 
for p,c in collections.Counter(x[3] for x in rows).items(): print(c,(p or '')[:260])
print('system subtypes (by experiment):')
for k,v in sorted(subt.items(),key=str): print(k,v)
