--- 
name: Comet Raids Redux
description: Configurable comet raid messages, waves and rewards.
author: Tordah
---

# Comet Raids Redux – In‑Game Guide

Comet Raids Redux adds **configurable comet attacks** with multi‑wave encounters, bosses and rewards.
This guide explains:

- How comets work in general.
- How to use the main commands.
- What the most important config options do (spawns, zones, themes, rewards, protections).
- How to customise all chat and banner messages.

---

## 1. How Comet Raids Work

- A **comet** is an event that targets a location near a player.
- When it lands, it starts a **raid** made of one or more **waves** of mobs.
- Waves can be **normal** or **boss** waves, depending on the theme.
- When all waves are defeated before the timer expires, the comet **drops loot** and disappears.
- If the timer expires, the wave **fails** and the comet is destroyed with no rewards.

Comets can spawn **naturally over time** or be **forced** with commands.

---

## 2. Basic Commands

> Exact permissions may depend on your permission plugin; this is a functional overview.

- **/comet spawn [--tier &lt;tier&gt;] [--theme &lt;theme&gt;] [--onme true]**
  - Spawns a comet near you.
  - **--tier** – Uncommon, Rare, Epic, Legendary, or Mythic (if Endgame&QoL is enabled). Default: Uncommon.
  - **--theme** – Theme ID (e.g. Skeleton, Void, Trork) to force a theme; otherwise one is chosen by zone and tier.
  - **--onme true** – Spawn the comet directly above you (for testing).

- **/comet test**
  - Runs a quick spawn test for your current zone so you can verify config and spawns.

- **/comet zone**
  - Shows your current zone and the tier distribution (chances per tier) for comet spawns in that zone.

- **/comet reload**
  - Reloads **config.json**, **themes.json**, and **fixed_spawns.json**.
  - Use this after changing config files.

- **Fixed spawn commands**
  - `/comet listspawns` – list all configured fixed spawn points (read-only; points are loaded from `fixed_spawns.json`).
  - `/comet setspawn`, `/comet removespawn`, `/comet schedulespawn` – placeholders in this snapshot; edit `fixed_spawns.json` manually. The mod does not run a scheduler, so comets are not auto-spawned at fixed locations.

- **Admin tools**
  - `/comet destroyall` – find and destroy all comet blocks in the world.

Your server’s documentation or permissions setup may give more detailed permission nodes for each sub‑command.

---

## 3. Config Overview

Comet Raids Redux uses three config files (generated on first run if missing):

- **config.json** – main settings: spawn behavior, zones, tier/reward tables, messages.
- **themes.json** – theme definitions (monster groups): which mobs/bosses and waves per theme. Optional **spawnBlock** per theme: set to a block type ID to place that block; omit or leave blank to use the tier comet stone (e.g. `Comet_Stone_Uncommon`). Optional **rewardOverride** per theme for custom loot per tier. The default themes set **spawnBlock** for skeleton/ghoul/undead themes to a comet coffin and for void themes to **Comet_VoidInvasion_Portal**; you can change or remove these in themes.json.
- **fixed_spawns.json** – fixed spawn points (loaded and listed; auto-trigger scheduler not implemented in this snapshot).

Key sections in **config.json**:

- **spawnSettings**
  - `naturalSpawnsEnabled` – `true` to allow automatic comets; `false` for commands/fixed spawns only.
  - `minDelaySeconds` / `maxDelaySeconds` – time between natural spawn checks.
  - `spawnChance` – chance that a spawn check actually triggers a comet.
  - `despawnTimeMinutes` – how long a comet can stay active before timing out.
  - `minSpawnDistance` / `maxSpawnDistance` – distance from players where comets may land.
  - `disabledWorlds` – list of world names where raids are **disabled**.
  - `globalComets` – if `true`, any player can trigger / interact with any comet; if `false`, comets are bound to their owner.

- **zoneSpawnChances**
  - Controls which **tiers** are likely to appear in each **zone** (0, 1, 2, 3…).
  - Higher zones can be configured to favour higher tiers (e.g. legendary/mythic).

- **tierSettings** + **rewardSettings** (in config.json)
  - **Tier settings** define wave timeout and radius for each tier.
  - **Reward settings** define default loot tables per tier.
  - **Themes** (mobs/bosses/waves), **spawnBlock**, and per-theme **rewardOverride** are all in **themes.json**.

