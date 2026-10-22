# FragX + KitsuSU Preview 1 — release readiness (2026-10-21)

## Verified

- DFRoot upstream maintainer merged GPL version 2 license: https://github.com/diabl0w/DFRoot/pull/171 (merge commit `4ec133c15c2b30460399e54afc0bfb0b5017bd79`).
- FragX Build #21 completed successfully: https://github.com/SysopBP/FragX/actions/runs/39618419710 . Its APK artifact is a CI artifact, not yet a verified release asset.
- The provenance workflow #16 completed successfully.
- Draft release: https://github.com/SysopBP/FragX/releases/tag/v0.1.0-preview.1 .

## Must reconcile before publishing

1. Draft release contains `FragX-preview1.apk` and `FragX-780a8ea-source.tar.gz` from the older Build #8. Replace with a signed APK and exact corresponding source from the same tested commit. Regenerate `SHA256SUMS.txt` and refresh the release's build identifiers and notes.
2. Permanent-signing/update-check PR #2 remains open and draft. Reconcile it with current master; validate signing certificate fingerprint against the privately retained keystore and perform upgrade-install tests. Do not commit keystores or passwords.
3. Build #22 (`39660966220`) was cancelled, not a successful release candidate. Trigger and verify a clean final build after source changes.
4. The FragX root LICENSE is explicitly scoped to original SysopBP contributions. Do not describe inherited DFRoot, DirtyFrag or third-party code as MIT. Preserve the DFRoot GPLv2 license and all separate component notices.
5. mitschud's response in https://github.com/mitschud/DirtyFrag/issues/72 predates the DFRoot GPLv2 merge. Confirm the licensing of his independently authored additions; four other upstream requests had no comments when last checked. Track unresolved items at https://github.com/SysopBP/FragX/issues/1 .
6. Recheck KitsuSU APK/source pairing, its separate GPLv3/GPLv2 license texts, notice files and all bundled third-party components.
7. README still has DirtyFrag branding and legacy local signing instructions. Update those against the actual final build workflow before advertising them as authoritative.

**Publication status:** Not yet verified for public distribution. A draft is appropriate until the above release blockers are resolved. A successful build or upstream DFRoot GPLv2 merge does not by itself establish complete downstream license compliance.
