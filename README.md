# Factory'I/O

Factorio's inserters and transport belts, in Minecraft. Items move between the inventories you
already have — vanilla or from any other mod — without a hopper in sight.

**Forge 1.20.1** · Java 17 · MIT

**[Download the beta](https://github.com/DrimoZ/FactoryIO/releases)**

📖 **[The wiki](https://github.com/DrimoZ/FactoryIO/wiki) is the reference** — player guide,
config, datapack docs and FAQ. This README is the short version.

> **Beta.** Inserters and belts are complete and tested. **Machines are not implemented**, and
> that is a deliberate pause rather than an oversight: this release is a transport toolkit, not
> a production chain. What ships works; what is missing is missing on purpose.

---

## What it does

### Inserters

An inserter takes items out of the block behind it and puts them into the block in front. That
is the whole idea, and everything else is a variation on how fast, how far, and how selectively.

| Inserter | Items/s | Reach | Hand | Filters | Upgrade slots | Power |
|---|---|---|---|---|---|---|
| Burner | 0.59 | 1 | 1 | — | 1 | fuel |
| Inserter | 0.83 | 1 | 1 | — | 2 | 8 FE/t |
| Long Handed | 1.25 | 2 | 1 | — | 2 | 10 FE/t |
| Filter | 0.83 | 1 | 1 | yes | 2 | 10 FE/t |
| Fast | 2.50 | 1 | 1 | — | 3 | 25 FE/t |
| Stack | 7.50 | 1 | 3 | — | 4 | 35 FE/t |
| Stack Filter | 7.50 | 1 | 3 | yes | 4 | 40 FE/t |

Throughput is within 8 % of Factorio's at 20 tps. A single field describes speed — the duration
of one arm swing — and rate, energy cost and animation are all derived from it, so two numbers
can never disagree about how fast a machine is.

Filters hold up to five ghost items, as a whitelist or a blacklist, and each slot matches
**either that exact item or its whole tag** — your choice, per slot. `#forge:ingots` in one slot
and a single plank in the next is a normal configuration.

Redstone is a **comparison, not a switch**: always on, on below a signal strength, or on at or
above it. A lever and a comparator do different things to the same inserter.

### Transport belts

| Belt | Items/s | Ticks per slot | Carries |
|---|---|---|---|
| Transport Belt | 10 | 4 | 8 items |
| Fast Transport Belt | 20 | 2 | 8 items |
| Express Transport Belt | 40 | 1 | 8 items |

Two lanes of four slots each, as in Factorio. Belts curve, merge from the side, compress against
an obstacle, and a **closed loop keeps turning when full** — which sounds obvious and is the
single hardest thing about the design.

**An inserter drops on the far lane**, the one furthest from it. That is the rule every two-lane
build in Factorio rests on, and it is enforced by the belt rather than by the inserter: belts
hand out their slots in far-first order based on which side you are asking from. Hoppers and
pipes from other mods get the same behaviour without knowing belts exist.

Belts expose an inventory on **every face**. A hopper above loads one, a hopper below drains one,
and it drains from the **front** — a belt is a queue, so what went on first comes off first.

A belt does not push into chests on its own, and does not pull from them. That is what inserters
are for.

### Right-click, and what it does

| Gesture | Effect |
|---|---|
| Right-click an inserter | opens the menu: filters, redstone condition, gauge |
| Right-click a belt with an item | drops one item on the lane and slot you clicked |
| Right-click a belt empty-handed | takes the front-most item back |
| Wrench, or shift + right-click bare-handed | rotates an inserter |
| Right-click with a **configurator** | applies the settings it remembers |
| Shift + right-click with a **configurator** | memorises this inserter's settings |
| Right-click with a **module** | installs an upgrade, returning the one it replaces |

Placing a block on top of a belt still works — sneak, as vanilla already expects.

### Upgrades

Three independent axes, three tiers each, and they **stack**: two Speed Module 3 are worth more
than one.

| Module | Axis | Per tier | Pays |
|---|---|---|---|
| Speed 1-3 | speed | −25 % swing duration | same cost per swing, so more energy per second |
| Productivity 1-3 | capacity | +1 item per swing | — |
| Efficiency 1-3 | efficiency | −25 % cost per swing | — |

How many you may install is a property of the machine, from one on a burner to four on a stack
filter. Breaking the block returns them.

> Modules and the configurator have **no recipes yet** and are creative-only. That is the main
> known gap in this beta.

---

## Configuration

`config/factory_io/factory_io-common.toml`:

- which of the seven inserters exist at all;
- belt speed, in ticks per slot, per tier;
- `insert_on_far_lane_only` — strict Factorio parity, where an inserter *waits* rather than
  falling back to the near lane. Off by default, because an inserter stalled in front of a
  half-empty belt reads as a fault to anyone who does not know Factorio.

Belt speed changes apply to belts **already placed**, not only to new ones.

---

## Data-driven

An inserter is a **data file, not a Java class**. Dropping a JSON into
`config/factory_io/inserters/` gives you a new inserter: block, item, block entity, menu and
screen are built from the definition, and its models, translations, loot table and tags are
generated in memory at resource load. No assets to draw, no code to write.

A **datapack** retunes the shipped inserters live — speed, reach, hand size, costs — through
`data/<namespace>/factory_io/inserters/<name>.json` and a `/reload`. Validation is strict and
refuses rather than coercing: a bad field names itself in the log instead of silently becoming a
default.

Everything the mod recognises goes through **item tags**, never a hardcoded list:
`factory_io:configurators` and `factory_io:upgrades/<axis>/<tier>`. Another mod's wrench or
another pack's component becomes usable by joining the tag — no Java, and the two mods never
need to know about each other.

**[Datapack guide →](https://github.com/DrimoZ/FactoryIO/wiki/Datapack-Guide)**

---

## Energy

The mod consumes Forge Energy and does not produce any. The **Creative Energy Source** feeds
whatever touches its six faces, and is deliberately recipe-less and creative-only: giving it a
recipe would erase energy progression before the mod has decided whether it generates its own
power or leans on Mekanism and Thermal. Until then, bring a generator.

Burner inserters need no power at all and refuel themselves from the chest they draw from.

---

## Compatibility

Anything exposing an `IItemHandler` works with both inserters and belts, which covers essentially
every storage mod on Forge 1.20.1. Vanilla hoppers work in both directions.

**JEI** is not integrated yet — the API is a compile-time dependency and no plugin is written.

---

## Permissions

**Modpacks: yes.** No permission needed, no message required, public or private, monetised or
not, on any platform or launcher. If you are reading this to find out whether you may include
Factory'I/O, the answer is yes and you can stop reading.

**Credit** is appreciated and never required.

**Forks and addons: yes**, under the MIT terms. Please do not publish a fork under the name
*Factory'I/O* — the name is not covered by the licence, and two mods sharing one name only
confuses players trying to work out which one broke their world.

**Contributions** are accepted under the same terms as the rest of the repository.

---

## Building

Requires **JDK 17**. Gradle 8.8 tolerates newer JDKs, but the project toolchain is 17 — if
anything looks odd, point `JAVA_HOME` at a 17.

```bash
./gradlew build
```

The jar lands in `build/libs/factory_io-1.20.1-0.3.0-beta.jar`.

```bash
./gradlew runClient
```

```bash
./gradlew runGameTestServer
```

`build` runs the JUnit suite; world-level tests run separately with the command above. There are
**41 GameTests, 3 benchmarks and roughly 110 JUnit cases**. Rendering is the one thing no
assertion reaches — it is checked by eye.

```bash
./gradlew runData
```

Regenerates the versioned assets under `src/generated/resources`. Commit what it produces.

### Layout

- `core/belts` and `core/inserters` — the mechanics. `BeltLane`, `BeltTransport`, `BeltShape`
  and `BeltPath` have **no dependency on Minecraft at all**, which is what let the tick cost be
  measured before a block existed, and what keeps them unit-testable.
- `core/registry`, `core/model` — the data-driven inserter pipeline.
- `client/` — everything `Dist.CLIENT`. Nothing under `shared/` may import
  `net.minecraft.client`; a dedicated review agent watches for it, because a dedicated server is
  the only thing that ever notices.
- `gametest/` — world-level tests and benchmarks.

### Documentation

Eleven documents under [`docs/`](docs/), in French, are the source of truth for design decisions
— including the ones that were wrong and why. [`03-BUGS.md`](docs/03-BUGS.md) catalogues fifty
fixed defects with their cause; [`10-BENCHMARKS.md`](docs/10-BENCHMARKS.md) records what was
measured and where an earlier measurement was off by two orders of magnitude.

---

## Licence

MIT — see [`LICENSE`](LICENSE).