- **zoneBaseLootPools** + **tierInheritanceWeights**
  - Base loot pools per zone (e.g. early zones drop copper, late zones drop adamantite etc.).
  - Inheritance weights allow higher tiers to pull some loot from lower tiers.

- **worldProtectSpawnRules** & **claimProtect**
  - Optional integrations to respect protected/claimed regions.
  - You can control whether comets can spawn inside specific regions/claims.

- **messages**
  - All chat and banner texts for comets and waves (explained in detail below).

Always make a backup before major edits; invalid JSON will cause the mod to log errors and fall back to safe defaults where possible.

---

## 3.1 Comet events wired to assets

Comet activation and rewards are wired into the mod’s assets as follows. Names must match exactly.

**Plugin registration (code)**  
In CometModPlugin.setup(), the mod registers these interaction names with the game’s Interaction codec:

- **Comet_Stone_Uncommon_Activate** – CometStoneActivateInteraction
- **Comet_Stone_Rare_Activate** – CometStoneActivateInteraction
- **Comet_Stone_Epic_Activate** – CometStoneActivateInteraction
- **Comet_Stone_Legendary_Activate** – CometStoneActivateInteraction
- **Comet_Stone_Mythic_Activate** – CometStoneActivateInteraction
- **Comet_Activate** – CometStoneActivateInteraction (shared for portals/coffins)
- **Comet_OpenRewardChest** – CometOpenRewardChestInteraction

**Block assets**  
Each comet stone block item (e.g. `Server/Item/Items/Comet_Stone_Uncommon.json`) must reference the matching interaction in `BlockType.Interactions.Use`:

- `Comet_Stone_Uncommon.json` → `"Use": "Comet_Stone_Uncommon_Activate"`
- Same pattern for Rare, Epic, Legendary, Mythic.
- Comet portals and coffins (and custom blocks that use the shared activator) → `"Use": "Comet_Activate"`
- `Comet_Reward_Chest.json` → `"Use": "Comet_OpenRewardChest"`

**Root interaction assets**  
Each name above must have a root interaction asset (e.g. `Server/Item/RootInteractions/Comet_Stone_Uncommon_Activate.json`) with `"Type": "<same name>"` so the game resolves the interaction to the mod’s registered handler.

**Tier-based assets (code expects these IDs)**  
CometTier in code uses these naming patterns; the pack must provide the corresponding assets:

- **Block ID** – Comet_Stone_&lt;Tier&gt; (e.g. Comet_Stone_Uncommon)
- **Falling projectile config** – Comet_Falling_&lt;Tier&gt; (e.g. Comet_Falling_Uncommon in Server/ProjectileConfigs/)
- **Landing explosion particle** – Comet_Explosion_Large_&lt;Tier&gt; (e.g. Comet_Explosion_Large_Uncommon in Server/Particles/)
- **Beam particle (on block)** – Comet_Beam_&lt;Tier&gt; (e.g. Comet_Beam_Uncommon; referenced in block item’s BlockType.Particles)

**Position-based fallback**  
Even if a block does not use the above interactions (e.g. a custom block or a themed coffin), **CometBlockEventSystem** listens for Use (F key) on any block. If that block’s position is a registered comet position (or within the configured radius), the mod still runs comet activation. So custom spawn blocks work as long as they are placed and registered by the mod.

### 3.2 spawnBlock and blocks without Use

- **spawnBlock** can be any block type ID (vanilla, mod, or custom). Blocks without a Use interaction (e.g. some portals) are supported as `spawnBlock` in themes.
- **How to activate:**
  - If the block has a **Use** interaction (e.g. coffin, comet stone), players press **F** to start the wave.
  - If the block has **no Use** (e.g. many portal assets), the engine never runs Use for that block. In that case, **hit the block once** (left-click); **CometBlockDamageActivationSystem** starts the comet and cancels damage so the block is not broken.
- To get **F** on a no-Use block, use a block asset that includes a Use (e.g. the mod's comet stones or a custom block whose JSON has `"Use": "Comet_Stone_Uncommon_Activate"` or the mod's shared activator).

### 3.3 Comet portal blocks (F to start wave)

