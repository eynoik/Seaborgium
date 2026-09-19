# Epic Fight Compatibility Fixes

## 0.2.3 — Iris shadow-map fast path

The target client Spark still showed a large remaining Epic Fight cost inside
`net.irisshaders.iris.shadows.ShadowRenderer.renderEntities`. Shader packs render living entities
again into a shadow map, causing Epic Fight's CPU `SkinnedMesh` path to be paid a second time.

0.2.3 keeps full Epic Fight rendering in the visible/main pass, but during Iris's shadow-map pass
returns `false` from `LivingEntityPatch#overrideRender()`. Epic Fight's own RenderEngine then does
not replace/cancel the vanilla LivingEntityRenderer, so vanilla geometry casts the shadow instead of
running the expensive Epic Fight skinned renderer again.

This is intentionally not a general LOD system. Near/far animation throttling and distance-based
renderer replacement are left for a later controlled test of Epic Fight FPS Optimizer.

0.2.3 also adds a stackless null-render-properties fallback for the optional
`Epic Fight - First Person Model` armor texture supplier, removing another exception-as-control-flow
path visible in the client profile.

# EpicFightTwilightApotheosisFix

External compatibility patch for Minecraft 1.21.1 NeoForge.

This project **does not replace, redistribute, or modify** TwilightForestEFCompat, Epic Fight, P1nero Epic Fight Bow, Apotheosis, MineColonies, or Epic Fight X MineColonies Compat. It is a separate runtime patch loaded alongside the original mods.

## 0.2.2 — client crash fix

0.2.2 fixes the `NoSuchMethodError` seen while rendering an Epic Fight entity with 0.2.0/0.2.2. The previous compile-only stub erased `LivingEntityPatch#getOriginal()` to an Object-returning JVM descriptor, while Epic Fight's real inherited method returns `Entity`. The civilian render gate now resolves that method reflectively with a cached lookup and fails open on mismatch.

The later Iris `GraphTranslucencyRenderOrderManager: Already in a group` error from the same run is consistent with the original render exception interrupting Iris between `startGroup()` and `endGroup()` while Not Enough Crashes attempted to continue the client.

## 0.2.2 — UI cleanup

0.2.2 additionally suppresses Epic Fight's client-side `VersionNotifier#render()`. Epic Fight 21.17.3.1 treats its four-part release string as a testing build and draws "Epic Fight is testing version." on screen. The patch only cancels that cosmetic renderer; the actual version string, networking and feature behavior remain untouched.

## 0.2.0 — MineColonies render correctness + performance

The 0.2.0 update is based on the target pack's client Spark profiles and exact runtime versions:

- Minecraft 1.21.1
- NeoForge 21.1.248
- Epic Fight 21.17.3.1
- MineColonies 1.1.1374-1.21.1-snapshot
- Epic Fight X MineColonies Compat 1.0.0

### 1. Stop rendering civilians as Epic Fight skinned mobs

Epic Fight X MineColonies patches citizens/visitors as Epic Fight living entities. In a dense colony this forces farmers, builders, couriers, visitors and other civilians through `PatchedLivingEntityRenderer -> SkinnedMesh.drawPosed` every frame.

0.2.0 intercepts Epic Fight's `LivingEntityPatch#overrideRender()` only for MineColonies citizens:

- MineColonies jobs with `IJob#isGuard() == true` keep Epic Fight rendering.
- Normal civilian jobs and visitors use the normal MineColonies renderer.
- Raiders and mercenaries are different entity types and are untouched, so they retain Epic Fight rendering/combat visuals.

This removes the expensive skinned-mesh path where it provides no gameplay value.

### 2. Fix cached headless/partial MineColonies meshes

The compatibility mod bakes shared MineColonies `HumanoidModel` objects into cached Epic Fight `SkinnedMesh` objects. Epic Fight's transformer only emits core parts that are visible when the bake occurs.

0.2.0 temporarily forces the seven vanilla humanoid core parts visible during the compatibility mod's bake, then restores the exact previous visibility flags. This prevents transient armor/layer state from permanently caching a mesh without a head, hat, torso or limb.

### 3. Restore omitted vanilla fallback layers

`DynamicMeshPatchRenderer` relies on Epic Fight's `initLayerLast()` to wrap original vanilla layers that it does not patch directly. The compatibility registration path constructs the renderer without invoking that initializer.

0.2.0 invokes it after the dynamic renderer's primary constructor, restoring fallback handling for layers such as MineColonies armor.

### 4. Remove the armor-texture exception hot path

Epic Fight 21.17.3.1 uses `ParseUtil.tryGetOr` for dynamically baked armor texture lookup. A null render-properties/custom-texture path falls back by throwing/catching a `NullPointerException`, which showed up in Spark as repeated `Throwable#fillInStackTrace` work on the Render Thread.

0.2.0 replaces only that null case with a stackless singleton exception. Epic Fight's existing fallback supplier still chooses the final armor texture, so behavior stays the same while the expensive stack trace allocation disappears.

## Retained 0.1.4 fixes

- One-client-tick Epic Fight tooltip mutation cache.
- Bounded Twilight displayed-attribute cache.
- Apotheosis/Twilight projectile recursion marker bridge.
- Ignore Apotheosis-generated arrows in Twilight bow combo processing.
- Disable P1nero automatic nearest-scanned-target fallback.
- Prevent competing P1nero bow yaw writes while keeping explicit Epic Fight lock-on.

## Test plan

Replace 0.1.4/0.2.0 with 0.2.2 on both client and server while keeping the original gameplay mods installed.

1. Enter the same MineColonies location used for the previous 120-second Spark profile.
2. Verify normal workers/visitors use their normal MineColonies models while guards/rangers, raiders and mercenaries still use Epic Fight.
3. Verify previously headless MineColonies guard models and armor layers.
4. Record the same 120-second client Spark: stationary camera first, then a separate profile while continuously rotating the camera.
5. Compare `RenderLivingEvent.Pre`, `PatchedLivingEntityRenderer.render`, `SkinnedMesh.drawPosed`, `getVertexPosition`, `getVertexNormal`, `WearableItemLayer` and `Throwable.fillInStackTrace` against the old profile.
