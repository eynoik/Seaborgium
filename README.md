# Create Logistics Overrequest Fix

Small dedicated-server-only NeoForge mod for Minecraft 1.21.1 / Create 6.0.10.

It backports the dedicated-server-relevant parts of Create PR #10496, which addresses Factory Gauge / Packager over-requesting (issues #9987 and #10486).

The relevant `PackagerBlockEntity#getAvailableItems()` and `InventorySummary#add(ItemStack, int)` code in the official Create 6.0.10 tag is identical to the code targeted by the upstream fix.

## What it fixes

1. **Transient missing Packager target inventory**
   - Create can temporarily receive `null` from the Packager target inventory during block-entity updates.
   - Vanilla Create 6.0.10 replaces its cached inventory summary with an empty summary in that situation.
   - Factory Gauge can then think stock disappeared and issue another production request.
   - This mod keeps the last valid cached summary while the target inventory is temporarily unavailable.

2. **Mutable ItemStack keys in InventorySummary**
   - Create can keep a live container `ItemStack` as a `BigItemStack` key.
   - Later mutation of that same stack can invalidate the inventory summary and incorrectly affect promises.
   - This mod always copies the stack key with count 1, matching the upstream PR.

The third change in upstream PR #10496 guards client-side arrival submission. This mod is dedicated-server-only, so that client/integrated-server-only part is intentionally not included.

## Installation

Put the built JAR in the **server** `mods` folder only.

Requirements:
- Minecraft 1.21.1
- NeoForge 21.1.x
- Create 6.0.10

Clients do not need this mod.

## Scope

This mod does not change Factory Gauge targets, recipes, Mixer speed, package sizes, redstone logic, or request intervals.
