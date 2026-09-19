# Changelog

## 0.2.1

- Disabled Epic Fight's client-side `VersionNotifier#render` overlay.
- Removes the persistent/temporary "Epic Fight is testing version." and version text without spoofing the Epic Fight version or changing networking, feature flags, datapacks, animations or compatibility checks.
- Retains every 0.2.0 MineColonies render/performance fix and all earlier Twilight/Apotheosis/P1nero fixes.

# Changelog

## 0.2.0

- Added a client render gate for MineColonies citizens patched by Epic Fight X MineColonies Compat.
- Non-combat citizens/visitors now keep MineColonies' vanilla renderer; only jobs whose MineColonies `IJob#isGuard()` is true stay on the Epic Fight skinned renderer. Raiders and mercenaries are separate entity types and remain fully Epic Fight-rendered.
- Fixed dynamic MineColonies mesh baking so transient `HumanoidModel` visibility flags cannot permanently cache a headless/partial `SkinnedMesh`.
- Fixed Epic Fight X MineColonies Compat's omitted `PatchedLivingEntityRenderer#initLayerLast()` call, restoring fallback handling for unpatched vanilla layers such as armor.
- Replaced Epic Fight 21.17.3.1's repeated armor texture NullPointerException fallback with a stackless fast fallback while preserving the existing fallback texture lookup.
- Targeted from client Spark profiles where Epic Fight consumed ~36% inclusive Render Thread time in the colony and `SkinnedMesh.drawPosed/getVertexPosition/getVertexNormal` dominated the added work.
- Retains every 0.1.4 tooltip cache, Twilight displayed-attribute cache, projectile bridge, P1nero target and yaw fix.

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
