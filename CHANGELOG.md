# Changelog

All notable changes to Factor'I/O. Versions follow `MAJOR.MINOR.PATCH`, and the jar is named
`factor_io-<minecraft>-<version>.jar`.

## [Unreleased]

### Added

- **A new inserter screen.** It resizes to the model, and everything that is not the machine's
  core job moved to side tabs: Information on the left, Upgrades, Redstone control and Settings
  on the right.
- **On/off button** on every inserter. Off wins over any redstone signal.
- **Hand size cap**, per inserter: take fewer items per swing than the machine could.
- **Drop lane choice**, per inserter: automatic, near or far, overriding
  `insert_on_far_lane_only` for that inserter.
- **Measured throughput** in the Information tab, next to the expected one, with the current
  state and the cost of a swing.
- The configurator also copies hand size and drop lane.
- JEI no longer draws over the screen's tabs.

### Changed

- Tooltips are translated as whole sentences; nothing is glued together from fragments anymore.

## [0.3.0-beta] — 2026-08-16

First public build. Inserters and transport belts are complete; machines are not.

### Added

- **Transport belts**, three tiers — 10, 20 and 40 items/s. Two lanes of four slots, curves,
  side merges, compression against an obstacle, and closed loops that keep turning when full.
- **Inserters drop on the far lane**, as in Factorio. The belt decides, from the face the
  request arrives on, so hoppers and other mods' pipes follow the same rule without knowing
  belts exist.
- Belts expose an **`IItemHandler` on every face**. A hopper above loads, a hopper below
  drains — from the front, because a belt is a queue.
- **Place and take items by hand**: right-click a belt with an item to drop one on the lane and
  slot you clicked, empty-handed to take the front one back.
- **Belt speed is configurable**, in ticks per slot, and applies to belts already placed.
- `insert_on_far_lane_only`, off by default: strict Factorio parity, where an inserter waits
  rather than falling back to the near lane.
- **Upgrade modules stack**: two Speed Module 3 are worth more than one, up to a per-machine
  slot count from one to four.
- **Configurator** item: copy an inserter's settings and paste them onto another.
- **Creative Energy Source**, recipe-less and creative-only.
- Crafting recipes for all seven inserters, as a chain from the burner.

### Fixed

Fifty defects catalogued in [`docs/03-BUGS.md`](docs/03-BUGS.md), with their cause. The ones a
player would have noticed:

- A saturated belt loop stopped for good instead of turning (BUG-050).
- An inserter opened a new stack instead of topping up a partial one, scattering a single item
  type across a whole chest (BUG-049).
- An item could cross an entire belt line in one tick when the line had been placed in the
  direction of travel.
- Belts pointing into an unloaded chunk loaded it, every tick, in a cascade.
- Two belts facing each other passed items through one another.

### Known gaps

- **No machines.** Deferred deliberately; see [`docs/05-ROADMAP.md`](docs/05-ROADMAP.md).
- **Modules and the configurator have no recipes** and are creative-only.
- No vertical belts or splitters — the code allows for the first, the models do not exist.
- No JEI plugin.
- Client and server run the belt simulation independently and reconcile only on events; a line
  watched for a long time may drift by one slot.
