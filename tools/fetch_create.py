import pathlib,urllib.request,hashlib,zipfile
root=pathlib.Path(__file__).resolve().parents[1]/'libs';root.mkdir(exist_ok=True)
jar=root/'create-fabric-6.0.8.1+build.1744-mc1.20.1.jar'
if not jar.exists():jar.write_bytes(urllib.request.urlopen('https://cdn.modrinth.com/data/Xbc0uyRg/versions/HAqwA6X1/create-fabric-6.0.8.1%2Bbuild.1744-mc1.20.1.jar',timeout=90).read())
assert hashlib.sha1(jar.read_bytes()).hexdigest()=='f750d019bc1230c8c7a8a4ccf549d1db192d7a69','Unexpected Create compile fixture'
output=root/'create-compile';output.mkdir(exist_ok=True)
for p in output.glob('*.jar'):p.unlink()
with zipfile.ZipFile(jar) as z:
 for n in z.namelist():
  if n.endswith('.jar') and (n.startswith('META-INF/jarjar/') or n.startswith('META-INF/jars/')):(output/pathlib.Path(n).name).write_bytes(z.read(n))
print('Verified optional Create compile fixture')
