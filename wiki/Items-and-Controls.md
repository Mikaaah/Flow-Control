# Items and controls

All three items are in the **Streams Flow Control** creative tab and have survival crafting recipes.

| Item | Action |
| --- | --- |
| **Flow Wand** | Use on water to apply the selected mode within a circular area. Directed water follows your horizontal view. |
| Wand modes | Aim at the sky and **sneak-use** to cycle **Direct flow → Still water → Restore natural flow**. |
| Wand radius | Aim at the sky and **use** to cycle the radius, or run `/streamcontrol radius <blocks>` while holding the wand. |
| Wand strength | `/streamcontrol strength <0.05–2>` while holding the wand. Server limits apply. |
| **Current Generator** | Place under or beside water. Its top arrow points in the current direction. Use with an empty hand to rotate clockwise. |
| **Stillwater Anchor** | Place under or beside your dock to cancel water currents in its area. |
| Controller radius | Sneak-use with an empty hand to increase the radius. Use a wand to copy its radius, strength and your facing to the controller. |
| Redstone | A powered controller is **disabled**. Remove power to enable it again. |
| Remove block control | Break the block; its region is removed immediately and underlying flow returns. |

A radius of four means a horizontal circular area four blocks from the selected water or controller.
The default vertical reach is four blocks above and below it. Only water in that volume is affected.
Flow strength changes current velocity; it does not pump water into empty blocks or move the water surface.

For a dock, place a Stillwater Anchor in the riverbed within four blocks of the moored boats. Adjust its radius if needed.
Stillness stops water-induced drift. It does not lock a boat against steering, collisions, wind from other mods or existing momentum.

[← Wiki home](https://github.com/Mikaaah/Streams-Flow-Control/wiki/Home)
