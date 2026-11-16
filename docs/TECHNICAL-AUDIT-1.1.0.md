# Flow Control 1.1.0 — multi-version technical audit

**Status: implementation complete; CI compilation and runtime validation pending.** This report describes the source changes prepared for the nine release targets below. A pending check is not a successful build, server test, client test, or performance measurement. The final release evidence must identify the exact commits, workflow runs, artifacts, and results used to replace this status.

The audit covers the existing Flow Control addon, its Streams Reflowing vector hooks, optional Create kinetic integration, and the supplied **Glazed Relics & Dock Instruments** models and textures. Corrected Java classes and resources are delivered as source in each target's repository tree; this report maps the changes to those files rather than duplicating nine versions of each class in prose.

## Supported target matrix

| Minecraft | Loader | Java toolchain | Serialization/rendering family | Native Create fixture |
| --- | --- | --- | --- | --- |
| 1.20.1 | Fabric | 17 | Legacy `CompoundTag`, Fabric networking, legacy vertex calls | Create Fabric `6.0.8.1+build.1744-mc1.20.1` |
| 1.20.1 | Forge | 17 | Legacy `CompoundTag`, `SimpleChannel`, legacy vertex calls | Create `6.0.8` |
| 1.21.1 | Fabric | 21 | Registry-provider NBT, payload networking, modern vertex calls | None; common dormant thruster |
| 1.21.1 | NeoForge | 21 | Registry-provider NBT, payload networking, modern vertex calls | Create `6.0.10`, Flywheel 1 API |
| 1.21.11 | NeoForge | 21 | `ValueInput`/`ValueOutput`, version-specific rendering APIs | None; common dormant thruster |
| 26.1.2 | Fabric | 25 | Value serialization, `Identifier`, version-specific rendering APIs | None; common dormant thruster |
| 26.1.2 | NeoForge | 25 | Value serialization, `Identifier`, version-specific rendering APIs | None; common dormant thruster |
| 26.3 | Fabric | 25 | Value serialization, `Identifier`, render-state extraction/submission | None; common dormant thruster |
| 26.3 | NeoForge | 25 | Value serialization, `Identifier`, render-state extraction/submission | None; common dormant thruster |

These are nine distinct loader/version builds, not a claim that one binary works on all versions. All retain a **minimum Streams Reflowing version of 2.17.3**, with the repository's current build/test fixture at **2.17.5**. Minor Streams updates are not blocked by an exact-version dependency pin. Native Create support is limited to the three fixture targets listed above; the remaining six builds do not pretend to implement a nonexistent target-specific Create API.

The exact native Create fixtures are downloaded and hash-checked by each applicable `tools/fetch_create.py`. They are compile/test dependencies, not bundled copies of Create. NeoForge 1.21.1 uses `create-1.21.1-6.0.10.jar`; Forge 1.20.1 uses `create-1.20.1-6.0.8.jar`; Fabric 1.20.1 uses `create-fabric-6.0.8.1+build.1744-mc1.20.1.jar`. Optional runtime fixture selection remains controlled by the repository build configuration.

## Findings and corrections

