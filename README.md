# Streams Flow Control — NeoForge 26.3

Flow Wand, Current Generator and Stillwater Anchor for Streams Reflowing.

Version **0.1.5** — [Download JAR](downloads/flow-control-0.1.5-neoforge-26.3.jar).

Stillwater areas suppress newly emitted Streams bubbles and floating debris. Water textures refresh when controller settings change, redstone disables a controller, or a controller is removed. The NeoForge 1.21.1 GUI was visually checked in the Minecraft client; rendering on this port should still be confirmed in-game.

## Requirements

- Minecraft 26.3, NeoForge 26.3.0.95 or newer in the 26.3 series, Java 25.
- Streams Reflowing 2.15.2. Install both mods on client and server.

## Controller GUI

One consistent parchment, dark brown and brass style, with custom sliders, buttons, compass selectors, a connected-water switch and a schematic water-area preview.

## Eight-way flow

Current Generators support N, NE, E, SE, S, SW, W and NW. Diagonal flow keeps the selected speed. The controller top arrow follows the chosen heading; existing cardinal settings are preserved.

## Controls

- Use the wand on water to apply a circular flow region. Directed flow follows your horizontal view.
- Aim at the sky: use to cycle radius; sneak-use to cycle Direct Flow, Still Water and Restore Natural Flow.
- Right-click either controller to open its settings GUI: radius, flow speed, facing, and connected-water-only controls.
- Using a Flow Wand on a controller opens the same settings screen.
- Redstone power disables controllers. Breaking one removes its region.
- `/streamcontrol radius <blocks>` and `/streamcontrol strength <value>` configure the held wand.
- `/streamcontrol status` inspects your region; operator-only `/streamcontrol clear` removes wand regions in the current dimension.

## Configuration

`config/streamcontrol-synced.toml` controls radius, vertical reach, current strength, permissions and region limits. NeoForge synchronizes these settings to connecting clients.

Regions persist per dimension. Block controllers take priority over wand edits; within a category stillness wins, then the newest edit. Water outside the selected volume remains unchanged. Stillness stops water currents, not steering or collisions. No water or terrain is created.

[Full documentation](https://github.com/Mikaaah/Streams-Flow-Control/wiki) · [Report a bug](https://github.com/Mikaaah/Streams-Flow-Control/issues)