The mod ships **Comet_** duplicates of vanilla portal blocks so that **F (Use)** starts the comet wave instead of teleporting. Each has Use: Comet_Activate and is registered to the same handler as the comet stones. Use these block IDs as spawnBlock in themes if you want a portal look with F to start:

- **Comet_VoidInvasion_Portal** – VoidInvasion_Portal
- **Comet_Portal_Return** – Portal_Return
- **Comet_Hub_Portal_Default** – Hub_Portal_Default
- **Comet_Hub_Portal_Flat** – Hub_Portal_Flat
- **Comet_Hub_Portal_Zone3_Taiga1** – Hub_Portal_Zone3_Taiga1
- **Comet_Forgotten_Temple_Portal_Enter** – Forgotten_Temple_Portal_Enter
- **Comet_Forgotten_Temple_Portal_Exit** – Forgotten_Temple_Portal_Exit
- **Comet_Instance_Gateway** – Instance_Gateway
- **Comet_Portal_Device** – Portal_Device
- **Comet_Spawn_Portal** – Spawn_Portal

**Comet coffin blocks** (F to start wave, beam included; use as spawnBlock for skeleton/undead themes):

- **Comet_Furniture_Village_Coffin** – Furniture_Village_Coffin
- **Comet_Furniture_Ancient_Coffin** – Furniture_Ancient_Coffin
- **Comet_Furniture_Temple_Dark_Coffin** – Furniture_Temple_Dark_Coffin
- **Comet_Furniture_Human_Ruins_Coffin** – Furniture_Human_Ruins_Coffin

### 3.4 Comet beam: add it in your custom asset JSON

**Vanilla assets never include a comet beam.** The mod does not inject a beam into blocks at runtime. If you want a beam above a spawn block (so it appears and disappears with the block), you must **add it in the block’s item JSON** in your custom asset.

- In the block’s **BlockType.Particles** array, add an entry with **SystemId** set to one of the beam IDs below.
- Example (add this inside your block item’s `BlockType`, alongside any existing `Particles` you want to keep):

```json
"Particles": [
  {
    "SystemId": "Comet_Beam_Portal_Blue"
  }
]
```

**Beam options (use exactly these IDs in SystemId):**

- **Comet_Beam_Uncommon** – Tier beam (green)
- **Comet_Beam_Rare** – Tier beam
- **Comet_Beam_Epic** – Tier beam
- **Comet_Beam_Legendary** – Tier beam
- **Comet_Beam_Mythic** – Tier beam
- **Comet_Beam_Portal_Blue** – Portal-style beam (blue)
- **Comet_Beam_Portal_Purple** – Portal-style beam (purple)
- **Comet_Beam_Portal_Cyan** – Portal-style beam (cyan)

- **Tier beams** (`Comet_Beam_Uncommon` etc.) match the comet stone tiers. **Portal beams** (`Comet_Beam_Portal_Blue`, `_Purple`, `_Cyan`) are for custom/comet portal blocks. Use one per block; the beam shows above the block and is removed when the block is broken.
- Blocks used as `spawnBlock` that do **not** define a beam in their JSON (e.g. vanilla `Furniture_Village_Coffin`) will have **no beam**. Duplicate the block in your pack and add a `Particles` entry as above if you want a beam.

### 3.5 Full setup: custom assets (JSON)

This section gives step-by-step instructions for using **custom blocks** (coffins, portals, or your own assets) as comet spawn blocks instead of the default comet stones.

**What you need:**

1. A block type the mod can place (vanilla, mod, or your own item JSON).
2. **themes.json** – set `spawnBlock` per theme to that block’s ID.
3. **Block item JSON** – so the block activates the comet (Use or hit-to-activate) and optionally shows a beam.

---

#### Step 1: Choose or create the block

- **Use an existing Comet_ block**  
  The mod ships comet variants (portals, coffins) that already have the right Use and beam. Use their IDs as `spawnBlock` in themes (see tables in 3.3). No JSON edits needed.

- **Use a vanilla/mod block as-is**  
  You can set `spawnBlock` to any block ID (e.g. `Furniture_Village_Coffin`). If that block has **no Use** interaction, players **hit the block once** to start the wave (damage is cancelled). If it has a Use, that interaction runs; the mod still detects Use on a registered comet position and starts the wave.

