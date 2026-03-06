# Comet_Raids_Redux

Source code: `https://github.com/FPSTordah/Comet-Raids-Redux`
Support Discord: `https://discord.gg/r5MBWdzWWW`

Ever wanted random events to spice up your Hytale gameplay? This mod adds falling comets that crash into your world, bringing waves of enemies to fight. Break the comet block (or press F on coffin/portal) to start the encounter - survive all waves and claim your rewards.

`Comet_Raids_Redux` is the actively maintained continuation of the original Comet Raids project.

Current release: **v4.0.0**

This mod is built for players who want a raid-like experience and server owners who want a customizable reward system. You can create custom themes, define multi-wave encounters, override loot tables per theme, and tweak every aspect of spawning and combat.

## Features

- **5 Comet Tiers** - Uncommon, Rare, Epic, Legendary, and Mythic. Higher tiers = tougher fights, better loot.
- **Themed Waves** - Skeletons, goblins, spiders, trorks, outlanders, undead hordes... each comet picks a random theme (or you can force one).
- **Multi-Wave Combat** - Enemies spawn in waves. Clear one, the next begins. Rewards drop after the final wave.
- **Timed Reward Chest** - Final rewards spawn in a per-comet chest instance. 
- **Map Markers** - Comets show up on your map so you can track them down.
- **Zone + Tier Loot Model** - Rewards combine zone identity pools, current tier pools, and lower-tier inheritance weights.
- **Fully Configurable** - Spawn rates, enemy counts, loot tables, zone distributions, and scaling multipliers are all configurable.

## Reward Chest Behavior

When a comet encounter is cleared, the reward chest system works like this:

- **Comet stone blocks** (`Comet_Stone_*`): the block is replaced by the reward chest on the same spot.
- **Other spawn blocks** (coffin, portal, custom): the block stays; the chest is placed in front of it. When the chest expires or is broken, the mod removes the linked block too.
- Each comet creates its own chest instance with loot generated for that comet's tier/theme.

## Quick Start

### Requirements

- Endgame & QoL mod (`Config:Endgame&QoL`) for Tier 5/Mythic content (optional)

Tier 5/Mythic is the Endgame integration tier (Prisma/Onyxium and related Endgame materials), so it requires Endgame & QoL to be active.
If Endgame & QoL is missing, the mod still loads and Tier 5/Mythic is automatically disabled.

### Permissions (LuckPerms)

- `/comet spawn` -> `hytale.command.comet.spawn`
- `/comet test` -> `hytale.command.comet.test`
- `/comet zone` -> `hytale.command.comet.zone`
- `/comet destroyall` -> `hytale.command.comet.destroyall`
- `/comet reload` -> `hytale.command.comet.reload`
- `/comet setspawn` -> `hytale.command.comet.setspawn`
- `/comet schedulespawn` -> `hytale.command.comet.schedulespawn`
- `/comet removespawn` -> `hytale.command.comet.removespawn`
- `/comet listspawns` -> `hytale.command.comet.listspawns`

Recommended admin setup in LuckPerms:
- Grant all comet command nodes above to your admin group, or grant wildcard `hytale.command.comet.*` if your permissions setup supports wildcards.

### Deploy

Place `Comet_Raids_Redux-4.0.0.jar` in your server mods/plugins location, replacing the old jar, then restart the server.

### IDE

Open/import as a Maven project, then Build/Rebuild in your IDE.

## Comet Ownership

By default, each comet is "owned" by the player it spawned for. Only that player can see the map marker and trigger the encounter by breaking the comet block. Other players can't interact with it.

If you want **any player** to be able to trigger **any comet** (useful for multiplayer servers), set `"globalComets": true` in the config. When enabled:
- All players see all comet markers on the map
- Any player can break and trigger any comet

## When Do Comets Spawn?

Comets spawn naturally based on these default settings:
- **Spawn interval**: Every 2-5 minutes (120-300 seconds)
- **Spawn chance**: 40% chance each time the interval triggers
- **Spawn distance**: 30-50 blocks away from a player
- **Despawn**: Unclaimed uncommon comets despawn after 30 minutes

