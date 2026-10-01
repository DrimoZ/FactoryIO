# Datapack Guide

Inserters, belts and crafters work the same way. Four jobs, four places:

| I want to… | Where | Applies |
|---|---|---|
| **create** an inserter, a belt or a crafter | `config/factor_io/<kind>/<name>.json` | next launch |
| **retune** an existing one | `data/<namespace>/factor_io/<kind>/<name>.json` | `/reload` |

`<kind>` is `inserters`, `belts` or `crafters`.
| **remove** a shipped one | `config/factor_io/factor_io-common.toml` | next launch |
| **dress** it | a resource pack providing the texture | resource reload |

**Only the `config/` file creates anything.** A texture is appearance, a datapack is tuning:
which blocks exist has to be known before registration, long before any datapack is read. A
missing texture gives the purple-and-black checkerboard, never a missing block; a datapack naming
something unknown logs a warning pointing to `config/`.

> **Multiplayer:** `config/factor_io/` must be **identical on the server and every client**, since
> it defines which blocks exist. Ship it with your modpack; a mismatch is refused at login with a
> registry error.

## The name is the file name

`dense_inserter.json` declares `factor_io:dense_inserter`. There is no `name` field.

## Fields

All are optional. Anything omitted keeps its default.

| Field | Type | Default | Meaning |
|---|---|---|---|
| `ticksPerSwing` | int > 0 | — | duration of one arm movement; **the only field describing speed** |
| `preferredItemCountPerAction` | int > 0 | 1 | hand size, items moved per swing |
| `grabDistance` | int > 0 | 1 | how far behind and in front it reaches |
| `filterable` | bool | false | gives the machine its five filter slots |
| `affectedByRedstone` | bool | true | whether the redstone condition applies at all |
| `upgradeSlots` | int ≥ 0 | — | how many modules may be installed |
| `useEnergy` | bool | false | electric when true, fuel-burning when false |
| `energyCapacity` | int > 0 | — | buffer, electric only |
| `energyTransferRate` | int > 0 | — | maximum intake per tick, electric only |
| `energyConsumption` | int > 0 | — | cost **per swing**, electric only |
| `fuelCapacity` | int > 0 | — | burn-time reserve, burner only |
| `fuelConsumption` | int > 0 | — | burn ticks **per swing**, burner only |
| `texture` | resource location | — | override the block texture |
| `translations` | map | — | display names per language code |

Rate is derived, never declared:

```
items/s = 20 × preferredItemCountPerAction / (2 × ticksPerSwing)
```

A swing takes `ticksPerSwing`, and a full cycle is two swings — out and back. Because one field
describes speed, no two numbers can disagree about how fast a machine is.

## Creating an inserter

`config/factor_io/inserters/dense_inserter.json`:

```json
{
  "useEnergy": true,
  "filterable": true,
  "ticksPerSwing": 6,
  "preferredItemCountPerAction": 2,
  "grabDistance": 1,
  "upgradeSlots": 3,
  "energyCapacity": 20000,
  "energyTransferRate": 500,
  "energyConsumption": 180
}
```

That is all. Block, item, block entity, menu and screen are built from it, and its model,
translations, loot table and tags are generated **in memory** at resource load — no assets to
draw.

You will want a recipe, which is an ordinary datapack recipe like any other.

## Retuning a shipped inserter

`data/mypack/factor_io/inserters/fast_inserter.json`:

```json
{
  "ticksPerSwing": 3,
  "energyConsumption": 40
}
```

`/reload` applies it, to inserters already placed in the world. Fields you leave out are
untouched. Naming an inserter that does not exist logs a warning telling you to declare it in
`config/` instead.

## Retuning the modules

`data/mypack/factor_io/upgrades/tuning.json` — one file, and only that name:

```json
{
  "speedFactor": 0.75,
  "efficiencyFactor": 0.75,
  "capacityBonus": 1,
  "maxLevel": 6,
  "requiresModule": ["advanced_redstone"]
}
```

| Field | Meaning | Default |
|---|---|---|
| `speedFactor` | swing duration multiplied by this, per Speed tier — in ]0, 1] | 0.75 |
| `efficiencyFactor` | cost per swing multiplied by this, per Efficiency tier — in ]0, 1] | 0.75 |
| `capacityBonus` | items added per swing, per Productivity tier | 1 |
| `maxLevel` | ceiling on the summed tiers of one axis | 6 |
| `requiresModule` | abilities that need a module to work; `[]` makes them free for everyone | `["advanced_redstone"]` |

Every field is optional. `/reload` applies it to modules already installed, and removing the
datapack restores the shipped values. A factor of `2` is **refused**, not clamped: it multiplies a
duration, so "twice as fast" is `0.5`.

The **number of upgrade slots** is not here: it sets the size of the machine's inventory, so it
lives in the inserter definition (`upgradeSlots`) and takes effect on the next launch.

## Validation refuses, it does not guess

A malformed file is **rejected and named in the log**, never silently coerced to a default. The
combinations that are contradictory are caught explicitly:

- `fuelCapacity` or `fuelConsumption` on an electric inserter (`useEnergy: true`);
- `energyCapacity`, `energyTransferRate` or `energyConsumption` on a burner;
- both `ticksPerSwing` and the obsolete `cooldownBetweenActions`.

Each of these produces a message naming the offending key and what to use instead.

## Tags, rather than lists

Nothing the mod recognises is hardcoded:

