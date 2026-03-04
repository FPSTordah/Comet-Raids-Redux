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

- **`/comet spawn [tier] [theme]`**
  - Spawns a comet near you.
  - `tier` can be things like `COMMON`, `UNCOMMON`, `RARE`, `EPIC`, `LEGENDARY`, `MYTHIC` (if enabled).
  - `theme` optionally forces a specific theme (otherwise one is chosen based on zone & tier).

- **`/comet test`**
  - Runs a quick spawn test for your current zone so you can verify config and spawns.

- **`/comet reload`**
  - Reloads `comet_config.json` and `comet_themes_and_monster_groups.json`.
  - Use this after changing config files.

- **Fixed spawn commands**
  - `/comet setspawn` – create a fixed comet spawn point at your location.
  - `/comet removespawn` – remove a specific fixed spawn.
  - `/comet listspawns` – list all fixed spawns.
  - `/comet schedulespawn` – schedule a fixed spawn to trigger.

- **Admin tools**
  - `/comet destroyall` – find and destroy all comet blocks in the world.

Your server’s documentation or permissions setup may give more detailed permission nodes for each sub‑command.

---

## 3. Config Overview (`comet_config.json`)

Comet Raids Redux uses `comet_config.json` as the **single source of truth**.
On first run, the mod generates a default config; after that, your edits are preserved and merged forward.

Key sections:

- **`spawnSettings`**
  - `naturalSpawnsEnabled` – `true` to allow automatic comets; `false` for commands/fixed spawns only.
  - `minDelaySeconds` / `maxDelaySeconds` – time between natural spawn checks.
  - `spawnChance` – chance that a spawn check actually triggers a comet.
  - `despawnTimeMinutes` – how long a comet can stay active before timing out.
  - `minSpawnDistance` / `maxSpawnDistance` – distance from players where comets may land.
  - `disabledWorlds` – list of world names where raids are **disabled**.
  - `globalComets` – if `true`, any player can trigger / interact with any comet; if `false`, comets are bound to their owner.

- **`zoneSpawnChances`**
  - Controls which **tiers** are likely to appear in each **zone** (0, 1, 2, 3…).
  - Higher zones can be configured to favour higher tiers (e.g. legendary/mythic).

- **`themes` + `tierSettings` + `rewardSettings`** (in `comet_themes_and_monster_groups.json`)
  - **Themes** define which mobs/bosses appear per tier.
  - **Tier settings** define wave timeout and radius for each tier.
  - **Reward settings** define loot tables per tier.

- **`zoneBaseLootPools` + `tierInheritanceWeights`**
  - Base loot pools per zone (e.g. early zones drop copper, late zones drop adamantite etc.).
  - Inheritance weights allow higher tiers to pull some loot from lower tiers.

- **`worldProtectSpawnRules` & `claimProtect`**
  - Optional integrations to respect protected/claimed regions.
  - You can control whether comets can spawn inside specific regions/claims.

- **`messages`**
  - All chat and banner texts for comets and waves (explained in detail below).

Always make a backup before major edits; invalid JSON will cause the mod to log errors and fall back to safe defaults where possible.

---

## 4. Message & UI Configuration

All message settings live in your `comet_config.json` under the top‑level `"messages"` block:

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

- `%tier%` – comet tier name (e.g. `Common`, `Mythic`)
- `%x%`, `%y%`, `%z%` – target comet block coordinates
- `%currentWave%` – current wave number
- `%totalWaves%` – total number of waves for this comet
- `%theme%` – current wave theme display name
- `%bossStatus%` – `"Alive"` or `"Defeated"` during boss waves
- `%killed%` – number of mobs killed this wave
- `%total%` – total mobs this wave
- `%time%` – remaining time text (formatted in‑game)

If you remove a placeholder from a string, that value simply will not be shown.

### 4.2 Comet Falling (Banner + Chat)

These messages are shown when a comet starts falling towards the world.

- **`msgCometFallingTitle`**
  - **Where:** Large banner title at the top of the screen.
  - **Default:** `%tier% Comet Falling!`
  - **Placeholders:** `%tier%`

