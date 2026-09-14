# Changelog

## 0.1.5

- Fixed Async/Sable server hang in `Level#getChunkForCollisions` when a collision lookup crosses into a chunk with no visible `ChunkHolder`.
- Replaced `ChunkSource#getChunkNow()` with a strict loaded-only lookup matching Async's `async$tryGetChunk` fast path.
- Explicitly returns no collision chunk when the holder/chunk is absent instead of entering Async's blocking `ServerChunkCache#getChunk` handoff.
- Preserved every 0.1.4 protection unchanged.
