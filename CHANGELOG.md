# Changelog

## 0.1.4 — Controller GUI redesign

- Replace the mixed dark panel and vanilla controls with one warm parchment, walnut and brass style inspired by the Unidentified Loot counters.
- Custom matching buttons, sliders, compass selectors and connected-water switch, including hover and keyboard focus feedback.
- Add a schematic water-area preview, clear radius/diameter/height values, and a dedicated stillwater summary.
- Keep redstone status, apply/cancel behavior, connected-water settings and server validation intact.
- No changes to water physics or particle suppression.



## 0.1.3-beta.3 — fabric 26.1.2

- Port the controller settings GUI with radius, flow speed, facing, and connected-water-only controls.
- Keep stillwater flow angles neutral and suppress Streams surface bubbles/debris in still regions.
- Refresh old and new water-render areas when controller settings change or controls are removed.
- Persist connected-water masks and validate edits on the server.
- Pin Streams Reflowing 2.15.2 and add build/dedicated-server regression CI.
- Client GUI and visual behavior still require in-game playtesting.


## 0.1.2 — Fabric 26.1.2

- Port Flow Control to Minecraft 26.1.2 and its native Fabric APIs.
- Retain the Flow Wand, Current Generator, Stillwater Anchor, creative tab and survival recipes.
- Preserve configurable regions, permissions, persistence, synchronization, redstone control and deterministic overlaps.
- Update client rendering integration for this Minecraft version.

Use the matching Streams Reflowing dependency listed in README.md. Install on both client and server.
