import pathlib,urllib.request,hashlib,zipfile
root=pathlib.Path(__file__).resolve().parents[1]/'libs';root.mkdir(exist_ok=True)
jar=root/'create-fabric-6.0.8.1+build.1744-mc1.20.1.jar'
if not jar.exists():jar.write_bytes(urllib.request.urlopen('https://cdn.modrinth.com/data/Xbc0uyRg/versions/HAqwA6X1/create-fabric-6.0.8.1%2Bbuild.1744-mc1.20.1.jar',timeout=90).read())
assert hashlib.sha1(jar.read_bytes()).hexdigest()=='f750d019bc1230c8c7a8a4ccf549d1db192d7a69','Unexpected Create compile fixture'
output=root/'create-compile';output.mkdir(exist_ok=True)
for p in output.glob('*.jar'):p.unlink()
import io,json
queue=[jar.read_bytes()];seen_bytes=set();seen_mods=set()
while queue:
 data=queue.pop(0)
 with zipfile.ZipFile(io.BytesIO(data)) as z:
  for n in z.namelist():
   if not n.endswith('.jar') or not (n.startswith('META-INF/jarjar/') or n.startswith('META-INF/jars/')):continue
   child=z.read(n);digest=hashlib.sha1(child).hexdigest()
   if digest in seen_bytes:continue
   seen_bytes.add(digest)
   with zipfile.ZipFile(io.BytesIO(child)) as nested:
    if 'fabric.mod.json' in nested.namelist():
     mod=json.loads(nested.read('fabric.mod.json'))['id']
     if mod in seen_mods:continue
     seen_mods.add(mod)
   (output/pathlib.Path(n).name).write_bytes(child);queue.append(child)
print('Verified optional Create fixture and all nested runtime libraries')
