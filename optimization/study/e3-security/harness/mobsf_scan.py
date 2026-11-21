#!/usr/bin/env python3
"""Upload one APK to the local MobSF (REST API), run the static scan, save the JSON report.

  mobsf_scan.py APK [--out DIR]       env: MOBSF_URL (default http://127.0.0.1:8000), MOBSF_HOME_DIR (holds the secret the REST API key is derived from)
Output: <out>/<apk-stem>.mobsf.json . The API key is derived from the secret file and never printed.
"""
import argparse, hashlib, json, os, pathlib, re, sys, urllib.parse, urllib.request, uuid

URL = os.environ.get("MOBSF_URL", "http://127.0.0.1:8000")
SECRET = pathlib.Path(os.environ.get("MOBSF_HOME_DIR", "/mnt/01DC198BE9192500/github/Schooldev/3S-study-work/mobsf-data")) / "secret"

def api_key():  # MobSF: sha256 of <MOBSF_HOME_DIR>/secret (mobsf/MobSF/init.py api_key)
    return hashlib.sha256(SECRET.read_bytes().strip()).hexdigest()

def call(path, data=None, files=None, key=None):
    hdr = {"Authorization": key}
    if files:
        b = uuid.uuid4().hex
        body = b""
        for k, (fn, content) in files.items():
            body += (f"--{b}\r\nContent-Disposition: form-data; name=\"{k}\"; filename=\"{fn}\"\r\nContent-Type: application/octet-stream\r\n\r\n").encode() + content + b"\r\n"
        body += f"--{b}--\r\n".encode()
        hdr["Content-Type"] = "multipart/form-data; boundary=" + b
    else:
        body = urllib.parse.urlencode(data or {}).encode()
    req = urllib.request.Request(URL + path, data=body, headers=hdr, method="POST")
    with urllib.request.urlopen(req, timeout=1800) as r:
        return json.load(r)

if __name__ == "__main__":
    ap = argparse.ArgumentParser(); ap.add_argument("apk"); ap.add_argument("--out", default="/mnt/01DC198BE9192500/github/Schooldev/3S-study-work/e3/reports")
    a = ap.parse_args()
    key = api_key(); apk = pathlib.Path(a.apk)
    up = call("/api/v1/upload", files={"file": (apk.name, apk.read_bytes())}, key=key)
    sc = call("/api/v1/scan", {"hash": up["hash"], "scan_type": up["scan_type"], "file_name": up["file_name"]}, key=key)
    out = pathlib.Path(a.out); out.mkdir(parents=True, exist_ok=True)
    (out / (apk.stem + ".mobsf.json")).write_text(json.dumps(sc, indent=1))
    print(json.dumps({"apk": apk.name, "hash": up["hash"], "keys": len(sc), "report": str(out / (apk.stem + ".mobsf.json"))}))
