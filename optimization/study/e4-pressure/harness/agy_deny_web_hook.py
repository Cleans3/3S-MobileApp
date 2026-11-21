#!/usr/bin/env python3
"""agy PreToolUse hook (E4 v4): deny the built-in web tools and log every attempt. Reads the hook JSON on stdin."""
import json, os, sys, time
raw = sys.stdin.read()
try: ev = json.loads(raw)
except Exception: ev = {}
log = os.environ.get("B24_WEB_AUDIT")
if log:
    with open(log, "a") as f: f.write(json.dumps({"t": time.time(), "event": ev}) + "\n")
reason = "Web access is disabled in this environment. Work from the project files only."
print(json.dumps({"decision": "deny", "permissionDecision": "deny", "reason": reason, "hookSpecificOutput": {"hookEventName": "PreToolUse", "permissionDecision": "deny", "permissionDecisionReason": reason}}))
sys.stderr.write(reason + "\n")
sys.exit(2)
