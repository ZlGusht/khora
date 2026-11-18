#!/usr/bin/env python3
"""Validate a Khora vault entity against its is_a chain and accumulated schemas.

Pure read. Invokes the `obsidian` CLI for vault I/O (subprocess).
Output: structured pass/fail per invariant, text or JSON. Exit 0 = PASS.

#TODO: extend invariant 3 to value-type / range / enum validation
#TODO: split out reference-integrity validation (cross-entity wikilink existence)
"""

import argparse
import json
import re
import os
import subprocess
import sys
from typing import Optional

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import khora_common  # noqa: E402

import yaml

try:  # Python 3.11+ ships tomllib in stdlib; older needs the tomli backport
    import tomllib as tomli
except ModuleNotFoundError:
    import tomli

BASE_ENTITY = "BaseEntity"
BASE_ENTITY_FALLBACKS = {"BaseEntity v1.0.0"}  # legacy versioned references — accepted as terminators for backward compat
WIKILINK_RE = re.compile(r"^\[\[(.+?)\]\]$")
FRONTMATTER_RE = re.compile(r"^---\s*\n(.*?)\n---\s*\n?(.*)$", re.DOTALL)
TOML_FENCE_RE = re.compile(r"```toml\s*\n(.*?)\n```", re.DOTALL)
PLACEHOLDER_RE = re.compile(r"<%.*?%>", re.DOTALL)


_alias_to_path = None


def _ensure_alias_map():
    """Build {alias: vault_path} from `obsidian aliases verbose`. Cached after first call.

    The CLI's `read file=<name>` is alias-blind (only canonical filenames resolve),
    so we maintain our own alias map for fallback resolution when walking chains
    written with unversioned wikilinks like `[[BaseEntity]]`.
    """
    global _alias_to_path
    if _alias_to_path is not None:
        return _alias_to_path
    _alias_to_path = {}
    try:
        out = khora_common.obsidian("aliases", "verbose")
    except khora_common.KitError:
        return _alias_to_path
    for line in out.splitlines():
        line = line.rstrip()
        if not line or "\t" not in line:
            continue
        alias, path = line.split("\t", 1)
        _alias_to_path[alias.strip()] = path.strip()
    return _alias_to_path


def _looks_like_cli_error(stdout: str) -> bool:
    """The CLI returns exit 0 even for `Error: File "X" not found.` — detect by message."""
    return stdout.lstrip().startswith("Error:")


def obsidian_read(name_or_path: str):
    """Invoke `obsidian read` and return (content, error). Falls back to alias resolution."""
    is_path = "/" in name_or_path or name_or_path.endswith(".md")
    arg = f"path={name_or_path}" if is_path else f"file={name_or_path}"
    try:
        out = khora_common.obsidian("read", arg, timeout=20)
    except khora_common.KitError as e:
        return None, str(e)
    if out and not _looks_like_cli_error(out):
        return out, None
    # Fallback: the CLI's file=<name> doesn't resolve aliases; do it ourselves.
    if not is_path:
        resolved = _ensure_alias_map().get(name_or_path)
        if resolved:
            try:
                retry = khora_common.obsidian("read", f"path={resolved}", timeout=20)
            except khora_common.KitError as e:
                return None, str(e)
            if retry and not _looks_like_cli_error(retry):
                return retry, None
    last = out.strip().splitlines()[-1] if out.strip() else "empty output"
    return None, f"obsidian read failed: {last}"


def split_frontmatter(content: str):
    # `obsidian read` prepends \r\n; strip leading whitespace before matching
    content = content.lstrip()
    m = FRONTMATTER_RE.match(content)
    if not m:
        return None, content, "no frontmatter block found"
    try:
        fm = yaml.safe_load(m.group(1)) or {}
    except yaml.YAMLError as e:
        return None, m.group(2), f"YAML parse error: {e}"
    if not isinstance(fm, dict):
        return None, m.group(2), "frontmatter is not a mapping"
    return fm, m.group(2), None


def extract_toml_block(body: str):
    body = body.replace("\r\n", "\n")  # a CRLF body is still TOML
    m = TOML_FENCE_RE.search(body)
    if not m:
        return None, None
    try:
        return tomli.loads(m.group(1)), None
    except tomli.TOMLDecodeError as e:
        return None, f"TOML parse error: {e}"


def parse_wikilink(s) -> Optional[str]:
    if not isinstance(s, str):
        return None
    m = WIKILINK_RE.match(s.strip())
    return m.group(1) if m else None


