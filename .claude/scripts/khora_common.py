"""Shared helpers for the kit's scripts: which vault to talk to, and how to call the Obsidian CLI.

The vault is the repository folder, so its name is the folder's name. KHORA_VAULT overrides it.
Obsidian resolves vault names without regard to case, so before the first call this module
checks that the named vault's path really is this repository.
"""
import os
import subprocess

OBSIDIAN = ["obsidian"]
_checked = set()


class KitError(Exception):
    pass


def repo_root(cwd=None):
    r = subprocess.run(["git", "rev-parse", "--show-toplevel"], cwd=cwd,
                       capture_output=True, text=True, encoding="utf-8")
    if r.returncode != 0:
        raise KitError("not inside a git repository")
    return r.stdout.strip()


def vault_name(cwd=None):
    return os.environ.get("KHORA_VAULT") or os.path.basename(repo_root(cwd))


def _run(args, timeout):
    try:
        r = subprocess.run(OBSIDIAN + list(args), capture_output=True, text=True,
                           encoding="utf-8", errors="replace", timeout=timeout)
    except (subprocess.TimeoutExpired, FileNotFoundError):
        raise KitError("Obsidian is not running or the CLI is off")
    out = r.stdout or ""
    if "Unable to connect" in out or "Unable to connect" in (r.stderr or ""):
        raise KitError("Obsidian is not running or the CLI is off")
    return out


def _same_path(a, b):
    return os.path.normcase(os.path.normpath(a)) == os.path.normcase(os.path.normpath(b))


def _check_vault(name, root, timeout):
    if (name, root) in _checked:
        return
    info = _run(["vault=" + name, "vault"], timeout)
    path = next((l.split("\t", 1)[1].strip() for l in info.splitlines() if l.startswith("path\t")), "")
    if not path:
        raise KitError(f"Obsidian has no vault named '{name}'. Open this repository folder in Obsidian.")
    if not _same_path(path, root):
        raise KitError(f"vault '{name}' points at {path}, not this repository ({root}). "
                       "Set KHORA_VAULT to this repository's vault name.")
    _checked.add((name, root))


def obsidian(*args, timeout=30, repo_root=None):
    """Run `obsidian vault=<name> <args>` against this repository's vault; return stdout, leading whitespace stripped."""
    root = repo_root if repo_root is not None else globals()["repo_root"]()
    name = vault_name()
    _check_vault(name, root, timeout)
    return _run(["vault=" + name] + list(args), timeout).lstrip()
