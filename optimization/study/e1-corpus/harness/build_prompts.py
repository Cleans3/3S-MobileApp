#!/usr/bin/env python3
"""Extract the frozen brief texts from 00-protocol.md section 8 into prompts/<brief>-<style>.txt.

Prompt = common frame (8.1) [+ platform line for detailed/guardrailed] + brief text, joined by one blank line.
The guardrailed prompt IS the detailed-functional prompt, byte-identical (the guardrail travels as a project-context file).
Refuses to run if 00-protocol.md no longer matches 00-protocol.sha256 (the pre-registered text must not drift).
"""
import hashlib, pathlib, re, sys

STUDY = pathlib.Path(__file__).resolve().parents[2]
OUT = pathlib.Path(__file__).resolve().parents[1] / "prompts"

def main():
    raw = (STUDY / "00-protocol.md").read_bytes()
    want = (STUDY / "00-protocol.sha256").read_text().split()[0]
    got = hashlib.sha256(raw).hexdigest()
    if got != want:
        sys.exit(f"00-protocol.md sha256 {got} != frozen {want}: amendment? re-verify before regenerating prompts")
    text = raw.decode()
    sec = text.split("## 8. Briefs")[1].split("## 9. ")[0]
    blocks = re.findall(r"(?:^\*\*(B\d)-(naive|detailed-functional)\*\*\s*\n)?```text\n(.*?)\n```", sec, flags=re.S | re.M)
    frame, platform = blocks[0][2], blocks[1][2]
    OUT.mkdir(exist_ok=True)
    n = 0
    for brief, style, body in blocks[2:]:
        assert brief and style, "unlabelled block after the frame"
        parts = [frame] + ([platform] if style == "detailed-functional" else []) + [body]
        (OUT / f"{brief}-{style}.txt").write_text("\n\n".join(parts) + "\n")
        n += 1
    (OUT / "PROTOCOL_SHA256").write_text(got + "\n")
    print(f"wrote {n} prompts from protocol sha256 {got[:12]}")

if __name__ == "__main__":
    main()
