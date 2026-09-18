# Changelog

## 0.1.4

- Added a client-only one-tick cache around Epic Fight's `RenderEngine#epicfight$itemTooltip` mutation.
- The cache keys by player, client tick, item/count/components and the incoming tooltip content, then reuses the already-mutated Epic Fight tooltip for repeated JEI/inventory requests inside the same tick.
- This targets the client Spark path `ItemStack#getTooltipLines -> ItemTooltipEvent -> Epic Fight -> LivingEntityPatch#getModifiedBaseDamage -> event listeners`.
- Maximum staleness is one client tick; changes to the stack or incoming tooltip naturally produce a different key.
- Retains the 0.1.3 Twilight displayed-attribute cache and all 0.1.2 projectile/yaw/targeting fixes.

## 0.1.3

- Added a bounded displayed-attribute cache around TwilightForestEFCompat's `TwilightWeaponAttributes#getDisplayedAttributeValue`.
- Cache key preserves item components, queried attribute and base value while normalizing durability damage only.
- Targets the Spark hot path that repeatedly calls NeoForge/Apotheosis item attribute modifier enumeration during sword combat and tooltip rendering.
- Retains all 0.1.2 projectile recursion, P1nero auto-target and yaw compatibility fixes.

## 0.1.2

- Bridged TwilightForestEFCompat and Apotheosis projectile recursion markers.
- Prevented Twilight bow combo processing for Apotheosis-generated arrows.
- Disabled P1nero automatic nearest-scanned-target fallback.
- Prevented competing P1nero yaw writes during bow attacks while preserving explicit Epic Fight lock-on.
