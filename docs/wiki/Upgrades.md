# Upgrades

Three independent axes, three tiers each. Right-click an inserter with a module to install it;
the module it replaces is handed back. Breaking the block returns everything.

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

Nowhere, yet. **The nine modules have no recipes** and are creative-only in this beta. The
mechanics, the stacking and the persistence are finished and tested; the crafting is not.

Modules are recognised by the `factory_io:upgrades/<axis>/<tier>` **tags**, so a pack can make its
own item act as a Speed 2 by adding it to `factory_io:upgrades/speed/2` — no Java involved.
