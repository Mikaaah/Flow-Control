![Streams Flow Control](docs/assets/banner.png)

# Streams Flow Control — Fabric 1.20.1

Flow Wand, Current Generator and Stillwater Anchor for Streams Reflowing.

## Requirements

- Minecraft 1.20.1, Fabric Loader 0.18.4 or newer, Java 17.
- Streams Reflowing 2.15.2. Install both mods on client and server.

## Controls

- Use the wand on water to apply a circular flow region. Directed flow follows your horizontal view.
- Aim at the sky: use to cycle radius; sneak-use to cycle Direct Flow, Still Water and Restore Natural Flow.
- Empty-hand use rotates a Current Generator; sneak-use increases a controller's radius.
- Use a wand on a controller to copy radius, strength and facing.
- Redstone power disables controllers. Breaking one removes its region.
- `/streamcontrol radius <blocks>` and `/streamcontrol strength <value>` configure the held wand.
- `/streamcontrol status` inspects your region; operator-only `/streamcontrol clear` removes wand regions in the current dimension.

## Configuration

`<world>/serverconfig/streamcontrol-server.json` controls radius, vertical reach, current strength, permissions and region limits. Supply modpack defaults through `defaultconfigs/streamcontrol-server.json`.

Regions persist per dimension. Block controllers take priority over wand edits; within a category stillness wins, then the newest edit. Water outside the selected volume remains unchanged. Stillness stops water currents, not steering or collisions. No water or terrain is created.

[Full documentation](https://github.com/Mikaaah/Streams-Flow-Control/wiki) · [Report a bug](https://github.com/Mikaaah/Streams-Flow-Control/issues)

Fabric API is required. Configuration changes take effect after restarting the server or reopening the world. Server settings determine the physical current and are synchronized where needed for client rendering.
