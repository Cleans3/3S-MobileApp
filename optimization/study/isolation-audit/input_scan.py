
import re,pathlib,os
S=pathlib.Path('/mnt/01DC198BE9192500/github/Schooldev/3S-MobileApp/optimization/study')
rx=re.compile(r"caveman|ponytail|context[-_]mode|\bctx_[a-z]+|jcodemunch|\bCleanse\b|herdr|\.agents\b|\brtk\b|laziest|\bYAGNI\b|compact-pilot|model-gate|usage-meter|\btuvn|/home/cleanse",re.I)
dirs=['e1-corpus/prompts','e4-pressure/prompts','e4-pressure/harness','e5-optimizer/harness','e5-optimizer/cases','guardrail','e1-corpus/harness']
tot=0; files=0
for d in dirs:
  p=S/d
  if not p.exists(): print('absent',d); continue
  for dp,dn,fn in os.walk(p):
    for f in fn:
      if f.endswith(('.pyc','.gz','.png','.jpg','.apk','.zip')): continue
      fp=pathlib.Path(dp)/f
      try: t=fp.read_text(errors='replace')
      except: continue
      files+=1
      for m in rx.finditer(t):
        tot+=1; ln=t.count('\n',0,m.start())+1
        print(f"{fp.relative_to(S)}:{ln}: {m.group(0)} | {t[max(0,m.start()-50):m.end()+50].replace(chr(10),' ')}")
print('files',files,'hits',tot)
