
import csv,statistics as S,collections
rows=list(csv.DictReader(open('transcript_scan.csv')))
ln={r['run_id']:i+2 for i,r in enumerate(rows)}
for k in ['R:e1-corpus/runs/claude-haiku-B3-naive-r2','R:e4-pressure/runs/c1-agy-B-r4']: print(k,'row',ln[k])
real=[r for r in rows if 'pilot/real' in r['run_id']]
for r in real: print('REAL',r['run_id'],'row',ln[r['run_id']],r['tool'],r['markers_found'][:120],'hooks',r['hook_events'],r['hook_additionalContext'],'Cleanse-open',r['opens_with_Cleanse'])
gen=[r for r in rows if r['experiment'] in ('E1','E4','E5','E1(infra-failed)') ]
print('gen rows',len(gen))
tab=collections.defaultdict(lambda:[0,0,0,0,0])
for r in gen:
    t=tab[(r['experiment'],r['kind'],r['tool'])]
    t[0]+=1; t[1]+=int(r['strong_marker_total']>'0' and int(r['strong_marker_total'])>0); t[2]+=int(r['opens_with_Cleanse']=='True'); t[3]+=int(r['mcp_servers'] not in ('0','n/a') or r['slash_commands'] not in ('0','n/a') or r['skills'] not in ('0','n/a')); t[4]+=int(r['hook_events'])
for k,v in sorted(tab.items()): print(k,v)
# article rate
ar=collections.defaultdict(list)
for r in gen:
    if r['article_per100w']: ar[r['tool']].append(float(r['article_per100w']))
for k,v in ar.items(): print('articles/100w',k,len(v),'median',round(S.median(v),2),'p10',round(sorted(v)[len(v)//10],2))
for r in rows:
    if 'pilot/real' in r['run_id'] and r['article_per100w']: print('REAL articles',r['tool'],r['article_per100w'])
print('claude tools sets',collections.Counter(r['claude_tools'] for r in gen if r['tool']=='claude'))
print('opens_with_Cleanse total in gen',sum(r['opens_with_Cleanse']=='True' for r in gen))
print('config hashes in gen',collections.Counter(r['config_hash'][:12] for r in gen))
print('real_home flag in gen',collections.Counter(r['real_home'] for r in gen),'canary_planted',collections.Counter(r['canary_planted'] for r in gen))
print('cli versions',collections.Counter((r['tool'],r['cli_version']) for r in gen))
