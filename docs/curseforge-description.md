=== SHORT DESCRIPTION ===

Factorio's inserters and belts, in Minecraft. Machines that move items on their own, and belts that carry them — between the inventories you already have.

=== FULL DESCRIPTION ===

# Factor'I/O

**Factorio's inserters and transport belts, in Minecraft.** Items move between the inventories
you already have — vanilla or from any other mod — without a hopper in sight.

Forge 1.20.1 · MIT · [GitHub](https://github.com/DrimoZ/FactoryIO) ·
[Wiki](https://github.com/DrimoZ/FactoryIO/wiki) ·
[Discord](https://discord.com/invite/b8ZutEfWyV)

> **This is a beta.** Inserters and belts are complete and tested. **There are no machines** — no
> furnaces, no assemblers, no ore processing. That is a deliberate pause, not an oversight: this
> release is a transport toolkit rather than a production chain. What ships works; what is
> missing is missing on purpose, and it is listed at the bottom of this page.

---

## Inserters

An inserter takes items out of the block behind it and puts them into the block in front. Seven
of them, differing in how fast, how far, and how selectively.

| Inserter | Items/s | Reach | Hand | Filters | Power |
|---|---|---|---|---|---|
| Burner | 0.59 | 1 | 1 | — | fuel |
| Inserter | 0.83 | 1 | 1 | — | 8 FE/t |
| Long Handed | 1.25 | 2 | 1 | — | 10 FE/t |
| Filter | 0.83 | 1 | 1 | yes | 10 FE/t |
| Fast | 2.50 | 1 | 1 | — | 25 FE/t |
| Stack | 7.50 | 1 | 3 | — | 35 FE/t |
| Stack Filter | 7.50 | 1 | 3 | yes | 40 FE/t |

Throughput sits within 8 % of Factorio's, converted to 20 ticks per second.

**Filters** hold five ghost items, whitelist or blacklist, and each slot matches either that
exact item **or its whole tag** — your choice, per slot. `#forge:ingots` in one slot and a single
plank in the next is a normal setup, and it keeps working when you add another mod.

**Redstone is a comparison, not a switch**: always on, on below a signal strength, or on at or
above it. A comparator reading a chest can run an inserter only when that chest is nearly empty,
with no additional circuit.

## Transport belts

| Belt | Items/s | Carries |
|---|---|---|
| Transport Belt | 10 | 8 items |
| Fast Transport Belt | 20 | 8 items |
| Express Transport Belt | 40 | 8 items |

Two lanes of four slots, as in Factorio. Belts curve, merge from the side, compress against an
obstacle, and **a closed loop keeps turning when full**.

**An inserter drops on the far lane** — the one furthest from it. That is the rule every two-lane
build in Factorio rests on, and it is the belt that enforces it, from the side the request comes
from. Hoppers and pipes from other mods follow the same rule without knowing belts exist.

Belts expose an inventory on **every face**. A hopper above loads one; a hopper below drains one,
taking the front-most item first, because a belt is a queue. A belt never fills a chest by itself
and never empties one — that is what inserters are for.

You can also load a belt by hand: right-click with an item to drop one on the lane and slot you
clicked, right-click empty-handed to take the front one back.

## Upgrades

Three axes — speed, capacity, efficiency — three tiers each, and **they stack**: two Speed
Module 3 are worth more than one. How many fit is a property of the machine, from one on a burner
to four on a stack filter inserter.

Speed shortens the swing without changing what a swing costs, so a faster inserter draws more
power per second and the same per item. Efficiency is the axis that lowers cost per item.

## Data-driven

**An inserter is a data file, not a Java class.** Drop a JSON into `config/factor_io/inserters/`
and you have a new one — block, item, block entity, menu and screen are built from the
definition, and its models, translations, loot table and tags are generated in memory. No assets
to draw, no code to write.

A **datapack** retunes the shipped inserters live and applies on `/reload`. Everything the mod
recognises goes through **item tags**, never a hardcoded list, so another mod's wrench or another
pack's component becomes usable by joining a tag.

Belt speeds, and which of the seven inserters exist at all, are config options. Belt speed
changes apply to belts **already placed**.

## Energy

The mod consumes Forge Energy and **produces none** — bring a generator from Mekanism, Thermal,
or anything outputting FE. **Burner inserters need no power at all** and refuel themselves from
the chest they draw from, so a whole line can run on coal.

The Creative Energy Source covers creative and testing, and is deliberately recipe-less.

---

## What is not in this build

Said plainly, because finding out in-game is worse:

- **No machines** — no furnaces, assemblers or ore processing.
- **Modules and the configurator have no recipes** and are creative-only.
- **No splitters and no vertical belts.**
- **No JEI plugin.**
- Belt worlds may not survive a `0.x` update. Back up.

## Compatibility and permissions

Anything exposing an `IItemHandler` works with both inserters and belts, which is essentially
every storage mod on Forge 1.20.1. Vanilla hoppers work in both directions.

**Modpacks: yes.** No permission needed, public or private, monetised or not. Credit appreciated,
never required.

## Getting help

The **[wiki](https://github.com/DrimoZ/FactoryIO/wiki)** is the reference — player guide, config,
datapack docs and FAQ. Bugs go to
**[GitHub issues](https://github.com/DrimoZ/FactoryIO/issues)**, questions to
**[Discord](https://discord.com/invite/b8ZutEfWyV)**.
