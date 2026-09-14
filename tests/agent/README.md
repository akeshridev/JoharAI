# Johar agent validation workspace

Temporary workspace for automated end-to-end validation of the installed Johar Android app.

Canonical test inventory: `docs/johar-200-command-validation.md`.

The goal is to let an agent run the full command matrix against the real UI, using stable Compose semantics/test tags, then write machine-readable observations into `results.jsonl`.

## Planned flow

`test id -> enter query -> tap Send -> wait for Johar -> inspect tagged result -> classify -> append JSONL result`

This directory is not product code and is not a permanent benchmark store. Delete it after the 200-command validation cycle and failure analysis are complete.

## Result ownership

`results.jsonl` is the scratch output file for the test agent/Codex-style runner. It may be overwritten or regenerated during test runs. Do not use it as source-of-truth product data.

## Next implementation

Expose stable tags in the branded chat UI and add the runner that consumes the 200-command inventory. The runner should avoid screen-coordinate automation wherever a semantic target is available.
