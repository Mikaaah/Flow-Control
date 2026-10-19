# Streams Flow Control

Control water around docks, canals and builds without replacing river terrain.
A NeoForge 1.21.1 addon for **Streams Reflowing 2.15.2**. Install on both client and server.
[CurseForge](https://www.curseforge.com/minecraft/mc-mods/flow-control-streams-reflowing-addon) · [Download GitHub build 0.1.1-beta](downloads/streams-flow-control-0.1.1-beta-neoforge-1.21.1.jar?raw=true) · [Report a bug](https://github.com/Mikaaah/Streams-Flow-Control/issues/new?template=bug_report.yml)

UltraTerraForged is **not** required; the addon can also be used with UltraTerraForged Streams Currents.

## Items and controls

All three items are in the **Streams Flow Control** creative tab and have survival crafting recipes.

| Item | Action |
| --- | --- |
| **Flow Wand** | Use on water to apply the selected mode within a circular area. Directed water follows your horizontal view. |
| Wand modes | Aim at the sky and **sneak-use** to cycle **Direct flow → Still water → Restore natural flow**. |
| Wand radius | Aim at the sky and **use** to cycle the radius, or run `/streamcontrol radius <blocks>` while holding the wand. |
| Wand strength | `/streamcontrol strength <0.05–2>` while holding the wand. Server limits apply. |
| **Current Generator** | Place under or beside water. Its top arrow points in the current direction. Use with an empty hand to rotate clockwise. |
| **Stillwater Anchor** | Place under or beside your dock to cancel water currents in its area. |
| Controller radius | Sneak-use with an empty hand to increase the radius. Use a wand to copy its radius, strength and your facing to the controller. |
| Redstone | A powered controller is **disabled**. Remove power to enable it again. |
| Remove block control | Break the block; its region is removed immediately and underlying flow returns. |

A radius of four means a horizontal circular area four blocks from the selected water or controller.
The default vertical reach is four blocks above and below it. Only water in that volume is affected.
Flow strength changes current velocity; it does not pump water into empty blocks or move the water surface.

For a dock, place a Stillwater Anchor in the riverbed within four blocks of the moored boats. Adjust its radius if needed.
Stillness stops water-induced drift. It does not lock a boat against steering, collisions, wind from other mods or existing momentum.

## Overlapping areas

Block controllers have priority over wand edits. Within the same category, stillness wins over directed flow;
otherwise the newest edit wins. Removing an override reveals any lower-priority area before natural flow.
The wand's Restore mode removes the selected **wand** area; it cannot remove a block controller.
Wand edits belong to their creator. Other players cannot replace or remove them without operator permission.

Regions are saved separately in each dimension and synchronized to joining players and players changing dimension.
They affect both ordinary water and Streams' baked stream blocks, including the extra Streams boat drift path.
No terrain is rewritten and no chunks are forced to load. Controller regions remain active while their block's chunk is unloaded.
Areas are geometric volumes: separate pools within the same volume are affected too.

## Configuration

Server configuration is at `<world>/serverconfig/streamcontrol-server.toml`.
For modpacks, place desired defaults in `defaultconfigs/streamcontrol-server.toml` before creating a world.

| Option | Default | Meaning |
| --- | --- | --- |
| `enabled` | `true` | Master override switch. Saved wand edits remain available. |
| `wandEnabled` | `true` | Allow wand edits and wand controller setup. |
| `wandRequiresOperator` | `false` | Restrict wand editing to operators. |
| `showWandFeedbackParticles` | `true` | Show a brief ring when applying an edit. |
| `defaultRadius` | `4` | Initial controller and wand radius. |
| `maxRadius` | `16` | Maximum radius, up to 32 blocks. |
| `verticalReach` | `4` | Vertical distance above and below the center, up to 16. |
| `defaultStrength` | `1.0` | Default directed current strength. |
| `maxStrength` | `2.0` | Maximum selectable strength. |
| `maxRegionsPerDimension` | `2048` | Bounds saved regions and synchronization work. |
| `maxWandRegionsPerPlayer` | `256` | Per-player wand edit limit. |

`/streamcontrol status` reports the number of regions and the override at your position.
Operators can run `/streamcontrol clear` to remove all wand edits in their current dimension. Blocks are preserved.
Standard build permissions are checked before wand edits; protection-mod behavior should be verified with the specific claim mod.

## Build and validation

Requires Java 21. Put `StreamsReflowing-1.21.1-neoforge-2.15.2.jar` in `libs/`, then run `./gradlew build`.
Dependency jars are not redistributed. Output is in `build/libs/`.

The opt-in dedicated-server integration harness checks actual ordinary/baked water flow, boat current APIs,
radius and height boundaries, overlapping regions, save/load and network codec round trips, dimension isolation,
redstone disable/re-enable and block-removal cleanup. It runs only with `-Dstreamcontrol.integrationTest=true`
and stops the test server afterward. Use a disposable development world.

Client visuals and full modpack playtesting remain necessary before calling this a stable release.

## License

GPL-3.0-only. This addon does not redistribute Streams Reflowing code or textures.
