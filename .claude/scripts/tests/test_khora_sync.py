import os
import sys
import unittest

sys.path.insert(0, os.path.join(os.path.dirname(__file__), ".."))
from khora_sync import State, as_filename, belongs, marker, plan  # noqa: E402

MS_RUN = "Milestone Stage 1 kernel core"
ISSUE_RUN = "Issue #18 Event types"


def run(kind, **props):
    fm = {"identity__type": "particular",
          "relationships__is_a": ["[[Process Instance v1.0.0]]", f"[[{kind}]]"], "properties__status": "open"}
    fm.update({f"properties__{k}": v for k, v in props.items()})
    return fm


def ms(number=2, title="Stage 1 kernel core", state="open", open_issues=()):
    return {"number": number, "title": title, "state": state, "open_issues": list(open_issues)}


def issue(state="OPEN", comments=()):
    return {"number": 18, "state": state, "comments": list(comments)}


def says(actions):
    return [(a.kind, a.say) for a in actions]


class MilestoneTest(unittest.TestCase):
    def test_open_creates_the_milestone_when_none_has_its_title(self):
        s = State(MS_RUN, run("Milestone Loop v1.0.0"))
        acts = plan(s, "open")
        self.assertEqual([a.kind for a in acts], ["gh", "set"])
        self.assertIn("title=Stage 1 kernel core", acts[0].args)

    def test_open_links_an_existing_milestone_by_title(self):
        s = State(MS_RUN, run("Milestone Loop v1.0.0"), milestone=ms())
        self.assertEqual(says(plan(s, "open")), [("set", "link GitHub Milestone 2")])

    def test_linked_and_matching_is_in_sync(self):
        s = State(MS_RUN, run("Milestone Loop v1.0.0", milestone=2), milestone=ms())
        self.assertEqual(plan(s, "check"), [])

    def test_close_closes_the_milestone_and_warns_of_open_issues(self):
        s = State(MS_RUN, run("Milestone Loop v1.0.0", milestone=2, status="closed"), milestone=ms(open_issues=[19]))
        self.assertEqual(says(plan(s, "close")), [("gh", "set GitHub Milestone 2 closed"),
                                                  ("note", "open issues still in the milestone: #19")])

    def test_check_only_reports_a_state_difference(self):
        s = State(MS_RUN, run("Milestone Loop v1.0.0", milestone=2, status="closed"), milestone=ms())
        self.assertEqual(says(plan(s, "check")), [("note", "GitHub Milestone 2 is open; the run is closed")])

    def test_a_retitled_milestone_is_reported_not_renamed(self):
        s = State(MS_RUN, run("Milestone Loop v1.0.0", milestone=2), milestone=ms(title="Stage 1: core"))
        self.assertEqual(says(plan(s, "check")),
                         [("note", "GitHub Milestone 2 is titled 'Stage 1: core', not 'Stage 1 kernel core'")])

    def test_characters_a_file_name_cant_hold_are_ignored(self):
        self.assertEqual(as_filename("Stage 1: kernel core"), "Stage 1 kernel core")
        s = State(MS_RUN, run("Milestone Loop v1.0.0", milestone=2), milestone=ms(title="Stage 1: kernel core"))
        self.assertEqual(plan(s, "check"), [])


class IssueTest(unittest.TestCase):
    def test_open_comments_once(self):
        s = State(ISSUE_RUN, run("Issue Loop v1.0.0", issue=18), branch="issue/18-event-types", issue=issue())
        acts = plan(s, "open")
        self.assertEqual([a.kind for a in acts], ["gh"])
        self.assertIn(marker(ISSUE_RUN), acts[0].args[-1])
        self.assertIn("issue/18-event-types", acts[0].args[-1])
        s.issue = issue(comments=["Work on this issue runs in ... " + marker(ISSUE_RUN)])
        self.assertEqual(plan(s, "open"), [])

    def test_close_records_the_pull_request(self):
        s = State(ISSUE_RUN, run("Issue Loop v1.0.0", issue=18, status="closed"), issue=issue(comments=[marker(ISSUE_RUN)]),
                  prs=[{"number": 40, "state": "OPEN"}, {"number": 39, "state": "CLOSED"}])
        self.assertEqual(says(plan(s, "close")), [("set", "record pull requests #40")])

    def test_close_without_a_pull_request_says_so(self):
        s = State(ISSUE_RUN, run("Issue Loop v1.0.0", issue=18), issue=issue(comments=[marker(ISSUE_RUN)]))
        self.assertEqual([a.kind for a in plan(s, "close")], ["note", "note"])

    def test_merged_closes_the_issue(self):
        s = State(ISSUE_RUN, run("Issue Loop v1.0.0", issue=18, status="closed", pull_requests=["40"]),
                  issue=issue(), prs=[{"number": 40, "state": "MERGED"}])
        self.assertEqual(says(plan(s, "merged")), [("gh", "close issue #18")])

    def test_merged_leaves_an_issue_closed_by_github_alone(self):
        s = State(ISSUE_RUN, run("Issue Loop v1.0.0", issue=18, status="closed", pull_requests=["40"]),
                  issue=issue(state="CLOSED"), prs=[{"number": 40, "state": "MERGED"}])
        self.assertEqual(plan(s, "merged"), [])

    def test_merged_waits_while_another_pull_request_is_open(self):
        s = State(ISSUE_RUN, run("Issue Loop v1.0.0", issue=18, pull_requests=["40", "41"]), issue=issue(),
                  prs=[{"number": 40, "state": "MERGED"}, {"number": 41, "state": "OPEN"}])
        self.assertEqual(plan(s, "merged"), [])

    def test_a_run_without_an_issue_number(self):
        self.assertEqual(says(plan(State(ISSUE_RUN, run("Issue Loop v1.0.0")), "open")),
                         [("note", "the run has no `issue`")])


class KindTest(unittest.TestCase):
    def test_roadmap_has_no_github_counterpart(self):
        s = State("Roadmap khora v1", run("Roadmap Loop v1.0.0"))
        self.assertEqual(says(plan(s, "open")), [("note", "a Roadmap run has no GitHub counterpart")])

    def test_a_kind_is_not_a_run(self):
        fm = {"identity__type": "universal", "relationships__is_a": ["[[Loop v2.0.0]]"]}
        self.assertEqual([a.kind for a in plan(State("Milestone Loop v1.0.0", fm), "check")], ["note"])

    def test_a_pull_request_belongs_by_keyword_or_branch(self):
        self.assertTrue(belongs({"body": "Closes #18", "headRefName": "x"}, 18))
        self.assertTrue(belongs({"body": "", "headRefName": "issue/18-event-types"}, 18))
        self.assertFalse(belongs({"body": "Closes #180", "headRefName": "issue/180-x"}, 18))
        self.assertFalse(belongs({"body": "see #18", "headRefName": "dev/x"}, 18))


if __name__ == "__main__":
    unittest.main()
