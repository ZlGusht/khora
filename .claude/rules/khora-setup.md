# Khora — environment setup and troubleshooting (macOS and Windows)

The person may not be technical. They talk; you do the work, explain in plain words, and fix problems yourself where you can. Never ask them to read these files. Detect the OS once with `uname -s` (`Darwin` = macOS, `MINGW*`/`MSYS*` = Windows Git Bash) and follow the matching column below.

## Environment

| | macOS | Windows |
|---|---|---|
| Agent shell | Claude Code's Bash (zsh/bash) | Claude Code's Bash tool = **Git Bash** (Git for Windows is required by Claude Code). Do vault work in Bash, **never PowerShell** — PowerShell truncates `content=` at `]]` and mangles quotes. |
| Obsidian app | `/Applications/Obsidian.app` | installed per-user under `%LOCALAPPDATA%\Obsidian` |
| CLI | `obsidian` | `obsidian` (Git Bash resolves `obsidian.exe`) |
| Python for the validator | usually `python3` | usually `python` (`python3` may be a Microsoft Store stub that does nothing) |
| Quit Obsidian fully | Cmd+Q | close the window **and** check the system tray; or `taskkill //IM Obsidian.exe` |

Platform-neutral helpers — always use these instead of OS-specific commands:

- `bash .claude/scripts/khora-tmp` — Obsidian's temp folder, root of the plugin queue dirs.
- `bash .claude/scripts/khora-uuid` — a fresh lowercase UUID.
- `bash .claude/scripts/khora-validate "<name or blob/path.md>"` — the entity validator, run with whichever Python has PyYAML.
- `date +%F` — today's date.

Every CLI call: `obsidian vault=<vault> <verb> …`. Obsidian must be **running** with the repository folder open — the CLI talks to the running app.

## Setup check (run on first contact, and whenever something fails)

1. `which obsidian` → found. If not, the CLI isn't enabled: in Obsidian, Settings → General → turn on the command line interface (follow what Obsidian shows; labels vary by version). The command only appears in **new** shells — a fresh Claude Code session may be needed.
2. `obsidian vault=<vault> vault` → its `path` is this repository. If it names another folder, another vault has the same name (Obsidian ignores case): rename that vault's folder, or set `KHORA_VAULT`. `obsidian vault=<vault> files folder=blob` → lists the entities. If the vault isn't found, the person must open this repository folder in Obsidian ("Open folder as vault").
3. `obsidian vault=<vault> commands` → includes `khora:dataview-query` and `khora:semantic-search`. If missing, community plugins are off (Restricted Mode): Settings → Community plugins → turn on; make sure Dataview, Smart Connections and Khora are enabled; then fully quit Obsidian and reopen.
4. A Dataview query (`LIST FROM "blob"`) via `khora-query-entities` returns results.
5. A semantic search via `khora-search-entities` returns ranked hits. On first launch Smart Connections downloads a small model and indexes the vault — "collections not yet loaded" or empty results in the first minutes means wait and retry.
6. `bash .claude/scripts/khora-validate "BaseEntity v1.0.0"` → `Result: PASS`. If it says no Python with PyYAML and a TOML parser (`tomli` is needed on Python 3.10 and older, which includes the `python3` that ships with macOS):
   - **macOS:** if `python3` pops a "install command line developer tools" dialog, ask the person to click Install and wait. Then `python3 -m pip install --user pyyaml tomli`.
   - **Windows:** install Python 3 from python.org (tick "Add python.exe to PATH") or `winget install Python.Python.3.12`, open a new session, then `python -m pip install --user pyyaml tomli`.
7. `gh auth status` → logged in. If not: `gh auth login`. Without it, ingestion can't open pull requests and the stale report skips its issue and pull-request checks.
8. In Claude Code, `/plugin` lists superpowers at v7.0.2 or later. The repository asks for it in `.claude/settings.json`; accept the prompt to install it.

## Recordings

- Record with Wispr Flow. Export each recording as Markdown, named `YYYY-MM-DD <topic>.md`, unedited, to the transcripts folder outside this repository: `KHORA_TRANSCRIPTS` if set, else a `khora-transcripts` folder beside the repository.
- Transcripts never enter the repository: it is open source and they hold personal information. The same holds for exported conversations.
- To share them in the team, sync that folder (e.g. with Syncthing), never through git.
- Wispr Flow does not label speakers reliably. Ingestion leaves a statement unattributed, or marks it `[?]`, rather than guess.

## Troubleshooting (symptom → fix)

- **`obsidian: command not found`** → CLI not enabled, or the shell started before it was enabled. See check 1.
- **CLI hangs or can't connect** → Obsidian isn't running. Ask the person to open it (macOS: `open -a Obsidian`; Windows: Start menu → Obsidian), wait a few seconds, retry.
- **No `khora:` commands** → community plugins off. See check 3.
- **Plugin queue never answers** → you wrote to the wrong folder. Always use `khora-tmp`; never `$TMPDIR`, and on Windows never a hand-built `/tmp` path.
- **Semantic search fails but Dataview works** → Smart Connections still indexing, or an Obsidian update changed internals it relies on. Wait and retry; if it persists, check `obsidian vault=<vault> dev:errors` and tell the person plainly that semantic search is unavailable for now — everything else still works.
- **A read returns empty** → transient; retry once. Reads by versioned name: always `path="blob/<name> vX.Y.Z.md"` with explicit `.md`.
- **Read output starts with a blank line** → CLI ornament. Strip leading whitespace before parsing or writing it back, or the file grows a blank line each round-trip.
- **Garbled em-dashes / quotes on Windows** → a script decoded CLI output as cp1252. Force UTF-8 (`encoding="utf-8"` in Python).
- **Validation fails "required field missing"** → a property required along the is_a chain is empty; set it and re-validate.
- **Validation fails once on a chain read, then passes** → transient CLI read; re-run once before treating it as real.
- **Plugin behaviour didn't change after updating plugin files** → plugins only reload on a full Obsidian quit and reopen.

## What this setup does NOT do (say so honestly when asked)

- **The agent doesn't start conversations by itself.** The Team Weekly runs when someone asks for it. A recurring reminder needs a scheduled routine set up separately.
- **No email, calendar, or messaging access** unless the person connects those to Claude separately. The weekly Laydown asks about the calendar instead of reading it.
