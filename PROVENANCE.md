# Provenance

Opened 2026-10-21. Audited FragX baseline: `780a8ea624dbeaf82b91508db6bb82bfd58600d3`. Release remains a draft. This is an evidence record, not a declaration of clearance.

## Seven-project lineage index

The README credits are leads, not proof that every file follows each edge. Exact per-path ancestry remains under review.

| Project | Relationship | Current status |
|---|---|---|
| [V4bel/dirtyfrag](https://github.com/V4bel/dirtyfrag) | Ancestor or credited contributor; path-level tracing pending | No general grant demonstrated; request delivery blocked by integration HTTP 403 on 2026-10-21; NOT delivered |
| [LSPosed/lspromise](https://github.com/LSPosed/lspromise) | Ancestor or credited contributor; path-level tracing pending | No general grant demonstrated; request delivery blocked by integration HTTP 403 on 2026-10-21; NOT delivered |
| [polygraphene/DFReroot](https://github.com/polygraphene/DFReroot) | Ancestor or credited contributor; path-level tracing pending | No general grant demonstrated; request delivery blocked by integration HTTP 403 on 2026-10-21; NOT delivered |
| [combeng6th/DirtyInit](https://github.com/combeng6th/DirtyInit) | Ancestor or credited contributor; path-level tracing pending | No general grant demonstrated; request delivery blocked by integration HTTP 403 on 2026-10-21; NOT delivered |
| [diabl0w/DFRoot](https://github.com/diabl0w/DFRoot) | Ancestor or credited contributor; path-level tracing pending | No general grant demonstrated; request delivery blocked by integration HTTP 403 on 2026-10-21; NOT delivered |
| [mitschud/DirtyFrag](https://github.com/mitschud/DirtyFrag) | Ancestor or credited contributor; path-level tracing pending | No general grant demonstrated; request delivery blocked by integration HTTP 403 on 2026-10-21; NOT delivered |
| [SysopBP/FragX](https://github.com/SysopBP/FragX) | Current downstream | Original-contribution license decision pending; inherited rights unresolved |

Direct DirtyFrag fork point: `ccd61e7c516df1469d335e41638e881328136dbc`.

## Classification rules

A: demonstrated license, compliance review required. B: grant unresolved, outreach pending or obtainable. C: no grant and documented inability to reach the rights holder. Silence is not C. `provenance/files.json` inventories every tracked baseline path; unresolved entries are B, not a completed authorship audit. Existing notices must be preserved. Do not infer ownership from commit authorship alone.

## Licensed components

| Component | Evidence | Status |
|---|---|---|
| Odzhan include.inc | Full three-condition BSD-style header | Full text copied into THIRD_PARTY_NOTICES.md and APK res/raw/third_party_notices.txt |
| Inter subset | Font metadata and rsms/inter/LICENSE.txt OFL 1.1 | Full text in same locations; in-app screen still pending |
| Companion KitsuSU | Separate GPL-3.0-or-later manager/daemon and GPL-2.0-only kernel | Matching source archive and license texts prepared; dependency completeness remains open |

## Supporting arguments

GitHub public-repository terms permit viewing and GitHub-side forking. Any broader inference about modification or publication remains an unresolved supporting argument, not a substitute for a demonstrated redistribution grant, and does not establish permission to distribute APKs onto devices. No patent clearance is claimed.

## Module record

`provenance/modules.json` records hashes of the eight currently tracked modules. These are observations, not verified build attestations. Their exact source commits and toolchain provenance are unresolved. Current CI names KMI-specific ylarod/ddk-min images tagged 20260828; tags are not immutable image digests. Rebuild and compare before claiming a source-to-binary match. MODULE_LICENSE is loader metadata, not a source license grant.

## Open release gates

- Deliver six license-commit requests; record public links and replies. All six integration attempts failed with HTTP 403.
- Public tracking issue creation failed because Issues is disabled in FragX (HTTP 410).
- Audit all six histories, release terms and XDA thread; trace per-path earliest origin.
- Add an accessible licenses screen with full texts; res/raw packaging alone does not complete this UI gate.
- Verify complete corresponding source, build scripts and dependencies for each shipped GPL binary, offered together with that exact binary; a commit link alone is insufficient.
- Attach notices alongside release APKs; verify their presence inside the final APK.
- Pin module source, KMI, toolchain/container digest; rebuild, record imported symbols, and compare shipped checksums.
- Choose the license for original contributions and DCO policy without relicensing inherited contributions.
- Deliberately choose product name, applicationId and signing identity. Current df.root conflicts with DirtyFrag; signing key continuity has not been established.
- Light trademark search and patent issue notation remain pending; no clearance conclusion.
- Decide any minimum rewrite set only after classification; independent implementation is evidence-sensitive and not guaranteed clearance.

## Outreach and UI update — 2026-10-21

Tracking issue is open: https://github.com/SysopBP/FragX/issues/1 . The earlier disabled-Issues blocker is resolved. Browser delivery supersedes the initial integration failures for five projects:

| Project | Request | Status |
|---|---|---|
| V4bel/dirtyfrag | https://github.com/V4bel/dirtyfrag/issues/76 | Delivered; grant pending |
| LSPosed/LSPromise | https://github.com/LSPosed/LSPromise/issues/4 | Delivered; grant pending |
| mitschud/DirtyFrag | https://github.com/mitschud/DirtyFrag/issues/72 | Delivered; grant pending |
| polygraphene/DFReroot | https://github.com/polygraphene/DFReroot/issues/10 | Delivered; grant pending |
| combeng6th/DirtyInit | https://github.com/combeng6th/DirtyInit/issues/1 | Delivered; grant pending |
| diabl0w/DFRoot | No issue delivered | New issues restricted to collaborators; not classified unreachable |

The overflow menu now provides Licenses & credits, with selectable full BSD/OFL notices and full GPL v2/v3 texts. Including GPL texts does not assign a source license to inherited code or resolve missing grants. Release validation checks the packaged notice bytes and module hashes against a build-generated manifest. This establishes integrity within that build, not an independent reproducibility result or legal clearance.

## Current audit checkpoint — 2026-10-21

See [path audit](provenance/PATH_AUDIT.md), [per-path evidence](provenance/files.json), and [six upstream history snapshots](provenance/upstream-history.json). The audit records every baseline tracked path, its first revision at the current path and exact blob matches at six pinned upstream HEADs. Import history and rename tracing remain incomplete; this is not a completed ownership audit. Gradle wrapper scripts have explicit Apache-2.0 headers. No path is classified C.

User confirmed all license text views on the S26 Ultra. The phone UI check is complete. Five grant requests have been delivered and remain pending; DFRoot remains collaborator-restricted. Earlier delivery/UI blockers above are historical.

All eight container digests were recovered from successful Build #11 Docker-pull logs and recorded in provenance/ddk-images.json. CI now requests those exact digests and includes them in new build manifests. Independent reproducibility and full dependency notice review remain open.

## Pre-import checkpoint — 2026-10-21

LSPromise was previously a shallow one-commit checkout; it is now unshallowed to 12 commits. Repeating the license-path history scan still found no LICENSE/COPYING additions or deletions. Other five checkout histories were already non-shallow.

provenance/pre-import.json records exact whole-file matches against reachable historical objects: DFRoot initial-import elf_parser.c matches historical LSPromise and DFReroot blobs; the initial dirtyfrag-lkm/dirtyfrag.c matches DFReroot. Other reviewed initial code files have no whole-file match, which does not rule out copied portions. Import ancestry is not fully resolved.

Both Gradle wrapper scripts retain explicit Apache-2.0 headers. Added LICENSE-Apache-2.0.txt and included its full text in source and APK notices. This is a component-scoped compliance change; transitive Android dependency notices remain under review.

## Android direct-library checkpoint — 2026-10-21

Build #13 passed the pre-import/Gradle license checkpoint: https://github.com/SysopBP/FragX/actions/runs/39600124684 . Exact published POMs for AppCompat 1.7.1, Material 1.13.0 and ConstraintLayout 2.2.1 declare Apache-2.0. AAR license-path inspection and SHA-256 values are recorded in provenance/android-direct-dependencies.json. Their component names are now included in source/APK notices, using the already packaged full Apache text. Transitive dependency and source-level notices remain open. CI now exports the actual releaseRuntimeClasspath Gradle dependency report as DEPENDENCIES.txt with the build bundle, so further review can use the resolved graph rather than an inferred POM tree.
