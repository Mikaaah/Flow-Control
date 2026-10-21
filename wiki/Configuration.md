# Configuration

Server configuration is at `<world>/serverconfig/streamcontrol-server.json`.
For modpacks, place desired defaults in `defaultconfigs/streamcontrol-server.json` before creating a world.

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

[← Wiki home](https://github.com/Mikaaah/Streams-Flow-Control/wiki/Home)

Fabric configuration is JSON and is loaded when the server starts.
