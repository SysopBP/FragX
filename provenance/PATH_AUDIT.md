# Path lineage audit — 2026-10-21

Baseline: `46a64438341fee0f350a427789cac8dfe7028fc7`. Every tracked baseline path has history metadata in `files.json`. This records repository evidence, not copyright ownership.

## Pinned upstream snapshots

| Repository | Commit |
|---|---|
| V4bel/dirtyfrag | `aab16fcada27142dd8ce8704906cf6736cf213b8` |
| LSPosed/LSPromise | `0258165d91aa72451dcd8406934ba3fc34f52c14` |
| diabl0w/DFRoot | `c274137e8c02b4f94f64d1b132d991b80c747340` |
| mitschud/DirtyFrag | `ccd61e7c516df1469d335e41638e881328136dbc` |
| polygraphene/DFReroot | `9edc769518f721d4cd39ad16e638e322abd74c45` |
| combeng6th/DirtyInit | `3409c35e723c73c527f395548a653966148e8f88` |

No tracked LICENSE/COPYING/NOTICE paths found at these HEADs. The requested case-sensitive `*LICEN*` / `*COPYING*` addition/deletion history scan returned no results in fetched refs. Case-insensitive text scans located Odzhan notices, module loader metadata, and Gradle wrapper Apache-2.0 headers; these have component scope. Alternate filenames, release attachments and private grants are not ruled out.

## Core paths

| Path | Present at direct fork | First commit at this path | Exact matches at upstream HEAD |
|---|---|---|---|
| app/src/main/java/df/root/MainActivity.java | Yes | `247e9907cbd6d225cc06d6871b5657523ccd8554` | No exact match; does not imply independent creation |
| app/src/main/jni/CMakeLists.txt | Yes | `247e9907cbd6d225cc06d6871b5657523ccd8554` | diabl0w/DFRoot:app/src/main/jni/CMakeLists.txt; mitschud/DirtyFrag:app/src/main/jni/CMakeLists.txt |
| app/src/main/jni/aes256.h | Yes | `247e9907cbd6d225cc06d6871b5657523ccd8554` | diabl0w/DFRoot:app/src/main/jni/aes256.h; mitschud/DirtyFrag:app/src/main/jni/aes256.h |
| app/src/main/jni/bootstrap.c | Yes | `1728222916d8fe9a2cdf18019ab44a23cb299cd9` | mitschud/DirtyFrag:app/src/main/jni/bootstrap.c |
| app/src/main/jni/elf_parser.c | Yes | `247e9907cbd6d225cc06d6871b5657523ccd8554` | diabl0w/DFRoot:app/src/main/jni/elf_parser.c; mitschud/DirtyFrag:app/src/main/jni/elf_parser.c |
| app/src/main/jni/exp.c | Yes | `247e9907cbd6d225cc06d6871b5657523ccd8554` | mitschud/DirtyFrag:app/src/main/jni/exp.c |
| app/src/main/jni/include.inc | Yes | `247e9907cbd6d225cc06d6871b5657523ccd8554` | diabl0w/DFRoot:app/src/main/jni/include.inc; mitschud/DirtyFrag:app/src/main/jni/include.inc |
| app/src/main/jni/libcxx.S | Yes | `247e9907cbd6d225cc06d6871b5657523ccd8554` | diabl0w/DFRoot:app/src/main/jni/libcxx.S; mitschud/DirtyFrag:app/src/main/jni/libcxx.S |
| app/src/main/jni/splicehelper.c | Yes | `247e9907cbd6d225cc06d6871b5657523ccd8554` | diabl0w/DFRoot:app/src/main/jni/splicehelper.c; mitschud/DirtyFrag:app/src/main/jni/splicehelper.c |
| lkm/dfroot.c | Yes | `1728222916d8fe9a2cdf18019ab44a23cb299cd9` | mitschud/DirtyFrag:lkm/dfroot.c |

## Classification and rewrite decision

Inventory classifications: {'B': 96, 'A': 6}. A means a demonstrated component license, with compliance tasks potentially outstanding. B includes inherited grants and original-contribution licensing decisions still unresolved. No files are C: inability to reach a rights holder has not been established. Therefore no defensible minimum rewrite set is established yet. MainActivity.java and native files remain review priorities, not a predetermined replacement list.

## Limits and outstanding evidence

- Git history begins at imports for several paths. First commit at current path is not the first creation of the work; renamed paths and earlier ancestor snapshots still need tracing.
- Exact blob equality establishes identical content at pinned revisions, not who owns it or which license applies. Absence of a match is not evidence of independent creation.
- XDA thread 4804221 was located, but its opening post returned HTTP 403 through retrieval. No licensing conclusion drawn from that unavailable page.
- Release attachments and complete dependency notice inventory remain open. GitHub-only scope excludes store-policy work.
- User confirmed the licenses menu and all text views on the S26 Ultra; that UI check is complete.
- Build #11 verified notices and module bytes; immutable DDK digest pinning and independent rebuild comparison remain open.
