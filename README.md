![Streams Flow Control](docs/assets/banner.png)

# Streams Flow Control — Fabric 1.20.1

Flow Wand, Current Generator and Stillwater Anchor for Streams Reflowing.

Version **0.1.3-beta.3** — [Download JAR](downloads/streams-flow-control-fabric-1.20.1-0.1.3-beta.3.jar).

Stillwater areas suppress newly emitted Streams bubbles and floating debris. Water textures refresh when controller settings change, redstone disables a controller, or a controller is removed. Client visuals still require in-game playtesting.

## Requirements

- Minecraft 1.20.1, Fabric Loader 0.18.4 or newer, Java 17.
- Streams Reflowing 2.15.2. Install both mods on client and server.

## Controls

- Use the wand on water to apply a circular flow region. Directed flow follows your horizontal view.
- Aim at the sky: use to cycle radius; sneak-use to cycle Direct Flow, Still Water and Restore Natural Flow.
- Right-click either controller to open its settings GUI: radius, flow speed, facing, and connected-water-only controls.
- Using a Flow Wand on a controller opens the same settings screen.
- Redstone power disables controllers. Breaking one removes its region.
- `/streamcontrol radius <blocks>` and `/streamcontrol strength <value>` configure the held wand.
- `/streamcontrol status` inspects your region; operator-only `/streamcontrol clear` removes wand regions in the current dimension.

## Configuration

`<world>/serverconfig/streamcontrol-server.json` controls radius, vertical reach, current strength, permissions and region limits. Supply modpack defaults through `defaultconfigs/streamcontrol-server.json`.

Regions persist per dimension. Block controllers take priority over wand edits; within a category stillness wins, then the newest edit. Water outside the selected volume remains unchanged. Stillness stops water currents, not steering or collisions. No water or terrain is created.

[Full documentation](https://github.com/Mikaaah/Streams-Flow-Control/wiki) · [Report a bug](https://github.com/Mikaaah/Streams-Flow-Control/issues)

Fabric API is required. Configuration changes take effect after restarting the server or reopening the world. Server settings determine the physical current and are synchronized where needed for client rendering.
