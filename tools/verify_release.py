"""Prepare build provenance or verify notices and embedded modules in an APK."""
import argparse
import hashlib
import json
from pathlib import Path
import subprocess
import zipfile

ROOT = Path(__file__).resolve().parents[1]
parser = argparse.ArgumentParser(description=__doc__)
parser.add_argument('--prepare', action='store_true')
parser.add_argument('--apk', type=Path)
args = parser.parse_args()
manifest_path = ROOT / 'app/src/main/assets/build_provenance.json'
if args.prepare:
    source = subprocess.check_output(['git', 'rev-parse', 'HEAD'], cwd=ROOT, text=True).strip()
    pins = json.loads((ROOT / "provenance/ddk-images.json").read_text())
    modules = []
    for path in sorted((ROOT / 'app/src/main/jni/ko').glob('*.ko')):
        kmi = path.stem.removeprefix('dfroot-')
        modules.append({'file': path.name, 'kmi': kmi,
                        'sha256': hashlib.sha256(path.read_bytes()).hexdigest(),
                        'source_commit': source,
                        'ddk_image_tag': f'ghcr.io/ylarod/ddk-min:{kmi}-20260828',
                        'container_digest': pins[kmi]['image'].split('@', 1)[1]})
    if len(modules) != 8:
        raise SystemExit('Expected eight modules')
    manifest = {'source_commit': source, 'modules': modules,
                'limits': 'CI build record with pinned image digest; independent reproducibility verification pending'}
    manifest_path.parent.mkdir(parents=True, exist_ok=True)
    manifest_path.write_text(json.dumps(manifest, indent=2) + '\n')
    (ROOT / 'MODULE_BUILD.json').write_bytes(manifest_path.read_bytes())
    (ROOT / 'THIRD_PARTY_NOTICES.txt').write_bytes((ROOT / 'THIRD_PARTY_NOTICES.md').read_bytes())
    subprocess.run(['git', 'archive', '--format=tar.gz', '--output=FragX-source.tar.gz', source], cwd=ROOT, check=True)
    print(f'Prepared source archive and module record for {source}')
if args.apk:
    manifest = json.loads(manifest_path.read_text())
    with zipfile.ZipFile(args.apk) as apk:
        if apk.read('assets/build_provenance.json') != manifest_path.read_bytes():
            raise SystemExit('Packaged build record differs')
        # AAPT may shorten resource paths; compare complete bytes, not ZIP names.
        packaged_resources = {apk.read(info.filename) for info in apk.infolist()
                              if info.filename.startswith('res/') and not info.is_dir()}
        for name in ['third_party_notices.txt', 'license_gpl_v2.txt', 'license_gpl_v3.txt']:
            expected = (ROOT / 'app/src/main/res/raw' / name).read_bytes()
            if expected not in packaged_resources:
                raise SystemExit(f'Missing or changed license text: {name}')
        executable = apk.read('lib/arm64-v8a/libdfroot.so')
        for entry in manifest['modules']:
            module = (ROOT / 'app/src/main/jni/ko' / entry['file']).read_bytes()
            if hashlib.sha256(module).hexdigest() != entry['sha256'] or module not in executable:
                raise SystemExit(f"Embedded module mismatch: {entry['file']}")
    print('APK has exact notice texts, build record and all eight module byte sequences')
