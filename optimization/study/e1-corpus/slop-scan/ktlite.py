"""ktlite: just enough Kotlin structure for the slop-scan rules (no compiler, stdlib only).

Kt(path, text) keeps three same-length views of the file: raw, code (comments blanked) and skel (comments AND string
contents blanked, so braces/parens inside strings never confuse matching). Newlines are preserved, so offsets map to lines.
"""
import bisect, re

IDENT = re.compile(r"[A-Za-z_][A-Za-z0-9_]*")

def _lex(t):
    """Return (code, skel). Handles line and nested block comments, string, raw-string (with templates) and char literals."""
    n = len(t); code = list(t); skel = list(t); i = 0
    def blank(a, b, both):
        for k in range(a, b):
            if t[k] != "\n":
                skel[k] = " "
                if both: code[k] = " "
    while i < n:
        c = t[i]
        if c == "/" and i + 1 < n and t[i + 1] == "/":
            j = t.find("\n", i); j = n if j < 0 else j
            blank(i, j, True); i = j
        elif c == "/" and i + 1 < n and t[i + 1] == "*":
            depth, j = 1, i + 2
            while j < n and depth:
                if t.startswith("/*", j): depth += 1; j += 2
                elif t.startswith("*/", j): depth -= 1; j += 2
                else: j += 1
            blank(i, j, True); i = j
        elif t.startswith('"""', i):
            j = t.find('"""', i + 3); j = n if j < 0 else j + 3
            while j < n and t[j] == '"': j += 1            # trailing quotes belong to the literal
            blank(i + 3, max(i + 3, j - 3), False); i = j
        elif c == '"':
            j = i + 1; depth = 0
            while j < n:
                if t[j] == "\\": j += 2; continue
                if t.startswith("${", j): depth += 1; j += 2; continue
                if depth and t[j] == "}": depth -= 1; j += 1; continue
                if t[j] == '"' and not depth: break
                if t[j] == "\n" and not depth: break      # unterminated: stop at EOL
                j += 1
            blank(i + 1, j, False); i = j + 1
        elif c == "'" and i + 2 < n and (t[i + 2] == "'" or (t[i + 1] == "\\" and "'" in t[i + 2:i + 8])):
            j = t.find("'", i + 2 if t[i + 1] != "\\" else i + 3); j = i + 2 if j < 0 else j
            blank(i + 1, j, False); i = j + 1
        else:
            i += 1
    return "".join(code), "".join(skel)

class Call:
    __slots__ = ("name", "start", "ostart", "oend", "lstart", "lend", "end")
    def __init__(s, **k):
        for a in s.__slots__: setattr(s, a, k.get(a))

class Fun:
    __slots__ = ("name", "start", "pstart", "pend", "bstart", "bend", "composable", "params")

