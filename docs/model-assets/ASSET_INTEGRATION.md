# Flow Control model integration

Minecraft Java 1.21.1, NeoForge, Mojang mappings; optional Create 6/Flywheel 1.0+. This package supplies art for the existing mod. It adds no gameplay classes, network messages, recipes, or save formats.

## Current models

Five models contain **124 cuboids**. The main generator and anchor fit full 16×16×16 volumes. The sensor is 16×14×16. The mooring timber and cap form a narrow 6×18×6 main body, with a small collar, band, and tie ring projecting beyond that core.

| Asset ID | Cuboids | Complete authored bounds, pixels |
| --- | --- | --- |
| `current_generator` | 26 | `[0, 0, 0]` to `[16, 16, 16]` |
| `flow_sensor` | 21 | `[0, 0, 0]` to `[16, 14, 16]` |
| `mechanical_current_generator` | 35 | `[0, 0, 0]` to `[16, 16, 16]` |
| `mooring_post` | 14 | `[4, 0, 3]` to `[12, 18, 12]` |
| `stillwater_anchor` | 28 | `[0, 0, 0]` to `[16, 16, 16]` |

One model unit is 1/16 of a block. Reference fronts face north, along negative Z; Y points upward. The Hydro-Current Thruster retains the existing asset identifier `mechanical_current_generator`.

## Files and rendering

- `blockbench/<id>.bbmodel`: five editable Generic Model projects with embedded texture previews and applicable movement clips.
- `assets/streamcontrol/models/block/<id>.json`: complete static assemblies.
- `assets/streamcontrol/models/item/<id>.json`: matching item parents.
- `assets/streamcontrol/models/block/parts/<id>/body.json`: fixed body geometry.
- Other JSONs in each `parts` directory: separate moving assemblies, glass, or attachment/indicator parts.
- `asset_manifest.json`: exact resources, groups, pivots, and parent relationships.

Render the complete assembly or its body plus partials. The latter form a disjoint partition: do not render both sets together. A child assembly inherits its parent transform once. Glass is separated so the opaque body can keep its own render material.

Java block JSON does not execute Blockbench animations or preserve animated bones. The supplied JSON is the runtime geometry reference. Generic `.bbmodel` clips are editor previews; the existing block entity renderer or Create visual must connect them to live state. Java Block/Item authoring copies are omitted because mixed native 16×16/32×16 texture sizes require care during editor re-export.

## Individual blocks

**Current Generator.** A full 16×16×16 frame has a cut-copper bottom plate at Y=0–3 and top rim at Y=13–16. Four dark-prismarine corners join the plates. Clear side/rear panels enclose the Y=3–13 chamber; the top includes an inspection window. The copper grille, impeller, and native Conduit core are visible through the case. The six-unit Conduit core is seated on its mount. The impeller rotates about local Z around `[8, 8, 4]`.

**Stillwater Anchor.** A dark-prismarine base fills Y=0–3; an oxidized-copper upper cap fills Y=13–16. Four prismarine-brick corner pillars connect them across the full ten-unit gap. Copper braces and darkened iron rails are attached to that frame. The active Conduit cage is one native alpha-cutout cube, eight units square, centered at `[8, 8, 8]`, with a separate native eye detail. The frame and caps stay fixed.

**Flow Sensor.** Its body spans 16×10×16 units; a low vane reaches total height 14. The front 8×6 dial is recessed behind a single clear glass pane, using the dark native redstone-lamp texture and a redstone needle. The vane uses copper, weathered copper, and small hardware. It has no Conduit ornament. Its small brass pivot uses an unchanged local copy of Create's brass texture, so the base block does not require Create for that texture. Vane yaw pivots at `[8, 12, 8]`; needle rotation pivots at `[8, 5, 1.25]`.

