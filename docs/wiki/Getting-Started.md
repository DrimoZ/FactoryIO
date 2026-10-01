# Getting Started

From a few iron ingots to a line of crafters that feeds itself.

## 1. Plates and gears

Everything starts with **plates**: put an iron or copper ingot in a **stonecutter**. Two iron plates
make an **iron gear wheel**, one copper plate makes two **copper cables**, and an iron plate with
three cables makes an **electronic circuit**. All of it at the crafting table.

[Recipes](Recipes) has the whole chain.

## 2. Your first inserter

Craft a **Burner Inserter** — a gear and a plate. It needs no power, and refuels itself from
whatever it takes from: put coal in the source chest and it keeps running.

Place it between two containers. **It takes from the block behind it and puts into the block in
front**, the face it looks at. If it faces the wrong way, right-click it with a wrench, or sneak
and right-click bare-handed.

That is the whole setup. A burner moves about one item every two seconds.

## 3. Power

Every other machine runs on Forge Energy, and the mod **does not produce any**: bring a generator
from Mekanism, Thermal, or any mod that outputs FE. In creative, the **Creative Energy Source**
feeds whatever touches it.

A machine without power simply stops. It loses nothing, and resumes when power comes back.

## 4. Your first belt line

Place **Transport Belts** in a row, walking in the direction items should travel. Each belt carries
items the way you were facing when you placed it, and curves on its own when fed from the side.

Nothing feeds a belt by itself. Put an inserter beside it, taking from a chest: it drops onto the
lane furthest from it. At the other end, another inserter takes items off into a second chest.

To test without building anything, **right-click a belt holding an item**: it drops one on the
lane you clicked. Right-click empty-handed to take the front one back.

## 5. Your first crafter

The **Crafter Mk1** is a 3×3 machine, two blocks tall, placed in one go — an outline shows where.
Right-click it, click the recipe socket, pick **Iron Gear Wheel**.

Now run a belt of iron plates past it, and put an inserter between the belt and the crafter. The
inserter takes only what the crafter needs, and stops once it has two crafts ahead. A second
inserter on the other side takes the gears out.

![Inserters feeding crafters from a belt](images/crafter_feed.jpg)

From there: advanced circuits only come out of a crafter, processing units need a Mk2, and the Mk3
needs both. See [Crafters](Crafters).

## Reading a jam

A belt with nowhere to go fills up and stops, and the backup travels upstream — correct behaviour,
not a bug. Inserters in front of a full target wait with an empty hand. Give the line an outlet and
everything resumes.