def schema_relationship_keys(toml_obj) -> set:
    """Collect relationship keys from [Metadata.Relationships] and any nested subsection."""
    keys = set()
    rel = (toml_obj or {}).get("Metadata", {}).get("Relationships", {})

    def walk(obj):
        if not isinstance(obj, dict):
            return
        for k, v in obj.items():
            if isinstance(v, dict) and "type" in v:
                keys.add(k.lower())
            elif isinstance(v, dict):
                walk(v)

    walk(rel)
    return keys


def schema_property_keys(toml_obj) -> set:
    keys = set()
    props = (toml_obj or {}).get("Metadata", {}).get("Properties", {})
    if isinstance(props, dict):
        for k, v in props.items():
            if isinstance(v, dict):
                keys.add(k.lower())
    return keys


def is_empty(v) -> bool:
    """A value is 'empty' if it's None or an empty string/list/dict."""
    if v is None:
        return True
    if isinstance(v, (str, list, dict)) and len(v) == 0:
        return True
    return False


def schema_required_keys(toml_obj) -> dict:
    """Returns {'identity': set, 'properties': set, 'relationships': set} of REQUIRED field
    names declared in a universal's body schema. A field is required iff its schema entry
    lacks `optional=true`. Walks nested subsections (e.g. [Metadata.Relationships.state])."""
    result = {"identity": set(), "properties": set(), "relationships": set()}
    metadata = (toml_obj or {}).get("Metadata", {})

    def walk(obj, section_key):
        if not isinstance(obj, dict):
            return
        for k, v in obj.items():
            if isinstance(v, dict) and "type" in v:
                if not v.get("optional", False):
                    result[section_key].add(k.lower())
            elif isinstance(v, dict):
                walk(v, section_key)

    walk(metadata.get("Identity", {}), "identity")
    walk(metadata.get("Properties", {}), "properties")
    walk(metadata.get("Relationships", {}), "relationships")
    return result


def walk_chain(start: str):
    """BFS from start along relationships__is_a. Stop at BaseEntity (which is_a points at itself)."""
    chain = []
    allowed_rels = {"is_a"}  # is_a is always allowed
    allowed_props = set()
    required = {"identity": set(), "properties": set(), "relationships": set()}
    errors = []
    visited = set()
    queue = [start]
    reached_base = False

    while queue:
        name = queue.pop(0)
        if name in visited:
            continue
        visited.add(name)
        content, err = obsidian_read(name)
        if err:
            errors.append(f"chain: cannot read [[{name}]]: {err}")
            continue
        fm, body, fm_err = split_frontmatter(content)
        if fm is None:
            errors.append(f"chain: [[{name}]] frontmatter: {fm_err}")
            continue
        chain.append(name)

        toml_obj, toml_err = extract_toml_block(body)
        if toml_err:
            errors.append(f"chain: [[{name}]] body TOML: {toml_err}")
        if toml_obj:
            allowed_rels |= schema_relationship_keys(toml_obj)
            allowed_props |= schema_property_keys(toml_obj)
            req = schema_required_keys(toml_obj)
            required["identity"] |= req["identity"]
            required["properties"] |= req["properties"]
            required["relationships"] |= req["relationships"]

        if name == BASE_ENTITY or name in BASE_ENTITY_FALLBACKS:
            reached_base = True
            continue

        for p in fm.get("relationships__is_a") or []:
            pname = parse_wikilink(p)
            if pname and pname != name and pname not in visited:
                queue.append(pname)

    if not reached_base:
        errors.append(f"chain: did not reach [[{BASE_ENTITY}]] from [[{start}]]")
    return chain, allowed_rels, allowed_props, required, errors


def short_key(prefixed: str, prefix: str) -> str:
    return prefixed[len(prefix):].lower()