- **`msgCometFallingSubtitle`**
  - **Where:** Small subtitle under the comet falling title.
  - **Default:** `Watch the sky!`
  - **Placeholders:** `%tier%` (optional)

- **`msgCometFallingChatCoords`**
  - **Where:** Chat line sent to the player who triggered the comet.
  - **Default:** `%tier% Comet falling! Target: X=%x%, Y=%y%, Z=%z%`
  - **Placeholders:** `%tier%`, `%x%`, `%y%`, `%z%`

### 4.3 Active Wave HUD – Boss Waves (Banner)

Used while a **boss wave** is active.

- **`msgWaveBossTitle`**
  - **Where:** Banner title when total wave count is known (e.g. multi‑wave encounters).
  - **Default:** `Boss Wave %currentWave%/%totalWaves%`
  - **Placeholders:** `%currentWave%`, `%totalWaves%`, `%theme%`

- **`msgWaveBossTitleNoCount`**
  - **Where:** Banner title when there is no multi‑wave count to display.
  - **Default:** `Boss Wave!`
  - **Placeholders:** `%theme%` (optional)

- **`msgWaveBossSubtitle`**
  - **Where:** Banner subtitle during boss waves.
  - **Default:** `Boss: %bossStatus% | Time: %time%`
  - **Placeholders:** `%bossStatus%`, `%time%`

### 4.4 Active Wave HUD – Normal Waves (Banner)

Used while a **normal wave** (non‑boss) is active.

- **`msgWaveTitle`**
  - **Where:** Banner title when total wave count is known.
  - **Default:** `Wave %currentWave%/%totalWaves% - %theme%`
  - **Placeholders:** `%currentWave%`, `%totalWaves%`, `%theme%`

- **`msgWaveTitleNoCount`**
  - **Where:** Banner title when there is no multi‑wave count to display.
  - **Default:** `%theme% Incoming!`
  - **Placeholders:** `%theme%`

- **`msgWaveSubtitle`**
  - **Where:** Banner subtitle during normal waves.
  - **Default:** `Mobs: %killed%/%total% | Time: %time%`
  - **Placeholders:** `%killed%`, `%total%`, `%time%`

### 4.5 Wave Failure (Banner)

Shown when a comet / wave fails due to timeout.

- **`msgWaveFailedTitle`**
  - **Where:** Banner title on failure.
  - **Default:** `Wave Failed!`
  - **Placeholders:** none

- **`msgWaveFailedSubtitle`**
  - **Where:** Banner subtitle on failure.
  - **Default:** `Time's Up!`
  - **Placeholders:** none

### 4.6 Wave Completion (Banner + Chat)

Shown when all waves are cleared and loot is dropped.

- **`msgWaveCompleteTitle`**
  - **Where:** Banner title when the comet is completed.
  - **Default:** `Wave Complete!`
  - **Placeholders:** none

- **`msgWaveCompleteSubtitle`**
  - **Where:** Banner subtitle on completion.
  - **Default:** `Loot Dropped!`
  - **Placeholders:** none

- **`msgWaveCompleteChatHeaderPrefix`**
  - **Where:** Colored prefix at the start of the loot summary chat line.
  - **Default:** `[Comet] `
  - **Placeholders:** none

- **`msgWaveCompleteChatHeader`**
  - **Where:** Main loot header text in chat.
  - **Default:** `Wave Complete! Your rewards:`
  - **Placeholders:** none

- **`msgWaveCompleteChatItemPrefix`**
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
2. Open `comet_config.json` and:
   - Adjust `spawnSettings` (delays, chances, disabled worlds, globalComets).
   - Check `zoneSpawnChances` so early zones are not too punishing.
3. Open `comet_themes_and_monster_groups.json` and:
   - Review themes, mobs and bosses for each tier.
   - Adjust tier timeouts and radii if needed.
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

If you continue having issues, temporarily restore from a known‑good backup of `comet_config.json` and `comet_themes_and_monster_groups.json`, then re‑apply your changes step by step.

