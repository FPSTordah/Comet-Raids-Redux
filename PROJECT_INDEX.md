# Project Index - Comet-Raids

Generated on 2026-02-20 (local workspace scan).

## 1) Snapshot

- Project type: Hytale server mod + asset pack
- Plugin entrypoint: `com.cometmod.CometModPlugin` (`manifest.json`)
- Main config: `comet_config.json`
- Fixed spawns config: `fixed_spawns.json`
- Top-level file counts:
  - `src`: 35 Java files
  - `Server`: 176 data/content files
  - `Common`: 124 asset files
- File types (excluding `.git`):
  - `121` `.particlespawner`
  - `106` `.png`
  - `42` `.json`
  - `35` `.java`
  - `17` `.blockymodel`
  - `15` `.particlesystem`
  - `2` `.lang`

## 2) Repository Layout

- `src/com/cometmod`: runtime plugin code (commands, systems, managers)
- `src/com/cometmod/config`: config model + parser/writer + defaults
- `Server/`: gameplay definitions used by the mod (items, projectiles, models, particles, interactions, localization)
- `Common/`: shared visual/audio assets (icons, textures, blockymodels)
- `manifest.json`: Hytale plugin metadata and main class
- `comet_config.json`: spawn/tier/theme/reward settings
- `fixed_spawns.json`: fixed or scheduled comet spawn points

## 3) Runtime Map

### Plugin bootstrap

- `src/com/cometmod/CometModPlugin.java`
  - Registers interaction codecs:
    - `Comet_Stone_Uncommon_Activate`
    - `Comet_Stone_Rare_Activate`
    - `Comet_Stone_Epic_Activate`
    - `Comet_Stone_Legendary_Activate`
  - Registers command root: `/comet` via `CometCommand`
  - Registers ECS systems:
    - `CometDeathDetectionSystem`
    - `CometBlockBreakSystem`
    - `CometStatModifierSystem`
    - `CometDamageModifierSystem`
  - Schedules periodic checks:
    - wave timeout checks
    - falling projectile fallback checks
  - Handles entity removal and resolves projectile -> comet block placement

### Spawn and encounter flow

- `CometSpawnTask` handles periodic natural spawn attempts based on `spawnSettings`.
- `CometFallingSystem` spawns/tracks projectile comets and places comet blocks.
- `CometStoneActivateInteraction` starts encounter on interaction.
- `CometWaveManager` drives wave lifecycle, mob tracking, timeout handling, and reward completion.
- `CometDespawnTracker` persists and reschedules comet despawn behavior.
- `CometMarkerProvider` exposes map markers for active comets.

## 4) Java Code Index

### Commands

- Root command: `src/com/cometmod/CometCommand.java`
- Implemented command classes:
  - `CometSpawnCommand`
  - `CometTestCommand`
  - `CometZoneCommand`
  - `CometDestroyAllCommand`
  - `CometReloadCommand`
  - `SpawnCustomNPCCommand`

### Systems and managers

- `CometWaveManager` (largest core logic)
- `CometFallingSystem`
- `CometSpawnTask`
- `CometDespawnTracker`
- `CometBlockBreakSystem`
- `CometBlockEventSystem`
- `CometDamageModifierSystem`
- `CometDeathDetectionSystem`
- `CometStatModifierSystem`
- `CometMarkerProvider`

### Config model/parsing

- `CometConfig`
- `ThemeConfig`, `ThemeConfigParser`, `ThemeConfigWriter`
- `DefaultThemes`
- `TierSettings`, `TierRewards`, `ZoneSpawnChances`
- `MobEntry`, `BossEntry`, `WaveEntry`, `RewardEntry`, `StatMultiplierConfig`

## 5) Largest Java Files (cleanup impact)

- `src/com/cometmod/CometWaveManager.java` - 2740 LOC
- `src/com/cometmod/config/ThemeConfigParser.java` - 1002 LOC
- `src/com/cometmod/CometConfig.java` - 652 LOC
- `src/com/cometmod/WaveThemeProvider.java` - 564 LOC
- `src/com/cometmod/CometFallingSystem.java` - 547 LOC
- `src/com/cometmod/CometDespawnTracker.java` - 546 LOC
- `src/com/cometmod/config/ThemeConfig.java` - 505 LOC
- `src/com/cometmod/CometSpawnTask.java` - 433 LOC
- `src/com/cometmod/CometSpawnCommand.java` - 415 LOC

## 6) Config Index

- `comet_config.json` top-level keys:
  - `spawnSettings`
  - `zoneSpawnChances`
  - `tierSettings`
  - `rewardSettings`
  - `themes`
- Current counts:
  - zones: `5`
  - tiers (settings): `4`
  - tiers (rewards): `4`
  - themes: `18`
- `fixed_spawns.json`:
  - `spawns` currently empty (`0`)
  - includes `_comment`, `_options`, `_examples` helper blocks

## 7) Integrity Findings (important before cleanup)

### Missing source files referenced by code

`CometCommand` references command classes that are not present in `src/com/cometmod`:

- `CometSetSpawnCommand`
- `CometScheduleSpawnCommand`
- `CometRemoveSpawnCommand`
- `CometListSpawnsCommand`

`CometModPlugin` references manager class not present in `src/com/cometmod`:

- `FixedSpawnManager`

These unresolved references are likely compile blockers and should be addressed first.

### JSON conventions

- `comet_config.json` contains `_comment_multiwave` inside `themes` as a string pseudo-comment.
- `fixed_spawns.json` uses `_comment/_options/_examples` helper keys.
- This is valid JSON, but tooling that assumes uniform object types under a section (for example, all `themes.*` are objects) needs to special-case these keys.

## 8) Suggested Cleanup Order

1. Restore or remove missing fixed-spawn command/manager classes so project compiles cleanly.
2. Split `CometWaveManager` into focused services (theme selection, wave state, rewards, UI updates).
3. Replace manual JSON parsing in `CometConfig`/`ThemeConfigParser` with a typed JSON library path (or consolidate parser rules in one module).
4. Normalize config schema docs for pseudo-comment keys to avoid tooling breakage.
5. Add a lightweight validation pass for `comet_config.json` and `fixed_spawns.json` at startup with actionable error messages.

## 9) Fast Navigation

- Plugin entry: `src/com/cometmod/CometModPlugin.java`
- Command root: `src/com/cometmod/CometCommand.java`
- Spawn scheduling: `src/com/cometmod/CometSpawnTask.java`
- Falling comet logic: `src/com/cometmod/CometFallingSystem.java`
- Wave orchestration: `src/com/cometmod/CometWaveManager.java`
- Config loader: `src/com/cometmod/CometConfig.java`
- Theme parser: `src/com/cometmod/config/ThemeConfigParser.java`
- Manifest: `manifest.json`
- Main config: `comet_config.json`
- Fixed spawns: `fixed_spawns.json`
