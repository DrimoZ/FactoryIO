# FAQ

### What machines are there?

Inserters, transport belts and [crafters](Crafters) — Factorio's assembling machine, in three
tiers. Furnaces, drills and a survival power source are planned, not built: smelting stays with
vanilla furnaces, and plates come from the stonecutter.

### Does it need another mod?

**GeckoLib**, always. For power in survival, yes: the mod consumes Forge Energy and produces none,
so bring a generator from Mekanism, Thermal, or anything producing FE. **Burner inserters need
nothing** — a whole line can run on coal. In creative, the Creative Energy Source covers it.

**JEI** is optional and recommended: it shows crafter recipes, and its `+` button sets a crafter's
recipe.

### Some items have no recipe and are not in the creative tab.

Science packs, rocket parts, fuels and uranium are registered for the machines to come, and have
no use yet. They are hidden rather than removed, so a world that holds some loses nothing. Every
item the mod uses has a recipe: see [Recipes](Recipes).

### My inserter is waiting with an empty hand.

Its target is full, or does not want what is on offer. Inserters only pick up what the target
will accept — the Factorio behaviour — so nothing is ever stuck in a hand. It resumes the moment
there is room.

### My crafter does not take more than a few items.

It accepts at most two crafts' worth of each ingredient, so an inserter cannot empty a belt into
it. See [Crafters](Crafters#feeding-it).

### My belt stopped and everything backed up.

The line has nowhere to go. Backups travel upstream one slot at a time, which is intended — give
the end an outlet and it resumes. A **full closed loop keeps turning**; if yours does not, that is
a bug worth reporting.

### Can a belt fill a chest on its own?

No, and it will not empty one either. That is what inserters are for, and it is Factorio's rule.
A hopper, however, can load or drain a belt from any face.

### Which lane does an inserter use?

The one **furthest** from it, then the near one once that is full — unless you set
`insert_on_far_lane_only`, or choose a lane in the inserter's **Settings** tab. See
[Transport Belts](Transport-Belts#the-far-lane).

### Are there splitters? Underground belts?

Not yet. Belts go up and down one block with [ramps](Transport-Belts#ramps). Splitters are next on
the list; underground belts are out of scope.

### Does it work on a dedicated server?

Yes, and that path is specifically guarded: client-only code is kept out of common packages,
because a dedicated server is the only place a leak shows.

### Will my world survive an update?

Within `0.x`, no promises: this is a beta. Back up before updating.

### Can I use it in a modpack?

Yes. No permission needed, public or private, monetised or not. Credit appreciated, never
required.
