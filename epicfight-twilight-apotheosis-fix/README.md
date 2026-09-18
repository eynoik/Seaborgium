# EpicFightTwilightApotheosisFix

External compatibility patch for Minecraft 1.21.1 NeoForge.

This project **does not replace, redistribute, or modify** TwilightForestEFCompat, Epic Fight, P1nero Epic Fight Bow, or Apotheosis. It is a separate runtime patch loaded alongside the original mods.

## 0.1.4

0.1.4 keeps every 0.1.3 fix and adds a client-side one-tick cache around Epic Fight's tooltip listener:

`yesman.epicfight.client.events.engine.RenderEngine#epicfight$itemTooltip(ItemTooltipEvent)`

The client Spark profile showed JEI repeatedly rebuilding the same tooltip through:

`ItemStack#getTooltipLines -> ItemTooltipEvent -> Epic Fight -> LivingEntityPatch#getModifiedBaseDamage -> Twilight damage listeners`

The cache:
- stores the final Epic Fight-mutated tooltip for repeated requests inside the same client tick,
- keys by player, client tick, item/count/components and the incoming tooltip contents,
- never runs on the dedicated server because the mixin is client-only,
- has a maximum visual staleness of one client tick (~50 ms at 20 TPS),
- remains bounded to 256 entries.

This avoids running the full Epic Fight damage/listener path multiple times per tick while hovering the same JEI/inventory item.

## Retained 0.1.3 performance fix

A bounded LRU cache remains around:

`com.edwar.twilightmortisbows.TwilightWeaponAttributes#getDisplayedAttributeValue(ItemStack, Holder, double)`

It preserves the original result, normalizes durability-only changes and invalidates naturally when affixes, enchantments or other item components change.

## Retained 0.1.2 fixes

- Apotheosis/Twilight projectile recursion marker bridge.
- Ignore Apotheosis-generated arrows in Twilight `BowComboEvents#onArrowSpawn`.
- Disable P1nero automatic nearest-scanned-target fallback.
- Prevent competing P1nero yaw writes while keeping explicit Epic Fight lock-on behavior.

## Target pack

- Minecraft 1.21.1
- NeoForge 21.1.x
- Epic Fight 21.17.3.1
- TwilightForestEFCompat 1.1.6-Fix
- P1nero Epic Fight Bow 21.16.1.0
- Apotheosis 8.7.0

## Test

Install 0.1.4 on both client and server in place of **this patch's** 0.1.3 JAR. Keep all original gameplay mods installed.

1. Open JEI/inventory and hover the same affected weapon for 20-30 seconds.
2. Compare frametime/FPS and record a client Spark.
3. Verify attack damage/speed text still updates after changing the item, affix/enchantment or equipment.
4. Re-test normal combat to confirm the 0.1.3 server-side TPS fix remains intact.
