# Store copy

Paste-ready text for the CurseForge project page. Not documentation: this file sells the mod, the
wiki explains it. Every claim here is something the mod does today — check it against
`CHANGELOG.md` before changing a number.

Images go through the description editor's own upload, which refuses anything wider than 850 px;
CurseForge then gives each one a `media.forgecdn.net` URL, to paste in place of the local path
below. They come from the showcase run and `Banners.java` ([`showcase/`](showcase/README.md)).

## Summary

> One line, 256 characters at most, shown under the name in every search result.

Factorio's inserters, transport belts and assembling machines, in Minecraft. Seven inserters,
three belt tiers with ramps, three tiers of crafters, modules — and the Factorio component chain
to build them.

## Categories

Main: **Technology**. Additional: Energy, Fluid, and Item Transport · Processing · Energy.

## Relations

- GeckoLib — **required dependency**
- JEI — **optional dependency**

## Files

Display name `Factor'I/O 0.4.0-beta`, release type **Beta**, game version **1.20.1 only** (not
`1.20`: `mods.toml` refuses it), loader **Forge**. Changelog: the `0.4.0-beta` section of
`CHANGELOG.md`.

## Description

<!-- Everything below this line is pasted into CurseForge's Markdown editor as-is. -->

![Factor'I/O](https://media.forgecdn.net/attachments/description/1013208/description_17cca1e6-fdf9-4e71-998a-227ca2242e28.png)

### Factorio's machines, in Minecraft.

Inserters that move items on their own, belts that carry them on two lanes, and crafters that turn
plates into gears, gears into circuits, circuits into the next machine. It works with the
inventories you already have — vanilla chests, furnaces, and every storage mod on Forge.

![A factory hall: two rows of crafters around a belt bus](https://media.forgecdn.net/attachments/description/1013208/description_f9378dc6-8bde-4fc9-9450-f61cf1236937.jpg)

![Inserters](https://media.forgecdn.net/attachments/description/1013208/description_cb6c08c2-6b72-4eb7-866f-a7bde02789f9.png)

An inserter takes from the block behind it and puts into the block in front. Seven of them, from
the coal-fed burner to the stack filter inserter:

| Inserter | Items/s | Reach | Hand | Filters | Power |
|---|---|---|---|---|---|
| Burner | 0.59 | 1 | 1 | — | coal |
| Inserter | 0.83 | 1 | 1 | — | 8 FE/t |
| Long Handed | 1.25 | 2 | 1 | — | 10 FE/t |
| Filter | 0.83 | 1 | 1 | ✔ | 10 FE/t |
| Fast | 2.50 | 1 | 1 | — | 25 FE/t |
| Stack | 7.50 | 1 | 3 | — | 35 FE/t |
| Stack Filter | 7.50 | 1 | 3 | ✔ | 40 FE/t |

- **They only pick up what the target will accept**, as in Factorio. One inserter on a mixed belt
  brings a crafter exactly the ingredient it is missing, and waits empty-handed in front of a full
  chest.
- **Filters by item or by whole tag**, slot by slot: `#forge:ingots` keeps working when you add a
  mod.
- **The burner refuels itself** from what it takes, so a whole line can run on coal.
- **A screen that tells you why**: expected and measured rate, what a swing costs, and side tabs
  for settings, modules and redstone.

![A stack inserter mid-swing](https://media.forgecdn.net/attachments/description/1013208/description_e9bcfbfd-9af3-4f09-bc1c-ec9d6cd483f7.jpg)

![The inserter screen and its side tabs](https://media.forgecdn.net/attachments/description/1013208/description_b2e1123d-25ad-4af8-a529-c5eda30d2925.jpg)

![Transport belts](https://media.forgecdn.net/attachments/description/1013208/description_2af89e63-74cd-40d2-a480-056a901208bd.png)

| Belt | Items/s |
|---|---|
| Transport Belt | 10 |
| Fast Transport Belt | 20 |
| Express Transport Belt | 40 |

- **Two lanes of four slots**, curves, side merges, compression, and closed loops that keep
  turning when full.
- **The far-lane rule**: an inserter drops on the lane furthest from it, so one inserter on each
  side fills each lane. Hoppers and other mods' pipes follow it too.
- **Ramps**, one per tier, to climb or cross over another belt.
- Every face is an inventory: a hopper above loads a belt, a hopper below drains it.

![Iron on one lane, copper on the other](https://media.forgecdn.net/attachments/description/1013208/description_de663454-7b96-4508-8e70-d67496cf37f7.jpg)

![Three belt tiers, and a ramp crossing them](https://media.forgecdn.net/attachments/description/1013208/description_1a6ce697-f17c-4a2f-b377-a1a7a99f1f86.jpg)

![A belt turning on its own](https://media.forgecdn.net/attachments/description/1013208/description_d7ce17da-31df-4a22-a28f-dc099dfec325.jpg)

![Crafters](https://media.forgecdn.net/attachments/description/1013208/description_c0c3494f-d043-49bf-a2dc-bb6402b84117.png)

Factorio's assembling machine. **Three tiers, one 3×3 machine** placed in one go, with an outline
showing where it will stand.

| Crafter | Speed | Power while working | Module slots |
|---|---|---|---|
| Mk1 | ×0.50 | 30 FE/t | 0 |
| Mk2 | ×0.75 | 60 FE/t | 2 |
| Mk3 | ×1.25 | 150 FE/t | 4 |

- **Pick a recipe from an icon grid**, with search. Recipes above the machine's tier are locked.
- **Feed it from any side, take from any side.** Inputs only accept the recipe's ingredients, and
  only two crafts ahead — an inserter never empties a belt into one machine.
- **Power only while working.** No recipe, no ingredients, no cost.
- **Item and fluid recipes**: up to two fluids in and out, through pipes from other mods or a
  bucket.
- **JEI** lists every crafter recipe, and its `+` button sets it.

![Inserters feeding a crafter](https://media.forgecdn.net/attachments/description/1013208/description_91fa901e-2e8d-4258-9d58-49bcbd14a419.jpg)

![The crafter screen and its recipe picker](https://media.forgecdn.net/attachments/description/1013208/description_f4c380a7-6daa-46e5-99a6-b891010f22aa.jpg)

![Modules](https://media.forgecdn.net/attachments/description/1013208/description_a0485f5a-0d60-48c1-b01f-bb347c8a335a.png)

Speed, productivity and efficiency, three tiers each, for **both** machines:

- In an **inserter**, they shorten the swing, add items per swing, or cut the cost per swing. They
  stack: two Speed 3 are worth more than one.
- In a **crafter**, they have Factorio's effects — up to +50 % speed, +10 % free output, −50 %
  power — and productivity fills a bar that pays out a free craft.
- The **Advanced Redstone Module** turns redstone from a stop into a comparison: run only while a
  chest is nearly empty, with no extra circuit.

![A Mk3 crafter with four modules](https://media.forgecdn.net/attachments/description/1013208/description_4d42b8c6-7833-4c24-8379-b2f913b7139c.jpg)

![The modules](https://media.forgecdn.net/attachments/description/1013208/description_1b0537de-b8a2-4631-9de2-19159a960ad6.png)

Everything is crafted through **Factorio's component chain** — plates from the stonecutter, steel
from the blast furnace, gears, cables, electronic circuits by hand; advanced circuits and
processing units only in a crafter. Forge tags throughout, so other mods' plates and circuits
work.

![The component chain](https://media.forgecdn.net/attachments/description/1013208/description_48894d7c-1c3b-4107-b901-bc46c62ef965.png)

![Everything in the creative tab](https://media.forgecdn.net/attachments/description/1013208/description_bbae93cd-2387-4110-8f3e-ddd441138210.jpg)

![Data-driven](https://media.forgecdn.net/attachments/description/1013208/description_c4d9e2eb-851a-4471-98d2-06cce9e5bbc5.png)

- **A new machine is a JSON file.** Drop one into `config/factor_io/inserters/`, `belts/` or
  `crafters/` and its block, item, screen, model and translations are built for it.
- **Every number is retunable by datapack**, live on `/reload` — speeds, costs, module effects.
- **Crafter recipes are plain datapack recipes**, `factor_io:crafting`, with counted
  ingredients, chances and fluids.
- Validation **refuses and names the problem** in the log. Nothing is silently clamped.

![JEI lists every crafter recipe](https://media.forgecdn.net/attachments/description/1013208/description_9a9e40b2-c450-4057-9fe6-4178db4cd840.jpg)

![Not in this build](https://media.forgecdn.net/attachments/description/1013208/description_38e7f53d-1487-43b2-b88d-0fdb1177271f.png)

- **Placeholder models** for the crafter and the ramps; the real ones are being drawn.
- **No splitters** yet — they are next. No furnaces, drills or power generation: bring Forge
  Energy from another mod, or run on burner inserters and coal.
- This is a **beta**: worlds may not survive a `0.x` update. Back up.

### Requirements

| | |
|---|---|
| Minecraft | 1.20.1 |
| Loader | Forge 47 or newer |
| [GeckoLib](https://www.curseforge.com/minecraft/mc-mods/geckolib) | 4.4 or newer, required |
| [JEI](https://www.curseforge.com/minecraft/mc-mods/jei) | optional, recommended |
| Power | any Forge Energy generator, in survival |

The `1.18.2` files on this page are the original prototype, under another mod ID: they are not
compatible with this version and are no longer supported.

### FAQ

**Can I put it in my modpack?** Yes. No permission needed, public or private, monetised or not.
Credit appreciated, never required.

**Does it work on a dedicated server?** Yes.

**Fabric? NeoForge? Other versions?** Forge 1.20.1 only, for now.

**Where is the documentation?** The [wiki](https://github.com/DrimoZ/FactoryIO/wiki) covers every
machine, the configuration and the datapack format.

### Links

[Wiki](https://github.com/DrimoZ/FactoryIO/wiki) ·
[Source](https://github.com/DrimoZ/FactoryIO) ·
[Issues](https://github.com/DrimoZ/FactoryIO/issues) ·
[Discord](https://discord.gg/YY9gk63rKz)