**Hydro-Current Thruster.** The exterior is a full copper-casing housing with a recessed industrial-iron tunnel. Andesite is restricted to small corner/bearing fittings; brass is confined to tiny rivets and a hub detail. Four pitched iron blades rotate around `[8, 9, 3]` on local Z. The underside input is a proper four-unit-square Create shaft rotating around Y. The vertical spindle and horizontal axle join inside the housing. The block contains no Conduit parts. Forward and reverse preview clips are separate alternatives.

**Mooring Post.** The stripped-spruce timber spans `[5, 0, 5]` to `[11, 16, 11]`; the small copper cap occupies Y=16–18. Total height is 18 units. A small dark-prismarine collar, one thin darkened iron band, and a tie ring provide the fittings. The cap's small warm lit-redstone-lamp indicator is recessed. There is no gold/brass bulk or Conduit ornament. The post, cap, band, ring, and indicator stay fixed; a real boat rope remains procedural renderer geometry.

Above-block art does not automatically change selection, collision, waterlogging, or renderer culling bounds. Those remain properties of the existing mod.

| Asset ID | Separate part | Pivot, pixels | Parent group |
| --- | --- | --- | --- |
| `current_generator` | `impeller` | `[8, 8, 4]` | `none` |
| `current_generator` | `conduit_core` | `[8, 8, 10]` | `none` |
| `current_generator` | `glass_windows` | `[8, 0, 8]` | `housing` |
| `flow_sensor` | `dial_needle` | `[8, 5, 1.25]` | `body` |
| `flow_sensor` | `current_vane` | `[8, 12, 8]` | `none` |
| `flow_sensor` | `dial_glass` | `[8, 0, 8]` | `body` |
| `mechanical_current_generator` | `shaft` | `[8, 2, 8]` | `gearbox` |
| `mechanical_current_generator` | `propeller` | `[8, 9, 3]` | `root` |
| `mooring_post` | `rope_anchor` | `[8, 9, 3.5]` | `body` |
| `mooring_post` | `status_indicator` | `[8, 17, 6]` | `body` |
| `stillwater_anchor` | `conduit_outer_cage` | `[8, 8, 8]` | `none` |
| `stillwater_anchor` | `conduit_eye` | `[8, 8, 8]` | `none` |

Divide pivots by 16 for renderers using block units. Shaft Y rotation and horizontal propeller Z rotation remain distinct even when both use the same kinetic RPM.

## Materials and source pixels

The catalog contains **27 materials**: 25 direct native Minecraft/Create resource references, one unchanged Create-brass copy under `streamcontrol` for the base sensor, and one explicitly darkened native-iron derivative. The native brass copy preserves the original pixels. `rusted_iron` preserves the iron texture structure while multiplying RGB by `.29/.265/.24`; it is the only recolored base surface.

| Alias | Runtime resource | UV image/frame size |
| --- | --- | --- |
| `brass_pivot` | `streamcontrol:block/brass_pivot` | 16×16 |
| `conduit_base` | `minecraft:entity/conduit/base` | 32×16 |
| `conduit_eye` | `minecraft:entity/conduit/open_eye` | 16×16 |
| `conduit_shell` | `minecraft:entity/conduit/cage` | 32×16 |
| `copper` | `minecraft:block/copper_block` | 16×16 |
| `create_andesite_casing` | `create:block/andesite_casing` | 16×16 |
| `create_brass_block` | `create:block/brass_block` | 16×16 |
| `create_brass_casing` | `create:block/brass_casing` | 16×16 |
| `create_copper_casing` | `create:block/copper_casing` | 16×16 |
| `create_fan_blades` | `create:block/fan_blades` | 16×16 |
| `create_industrial_iron` | `create:block/industrial_iron_block` | 16×16 |
| `cut_copper` | `minecraft:block/cut_copper` | 16×16 |
| `dark_prismarine` | `minecraft:block/dark_prismarine` | 16×16 |
| `dial_face` | `minecraft:block/redstone_lamp` | 16×16 |
| `glass` | `minecraft:block/glass` | 16×16 |
| `iron` | `minecraft:block/iron_block` | 16×16 |
| `needle_red` | `minecraft:block/redstone_block` | 16×16 |
| `oxidized_copper` | `minecraft:block/oxidized_copper` | 16×16 |
| `prismarine_bricks` | `minecraft:block/prismarine_bricks` | 16×16 |
| `redstone` | `minecraft:block/redstone_block` | 16×16 |
| `redstone_lamp_lit` | `minecraft:block/redstone_lamp_on` | 16×16 |
| `rusted_iron` | `streamcontrol:block/rusted_iron` | 16×16 |
| `shaft` | `create:block/axis` | 16×16 |
| `shaft_top` | `create:block/axis_top` | 16×16 |
| `spruce` | `minecraft:block/stripped_spruce_log` | 16×16 |
| `spruce_end` | `minecraft:block/stripped_spruce_log_top` | 16×16 |
| `weathered_copper` | `minecraft:block/weathered_copper` | 16×16 |

