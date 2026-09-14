#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
cd "$ROOT_DIR"

LOCAL_REPORT="tests/agent/results.jsonl"
BATCH_REPORT="tests/agent/.batch-results.jsonl"
LOG_TAG="JoharAgent"
LOG_PREFIX="JOHAR_AGENT_RESULT "
TEST_CLASS="com.akeshridev.johar.Johar1000CommandUiTest"
PACKAGE="com.akeshridev.johar"
BATCH_SIZE=50
TOTAL_CASES=1000

command -v adb >/dev/null 2>&1 || { echo "adb is required" >&2; exit 1; }
command -v python3 >/dev/null 2>&1 || { echo "python3 is required" >&2; exit 1; }

if ! adb get-state >/dev/null 2>&1; then
  echo "No adb device connected" >&2
  exit 1
fi

python3 tests/agent/generate_1000_cases.py

: > "$LOCAL_REPORT"
: > "$BATCH_REPORT"

adb logcat -G 16M >/dev/null 2>&1 || true

for ((START_ID=1; START_ID<=TOTAL_CASES; START_ID+=BATCH_SIZE)); do
  END_ID=$((START_ID + BATCH_SIZE - 1))
  if (( END_ID > TOTAL_CASES )); then
    END_ID=$TOTAL_CASES
  fi

  echo
  echo "Running Johar baseline batch ${START_ID}-${END_ID}..."

  # Restart the target app process between shards so UI/map/native resources from
  # earlier cases cannot accumulate until the Android app heap reaches its limit.
  adb shell am force-stop "$PACKAGE" >/dev/null 2>&1 || true
  adb logcat -c

  ./gradlew :app:connectedDebugAndroidTest \
    -Pandroid.testInstrumentationRunnerArguments.class="$TEST_CLASS" \
    -Pandroid.testInstrumentationRunnerArguments.startId="$START_ID" \
    -Pandroid.testInstrumentationRunnerArguments.endId="$END_ID"

  adb logcat -d -v raw -s "${LOG_TAG}:I" '*:S' \
    | sed -n "s/^${LOG_PREFIX}//p" \
    > "$BATCH_REPORT"

  EXPECTED_BATCH_COUNT=$((END_ID - START_ID + 1))
  BATCH_COUNT="$(wc -l < "$BATCH_REPORT" | tr -d ' ')"
  if [[ "$BATCH_COUNT" -ne "$EXPECTED_BATCH_COUNT" ]]; then
    echo "Expected $EXPECTED_BATCH_COUNT rows for batch ${START_ID}-${END_ID}, found $BATCH_COUNT" >&2
    echo "Stopping before the baseline becomes incomplete." >&2
    exit 2
  fi

  cat "$BATCH_REPORT" >> "$LOCAL_REPORT"
  TOTAL_CAPTURED="$(wc -l < "$LOCAL_REPORT" | tr -d ' ')"
  echo "Captured ${TOTAL_CAPTURED}/${TOTAL_CASES} cases."
done

rm -f "$BATCH_REPORT"

RESULT_COUNT="$(wc -l < "$LOCAL_REPORT" | tr -d ' ')"
if [[ "$RESULT_COUNT" -ne "$TOTAL_CASES" ]]; then
  echo "Expected $TOTAL_CASES result rows, found $RESULT_COUNT" >&2
  exit 2
fi

python3 tests/agent/summarize_results.py "$LOCAL_REPORT"

echo
echo "Baseline results: $LOCAL_REPORT"
