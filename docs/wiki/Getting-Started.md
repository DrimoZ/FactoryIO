# Getting Started

## Your first inserter

Craft a **Burner Inserter**. It needs no power, and it refuels itself from whatever it is taking
from — put coal in the source chest and it keeps itself running.

Place it between two containers. **The face it looks at is where items go**; it takes from the
block directly behind. If it faces the wrong way, right-click it with a wrench, or sneak and
right-click bare-handed.

That is the whole setup. A burner moves about one item every two seconds.

## Going electric

Every other inserter needs Forge Energy. The mod **does not generate any** — bring a generator
from Mekanism, Thermal, or any mod that outputs FE. In creative, the **Creative Energy Source**
feeds anything touching its six faces.

An electric inserter with no power simply stops. It does not break, and it keeps whatever it was
holding.

## Your first belt line

Place **Transport Belts** in a row, walking along in the direction you want items to travel. Each
belt outputs in the direction you were facing when you placed it.

Nothing feeds a belt on its own. Put an inserter beside it, taking from a chest — it will drop
onto the belt's far lane. At the other end, another inserter takes items off and puts them into a
second chest.

To test without building anything, **right-click a belt while holding an item**: it drops one, on
the lane and slot you clicked. Right-click empty-handed to take the front-most one back.

## Reading a jam

A belt that has nowhere to go fills up and stops, and the backup travels upstream — that is
correct behaviour, not a bug. The inserter feeding it will stall too, arm extended, holding its
item. Give the line an outlet and everything resumes.

## What is not here

No furnaces, assemblers or ore processing. Inserters and belts move items between the inventories
you already have. See the [FAQ](FAQ).
