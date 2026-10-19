# Contributor reference

## Build and validation

Requires Java 21. Put `StreamsReflowing-1.21.1-neoforge-2.15.2.jar` in `libs/`, then run `./gradlew build`.
Dependency jars are not redistributed. Output is in `build/libs/`.

The opt-in dedicated-server integration harness checks actual ordinary/baked water flow, boat current APIs,
radius and height boundaries, overlapping regions, save/load and network codec round trips, dimension isolation,
redstone disable/re-enable and block-removal cleanup. It runs only with `-Dstreamcontrol.integrationTest=true`
and stops the test server afterward. Use a disposable development world.

Client visuals and full modpack playtesting remain necessary before calling this a stable release.
