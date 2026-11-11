[← Guide home](https://github.com/Mikaaah/Flow-Control/wiki) · [Project](https://github.com/Mikaaah/Flow-Control) · [Get help](https://github.com/Mikaaah/Flow-Control/issues)

# Configuration

Server configuration is at `<world>/serverconfig/streamcontrol-server.toml`.
For modpacks, place desired defaults in `defaultconfigs/streamcontrol-server.toml` before creating a world.

| Option | Default | Meaning |
| --- | --- | --- |
| `enabled` | `true` | Master override switch. Saved wand edits remain available. |
| `wandEnabled` | `true` | Enable the Calibrator; the legacy configuration key is retained. |
| `wandRequiresOperator` | `false` | Restrict Calibrator operations to operators. |
| `showWandFeedbackParticles` | `true` | Show Calibrator range outlines. |
| `defaultRadius` | `4` | Initial controller radius. |
| `maxRadius` | `16` | Maximum radius, up to 32 blocks. |
| `verticalReach` | `4` | Vertical distance above and below the center, up to 16. |
| `defaultStrength` | `1.0` | Default directed current strength. |
| `maxStrength` | `2.0` | Maximum selectable strength. |
| `maxRegionsPerDimension` | `2048` | Bounds saved regions and synchronization work. |
| `maxWandRegionsPerPlayer` | `256` | Per-player legacy edit limit. |

| `maxControllerVerticalReach` | `16` | Controller height limit above and below the centre. |
| `maxControllerLinkDistance` | `64` | Maximum same-dimension link distance. |
| `maxSensorRadius` | `16` | Sensor horizontal radius limit. |
| `maxSensorVerticalReach` | `16` | Sensor vertical reach limit. |
| `sensorSampleIntervalTicks` | `20` | Sensor update interval. |
| `maxMooringRadius` | `8` | Mooring horizontal radius limit. |
| `maxMooringVerticalReach` | `8` | Mooring vertical reach limit. |
| `kineticRpmForFullStrength` | `128.0` | RPM producing the configured profile strength. |
| `kineticStressImpact` | `4.0` | Optional Create stress impact per RPM. |

`/streamcontrol status` reports the number of regions and the override at your position.
Operators can run `/streamcontrol clear` to remove all wand edits in their current dimension. Blocks are preserved.
Standard build permissions are checked before wand edits; protection-mod behavior should be verified with the specific claim mod.

[← Wiki home](https://github.com/Mikaaah/Flow-Control/wiki/Home)