| Finding | Consequence | Corrected source / behavior | Targets |
| --- | --- | --- | --- |
| Controller block entities did not unregister active flow on removal/unload. | A controller could leave a stale override after its block entity stopped ticking. | `ControllerEntity.setRemoved()`, native `MechanicalCurrentEntity.invalidate()`, and `ControllerSettings.removed()` remove the runtime region. Create's `SmartBlockEntity.setRemoved()` is final and delegates to `invalidate()`, so the native kinetic classes override that supported lifecycle hook instead. Persistent profiles remain in the block entity and refresh when it loads again. | All nine; kinetic entity on native Create targets |
| Saved controller claims could survive independently of a live controller after restarting. | Old SavedData could apply flow from absent or unloaded controllers. | `ControlData` validates controller claims once on runtime access, checks only already loaded chunks, and removes stale claims. Wand edits remain saved independently. A subsequently loaded controller recreates its own claim from its block-entity settings. | All nine |
| Controller refreshes and periodic connectivity refreshes aligned on global tick boundaries. | Many controllers could perform their work on the same tick. | `ControllerSettings` staggers the ten-tick refresh by block position. Connectivity masks use per-controller deadlines, dirty invalidation, and a 100-tick recovery refresh. Repeated water notifications in one tick retain pending invalidation rather than repeatedly scanning an unchanged volume. | All nine |
| Clearing many regions rebuilt the spatial index once per removal. | Clear commands incurred repeated whole-index rebuilds, producing quadratic work in the number of regions cleared. | `ControlData.removeMatching()` removes the selected records, rebuilds once, then sends the required removals. `ControlCommands` uses this bulk path. | All nine |
| A flow region could retain a mutable `BlockPos`, or an oversized connectivity mask. | Later cursor mutation could move an indexed region; malformed data could retain more mask storage than its bounded geometry requires. | `FlowRegion` copies the center to an immutable position, bounds radius/vertical reach, and caps the copied mask to its maximum volume. Accessors return a mask copy. | All nine |
| Modern ownership loading did not accept the legacy four-integer UUID encoding. | A migrated wand region could lose its recorded player owner. | Modern `FlowRegion.load()` accepts the current UUID representation and the legacy four-integer form. | 1.21.11 / 26.x; legacy readers remain native to their API |
| Sensor client updates depended on the quantized comparator output changing. | A same-strength direction change could leave the animated vane stale; small speed changes could leave the dial stale. | `FlowSensorEntity` synchronizes measured speed and heading in block-entity update tags, and sends a block update when measurements change. Comparator neighbor updates still occur only when the actual 0–15 output changes. | All nine |
| Sensor fluid sampling checked chunk availability repeatedly at every Y coordinate. | Redundant chunk checks added work within a bounded scan. | `FlowSensorEntity` checks each X/Z column before its vertical samples. The configurable sample interval is defensively kept positive. | All nine |
| Some modern sensor save overrides omitted the superclass save call. | Base block-entity data was not explicitly preserved through that override. | `FlowSensorEntity.saveAdditional(ValueOutput)` calls the matching superclass method. | Value-serialization targets |
| Client override/animation caches were not explicitly cleared on disconnect. | Joining another world or server in the same dimension could briefly retain stale local state. | `FlowOverrides.clearClient()` and client logout handlers clear the region index, animation state, and inspection readout. | All nine |
| Legacy cancellable flow-return callbacks replaced the return value even when no override applied. | They could cancel later return injections outside Flow Control's affected regions, interfering with vanilla/Streams water handling. | Legacy `WaterFlowMixin` and `FluidStateFlowMixin` change the callback value only when `FlowOverrides` actually replaces the original vector. Modern composable return-value hooks and the earlier Fabric 26.3 compatibility correction are retained. | Legacy callbacks corrected; modern hooks reviewed |
| Configuration packets could reach block-entity lookup in an unloaded chunk on affected NeoForge paths. | A forged or stale GUI position could cause unintended chunk work. | `ControlNetwork` requires the chunk to be loaded before lookup, while retaining distance and block-entity checks. | Applicable NeoForge packet paths; equivalent platform guards reviewed |
| Hollow engineering blocks inherited full-cube occlusion properties. | Surrounding water/faces could be hidden despite visibly open frames or the narrow post. | Block registrations use `noOcclusion()`, including native and dormant thrusters. Waterlogging remains a separate fluid-state property. | All nine |
| Mooring auto-release relied on the tick fallback. | A passenger could mount a boat while the post still considered it tethered until a later tick. | `MooringMountMixin` releases after a passenger has actually been added. The common successful-mount hook works across the loader matrix; tick validation remains as a fallback. | All nine |
| Mooring saved tether coordinates accepted non-finite values. | Invalid persisted coordinates could be used for boat correction. | `MooringEntity` rejects invalid tether coordinates and clears the claim instead. | All nine |
| Client rope rendering lacked a synchronized numeric boat identity. | A client could not resolve the authoritative tether cheaply and reliably. | Server UUID ownership remains authoritative; a numeric boat entity ID is sent in update tags for constant-time client lookup. Claim, release, unload cleanup, and actual entity-ID changes synchronize state. | All nine |
| Mooring acquisition sorted candidates and corrected boats unnecessarily. | Busy docks could do avoidable work while searching or holding an already stationary boat. | The nearest available candidate is selected in one pass on staggered ten-tick acquisition, avoiding an additional sorted candidate collection. The active tether checks one claimed boat each tick; movement correction runs only when horizontal displacement or momentum requires it, preserving vertical bobbing. | All nine |
| A thruster existed only when native Create registration was available. | Worlds/items had no consistent fifth-block fallback when Create was absent. | `DormantMechanicalBlock`, `DormantMechanicalEntity`, and `DormantMechanicalIntegration` register the same block/item/block-entity identifiers without Create classes, preserve controller settings, report zero RPM, and generate no current. | All nine absent Create; permanent common fallback on the six non-native targets |
| The supplied moving pieces, glass, emissive details, and tie-ring pivot were not connected to runtime state. | Static exports alone could not provide the requested live animation, rope attachment, or controlled glow. | Version-specific renderers use the supplied fixed-body/partial split, synchronized controller/sensor/mooring state, exact pivots, native texture resources, and explicit full-bright support geometry. Native Create targets use the supplied shaft/propeller partials with Flywheel and a block-entity-renderer fallback. | All nine; native Flywheel only where supported |