The tier of comet that spawns depends on `zoneSpawnChances` (`0..3` keys in config), including Mythic probabilities.

## Commands

### Main Commands

| Command | Description |
|---------|-------------|
| `/comet spawn` | Spawns an Uncommon comet near you |
| `/comet spawn --tier Rare` | Spawns a specific tier (Uncommon, Rare, Epic, Legendary, Mythic) |
| `/comet spawn --theme Skeleton` | Spawns with a specific theme |
| `/comet spawn --tier Legendary --theme Void` | Combine tier and theme |
| `/comet spawn --onme true` | Spawns comet directly above you (for testing) |
| `/comet test` | Simulates automatic zone-based comet spawn for your location |
| `/comet zone` | Shows your current zone and comet tier distribution |
| `/comet destroyall` | Removes all active comet blocks in the world |
| `/comet reload` | Reloads the config from file |

### Fixed Spawn Point Commands

Fixed spawn *points* are loaded from `fixed_spawns.json` and can be listed; the runtime that would *auto-trigger* comets at those points on a cooldown or schedule is **not implemented** in this snapshot (intentionally left out), so comets are not spawned at fixed locations by the mod. You can still use the file for your own reference or future re-enable.

- `/comet listspawns` — lists configured points from `fixed_spawns.json` (read-only)
- `/comet setspawn`, `/comet schedulespawn`, `/comet removespawn` — placeholders; edit `fixed_spawns.json` manually.

| Command | Description |
|---------|-------------|
| `/comet listspawns` | List all configured fixed spawn points |
| `/comet setspawn <cooldown>` | Placeholder: edit `fixed_spawns.json` manually |
| `/comet schedulespawn <times>` | Placeholder: edit `fixed_spawns.json` manually |
| `/comet removespawn` | Placeholder: edit `fixed_spawns.json` manually |
| `/comet removespawn --target <name or index>` | Placeholder: edit `fixed_spawns.json` manually |

**Fixed-spawn schema keys (for manual JSON editing):**
`name`, `enabled`, `cooldownSeconds`, `scheduledTimes`, `tier`, `theme`, `despawnMinutes`, `notifyRadius`, `notifyTitle`, `notifySubtitle`

### Spawn Command Examples

```
/comet spawn
/comet spawn --tier Legendary
/comet spawn --tier Epic --theme Trork
/comet spawn --theme Undead
/comet spawn --tier Rare --theme Spider
/comet spawn --onme true --tier Legendary
```

### More Fixed Spawn Examples

These are still valid as JSON examples, but command-based editing is currently disabled:

```json
{
  "spawns": [
    {
      "x": 100,
      "y": 64,
      "z": 200,
      "name": "Town Square",
      "enabled": true,
      "cooldownSeconds": 300,
      "tier": "Epic",
      "theme": "Trork Warband"
    }
  ]
}
```

### Available Themes

> Tier 1 = Uncommon, Tier 2 = Rare, Tier 3 = Epic, Tier 4 = Legendary, Tier 5 = Mythic

- `Skeleton` - Skeleton Horde (Tier 1-2)
- `Goblin` - Goblin Gang (Tier 1-2)
- `Spider` - Spider Swarm (Tier 1-2)
- `Trork` - Trork Warband (Tier 1-3)
- `Skeleton_Sand` - Sand Skeleton Legion (Tier 1-3)
- `Sabertooth` - Sabertooth Pack (Tier 1-3)
- `Void` - Voidspawn (Tier 1-3)
- `Outlander` - Outlander Cult (Tier 2-5)
- `Leopard` - Snow Leopard Pride (Tier 2-5)
- `Skeleton_Burnt` - Burnt Legion (Tier 3-5)
- `Ice` - Legendary Ice (Tier 3-5)
- `Lava` - Legendary Lava (Tier 3-5)
- `Earth` - Legendary Earth (Tier 3-5)
- `Undead` / `Undead_Legendary` - Undead variants (Tier 1-5 depending on theme)
- `Zombie` - Zombie Aberration (Tier 3-5)
- `Frostbound_Pack` - Frostbound Pack (Tier 2-5)
- `Ashen_Vanguard` - Ashen Vanguard (Tier 3-5)
- `Dune_Stalkers` - Dune Stalkers (Tier 2-4)
- `Void_Reavers` - Void Reavers (Tier 2-4)
- `Plague_Horde` - Plague Horde (Tier 2-5)
- `Trork_Siege` - Trork Siege (Tier 2-4)
- `Ember_Hunters` - Ember Hunters (Tier 3-5)
- `Crystal_Marauders` - Crystal Marauders (Tier 3-5)
- `Wraithborn_Legion` - Wraithborn Legion (Tier 3-5)
- `Predator_Clan` - Predator Clan (Tier 1-4)