def validate(entity: str):
    content, err = obsidian_read(entity)
    if err:
        return None, err

    fm, body, fm_err = split_frontmatter(content)
    invariants = {}

    # I4: frontmatter parses
    invariants["4_frontmatter_yaml"] = {
        "pass": fm is not None,
        "violations": [fm_err] if fm_err else [],
    }
    if fm is None:
        # downstream invariants can't run; still report what we can
        fm = {}

    # I1: is_a chain reaches BaseEntity
    parents = fm.get("relationships__is_a") or []
    parent_names = [parse_wikilink(p) for p in parents]
    parent_names = [n for n in parent_names if n]
    chain_violations = []
    full_chain = []
    allowed_rels = {"is_a"}
    allowed_props = set()
    required = {"identity": set(), "properties": set(), "relationships": set()}
    if not parent_names:
        chain_violations.append("entity has no relationships__is_a")
    else:
        for p in parent_names:
            chain, rels, props, req, errs = walk_chain(p)
            full_chain.extend(chain)
            allowed_rels |= rels
            allowed_props |= props
            required["identity"] |= req["identity"]
            required["properties"] |= req["properties"]
            required["relationships"] |= req["relationships"]
            chain_violations.extend(errs)
    invariants["1_is_a_chain"] = {
        "pass": not chain_violations,
        "violations": chain_violations,
        "chain": full_chain,
    }

    # I2: closed relationship vocabulary
    rel_violations = []
    for key in fm:
        if isinstance(key, str) and key.startswith("relationships__"):
            short = short_key(key, "relationships__")
            if short and short not in allowed_rels:
                rel_violations.append(
                    f"{key} not declared in any ancestor's [Metadata.Relationships]"
                )
    invariants["2_closed_vocabulary"] = {
        "pass": not rel_violations,
        "violations": rel_violations,
        "allowed_keys": sorted(allowed_rels),
    }

    # I3: property conformance (key membership only in v1)
    prop_violations = []
    for key in fm:
        if isinstance(key, str) and key.startswith("properties__"):
            short = short_key(key, "properties__")
            if short and short not in allowed_props:
                prop_violations.append(
                    f"{key} not declared in any ancestor's [Metadata.Properties]"
                )
    invariants["3_property_conformance"] = {
        "pass": not prop_violations,
        "violations": prop_violations,
        "allowed_keys": sorted(allowed_props),
        "_todo": "extend to value-type / range / enum validation",
    }

    # I5: body TOML parses (if a fence is present)
    toml_violations = []
    if body and "```toml" in body:
        _, terr = extract_toml_block(body)
        if terr:
            toml_violations.append(terr)
    invariants["5_body_toml"] = {
        "pass": not toml_violations,
        "violations": toml_violations,
    }

    # I6: no unresolved Templater placeholders
    placeholder_violations = []
    for m in PLACEHOLDER_RE.finditer(content):
        placeholder_violations.append(f"unresolved placeholder: {m.group()!r}")
    invariants["6_no_placeholders"] = {
        "pass": not placeholder_violations,
        "violations": placeholder_violations,
    }

    # I7: required fields present (non-null, non-empty)
    # Every field declared in an ancestor's schema without `optional=true` must
    # be present on the entity with a non-empty value. An ancestor's schema
    # describes instances, so a universal that extends it (a kind of Letter, say)
    # is checked only for the identity fields every entity carries.
    missing = []
    sections = ("identity",) if fm.get("identity__type") == "universal" else ("identity", "relationships", "properties")
    for section in sections:
        for k in sorted(required[section]):
            fm_key = f"{section}__{k}"
            if fm_key not in fm or is_empty(fm[fm_key]):
                missing.append(f"required {fm_key} is missing or empty")
    invariants["7_required_fields_present"] = {
        "pass": not missing,
        "violations": missing,
    }

    all_pass = all(inv["pass"] for inv in invariants.values())
    return {
        "entity": entity,
        "result": "PASS" if all_pass else "FAIL",
        "invariants": invariants,
    }, None


def render_text(report):
    lines = [f"Entity: [[{report['entity']}]]", f"Result: {report['result']}", ""]
    for k, inv in report["invariants"].items():
        status = "PASS" if inv["pass"] else "FAIL"
        lines.append(f"Invariant {k}: {status}")
        for v in inv["violations"]:
            lines.append(f"  - {v}")
    return "\n".join(lines)


def main():
    ap = argparse.ArgumentParser(description="Validate a Khora vault entity.")
    ap.add_argument("entity", help="Entity name (wikilink-style, no .md) or vault-relative path")
    ap.add_argument("--format", choices=["text", "json"], default="text")
    args = ap.parse_args()

    report, err = validate(args.entity)
    if err:
        print(f"error: {err}", file=sys.stderr)
        sys.exit(2)

    if args.format == "json":
        print(json.dumps(report, indent=2))
    else:
        print(render_text(report))

    sys.exit(0 if report["result"] == "PASS" else 1)


if __name__ == "__main__":
    main()