- **Create a custom duplicate**  
  Copy the block’s item JSON (e.g. from the game’s assets or another mod), give it a new ID (e.g. `MyPack_Comet_Coffin`), and add the fields below so **F** starts the wave and optionally a beam appears.

---

#### Step 2: Set spawnBlock in themes.json

In **themes.json**, each theme can have an optional **spawnBlock** field. When a comet is placed for that theme, the mod uses this block instead of the tier comet stone.

- **Key:** `"spawnBlock"` (string).
- **Value:** Block type ID, e.g. `"Comet_Furniture_Village_Coffin"` or `"Comet_VoidInvasion_Portal"` or your custom ID.
- **Omit or leave blank:** the mod uses the tier default (e.g. `Comet_Stone_Uncommon`).

**Example: theme entry with spawnBlock**

```json
"skeleton": {
  "displayName": "Skeleton",
  "tiers": [1, 2, 3, 4],
  "spawnBlock": "Comet_Furniture_Village_Coffin",
  "mobs": [
    { "id": "Skeleton_Uncommon", "weight": 1 },
    { "id": "Skeleton_Rare", "weight": 1 }
  ],
  "bosses": [
    { "id": "SkeletonBoss_Uncommon", "weight": 1 }
  ]
}
```

**Example: void theme using a portal block**

```json
"void": {
  "displayName": "Void",
  "tiers": [2, 3, 4],
  "spawnBlock": "Comet_VoidInvasion_Portal",
  "mobs": [
    { "id": "Void_Uncommon", "weight": 1 }
  ],
  "bosses": []
}
```

After editing, run **/comet reload** so the mod picks up the new spawnBlock values.

---

#### Step 3: Block item JSON (for F key and optional beam)

If you are **creating or editing** a block item JSON (e.g. your own comet coffin or portal), use the following.

**Location:**  
`Server/Item/Items/<YourBlockId>.json` (or your mod’s equivalent).

**1) Use interaction (so F starts the wave)**

Under BlockType.Interactions, set **Use** to one of:

- **Comet_Activate** – shared activator used by all Comet_ portals and coffins. Use this for custom portals/coffins so **F** starts the wave without teleporting or other vanilla behavior.
- Or a tier-specific one: **Comet_Stone_Uncommon_Activate**, **Comet_Stone_Rare_Activate**, etc., if you want the block tied to a single tier’s handler.

**2) Root interaction asset (required for Comet_Activate)**

The game resolves `"Use": "Comet_Activate"` via a root interaction. The mod ships:

- **Server/Item/RootInteractions/Comet_Activate.json**

Content (must exist in your pack if you use `Comet_Activate`):

```json
{
  "Interactions": [
    {
      "Type": "Comet_Activate"
    }
  ]
}
```

**3) Optional: beam particle**

To show a beam above the block, add a **Particles** entry under BlockType as in section 3.4. Use one of the beam IDs from the table in 3.4. You can keep other particles (e.g. portal effect) in the same array.

**4) Other block fields**

Leave **Material**, **DrawType**, **CustomModel**, **HitboxType**, etc. as needed for your block. The mod does not require any other special fields.

---

**Full example: custom portal block** (F to start, portal effect + beam)

Save as e.g. `Server/Item/Items/MyPack_Comet_Portal.json`:

```json
{
  "TranslationProperties": {
    "Name": "MyPack_Comet_Portal"
  },
  "Icon": "Icons/ItemsGenerated/Portal_Return.png",
  "Categories": ["Blocks.Portals"],
  "BlockType": {
    "DrawType": "Model",
    "Material": "Solid",
    "Opacity": "Transparent",
    "CustomModel": "Blocks/Miscellaneous/Platform_Magic_Exit.blockymodel",
    "CustomModelTexture": [
      { "Texture": "Blocks/Miscellaneous/Platform_Magic_Red.png", "Weight": 1 }
    ],
    "HitboxType": "Pad_Portal",
    "BlockParticleSetId": "Stone",
    "BlockSoundSetId": "Stone",
    "VariantRotation": "NESW",
    "Flags": { "IsUsable": true },
    "Gathering": { "Breaking": { "GatherType": "Unbreakable" } },
    "Interactions": {
      "Use": "Comet_Activate"
    },
    "Particles": [
      {
        "SystemId": "Portal_Purple",
        "PositionOffset": { "Y": -1 }
      },
      {
        "SystemId": "Comet_Beam_Portal_Blue"
      }
    ]
  },
  "PlayerAnimationsId": "Block",
  "Tags": { "Type": ["Portal"] },
  "Quality": "Technical"
}
```