## Configuration

Settings are split across three files (all generated on first run if missing):

- `config.json` - main configuration: spawn behavior, zone chances, tier settings, reward tables, inheritance, WorldProtect rules, messages
- `themes.json` - theme definitions (monster groups, optional **spawnBlock** and per-theme reward overrides): which mobs/bosses and waves per theme
- `fixed_spawns.json` - fixed spawn points

### Message placeholders

Chat and banner messages in `config.json` under `"messages"` support placeholders. Full list and message keys are in the in-game Voile doc (Comet Raids Redux guide). Quick reference:

| Placeholder | Description |
|-------------|-------------|
| `%tier%` | Comet tier (Uncommon, Rare, Mythic, etc.) |
| `%x%`, `%y%`, `%z%` | Comet block coordinates |
| `%currentWave%`, `%totalWaves%` | Wave progress |
| `%theme%` | Theme display name |
| `%bossStatus%` | Alive / Defeated |
| `%killed%`, `%total%` | Mobs killed / total this wave |
| `%time%` | Remaining time |

Message keys: `msgCometFallingTitle`, `msgCometFallingSubtitle`, `msgCometFallingChatCoords`, `msgWaveBossTitle`, `msgWaveBossTitleNoCount`, `msgWaveBossSubtitle`, `msgWaveTitle`, `msgWaveTitleNoCount`, `msgWaveSubtitle`, `msgWaveFailedTitle`, `msgWaveFailedSubtitle`, `msgWaveCompleteTitle`, `msgWaveCompleteSubtitle`, `msgWaveCompleteChatHeaderPrefix`, `msgWaveCompleteChatHeader`, `msgWaveCompleteChatItemPrefix`.

## Packaging As One JAR

This project is now Maven-based and builds a single distributable JAR directly.

To build:

```bash
mvn clean package
```

This creates:

- `target/Comet_Raids_Redux-4.0.0.jar`

The output JAR includes:

- `Common/`
- `Server/`
- `manifest.json`

Config files (`config.json`, `themes.json`, `fixed_spawns.json`) are generated in the plugin directory on first run if missing.

In IntelliJ, open/import the project as a Maven project and use Build/Rebuild.

### Fixed Spawn Points

Fixed spawn points are stored in `fixed_spawns.json`. The mod loads and lists them, but **does not run a scheduler**: no comets are auto-spawned at these locations. Edit the file manually and use `/comet listspawns` to verify. Command-based editing (`setspawn` / `schedulespawn` / `removespawn`) is not implemented.

The schema supports (for future or external use):
- **Cooldown mode**: `cooldownSeconds` — would spawn every X seconds
- **Scheduled mode**: `scheduledTimes` — would spawn at specific real-world times (24-hour)

The file format is documented for compatibility and possible future re-enable of a scheduler.

```json
{
  "spawns": [
    {
      "x": 100,
      "y": 64,
      "z": 200,
      "name": "Town Square",
      "enabled": true,
      "cooldownSeconds": 300,
      "tier": "Epic",
      "theme": "Trork Warband",
      "despawnMinutes": 15.0
    },
    {
      "x": -50,
      "y": 70,
      "z": 150,
      "name": "Evening Raid",
      "enabled": true,
      "scheduledTimes": ["18:00", "06:00", "12:00"],
      "tier": "Legendary",
      "notifyRadius": "global",
      "notifyTitle": "The Arena Awakens!",
      "notifySubtitle": "A legendary challenge awaits..."
    }
  ]
}
```

