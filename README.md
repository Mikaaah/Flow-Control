# Streams Flow Control — NeoForge 26.3

Flow Wand, Current Generator and Stillwater Anchor for Streams Reflowing.

## Requirements

- Minecraft 26.3, NeoForge 26.3.0.95 or newer in the 26.3 series, Java 25.
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

`config/streamcontrol-synced.toml` controls radius, vertical reach, current strength, permissions and region limits. NeoForge synchronizes these settings to connecting clients.

Regions persist per dimension. Block controllers take priority over wand edits; within a category stillness wins, then the newest edit. Water outside the selected volume remains unchanged. Stillness stops water currents, not steering or collisions. No water or terrain is created.

[Full documentation](https://github.com/Mikaaah/Streams-Flow-Control/wiki) · [Report a bug](https://github.com/Mikaaah/Streams-Flow-Control/issues)
