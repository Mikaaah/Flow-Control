![Flow Control](https://i.imgur.com/JGT2lvz.png)


## 1.0.1 — Streams Reflowing Compatibility

- Updated support to **Streams Reflowing 2.17.3** across all nine builds.
- Updated dependency requirements so the latest Streams release can load alongside Flow Control.

Update Flow Control and Streams Reflowing on both client and server. Create remains optional.

## 1.0.0 — Streams Reflowing Update

- Updated NeoForge 1.21.1 support to **Streams Reflowing 2.17.1**.
- Fixed compatibility with the renamed stream-water classes used by the new Streams release.
- Retained the Calibrator, controller profiles, flow sensors, mooring posts and optional Create integration.
- Existing controller settings and items carry over. Create water-wheel speeds remain unchanged.

## 0.2.2 — Verified Water Engineering and Version Ports

- Fixed controller and utility block interactions so Copy, Paste and Link reach the Calibrator instead of opening the normal configuration screen.
- Carried the redesigned, centered interfaces and water-engineering functions to the existing Minecraft/loader ports.
- Updated every port to the exact Streams Reflowing 2.16.2 dependency and checked its current-grid and surface-particle APIs against the matching JAR.
- Preserved old wand items, legacy edits and controller settings; clients and servers must use matching 0.2.2 builds.
- Added real client/server checks for GUI Apply and synchronization, all Calibrator modes, links and unlinking, profiles, redstone switching, vertical sensors and comparator output, mooring release/recapture and real-player boarding.
- Verified the optional mechanical controller through actual Create motor input, reversal and stopping. Create's normal water-wheel RPM behavior is unchanged.
- Adapted modern ports to their own persistence, comparator and rendering APIs, including world-bound item components and 26.3 geometry submission.

The complete feature changelog and exact supported dependency matrix are supplied with the verified release bundle.

## 0.2.1 — Barrel and Create Model Refinement

- Kept the original control lid on the Mechanical Current Generator and replaced its entire body with native Create Andesite Casing textures.
- Added a recessed bottom shaft socket and matched the inventory shaft to Create's actual shaft geometry and end texture.
- Rebuilt the Mooring Post as a compact barrel with straight staves, native barrel hoops, a wooden lid and subtly chamfered corners.
- Removed the old recolored machine textures from the Mooring Post.
- Preserved all water-control, sensor, mooring and Create behavior.


## 0.2.0 — Water Engineering

- Repurposed the Flow Wand as the Flow Calibrator: inspect currents, show horizontal/vertical reach, copy and paste settings, and link controllers.
- Preserved the existing tool registry ID, controller settings and legacy wand regions; added a cleanup mode for old edits.
- Added two saved controller profiles with directed, reversed, calm and natural-flow actions.
- Added analog redstone strength, powered reversal, profile switching and an always-active mode alongside the original power-to-pause behavior.
- Added configurable vertical reach to controller profiles.
- Added Flow Sensors with independent horizontal and vertical ranges, comparator output for strength/direction/boats, and empty-boat filtering.
- Added Mooring Posts that secure one empty boat and release it when boarded, powered, disabled or removed.
- Added optional Create support: Mechanical Current Generator, real bottom-shaft input, RPM-based strength, reversal and stress handling.
- Reused the existing block model family and pixel patterns; added steel-blue sensors, wooden mooring posts and an andesite base for the Create variant.
- Added matching Automation, sensor and mooring screens, live utility status, survival recipes and item descriptions.
- Updated networking for the new settings. Client and server must use matching 0.2.0 builds.
- Verified NeoForge 1.21.1 / Java 21 / Streams Reflowing 2.16.2, both with and without Create 6.0.10. Other ports are unchanged.


## 0.1.7

- Removed the header corner rivets.
- Rebuilt both controller layouts with centered charts, balanced spacing and contained compass buttons.
- Current Generator now has an eight-way navigation chart with a separate radius and strength panel.
- Stillwater Anchor now has a central anchor chart, symmetric range readouts and a full-width radius control.
- Updated NeoForge 1.21.1 to Streams Reflowing 2.16.2, including renamed flow-grid, baked-water and surface-particle hooks.
- Preserved all controller settings, connected-water filtering and redstone behavior.

## 0.1.6 — TNK GUI Rework

![Flow Control](https://i.imgur.com/JGT2lvz.png)

- Restyled Current Generator and Stillwater Anchor using the latest TNK Loot / Progression menu design.
- Slate-blue frames, white outer outlines, inset copper panels and a raised title plate.
- Matching textured buttons, compass selections and slider controls, with visible keyboard focus.
- Embedded the supplied mixed-case menu font for titles; retained Minecraft text for settings and values.
- Preserved eight compass directions, normalized diagonal current strength, connected-water mode, redstone behavior and Apply/Cancel.
- Existing saves, config limits and network packet formats remain compatible with 0.1.5.

# Changelog

## 0.1.5 — Eight-way current compass

- Add N, NE, E, SE, S, SW, W and NW selections in a clickable compass.
- Normalize diagonal currents to preserve the selected speed.
- Save compass directions in block states; preserve old cardinal directions and handle rotations/mirrors.
- Update server validation and networking for all eight headings.


## 0.1.4 — Controller GUI redesign

- Replace the mixed dark panel and vanilla controls with one warm parchment, walnut and brass style inspired by the Unidentified Loot counters.
- Custom matching buttons, sliders, compass selectors and connected-water switch, including hover and keyboard focus feedback.
- Add a schematic water-area preview, clear radius/diameter/height values, and a dedicated stillwater summary.
- Keep redstone status, apply/cancel behavior, connected-water settings and server validation intact.
- No changes to water physics or particle suppression.


## 0.1.3-beta.3 — all supported ports

- Port the new controller GUI, connected-water settings, stillwater particle suppression and render-refresh fixes to all seven additional version branches.
- Compile and pass the dedicated-server regression suite on every port; commit matching JARs, checksums and test evidence.
- All seven ports now use Streams Reflowing 2.15.2. Client visuals still require in-game playtesting.


## 0.1.3-beta.2 — NeoForge 1.21.1 (test build)

- Keep the controller's panel and labels crisp by replacing the deferred vanilla background blur with a translucent dim overlay.

- Prevent Streams Reflowing from emitting new surface bubbles, leaves, sticks and adjacent-current spray from water cells controlled by Still Water or a Stillwater Anchor.
- Add a shared controller settings GUI: clearly labeled radius and diameter, vertical reach, Current Generator speed and facing, and an optional **Connected water only** toggle.
- Connected-only controller regions now use bounded water connectivity, not a blanket circular override. The masks persist and sync to clients so physics, textures and particles agree; water changes are refreshed periodically.
- Keep existing worlds compatible: connected-only is off by default and old saved controller settings still load.

- Apply final stillwater/current overrides at `FluidState.getFlow`, including client render lookups, so a later fluid implementation cannot reintroduce directional movement in the rendered flow vector.
- Use a direction-neutral angle for stillwater in both Streams' client grid and vessel grid, rather than preserving the Flow Wand or Stillwater Anchor facing while speed is zero.
- Allow client flow-grid lookups on chunk rendering threads without a thread-group logical-side gate.
- Add regression assertions for stillwater and directed current angle behavior.
- **Testing:** server-side integration check and in-game client visual verification are required before publishing a JAR.

## 0.1.1-beta

- Replace the Flow Wand, generator top, anchor top, shared sides and shared bottoms with the supplied textures, unchanged.
- Align the generator model with the supplied arrow orientation.


## 0.1.0-beta

- Flow Wand with directed, still and restore modes; configurable circular radius and current strength.
- Waterloggable Current Generator with directional top arrow and rotation controls.
- Waterloggable Stillwater Anchor for docks and other calm-water areas.
- Redstone disables controllers; breaking one immediately removes its override.
- Dedicated creative tab, original pixel textures and survival recipes.
- Per-dimension persistence, chunk-indexed physics queries and server-to-client synchronization.
- Override ordinary water, baked Streams fluids and the extra Streams vessel drift path.
- Height-bounded regions, deterministic overlap priority, owner checks and configurable edit limits.
- Opt-in dedicated-server integration checks.

Targets Minecraft 1.21.1, NeoForge 21.1.219 and Streams Reflowing 2.15.2.
