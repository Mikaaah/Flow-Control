"""Fetch the exact compile/test fixture; never bundle Create in the addon."""
import hashlib
import pathlib
import urllib.request
import zipfile

root = pathlib.Path(__file__).resolve().parents[1] / 'libs'
root.mkdir(exist_ok=True)
jar = root / 'create-1.21.1-6.0.10.jar'
url = 'https://cdn.modrinth.com/data/LNytGWDc/versions/UjX6dr61/create-1.21.1-6.0.10.jar'
if not jar.exists():
    with urllib.request.urlopen(url, timeout=90) as response:
        jar.write_bytes(response.read())
assert hashlib.sha1(jar.read_bytes()).hexdigest() == '0e97e49837bed766e6f28a4c95b04885d6acc353', 'Unexpected Create fixture'
compile_dir = root / 'create-compile'
compile_dir.mkdir(exist_ok=True)
for stale in compile_dir.glob('*.jar'):
    stale.unlink()
with zipfile.ZipFile(jar) as archive:
    for entry in archive.namelist():
        if entry.startswith('META-INF/jarjar/') and entry.endswith('.jar'):
            (compile_dir / pathlib.PurePosixPath(entry).name).write_bytes(archive.read(entry))
print('Verified Create 6.0.10 and extracted its compile-only dependencies.')
