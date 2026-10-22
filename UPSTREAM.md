# Upstream permission requests

Status checked 2026-10-21. Pending requests and reactions are not redistribution grants. DFRoot's maintainer has merged the GNU GPL version 2 license into upstream master; this does not independently clear later DirtyFrag contributions or separately owned third-party code.

| Ancestor / credited source | Request | Status |
|---|---|---|
| V4bel/dirtyfrag | https://github.com/V4bel/dirtyfrag/issues/76 | pending |
| LSPosed/LSPromise | https://github.com/LSPosed/LSPromise/issues/4 | pending |
| polygraphene/DFReroot | https://github.com/polygraphene/DFReroot/issues/10 | pending |
| combeng6th/DirtyInit | https://github.com/combeng6th/DirtyInit/issues/1 | pending |
| diabl0w/DFRoot | https://github.com/diabl0w/DFRoot/pull/171 | **Merged** by diabl0w as commit `4ec133c15c2b30460399e54afc0bfb0b5017bd79`; full GPL version 2 text now in upstream `master` ([LICENSE](https://github.com/diabl0w/DFRoot/blob/master/LICENSE)). Version-only vs or-later designation not explicitly confirmed. |
| mitschud/DirtyFrag | https://github.com/mitschud/DirtyFrag/issues/72 | Maintainer replied 2026-10-21 that he could not license the fork while DFRoot remained unlicensed and advised waiting for diabl0w. DFRoot has since adopted GPLv2; no separate explicit license from mitschud for his own original additions has been verified. Follow-up needed. |

DFRoot PR #171 was merged by upstream maintainer diabl0w after requesting “GPL2 covering the full prject please.” This is direct evidence of upstream license adoption. The merged license text is GPL version 2; any `-or-later` permission needs separate confirmation. Its scope does not automatically extend to later DirtyFrag additions or independently owned third-party code.

Direct fork point: `ccd61e7c516df1469d335e41638e881328136dbc`.
The root LICENSE applies only to original SysopBP contributions, not the whole inherited repository. Preserve all component notices. See PROVENANCE.md for detailed evidence and historical checkpoints.

## Release verification checkpoint (2026-10-21)

- DFRoot upstream GPLv2 license merged as `4ec133c`; verified against upstream `master`.
- FragX release `v0.1.0-preview.1` remains a **draft**. Its attached `FragX-preview1.apk` and source archive refer to historical commit `780a8ea`, build #8, **not** the latest signed build #21 or subsequent commits. Do not present the attached APK as current.
- Build #21 completed successfully (`39618419710`); build #22 was still running at this checkpoint. A green build alone does not prove source-archive correspondence, signing continuity, or license clearance.
- Signing/update PR #2 (`fix/permanent-signing-preview-updates`) remains **open and draft**, not merged. Its README mentions pending secrets, so revalidate that text against the subsequently configured repository secrets without exposing them.
- Other upstream requests (#76, #4, #10, #1) had no issue comments when checked; DirtyFrag #72 has the response summarized above.
- Release gate: reconcile PR #2 with current `master`, verify final signing certificate, attach source and notices corresponding to the final APK, and finish inherited-contribution permissions review before publishing.