Stillwater arbitration was checked and retained: `RegionIndex` prioritizes controller regions over wand regions, then stillness within that category, then the newest sequence for equal priority. A controller Stillwater Anchor therefore keeps precedence over overlapping controller generators even when those generators refresh. This was an existing correct rule, not a newly discovered flicker bug. A regression check now exercises repeated generator updates against an overlapping anchor.

## Version-specific compliance reviewed

The requested API families were checked against the code appropriate to each target. A newer-looking API is not automatically correct on an older release.

| Area | Audit conclusion |
| --- | --- |
| 1.20.1 serialization and networking | Legacy `CompoundTag` methods and Forge `SimpleChannel` / Fabric networking remain appropriate. They were not mechanically replaced with newer signatures. |
| 1.20.1 vertices and resource identifiers | `vertex().color().uv()...endVertex()` and the legacy `ResourceLocation` constructor are correct for these targets. Their presence is not a deprecation defect for 1.20.1. |
| 1.21.1 block entities | `HolderLookup.Provider` save/load/update-tag signatures and `ClientboundBlockEntityDataPacket` are used for the target API. |
| 1.21.1 vertices and identifiers | Render paths use `addVertex`, `setColor`, `setUv`, `setOverlay`, `setLight`, and `setNormal` where applicable, with `ResourceLocation.fromNamespaceAndPath(...)`. |
| 1.21.11 serialization | `ValueInput` / `ValueOutput` is already the correct family here. It must not be reverted to 1.21.1 provider-NBT overrides merely because both version names begin with 1.21. |
| 26.x loaded-chunk checks | Deprecated `LevelReader.hasChunkAt` convenience calls in audited server paths use `getChunkSource().hasChunk(chunkX, chunkZ)` instead, including `ControllerSettings`, `ConnectedWater`, `FlowSensorEntity`, `ControlData`, `MooringEntity`, `FlowWand`, and `ControlNetwork`. Exact 26.1.2/26.3 bytecode confirms this is a non-loading check; the deprecated `LevelReader.hasChunk` wrapper is also avoided. |
| 26.x serialization, identifiers and rendering | Value serialization, target `Identifier` APIs, entity/passenger hooks, and the newer rendering interfaces are adapted per target. The 26.3 renderers extract immutable visual data and submit using their target API. |
| Item settings | Legacy custom item NBT is retained where appropriate; newer item settings use the target's `DataComponents`/custom-data access. No new component registration is required for the supplied art. |
| Waterlogging | All five blocks expose `WATERLOGGED`, return source water for a wet state, schedule fluid updates through the target's neighbor-update API, and retain the wet property through horizontal rotations/mirrors. Native Create shaft behavior does not replace the block's fluid state. |
| Optional dependencies | Common/client initialization selects native Create code only behind the platform's loaded-mod check. Common dormant classes, animation snapshots, and normal blocks do not import Create/Flywheel types. A build without native Create uses the dormant registration directly. |
| Kinetic behavior | The native thruster accepts a shaft only from `Direction.DOWN`; stress impact comes from `kineticStressImpact`. Flow strength uses absolute RPM divided by `kineticRpmForFullStrength`, bounded by maximum strength; negative RPM reverses flow, and stopped/non-finite RPM removes its claim. These existing rules were retained and included in fixture checks. |
| Resource layout | 1.20.1 retains plural tag directories and legacy item model entry points. Later targets use their singular tag directories and client `items/*.json` definitions where required. Render-layer declarations/registration follow each loader/version rather than relying on a NeoForge-only JSON key everywhere. |

