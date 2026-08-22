# Filters and Redstone

## Filters

The Filter and Stack Filter inserters carry **five filter slots**. Drop an item onto one to set
it — the item is a ghost, it is not consumed and you get it straight back.

A button switches the whole panel between **whitelist** — only these pass — and **blacklist** —
these do not.

### Item or tag, per slot

Each slot matches **either that exact item, or its entire tag**. You choose, slot by slot; a
slot in tag mode is tinted so the two are never confused.

`#forge:ingots` in one slot passes every ingot in the pack, including ones added later by another
mod. A plain oak plank in the next slot passes oak planks and nothing else. Mixing the two in the
same filter is normal.

Tag mode is what keeps a filter working after you add a mod. An item list would not.

## Redstone

Redstone is a **comparison, not a switch**. Three conditions:

| Condition | Runs when |
|---|---|
| Always | the signal is ignored entirely |
| Signal below N | strength is under N — the default, with N = 1, so any signal stops it |
| Signal at or above N | strength is N or more |

The default reproduces the intuitive "redstone turns it off". The other two are what a comparator
is for: an inserter that only runs when a chest is nearly empty, or only when it is nearly full,
needs no additional circuit.

A disabled inserter changes texture, so a stopped line is readable at a glance.

## Copying settings

The **Configurator** copies everything above between machines.

- **Sneak + right-click** an inserter to memorise its filters, whitelist mode, redstone condition
  and animation setting.
- **Right-click** another to apply them.

It works across inserter types, applying whatever the target can hold: settings copied from a
filter inserter onto one without filters simply drop the filter part.

The tool is recognised by the `factory_io:configurators` **tag**, so a pack can make another
mod's tool do the same job by adding it to the tag.

> The configurator has **no recipe yet** and is creative-only in this beta.
