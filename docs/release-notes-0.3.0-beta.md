# Factor'I/O 0.3.0-beta

First public build. **Inserters and transport belts are complete and tested. There are no
machines** — that is a deliberate pause, not an oversight: this release is a transport toolkit
rather than a production chain.

**Minecraft 1.20.1 · Forge 47+ · requires GeckoLib 4.4+**

## New

- **Transport belts**, three tiers — 10, 20 and 40 items/s. Two lanes of four slots, curves,
  side merges, compression against an obstacle, and closed loops that keep turning when full.
- **Inserters drop on the far lane**, as in Factorio. The belt decides, from the face the
  request arrives on, so hoppers and other mods' pipes follow the rule without knowing belts
  exist.
- Belts expose an **`IItemHandler` on every face**. A hopper above loads, a hopper below drains
  — from the front, because a belt is a queue.
- **Place and take items by hand**: right-click a belt with an item, or empty-handed.
- **Belt speed is configurable** and applies to belts already placed.
- **Upgrade modules stack** — two Speed Module 3 beat one — up to a per-machine slot count.
- **Configurator** item, and a recipe-less **Creative Energy Source**.
- Crafting recipes for all seven inserters.

## Fixed

Fifty defects, catalogued with their cause. The ones a player would have noticed: a saturated
belt loop stopping for good; an inserter scattering one item type across a whole chest instead of
topping up stacks; an item crossing an entire belt line in one tick; belts loading unloaded
chunks in a cascade; two belts facing each other passing items through one another.

## ⚠️ Breaking

**The mod identifier changed from `factory_io` to `factor_io`**, to match the mod's actual name.
Worlds created with `0.0.3` will not find their blocks and items. There is no migration — the
rename was made at the first beta precisely so it never has to happen again.

## Known gaps

- No machines, no production chain.
- **Modules and the configurator have no recipes** and are creative-only.
- No splitters, no vertical belts, no JEI plugin.
- Client and server simulate belts independently and reconcile only on events; a line watched for
  a long time may drift by one slot.
- `0.x` worlds are not guaranteed across updates. Back up.

---

📖 [Wiki](https://github.com/DrimoZ/FactoryIO/wiki) ·
🐛 [Issues](https://github.com/DrimoZ/FactoryIO/issues) ·
💬 [Discord](https://discord.gg/b8ZutEfWyV)
