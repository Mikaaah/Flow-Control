# Contributor reference

Requires Java 17. Put `StreamsReflowing-1.20.1-fabric-2.15.2.jar` in `libs/`, then run `./gradlew build`.
Fabric Loader and Fabric API are resolved by Gradle. Dependency JARs are not redistributed.
Output is in `build/libs/`; install the main JAR, not the sources JAR.

Run `./gradlew runServer -Dstreamcontrol.integrationTest=true` in a disposable development world.
The opt-in checks cover ordinary and baked water flow, boats, region boundaries, overlaps, saved data,
network encoding, dimensions, redstone and controller cleanup. The test server stops afterward.
For Streams versions that disable baked water on dedicated servers, set `forceBakedFluid=true`
in the test world's `config/streamsreflowing.properties` before running the baked-water checks.
Results are written to `run/streamcontrol-integration-results.json`.

Client rendering and complete modpack behavior require in-game playtesting.
