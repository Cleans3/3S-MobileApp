
import csv,statistics as S,collections
R='/mnt/01DC198BE9192500/github/Schooldev/3S-MobileApp/optimization/study/e2-bench/results/'
it=list(csv.DictReader(open(R+'iterlog.csv')))
print('iterlog cols',list(it[0].keys()),len(it))
print('status',collections.Counter(r['status'] for r in it))
la=[float(r['loadavg1']) for r in it if r['loadavg1']]
ma=[float(r['mem_avail_mb']) for r in it if r['mem_avail_mb']]
print('loadavg1 n',len(la),'min/med/p95/max',min(la),S.median(la),sorted(la)[int(.95*len(la))],max(la))
print('mem_avail min/med',min(ma),S.median(ma))
print('swapin>0',sum(1 for r in it if r['swapin_pages'] and float(r['swapin_pages'])>0))
by=collections.defaultdict(list)
for r in it: by[(r['factor_id'],r['variant'])].append(float(r['loadavg1']) if r['loadavg1'] else 0)
for k,v in sorted(by.items()): print(k,len(v),'load med',round(S.median(v),2),'max',max(v))
rl=list(csv.DictReader(open(R+'runlog.csv')))
lw=[float(r['lock_wait_s']) for r in rl if r['lock_wait_s']]
print('runlog rows',len(rl),'lock_wait>0:',sum(1 for x in lw if x>0),'max',max(lw),'sum',sum(lw), 'results',collections.Counter(r['result'] for r in rl))
ex=list(csv.DictReader(open(R+'exclusions.csv')))
print('exclusions rows',len(ex)); 
print('host_limited',collections.Counter(r['host_limited'] for r in ex)); print('dropped_total sum',sum(int(r['dropped_total']) for r in ex),'mem',sum(int(r['dropped_mem_lt_1024']) for r in ex),'swapin',sum(int(r['dropped_swapin']) for r in ex))
print([ (r['factor_id'],r['phase'],r['variant'],r['dropped_total'],r['host_limited']) for r in ex if r['host_limited']!='no' or r['dropped_total']!='0'])
