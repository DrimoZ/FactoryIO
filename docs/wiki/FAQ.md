# FAQ

### Where are the machines?

There are none, and that is deliberate. This build is a transport toolkit: inserters and belts
move items between the inventories you already have. Furnaces, assemblers and ore processing were
specified and then **postponed** rather than half-built.

### Does it need another mod?

For energy, in survival: yes. The mod consumes Forge Energy and generates none, so bring a
generator from Mekanism, Thermal, or anything producing FE. **Burner inserters need nothing** —
a whole line can run on coal.

In creative, the Creative Energy Source covers it.

### Some items have no recipe and are not in the creative tab.

Science packs, rocket parts, fuels and uranium are registered for the machines to come, and have
no use yet. They are hidden rather than removed, so a world that holds some loses nothing. Every
item the mod actually uses — plates, gears, cables, circuits, modules, configurator — has a recipe:
see [Upgrades](Upgrades).

### My inserter stopped with an item in its hand.

Its target is full, or unpowered. It holds the item, arm extended, and resumes the moment there is
room. Nothing is lost.

### My belt stopped and everything backed up.

The line has nowhere to go. Backups travel upstream one slot at a time, which is the intended
behaviour — give the end an outlet and it resumes. A **full closed loop keeps turning**; if yours
does not, that is a bug worth reporting.

### Can a belt fill a chest on its own?

No, and it will not empty one either. That is what inserters are for, and it is Factorio's rule.
A hopper, however, can load or drain a belt from any face — belts expose an inventory, so vanilla
and other mods' pipes work.

### Which lane does an inserter use?

The one **furthest** from it. Once that lane is full it falls back to the near one, unless you
set `insert_on_far_lane_only`. See [Transport Belts](Transport-Belts).

### Are there vertical belts or splitters?

No. The code anticipates vertical belts — a belt already carries its flow direction as a property
— but no models exist for either.

### Does it work on a dedicated server?

Yes, and that path is specifically guarded: client-only code is kept out of common packages, with
a review pass dedicated to it, because a dedicated server is the only thing that notices a leak.

### Will my world survive an update?

Within `0.x`, no promises. This is a beta and the belt data format may still change. Back up.

### Can I use it in a modpack?

Yes. No permission needed, public or private, monetised or not. Credit appreciated, never
required.
