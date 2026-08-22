# Datapack Guide

Two separate mechanisms, easy to confuse:

| Where | What it does |
|---|---|
| `config/factory_io/inserters/<name>.json` | **creates** an inserter — read once at startup |
| `data/<namespace>/factory_io/inserters/<name>.json` | **retunes** an existing one — applied on `/reload` |

A datapack tunes; it cannot create, delete or rename. The list of inserters is a config
question, because which blocks exist has to be known before registration, long before any
datapack is read.

## The name is the file name

`dense_inserter.json` declares `factory_io:dense_inserter`. There is no `name` field.

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

`config/factory_io/inserters/dense_inserter.json`:

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

`data/mypack/factory_io/inserters/fast_inserter.json`:

```json
{
  "ticksPerSwing": 3,
  "energyConsumption": 40
}
```

`/reload` applies it, to inserters already placed in the world. Fields you leave out are
untouched. Naming an inserter that does not exist logs a warning telling you to declare it in
`config/` instead.

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
| `factory_io:configurators` | items that copy and paste inserter settings |
| `factory_io:upgrades/speed/<1-3>` | modules acting as a Speed module of that tier |
| `factory_io:upgrades/productivity/<1-3>` | likewise for capacity |
| `factory_io:upgrades/efficiency/<1-3>` | likewise for cost |
| `factory_io:inserter_fuel` | what a burner will accept |
| `factory_io:wrench` | what rotates a block |

Adding another mod's wrench to `factory_io:wrench` makes it rotate inserters. No Java, and the
two mods never need to know about each other.
