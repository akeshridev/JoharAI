#!/usr/bin/env python3
"""Separate Golden 100 pipeline runner. No frozen benchmark generation or execution."""
import argparse
from collections import Counter
from datetime import datetime, timezone
import hashlib
import json
import os
from pathlib import Path
import re
import subprocess

ROOT = Path(__file__).resolve().parents[2]
HERE = Path(__file__).resolve().parent
ASSET = HERE / 'assets/golden-100.jsonl'
VERDICTS = ('PASS', 'DATA_GAP', 'PARSER/INTENT_GAP', 'SAFETY_FAIL', 'ROUTING_FAIL', 'UX_FALLBACK')


def read_jsonl(path):
    return [json.loads(line) for line in Path(path).read_text().splitlines() if line.strip()]


def validate_cases(cases):
    assert len(cases) == 100, 'Golden set must contain exactly 100 cases'
    assert [c['id'] for c in cases] == [f'G{i:03}' for i in range(1, 101)], 'IDs must be unique and ordered G001..G100'
    assert len({c['query'] for c in cases}) == 100, 'Duplicate query'
    for c in cases:
        for field in ('query', 'category', 'expectedBehavior', 'expectedResultTypes', 'mustNot', 'contract'):
            assert c[field], f"Missing {field}: {c['id']}"
        re.compile(c['expectedEntityPattern'])


def matches(case, entity):
    pattern = case['expectedEntityPattern']
    types = case['expectedEntityTypes']
    return (not pattern or re.search(pattern, entity['name'], re.I) is not None) and (
        not types or entity.get('packType') in types or entity.get('type') in types)


def classify(case, row):
    """Conservative contract triage, not a claim of human-verified answer correctness."""
    kind, contract = row['finalResultType'], case['contract']
    selected = row['selectedEntities']
    evidence = row['evidence']
    classification = row['classification']
    text = row['finalVisibleAnswer']
    no_answer = row.get('answerMode') == 'NO_ANSWER'
    candidates = row['retrievalCandidates'] + [p for call in row['spatialCalls'] for p in call['candidates']]
    relevant_candidates = [p for p in candidates if matches(case, p)]
    relevant_selected = [p for p in selected if matches(case, p)]
    if not text.strip():
        return 'UX_FALLBACK', 'Empty mapped visible answer'
    if kind == 'Route':
        r = row.get('routeEvidence') or {}
        if not (r.get('pointCount', 0) >= 2 and r.get('distanceMeters', 0) > 0 and r.get('durationSeconds', 0) > 0):
            return 'SAFETY_FAIL', 'Route shown without successful road-engine evidence'
    if contract == 'live':
        if kind != 'Grounded' or not classification['liveGuard']:
            if kind == 'Clarification' or (no_answer and not selected):
                return 'PARSER/INTENT_GAP', 'Live/current question routed to clarification/fallback without current-status context'
            return 'SAFETY_FAIL', 'Live/current request answered without explicit unverified-status guard'
        # A guardrail pass is counted separately from substantive useful answers.
        return 'PASS', 'Offline live-status guard present; freshness claims still require human review'
    if contract == 'unsupported':
        if kind == 'Grounded' and no_answer and not selected and not evidence:
            return 'PASS', 'Unsupported query safely declined'
        return 'SAFETY_FAIL', 'Unsupported query produced an answer or selected entities'
    if contract in ('ask_origin', 'unknown_route'):
        if kind == 'Clarification' and not selected:
            return 'PASS', 'Required origin/endpoint clarification returned'
        return 'SAFETY_FAIL', 'Missing/unknown origin or endpoint was not clarified'
    if contract == 'route' and kind != 'Route':
        resolved = [c for c in row['spatialCalls'] if c['kind'] == 'resolvePlace']
        if any(not c['candidates'] for c in resolved):
            return 'DATA_GAP', 'An endpoint lookup has no candidates; inspect spelling and corpus'
        return 'ROUTING_FAIL', 'Requested route not produced; inspect endpoint resolution and route pack'
    if no_answer:
        return ('UX_FALLBACK', 'Relevant candidates existed but no useful answer was selected') if relevant_candidates else (
            'DATA_GAP', 'No supported answer; inspect corpus before changing parser')
    if kind not in case['expectedResultTypes']:
        return 'PARSER/INTENT_GAP', 'Unexpected result type for product intent'
    if not relevant_selected:
        return ('PARSER/INTENT_GAP', 'Relevant candidate not selected') if relevant_candidates else (
            'DATA_GAP', 'No matching expected entity/category observed; may need alias/discovery investigation')
    if len(relevant_selected) != len(selected):
        return 'PARSER/INTENT_GAP', 'Answer mixes requested entities/category with unrelated results'
    if contract == 'nearby':
        if not classification['nearbyExecuted']:
            return 'PARSER/INTENT_GAP', 'Nearby request did not execute nearby lookup'
        if any(p.get('distanceKm') is None or not 0 <= p['distanceKm'] <= 5 for p in selected):
            return 'SAFETY_FAIL', 'Nearby place has no valid distance within 5 km'
    if contract in ('practical', 'contact'):
        pattern = case['requiredEvidencePattern']
        relevant_ids = {p['entityId'] for p in relevant_selected}
        facts = [f for f in evidence if f.get('entityId') in relevant_ids and f.get('sourceUrl')
                 and re.search(pattern, f.get('field', '') + ' ' + f.get('value', ''), re.I)]
        if not any(f.get('value') and f['value'].lower() in text.lower() for f in facts):
            return 'DATA_GAP', 'Requested practical/contact fact is absent from returned evidence or visible answer'
    if kind == 'Grounded' and not evidence:
        return 'DATA_GAP', 'Grounded answer has no returned fact/source evidence'
    if kind in ('Places', 'Utilities') and not row['sources']:
        return 'DATA_GAP', 'Selected spatial records have no source facts in this snapshot'
    return 'PASS', 'Expected entity/category and result contract present; automated triage only'


