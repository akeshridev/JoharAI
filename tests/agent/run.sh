#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
cd "$ROOT_DIR"

LOCAL_REPORT="tests/agent/results.jsonl"
LOG_TAG="JoharAgent"
LOG_PREFIX="JOHAR_AGENT_RESULT "

command -v adb >/dev/null 2>&1 || { echo "adb is required" >&2; exit 1; }

if ! adb get-state >/dev/null 2>&1; then
  echo "No adb device connected" >&2
  exit 1
fi

: > "$LOCAL_REPORT"

# Keep only this run's machine-readable result rows.
adb logcat -c

echo "Running Johar 200-command UI automation..."
./gradlew :app:connectedDebugAndroidTest

echo "Collecting machine-readable results from device logcat..."
adb logcat -d -v raw -s "${LOG_TAG}:I" '*:S' \
  | sed -n "s/^${LOG_PREFIX}//p" \
  > "$LOCAL_REPORT"

RESULT_COUNT="$(wc -l < "$LOCAL_REPORT" | tr -d ' ')"
if [[ "$RESULT_COUNT" -ne 200 ]]; then
  echo "Expected 200 result rows, found $RESULT_COUNT" >&2
  echo "Instrumentation completed, but result export is incomplete." >&2
  exit 2
fi

python3 tests/agent/summarize_results.py "$LOCAL_REPORT"

echo
echo "Results: $LOCAL_REPORT"
