# Troubleshooting

| Problem | Check |
| --- | --- |
| The mod is rejected at startup | Match 1.0.0 with Streams 2.17.1. Older Flow builds require their own Streams version. |
| A controller does nothing | Check the selected profile, Apply changes, redstone mode, radius, height and connected-water filter. |
| A linked controller stops | Check that its source is present and loaded, and that it has local power when mechanical. |
| The mechanical controller stops | Check real shaft power, rotation, RPM, overstress and the profile's redstone mode. |
| A boat drifts | An Anchor calms currents; a Mooring Post secures one empty boat. Boarding or powering the post releases it. |
| A sensor gives an unexpected signal | Check strength/direction/boat mode, vertical reach and comparator placement. |
| Water level does not change | The addon controls current velocity. |

Run `/streamcontrol status` at the affected position. Include mod versions, dimension, controller settings and reproduction steps in a new report.

[Guide home](https://github.com/Mikaaah/Flow-Control/wiki) · [Report an issue](https://github.com/Mikaaah/Flow-Control/issues) · [Discord](https://discord.gg/CRYk3uDPVt)
