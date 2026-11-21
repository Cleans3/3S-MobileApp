#!/usr/bin/env python3
"""Landlock write-jail: run a command that may only WRITE under the given directories (reads stay open).

  jail_exec.py --rw DIR [--rw DIR ...] -- cmd args...

Why: codex's own sandbox (bubblewrap) cannot start on this host (AppArmor restricts unprivileged user namespaces: 'bwrap: setting up uid map:
Permission denied' / 'Failed RTM_NEWADDR'), so codex runs with its sandbox off and THIS wrapper is the containment. Landlock needs no namespaces.
Network is not restricted (the CLIs need their API); network tool use is audited from the transcripts instead.
"""
import ctypes, os, struct, sys

W_FILE, RM_DIR, RM_FILE, MK_CHAR, MK_DIR, MK_REG, MK_SOCK, MK_FIFO, MK_BLOCK, MK_SYM, REFER, TRUNC = (1 << n for n in (1, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14))
HANDLED = W_FILE | RM_DIR | RM_FILE | MK_CHAR | MK_DIR | MK_REG | MK_SOCK | MK_FIFO | MK_BLOCK | MK_SYM | REFER | TRUNC

def jail(rw_dirs):
    libc = ctypes.CDLL(None, use_errno=True)
    attr = struct.pack("<Q", HANDLED)
    fd = libc.syscall(444, attr, len(attr), 0)  # landlock_create_ruleset
    if fd < 0: raise OSError(ctypes.get_errno(), "landlock_create_ruleset")
    for d in rw_dirs:
        pfd = os.open(d, os.O_PATH | os.O_CLOEXEC)
        rule = struct.pack("<Qi", HANDLED, pfd)
        if libc.syscall(445, fd, 1, rule, 0) < 0: raise OSError(ctypes.get_errno(), "landlock_add_rule " + d)
        os.close(pfd)
    if libc.prctl(38, 1, 0, 0, 0) < 0: raise OSError(ctypes.get_errno(), "no_new_privs")
    if libc.syscall(446, fd, 0) < 0: raise OSError(ctypes.get_errno(), "landlock_restrict_self")

if __name__ == "__main__":
    a = sys.argv[1:]; rw = []
    while a and a[0] == "--rw": rw.append(a[1]); a = a[2:]
    if not a or a[0] != "--" or len(a) < 2: sys.exit(__doc__)
    jail(rw)
    os.execvp(a[1], a[1:])
