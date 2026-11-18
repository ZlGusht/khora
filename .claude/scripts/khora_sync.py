"""Sync between a loop run in the blob and its GitHub counterpart. The blob is the source;
GitHub reflects it, and what changed on GitHub is read back.

A Milestone Loop run is reflected by a GitHub Milestone; an Issue Loop run by its issue and
pull requests. A Roadmap Loop run has no GitHub counterpart. Sync runs at three points:

    bash .claude/scripts/khora-sync open   "<run name>"   when the run opens
    bash .claude/scripts/khora-sync close  "<run name>"   when the run closes
    bash .claude/scripts/khora-sync merged "<run name>"   when its pull request merges
    bash .claude/scripts/khora-sync check  "<run name>"   report differences; change nothing

Add --dry-run to print the actions without taking them.

plan() is pure and holds every rule. collect() does the reading: the run through the Obsidian
CLI, the rest through gh. apply() takes the actions.
"""
import json
import re
import subprocess
import sys
from dataclasses import dataclass, field
from typing import List, Optional

import yaml

import khora_common

EVENTS = ("open", "close", "merged", "check")
MILESTONE_KIND = "Milestone Loop v1.0.0"
ISSUE_KIND = "Issue Loop v1.0.0"
ROADMAP_KIND = "Roadmap Loop v1.0.0"


@dataclass(frozen=True)
class Action:
    kind: str          # "gh": run gh with args; "set": set a run property; "note": report only
    args: tuple
    say: str


@dataclass
class State:
    name: str                          # the run's name
    fm: dict                           # its frontmatter
    branch: str = ""                   # the current git branch
    milestone: Optional[dict] = None   # {number, title, state, open_issues: [numbers]}
    issue: Optional[dict] = None       # {number, state, comments: [bodies]}
    prs: List[dict] = field(default_factory=list)  # [{number, state}], state: OPEN, CLOSED or MERGED


def _list(v):
    return [] if v is None else v if isinstance(v, list) else [v]


def _strip(s, prefix):
    return s[len(prefix):] if s.startswith(prefix) else s


def _name(link):
    s = _strip(str(link).strip().strip('"'), "[[")
    return (s[:-2] if s.endswith("]]") else s).split("|")[0]


def run_kind(fm):
    kinds = {_name(l) for l in _list(fm.get("relationships__is_a"))}
    for k in (MILESTONE_KIND, ISSUE_KIND, ROADMAP_KIND):
        if k in kinds:
            return k
    return None


def milestone_title(name):
    return _strip(name, "Milestone ").strip()


def as_filename(title):
    """A GitHub title as it can appear in a run's file name: without the characters file
    names can't hold. "Stage 1: kernel core" becomes "Stage 1 kernel core"."""
    return re.sub(r"\s+", " ", re.sub(r'[\\/:*?"<>|#^\[\]]', "", title)).strip()


def marker(name):
    return f"<!-- khora-run: {name} -->"


def _milestone(s: State, event: str) -> List[Action]:
    acts = []
    status = s.fm.get("properties__status")
    number = s.fm.get("properties__milestone")
    ms = s.milestone
    title = milestone_title(s.name)
    if ms is None:
        if number:
            return [Action("note", (), f"GitHub Milestone {number} not found")]
        if event == "open":
            return [Action("gh", ("api", "-X", "POST", "repos/{owner}/{repo}/milestones", "-f", f"title={title}"),
                           f"create the GitHub Milestone '{title}'"),
                    Action("set", ("properties__milestone", "<created>", "number"), "set milestone to the new number")]
        return [Action("note", (), f"no GitHub Milestone titled '{title}'; run `khora-sync open` first")]
    if not number:
        acts.append(Action("set", ("properties__milestone", str(ms["number"]), "number"),
                           f"link GitHub Milestone {ms['number']}"))
    if as_filename(ms["title"]) != title:
        acts.append(Action("note", (), f"GitHub Milestone {ms['number']} is titled '{ms['title']}', not '{title}'"))
    want = "closed" if status == "closed" else "open"
    if ms["state"] != want:
        if event in ("open", "close"):
            acts.append(Action("gh", ("api", "-X", "PATCH", f"repos/{{owner}}/{{repo}}/milestones/{ms['number']}",
                                      "-f", f"state={want}"), f"set GitHub Milestone {ms['number']} {want}"))
        else:
            acts.append(Action("note", (), f"GitHub Milestone {ms['number']} is {ms['state']}; the run is {status}"))
    if want == "closed" and ms["open_issues"]:
        acts.append(Action("note", (), "open issues still in the milestone: " +
                           ", ".join(f"#{n}" for n in ms["open_issues"])))
    return acts