**Options:**
- `name` - Custom name to identify this spawn point (optional, used for removal by name)
- `enabled` - true/false to enable or disable this spawn point
- `cooldownSeconds` - Seconds between spawns (used if scheduledTimes is empty)
- `scheduledTimes` - Array of real-world times like `["18:00", "06:00"]` - spawns at these times daily
- `tier` - Uncommon, Rare, Epic, Legendary, or Mythic (optional, random if not set)
- `theme` - Theme name like "Skeleton Horde" (optional, random if not set)
- `despawnMinutes` - Custom despawn time for this spawn point (optional, uses global if not set)
- `notifyRadius` - Notification radius: omit = 100 blocks (default), `"none"` = no notification, `"global"` = all players, number = custom radius in blocks
- `notifyTitle` - Custom notification title (optional, default: "Tier Comet Falling!")
- `notifySubtitle` - Custom notification subtitle (optional, default: "Watch the sky!")

### Main Sections

**spawnSettings** - Controls natural comet spawning
```json
"spawnSettings": {
  "naturalSpawnsEnabled": true, // Set to false to disable random spawns (comets only via commands)
  "minDelaySeconds": 120,      // Minimum time between spawn attempts
  "maxDelaySeconds": 300,      // Maximum time between spawn attempts
  "spawnChance": 0.4,          // 40% chance to spawn when timer triggers
  "despawnTimeMinutes": 30.0,  // How long uncommon comets last before despawning
  "minSpawnDistance": 30,      // Minimum blocks from player
  "maxSpawnDistance": 50,      // Maximum blocks from player
  "disabledWorlds": [],        // World names where comet raids are disabled (case-insensitive)
  "globalComets": false,       // If true, any player can trigger any comet
  "disableWaveMobLoot": true  // If true, mobs spawned in waves drop no loot (default true)
}
```

> **Tip:** To disable random spawns, set `"naturalSpawnsEnabled": false`; use `/comet spawn` to spawn comets manually. Fixed-spawn auto-trigger is not implemented.

**claimProtect** - Generic claim-plugin spawn blocking
```json
"claimProtect": {
  "enabled": true,                     // If true, block natural comet spawns inside configured claim systems
  "autoDetectProviders": false,        // If true, auto-detect installed supported providers and ignore the providers list
  "providers": ["WorldProtect", "HyperFactions", "Hyfaction", "SimpleClaims", "WiFlowsClaims", "UltimateFaction", "ElbaphFactions"] // Case-insensitive provider names
}
```

Supported providers right now:
- `HyperFactions`
- `WorldProtect`
- `Hyfaction`
- `SimpleClaims`
- `WiFlowsClaims`
- `UltimateFaction`
- `ElbaphFactions`

**zoneSpawnChances** - Tier distribution per zone
```json
"zoneSpawnChances": {
  "0": { "tier1": 0.8, "tier2": 0.1, "tier3": 0.07, "tier4": 0.03, "tier5": 0.0 },
  "1": { "tier1": 0.6, "tier2": 0.2, "tier3": 0.13, "tier4": 0.07, "tier5": 0.0 }
}
```

**tierSettings** - Per-tier combat settings
```json
"tierSettings": {
  "1": {
    "timeoutSeconds": 90,    // How long before wave times out
    "minRadius": 3.0,        // Min spawn radius for enemies
    "maxRadius": 5.0         // Max spawn radius for enemies
  }
}
```

**rewardSettings** - Loot drops per tier
```json
"rewardSettings": {
  "1": {
    "drops": [
      {
        "id": "Ingredient_Bar_Copper",
        "minCount": 5,
        "maxCount": 7,
        "chance": 100,
        "displayName": "Copper Ingots"
      }
    ]
  }
}
```

**zoneBaseLootPools** - Base material identity per zone

**tierInheritanceWeights** - Lower-tier inclusion chances per active tier

Theme and multiplier config: `tierStatScaling`, **spawnBlock** (which block to place), and per-theme reward overrides all live in `themes.json`. For custom blocks (coffin/portal or your own), see the in-game Voile doc (Comet Raids Redux guide).

