[English] | [Русский](READMEru.md)

# AFF (Akfz Firearms Framework)
AFF is a high-performance firearms framework built around a native C++ bullet physics core and a flexible configuration system. While the backend and physics engine are technically complete, the visual rendering and 3D models are left entirely to the user.

*(Note: macOS is not currently supported, as I do not have a Mac to compile the native binaries).*

## Features
* **Native Physics Core (C++ / JNI)**: High-performance bullet flight simulation featuring sub-stepping, AABB raycasting, gravity, air resistance, and wind. Runs in a dedicated thread pool (`BulletManager`) to avoid blocking the main server tick.
    * **Automatic Fallback**: If the native library is unavailable (e.g., `NativeBallisticsLoader` fails to find a match for the current platform), the engine seamlessly falls back to a simpler, pure-Java implementation (`JavaBulletPhysics`) using DDA raycasting.
* **Advanced Ballistics**:
    * Calculates impact energy (`E = ½mv²`) and determines penetration based on block thickness and angle of incidence.
    * **Ricochets** with configurable probability based on angle, material hardness, and velocity.
    * **Through-penetration** (over-penetration) of up to 16 blocks per shot, while tracking individual hit coordinates.
    * Distinction between **entity** and **block** hits, with accurate result reporting to the Java layer (`HitResult`).
* **Material System**: Each block is mapped to a material via `materials.json` (defining density, hardness, `maxPenetrationJoules`, and flags like `breakAble` / `dropOnBreak`). Uses `ResourceLocation` for mapping and caches data based on `BlockState`.
* **Dynamic Wind and Environment** (`EnvironmentSystem`): Directional wind (km/h) per dimension, an 8-block raycast to detect enclosed spaces, a `fluidDragMult` multiplier (e.g., x15 in water), and a per-chunk cache with a 5-second TTL.
* **Multithreaded Physics**: `BulletManager` runs a loop within a `ScheduledExecutorService` (25 ms), while calculations occur in a fixed-thread `physicsExecutor`. Results are safely synchronized back to the main server thread.
* **Flexible Weapon System**:
    * `Gun` interface + `DefaultGun` implementation + `InfiniteGun` for Creative mode.
    * **Five firing modes**: `SINGLE`, `BURST`, `AUTO`, `BOLT`, with proper handling of `triggerReleased`, recoil, and inter-shot delays.
    * **Three feeding systems**: `DirectFeedingSystem` (internal magazine and chamber), `MagazineFeedingSystem` (detachable magazines with NBT serialization), `InfiniteFeedingSystem` (infinite ammo).
    * State serialization to NBT: chambered ammo type, magazine contents, firing mode, `lastShotTime`, `needsChambering`.
* **Customizable Keybinding System** (`BindManager`):
    * Five types: `CLICK`, `RELEASE`, `HOLD`, `DOUBLE_CLICK`, `TOGGLE`.
    * **Key-based grouping** with conflict resolution (e.g., the 'R' key simultaneously mapped to: reload on release, unload on double-click, check status on hold).
    * Mouse wheel support; configurable `doubleClickWindow` and `holdActivationTime` thresholds.
    * Serialization to `binds.json` with automatic default creation.
* **Event Model** (`AsLib.EVENT_BUS`): `BulletSpawnEvent`, `BulletHitEvent`, `RecoilReceiverEvent`, `PressedBindEvent`, `PreLoadRegistryEvent` — all cancellable, all with mutable context (`BulletHitContext` provides access to velocity, angle, and energy, and allows damage overrides).
* **Network Layer**: `BindsSyncPacket` (syncs high-frequency bind events with server-side validation), `SyncRecoil` (client-side recoil).
* **Registries**: `AmmoRegistry`, `MagazineRegistry`, `GunRegistry` — registration of ammo, magazines, and weapon configs with duplicate protection.
* **Debugging**: `BulletDebug` renders trails, hitboxes, and velocity/wind vectors using particles. The `BulletManager.DEBUG` flag enables detailed logging of the native and Java cores, including result buffer dumps.