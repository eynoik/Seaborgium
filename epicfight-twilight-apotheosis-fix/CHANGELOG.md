# Changelog

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
