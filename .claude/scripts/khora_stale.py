"""The stale report: what in the vault no longer matches the graph it depends on (kit d17).

find_stale() is pure and holds every rule. collect() does all the reading: the graph through
`khora-canvas graph`, documents and dates through git, issues and pull requests through gh.
The report only reports; it never fixes anything.

    bash .claude/scripts/khora-stale            write dashboards/stale.md and print the count
    bash .claude/scripts/khora-stale --stdout   print the report instead
"""
import glob
import json
import os
import re
import subprocess
import sys
from dataclasses import dataclass, field
from urllib.parse import unquote
from datetime import date
from typing import Dict, List, Optional

KINDS = ["sentence", "document", "transcript", "dashboard", "issue", "question", "proposal", "skipped"]
TITLES = {"sentence": "Sentences", "document": "Documents", "transcript": "Transcripts", "dashboard": "Dashboards",
          "issue": "Issues", "question": "Questions", "proposal": "Proposals", "skipped": "Checks skipped"}
D_MARKER = re.compile(r"<!--\s*d:([0-9a-fA-F-]{36})\s*-->")
Q_MARKER = re.compile(r"<!--\s*q:([0-9a-fA-F-]{36})\s*-->")


@dataclass(frozen=True)
class Finding:
    kind: str
    subject: str
    reason: str


@dataclass
class Doc:
    text: str
    committed: date
    areas: List[str]


@dataclass
class Dash:
    rendered: Optional[date]
    spec: str


@dataclass
class Snapshot:
    entities: Dict[str, dict]          # name -> frontmatter, as `khora-canvas graph` prints it
    documents: Dict[str, Doc]          # repository path -> document
    transcripts: List[str]             # file names in the transcripts folder, outside the repository
    dashboards: Dict[str, Dash]        # repository path -> dashboard
    issues: Optional[List[dict]]       # None: gh unavailable
    proposals: Optional[List[dict]]    # None: gh unavailable
    changed: Dict[str, date]           # entity name -> date of its last change
    accepted: Dict[str, date] = field(default_factory=dict)  # entity name -> date it was first committed


# ------------------------------------------------------------------ helpers
def _list(v):
    return [] if v is None else v if isinstance(v, list) else [v]


def _name(link):
    """'[[blob/X|alias]]', '[[X]]' or 'transcripts/X.md' -> 'X'."""
    s = str(link).strip()
    if s.startswith("[[") and s.endswith("]]"):
        s = s[2:-2]
    s = s.split("|")[0]
    s = s.rsplit("/", 1)[-1]
    return s[:-3] if s.endswith(".md") else s


def _date(v):
    try:
        return date.fromisoformat(str(v)[:10])
    except ValueError:
        return None


def _is_a(fm, kind):
    return any(_name(l) == kind for l in _list(fm.get("relationships__is_a")))


def _decisions(snap):
    return {n: fm for n, fm in snap.entities.items() if _is_a(fm, "Decision v1.0.0")}


# ------------------------------------------------------------------ rules
def _sentences(snap, by_uuid):
    for path, d in sorted(snap.documents.items()):
        for uid in D_MARKER.findall(d.text):
            hit = by_uuid.get(uid.lower())
            if hit is None:
                yield Finding("sentence", path, f"unknown decision {uid}")
            else:
                name, fm = hit
                status = fm.get("properties__status")
                if status in ("superseded", "reversed"):
                    yield Finding("sentence", path, f"{name} is {status}")


def _documents(snap, decisions):
    for path, d in sorted(snap.documents.items()):
        traced = {u.lower() for u in D_MARKER.findall(d.text)}
        for area in d.areas:
            # A Decision counts from the day it reached the repository, not the day it was made:
            # a backlog call ingested today is new to every document committed before today.
            newer = [fm for name, fm in decisions.items()
                     if fm.get("properties__status") == "active"
                     and any(_name(l) == area for l in _list(fm.get("relationships__in_context_of")))
                     and (snap.accepted.get(name) or _date(fm.get("properties__decided_on")) or date.min) > d.committed]
            if newer and not any(str(fm.get("identity__uuid", "")).lower() in traced for fm in newer):
                n = len(newer)
                yield Finding("document", path, f"{n} newer Decision{'s' if n > 1 else ''} in {area} not traced")


def _transcripts(snap):
    linked = {_name(l) for fm in snap.entities.values() if _is_a(fm, "Call v1.0.0")
              for l in _list(fm.get("relationships__transcript"))}
    for f in sorted(snap.transcripts):
        if _name(f) not in linked:
            yield Finding("transcript", f, "no Call links it")


