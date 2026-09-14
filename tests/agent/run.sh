#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
cd "$ROOT_DIR"

PACKAGE="com.akeshridev.johar"
REMOTE_REPORT="files/johar-agent-results.jsonl"
LOCAL_REPORT="tests/agent/results.jsonl"

command -v adb >/dev/null 2>&1 || { echo "adb is required" >&2; exit 1; }

if ! adb get-state >/dev/null 2>&1; then
  echo "No adb device connected" >&2
  exit 1
fi

: > "$LOCAL_REPORT"

echo "Running Johar 200-command UI automation..."
./gradlew :app:connectedDebugAndroidTest

echo "Pulling machine-readable results..."
adb exec-out run-as "$PACKAGE" cat "$REMOTE_REPORT" > "$LOCAL_REPORT"

python3 tests/agent/summarize_results.py "$LOCAL_REPORT"

echo
echo "Results: $LOCAL_REPORT"
