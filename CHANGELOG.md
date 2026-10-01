# Changelog

All notable changes to Factor'I/O. Versions follow `MAJOR.MINOR.PATCH`, and the jar is named
`factor_io-<minecraft>-<version>.jar`.

## [Unreleased]

### Added

- **Crafters**, Factorio's assembling machines: three tiers (Mk1, Mk2, Mk3) of a 3×3×2 machine
  placed in one go, with an outline showing where it will stand. Pick a recipe from the icon
  selector — tiers above the machine are greyed out — and feed it with inserters from any
  side. Inputs only accept the recipe's ingredients, two crafts ahead, so an inserter never
  empties a belt into a single machine. Runs on FE, only while working. The model is a
  placeholder.
- **`factor_io:crafting` recipes** for datapacks: counted ingredients, up to four results with
  an optional chance, a crafting time and a minimum crafter tier. Crafting table recipes can be
  allowed in crafters too, per world (`vanillaRecipes` in `factor_io-server.toml`, off by
  default).
- **A new inserter screen.** It resizes to the model and shows the blocks an inserter takes from
  and drops into. Everything that is not the machine's core job moved to side tabs: Information
  and Settings on the left, Upgrades and Control on the right.
- **On/off switch** on every inserter, in the Control tab. Off wins over any redstone signal.
- **Hand size cap**, per inserter: take fewer items per swing than the machine could.
- **Drop lane choice**, per inserter: automatic, near or far, overriding
  `insert_on_far_lane_only` for that inserter.
- **Measured throughput** in the Information tab, next to the expected one, with the current
  state and the cost of a swing.
- The configurator also copies hand size and drop lane.
- JEI no longer draws over the screen's tabs.
- **Belt ramps**, one per belt tier, to climb or descend one block. A ramp placed below the end of
  a belt goes down, otherwise it goes up. Crafted from the belt and an iron plate. The models are
  placeholders.
- **Recipes for the modules and the configurator**, through the Factorio component chain: plates
  (stonecutter), steel (blast furnace), the new **iron gear wheel** and **copper cable**, then
  electronic, advanced circuits and processing units. Ingredients are Forge tags, circuits
  included (`forge:circuits/basic|advanced|elite`).
- **New item textures** in the Minecraft style for plates, circuits, modules and the configurator.
- **Transport belts can be crafted.** Inserters and belts now follow Factorio's recipes — gears,
  plates and circuits — instead of hoppers, comparators and redstone blocks.
- The Information tab shows the **energy draw per tick** of an electric inserter.

### Removed

- Science packs, rocket parts, fuels and uranium are **hidden from the creative tab**: they have no
  use until the machines arrive. They stay registered.

### Changed

- Tooltips are translated as whole sentences; nothing is glued together from fragments anymore.
- An inserter's item tooltip no longer ends with the long upgrade hint: the screen's Upgrades tab
  says it.
- Moving belts resend their state every 10 s, so a line watched in multiplayer no longer drifts
  away from the server's. About 3.3 KB/s per player for 500 moving items; idle and stopped belts
  send nothing.

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
