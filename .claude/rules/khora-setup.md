# Khora — environment setup and troubleshooting (macOS, Windows and Linux)

The person may not be technical. They talk; you do the work, explain in plain words, and fix problems yourself where you can. Never ask them to read these files. Detect the OS once with `uname -s` (`Darwin` = macOS, `MINGW*`/`MSYS*` = Windows Git Bash, `Linux` = Linux) and follow the matching column below. On Linux, also read the Linux section: a machine without a screen runs Obsidian headless.

## Environment

| | macOS | Windows | Linux |
|---|---|---|---|
| Agent shell | Claude Code's Bash (zsh/bash) | Claude Code's Bash tool = **Git Bash** (Git for Windows is required by Claude Code). Do vault work in Bash, **never PowerShell** — PowerShell truncates `content=` at `]]` and mangles quotes. | bash |
| Obsidian app | `/Applications/Obsidian.app` | installed per-user under `%LOCALAPPDATA%\Obsidian` | the `.deb` or AppImage; headless on a machine without a screen |
| CLI | `obsidian` | `obsidian` (Git Bash resolves `obsidian.exe`) | `obsidian` on a desktop; `bash .claude/scripts/obs` headless |
| Python for the validator | usually `python3` | usually `python` (`python3` may be a Microsoft Store stub that does nothing) | `python3` |
| Quit Obsidian fully | Cmd+Q | close the window **and** check the system tray; or `taskkill //IM Obsidian.exe` | close the window; headless: `obsidian-headless.sh stop` |

Platform-neutral helpers — always use these instead of OS-specific commands:

- `bash .claude/scripts/khora-tmp` — Obsidian's temp folder, root of the plugin queue dirs.
- `bash .claude/scripts/khora-uuid` — a fresh lowercase UUID.
- `bash .claude/scripts/khora-validate "<name or blob/path.md>"` — the entity validator, run with whichever Python has PyYAML.
- `bash .claude/scripts/obs` — the CLI, pointed at the right Obsidian. Plain `obsidian` on a desktop; on headless Linux it sets the daemon's HOME and DISPLAY.
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

## Linux

Two cases. Find out which with `echo "$DISPLAY$WAYLAND_DISPLAY"`: empty means no screen, so headless.

### Linux desktop

1. Install Obsidian from https://obsidian.md/download as the `.deb` (Debian, Ubuntu) or the AppImage. Avoid the Flatpak and Snap builds: their sandbox gives Obsidian a temp folder the agent can't see, and the plugin queue never answers. If one is already installed, set `KHORA_OBSIDIAN_TMP` to the temp folder it uses.
2. Open the repository folder as a vault, trust it, turn community plugins on, and turn on the command line interface (Settings → General). The AppImage's CLI is only on PATH if Obsidian put it there; check `which obsidian` in a new shell.
3. Python: `sudo apt install python3 python3-yaml python3-tomli` (or `python3 -m pip install --user pyyaml tomli`).
4. Then run the setup check above. `obsidian` works as on macOS.

### Linux without a screen (server, VM, container)

Obsidian runs as a daemon under a virtual display, started by `.claude/scripts/obsidian-headless.sh`. The CLI only reaches it with the daemon's HOME and DISPLAY, so every hand-made call goes through `bash .claude/scripts/obs`, which sets them. The kit's scripts do the same by themselves.

1. Packages (Debian, Ubuntu):
   `sudo apt install xvfb dbus-x11 python3 python3-yaml python3-tomli libnss3 libnspr4 libgbm1 libxshmfence1 libgtk-3-0 libcups2 libdrm2 libasound2 libxss1 libsecret-1-0`
   (`libasound2t64` on Ubuntu 24.04 and later.)
2. Obsidian: download the `.deb` from https://obsidian.md/download and `sudo apt install ./obsidian_<version>_amd64.deb`.
3. Choose where this Obsidian keeps its config, and keep it set in every shell that works on the vault (e.g. in `~/.bashrc`):
   `export KHORA_OBS_HOME="$HOME/.khora-obs"` and, if `:99` is taken, `export KHORA_OBS_DISPLAY=:98`.
4. Start it, in the foreground, from the repository root:
   `bash .claude/scripts/obsidian-headless.sh start "$(git rev-parse --show-toplevel)"`
   Never start it as a background job of an agent harness; the harness kills the job's process group when it reaps it.
5. The first start comes up in Restricted Mode, so the community plugins don't load. Once:
   `bash .claude/scripts/obs vault=<vault> plugins:restrict off`, then `bash .claude/scripts/obs vault=<vault> restart`, wait ~15 s.
6. Run the setup check above, with `bash .claude/scripts/obs` in place of `obsidian`.

Operating it:
- `bash .claude/scripts/obsidian-headless.sh status` / `stop`. `start` is safe to repeat: it does nothing when the daemon answers, and replaces a hung one.
- **Exactly one Obsidian per `KHORA_OBS_HOME`.** Two break Dataview's index: every query then answers with nothing while semantic search still works. Fix with `stop`, then `start`.
- **A CLI call while no daemon runs starts a stray Obsidian.** If calls hang or say they can't connect, run `status` and `start` before anything else.
- **Plugin or Obsidian updates need a restart:** `stop`, then `start`.
- **Agent sandboxes on Linux** may block the daemon's socket or fail to start at all. If every command fails before running, the agent needs to run unsandboxed; if only CLI calls fail, allow Unix sockets in the sandbox settings.
- Obsidian updates itself. If semantic search breaks after an update while Dataview works, read `bash .claude/scripts/obs vault=<vault> dev:errors` before anything else.

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
