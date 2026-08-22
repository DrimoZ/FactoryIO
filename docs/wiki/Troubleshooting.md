# Troubleshooting

## The mod does not load

Check the three versions match: **Minecraft 1.20.1**, **Forge 47+**, and **GeckoLib 4.4 or
newer**. GeckoLib is a hard dependency and the mod will not start without it.

The jar goes in `mods/`, not in a subfolder.

## It crashes on startup

Read the top of `logs/latest.log` — Forge names the mod responsible. If it names this one,
open an [issue](https://github.com/DrimoZ/FactoryIO/issues) with that log attached. The log is
worth more than a description of what you saw.

## My settings in the config file do nothing

Two possibilities.

**Which inserters exist** is read *before* Forge loads the config file — block registration
happens earlier — so a change takes effect on the **next** launch, not this one. On the very
first launch the file does not exist at all, and defaults apply.

**Belt speeds** apply immediately, including to belts already placed. If they do not, check you
edited `ticks_per_slot` and not the old `duration` key, which is inert.

## Items are not moving on a belt

- Does anything feed it? Belts do not pull from chests. Put an inserter beside it, or test by
  right-clicking the belt with an item in hand.
- Is the line blocked? A belt with nowhere to go fills up and stops, and the backup travels
  upstream. That is intended.
- Are the belts facing the way you think? Each outputs in the direction you faced when placing
  it, and only accepts input from behind and the sides.

## An inserter is not picking anything up

- **No power.** Everything but the burner needs Forge Energy, and the mod generates none.
- **Facing the wrong way.** It takes from behind and gives in front. Rotate with a wrench, or
  sneak and right-click bare-handed.
- **A filter is excluding it.** Check whitelist versus blacklist, and whether a slot is in tag
  mode.
- **Redstone.** The default condition stops it on *any* signal.
- **Its target is full.** It holds the item, arm extended, and resumes when there is room.
- **It is looking at something it cannot read.** Inserters use block inventories; items on the
  ground, minecarts and entities are ignored.

## Lag

Belts and inserters both sleep when idle, and the server cost has been measured — roughly 1.4 ms
per thousand belts saturated with items, against a 3 ms budget for two thousand.

If you are seeing lag with this mod loaded, please open an issue with a
[Spark](https://spark.lucko.me/) profile. A profile identifies the cause; an impression does not.

## My world lost all its Factor'I/O blocks

The mod identifier changed from `factory_io` to `factor_io` in `0.3.0-beta`, to match the mod's
actual name. Blocks and items placed by an earlier version no longer resolve.

There is no migration. The earlier versions were pre-alpha and announced as unstable; the rename
was made at the first beta precisely so that it would never have to happen again.
