import pathlib,urllib.request,hashlib,zipfile
root=pathlib.Path(__file__).resolve().parents[1]/'libs';root.mkdir(exist_ok=True)
jar=root/'create-1.20.1-6.0.8.jar'
if not jar.exists():jar.write_bytes(urllib.request.urlopen('https://cdn.modrinth.com/data/LNytGWDc/versions/8amzvn9x/create-1.20.1-6.0.8.jar',timeout=90).read())
assert hashlib.sha1(jar.read_bytes()).hexdigest()=='b13d912b9247a38d66d11598c121442585a1c1e9','Unexpected Create compile fixture'
output=root/'create-compile';output.mkdir(exist_ok=True)
for p in output.glob('*.jar'):p.unlink()
with zipfile.ZipFile(jar) as z:
 for n in z.namelist():
  if n.endswith('.jar') and (n.startswith('META-INF/jarjar/') or n.startswith('META-INF/jars/')):(output/pathlib.Path(n).name).write_bytes(z.read(n))
print('Verified optional Create compile fixture')