There are 27 `_e.png` support masks: three contain the native Conduit eye, bright redstone needle, and warm lit-redstone-lamp indicator; the remaining 24 are transparent. A suffix does not activate emission automatically: the existing renderer must apply a full-bright pass/material, and block-light emission is a separate state property.

## UV and transparency contract

Ordinary faces use one native texture pixel per model unit with deterministic Minecraft planar projection. Adjacent casing pieces share coherent UV coordinates. Explicit native crops place the proper shaft ends and casing/fan details; source textures are not randomly remapped between cuboids.

The eye is a deliberate semantic detail: the complete native upper-left 8×8 glyph is mapped onto its four-unit-square front detail at two pixels per model unit. This preserves the full glyph's square proportions rather than cropping away its pupil. Other eye-detail faces are omitted; it is not a box wrapped in six duplicated eye images.

The six-unit Conduit base uses the original 32×16 `entity/conduit/base` cube layout. The eight-unit active cage uses the native 32×16 `entity/conduit/cage` layout and its alpha-cutout openings. Do not shrink either full atlas into a square tile. Java UV values normalize source pixels into 0–16; therefore U values from a 32-pixel-wide atlas are halved.

Element rotations remain legal for Java 1.21.1: one axis per cube, using ±22.5° or ±45°. Continuous spin belongs to the assembly renderer. Horizontal shaft-side UVs rotate 90° where needed to keep the native shaft stripe aligned with its long axis.

Native clear glass has binary alpha and uses a cutout pass. The sensor uses one clear front pane, separated into its own cutout partial; its body remains opaque. The needle sits physically behind the pane and uses a native bright-red crop plus a controlled full-bright support mask. The final proof shows the red needle clearly against the dark dial. The raised one-pixel anchor rails leave the eye’s center open.

## Verification and limits

`docs/asset_validation.json` records offline structure, source-pixel, material, dimensional, and principal motion-clearance checks, plus the actual proof images reviewed. A file-format check is not an in-game compatibility test. Static preview clips do not imply runtime animation is already wired.

After integration, inspect all horizontal facings, glass depth and needle visibility, the bottom kinetic shaft, forward/reverse rotation, the anchor's attached supports, and the post's protruding ring. The exported models and resource identifiers are ready for the existing art/rendering pipeline; gameplay behavior remains in the existing mod.

## Sources

- [Blockbench format feature matrix](https://blockbench.net/wiki/blockbench/formats/): model/bone/animation limits and the pre-1.21.6 rotation restriction.
- Minecraft Java 1.21.1 resource archive and Mojang-mapped `BlockElement`, `ModelPart`, and `ConduitRenderer`: native PNGs, animation frame sizes, planar UVs, and Conduit layouts inspected locally.
- Create 6.0.10-280 client resources: native casing, brass, fan-blade, industrial-iron, and shaft textures inspected locally.
