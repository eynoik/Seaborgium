# EpicFightTwilightApotheosisFix

External compatibility patch for Minecraft 1.21.1 NeoForge.

This project **does not replace, redistribute, or modify** TwilightForestEFCompat, Epic Fight, P1nero Epic Fight Bow, or Apotheosis. It is a separate runtime patch loaded alongside the original mods.

## 0.1.3

0.1.3 keeps every 0.1.2 fix and adds a targeted performance cache for:

`com.edwar.twilightmortisbows.TwilightWeaponAttributes#getDisplayedAttributeValue(ItemStack, Holder, double)`

The server Spark profile showed this path repeatedly expanding into `ItemStack.forEachModifier`, NeoForge attribute modifiers, and Apotheosis affix modifiers during sword combat. The same displayed-attribute path is also used by sword tooltip/stat rendering.

The cache:
- preserves the original result and only skips repeated recalculation,
- keys by item, item components, queried attribute and base value,
- normalizes **durability damage only**, so merely losing durability does not throw away an otherwise identical calculation,
- still invalidates naturally when affixes, enchantments or other stack components change,
- is bounded to 512 LRU entries.

No damage formula, affix, Epic Fight capability, or Twilight weapon behavior is disabled.

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
- Apotheosis 8.7.0 (optional but the performance regression is most visible with it)

## Test

Install 0.1.3 on both client and server in place of **this patch's** 0.1.2 JAR. Keep the original gameplay mods installed.

1. Hover the same affected sword in inventory and compare FPS.
2. Hit a mob 20-30 times with the same sword.
3. Record Spark MSPT/TPS while attacking.
4. Verify the sword's displayed attack value and actual damage still react to affix/enchantment changes.