| Tag | Contains |
|---|---|
| `factor_io:configurators` | items that copy and paste inserter settings |
| `factor_io:upgrades/speed/<1-3>` | modules acting as a Speed module of that tier |
| `factor_io:upgrades/capacity/<1-3>` | likewise for Productivity modules |
| `factor_io:upgrades/advanced_redstone` | items unlocking the full redstone condition |
| `factor_io:upgrades/efficiency/<1-3>` | likewise for cost |
| `factor_io:inserter_fuel` | what a burner will accept |
| `forge:tools/wrench` | what rotates a block |

Adding another mod's wrench to `forge:tools/wrench` makes it rotate inserters. No Java, and the
two mods never need to know about each other.

## Belts

### Fields

| Field | Type | Default | Meaning |
|---|---|---|---|
| `ticksPerSlot` | int, 1 to 200 | 4 | ticks for an item to advance one slot; **the only field describing speed** |
| `ramp` | bool | true | whether this belt gets a ramp variant |
| `next` | resource location | — | the tier above, for in-place upgrade and JEI |
| `texture` | resource location | `<namespace>:block/<name>` | texture of the belt surface |
| `translations` | map | — | display names per language code |

Rate is derived: `items/s = 2 lanes × 20 / ticksPerSlot`, so 4 gives 10 items/s, 2 gives 20, 1
gives 40 — the fastest Minecraft allows, one slot per tick.

### Creating a belt

`config/factor_io/belts/turbo_belt.json`:

```json
{
  "ticksPerSlot": 1,
  "next": "factor_io:express_transport_belt",
  "texture": "mypack:block/belts/turbo",
  "translations": { "en_us": "Turbo Belt", "fr_fr": "Convoyeur turbo" }
}
```

Its block, item, blockstate and models (the shipped belt shapes with your texture), loot table,
tags and names are generated in memory. Provide `assets/mypack/textures/block/belts/turbo.png` in a
resource pack, and a recipe in a datapack like any other item.

A file named after a shipped belt (`transport_belt.json`) **replaces** it, keeping its shipped
assets.

### Retuning a belt

`data/mypack/factor_io/belts/transport_belt.json`:

```json
{ "ticksPerSlot": 3 }
```

`/reload` applies it to belts already placed, and sends it to connected players, whose clients
replay the belt simulation. Only the speed is taken from a datapack; appearance, ramp and next
tier are fixed at launch.

## Crafters

Crafters follow the same four places as inserters and belts: a file in
`config/factor_io/crafters/` creates one, a datapack file in `data/<namespace>/factor_io/crafters/`
retunes it on `/reload`, the TOML removes one.

### Fields

| Field | Type | Default | Meaning |
|---|---|---|---|
| `tier` | int, 1 to 16 | 1 | recipes with a higher `minTier` are refused; **fixed at launch** |
| `moduleSlots` | int, 0 to 4 | 0 | module slots; **fixed at launch** |
| `craftingSpeed` | number > 0, up to 100 | — | a recipe takes `time ÷ craftingSpeed` |
| `energyPerTick` | int ≥ 0 | — | FE drawn per tick, only while crafting |
| `energyCapacity` | int > 0 | — | buffer |
| `inputCrafts` | int, 1 to 64 | 2 | how many crafts ahead the inputs accept |
| `fluidCapacity` | int, 1,000 to 64,000 | — | size of each fluid tank, in mB |
| `translations` | map | — | display names per language code |

A datapack may change everything but `tier` and `moduleSlots`; a file that contradicts either is
refused whole and named in the log.

### Creating a crafter

`config/factor_io/crafters/crafter_mk4.json`:

```json
{
  "tier": 4,
  "moduleSlots": 4,
  "craftingSpeed": 2.0,
  "energyPerTick": 300,
  "energyCapacity": 200000,
  "fluidCapacity": 64000,
  "translations": { "en_us": "Crafter Mk4", "fr_fr": "Crafter Mk4" }
}
```

## Crafting recipes

Crafter recipes are ordinary datapack recipes of type `factor_io:crafting`:

```json
{
  "type": "factor_io:crafting",
  "ingredients": [
    { "ingredient": { "tag": "forge:circuits/basic" }, "count": 20 },
    { "ingredient": { "tag": "forge:circuits/advanced" }, "count": 2 }
  ],
  "fluidIngredients": [
    { "fluid": "minecraft:water", "amount": 500 }
  ],
  "results": [
    { "item": "factor_io:processing_unit", "count": 1 },
    { "item": "minecraft:glowstone_dust", "count": 1, "chance": 0.1 }
  ],
  "time": 10.0,
  "minTier": 2
}
```

| Field | Bounds | Notes |
|---|---|---|
| `ingredients` | 1 to 9, `count` 1 to 64 | items or tags |
| `results` | 1 to 4, `count` 1 to 64 | `chance` optional, above 0 and up to 1 |
| `time` | > 0 | seconds, before the machine's speed |
| `minTier` | ≥ 1, default 1 | lowest crafter tier allowed |
| `fluidIngredients` | 0 to 2, `amount` 1 to 32,000 mB | exactly one of `fluid` or `tag` |
| `fluidResults` | 0 to 2 | `fluid` and `amount` |

Any other key starting with `fluid` is **refused**: a typo must not silently give a recipe without
its fluid. The machine reserves room for every possible result before it starts, so a lucky roll
is never lost.

Removing a recipe on `/reload` stops the machines that use it without destroying anything; they
restart by themselves if it comes back.