## Flow and tick-cost boundaries

The audit found no unbounded whole-world block search in the flow query path. The following are code-level limits and algorithmic observations, **not measured TPS results**.

| Operation | Bound / cadence | Remaining cost |
| --- | --- | --- |
| Physics/vector lookup | `RegionIndex` maps regions to intersected chunks; a query examines only that chunk's candidate regions. | Cost still grows with the number of overlapping regions in one chunk. It is not globally constant time. |
| Region indexing | Radius is limited to 32; one region intersects at most 5×5 horizontal chunk columns. Configured regions default to 2,048 and cap at 8,192 per dimension. | An actual edit rebuilds the immutable index; frequent edits across many regions remain more expensive than a read. Bulk clear now rebuilds once. |
| Connected-water mask | Six-neighbor flood fill over a bounded cylinder, never loading absent chunks; radius ≤32 and vertical reach ≤16. | The safe enclosing volume is 65×65×33 = 139,425 cells, with at most 2,179 stored 64-bit mask words. Large dirty controllers still have a real scan cost. |
| Mask invalidation | Configuration/geometric changes, local water-change notifications, then a per-controller 100-tick recovery refresh; ordinary controller refreshes are staggered over ten ticks. | Recovery refresh permits changes not covered by an adjacent notification to converge without a per-tick flood fill. A pathological stream of genuine geometric edits can still request immediate rescans. |
| Flow Sensor | Configurable sampling interval defaults to 20 ticks, allowed 5–200; positions are staggered. Radius ≤16, vertical reach ≤16, loaded columns only. | The enclosing maximum is 33×33×33 = 35,937 positions before the horizontal cylinder filter. This is bounded periodic sampling, not a claim that sensors are free at scale. |
| Sensor comparator | Neighbor comparator notifications only when the numeric output changes. | Visual measurement changes may still send a block-entity update without a comparator notification. |
| Mooring acquisition | One local boat query every ten staggered ticks while searching, then a single-pass nearest selection. | Cost depends on boats in the configured local area. A held boat receives a single-entity validity/correction check each tick. |
| Linked controllers | Links are range-limited and followed through at most sixteen nodes with cycle detection and loaded-chunk checks. | A missing or unloaded source pauses the dependent controller; resolving a link does not load its source chunk. |
| Client animation | Local synchronized state and cached model geometry; no server packets or block-volume queries per frame. Rope geometry uses a fixed 24-segment crossed-ribbon curve. | Geometry submission and per-visible-entity animation still have client CPU/GPU cost; no frame-rate measurements have been made. |

Controller unload removes only its active claim. It does not erase Profile A/B, redstone mode, connectivity preference, or linked-source settings from the block entity. Mooring claims use weak world ownership and explicit release/removal cleanup. Client logout clears local flow/animation caches; a dimension-only key is not treated as a server identity.

## Supplied model and texture integration

The input supplies five editable `.bbmodel` projects, corresponding Java JSON geometry, **124 cuboids**, and a **27-material** manifest. Runtime geometry comes from the JSON parts; Generic Blockbench animation clips are editor previews, not executable Minecraft animations.

Placed blocks use the fixed `parts/<id>/body` plus non-overlapping renderer partials. Item models use the complete assembly. The complete assembly and its moving parts are never deliberately submitted together for a placed block. This avoids duplicate faces/z-fighting introduced by rendering both representations. Glass and native Conduit cage alpha are rendered as cutout; the supplied glass is binary-alpha and does not require a translucent pass.