Then set `"spawnBlock": "MyPack_Comet_Portal"` in your theme in themes.json.

---

**Full example: custom coffin block** (F to start, beam only)

Save as e.g. `Server/Item/Items/MyPack_Comet_Coffin.json`:

```json
{
  "TranslationProperties": {
    "Name": "MyPack_Comet_Coffin"
  },
  "Categories": ["Furniture.Containers"],
  "Icon": "Icons/ItemsGenerated/Furniture_Village_Coffin.png",
  "BlockType": {
    "BlockParticleSetId": "Stone",
    "BlockSoundSetId": "Stone",
    "CustomModel": "Blocks/Decorative_Sets/Village/Coffin.blockymodel",
    "CustomModelTexture": [
      { "Texture": "Blocks/Decorative_Sets/Village/Coffin_Texture.png", "Weight": 1 }
    ],
    "DrawType": "Model",
    "Gathering": { "Breaking": { "GatherType": "Rocks", "DropList": "Container_Coffins" } },
    "Interactions": {
      "Use": "Comet_Activate"
    },
    "Flags": { "IsUsable": true },
    "HitboxType": "Coffin",
    "Material": "Solid",
    "Opacity": "Transparent",
    "Support": { "Down": [{ "FaceType": "Full" }] },
    "VariantRotation": "Wall",
    "Particles": [
      {
        "SystemId": "Comet_Beam_Portal_Blue"
      }
    ]
  },
  "PlayerAnimationsId": "Block",
  "Tags": { "Type": ["Furniture"] }
}
```

Then set `"spawnBlock": "MyPack_Comet_Coffin"` in your theme in themes.json.

---

#### Step 4: Activation (F vs hit)

- **Block has `Use` set (e.g. `Comet_Activate`):**  
  Player presses **F** on the block; the mod’s handler runs and starts the comet wave.

- **Block has no Use:**  
  The engine does not run Use for that block. Player must **left-click (hit) the block once**; **CometBlockDamageActivationSystem** starts the wave and cancels damage so the block is not broken.

---

#### Step 5: What happens when the wave completes

- **Comet stone blocks** (`Comet_Stone_*`): the block is broken and replaced by the **reward chest** on the same spot.

- **Custom assets** (any other `spawnBlock`): the **asset block is not broken**. The **reward chest is placed in front** of it (adjacent position). When the chest **expires or is broken**, the mod **removes the linked asset block** as well, so the asset disappears with the chest.

So for coffins/portals/custom blocks, the chest appears beside the asset and both go away together when the chest is gone.

---

#### Checklist: minimal custom asset

- [ ] **themes.json:** theme has `"spawnBlock": "Your_Block_Id"`.
- [ ] **Block item JSON:** `BlockType.Interactions.Use` = `"Comet_Activate"` (or a tier activator) if you want F to start the wave.
- [ ] **Root interaction:** `Comet_Activate.json` (or the tier one) exists under `Server/Item/RootInteractions/` with the correct `"Type"`.
- [ ] **Optional beam:** BlockType.Particles includes an entry with SystemId set to a beam from the list in 3.4.
- [ ] **Reload:** run `/comet reload` after changing themes or config.

---

## 4. Message & UI Configuration

All message settings live in your `config.json` under the top‑level `"messages"` block:

```json
"messages": {
  "msgCometFallingTitle": "...",
  "msgCometFallingSubtitle": "...",
  "msgCometFallingChatCoords": "...",
  "msgWaveBossTitle": "...",
  "msgWaveBossTitleNoCount": "...",
  "msgWaveBossSubtitle": "...",
  "msgWaveTitle": "...",
  "msgWaveTitleNoCount": "...",
  "msgWaveSubtitle": "...",
  "msgWaveFailedTitle": "...",
  "msgWaveFailedSubtitle": "...",
  "msgWaveCompleteTitle": "...",
  "msgWaveCompleteSubtitle": "...",
  "msgWaveCompleteChatHeaderPrefix": "...",
  "msgWaveCompleteChatHeader": "...",
  "msgWaveCompleteChatItemPrefix": "..."
}
```

