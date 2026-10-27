# Troubleshooting

| Problem | Check |
| --- | --- |
| A controller does nothing | Redstone power disables it. Check water is inside the horizontal radius and vertical reach. |
| Restore mode does not clear an area | Restore removes wand edits; break a block controller to remove its region. |
| A boat still moves | An anchor stops current-induced drift, not steering, collisions or momentum. |
| Water height does not change | The addon changes current velocity. It does not pump water or reshape terrain. |
| An edit is refused | Check permissions, region limits and whether another player owns the edit. |

Run `/streamcontrol status` at the affected position. Include the versions, dimension, controller type, radius and a short recording in bug reports.


[← Wiki home](https://github.com/Mikaaah/Flow-Control/wiki/Home)
