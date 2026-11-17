import os
import sys
import unittest
from datetime import date

sys.path.insert(0, os.path.join(os.path.dirname(__file__), ".."))
from khora_stale import Dash, Doc, Finding, Snapshot, find_stale, render_report  # noqa: E402

TODAY = date(2026, 11, 17)
D_ID = "11111111-1111-4111-8111-111111111111"
D2_ID = "22222222-2222-4222-8222-222222222222"
Q_ID = "33333333-3333-4333-8333-333333333333"


def decision(uuid, status="active", decided_on="2026-11-10", area="Log", answers=None):
    fm = {"identity__uuid": uuid, "identity__type": "particular",
          "relationships__is_a": ["[[Decision v1.0.0]]"], "relationships__in_context_of": [f"[[{area}]]"],
          "properties__status": status, "properties__decided_on": decided_on}
    if answers:
        fm["relationships__answers"] = [f"[[{a}]]" for a in answers]
    return fm


def snapshot(**kw):
    base = dict(entities={}, documents={}, transcripts=[], dashboards={}, issues=[], proposals=[], changed={})
    base.update(kw)
    return Snapshot(**base)


def doc(text, committed="2026-11-12", areas=("Log",)):
    return Doc(text=text, committed=date.fromisoformat(committed), areas=list(areas))


class Sentences(unittest.TestCase):
    def test_marker_names_superseded_decision(self):
        s = snapshot(entities={"Old call": decision(D_ID, status="superseded")},
                     documents={"docs/adr/khora-log.md": doc(f"The log is append-only. <!-- d:{D_ID} -->\n")})
        self.assertEqual(find_stale(s, TODAY),
                         [Finding("sentence", "docs/adr/khora-log.md", "Old call is superseded")])

    def test_unknown_decision_marker(self):
        s = snapshot(documents={"docs/x.md": doc(f"A sentence. <!-- d:{D_ID} -->\n", areas=())})
        self.assertEqual(find_stale(s, TODAY), [Finding("sentence", "docs/x.md", f"unknown decision {D_ID}")])

    def test_crlf_document_markers_found(self):
        s = snapshot(entities={"Old call": decision(D_ID, status="reversed")},
                     documents={"docs/x.md": doc(f"Line one.\r\nIt holds. <!-- d:{D_ID} -->\r\n", areas=())})
        self.assertEqual(find_stale(s, TODAY), [Finding("sentence", "docs/x.md", "Old call is reversed")])


class Documents(unittest.TestCase):
    def test_area_has_newer_untraced_decision(self):
        s = snapshot(entities={"New call": decision(D_ID, decided_on="2026-11-15")},
                     documents={"docs/adr/khora-log.md": doc("Nothing traced.\n", committed="2026-11-12")})
        self.assertEqual(find_stale(s, TODAY),
                         [Finding("document", "docs/adr/khora-log.md", "1 newer Decision in Log not traced")])

    def test_newer_decision_traced_is_fresh(self):
        s = snapshot(entities={"New call": decision(D_ID, decided_on="2026-11-15")},
                     documents={"docs/adr/khora-log.md": doc(f"It holds. <!-- d:{D_ID} -->\n", committed="2026-11-12")})
        self.assertEqual(find_stale(s, TODAY), [])


class Transcripts(unittest.TestCase):
    def test_transcript_without_call(self):
        call = {"identity__type": "particular", "relationships__is_a": ["[[Call v1.0.0]]"],
                "relationships__transcript": "[[transcripts/2026-11-05]]"}
        s = snapshot(entities={"Call 2026-11-05 X": call},
                     transcripts=["2026-11-05.md", "2026-11-14.md"])
        self.assertEqual(find_stale(s, TODAY),
                         [Finding("transcript", "transcripts/2026-11-14.md", "no Call links it")])


class Dashboards(unittest.TestCase):
    def test_dashboard_older_than_listed_kind(self):
        s = snapshot(entities={"New call": decision(D_ID)},
                     changed={"New call": date(2026, 11, 16)},
                     dashboards={"dashboards/Areas.canvas": Dash(rendered=date(2026, 11, 15),
                                                                 spec="Purpose.\nKinds: Decision v1.0.0, Question v1.0.0\n")})
        self.assertEqual(find_stale(s, TODAY),
                         [Finding("dashboard", "dashboards/Areas.canvas", "rendered 2026-11-15, data changed 2026-11-16")])

    def test_dashboard_ignores_kinds_not_listed(self):
        s = snapshot(entities={"New call": decision(D_ID)},
                     changed={"New call": date(2026, 11, 16)},
                     dashboards={"dashboards/Loops.canvas": Dash(rendered=date(2026, 11, 15),
                                                                 spec="Kinds: Loop v2.0.0\n")})
        self.assertEqual(find_stale(s, TODAY), [])

    def test_dashboard_never_rendered(self):
        s = snapshot(dashboards={"dashboards/Stale.canvas": Dash(rendered=None, spec="")})
        self.assertEqual(find_stale(s, TODAY), [Finding("dashboard", "dashboards/Stale.canvas", "not rendered yet")])


