# Async Sable Fix 0.1.6.1

Compatibility mod for Minecraft 1.21.1 / NeoForge 21.1.x, Async 0.2.0 alpha and Sable 2.0.5.

## 0.1.6.1

Startup-safe rebuild of 0.1.6. The 0.1.6 binary was malformed: the two newly rebuilt mixin classes stored class-level `@Mixin` metadata with the wrong retention, so Mixin rejected them during PREPARE before the server could start.

0.1.6.1 keeps the exact 0.1.6 collision/cache behavior:

- no per-block `Level#isLoaded` calls in Sable `LevelAccelerator` collision scans;
- loaded-only, per-chunk cached guards including negative results;
- no blocking chunk-load fallback from Async collision workers;
- hard 1024 integer-block scan cap plus the earlier 4096.0 volume guard;
- Create contraptions, MCA villagers and MineColonies citizens remain forced to synchronous ticking.

The main Seaborgium branch remains untouched. Work lives on the dedicated Async Sable Fix branches under `async-sable-fix/`.
