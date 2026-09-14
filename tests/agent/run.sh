#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
cd "$ROOT_DIR"

LOCAL_REPORT="tests/agent/results.jsonl"
LOG_TAG="JoharAgent"
LOG_PREFIX="JOHAR_AGENT_RESULT "
TEST_CLASS="com.akeshridev.johar.Johar1000CommandUiTest"

command -v adb >/dev/null 2>&1 || { echo "adb is required" >&2; exit 1; }
command -v python3 >/dev/null 2>&1 || { echo "python3 is required" >&2; exit 1; }

if ! adb get-state >/dev/null 2>&1; then
  echo "No adb device connected" >&2
  exit 1
fi

# Generate the deterministic matrix before Gradle packages androidTest assets.
python3 tests/agent/generate_1000_cases.py

: > "$LOCAL_REPORT"

# The 1000-case suite can run close to an hour on an emulator. Keep enough
# device log history for all JSON rows, then clear any previous-run results.
adb logcat -G 32M >/dev/null 2>&1 || true
adb logcat -c

echo "Running Johar 1000-command UI baseline..."
./gradlew :app:connectedDebugAndroidTest \
  -Pandroid.testInstrumentationRunnerArguments.class="$TEST_CLASS"

echo "Collecting machine-readable results from device logcat..."
adb logcat -d -v raw -s "${LOG_TAG}:I" '*:S' \
  | sed -n "s/^${LOG_PREFIX}//p" \
  > "$LOCAL_REPORT"

RESULT_COUNT="$(wc -l < "$LOCAL_REPORT" | tr -d ' ')"
if [[ "$RESULT_COUNT" -ne 1000 ]]; then
  echo "Expected 1000 result rows, found $RESULT_COUNT" >&2
  echo "Instrumentation completed, but result export is incomplete." >&2
  exit 2
fi

python3 tests/agent/summarize_results.py "$LOCAL_REPORT"

echo
echo "Baseline results: $LOCAL_REPORT"