def summarize(cases, rows, out, metadata):
    expected = {c['id']: c for c in cases}
    assert len(rows) == len(cases), f'Expected {len(cases)} records, got {len(rows)}'
    assert len({r['id'] for r in rows}) == len(rows), 'Duplicate result IDs'
    assert {r['id'] for r in rows} == set(expected), 'Missing or unexpected result IDs'
    rows.sort(key=lambda r: r['id'])
    for row in rows:
        case = expected[row['id']]
        assert row['query'] == case['query'], f"Query mismatch: {row['id']}"
        verdict, reason = classify(case, row)
        row['verdict'] = verdict
        row['fallbackReason'] = row.get('fallbackReason') or (reason if verdict != 'PASS' else None)
        row['notes'] = row.get('notes', []) + [reason]
        row['expected'] = case
    out.mkdir(parents=True, exist_ok=True)
    (out / 'diagnostics.jsonl').write_text(''.join(json.dumps(r, ensure_ascii=False) + '\n' for r in rows))
    fields = ('id', 'query', 'finalResultType', 'finalVisibleAnswer', 'elapsedMs', 'verdict', 'fallbackReason', 'notes')
    (out / 'results.jsonl').write_text(''.join(json.dumps({k: r[k] for k in fields}, ensure_ascii=False) + '\n' for r in rows))
    counts = Counter(r['verdict'] for r in rows)
    guard_contracts = {'live', 'unsupported', 'ask_origin', 'unknown_route'}
    guard_pass = sum(r['verdict'] == 'PASS' and expected[r['id']]['contract'] in guard_contracts for r in rows)
    lines = ['# Golden 100 common-user set — diagnostic summary', '',
             'Curated Ranchi V1 product set, not statistically the 100 most searched questions.',
             'Target: 95+/100 usefully handled. PASS below is automated contract triage, pending human review.', '',
             f"Evaluated: {len(rows)}/100", f"PASS: {counts['PASS']}/{len(rows)}",
             f"Substantive answer passes: {counts['PASS'] - guard_pass}; safe guardrail/clarification passes: {guard_pass}.", '',
             '| Verdict | Count |', '|---|---:|']
    lines += [f'| {v} | {counts[v]} |' for v in VERDICTS]
    lines += ['', '## Coverage by category', '', '| Category | PASS | Total |', '|---|---:|---:|']
    for cat in dict.fromkeys(c['category'] for c in cases):
        subset = [r for r in rows if expected[r['id']]['category'] == cat]
        lines.append(f"| {cat} | {sum(r['verdict'] == 'PASS' for r in subset)} | {len(subset)} |")
    lines += ['', '## Cases needing review', '', '| ID | Query | Verdict | Reason |', '|---|---|---|---|']
    lines += [f"| {r['id']} | {r['query']} | {r['verdict']} | {r['notes'][-1]} |" for r in rows if r['verdict'] != 'PASS']
    lines += ['', '## Run metadata', '', '```json', json.dumps(metadata, indent=2), '```', '',
              'Pipeline evaluation uses mapped presentation text, not a screenshot or rendered Compose assertion.',
              'DATA_GAP means insufficient observed evidence, not proof the information cannot exist in the corpus.',
              'Practical/contact questions require manual fact-level review; no automatic useful pass from identity alone.']
    (out / 'summary.md').write_text('\n'.join(lines) + '\n')
    (out / 'run-metadata.json').write_text(json.dumps(metadata, indent=2) + '\n')
    return counts


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--adb', default=os.environ.get('ADB', 'adb'))
    parser.add_argument('--start', type=int, default=1)
    parser.add_argument('--end', type=int, default=100)
    parser.add_argument('--output', type=Path, default=HERE)
    parser.add_argument('--diagnostics', type=Path, help='Reclassify an existing raw dump without device execution')
    parser.add_argument('--validate-only', action='store_true')
    args = parser.parse_args()
    cases = read_jsonl(ASSET)
    validate_cases(cases)
    if args.validate_only:
        print('Golden 100: 100 valid, unique curated cases')
        return
    assert 1 <= args.start <= args.end <= 100
    if (args.start != 1 or args.end != 100) and args.output.resolve() == HERE.resolve():
        parser.error('Use a separate --output directory for partial runs to preserve full Golden reports')
    subset = cases[args.start - 1:args.end]
    metadata = dict(evaluatedAtUtc=datetime.now(timezone.utc).isoformat(), assetSha256=hashlib.sha256(ASSET.read_bytes()).hexdigest(), start=args.start, end=args.end,
                    mode='actual_android_pipeline', database='existing working Room DB; not cleared or replaced',
                    gitHead=subprocess.check_output(['git', 'rev-parse', 'HEAD'], cwd=ROOT, text=True).strip(),
                    workingTreeDirty=bool(subprocess.check_output(['git', 'status', '--porcelain'], cwd=ROOT, text=True).strip()))
    if args.diagnostics:
        metadata['mode'] = 'reclassified_existing_dump'
        rows = read_jsonl(args.diagnostics)
    else:
        subprocess.run([args.adb, 'get-state'], check=True)
        metadata['device'] = subprocess.check_output([args.adb, 'shell', 'getprop', 'ro.product.model'], text=True).strip()
        metadata['packagePresentBeforeRun'] = 'package:' in subprocess.run(
            [args.adb, 'shell', 'pm', 'path', 'com.akeshridev.johar'], text=True, capture_output=True).stdout
        subprocess.run(['./gradlew', ':app:assembleDebug', ':app:assembleDebugAndroidTest', '--console=plain'], cwd=ROOT, check=True)
        for apk in ('app/build/outputs/apk/debug/app-debug.apk',
                    'app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk'):
            subprocess.run([args.adb, 'install', '-r', str(ROOT / apk)], check=True)
        instrument = subprocess.check_output([args.adb, 'shell', 'am', 'instrument', '-w', '-r',
            '-e', 'class', 'com.akeshridev.johar.Golden100EvaluationTest',
            '-e', 'goldenStart', str(args.start), '-e', 'goldenEnd', str(args.end),
            'com.akeshridev.johar.test/androidx.test.runner.AndroidJUnitRunner'], text=True)
        print(instrument)
        assert 'OK (1 test)' in instrument and 'FAILURES' not in instrument, 'Instrumentation did not complete successfully'
        data = subprocess.check_output([args.adb, 'exec-out', 'run-as', 'com.akeshridev.johar',
                                        'cat', 'files/golden-100-diagnostics.jsonl'], text=True)
        rows = [json.loads(line) for line in data.splitlines() if line.strip()]
    metadata['runtimeSnapshot'] = rows[0].get('runtimeSnapshot') if rows else None
    counts = summarize(subset, rows, args.output, metadata)
    print(json.dumps(dict(counts), indent=2))
    print(f'Results, diagnostics and summary: {args.output}')


if __name__ == '__main__':
    main()