class Issues(unittest.TestCase):
    def test_issue_links_reversed_decision(self):
        s = snapshot(entities={"Use one log": decision(D_ID, status="reversed")},
                     issues=[{"number": 31, "title": "Build the log", "body": "From blob/Use one log.md"}])
        self.assertEqual(find_stale(s, TODAY), [Finding("issue", "#31", "links Use one log, which is reversed")])


class Questions(unittest.TestCase):
    def test_question_listed_open_but_answered(self):
        q = {"identity__uuid": Q_ID, "identity__type": "particular", "relationships__is_a": ["[[Question v1.0.0]]"]}
        s = snapshot(entities={"Which store": q, "Use SQLite": decision(D_ID, answers=["Which store"])},
                     documents={"docs/adr/khora-log.md": doc(f"| Which store | SQLite | <!-- q:{Q_ID} -->\n"
                                                             f"It holds. <!-- d:{D_ID} -->\n")})
        self.assertEqual(find_stale(s, TODAY),
                         [Finding("question", "docs/adr/khora-log.md", "Which store is answered by Use SQLite")])


class Proposals(unittest.TestCase):
    def test_proposal_older_than_latest_weekly(self):
        weekly = {"identity__type": "particular", "properties__started_at": "2026-11-16",
                  "relationships__is_a": ["[[Process Instance v1.0.0]]", "[[Team Weekly v1.0.0]]"]}
        s = snapshot(entities={"Y26Q4W47": weekly},
                     proposals=[{"number": 40, "title": "Ingest 2026-11-14", "createdAt": "2026-11-14T10:00:00Z"},
                                {"number": 41, "title": "Ingest 2026-11-17", "createdAt": "2026-11-17T10:00:00Z"}])
        self.assertEqual(find_stale(s, TODAY),
                         [Finding("proposal", "#40", "open since 2026-11-14, before the weekly of 2026-11-16")])


class GitHubUnavailable(unittest.TestCase):
    def test_gh_unavailable_skips_only_github_checks(self):
        s = snapshot(issues=None, proposals=None, transcripts=["2026-11-14.md"])
        self.assertEqual(find_stale(s, TODAY), [
            Finding("transcript", "transcripts/2026-11-14.md", "no Call links it"),
            Finding("skipped", "issues", "gh not available"),
            Finding("skipped", "proposals", "gh not available"),
        ])


class Fresh(unittest.TestCase):
    def test_fresh_vault_reports_nothing(self):
        call = {"identity__type": "particular", "relationships__is_a": ["[[Call v1.0.0]]"],
                "relationships__transcript": "transcripts/2026-11-05.md"}
        s = snapshot(entities={"Call 2026-11-05 X": call, "D": decision(D_ID, decided_on="2026-11-05")},
                     transcripts=["2026-11-05.md"],
                     documents={"docs/adr/khora-log.md": doc(f"It holds. <!-- d:{D_ID} -->\n")},
                     changed={"D": date(2026, 11, 5)},
                     dashboards={"dashboards/Areas.canvas": Dash(rendered=date(2026, 11, 6), spec="")})
        self.assertEqual(find_stale(s, TODAY), [])


class Report(unittest.TestCase):
    def test_report_groups_by_kind(self):
        text = render_report([Finding("transcript", "transcripts/a.md", "no Call links it"),
                              Finding("dashboard", "dashboards/Areas.canvas", "not rendered yet")], TODAY)
        self.assertTrue(text.startswith("# Stale · 2026-11-17\n"))
        self.assertIn("## Transcripts\n\n- `transcripts/a.md`: no Call links it\n", text)
        self.assertIn("## Dashboards\n\n- `dashboards/Areas.canvas`: not rendered yet\n", text)

    def test_report_when_nothing_is_stale(self):
        self.assertEqual(render_report([], TODAY), "# Stale · 2026-11-17\n\nNothing is stale.\n")


if __name__ == "__main__":
    unittest.main()
