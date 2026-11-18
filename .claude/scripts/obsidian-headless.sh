#!/usr/bin/env bash
# obsidian-headless — run Obsidian Desktop on a Linux machine with no screen, so the
# `obsidian` CLI (and so the whole kit) works on a server, a VM or a container.
#
# It pre-seeds Obsidian's vault registry with the CLI enabled, starts a virtual display
# and a dbus session, launches Obsidian with container-safe Electron flags in its own
# session, and waits until the CLI answers. Exactly one Obsidian runs per KHORA_OBS_HOME.
#
# Usage (from the repository root):
#   export KHORA_OBS_HOME="$HOME/.khora-obs"     # where this Obsidian keeps its config
#   bash .claude/scripts/obsidian-headless.sh start "$(git rev-parse --show-toplevel)"
#   bash .claude/scripts/obsidian-headless.sh status
#   bash .claude/scripts/obsidian-headless.sh stop
# Then call the CLI through `bash .claude/scripts/obs`, which sets HOME and DISPLAY from
# KHORA_OBS_HOME and KHORA_OBS_DISPLAY.
#
# Env:
#   KHORA_OBS_HOME     HOME for Obsidian's config (required for start/status/stop)
#   KHORA_OBS_DISPLAY  X display to use (default :99)
#
# Needs: obsidian (the .deb, or /opt/Obsidian/obsidian), Xvfb, dbus-launch, python3, and
# Electron's libraries (libnss3, libnspr4, libgbm1, libxshmfence1, libgtk-3-0, libcups2,
# libdrm2, libasound2). See .claude/rules/khora-setup.md.
#
# Run it in the foreground, never as a background job of an agent harness: when the
# harness reaps a job it kills the job's process group. setsid below protects Obsidian,
# but the launcher's own wait would be cut short.
set -u

CMD="start"
case "${1:-}" in
  start|status|stop) CMD="$1"; shift ;;
esac
VAULT_DIR="${1:-$PWD}"
VAULT_DIR="$(cd "$VAULT_DIR" 2>/dev/null && pwd || echo "$VAULT_DIR")"

if [ -z "${KHORA_OBS_HOME:-}" ]; then
  echo "error: set KHORA_OBS_HOME (e.g. export KHORA_OBS_HOME=\"\$HOME/.khora-obs\")" >&2
  exit 2
fi
OBS_HOME="$KHORA_OBS_HOME"
DISPLAY_NUM="${KHORA_OBS_DISPLAY:-:99}"
CFG="$OBS_HOME/.config/obsidian"
LOG="/tmp/obsidian-headless-$(id -u).log"

OBS_BIN="$(command -v obsidian || true)"
[ -z "$OBS_BIN" ] && [ -x /opt/Obsidian/obsidian ] && OBS_BIN=/opt/Obsidian/obsidian
if [ -z "$OBS_BIN" ]; then
  echo "error: obsidian not found (looked on PATH and /opt/Obsidian/obsidian)" >&2
  exit 1
fi

export HOME="$OBS_HOME"
export DISPLAY="$DISPLAY_NUM"

# Detect a running instance by Electron's singleton files, not by calling the CLI: a CLI
# call with no instance up starts a standalone app, and polling would start a swarm.
instance_up() { [ -e "$CFG/SingletonSocket" ] || [ -e "$CFG/SingletonLock" ]; }
# A connected `obsidian vault` prints name/path fields; a failed one still exits 0.
cli_ok() { obsidian vault 2>/dev/null | grep -qE '^(files|path)[[:space:]]'; }

# Kill every Obsidian process bound to THIS config home, and nothing else. Match on the
# process's real executable, not its command line: `pgrep -f obsidian` also matches this
# script and the caller's shell.
kill_this_home() {
  local pid h exe
  for pid in $(pgrep -f obsidian 2>/dev/null); do
    [ "$pid" = "$$" ] && continue
    exe=$(readlink -f "/proc/$pid/exe" 2>/dev/null) || continue
    case "$exe" in *[Oo]bsidian*) ;; *) continue ;; esac
    [ -r "/proc/$pid/environ" ] || continue
    h=$(tr '\0' '\n' < "/proc/$pid/environ" 2>/dev/null | grep -m1 '^HOME=')
    [ "$h" = "HOME=$OBS_HOME" ] && kill "$1" "$pid" 2>/dev/null
  done
}

case "$CMD" in
status)
  if instance_up && cli_ok; then
    echo "running: $(obsidian vault 2>/dev/null | grep -m1 '^path' | cut -f2)"; exit 0
  else echo "not running (or the CLI is not answering)"; exit 1; fi
  ;;
