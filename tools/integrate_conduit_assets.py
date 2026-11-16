"""Install the supplied split-part models without editing their authored geometry/UVs."""
from pathlib import Path
import json,shutil
source=Path(__file__).resolve().parents[2]/'model-input/Flow-Control-Conduit-Models'
root=Path(__file__).resolve().parents[1]
out=root/'src/main/resources/assets/streamcontrol'
shutil.copytree(source/'assets/streamcontrol',out,dirs_exist_ok=True)
# Local unmodified Create samples keep a static texture fallback valid without Create.
for p in (source/'reference_textures').glob('*.png'):
 if p.stem.startswith('create_') or p.stem in ('shaft','shaft_top'):
  shutil.copy2(p,out/'textures/block'/('fallback_'+p.name))
for p in (out/'models/block').rglob('*.json'):
 data=json.loads(p.read_text());textures=data.get('textures',{})
 for key,value in list(textures.items()):
  if value.startswith('create:'):
   match={'create:block/axis':'shaft','create:block/axis_top':'shaft_top','create:block/industrial_iron_block':'create_industrial_iron'}.get(value,'create_'+value.split('/')[-1])
   textures[key]='streamcontrol:block/fallback_'+match
 p.write_text(json.dumps(data,indent=2)+'\n')
manifest=json.loads((source/'asset_manifest.json').read_text())
for model in manifest['models']:
 ident=model['id'];variants={}
 for facing,yaw in [('north',0),('east',90),('south',180),('west',270)]:
  if ident in ('current_generator','mechanical_current_generator'):
   for diagonal in ('false','true'):variants[f'facing={facing},diagonal={diagonal}']={'model':model['body_model'],'y':yaw}
  else:variants[f'facing={facing}']={'model':model['body_model'],'y':yaw}
 (out/'blockstates'/f'{ident}.json').write_text(json.dumps({'variants':variants},indent=2)+'\n')
 for part in model['parts']:
  p=out/'models'/part['model'].split(':',1)[1];p=p.with_suffix('.json')
  data=json.loads(p.read_text());data['render_type']='minecraft:cutout'
  p.write_text(json.dumps(data,indent=2)+'\n')
  # Emission must be explicit: map every face material to its author-supplied mask.
  if part['bone'] in ('conduit_core','conduit_eye','dial_needle','status_indicator'):
   emission=json.loads(json.dumps(data))
   for key in list(emission['textures']):
    if key!='particle':emission['textures'][key]='streamcontrol:block/'+key+'_e'
   for element in emission.get('elements',[]):element['shade']=False
   p.with_name(p.stem+'_emissive.json').write_text(json.dumps(emission,indent=2)+'\n')
# Item views use the complete static assembly. Worlds use only fixed bodies + BER partials.
for ident in [m['id'] for m in manifest['models']]:
 (out/'models/item'/f'{ident}.json').write_text(json.dumps({'parent':'streamcontrol:block/'+ident},indent=2)+'\n')
(root/'docs/conduit-models').mkdir(parents=True,exist_ok=True)
shutil.copy2(source/'asset_manifest.json',root/'docs/conduit-models/asset_manifest.json')
shutil.copytree(source/'blockbench',root/'docs/conduit-models/blockbench',dirs_exist_ok=True)
print('Installed five supplied models, disjoint partials and explicit emissive masks')

atlas=root/'src/main/resources/assets/minecraft/atlases/blocks.json'
atlas.parent.mkdir(parents=True,exist_ok=True)
atlas.write_text(json.dumps({'sources':[{'type':'single','resource':'minecraft:entity/conduit/'+n,'sprite':'minecraft:entity/conduit/'+n} for n in ['base','cage','open_eye']]},indent=2)+'\n')
lang=out/'lang/en_us.json'
n=json.loads(lang.read_text());n['block.streamcontrol.mechanical_current_generator']='Hydro-Current Thruster';lang.write_text(json.dumps(n,indent=2,ensure_ascii=False)+'\n')

for id in ['current_generator','mechanical_current_generator']:
 (out/'models/block'/f'{id}_diagonal.json').write_text(json.dumps({'parent':'streamcontrol:block/'+id},indent=2)+'\n')
