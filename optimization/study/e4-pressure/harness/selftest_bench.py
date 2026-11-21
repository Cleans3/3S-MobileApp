#!/usr/bin/env python3
"""Self-check of gen_launch.bench_shared (no AI, no benchmark): nested caller, the Q#dl-1 3-way wait, and the bench.hold file. Exits non-zero on a hang or a wrong result.
  python3 -I selftest_bench.py"""
import pathlib, subprocess, sys, tempfile, threading, time
sys.path.insert(0, str(pathlib.Path(__file__).resolve().parent))
import fcntl, gen_launch as g

d = pathlib.Path(tempfile.mkdtemp(dir=str(g.WORK / "tmp"))); g.WORK = d; g.HOLD_POLL = 0.2
(d / "bench.lock").touch(); (d / "bench.turnstile").touch()
def py(code): return subprocess.Popen([sys.executable, "-c", code])
def ok(cond, msg):
    if not cond: print("FAIL:", msg); sys.exit(1)
    print("ok:", msg)

# 1) a caller that already holds LOCK_SH (e1 generate.py) must not wait on a turnstile or a hold held by someone else
p = py(f"import fcntl,time;f=open({str(d/'bench.turnstile')!r},'a');fcntl.flock(f,fcntl.LOCK_EX);time.sleep(20)"); time.sleep(0.5)
(d / "bench.hold").write_text(str(p.pid))
ext = open(d / "bench.lock", "a"); fcntl.flock(ext, fcntl.LOCK_SH); t0 = time.time()
with g.bench_shared(): pass
ok(time.time() - t0 < 2, "nested caller does not block on turnstile or hold"); fcntl.flock(ext, fcntl.LOCK_UN); ext.close(); (d / "bench.hold").unlink(); p.kill(); p.wait()

# 2) Q#dl-1: worker A in the block, worker B waiting for the turnstile that a benchmark holds while it waits for LOCK_EX; A must be able to leave
a_in, a_go, a_out, b_in = threading.Event(), threading.Event(), [], []
def A():
    with g.bench_shared(): a_in.set(); a_go.wait(20)
    a_out.append(1)
def B():
    with g.bench_shared(): b_in.append(1)
ta = threading.Thread(target=A, daemon=True); ta.start(); ok(a_in.wait(5), "worker A inside")
bench = py(f"import fcntl,time;t=open({str(d/'bench.turnstile')!r},'a');l=open({str(d/'bench.lock')!r},'a');fcntl.flock(t,fcntl.LOCK_EX);fcntl.flock(l,fcntl.LOCK_EX);time.sleep(2)"); time.sleep(0.7)
tb = threading.Thread(target=B, daemon=True); tb.start(); time.sleep(0.7)
a_go.set(); ta.join(5); ok(bool(a_out), "A leaves while B waits on the benchmark's turnstile (no 3-way wait)")
tb.join(10); ok(bool(b_in) and g._mine == 0, "B enters after the benchmark ends, counter back to 0"); bench.wait()

# 3) hold file: a live pid blocks a new entry, removing it releases, a dead pid is ignored
h = py("import time;time.sleep(30)"); (d / "bench.hold").write_text(f"{h.pid} f2-3 P10/P11")
c_in = []
def C():
    with g.bench_shared(): c_in.append(time.time())
tc = threading.Thread(target=C, daemon=True); tc.start(); time.sleep(1.2)
ok(not c_in, "live bench.hold blocks a new generation"); (d / "bench.hold").unlink(); tc.join(5); ok(bool(c_in), "released when the hold file is removed")
h.kill(); h.wait(); (d / "bench.hold").write_text(str(h.pid)); t0 = time.time()
with g.bench_shared(): pass
ok(time.time() - t0 < 2, "stale hold file from a dead pid is ignored")
print("selftest_bench: all ok")