def _issue(s: State, event: str) -> List[Action]:
    number = s.fm.get("properties__issue")
    if not number:
        return [Action("note", (), "the run has no `issue`")]
    if s.issue is None:
        return [Action("note", (), f"issue #{number} not found")]
    acts = []
    status = s.fm.get("properties__status")
    if event == "open":
        if s.issue["state"] != "OPEN":
            acts.append(Action("note", (), f"issue #{number} is {s.issue['state'].lower()}"))
        if not any(marker(s.name) in c for c in s.issue["comments"]):
            where = f" on branch `{s.branch}`" if s.branch else ""
            acts.append(Action("gh", ("issue", "comment", str(number), "--body",
                                      f"Work on this issue runs in `blob/{s.name}.md`{where}.\n\n{marker(s.name)}"),
                               f"comment on issue #{number} with the run"))
    known = sorted({str(p) for p in _list(s.fm.get("properties__pull_requests"))}, key=int)
    found = sorted({str(p["number"]) for p in s.prs if p["state"] != "CLOSED"} | set(known), key=int)
    if found != known:
        acts.append(Action("set", ("properties__pull_requests", ", ".join(found), "list"),
                           "record pull requests " + ", ".join(f"#{n}" for n in found)))
    if event == "close":
        if status != "closed":
            acts.append(Action("note", (), "the run is not closed yet; finish its Process(v) first"))
        if not found:
            acts.append(Action("note", (), "no pull request yet; open one that says `Closes #%s`" % number))
    if event in ("merged", "check"):
        merged = [p for p in s.prs if p["state"] == "MERGED"]
        if merged and not [p for p in s.prs if p["state"] == "OPEN"] and s.issue["state"] == "OPEN":
            if event == "merged":
                acts.append(Action("gh", ("issue", "close", str(number), "--comment",
                                          f"Merged in #{merged[-1]['number']}. {marker(s.name)}"),
                                   f"close issue #{number}"))
            else:
                acts.append(Action("note", (), f"issue #{number} is open, but its pull requests are merged"))
        if event == "merged" and not merged:
            acts.append(Action("note", (), "no merged pull request found"))
    if status == "closed" and s.issue["state"] == "OPEN" and event == "check" and not s.prs:
        acts.append(Action("note", (), f"the run is closed, but issue #{number} is open with no pull request"))
    return acts


def plan(s: State, event: str) -> List[Action]:
    kind = run_kind(s.fm)
    if s.fm.get("identity__type") != "particular" or kind is None:
        return [Action("note", (), f"'{s.name}' is not a loop run")]
    if kind == ROADMAP_KIND:
        return [Action("note", (), "a Roadmap run has no GitHub counterpart")]
    return _milestone(s, event) if kind == MILESTONE_KIND else _issue(s, event)


# ------------------------------------------------------------------ reading

def _gh(*args):
    r = subprocess.run(["gh", *args], capture_output=True, text=True, encoding="utf-8", errors="replace", timeout=60)
    if r.returncode != 0:
        raise khora_common.KitError(f"gh {' '.join(args[:2])}: {r.stderr.strip() or r.stdout.strip()}")
    return r.stdout


def read_run(name):
    text = khora_common.obsidian("read", f"path=blob/{name}.md")
    m = re.match(r"---\n(.*?)\n---", text, re.S)
    if not m:
        raise khora_common.KitError(f"cannot read the run '{name}'")
    return yaml.safe_load(m.group(1)) or {}


def belongs(pr, number):
    """A pull request carries an issue's run if it says it closes the issue, or if its branch
    follows the Issue Loop's naming, `issue/<number>-<short title>`."""
    closes = re.compile(rf"\b(close[sd]?|fix(e[sd])?|resolve[sd]?) #{number}\b", re.I)
    return bool(closes.search(pr.get("body") or "")) or (pr.get("headRefName") or "").startswith(f"issue/{number}-")


def _milestones():
    data = json.loads(_gh("api", "repos/{owner}/{repo}/milestones?state=all&per_page=100"))
    return {m["number"]: m for m in data}


def collect(name) -> State:
    fm = read_run(name)
    branch = subprocess.run(["git", "branch", "--show-current"], capture_output=True, text=True).stdout.strip()
    s = State(name=name, fm=fm, branch=branch)
    kind = run_kind(fm)
    if kind == MILESTONE_KIND:
        all_ms = _milestones()
        number = fm.get("properties__milestone")
        ms = all_ms.get(int(number)) if number else next(
            (m for m in all_ms.values() if as_filename(m["title"]) == milestone_title(name)), None)
        if ms:
            opened = json.loads(_gh("issue", "list", "--milestone", ms["title"], "--state", "open",
                                    "--limit", "500", "--json", "number"))
            s.milestone = {"number": ms["number"], "title": ms["title"], "state": ms["state"],
                           "open_issues": sorted(i["number"] for i in opened)}
    elif kind == ISSUE_KIND and fm.get("properties__issue"):
        number = str(fm["properties__issue"])
        try:
            i = json.loads(_gh("issue", "view", number, "--json", "number,state,comments"))
        except khora_common.KitError:
            i = None
        if i:
            s.issue = {"number": i["number"], "state": i["state"], "comments": [c["body"] for c in i["comments"]]}
            prs = json.loads(_gh("pr", "list", "--state", "all", "--limit", "500",
                                 "--json", "number,state,body,headRefName"))
            s.prs = [{"number": p["number"], "state": p["state"]} for p in prs if belongs(p, int(number))]
    return s


# ------------------------------------------------------------------ acting

def apply(name, actions):
    path = f"path=blob/{name}.md"
    created = None
    for a in actions:
        if a.kind == "gh":
            out = _gh(*a.args)
            if a.args[:3] == ("api", "-X", "POST"):
                created = str(json.loads(out)["number"])
        elif a.kind == "set":
            key, value, typ = a.args
            if value == "<created>":
                value = created
            khora_common.obsidian("property:set", path, f"name={key}", f"value={value}", f"type={typ}")
        print(("note: " if a.kind == "note" else "done: ") + a.say)


def main(argv):
    args = [a for a in argv if a != "--dry-run"]
    if len(args) != 2 or args[0] not in EVENTS:
        print(__doc__.split("\n\n")[2], file=sys.stderr)
        return 2
    event, name = args
    try:
        actions = plan(collect(name), event)
        if event == "check" or "--dry-run" in argv:
            for a in actions:
                print(("note: " if a.kind == "note" else "would: ") + a.say)
        else:
            apply(name, actions)
    except khora_common.KitError as e:
        print(f"khora-sync: {e}", file=sys.stderr)
        return 1
    if not actions:
        print("in sync")
    return 0


if __name__ == "__main__":
    sys.exit(main(sys.argv[1:]))
