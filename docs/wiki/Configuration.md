# Configuration

Everything lives in `config/factor_io/factor_io-common.toml`, written by Forge on first launch.

> On the **very first** launch the file does not exist yet, so defaults apply and your settings
> take effect from the second launch onwards. This is a Forge ordering constraint: which
> inserters exist has to be known before the file is read.

## Which inserters exist

```toml
[factor_io.Inserters]
    burner_inserter = true
    inserter = true
    long_handed_inserter = true
    filter_inserter = true
    fast_inserter = true
    stack_inserter = true
    stack_filter_inserter = true
```

Setting one to `false` removes it entirely — no block, no item, no recipe. Useful for a pack that
wants a shorter progression.

## Which belts exist

```toml
[factor_io.TRANSPORT_BELTS]
    transport_belt = true
    fast_transport_belt = true
    express_transport_belt = true
```

Same as inserters: `false` removes the belt entirely, effective on the next launch.

## Which crafters exist

```toml
[factor_io.CRAFTERS]
    crafter_mk1 = true
    crafter_mk2 = true
    crafter_mk3 = true
```

Same rule: `false` removes the tier entirely, on the next launch.

## Crafters

The one per-world option lives in `<world>/serverconfig/factor_io-server.toml`, and the server
sends it to every player:

```toml
[crafter]
    vanillaRecipes = false
    vanillaTime = 0.5
```

`vanillaRecipes = true` lets crafters run crafting table recipes, shaped and shapeless — special
ones such as fireworks, dyes or map copies excepted, since their result depends on the input's
data. They appear after the mod's own recipes in the picker. `vanillaTime` is their crafting
time in seconds, before the machine's speed (0.05 to 60).

Off by default, so the picker is not buried under a thousand recipes.

## Belt speed

Belt speed is **no longer in this file**: it is set by datapack, and applies on `/reload` to belts
already placed. See [Datapack Guide](Datapack-Guide#belts). The old `ticks_per_slot` keys are inert
and can be deleted.

## Far lane

```toml
[factor_io.TRANSPORT_BELTS]
    insert_on_far_lane_only = false
```

`true` is strict Factorio parity: an inserter only ever fills the lane furthest from it, and
**waits** when that lane is full instead of using the near one.

Off by default because an inserter stalled in front of a visibly half-empty belt reads as a fault
to anyone who does not know Factorio. The two behaviours are indistinguishable until the far lane
saturates.

The restriction covers **dropping only**. Taking from the near lane is always allowed — Factorio
forbids putting there, not taking.