stop)
  kill_this_home -TERM; sleep 2; kill_this_home -KILL
  rm -f "$CFG/SingletonLock" "$CFG/SingletonSocket" "$CFG/SingletonCookie" 2>/dev/null
  echo "stopped Obsidian for $OBS_HOME"
  exit 0
  ;;
esac

# --- start ---
if [ ! -d "$VAULT_DIR/blob" ] || [ ! -d "$VAULT_DIR/.obsidian" ]; then
  echo "warning: $VAULT_DIR doesn't look like the kit's vault (no blob/ or .obsidian/)" >&2
fi

mkdir -p "$CFG"
if instance_up && cli_ok; then
  echo "Obsidian already running for: $(obsidian vault 2>/dev/null | grep -m1 '^path' | cut -f2)"
  exit 0
fi
# Nothing is serving this config home, but a hung Obsidian may still hold it. Kill it
# before starting a new one, so there is never more than one: two instances on one config
# home break Dataview's index (it then answers every query with nothing).
kill_this_home -TERM; sleep 2; kill_this_home -KILL
rm -f "$CFG/SingletonLock" "$CFG/SingletonSocket" "$CFG/SingletonCookie" 2>/dev/null

# 1. Register the vault and enable the CLI, keeping any vaults already registered.
python3 - "$CFG/obsidian.json" "$VAULT_DIR" <<'PY'
import hashlib, json, os, sys, time
cfg_path, vault = sys.argv[1], sys.argv[2]
data = {}
if os.path.exists(cfg_path):
    try:
        with open(cfg_path) as f:
            data = json.load(f)
    except Exception:
        data = {}
data.setdefault("vaults", {})
vid = hashlib.md5(vault.encode()).hexdigest()[:16]
data["vaults"][vid] = {"path": vault, "ts": int(time.time() * 1000), "open": True}
for k, v in data["vaults"].items():
    if k != vid and isinstance(v, dict):
        v["open"] = False
data["cli"] = True
with open(cfg_path, "w") as f:
    json.dump(data, f, indent=2)
print(f"registered vault {os.path.basename(vault)} -> {vault} (CLI enabled)")
PY

# 2. A dbus session bus (Electron uses it).
if [ -z "${DBUS_SESSION_BUS_ADDRESS:-}" ]; then
  eval "$(dbus-launch --sh-syntax)"
  export DBUS_SESSION_BUS_ADDRESS
fi

# 3. A virtual display, reused if one is already up on this number.
if ! (command -v xdpyinfo >/dev/null 2>&1 && xdpyinfo -display "$DISPLAY_NUM" >/dev/null 2>&1) \
   && ! pgrep -f "Xvfb $DISPLAY_NUM" >/dev/null 2>&1; then
  Xvfb "$DISPLAY_NUM" -screen 0 1280x800x24 >/dev/null 2>&1 &
  sleep 2
fi

# 4. Launch Obsidian in its own session (setsid), so a process-group kill of whatever ran
#    this script does not take Obsidian down. TMPDIR=/tmp fixes where the khora plugin's
#    queue lives, whatever TMPDIR the caller had (see khora-tmp).
echo "launching Obsidian (display $DISPLAY_NUM, config $OBS_HOME)..."
TMPDIR=/tmp setsid "$OBS_BIN" \
  --no-sandbox --no-zygote --disable-gpu --disable-software-rasterizer \
  --disable-dev-shm-usage --disable-setuid-sandbox \
  < /dev/null >"$LOG" 2>&1 &
APP_PID=$!
echo "obsidian pid=$APP_PID  log=$LOG"

# 5. Wait for the singleton files, then confirm the CLI answers.
for i in $(seq 1 45); do
  if ! kill -0 "$APP_PID" 2>/dev/null; then
    echo "✗ Obsidian exited during startup. Log tail:" >&2
    tail -15 "$LOG" 2>&1 | grep -v dbus/bus.cc >&2
    exit 1
  fi
  if instance_up && cli_ok; then
    echo "✓ CLI ready (~$((i * 2))s)."
    echo "  Keep KHORA_OBS_HOME=$OBS_HOME (and KHORA_OBS_DISPLAY=$DISPLAY_NUM) set, and call the CLI as:"
    echo "    bash .claude/scripts/obs vault=$(basename "$VAULT_DIR") vault"
    exit 0
  fi
  sleep 2
done
echo "✗ the CLI did not answer within ~90s. Log: $LOG" >&2
tail -15 "$LOG" 2>&1 | grep -v dbus/bus.cc >&2
exit 1