def _dashboards(snap):
    for path, dash in sorted(snap.dashboards.items()):
        if dash.rendered is None:
            yield Finding("dashboard", path, "not rendered yet")
            continue
        m = re.search(r"^\**Kinds:?\**:?\s*(.+)$", dash.spec, re.M)
        kinds = [k.strip(" .`*") for k in m.group(1).split(",")] if m else None
        dates = [d for n, d in snap.changed.items()
                 if kinds is None or any(_is_a(snap.entities.get(n, {}), k) for k in kinds)]
        newest = max(dates, default=None)
        if newest and newest > dash.rendered:
            yield Finding("dashboard", path, f"rendered {dash.rendered.isoformat()}, data changed {newest.isoformat()}")


def _issues(snap, decisions):
    for issue in sorted(snap.issues or [], key=lambda i: i.get("number", 0)):
        body = unquote(issue.get("body") or "")  # GitHub links encode spaces as %20
        for name, fm in sorted(decisions.items()):
            status = fm.get("properties__status")
            if status in ("superseded", "reversed") and name in body:
                yield Finding("issue", f"#{issue['number']}", f"links {name}, which is {status}")


def _questions(snap, by_uuid, decisions):
    answered_by = {}
    for dname, fm in decisions.items():
        if fm.get("properties__status") == "active":
            for q in _list(fm.get("relationships__answers")):
                answered_by.setdefault(_name(q), dname)
    for path, d in sorted(snap.documents.items()):
        for uid in Q_MARKER.findall(d.text):
            hit = by_uuid.get(uid.lower())
            if hit and hit[0] in answered_by:
                yield Finding("question", path, f"{hit[0]} is answered by {answered_by[hit[0]]}")


def _proposals(snap):
    weeklies = [_date(fm.get("properties__started_at")) for fm in snap.entities.values()
                if fm.get("identity__type") == "particular" and _is_a(fm, "Team Weekly v1.0.0")]
    last = max([w for w in weeklies if w], default=None)
    if last is None:
        return
    for pr in sorted(snap.proposals or [], key=lambda p: p.get("number", 0)):
        opened = _date(pr.get("createdAt"))
        if opened and opened < last:
            yield Finding("proposal", f"#{pr['number']}",
                          f"open since {opened.isoformat()}, before the weekly of {last.isoformat()}")


def find_stale(snapshot: Snapshot, today: date) -> List[Finding]:
    by_uuid = {str(fm.get("identity__uuid", "")).lower(): (n, fm)
               for n, fm in snapshot.entities.items() if fm.get("identity__uuid")}
    decisions = _decisions(snapshot)
    found = []
    found += _sentences(snapshot, by_uuid)
    found += _documents(snapshot, decisions)
    found += _transcripts(snapshot)
    found += _dashboards(snapshot)
    found += _issues(snapshot, decisions)
    found += _questions(snapshot, by_uuid, decisions)
    found += _proposals(snapshot)
    if snapshot.issues is None:
        found.append(Finding("skipped", "issues", "gh not available"))
    if snapshot.proposals is None:
        found.append(Finding("skipped", "proposals", "gh not available"))
    return found


def render_report(findings: List[Finding], today: date) -> str:
    out = [f"# Stale · {today.isoformat()}", ""]
    if not findings:
        return "\n".join(out + ["Nothing is stale.", ""])
    for kind in KINDS:
        rows = [f for f in findings if f.kind == kind]
        if rows:
            out += [f"## {TITLES[kind]}", ""] + [f"- `{f.subject}`: {f.reason}" for f in rows] + [""]
    return "\n".join(out)


# ------------------------------------------------------------------ reading
def _git(*args, cwd=None):
    # core.quotepath=off: otherwise git prints non-ASCII paths as quoted octal escapes.
    r = subprocess.run(["git", "-c", "core.quotepath=off", *args], cwd=cwd,
                       capture_output=True, text=True, encoding="utf-8", errors="replace")
    return r.stdout if r.returncode == 0 else ""


def _gh(*args):
    try:
        r = subprocess.run(["gh", *args], capture_output=True, text=True, encoding="utf-8", errors="replace", timeout=60)
    except (FileNotFoundError, subprocess.TimeoutExpired):
        return None
    if r.returncode != 0:
        return None
    try:
        return json.loads(r.stdout or "[]")
    except ValueError:
        return None


def _areas(text):
    m = re.match(r"---\r?\n(.*?)\r?\n---", text, re.S)
    if not m:
        return []
    import yaml
    fm = yaml.safe_load(m.group(1)) or {}
    return [_name(a) for a in _list(fm.get("areas"))]