| Block / retained asset ID | Moving or attached parts | Authored pivot in model pixels | Runtime connection |
| --- | --- | --- | --- |
| Current Generator / `current_generator` | `impeller`; `conduit_core`; glass windows | Impeller `[8,8,4]`; core `[8,8,10]` | Local-Z impeller rotation; eased core spin/bob and glow from resolved active state/strength. |
| Stillwater Anchor / `stillwater_anchor` | `conduit_outer_cage`; `conduit_eye` | Both `[8,8,8]` | Fixed housing stays still; cage/eye spin/bob and emission ease with active state, including redstone disable. |
| Flow Sensor / `flow_sensor` | `current_vane`; `dial_needle`; single glass pane | Vane `[8,12,8]`; needle `[8,5,1.25]` | Vane follows synchronized measured heading with `rotLerp`; needle follows synchronized speed. The vane's water heading is independent of the housing's cardinal facing. |
| Create Hydro-Current Thruster / `mechanical_current_generator` | `shaft`; `propeller` | Shaft `[8,2,8]`; propeller `[8,9,3]` | Shaft spins on local Y; propeller spins on local Z. Both follow signed live RPM on native Create targets. Without Create they remain static and generate no flow. |
| Mooring Post / `mooring_post` | `rope_anchor`; `status_indicator`; procedural rope | Ring `[8,9,3.5]`; indicator `[8,17,6]` | Ring world offset is `[0.5,0.5625,0.21875]` before horizontal facing rotation. The rope ends at the live client boat; indicator emission follows secured state. |

All pivot coordinates are divided by sixteen for block-unit transforms. The housing uses cardinal placement facing; diagonal flow direction does not incorrectly rotate the entire frame. Motion uses elapsed game time/partial ticks rather than adding a fixed amount per rendered frame. Newer renderer APIs copy a data-only animation frame into their render state.

Native Create visuals use the actual supplied bottom shaft and propeller meshes, not a generic centered shaft rendered through the wrong origin. Where Flywheel visualization is active it owns those two partials and the ordinary renderer skips them; where it is unavailable the block-entity renderer supplies them. Visual deletion and light refresh are implemented for the native instances.

Texture handling preserves native Minecraft resource identifiers and the non-square **32×16 Conduit base/cage layouts**. All supplied Create material identifiers are remapped to local `streamcontrol:block/fallback_*` reference-pixel copies, including the full assembly, static body, moving partials and item parents. The same local identifiers are used with or without Create, so a placed dormant thruster or its item does not depend on a Create resource pack being loaded. Entity texture resources are read as their actual PNG resources instead of assuming that every material is a 16×16 terrain-atlas sprite. The native eye uses its authored glyph crop. The base Flow Sensor retains the local unchanged brass texture so it does not require Create merely to display its pivot. The local Create copies preserve the supplied source pixels; remapping changes their resource namespace rather than recoloring those materials.

The supplied 27 `_e.png` support masks include three nonempty details: the Conduit eye, redstone needle, and lit indicator. The integration creates explicit emissive partial geometry/material passes and submits them full-bright with controlled brightness; the filename suffix alone is not treated as emission. Full-bright rendering does not claim to add dynamic world lighting. Transparent support masks do not create phantom glow surfaces.

The Sensor outline reflects its 14-pixel height; the Mooring Post selection/collision bounds account for its narrow body and 18-pixel-high cap. Hollow blocks are non-occluding. These code properties are handled explicitly because importing a JSON model cannot change a block's collision, fluid state, or neighbor-face culling by itself.

Resource deliverables include the five `blockstates/*.json`, complete `models/block/*.json`, fixed/moving/glass/emissive `models/block/parts/**`, all five `models/item/*.json`, modern `items/*.json` where needed, direct/fallback textures, retained emissive masks, and the five editable Blockbench sources with the manifest under `docs/model-assets/`.

## Source coverage inventory

The baseline NeoForge 1.21.1 tree contained **45 original Java classes**. All are covered below, with their platform equivalents inspected in the other eight trees. A listed class need not change when its existing implementation is already correct.