### 4.1 Placeholders

You can embed dynamic values into messages using **placeholders**.
They will be replaced in‑game when the message is shown.

- **%tier%** – Comet tier name (e.g. Uncommon, Rare, Mythic)
- **%x%**, **%y%**, **%z%** – Target comet block coordinates
- **%currentWave%** – Current wave number (1-based)
- **%totalWaves%** – Total number of waves for this comet
- **%theme%** – Current wave theme display name
- **%bossStatus%** – Alive or Defeated during boss waves
- **%killed%** – Number of mobs killed this wave
- **%total%** – Total mobs this wave
- **%time%** – Remaining time text (formatted in‑game)

If you remove a placeholder from a string, that value simply will not be shown.

**Message keys reference** (all keys under messages in config.json):

- **msgCometFallingTitle** – Banner title when comet is falling
- **msgCometFallingSubtitle** – Banner subtitle when comet is falling
- **msgCometFallingChatCoords** – Chat line with comet target coordinates
- **msgWaveBossTitle** – Banner title during boss wave (with wave count)
- **msgWaveBossTitleNoCount** – Banner title during boss wave (no count)
- **msgWaveBossSubtitle** – Banner subtitle during boss wave
- **msgWaveTitle** – Banner title during normal wave (with count)
- **msgWaveTitleNoCount** – Banner title during normal wave (no count)
- **msgWaveSubtitle** – Banner subtitle during normal wave
- **msgWaveFailedTitle** – Banner title when wave fails
- **msgWaveFailedSubtitle** – Banner subtitle when wave fails
- **msgWaveCompleteTitle** – Banner title when wave completes
- **msgWaveCompleteSubtitle** – Banner subtitle when wave completes
- **msgWaveCompleteChatHeaderPrefix** – Prefix for chat reward header
- **msgWaveCompleteChatHeader** – Chat header when wave completes
- **msgWaveCompleteChatItemPrefix** – Prefix for each reward line in chat

### 4.2 Comet Falling (Banner + Chat)

These messages are shown when a comet starts falling towards the world.

- **msgCometFallingTitle**
  - **Where:** Large banner title at the top of the screen.
  - **Default:** `%tier% Comet Falling!`
  - **Placeholders:** `%tier%`

- **msgCometFallingSubtitle**
  - **Where:** Small subtitle under the comet falling title.
  - **Default:** `Watch the sky!`
  - **Placeholders:** `%tier%` (optional)

- **msgCometFallingChatCoords**
  - **Where:** Chat line sent to the player who triggered the comet.
  - **Default:** `%tier% Comet falling! Target: X=%x%, Y=%y%, Z=%z%`
  - **Placeholders:** `%tier%`, `%x%`, `%y%`, `%z%`

### 4.3 Active Wave HUD – Boss Waves (Banner)

Used while a **boss wave** is active.

- **msgWaveBossTitle**
  - **Where:** Banner title when total wave count is known (e.g. multi‑wave encounters).
  - **Default:** `Boss Wave %currentWave%/%totalWaves%`
  - **Placeholders:** `%currentWave%`, `%totalWaves%`, `%theme%`

- **msgWaveBossTitleNoCount**
  - **Where:** Banner title when there is no multi‑wave count to display.
  - **Default:** `Boss Wave!`
  - **Placeholders:** `%theme%` (optional)

- **msgWaveBossSubtitle**
  - **Where:** Banner subtitle during boss waves.
  - **Default:** `Boss: %bossStatus% | Time: %time%`
  - **Placeholders:** `%bossStatus%`, `%time%`

### 4.4 Active Wave HUD – Normal Waves (Banner)

Used while a **normal wave** (non‑boss) is active.

- **msgWaveTitle**
  - **Where:** Banner title when total wave count is known.
  - **Default:** `Wave %currentWave%/%totalWaves% - %theme%`
  - **Placeholders:** `%currentWave%`, `%totalWaves%`, `%theme%`

