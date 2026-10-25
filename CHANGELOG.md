# Changelog

## 0.1.3-beta.3 — neoforge 1.21.11

- Port the controller settings GUI with radius, flow speed, facing, and connected-water-only controls.
- Keep stillwater flow angles neutral and suppress Streams surface bubbles/debris in still regions.
- Refresh old and new water-render areas when controller settings change or controls are removed.
- Persist connected-water masks and validate edits on the server.
- Pin Streams Reflowing 2.15.2 and add build/dedicated-server regression CI.
- Client GUI and visual behavior still require in-game playtesting.


## 0.1.2 — NeoForge 1.21.11

- Port Flow Control to Minecraft 1.21.11 and its native NeoForge APIs.
- Retain the Flow Wand, Current Generator, Stillwater Anchor, creative tab and survival recipes.
- Preserve configurable regions, permissions, persistence, synchronization, redstone control and deterministic overlaps.
- Update client rendering integration for this Minecraft version.

Use the matching Streams Reflowing dependency listed in README.md. Install on both client and server.