class Kt:
    def __init__(self, path, text):
        self.path, self.raw = path, text
        self.code, self.skel = _lex(text)
        self._nl = [m.start() for m in re.finditer("\n", text)]
        self.match = {}
        st = []
        for i, ch in enumerate(self.skel):
            if ch in "({[": st.append((ch, i))
            elif ch in ")}]":
                want = {")": "(", "}": "{", "]": "["}[ch]
                while st and st[-1][0] != want: st.pop()
                if st: self.match[st.pop()[1]] = i
        self.close_to_open = {v: k for k, v in self.match.items()}
        self.funs = self._funs()
        self.blocks = self._blocks()

    def line(self, pos): return bisect.bisect_left(self._nl, pos) + 1
    def snippet(self, a, b=None, n=140):
        b = b if b is not None else a + n
        return " ".join(self.code[a:min(b, a + n)].split())

    # ---- declarations
    def _funs(self):
        out = []
        for m in re.finditer(r"\bfun\s+(?:<[^>{(]*>\s*)?(?:[\w.<>?, ]+\.)?(\w+)\s*\(", self.skel):
            po = m.end() - 1; pc = self.match.get(po)
            if pc is None: continue
            j = pc + 1; depth = 0; bstart = None
            while j < len(self.skel):               # skip ': ReturnType' / where-clauses to the body '{' or '='
                ch = self.skel[j]
                if ch == "<": depth += 1
                elif ch == ">" and self.skel[j - 1] != "-": depth -= 1
                elif ch == "(" and j in self.match: j = self.match[j]
                elif ch == "{" and depth <= 0: bstart = j; break
                elif ch == "=" and depth <= 0: break
                elif ch == "\n" and self.skel[j:j + 40].strip().startswith(("fun ", "val ", "var ", "class ", "@", "}", "private ", "internal ", "public ", "object ")): break
                j += 1
            if bstart is None or bstart not in self.match: continue
            f = Fun(); f.name = m.group(1); f.start = m.start(); f.pstart, f.pend = po, pc
            f.bstart, f.bend = bstart, self.match[bstart]; f.params = self.code[po + 1:pc]
            head = self.skel[max(0, m.start() - 160):m.start()]
            cut = max(head.rfind("}"), head.rfind(";"), head.rfind("\n\n"), head.rfind("="))
            f.composable = "@Composable" in head[cut + 1:] or "@Composable" in self.code[max(0, m.start() - 60):m.start()]
            out.append(f)
        return out

    def _label(self, o):
        s = self.skel; k = o - 1
        while k >= 0 and s[k] in " \t\r\n": k -= 1
        if k < 0: return "{"
        c = s[k]
        if c == ")":
            po = self.close_to_open.get(k)
            if po is not None:
                q = po - 1
                while q >= 0 and s[q] in " \t": q -= 1
                m = re.search(r"([A-Za-z_]\w*)$", s[max(0, q - 60):q + 1])
                if m: return m.group(1)
        if c == "=":
            m = re.search(r"([A-Za-z_]\w*)\s*$", s[max(0, k - 60):k])
            return "=" + (m.group(1) if m else "")
        m = re.search(r"([A-Za-z_]\w*)$", s[max(0, k - 60):k + 1])
        if m: return m.group(1)
        if s[max(0, k - 1):k + 1] == "->": return "->"
        return "{"

    def _blocks(self):
        fun_at = {f.bstart: f for f in self.funs}
        out = []
        for o, c in self.match.items():
            if self.skel[o] != "{": continue
            f = fun_at.get(o)
            out.append((o, c, "fun:" + f.name if f else self._label(o), f))
        out.sort()
        return out

    def enclosing(self, pos):
        """Labels of every brace block that contains pos, outermost first."""
        return [b for b in self.blocks if b[0] < pos < b[1]]

    def inside(self, pos, labels):
        return any(b[2] in labels or b[2].lstrip("=") in labels for b in self.enclosing(pos))

    def enclosing_fun(self, pos):
        best = None
        for f in self.funs:
            if f.bstart < pos < f.bend and (best is None or f.bstart > best.bstart): best = f
        return best

    # ---- calls
    def calls(self, names):
        """Calls 'name(args) [{lambda}]' and brace-only calls 'name {lambda}' for names (regex alternation)."""
        pat = re.compile(r"(?<![\w])(" + names + r")\b")
        out = []
        for m in pat.finditer(self.skel):
            k = m.end()
            while k < len(self.skel) and self.skel[k] in " \t": k += 1
            c = Call(name=m.group(1), start=m.start(), end=m.end())
            if k < len(self.skel) and self.skel[k] == "(" and k in self.match:
                c.ostart, c.oend = k, self.match[k]; c.end = c.oend + 1
                j = c.end
                while j < len(self.skel) and self.skel[j] in " \t": j += 1
                if j < len(self.skel) and self.skel[j] == "{" and j in self.match:
                    c.lstart, c.lend = j, self.match[j]; c.end = c.lend + 1
            elif k < len(self.skel) and self.skel[k] == "{" and k in self.match:
                c.lstart, c.lend = k, self.match[k]; c.end = c.lend + 1
            else:
                continue
            out.append(c)
        return out

    def args(self, c): return self.code[c.ostart + 1:c.oend] if c.ostart is not None else ""
    def args_skel(self, c): return self.skel[c.ostart + 1:c.oend] if c.ostart is not None else ""
    def body(self, c): return self.code[c.lstart + 1:c.lend] if c.lstart is not None else ""
    def full(self, c): return self.code[c.start:c.end]

def split_top(s):
    """Split an argument string on top-level commas."""
    out, depth, cur = [], 0, []
    for ch in s:
        if ch in "([{": depth += 1
        elif ch in ")]}": depth -= 1
        if ch == "," and depth == 0: out.append("".join(cur)); cur = []
        else: cur.append(ch)
    if "".join(cur).strip(): out.append("".join(cur))
    return out

def modifier_arg(argstr):
    """The modifier argument of a composable call: 'modifier = ..' or the first positional argument that starts with Modifier/modifier."""
    v = named_arg(argstr, "modifier")
    if v is not None: return v
    for a in split_top(argstr):
        if re.match(r"\s*[Mm]odifier\b", a) and not re.match(r"\s*\w+\s*=(?!=)", a): return a.strip()
    return None

def named_arg(argstr, name):
    """Return the text of 'name = value' in a call's argument string, else None."""
    for a in split_top(argstr):
        m = re.match(r"\s*" + re.escape(name) + r"\s*=\s*(.*)$", a, re.S)
        if m: return m.group(1).strip()
    return None
