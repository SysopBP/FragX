"""Check origin inventory coverage and observed prebuilt-module integrity."""
import hashlib
import json
from pathlib import Path
import subprocess

root = Path(__file__).resolve().parents[1]
tracked = set(subprocess.check_output(["git", "ls-files", "-z"], cwd=root).decode().rstrip("\0").split("\0"))
inventory = json.loads((root / "provenance/files.json").read_text())["files"]
errors = []
for path in sorted(tracked):
    entry = inventory.get(path)
    if not entry or not entry.get("ORIGIN") or entry.get("bucket") not in {"A", "B", "C"}:
        errors.append(f"{path}: missing ORIGIN/classification inventory entry")
for module in json.loads((root / "provenance/modules.json").read_text())["modules"]:
    path = root / module["path"]
    if hashlib.sha256(path.read_bytes()).hexdigest() != module["sha256"]:
        errors.append(f"{module['path']}: observed checksum changed; update provenance after review")
source = (root / "THIRD_PARTY_NOTICES.md").read_bytes()
if source != (root / "app/src/main/res/raw/third_party_notices.txt").read_bytes():
    errors.append("Source and APK notice texts differ")
if errors:
    raise SystemExit("\n".join(errors))
print(f"Origin coverage: {len(tracked)} paths. Observed module hashes and notice copy match.")
print("This checks record coverage and integrity, not license clearance or source-to-binary provenance.")