- **msgWaveTitleNoCount**
  - **Where:** Banner title when there is no multi‑wave count to display.
  - **Default:** `%theme% Incoming!`
  - **Placeholders:** `%theme%`

- **msgWaveSubtitle**
  - **Where:** Banner subtitle during normal waves.
  - **Default:** `Mobs: %killed%/%total% | Time: %time%`
  - **Placeholders:** `%killed%`, `%total%`, `%time%`

### 4.5 Wave Failure (Banner)

Shown when a comet / wave fails due to timeout.

- **msgWaveFailedTitle**
  - **Where:** Banner title on failure.
  - **Default:** `Wave Failed!`
  - **Placeholders:** none

- **msgWaveFailedSubtitle**
  - **Where:** Banner subtitle on failure.
  - **Default:** `Time's Up!`
  - **Placeholders:** none

### 4.6 Wave Completion (Banner + Chat)

Shown when all waves are cleared and loot is dropped.

- **msgWaveCompleteTitle**
  - **Where:** Banner title when the comet is completed.
  - **Default:** `Wave Complete!`
  - **Placeholders:** none

- **msgWaveCompleteSubtitle**
  - **Where:** Banner subtitle on completion.
  - **Default:** `Loot Dropped!`
  - **Placeholders:** none

- **msgWaveCompleteChatHeaderPrefix**
  - **Where:** Colored prefix at the start of the loot summary chat line.
  - **Default:** `[Comet] `
  - **Placeholders:** none

- **msgWaveCompleteChatHeader**
  - **Where:** Main loot header text in chat.
  - **Default:** `Wave Complete! Your rewards:`
  - **Placeholders:** none

- **msgWaveCompleteChatItemPrefix**
  - **Where:** Prefix before each item line in the loot summary chat.
  - **Default:** ` - `
  - **Placeholders:** none

### 4.7 Tips for Editing Messages

- You can safely change the wording of any message.
- Keep placeholders you want to display; remove ones you do not need.
- Avoid adding new placeholder names unless the mod explicitly supports them.
- If you break JSON syntax (e.g. missing quotes or commas), the mod will fall back to defaults or log a config error.

---

## 5. Recommended Setup Flow

1. **Install the mod** and start the server once so it can generate default configs.
2. Open `config.json` and:
   - Adjust `spawnSettings` (delays, chances, disabled worlds, globalComets).
   - Check `zoneSpawnChances` so early zones are not too punishing.
3. Open **themes.json**:
   - Review themes, mobs and bosses for each tier.
   - Set **spawnBlock** per theme to choose which block is placed (comet stone, coffin, portal, or custom).
   - Set per-theme **rewardOverride** if needed for custom loot per tier.
   - Adjust tier timeouts and radii in `config.json` if needed.
4. Configure **protections**:
   - If you use WorldProtect or claim plugins, tune `worldProtectSpawnRules` and `claimProtect`.
5. Customise **messages**:
   - Use this page as a reference for which placeholders are available.
6. Run `/comet reload` and test with `/comet test` and `/comet spawn`.

---

## 6. Troubleshooting

- **No comets are spawning naturally**
  - Check `naturalSpawnsEnabled` is `true`.
  - Make sure the current world is **not** in `disabledWorlds`.
  - Verify `spawnChance` is not set too low.

- **Tier 5 / Mythic never appears**
  - Comet Raids Redux can optionally depend on *Endgame&QoL* for tier 5.
  - If that plugin is missing, the mod automatically clamps tier 5 requests down to tier 4.

- **Comets never spawn in protected/claimed areas**
  - Check `worldProtectSpawnRules` and `claimProtect` sections.
  - Some setups may intentionally block spawns inside claims by default.

- **Messages look wrong or placeholders show literally**
  - Make sure you did not remove the `%` signs or accidentally escape them.
  - Only the placeholders listed in this document are supported.

- **Wave mobs never drop loot**
  - By default, mobs spawned during a comet wave drop no loot. To allow wave mob loot, set `"disableWaveMobLoot": false` in `config.json` under `spawnSettings` (or at root), then run `/comet reload`.

If you continue having issues, temporarily restore from a known‑good backup of **config.json**, **themes.json**, and optionally **fixed_spawns.json**, then re‑apply your changes step by step.

