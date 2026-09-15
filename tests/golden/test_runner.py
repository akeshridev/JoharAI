import copy
import tempfile
import unittest
from pathlib import Path
import run


class GoldenRunnerTest(unittest.TestCase):
    def setUp(self):
        self.cases = run.read_jsonl(run.ASSET)
        self.row = dict(id='G001', query='pani puri', finalResultType='Grounded',
                        finalVisibleAnswer='Golgappa: a snack', answerMode='DETERMINISTIC',
                        selectedEntities=[dict(entityId='food', name='Golgappa', type='FOOD')],
                        retrievalCandidates=[], spatialCalls=[], evidence=[dict(sourceUrl='https://example.org', value='a snack')],
                        sources=[dict(url='https://example.org')], classification=dict(liveGuard=False, nearbyExecuted=False),
                        elapsedMs=1, notes=[])

    def test_asset_has_exactly_100_curated_contracts(self):
        run.validate_cases(self.cases)
        self.assertEqual(14, len({c['category'] for c in self.cases}))

    def test_supported_alias_with_evidence_passes(self):
        self.assertEqual('PASS', run.classify(self.cases[0], self.row)[0])

    def test_unrelated_nonempty_answer_never_passes(self):
        self.row['selectedEntities'][0]['name'] = 'Tagore Hill'
        self.assertEqual('DATA_GAP', run.classify(self.cases[0], self.row)[0])

    def test_no_evidence_is_data_gap(self):
        self.row['evidence'] = []
        self.assertEqual('DATA_GAP', run.classify(self.cases[0], self.row)[0])

    def test_live_guard_and_unsupported_answer(self):
        live = next(c for c in self.cases if c['contract'] == 'live')
        self.assertEqual('SAFETY_FAIL', run.classify(live, self.row)[0])
        self.row['classification']['liveGuard'] = True
        self.assertEqual('PASS', run.classify(live, self.row)[0])
        unsupported = self.cases[-1]
        self.assertEqual('SAFETY_FAIL', run.classify(unsupported, self.row)[0])

    def test_route_without_engine_evidence_fails_safety(self):
        case = next(c for c in self.cases if c['contract'] == 'route')
        self.row['finalResultType'] = 'Route'
        self.assertEqual('SAFETY_FAIL', run.classify(case, self.row)[0])

    def test_live_question_misparsed_as_route_is_intent_gap_not_fabrication(self):
        case = next(c for c in self.cases if c['contract'] == 'live')
        self.row.update(finalResultType='Clarification', selectedEntities=[], evidence=[])
        self.assertEqual('PARSER/INTENT_GAP', run.classify(case, self.row)[0])

    def test_duplicate_and_missing_output_rejected(self):
        with tempfile.TemporaryDirectory() as temp:
            for rows in [[], [self.row, copy.deepcopy(self.row)]]:
                with self.assertRaises(AssertionError):
                    run.summarize(self.cases[:1], rows, Path(temp), {})


if __name__ == '__main__':
    unittest.main()
