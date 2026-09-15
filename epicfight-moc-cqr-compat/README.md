# Epic Fight Mo' Creatures + CQR Compat

Standalone data-driven compatibility mod for Minecraft 1.21.1 / NeoForge / Epic Fight 21.17.3.x.

It adds Epic Fight weapon capability files for the real registered combat items in:

- Mo' Creatures: Aura Edition 1.21.1-1.0.10-nameplate-fix
- Chocolate Quest Repoured 3.0.0-neoforge-alpha.1

The mod does **not** replace either source mod's hit effects, right-click abilities, projectiles, poison/fire effects, healing, hookshots, etc. It only tells Epic Fight which weapon category/moveset to use. It intentionally adds no damage/impact/armor-negation overrides, so original item balance stays owned by the source mods.

## Mapping policy

### Mo' Creatures
- Ninja Katana -> `uchigatana`
- Ninja Sai + Scorpion Sting weapons -> `dagger`
- Ninja Bo -> `spear`
- axes -> `axe`
- swords -> `sword`
- Ninja Nunchaku -> `sword` fallback (Epic Fight has no native nunchaku category)
- Whip -> `sword` fallback (Epic Fight has no native whip category)

### Chocolate Quest Repoured
- daggers -> `dagger`
- great swords -> `greatsword`
- Bull Battle Axe -> `greatsword` (heavy two-handed fallback)
- spears / musket-bayonets / staffs -> `spear`
- Spiked Glove -> `fist`
- normal special swords -> `sword`
- revolvers, muskets, bubble guns, flamethrower and hookshot tools -> `crossbow` as the closest ranged Epic Fight stance
- real CQR shields -> `shield`

## Notes

Pure utility/legacy/internal items are deliberately not mapped (for example MoC portal staff, CQR dummy shield, CQR fake healing staff and creative super tool).

The custom CQR ranged items are not vanilla `CrossbowItem` subclasses. The `crossbow` capability gives them the closest available Epic Fight ranged presentation, while their actual firing behavior remains CQR's. If a specific right-click action is blocked in Battle Mode on the real pack, that should be fixed narrowly in a later version rather than by globally replacing CQR item logic.

## Installation

Install this JAR on both server and clients that use Epic Fight. Keep the original Mo' Creatures, CQR and Epic Fight JARs unchanged. This mod is separate from AsyncSableFix and Async Epic Fight Fix.

## Coverage

- Mo' Creatures mapped items: 24
- CQR mapped items: 63
- Total capability files: 87