```json
"tierStatScaling": {
  "enabled": true,
  "percentPerTier": 5.0,
  "zonePercentPerLevel": 2.0,
  "applyHp": true,
  "applyDamage": true,
  "applySpeed": true,
  "applyScale": false
}
```

### Startup Validation

On load, the mod runs lightweight validation for:

- `config.json`
- `fixed_spawns.json`

Validation emits actionable logs with this prefix:

- `[ConfigValidation][config.json]`
- `[ConfigValidation][fixed_spawns.json]`

Errors do not hard-stop startup, but indicate config issues you should fix.

### Boot-Time JSON Sync

On startup, the mod now automatically:
- Creates missing `config.json`, `themes.json`, and `fixed_spawns.json`
- Merges/synchronizes missing config keys into existing files using current schema defaults

This keeps older config files forward-compatible after updates without manual key-by-key edits.

### Schema Notes (Tooling-Safe)

Some config objects intentionally include pseudo-comment keys for human guidance.
These keys are supported by the mod parser and should be treated as metadata by tools:

- Any key starting with `_` is metadata/comment-only (for example `_comment_multiwave`).
- Under `themes`, non-theme entries should use `_` prefix so validators and editors can ignore them.
- Theme definitions should remain object values (`"theme_id": { ... }`), while pseudo-comments can be strings.

### Theme Configuration

Theme definitions (mobs, bosses, waves) go in `themes.json`. You can set **spawnBlock** per theme to the block type ID to place instead of the tier comet stone (e.g. `Comet_Furniture_Village_Coffin`, `Comet_VoidInvasion_Portal`, or your custom block). Omit or leave blank to use the default comet stone. Per-theme reward overrides are optional per-theme entries in the same `themes.json` (e.g. a **rewardOverride** object keyed by tier number).

Use `"naturalSpawn": false` on themes in `themes.json` to prevent them from spawning naturally.

### Creating Custom Themes

Want to make your own encounters? Edit `themes.json` (monster groups, optional **spawnBlock** and **rewardOverride** per theme). You can:

- Define multiple waves with different enemy compositions
- Configure boss waves separately from normal waves
- Override the default loot table with custom rewards per tier
- Use `"naturalSpawn": false` to prevent a theme from spawning naturally while you test it

Mob/boss stat scaling is global now via `tierStatScaling` (tier and zone based), rather than per-mob inline stat blocks.

The config is fully JSON - just copy an existing theme, rename it, and start tweaking.

> **Tip:** To restrict a theme to command-spawn or future fixed-spawn use only (e.g. boss arenas), set `"naturalSpawn": false` on that theme so it never appears in random natural spawns.

## Version Notes

- `v4.0.0`: Custom spawn blocks (coffin/portal or any block via `spawnBlock` in themes); reward chest in front of non–comet-stone assets, asset removed when chest expires; configurable wave mob loot (`disableWaveMobLoot`); beam raised above portal; chest opens only when clicking the chest block; full docs and JSON examples for custom assets.
- `v3.3.0`: Added configurable comet chat and banner messages with placeholders, plus an in-game Voile documentation page for configuration help.
- `v3.2.4`: Generic claim protection integration.
- `v3.2.3`: Improved wave timer reliability, fixed stuck wave progression in edge cases, and improved kill-count tracking compatibility with mob/combat overhaul mods.
- `manifest.json` controls runtime compatibility (`ServerVersion`).
- `pom.xml` controls compile dependency (`hytale.server.version`).
- Keep them aligned when updating to a new server release.

## Attribution

Special thanks to Frog for the original Comet Raids groundwork and for allowing continued development with free rein.

- Original repository: https://github.com/FrogCsLoL/Comet-Raids

## Usage & Distribution

This project is being maintained under MIT permission from the original owner.

## Credits

Original mod created by **Frog**.

Current continuation and maintenance: **Tordah**.

Ty to Pferd for balancing this Mod.


Some parts of this mod were made with AI assistance - mainly coding help and upscaling some visual assets like the mod icon.

---

Have fun getting obliterated by Legendary comets.