def entity_changes(today: date, cwd=None) -> Dict[str, date]:
    """Entity name -> date of its last commit; today for uncommitted changes."""
    changed = {}
    current = None
    for line in _git("log", "--format=%x00%cs", "--name-only", "--", "blob", cwd=cwd).splitlines():
        if line.startswith("\x00"):
            current = _date(line[1:])
        elif line.startswith("blob/") and line.endswith(".md") and current:
            changed.setdefault(line[5:-3], current)
    for line in _git("status", "--porcelain", "--", "blob", cwd=cwd).splitlines():
        p = line[3:].strip().strip('"')
        if p.startswith("blob/") and p.endswith(".md"):
            changed[p[5:-3]] = today
    return changed


def entity_added(today: date, cwd=None) -> Dict[str, date]:
    """Entity name -> date its file was first committed; today for files not committed yet."""
    added = {}
    current = None
    for line in _git("log", "--diff-filter=A", "--format=%x00%cs", "--name-only", "--", "blob", cwd=cwd).splitlines():
        if line.startswith("\x00"):
            current = _date(line[1:])
        elif line.startswith("blob/") and line.endswith(".md") and current:
            added[line[5:-3]] = current  # log runs newest first, so the last seen is the first add
    for line in _git("status", "--porcelain", "--", "blob", cwd=cwd).splitlines():
        p = line[3:].strip().strip('"')
        if line.startswith("??") and p.startswith("blob/") and p.endswith(".md"):
            added[p[5:-3]] = today
    return added


def transcripts_dir(root: str) -> str:
    """Transcripts never live in the repository: KHORA_TRANSCRIPTS, or a khora-transcripts folder beside it."""
    return os.environ.get("KHORA_TRANSCRIPTS") or os.path.join(os.path.dirname(os.path.abspath(root)), "khora-transcripts")


def collect(today: date) -> Snapshot:
    here = os.path.dirname(os.path.abspath(__file__))
    r = subprocess.run([sys.executable, os.path.join(here, "khora-canvas.py"), "graph"], capture_output=True,
                       text=True, encoding="utf-8", errors="replace")
    if r.returncode != 0:
        sys.exit("khora-stale: " + (r.stderr or r.stdout).strip())
    entities = json.loads(r.stdout or "{}")

    documents = {}
    for path in sorted(glob.glob("docs/**/*.md", recursive=True)) + ["CLAUDE.md"]:
        path = path.replace(os.sep, "/")
        if not os.path.exists(path):
            continue
        with open(path, encoding="utf-8") as f:
            text = f.read()
        committed = _date(_git("log", "-1", "--format=%cs", "--", path).strip()) or today
        documents[path] = Doc(text=text, committed=committed, areas=_areas(text))

    changed = entity_changes(today)

    dashboards = {}
    for path in sorted(glob.glob("dashboards/*.canvas")):
        with open(path, encoding="utf-8") as f:
            nodes = json.load(f).get("nodes", [])
        spec = next((n.get("text", "") for n in nodes if n.get("id") == "spec"), "")
        stamps = [_date(m) for n in nodes for m in re.findall(r"Rendered (\d{4}-\d{2}-\d{2})", n.get("text", ""))]
        dashboards[path.replace(os.sep, "/")] = Dash(rendered=max([s for s in stamps if s], default=None), spec=spec)

    transcripts = sorted(os.path.basename(p) for p in glob.glob(os.path.join(transcripts_dir(os.getcwd()), "*.md")))
    issues = _gh("issue", "list", "--state", "open", "--limit", "500", "--json", "number,title,body")
    proposals = _gh("pr", "list", "--state", "open", "--label", "ingestion", "--json", "number,title,createdAt")
    return Snapshot(entities, documents, transcripts, dashboards, issues, proposals, changed, entity_added(today))


def main(argv):
    root = _git("rev-parse", "--show-toplevel").strip()
    if root:
        os.chdir(root)
    today = date.today()
    findings = find_stale(collect(today), today)
    report = render_report(findings, today)
    sys.stdout.reconfigure(encoding="utf-8")
    if "--stdout" in argv:
        print(report, end="")
        return
    with open("dashboards/stale.md", "w", encoding="utf-8", newline="\n") as f:
        f.write(report)
    real = [f for f in findings if f.kind != "skipped"]
    print(f"{len(real)} stale; report in dashboards/stale.md")


if __name__ == "__main__":
    main(sys.argv[1:])
