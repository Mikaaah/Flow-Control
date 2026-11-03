# Flow Control 0.2.1 — Water Engineering

| Minecraft | Loader | Java | Required | Optional integration tested |
| --- | --- | --- | --- | --- |
| 1.21.1 | NeoForge 21.1.219+ within 21.1 | 21 | Streams Reflowing 2.16.2 | Create 6.0.10 |

The same Flow Control JAR works with and without Create. The Mechanical Current Generator and its crafting recipe are available only when Create is installed. Other loader/version branches have not received this feature update.

## Flow Calibrator

Existing Flow Wands become Calibrators automatically because their registry ID is retained. Calibrators inspect and configure water engineering; freehand current painting is left to Streams Reflowing's own tool. Existing wand-made regions remain in the world.

| Input or mode | Action |
| --- | --- |
| Hold the Calibrator and aim at water | Read direction, current strength and whether the source is natural, a controller or a legacy edit. |
| Aim at a controller, sensor or mooring post | Read its range; a cylinder outline shows horizontal and vertical reach. |
| Use while aiming at empty air | Toggle range outlines. |
| Sneak-use while aiming at empty air | Cycle Inspect, Copy, Paste, Link and Remove legacy edit. |
| Inspect: use a controller or utility block | Open its settings. |
| Copy, then Paste | Copy both controller profiles or compatible sensor/mooring settings. Paste does not copy links. |
| Link: use source, then target controller | Target follows the source's profiles and redstone mode. Each target reads its own local redstone signal and, if mechanical, its own drive. |
| Link: sneak-use a controller | Unlink it and clear the selected source. |
| Remove legacy edit: use edited water | Remove your old wand region. Operators can also remove other players' old edits. |

Links are restricted to the same dimension and default to a maximum of 64 blocks. Self-links and loops are rejected. A linked controller pauses if the source is unloaded or missing; linking never keeps chunks loaded. Unlinking restores its own saved profiles. The outline shows the reach cylinder; connected-water filtering can exclude water inside it.

## Controller profiles

The Settings tab edits radius, current strength, direction and connected-water filtering for the selected profile. In Automation, select Profile A or B, choose its action and vertical reach, and select the redstone mode. Changes are saved only with Apply changes.

| Redstone mode | Behavior |
| --- | --- |
| Pause when powered | Original behavior: power releases the controller's override. This is the migration default. |
| Always active | Ignore the redstone signal. |
| Analog strength 0–15 | Scale the selected strength by signal / 15. Signal 0 calms a Current Generator's region. On a Stillwater Anchor, 0 releases the override and positive signals calm the region. |
| Reverse when powered | Reverse the selected current while powered. |
| Switch Profile A / B | No power uses A; positive power uses B. Each profile saves its own settings. |

Profile actions are directed current, reversed current, calm water or natural flow. Stillwater Anchors offer calm water and natural flow. Natural flow releases this controller's override, allowing underlying controls or natural currents to resume. Eight compass headings keep equal cardinal and diagonal strength.

## Flow Sensor

Horizontal radius and vertical reach are independent. Defaults are radius 4 and height ±4; server limits default to 16 for each. The sensor samples a cylinder, skips unloaded chunks and updates periodically rather than scanning every tick. Place a comparator against it to read the result.

| Measurement | Comparator output |
| --- | --- |
| Current strength | Strongest horizontal current in range. 0 means still; 15 means the configured maximum current strength. At the default maximum of 200%, a 100% current gives output 8. |
| Compass direction | Direction of the strongest sampled current: N=1, NE=2, E=3, SE=4, S=5, SW=6, W=7, NW=8. Still water gives 0. |
| Nearby boats | Boat count, capped at 15. The empty-boats option excludes boats with passengers in this mode. |

Vertical reach applies to both water sampling and boat detection. Comparator output and range/mode settings persist; live status is synchronized to clients.

## Mooring Post

The barrel-style post uses native wooden staves and barrel hoops. It automatically secures one empty boat already inside its configured cylinder. It holds horizontal position while preserving vertical bobbing. It does not pull boats from outside its range or keep an occupied boat captive.

Boarding, redstone power, disabling automatic mooring or breaking the post releases the boat. A short cooldown prevents immediate recapture. Multiple posts cannot hold the same boat. Defaults are radius 4 and vertical reach ±2; both are configurable in the GUI within server limits.

## Optional Create support

The Mechanical Current Generator keeps the original control lid over a fully Create-textured Andesite Casing body, with a recessed rotating shaft input on its bottom face. Its recipe uses a Current Generator, Andesite Casing and Shaft.

Connect a real Create shaft or drive below it. The default calibration is 128 RPM for 100% of the configured profile strength; 64 RPM gives half strength and 256 RPM gives double, limited by the server maximum. Negative RPM reverses the current. Low RPM remains proportionally weak rather than jumping to the manual 5% minimum. The default stress impact is 4 SU per RPM.

A stopped or overstressed drive releases its override. The controller supports the same profiles, connected-water filtering, links and redstone modes as the standard generator. Create is optional; its own dependencies are supplied by Create rather than bundled in Flow Control.

## Migration and validation

Existing controller settings are loaded into Profile A. Old wand regions remain editable through the cleanup mode and `/streamcontrol clear` for operators. `/streamcontrol status` remains available. The old wand radius/strength commands are replaced by GUI profile editing.

Use matching Flow Control 0.2.1 versions on client and server because the settings packet protocol changed. The original `wandEnabled`, `wandRequiresOperator` and `showWandFeedbackParticles` configuration keys are retained for compatibility; they now control the Calibrator and its range outlines.

Both standalone and Create 6.0.10 builds passed compilation and the dedicated-server suite. Tests cover legacy flow behavior, profiles and migration, linking and loop rejection, Calibrator copy/paste/link/cleanup, sensor vertical and horizontal limits, comparator output, occupied boats, mooring release and cooldown, packet round trips, optional registration/recipes, and a real Create motor network including reversal, low RPM, stress and stopping. Actual Minecraft screenshots cover eight GUI/model states with and without Create. This is not a full TNK pack or interactive client-world playtest.