| Category | Count | Original classes covered |
| --- | --- | --- |
| Flow execution, profiles and spatial data | 9 | `ConnectedWater`, `ControlData`, `ControllerSettings`, `FlowController`, `FlowHeading`, `FlowOverrides`, `FlowProfile`, `FlowRegion`, `RegionIndex` |
| Blocks and block entities | 5 | `ControllerBlock`, `ControllerEntity`, `FlowSensorEntity`, `MooringEntity`, `UtilityBlock` |
| Registration, configuration, networking and player tools | 7 | `ControlCommands`, `ControlConfig`, `ControlNetwork`, `EngineeringBlockItem`, `FlowWand`, `StreamControl`, `UtilityAccess` |
| Common validation/fixtures | 4 | `ClientTestWorld`, `EngineeringChecks`, `HeadingRegressionChecks`, `IntegrationChecks` |
| Client UI, inspection and existing previews | 8 | `client/ClientGameplayChecks`, `client/ClientUpdates`, `client/ControllerPreview`, `client/ControllerScreen`, `client/EngineeringClient`, `client/EngineeringWidgets`, `client/ModelPreviewScreen`, `client/UtilityScreen` |
| Native Create integration and fixtures | 6 | `compat/create/CreateChecks`, `compat/create/CreateClient`, `compat/create/CreateClientFixture`, `compat/create/CreateIntegration`, `compat/create/MechanicalCurrentBlock`, `compat/create/MechanicalCurrentEntity` |
| Streams/vanilla mixin hooks | 6 | `mixin/BakedFlowMixin`, `mixin/ClientGridMixin`, `mixin/FluidStateFlowMixin`, `mixin/GridFlowMixin`, `mixin/SurfaceParticlesMixin`, `mixin/WaterFlowMixin` |

New common classes include `DormantMechanicalBlock`, `DormantMechanicalEntity`, `DormantMechanicalIntegration`, `SharedAuditChecks`, and `ModelAuditChecks`. New rendering classes include `client/ConduitAnimations`, `client/ConduitMeshes`, `client/ConduitBlockRenderer`, the target-specific render-state adapter where needed, and native `compat/create/ThrusterVisual`. `mixin/MooringMountMixin` supplies the successful-passenger hook. Legacy/native Create ports retain their own appropriate Flywheel interfaces; the modern implementation is not blindly copied into older APIs.

JSON/resource coverage includes loader metadata, mixin declarations, blockstates, full/partial/item models, client item definitions, tags, recipes/loot where present, language entries, texture paths, PNG alpha/emissive masks, and the supplied manifest/editor projects. Existing item/block registry IDs and gameplay recipe identities are preserved.

## Corrected-source map

Paths below are relative to `src/main/java/nl/townsandkingdoms/streamcontrol/` in the appropriate release tree.

| Files | Purpose |
| --- | --- |
| `ControllerSettings.java`, `FlowController.java`, `ControllerEntity.java` | Staggered cached execution, active-state synchronization, removal cleanup, compatible fallback inheritance. |
| `ControlData.java`, `ControlCommands.java`, `FlowRegion.java`, `FlowOverrides.java` | Runtime claim lifecycle, bulk clear, immutable/bounded records, UUID migration and client cache reset. |
| `FlowSensorEntity.java` | Correct measurement/update-tag sync, comparator notification behavior and loaded-column sampling. |
| `MooringEntity.java`, `mixin/MooringMountMixin.java` | Immediate successful-mount release, robust saved tether state, ownership cleanup, boat-ID sync, authored rope anchor and cheaper acquisition. |
| `StreamControl.java`, `ControllerBlock.java`, `UtilityBlock.java`, `DormantMechanical*.java` | Non-occluding waterloggable blocks, accurate utility outlines, stable dormant thruster registration and preserved settings. |
| `ControlNetwork.java`, `FlowWand.java`, affected 26.x server classes | Loaded-chunk checks before server lookups; direct nondeprecated chunk-source checks on 26.x. |
| `mixin/WaterFlowMixin.java`, `mixin/FluidStateFlowMixin.java` | Legacy pass-through behavior outside actual overrides; modern composable hooks retained. |
| `client/ConduitAnimations.java`, `client/ConduitMeshes.java`, `client/ConduitBlockRenderer.java`, target render-state adapters | Exact supplied partial geometry, pivots, animation snapshots, cutout/native-resource handling, full-bright details and procedural rope. |
| `client/EngineeringClient.java`, platform client initialization/lifecycle adapters | Renderer/reload registration, target render-layer setup and disconnect cleanup. |
| `compat/create/CreateClient.java`, `compat/create/ThrusterVisual.java`, `compat/create/MechanicalCurrentEntity.java` | Native kinetic partial rendering, instancing/fallback ownership, lighting/deletion and controller unload cleanup through Create's `invalidate()` hook. |
| `SharedAuditChecks.java`, `ModelAuditChecks.java`, `EngineeringChecks.java`, `IntegrationChecks.java` | Targeted regression assertions and opt-in disposable-world harness integration. |

