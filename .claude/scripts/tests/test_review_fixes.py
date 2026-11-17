import importlib.util
import os
import sys
import unittest

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, os.path.join(HERE, ".."))
import khora_common  # noqa: E402


def load(name, file):
    spec = importlib.util.spec_from_file_location(name, os.path.join(HERE, "..", file))
    mod = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(mod)
    return mod


canvas = load("khora_canvas", "khora-canvas.py")
validator = load("khora_validate_entity", "khora-validate-entity.py")


class GraphEnvelope(unittest.TestCase):
    def test_failed_query_raises(self):
        with self.assertRaises(khora_common.KitError):
            canvas.rows_from_envelope({"success": False, "error": "plugin not loaded"})

    def test_empty_graph_raises(self):
        with self.assertRaises(khora_common.KitError):
            canvas.rows_from_envelope({"success": True, "value": {"values": []}})

    def test_rows_become_a_map(self):
        env = {"success": True, "value": {"values": [["Log", {"identity__type": "particular"}]]}}
        self.assertEqual(canvas.rows_from_envelope(env), {"Log": {"identity__type": "particular"}})


class CrlfBody(unittest.TestCase):
    def test_toml_body_with_crlf_parses(self):
        body = '```toml\r\nDescription = """\r\nTwo\r\nlines.\r\n"""\r\n```\r\n'
        parsed, err = validator.extract_toml_block(body)
        self.assertIsNone(err)
        self.assertEqual(parsed["Description"].strip(), "Two\nlines.")


if __name__ == "__main__":
    unittest.main()
