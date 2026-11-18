import os
import subprocess
import sys
import tempfile
import textwrap
import unittest

sys.path.insert(0, os.path.join(os.path.dirname(__file__), ".."))
import khora_common  # noqa: E402


def fake_obsidian(tmp, body):
    """A stand-in for the obsidian CLI: a Python script with the given body."""
    path = os.path.join(tmp, "fake_obsidian.py")
    with open(path, "w", encoding="utf-8") as f:
        f.write(textwrap.dedent(body))
    return [sys.executable, path]


class HeadlessEnv(unittest.TestCase):
    KEYS = ("KHORA_OBS_HOME", "KHORA_OBS_DISPLAY", "KHORA_OBSIDIAN_TMP")

    def setUp(self):
        self.saved = {k: os.environ.pop(k, None) for k in self.KEYS}

    def tearDown(self):
        for k, v in self.saved.items():
            os.environ.pop(k, None)
            if v is not None:
                os.environ[k] = v

    def test_desktop_env_is_unchanged(self):
        self.assertIsNone(khora_common.obsidian_env())

    def test_headless_env_points_at_the_daemon(self):
        os.environ["KHORA_OBS_HOME"] = "/srv/obs-home"
        env = khora_common.obsidian_env()
        self.assertEqual(env["HOME"], "/srv/obs-home")
        self.assertEqual(env["DISPLAY"], ":99")

    def test_headless_display_override(self):
        os.environ["KHORA_OBS_HOME"] = "/srv/obs-home"
        os.environ["KHORA_OBS_DISPLAY"] = ":77"
        self.assertEqual(khora_common.obsidian_env()["DISPLAY"], ":77")

    def test_obsidian_tmp_override(self):
        os.environ["KHORA_OBSIDIAN_TMP"] = "/var/obs-tmp"
        self.assertEqual(khora_common.obsidian_tmp(), "/var/obs-tmp")

    @unittest.skipUnless(sys.platform.startswith("linux"), "Linux default")
    def test_obsidian_tmp_linux_default_ignores_agent_tmpdir(self):
        os.environ["TMPDIR"] = "/tmp/claude-1000"
        self.assertEqual(khora_common.obsidian_tmp(), "/tmp")


class VaultName(unittest.TestCase):
    def setUp(self):
        self.saved = os.environ.pop("KHORA_VAULT", None)

    def tearDown(self):
        os.environ.pop("KHORA_VAULT", None)
        if self.saved is not None:
            os.environ["KHORA_VAULT"] = self.saved

    def test_vault_name_env_override(self):
        os.environ["KHORA_VAULT"] = "x"
        self.assertEqual(khora_common.vault_name(), "x")

    def test_vault_name_from_repo_folder(self):
        with tempfile.TemporaryDirectory() as tmp:
            repo = os.path.join(tmp, "khora-main")
            os.makedirs(repo)
            subprocess.run(["git", "init", "-q", repo], check=True)
            self.assertEqual(khora_common.vault_name(cwd=repo), "khora-main")


class Obsidian(unittest.TestCase):
    def setUp(self):
        self.saved = khora_common.OBSIDIAN
        self.tmp = tempfile.TemporaryDirectory()
        os.environ["KHORA_VAULT"] = "khora"
        khora_common._checked.clear()

    def tearDown(self):
        khora_common.OBSIDIAN = self.saved
        os.environ.pop("KHORA_VAULT", None)
        self.tmp.cleanup()

    def test_obsidian_timeout_raises_kiterror(self):
        khora_common.OBSIDIAN = fake_obsidian(self.tmp.name, "import time; time.sleep(5)\n")
        with self.assertRaises(khora_common.KitError) as cm:
            khora_common.obsidian("files", timeout=1)
        self.assertEqual(str(cm.exception), "Obsidian is not running or the CLI is off")

    def test_obsidian_wrong_vault_raises_kiterror(self):
        # vault=khora can resolve to another vault whose name differs only in case.
        khora_common.OBSIDIAN = fake_obsidian(self.tmp.name, r"""
            print("name\tKhora\npath\tC:\\elsewhere\\Khora")
        """)
        with self.assertRaises(khora_common.KitError) as cm:
            khora_common.obsidian("files", repo_root=self.tmp.name)
        self.assertIn("C:\\elsewhere\\Khora", str(cm.exception))
        self.assertIn("KHORA_VAULT", str(cm.exception))

    def test_obsidian_strips_leading_whitespace(self):
        root = self.tmp.name
        khora_common.OBSIDIAN = fake_obsidian(self.tmp.name, f"""
            import sys
            if sys.argv[2] == "vault":
                print("name\\tkhora\\npath\\t" + {root!r})
            else:
                print("\\r\\n\\nhello")
        """)
        self.assertEqual(khora_common.obsidian("read", repo_root=root), "hello\n")

    def test_timeout_holds_even_when_a_child_keeps_the_pipes_open(self):
        import time
        khora_common.OBSIDIAN = fake_obsidian(self.tmp.name, """
            import subprocess, sys, time
            subprocess.Popen([sys.executable, "-c", "import time; time.sleep(20)"])
            time.sleep(20)
        """)
        start = time.time()
        with self.assertRaises(khora_common.KitError):
            khora_common.obsidian("files", timeout=2, repo_root=self.tmp.name)
        self.assertLess(time.time() - start, 8)


if __name__ == "__main__":
    unittest.main()
