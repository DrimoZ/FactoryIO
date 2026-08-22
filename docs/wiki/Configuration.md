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

## Belt speed

```toml
[factor_io.TRANSPORT_BELTS.transport_belt]
    ticks_per_slot = 4
[factor_io.TRANSPORT_BELTS.fast_transport_belt]
    ticks_per_slot = 2
[factor_io.TRANSPORT_BELTS.express_transport_belt]
    ticks_per_slot = 1
```

Ticks for an item to advance **one slot**. Four slots per lane, so a block takes four times this.
Lower is faster, and `1` is the fastest Minecraft allows — a belt cannot move more than one slot
per tick without giving up smooth interpolation.

Changes apply to belts **already placed**, not only to new ones.

> These keys were once called `duration` and held values in a unit that no longer exists. If you
> have an old config, the old keys are inert and can be deleted.

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