## Validation prepared and remaining

The existing opt-in harness is extended, rather than treating a source review as a runtime pass:

- `SharedAuditChecks` checks immutable centers, legacy UUID ownership, stable Stillwater priority through repeated generator updates, bulk clear/index removal, controller unload/re-registration, resolved controller visual state, and a sensor direction change whose comparator strength stays constant. It inspects the sensor update tag for synchronized measurements.
- `ModelAuditChecks` checks that all five blocks exist, expose waterlogging, preserve it through rotation/mirror, expose the expected water fluid state, remain non-occluding, and preserve wet placement. A stopped/dormant thruster must report zero RPM and create no flow region.
- Existing engineering/integration checks continue to cover controller profiles, redstone behaviors, saved settings, utility behavior and mooring logic; they are updated to invoke the new checks.
- Native `CreateChecks` exercises real kinetic network RPM, reversal, configured stress, stopped/overstressed flow removal, and the bottom input arrangement in each supported fixture.
- Resource checks are prepared for JSON validity, reference resolution, part partitioning, pivot/material consistency, emissive resources, loader metadata and the expected per-target resource layout. Runtime resource reload/visual inspection is a separate check.
- Each release tree must compile and run its required dedicated-server/fixture workflow, including absent-Create startup. Native Create targets also require their optional Create fixture run. Client checks must record any actual in-game rendering, resource reload, facings, glass/needle depth, forward/reverse RPM, rope attachment, and shader/instancing fallback evidence.

Limited local API validation has completed for **Fabric 26.1.2** and **Fabric 26.3**: direct `javac` checks against their exact Minecraft/Fabric target fixtures compiled the tested source snapshots with **zero errors**, including the required Fabric access widening. These checks helped identify the deprecated chunk-check wrappers and the correct renderer API signatures. They are not Gradle release builds, remapped JAR verification, packaged artifacts, dedicated-server runs, in-game client passes, or proof that subsequent edits passed. Final CI must validate the final source snapshots and produce the release artifacts.

At the time this draft was written, these new CI/runtime results were **pending**. No new success percentages, TPS timings, profiler traces, screenshots, frame-rate figures, or complete nine-target client passes are asserted here. Historical validation JSON files elsewhere in `docs/` refer to their named older releases and must not be presented as 1.1.0 evidence.

## Full-modpack limits

The reported Fabric 26.3 vanilla-water/`forceBakedFluid`/naturally waterlogged-block symptoms motivated special attention to composable vector hooks and non-occluding waterlogged models. The user's complete modpack, world, configuration, dependency list and reproduction sequence are not present in this source workspace. The audit therefore does not claim that every visual symptom in that pack has been reproduced, or that Flow Control was proven to be its only cause.

The prepared fixes address concrete interactions visible in the code: pass through untouched vectors, avoid stale controller claims, preserve waterlogged states, avoid hiding surrounding water with full-cube occlusion, and keep Streams as a minimum-version dependency. Pack-specific interactions, render mods/shaders, Create/Flywheel backends, chunk-unload/reload behavior under a real pack workload, and performance with thousands of overlapping controllers still require the actual pack or a documented equivalent fixture. Those limits do not change the nine-target source/API audit scope.
