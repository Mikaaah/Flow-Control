<p align="center"><img src="docs/assets/banner.png" alt="Flow Control" width="100%"></p>

<h1 align="center">Flow Control</h1>

<p align="center"><a href="https://www.curseforge.com/minecraft/mc-mods/flow-control-streams-reflowing-addon">CurseForge</a> · <a href="https://github.com/Mikaaah/Flow-Control/wiki">Wiki</a> · <a href="https://github.com/Mikaaah/Flow-Control/issues">Report an issue</a> · <a href="https://github.com/Mikaaah/Flow-Control/blob/main/CHANGELOG.md">Changelog</a></p>

<p align="center"><img alt="Minecraft 1.21.1" src="https://img.shields.io/badge/Minecraft-1.21.1-568736?style=flat-square"> <img alt="NeoForge" src="https://img.shields.io/badge/Loader-NeoForge-d77828?style=flat-square"></p>

Shape currents and create calm water around docks, canals and builds. An addon for **Streams Reflowing**, available for NeoForge, Forge and Fabric.

## Water Engineering — 0.2.0

Turn waterways into configurable transport and docking systems. The Flow Calibrator inspects currents, displays reach, copies settings and links controllers. Two saved profiles add analog redstone control, reversal and automatic switching. Flow Sensors detect current strength, direction or boats through configurable horizontal and vertical ranges. Mooring Posts secure empty boats and release them when boarded or powered.

With **Create** installed, the **Mechanical Current Generator** accepts a bottom shaft: RPM sets strength, rotation direction reverses flow, and stopped or overstressed drives release their override. The standard controllers, sensors, posts and Calibrator work without Create.

The existing controller model family is retained, with steel-blue sensor colors, wooden mooring posts and an andesite base for the mechanical variant. Existing Flow Wands become Calibrators; saved controller settings and old edits are preserved.

| Minecraft | Loader | Java | Streams Reflowing | Optional Create | Download |
| --- | --- | --- | --- | --- | --- |
| 1.21.1 | NeoForge 21.1.219+ within 21.1 | 21 | 2.16.2 | Tested with 6.0.10 | [0.2.0 JAR](https://github.com/Mikaaah/Flow-Control/raw/refs/heads/feature/water-engineering/downloads/flow-control-0.2.0-neoforge-1.21.1.jar) |

[Controls and behavior](docs/WATER-ENGINEERING.md) · [Feature branch](https://github.com/Mikaaah/Flow-Control/tree/feature/water-engineering) · [Build and regression checks](https://github.com/Mikaaah/Flow-Control/actions/runs/41593515484)

The 0.2.0 feature update currently targets NeoForge 1.21.1. Earlier downloads and other loader branches remain unchanged. Use matching client/server versions.

## Make water work for your build

| Tool or block | Purpose |
| --- | --- |
| Flow Calibrator | Inspect currents, preview reach, copy/paste settings and link controllers. |
| Current Generator | Maintain an eight-way current with configurable profiles and redstone behavior. |
| Stillwater Anchor | Calm water around docks and switch between calm and natural water. |
| Flow Sensor | Measure strength, compass direction or boats above, below and around the sensor. |
| Mooring Post | Secure one empty boat; release it when boarded or powered. |
| Mechanical Current Generator | Optional Create shaft-driven variant with RPM and stress support. |

Right-click blocks to configure them. Sneak-use the Calibrator in the air to switch modes. All tools and blocks have survival recipes; the mechanical recipe is available only with Create.

## Credits and license

By **Towns & Kingdoms**. [GPL-3.0-only](LICENSE). Requires Streams Reflowing; no dependency code or textures are redistributed.
