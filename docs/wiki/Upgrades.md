# Upgrades

Three independent axes, three tiers each. Right-click an inserter with a module to install it in
the first free slot, or place it yourself in the **Upgrades** tab of the screen — which also shows
what the installed modules change, before → after. Breaking the block returns everything.

| Module | Axis | Per tier | Pays |
|---|---|---|---|
| Speed 1-3 | speed | −25 % swing duration | same cost per swing, so more energy per second |
| Productivity 1-3 | capacity | +1 item per swing | — |
| Efficiency 1-3 | efficiency | −25 % cost per swing | — |

## They stack

Two Speed Module 3 are worth more than one — tiers add up rather than the best one winning. What
limits you is the number of **slots on the machine**, not one module per axis.

| Inserter | Slots |
|---|---|
| Burner | 1 |
| Inserter, Long Handed, Filter | 2 |
| Fast | 3 |
| Stack, Stack Filter | 4 |

Slots are a property of the model, following the crafting chain: the machines that cost more to
build take more modules. Four slots on a stack filter inserter means four Speed 3, or two Speed 3
and two Efficiency 3, or any other split.

## Reading the trade

Speed shortens the swing without changing what a swing costs. Doubling an inserter's rate
therefore doubles its **power draw per second** while leaving its cost **per item** untouched.
Efficiency is the axis that lowers cost per item; Productivity raises items per swing, which
lowers cost per item too, by the other route.

Nothing here is a pure loss, and nothing is free: the cost is always the slot you spent.

## Where modules come from

From the Factorio component chain, brought to a crafting table:

| Step | How |
|---|---|
| Iron, copper plate | an ingot in the **stonecutter** |
| Steel plate | an iron plate in the **blast furnace** |
| Iron gear wheel | 2 iron plates |
| Copper cable | 1 copper plate → 2 |
| Electronic circuit | 1 iron plate + 3 copper cables |
| Advanced circuit | 2 electronic circuits + 2 dried kelp + 4 copper cables |
| Processing unit | 4 electronic + 2 advanced circuits + 1 gunpowder |

Dried kelp stands in for plastic and gunpowder for sulfuric acid until machines exist.

**Tier 1** — 4 electronic and 4 advanced circuits around a core. **Tiers 2 and 3** — 2 modules of
the tier below, 3 processing units, 3 advanced circuits, around a core. The core tells the families
apart, since Factorio gives all three the same recipe:

| | Tier 1 | Tier 2 | Tier 3 |
|---|---|---|---|
| Speed | sugar | rabbit's foot | phantom membrane |
| Productivity | piston | sticky piston | shulker shell |
| Efficiency | lapis lazuli | amethyst shard | echo shard |

Every ingredient is a Forge **tag** where one exists — `forge:plates/iron`, `forge:wires/copper`,
`forge:circuits/basic`, `/advanced`, `/elite` — so plates and circuits from other mods work in
these recipes, and ours work in theirs.

Modules are recognised by the `factor_io:upgrades/<axis>/<tier>` **tags**, so a pack can make its
own item act as a Speed 2 by adding it to `factor_io:upgrades/speed/2` — no Java involved.
