package com.cometmod.wave;

import com.cometmod.*;
import com.cometmod.commands.*;
import com.cometmod.loot.*;
import com.cometmod.services.*;
import com.cometmod.spawn.*;
import com.cometmod.systems.*;
import com.cometmod.wave.*;


import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.math.vector.Vector3f;
import com.hypixel.hytale.math.vector.Vector3i;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.event.events.ecs.UseBlockEvent;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.core.util.EventTitleUtil;
import com.hypixel.hytale.server.npc.NPCPlugin;
import it.unimi.dsi.fastutil.Pair;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

/**
 * Manages comet wave spawning, combat, and rewards.
 */
public class CometWaveManager {
    private static final Logger LOGGER = Logger.getLogger(CometWaveManager.class.getName());

    private com.hypixel.hytale.server.core.plugin.PluginBase plugin;

    // Focused services split from CometWaveManager
    private final CometWaveStateService waveState = new CometWaveStateService();
    private final CometThemeSelectionService themeSelectionService = new CometThemeSelectionService();
    private final CometRewardService rewardService = new CometRewardService();
    private final CometWaveUiService waveUiService = new CometWaveUiService();

    // State aliases for concise access throughout this class.
    private final Map<Vector3i, CometState> activeComets = waveState.activeComets();
    private final Map<Vector3i, CometTier> cometTiers = waveState.cometTiers();
    private final Map<Vector3i, java.util.UUID> cometOwners = waveState.cometOwners();
    private final Map<Vector3i, Integer> cometZones = waveState.cometZones();

    public void setPlugin(com.hypixel.hytale.server.core.plugin.PluginBase plugin) {
        this.plugin = plugin;
    }

    private static final int WAVE_MOB_COUNT = 5;
    private static final Random RANDOM = new Random();

    // Track current/forced theme IDs for comet blocks.
    private final Map<Vector3i, String> cometThemes = waveState.cometThemes();
    private final Map<Vector3i, String> forcedThemes = waveState.forcedThemes();

    // Max ranged mobs per wave (applies to ALL tiers)
    private static final int MAX_RANGED_PER_WAVE = 1;

    public enum CometState {
        UNTOUCHED,
        WAVE_ACTIVE,
        COMPLETED
    }

    // Get comet state from block state (persists across relogs)
    public CometState getCometState(Vector3i blockPos) {
        return waveState.getCometState(blockPos);
    }

    /**
     * Get all active comets for map marker display
     * 
     * @return Map of comet positions to their states
     */
    public Map<Vector3i, CometState> getActiveComets() {
        return waveState.getActiveCometsSnapshot();
    }

    /**
     * Get all comet tiers for map marker display
     * 
     * @return Map of comet positions to their tiers
     */
    public Map<Vector3i, CometTier> getCometTiers() {
        return waveState.getCometTiersSnapshot();
    }

    /**
     * Get all comet owners for map marker filtering
     *
     * @return Map of comet positions to their owner UUIDs
     */
    public Map<Vector3i, java.util.UUID> getCometOwners() {
        return waveState.getCometOwnersSnapshot();
    }

    /**
     * Check if there's an active comet near the given position
     *
     * @param x        X coordinate
     * @param y        Y coordinate
     * @param z        Z coordinate
     * @param distance Maximum distance to check
     * @return true if there's an active comet within distance
     */
    public boolean hasActiveCometNear(int x, int y, int z, int distance) {
        return waveState.hasActiveCometNear(x, y, z, distance);
    }

    /**
     * Get the owner UUID for a specific comet
     *
     * @param blockPos The comet block position
     * @return The owner UUID, or null if not found
     */
    public java.util.UUID getCometOwner(Vector3i blockPos) {
        return cometOwners.get(blockPos);
    }

    /**
     * Find the registered comet/replacement position at these coordinates (for USE on themed blocks where map key may differ by instance).
     */
    public Vector3i getRegisteredBlockPos(int x, int y, int z) {
        return waveState.findRegisteredPosition(x, y, z);
    }

    /** Find a registered position within distance of (x,y,z) so USE on adjacent or multi-block (coffin/portal) still activates. */
    public Vector3i getRegisteredBlockPosNear(int x, int y, int z, int maxDistance) {
        return waveState.findRegisteredPositionNear(x, y, z, maxDistance);
    }

    /** Theme at a registered position (forced theme wins). Used to link USE on themed blocks by position when block ID may vary. */
    public String getThemeAtPosition(Vector3i blockPos) {
        if (blockPos == null) return null;
        String forced = waveState.getForcedTheme(blockPos);
        if (forced != null && !forced.isBlank()) return forced;
        return waveState.getTheme(blockPos);
    }

    private static class WaveData {
        final List<Ref<EntityStore>> spawnedMobs = new ArrayList<>();
        final Vector3i blockPos;
        final Ref<EntityStore> playerRef;
        final Store<EntityStore> store;
        long startTime; // Track when wave started for timeout (not final - needs to be reset for each wave)
        long lastTimerUpdate = 0; // Track last time timer was updated (to update every 5 seconds)
        int initialSpawnCount = 0; // Track how many mobs were actually spawned
        int remainingCount = WAVE_MOB_COUNT;
        int previousRemainingCount = WAVE_MOB_COUNT; // Track previous count to detect changes
        int currentWave = 1; // Track wave number (1-based for display, internally converted from 0-based index)
        int currentWaveIndex = 0; // 0-based wave index for multi-wave support
        int totalWaveCount = 2; // Total waves in this encounter (default 2: 1 normal + 1 boss)
        String themeName = "Unknown"; // Display name of the current theme

        WaveData(Vector3i blockPos, Ref<EntityStore> playerRef, Store<EntityStore> store) {
            this.blockPos = blockPos;
            this.playerRef = playerRef;
            this.store = store;
            this.startTime = System.currentTimeMillis();
            this.lastTimerUpdate = this.startTime;
        }

        boolean hasMoreWaves() {
            return currentWaveIndex < totalWaveCount - 1;
        }

        void advanceToNextWave() {
            currentWaveIndex++;
            currentWave = currentWaveIndex + 1;
            startTime = System.currentTimeMillis();
            lastTimerUpdate = startTime;
            spawnedMobs.clear();
            initialSpawnCount = 0;
        }
    }

    private static class FailedSpawnInfo {
        final String npcType;
        final Vector3f rotation;
        final boolean isRanged;

        FailedSpawnInfo(String n, Vector3f r, boolean ir) {
            npcType = n;
            rotation = r;
            isRanged = ir;
        }
    }

    private static boolean isRangedMob(String npcType) {
        if (npcType == null)
            return false;
        String[] r = { "Archer", "Archmage", "Lobber", "Shaman", "Mage", "Ranger", "Hunter", "Stalker", "Priest",
                "Gunner", "Alchemist" };
        for (String x : r)
            if (npcType.contains(x))
                return true;
        return false;
    }

    private final Map<Vector3i, WaveData> activeWaves = new ConcurrentHashMap<>();

    /**
     * Check for wave timeouts and destroy expired comets
     * This should be called periodically (every 1 second) from the plugin
     * NOTE: This runs on the scheduler thread, so we need to execute world
     * operations on WorldThread
     */
    public void checkTimeouts() {
        // Check all active waves for timeout
        long currentTime = System.currentTimeMillis();
        java.util.List<Vector3i> timedOutWaves = new java.util.ArrayList<>();
        java.util.List<WaveData> activeWavesToRefresh = new java.util.ArrayList<>();

        for (Map.Entry<Vector3i, WaveData> entry : activeWaves.entrySet()) {
            WaveData waveData = entry.getValue();
            long elapsedTime = currentTime - waveData.startTime;

            // Get tier-specific timeout
            CometTier tier = cometTiers.getOrDefault(entry.getKey(), CometTier.UNCOMMON);
            long tierTimeout = CometWaveRunner.getTierTimeoutMs(tier);

            if (elapsedTime >= tierTimeout) {
                LOGGER.info("[checkTimeouts] TIMEOUT for wave at " + entry.getKey() +
                        " (elapsed=" + (elapsedTime / 1000) + "s)");
                timedOutWaves.add(entry.getKey());
            } else {
                activeWavesToRefresh.add(waveData);
            }
        }

        // Keep countdown UI advancing even when no mobs die.
        for (WaveData waveData : activeWavesToRefresh) {
            refreshWaveCountdown(waveData);
        }

        // Destroy timed out waves - must execute on WorldThread
        for (Vector3i blockPos : timedOutWaves) {
            WaveData waveData = activeWaves.get(blockPos);
            if (waveData == null)
                continue;

            // Try to find a valid store to execute the cleanup
            com.hypixel.hytale.component.Store<com.hypixel.hytale.server.core.universe.world.storage.EntityStore> store = waveData.store;

            if (store == null && waveData.playerRef != null && waveData.playerRef.isValid()) {
                store = waveData.playerRef.getStore();
            } else if (store == null) {
                // Player dead/gone, try to use a mob ref
                for (Ref<EntityStore> mobRef : waveData.spawnedMobs) {
                    if (mobRef != null && mobRef.isValid()) {
                        store = mobRef.getStore();
                        break;
                    }
                }
            }

            if (store != null) {
                // Get world and execute on WorldThread
                try {
                    com.hypixel.hytale.server.core.universe.world.World world = ((com.hypixel.hytale.server.core.universe.world.storage.EntityStore) store
                            .getExternalData()).getWorld();

                    final Store<EntityStore> finalStore = store;
                    final WaveData finalWaveData = waveData;

                    world.execute(() -> {
                        destroyCometOnTimeout(finalStore, finalWaveData);
                    });
                } catch (Exception e) {
                    LOGGER.warning("Error executing cleanup for timed out wave at " + blockPos + ": " + e.getMessage());
                }
            } else {
                // Critical failure: No valid store found to clean up wave.
                // Just remove it from active waves to prevent infinite loops,
                // though the block and mobs might linger.
                activeWaves.remove(blockPos);
                LOGGER.warning("Could not find valid store to clean up orphaned wave at " + blockPos);
            }
        }
    }

    private void refreshWaveCountdown(WaveData waveData) {
        if (waveData == null) {
            return;
        }

        Store<EntityStore> store = waveData.store;
        if (store == null && waveData.playerRef != null && waveData.playerRef.isValid()) {
            store = waveData.playerRef.getStore();
        } else if (store == null) {
            for (Ref<EntityStore> mobRef : waveData.spawnedMobs) {
                if (mobRef != null && mobRef.isValid()) {
                    store = mobRef.getStore();
                    break;
                }
            }
        }

        if (store == null) {
            return;
        }

        try {
            com.hypixel.hytale.server.core.universe.world.World world = ((com.hypixel.hytale.server.core.universe.world.storage.EntityStore) store
                    .getExternalData()).getWorld();
            final Store<EntityStore> finalStore = store;
            final Ref<EntityStore> finalPlayerRef = waveData.playerRef;
            final WaveData finalWaveData = waveData;
            world.execute(() -> {
                if (!activeWaves.containsKey(finalWaveData.blockPos)) {
                    return;
                }
                updateWaveCountdown(finalStore, finalPlayerRef, finalWaveData);
            });
        } catch (Exception e) {
            LOGGER.warning("Error refreshing countdown for wave at " + waveData.blockPos + ": " + e.getMessage());
        }
    }

    public void handleCometActivation(Store<EntityStore> store, Ref<EntityStore> playerRef, Vector3i blockPos) {
        if (CometConfig.DEBUG) {
            LOGGER.info("[CometDebug] handleCometActivation at " + blockPos);
        }
        com.hypixel.hytale.server.core.universe.world.World world = ((com.hypixel.hytale.server.core.universe.world.storage.EntityStore) store
                .getExternalData()).getWorld();

        // Use canonical registered position so map lookups work (themed blocks may use a different Vector3i instance; allow nearby for multi-block)
        Vector3i canonical = waveState.findRegisteredPositionNear(blockPos.x, blockPos.y, blockPos.z, 4);
        if (canonical != null) blockPos = canonical;

        CometConfig config = CometConfig.getInstance();
        if (config != null && !config.isRaidEnabledInWorld(world)) {
            LOGGER.info("Blocked comet activation at " + blockPos + " in disabled world " + world.getName());
            return;
        }

        com.hypixel.hytale.server.core.universe.world.meta.BlockState blockState = world.getState(blockPos.x,
                blockPos.y, blockPos.z, false);

        // If block is already an ItemContainerState with droplist, it's completed
        if (blockState instanceof com.hypixel.hytale.server.core.universe.world.meta.state.ItemContainerState) {
            com.hypixel.hytale.server.core.universe.world.meta.state.ItemContainerState containerState = (com.hypixel.hytale.server.core.universe.world.meta.state.ItemContainerState) blockState;
            String droplist = containerState.getDroplist();
            if (droplist != null && droplist.startsWith("Comet_Rewards")) {
                // Already completed - open container or destroy if empty
                LOGGER.info("Comet at " + blockPos + " already completed (persisted state)");

                // Try to determine tier from droplist name
                CometTier tier = CometTier.UNCOMMON;
                for (CometTier t : CometTier.values()) {
                    if (droplist.equals(t.getLootTableName())) {
                        tier = t;
                        break;
                    }
                }
                tier = CometConfig.clampUnavailableTier(tier);
                cometTiers.put(blockPos, tier);

                // Don't check if empty here - the droplist might not have populated yet
                // Just open the container - it will populate when opened
                // We'll check if empty when the window closes
                LOGGER.info("Comet already completed, will open container via interaction");
                return;
            }
        }

        // Tier from block type (comet block) or from registration (themed replacement: coffin/portal/volcano)
        com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockType blockTypeAt = world.getBlockType(blockPos.x, blockPos.y, blockPos.z);
        CometTier tier;
        if (blockTypeAt != null && blockTypeAt.getId().startsWith("Comet_Stone_")) {
            tier = determineTierFromBlock(world, blockPos);
        } else {
            tier = waveState.getTierOrDefault(blockPos, CometTier.UNCOMMON);
        }
        if (tier == null) tier = CometTier.UNCOMMON;
        tier = CometConfig.clampUnavailableTier(tier);
        cometTiers.put(blockPos, tier);

        // Check if this comet is already active (in memory)
        CometState state = activeComets.getOrDefault(blockPos, CometState.UNTOUCHED);

        if (state == CometState.WAVE_ACTIVE) {
            // Wave already active, don't spawn again
            LOGGER.info("Comet at " + blockPos + " already has active wave");
            return;
        }

        if (state == CometState.COMPLETED) {
            // Already completed, allow opening chest
            LOGGER.info("Comet at " + blockPos + " already completed (memory state)");
            // The interaction will handle opening the container
            return;
        }

        // Start a new wave
        activeComets.put(blockPos, CometState.WAVE_ACTIVE);

        LOGGER.info("Starting wave for comet at " + blockPos + " (tier: " + tier.getName() + ") - 3 second countdown");

        final com.hypixel.hytale.server.core.universe.world.World worldForCountdown = world;
        final Store<EntityStore> storeFinal = store;
        final Ref<EntityStore> playerRefFinal = playerRef;
        final Vector3i blockPosFinal = blockPos;
        final CometTier tierFinal = tier;

        // 3 second countdown: show "3", "2", "1" then spawn wave
        for (int i = 3; i >= 1; i--) {
            final int count = i;
            com.hypixel.hytale.server.core.HytaleServer.SCHEDULED_EXECUTOR.schedule(() -> {
                worldForCountdown.execute(() -> {
                    if (!playerRefFinal.isValid())
                        return;
                    PlayerRef pr = storeFinal.getComponent(playerRefFinal, PlayerRef.getComponentType());
                    if (pr == null)
                        return;
                    EventTitleUtil.hideEventTitleFromPlayer(pr, 0.0F);
                    EventTitleUtil.showEventTitleToPlayer(
                            pr,
                            Message.raw(String.valueOf(count)),
                            Message.raw(""),
                            true,
                            null,
                            1.0F,
                            0.1F,
                            0.1F);
                });
            }, 3L - count, TimeUnit.SECONDS);
        }

        // After 3 seconds, spawn the wave
        com.hypixel.hytale.server.core.HytaleServer.SCHEDULED_EXECUTOR.schedule(() -> {
            worldForCountdown.execute(() -> {
                if (!playerRefFinal.isValid()) {
                    activeComets.remove(blockPosFinal);
                    return;
                }
                spawnWave(storeFinal, playerRefFinal, blockPosFinal, tierFinal);
            });
        }, 3L, TimeUnit.SECONDS);
    }

    /**
     * Determine tier from block type at position
     */
    private CometTier determineTierFromBlock(com.hypixel.hytale.server.core.universe.world.World world,
            Vector3i blockPos) {
        try {
            com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockType blockType = world
                    .getBlockType(blockPos.x, blockPos.y, blockPos.z);
            if (blockType != null) {
                String blockId = blockType.getId();
                for (CometTier tier : CometTier.values()) {
                    if (blockId.equals(tier.getBlockId("Comet_Stone"))) {
                        return tier;
                    }
                }
            }
        } catch (Exception e) {
            LOGGER.warning("Error determining tier from block: " + e.getMessage());
        }
        return CometTier.UNCOMMON; // Default
    }

    /**
     * Helper to spawn an NPC and register global tier-based stat modifiers.
     * This wraps the standard spawnNPC call and adds stat modifier registration.
     * 
     * @param store     The entity store
     * @param npcPlugin The NPC plugin
     * @param npcType   The full NPC type string (with tier suffix)
     * @param baseMobId The base mob ID (without tier suffix)
     * @param spawnPos  The spawn position
     * @param rotation  The rotation
     * @param themeId   The theme ID (kept for compatibility with existing call sites)
     * @param tier      The comet tier
     * @param zoneLevel The comet zone level/index
     * @param isBoss    Whether this is a boss spawn
     * @return The spawn result pair, or null if failed
     */
    private Pair<Ref<EntityStore>, com.hypixel.hytale.server.core.universe.world.npc.INonPlayerCharacter> spawnCometNPC(
            Store<EntityStore> store,
            NPCPlugin npcPlugin,
            String npcType,
            String baseMobId,
            Vector3d spawnPos,
            Vector3f rotation,
            String themeId,
            CometTier tier,
            int zoneLevel,
            boolean isBoss) {

        try {
            Pair<Ref<EntityStore>, com.hypixel.hytale.server.core.universe.world.npc.INonPlayerCharacter> result = npcPlugin
                    .spawnNPC(store, npcType, null, spawnPos, rotation);

            if (result != null && result.first() != null) {
                // Apply global tier stat scaling (vanilla base + per-tier increase)
                try {
                    float[] multipliers = null;
                    if (isBoss) {
                        multipliers = WaveThemeProvider.getBossStatMultipliers(themeId, tier, baseMobId, zoneLevel);
                    } else {
                        multipliers = WaveThemeProvider.getMobStatMultipliers(themeId, tier, baseMobId, zoneLevel);
                    }

                    if (multipliers != null && multipliers.length >= 4) {
                        float hpMult = multipliers[0];
                        float damageMult = multipliers[1];
                        float scaleMult = multipliers[2];
                        float speedMult = multipliers[3];

                        // Get the UUID from the spawned entity
                        com.hypixel.hytale.server.core.entity.UUIDComponent uuidComp = store.getComponent(
                                result.first(),
                                com.hypixel.hytale.server.core.entity.UUIDComponent.getComponentType());

                        if (uuidComp != null) {
                            UUID npcUUID = uuidComp.getUuid();
                            // Call directly to apply modifiers immediately (fixes timing issue)
                            CometStatModifierSystem.applyModifiers(store, result.first(), hpMult, damageMult,
                                    scaleMult, speedMult);
                            LOGGER.info("[CometWave] Applied tier stat scaling for " + npcType +
                                    " (UUID: " + npcUUID + "): HP=" + hpMult + "x, Dmg=" + damageMult + "x, Scale="
                                    + scaleMult + "x, Speed=" + speedMult + "x, ZoneLevel=" + zoneLevel);
                        }
                    }
                } catch (Exception e) {
                    LOGGER.warning(
                            "[CometWave] Could not register stat modifiers for " + npcType + ": " + e.getMessage());
                }
            }

            return result;
        } catch (Exception e) {
            LOGGER.warning("[CometWave] Exception spawning NPC " + npcType + ": " + e.getMessage());
            return null;
        }
    }

    private void spawnWave(Store<EntityStore> store, Ref<EntityStore> playerRef, Vector3i blockPos, CometTier tier) {
        if (CometConfig.DEBUG) {
            LOGGER.info("[CometDebug] spawnWave at " + blockPos + " tier=" + (tier != null ? tier.getName() : "null"));
        }
        NPCPlugin npcPlugin = NPCPlugin.get();
        if (npcPlugin == null) {
            LOGGER.warning("NPCPlugin not available!");
            return;
        }

        Vector3d centerPos = new Vector3d(blockPos.x + 0.5, blockPos.y + 1, blockPos.z + 0.5);
        WaveData waveData = new WaveData(blockPos, playerRef, store);
        activeWaves.put(blockPos, waveData);

        // Select theme and get mob list based on tier.
        String themeId = themeSelectionService.selectThemeId(
                blockPos,
                tier,
                forcedThemes,
                LOGGER);
        if (themeId == null || themeId.isBlank()) {
            LOGGER.severe("Could not resolve a theme for tier " + tier.getName() + " at " + blockPos);
            activeWaves.remove(blockPos);
            return;
        }
        if (CometConfig.DEBUG) {
            LOGGER.info("[CometDebug] spawnWave theme=" + themeId + " at " + blockPos);
        }

        cometThemes.put(blockPos, themeId);

        // Initialize wave count from theme config (multi-wave support)
        waveData.totalWaveCount = WaveThemeProvider.getWaveCount(themeId);
        waveData.currentWaveIndex = 0;
        waveData.currentWave = 1;
        LOGGER.info("Theme '" + themeId + "' has " + waveData.totalWaveCount + " waves (" +
                WaveThemeProvider.getNormalWaveCount(themeId) + " normal, " +
                WaveThemeProvider.getBossWaveCount(themeId) + " boss)");

        // Get mob list for wave 0 (first wave)
        String[] mobList = WaveThemeProvider.getMobListForWave(tier, themeId, 0);

        // Store theme name for display
        waveData.themeName = WaveThemeProvider.getThemeName(themeId);
        LOGGER.info("Selected theme: " + waveData.themeName + " (ID: " + themeId + ") for tier " + tier.getName());

        int zoneLevel = Math.max(0, cometZones.getOrDefault(blockPos, 0));

        if (mobList == null || mobList.length == 0) {
            LOGGER.warning("No mobs available for tier " + tier.getName() + " theme " + themeId);
            resolveWaveWithoutSpawns(store, playerRef, waveData,
                    "Wave 1 has no spawnable mobs for theme '" + themeId + "' at tier " + tier.getName() + ".");
            return;
        }

        // Get tier-specific spawn radius from config
        double[] radiusRange = WaveThemeProvider.getSpawnRadius(tier);
        double minRadius = radiusRange[0];
        double maxRadius = radiusRange[1];

        // Use actual mobList length for tier-specific counts
        int waveMobCount = mobList.length;

        // Shuffle mobList to randomize spawn order while maintaining exact counts
        java.util.List<String> mobListShuffled = new java.util.ArrayList<>(java.util.Arrays.asList(mobList));
        java.util.Collections.shuffle(mobListShuffled, RANDOM);
        mobList = mobListShuffled.toArray(new String[0]);

        // Track ranged mobs - max 1 ranged per wave for ALL tiers
        int rangedCount = 0;
        // All ranged mob identifiers across all tiers
        String[] rangedMobs = {
                "Archer", "Archmage", "Lobber", "Shaman", "Mage", "Ranger",
                "Hunter", "Stalker", "Priest", "Gunner", "Alchemist"
        };

        com.hypixel.hytale.server.core.universe.world.World world = null;
        try {
            world = ((com.hypixel.hytale.server.core.universe.world.storage.EntityStore) store.getExternalData())
                    .getWorld();
        } catch (Exception e) {
            LOGGER.warning("Could not get World for mob spawn validation: " + e.getMessage());
        }

        List<Vector3d> successPositions = new ArrayList<>();
        List<FailedSpawnInfo> failedSpawns = new ArrayList<>();

        // Spawn mobs from configured wave composition in a circle around the comet
        for (int i = 0; i < waveMobCount; i++) {
            double angle = (2.0 * Math.PI * i) / waveMobCount;
            double radius = minRadius + (Math.random() * (maxRadius - minRadius));
            double x = centerPos.x + Math.cos(angle) * radius;
            double y = centerPos.y;
            double z = centerPos.z + Math.sin(angle) * radius;

            Vector3d spawnPos = new Vector3d(x, y, z);
            Vector3f rotation = new Vector3f(0.0f, (float) (angle + Math.PI), 0.0f);

            String npcType = mobList[i];
            if (rangedCount >= MAX_RANGED_PER_WAVE) {
                String nonRangedType = null;
                for (String mob : mobList) {
                    boolean isRanged = false;
                    for (String ranged : rangedMobs) {
                        if (mob.contains(ranged)) {
                            isRanged = true;
                            break;
                        }
                    }
                    if (!isRanged) {
                        nonRangedType = mob;
                        break;
                    }
                }
                if (nonRangedType != null) {
                    npcType = nonRangedType;
                }
            }
            for (String ranged : rangedMobs) {
                if (npcType.contains(ranged)) {
                    rangedCount++;
                    break;
                }
            }

            Vector3d toSpawn = spawnPos;
            if (world != null) {
                Vector3d v = CometSpawnUtil.findValidMobSpawn(world, spawnPos, 11);
                if (v != null) {
                    toSpawn = v;
                } else {
                    failedSpawns.add(new FailedSpawnInfo(npcType, rotation, isRangedMob(npcType)));
                    LOGGER.info("No valid mob spawn at " + spawnPos + ", will retry near success: " + npcType);
                    continue;
                }
            }

            Pair<Ref<EntityStore>, com.hypixel.hytale.server.core.universe.world.npc.INonPlayerCharacter> result = spawnCometNPC(
                    store, npcPlugin, npcType, npcType, toSpawn, rotation, themeId, tier, zoneLevel, false);
            if (result != null && result.first() != null) {
                waveData.spawnedMobs.add(result.first());
                successPositions.add(toSpawn);
                LOGGER.info("Spawned " + npcType + " at " + toSpawn);
            } else {
                LOGGER.warning("Failed to spawn NPC: " + npcType);
            }
        }

        // Retry failed spawns near a successful one
        if (!failedSpawns.isEmpty() && !successPositions.isEmpty() && world != null) {
            for (FailedSpawnInfo f : failedSpawns) {
                Vector3d base = successPositions.get(RANDOM.nextInt(successPositions.size()));
                double dx = (RANDOM.nextBoolean() ? 1 : -1) * (0.5 + RANDOM.nextDouble());
                double dz = (RANDOM.nextBoolean() ? 1 : -1) * (0.5 + RANDOM.nextDouble());
                Vector3d newPref = new Vector3d(base.x + dx, base.y, base.z + dz);
                Vector3d retryPos = CometSpawnUtil.findValidMobSpawn(world, newPref, 11);
                if (retryPos != null) {
                    // Mob IDs are base IDs without tier suffixes
                    Pair<Ref<EntityStore>, com.hypixel.hytale.server.core.universe.world.npc.INonPlayerCharacter> res = spawnCometNPC(
                            store, npcPlugin, f.npcType, f.npcType, retryPos, f.rotation, themeId, tier, zoneLevel, false);
                    if (res != null && res.first() != null) {
                        waveData.spawnedMobs.add(res.first());
                        successPositions.add(retryPos);
                        LOGGER.info("Spawned " + f.npcType + " at " + retryPos + " (retry near success)");
                    }
                }
            }
        }

        // Store the actual number of mobs that were successfully spawned
        waveData.initialSpawnCount = waveData.spawnedMobs.size();
        waveData.previousRemainingCount = waveData.initialSpawnCount;
        LOGGER.info("Successfully spawned " + waveData.initialSpawnCount + " mobs out of " + waveMobCount
                + " attempted for tier " + tier.getName() + " theme " + themeId);

        // Start tracking and updating UI
        waveData.lastTimerUpdate = 0;
        updateWaveCountdown(store, playerRef, waveData);
    }

    /**
     * Get tier-specific timeout in milliseconds
     */
    public void updateWaveCountdown(Store<EntityStore> store, Ref<EntityStore> playerRef, WaveData waveData) {
        if (store == null) {
            LOGGER.warning("Cannot update wave countdown at " + waveData.blockPos + ": store is null");
            return;
        }

        // Attempt to re-find the player if their reference is invalid (e.g. died and
        // respawned)
        Ref<EntityStore> currentPlayerRef = playerRef;
        if (currentPlayerRef == null || !currentPlayerRef.isValid()) {
            java.util.UUID ownerUUID = cometOwners.get(waveData.blockPos);
            if (ownerUUID != null) {
                try {
                    com.hypixel.hytale.server.core.universe.world.World world = ((com.hypixel.hytale.server.core.universe.world.storage.EntityStore) store
                            .getExternalData()).getWorld();
                    for (com.hypixel.hytale.server.core.entity.entities.Player p : world.getPlayers()) {
                        if (ownerUUID.equals(p.getUuid())) {
                            currentPlayerRef = p.getReference();
                            LOGGER.info("[CometWaveManager] Re-synchronized player " + p.getDisplayName()
                                    + " for wave at " + waveData.blockPos + " after respawn");
                            // We can't update waveData.playerRef because it's final (usually),
                            // but we can use currentPlayerRef for this run.
                            break;
                        }
                    }
                } catch (Exception e) {
                    LOGGER.warning("Error re-synchronizing player: " + e.getMessage());
                }
            }
        }

        // PlayerRef can be null when player is dead (component may be removed/invalid);
        // we still run
        // mob counting and completion so loot/finish trigger. UI updates are skipped
        // when null.
        PlayerRef playerRefComponent = (currentPlayerRef != null && currentPlayerRef.isValid())
                ? store.getComponent(currentPlayerRef, PlayerRef.getComponentType())
                : null;

        // Count remaining mobs - check for DeathComponent (more reliable than
        // EntityRemoveEvent)
        // Remove dead mobs (supports DeathComponent and health-based death states).
        int beforeCleanup = waveData.spawnedMobs.size();
        waveData.spawnedMobs.removeIf(ref -> isTrackedMobDead(store, ref));
        int afterCleanup = waveData.spawnedMobs.size();
        if (beforeCleanup != afterCleanup) {
            LOGGER.info("Cleaned up " + (beforeCleanup - afterCleanup) + " dead/invalid mob refs");
        }

        // Count remaining alive mobs
        int remaining = 0;
        for (Ref<EntityStore> mobRef : waveData.spawnedMobs) {
            if (!isTrackedMobDead(store, mobRef)) {
                remaining++;
            }
        }

        LOGGER.info("Wave at " + waveData.blockPos + ": " + remaining + " mobs remaining (out of "
                + waveData.spawnedMobs.size() + " in list)");

        // Check if mob count changed (real-time detection)
        boolean mobCountChanged = (remaining != waveData.previousRemainingCount);
        waveData.remainingCount = remaining;
        waveData.previousRemainingCount = remaining;

        // Get tier-specific timeout from config
        CometTier tier = cometTiers.getOrDefault(waveData.blockPos, CometTier.UNCOMMON);
        long tierTimeout = WaveThemeProvider.getTimeoutMillis(tier);

        // Check if wave has exceeded tier-specific timeout
        long currentTime = System.currentTimeMillis();
        long elapsedTime = currentTime - waveData.startTime;
        long remainingTime = tierTimeout - elapsedTime;

        if (remainingTime <= 0) {
            LOGGER.warning("Wave at " + waveData.blockPos + " exceeded " + (tierTimeout / 1000)
                    + " second timeout! Destroying comet.");
            // Timeout reached - destroy comet and clean up
            // Must execute on WorldThread
            try {
                com.hypixel.hytale.server.core.universe.world.World world = ((com.hypixel.hytale.server.core.universe.world.storage.EntityStore) store
                        .getExternalData()).getWorld();
                final Store<EntityStore> finalStore = store;
                final WaveData finalWaveData = waveData;
                world.execute(() -> {
                    destroyCometOnTimeout(finalStore, finalWaveData);
                });
            } catch (Exception e) {
                LOGGER.warning("Error executing timeout on WorldThread: " + e.getMessage());
            }
            return;
        }

        // Calculate killed count using actual initial spawn count
        int totalMobs = waveData.initialSpawnCount;
        int killedMobs = totalMobs - remaining;

        // Calculate remaining time in seconds
        int remainingSeconds = (int) (remainingTime / 1000);
        String timeText = remainingSeconds + "s";

        // Always update when mob count changes (real-time), or update timer every second
        long timeSinceLastUpdate = currentTime - waveData.lastTimerUpdate;
        boolean shouldUpdateTimer = mobCountChanged || (timeSinceLastUpdate >= 1000);

        if (shouldUpdateTimer) {
            // Update last timer update time (always, so periodic logic works)
            waveData.lastTimerUpdate = currentTime;

            // Only update title if player is available (dead players have null PlayerRef)
            if (playerRefComponent != null) {
                Message primaryTitle;
                Message secondaryTitle;

                // Determine wave type for display
                String themeId = cometThemes.get(waveData.blockPos);
                boolean isBossWave = WaveThemeProvider.isWaveBoss(themeId, waveData.currentWaveIndex);

                CometConfig cfg = CometConfig.getInstance();

                if (isBossWave) {
                    // Boss wave display
                    String titleTemplate = (cfg != null ? cfg.msgWaveBossTitle : "Boss Wave %currentWave%/%totalWaves%");
                    String titleNoCountTemplate = (cfg != null ? cfg.msgWaveBossTitleNoCount : "Boss Wave!");
                    String subtitleTemplate = (cfg != null ? cfg.msgWaveBossSubtitle : "Boss: %bossStatus% | Time: %time%");

                    String waveLabelTemplate = waveData.totalWaveCount > 2 ? titleTemplate : titleNoCountTemplate;
                    String waveLabel = waveLabelTemplate
                            .replace("%currentWave%", Integer.toString(waveData.currentWave))
                            .replace("%totalWaves%", Integer.toString(waveData.totalWaveCount))
                            .replace("%theme%", waveData.themeName != null ? waveData.themeName : "");

                    String bossStatus = remaining > 0 ? "Alive" : "Defeated";
                    String secondaryText = subtitleTemplate
                            .replace("%bossStatus%", bossStatus)
                            .replace("%time%", timeText);

                    primaryTitle = Message.raw(waveLabel);
                    secondaryTitle = Message.raw(secondaryText);
                } else {
                    // Normal wave display
                    String titleTemplate = (cfg != null ? cfg.msgWaveTitle
                            : "Wave %currentWave%/%totalWaves% - %theme%");
                    String titleNoCountTemplate = (cfg != null ? cfg.msgWaveTitleNoCount
                            : "%theme% Incoming!");
                    String subtitleTemplate = (cfg != null ? cfg.msgWaveSubtitle
                            : "Mobs: %killed%/%total% | Time: %time%");

                    String waveLabelTemplate = waveData.totalWaveCount > 2 ? titleTemplate : titleNoCountTemplate;
                    String waveLabel = waveLabelTemplate
                            .replace("%currentWave%", Integer.toString(waveData.currentWave))
                            .replace("%totalWaves%", Integer.toString(waveData.totalWaveCount))
                            .replace("%theme%", waveData.themeName != null ? waveData.themeName : "");

                    String secondaryText = subtitleTemplate
                            .replace("%killed%", Integer.toString(killedMobs))
                            .replace("%total%", Integer.toString(totalMobs))
                            .replace("%time%", timeText);

                    primaryTitle = Message.raw(waveLabel);
                    secondaryTitle = Message.raw(secondaryText);
                }

                LOGGER.info("Updating title: Wave=" + waveData.currentWave + "/" + waveData.totalWaveCount +
                        " (boss=" + isBossWave + ") | Mobs=" + killedMobs + "/" + totalMobs +
                        " | Time: " + timeText + (mobCountChanged ? " (mob died - real-time)" : " (periodic)"));

                EventTitleUtil.hideEventTitleFromPlayer(playerRefComponent, 0.0F);
                EventTitleUtil.showEventTitleToPlayer(
                        playerRefComponent,
                        primaryTitle,
                        secondaryTitle,
                        true,
                        null,
                        999.0F,
                        0.0F,
                        0.0F);
            }
        }

        // If all mobs are dead, check if we need to spawn next wave or complete
        if (remaining == 0) {
            if (waveData.hasMoreWaves()) {
                // More waves to spawn - advance to next wave
                LOGGER.info("=== Wave " + waveData.currentWave + " complete! Spawning wave " +
                        (waveData.currentWave + 1) + "/" + waveData.totalWaveCount + " at " + waveData.blockPos + " ===");
                spawnNextWave(store, playerRef, waveData);
                // Don't continue processing - spawnNextWave will handle the next update
                return;
            } else {
                // All waves complete - finish the comet
                LOGGER.info("=== All " + waveData.totalWaveCount + " waves defeated! Completing comet at " +
                        waveData.blockPos + " ===");
                completeWave(store, playerRefComponent, waveData);
            }
        }
        // Note: Timer updates when mobs die (real-time) and via the periodic
        // checkTimeouts refresh pass.
    }

    private boolean isTrackedMobDead(Store<EntityStore> store, Ref<EntityStore> mobRef) {
        if (mobRef == null || !mobRef.isValid()) {
            return true;
        }

        try {
            com.hypixel.hytale.server.core.modules.entity.damage.DeathComponent deathComponent = store.getComponent(mobRef,
                    com.hypixel.hytale.server.core.modules.entity.damage.DeathComponent.getComponentType());
            if (deathComponent != null) {
                return true;
            }

            // If a compatibility mod swapped this away from an NPC archetype, stop tracking it.
            com.hypixel.hytale.server.npc.entities.NPCEntity npc = store.getComponent(
                    mobRef,
                    com.hypixel.hytale.server.npc.entities.NPCEntity.getComponentType());
            if (npc == null) {
                return true;
            }

            // Some combat mods can keep entities alive without DeathComponent; treat <= 0 HP as dead.
            com.hypixel.hytale.server.core.modules.entitystats.EntityStatMap statMap = store.getComponent(
                    mobRef,
                    com.hypixel.hytale.server.core.modules.entitystats.EntityStatMap.getComponentType());
            if (statMap != null) {
                int healthIndex = com.hypixel.hytale.server.core.modules.entitystats.asset.EntityStatType.getAssetMap()
                        .getIndex("Health");
                if (healthIndex >= 0 && statMap.get(healthIndex).get() <= 0.01f) {
                    return true;
                }
            }
        } catch (Exception e) {
            LOGGER.fine("Failed dead-check for tracked mob: " + e.getMessage());
        }

        return false;
    }

    private java.util.UUID getEntityUuid(Store<EntityStore> store, Ref<EntityStore> ref) {
        if (store == null || ref == null || !ref.isValid()) {
            return null;
        }
        try {
            com.hypixel.hytale.server.core.entity.UUIDComponent uuidComponent = store.getComponent(
                    ref,
                    com.hypixel.hytale.server.core.entity.UUIDComponent.getComponentType());
            return uuidComponent != null ? uuidComponent.getUuid() : null;
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Destroy comet when wave times out (1 minute elapsed)
     */
    private void destroyCometOnTimeout(Store<EntityStore> store, WaveData waveData) {
        Vector3i blockPos = waveData.blockPos;
        LOGGER.info("Destroying comet at " + blockPos + " due to timeout");

        // Despawn all spawned mobs (wave 1 and/or boss) when the wave fails
        int despawned = 0;
        try {
            com.hypixel.hytale.component.CommandBuffer<EntityStore> cb = com.cometmod.util.CommandBufferUtil.take(store);
            if (cb != null) {
                for (Ref<EntityStore> mobRef : waveData.spawnedMobs) {
                    if (mobRef != null && mobRef.isValid()) {
                        try {
                            cb.removeEntity(mobRef, com.hypixel.hytale.component.RemoveReason.REMOVE);
                            despawned++;
                        } catch (Exception e) {
                            LOGGER.warning("Failed to remove mob on wave fail: " + e.getMessage());
                        }
                    }
                }
                com.cometmod.util.CommandBufferUtil.consume(cb);
            }
        } catch (Exception e) {
            LOGGER.warning("Could not get CommandBuffer to despawn mobs on wave fail: " + e.getMessage());
        }
        if (despawned > 0) {
            LOGGER.info("Despawned " + despawned + " mobs due to wave failure at " + blockPos);
        }

        // Remove from active tracking
        activeComets.remove(blockPos);
        activeWaves.remove(blockPos);

        // Break the comet block
        try {
            com.hypixel.hytale.server.core.universe.world.World world = ((com.hypixel.hytale.server.core.universe.world.storage.EntityStore) store
                    .getExternalData()).getWorld();

            // Remove map marker (must be done before removing cometOwners)
            removeCometMapMarker(world, blockPos);
            world.breakBlock(blockPos.x, blockPos.y, blockPos.z, 0);
            LOGGER.info("Broke comet block at " + blockPos + " due to timeout");

        } catch (Exception e) {
            LOGGER.severe("Error breaking comet block on timeout: " + e.getMessage());
            e.printStackTrace();
        } finally {
            // Always clear all tracking (including forced theme and zone) on timeout cleanup.
            waveState.clearForBlock(blockPos);
            CometDespawnTracker.getInstance().unregisterComet(blockPos);
        }

        // Show "Wave Failed!" message and hide the "Wave Active!" title
        if (waveData.playerRef != null && waveData.playerRef.isValid()) {
            try {
                PlayerRef playerRefComponent = store.getComponent(waveData.playerRef, PlayerRef.getComponentType());
                if (playerRefComponent != null) {
                    // Hide the current "Wave Active!" title first
                    EventTitleUtil.hideEventTitleFromPlayer(playerRefComponent, 0.0F);

                    // Show "Wave Failed!" message
                    CometConfig cfg = CometConfig.getInstance();
                    String titleTemplate = (cfg != null ? cfg.msgWaveFailedTitle : "Wave Failed!");
                    String subtitleTemplate = (cfg != null ? cfg.msgWaveFailedSubtitle : "Time's Up!");

                    Message primaryTitle = Message.raw(titleTemplate);
                    Message secondaryTitle = Message.raw(subtitleTemplate);

                    EventTitleUtil.showEventTitleToPlayer(
                            playerRefComponent,
                            primaryTitle,
                            secondaryTitle,
                            true, // isMajor = true to make it prominent
                            null,
                            3.0F, // Show for 3 seconds
                            0.0F, // No fade in
                            0.5F // Fade out
                    );

                    LOGGER.info("Showed 'Wave Failed!' message on timeout");

                    // Schedule hide after 3 seconds
                    com.hypixel.hytale.server.core.universe.world.World world = ((com.hypixel.hytale.server.core.universe.world.storage.EntityStore) store
                            .getExternalData()).getWorld();

                    final PlayerRef finalPlayerRef = playerRefComponent;
                    com.hypixel.hytale.server.core.HytaleServer.SCHEDULED_EXECUTOR.schedule(() -> {
                        try {
                            world.execute(() -> {
                                EventTitleUtil.hideEventTitleFromPlayer(finalPlayerRef, 0.0F);
                                LOGGER.info("Hid 'Wave Failed!' message after 3 seconds");
                            });
                        } catch (Exception e) {
                            LOGGER.warning("Error hiding failed message: " + e.getMessage());
                        }
                    }, 3L, java.util.concurrent.TimeUnit.SECONDS);
                }
            } catch (Exception e) {
                LOGGER.warning("Error showing failed message on timeout: " + e.getMessage());
            }
        }
    }

    /**
     * Spawn the next wave in a multi-wave encounter.
     * Determines wave type and calls appropriate spawn method.
     */
    private void spawnNextWave(Store<EntityStore> store, Ref<EntityStore> playerRef, WaveData waveData) {
        // Advance to next wave
        waveData.advanceToNextWave();

        Vector3i blockPos = waveData.blockPos;
        String themeId = cometThemes.get(blockPos);
        if (themeId == null) themeId = "skeleton";

        CometTier tier = cometTiers.getOrDefault(blockPos, CometTier.UNCOMMON);
        int waveIndex = waveData.currentWaveIndex;

        LOGGER.info("=== SPAWNING WAVE " + waveData.currentWave + "/" + waveData.totalWaveCount +
                " (index " + waveIndex + ") ===");

        // Check wave type and spawn accordingly
        if (WaveThemeProvider.isWaveBoss(themeId, waveIndex)) {
            // Boss wave
            LOGGER.info("Wave " + waveData.currentWave + " is a BOSS wave");
            spawnBossWaveAtIndex(store, playerRef, waveData, waveIndex);
        } else {
            // Normal wave (mob wave)
            LOGGER.info("Wave " + waveData.currentWave + " is a NORMAL wave");
            spawnNormalWaveAtIndex(store, playerRef, waveData, waveIndex);
        }
    }

    /**
     * Spawn a normal (mob) wave at a specific wave index.
     */
    private void spawnNormalWaveAtIndex(Store<EntityStore> store, Ref<EntityStore> playerRef,
            WaveData waveData, int waveIndex) {
        NPCPlugin npcPlugin = NPCPlugin.get();
        if (npcPlugin == null) {
            LOGGER.severe("NPCPlugin not available for normal wave!");
            return;
        }

        Vector3i blockPos = waveData.blockPos;
        CometTier tier = cometTiers.getOrDefault(blockPos, CometTier.UNCOMMON);
        String themeId = cometThemes.get(blockPos);
        if (themeId == null) themeId = "skeleton";
        int zoneLevel = Math.max(0, cometZones.getOrDefault(blockPos, 0));

        // Get mob list for this wave
        String[] mobList = WaveThemeProvider.getMobListForWave(tier, themeId, waveIndex);
        if (mobList == null || mobList.length == 0) {
            LOGGER.warning("No mobs found for wave " + waveData.currentWave + " in theme " + themeId);
            resolveWaveWithoutSpawns(store, playerRef, waveData,
                    "Normal wave " + waveData.currentWave + " has no spawnable mobs.");
            return;
        }

        LOGGER.info("Spawning " + mobList.length + " mobs for wave " + waveData.currentWave);

        // Get spawn radius
        double[] radiusRange = WaveThemeProvider.getSpawnRadius(tier);
        double minRadius = radiusRange[0];
        double maxRadius = radiusRange[1];

        Vector3d centerPos = new Vector3d(blockPos.x + 0.5, blockPos.y + 1, blockPos.z + 0.5);

        // Shuffle for randomization
        java.util.List<String> mobListShuffled = new java.util.ArrayList<>(java.util.Arrays.asList(mobList));
        java.util.Collections.shuffle(mobListShuffled, RANDOM);

        com.hypixel.hytale.server.core.universe.world.World world = null;
        try {
            world = ((com.hypixel.hytale.server.core.universe.world.storage.EntityStore) store.getExternalData())
                    .getWorld();
        } catch (Exception e) {
            LOGGER.warning("Could not get World for mob spawn validation: " + e.getMessage());
        }

        int rangedCount = 0;
        String[] rangedMobs = { "Archer", "Archmage", "Lobber", "Shaman", "Mage", "Ranger",
                "Hunter", "Stalker", "Priest", "Gunner", "Alchemist" };

        List<Vector3d> successPositions = new ArrayList<>();
        List<FailedSpawnInfo> failedSpawns = new ArrayList<>();

        for (int i = 0; i < mobListShuffled.size(); i++) {
            String npcType = mobListShuffled.get(i);

            // Enforce ranged limit
            if (rangedCount >= MAX_RANGED_PER_WAVE) {
                boolean isRanged = false;
                for (String ranged : rangedMobs) {
                    if (npcType.contains(ranged)) {
                        isRanged = true;
                        break;
                    }
                }
                if (isRanged) {
                    // Find a non-ranged replacement
                    for (String mob : mobListShuffled) {
                        boolean mobIsRanged = false;
                        for (String ranged : rangedMobs) {
                            if (mob.contains(ranged)) {
                                mobIsRanged = true;
                                break;
                            }
                        }
                        if (!mobIsRanged) {
                            npcType = mob;
                            break;
                        }
                    }
                }
            }

            // Count ranged
            for (String ranged : rangedMobs) {
                if (npcType.contains(ranged)) {
                    rangedCount++;
                    break;
                }
            }

            double angle = (2.0 * Math.PI * i) / mobListShuffled.size();
            double radius = minRadius + (RANDOM.nextDouble() * (maxRadius - minRadius));
            Vector3d spawnPos = new Vector3d(centerPos.x + Math.cos(angle) * radius, centerPos.y,
                    centerPos.z + Math.sin(angle) * radius);

            Vector3d toSpawn = spawnPos;
            if (world != null) {
                Vector3d v = CometSpawnUtil.findValidMobSpawn(world, spawnPos, 11);
                if (v != null) {
                    toSpawn = v;
                } else {
                    failedSpawns.add(new FailedSpawnInfo(npcType,
                            new Vector3f(0.0f, (float) (angle + Math.PI), 0.0f), isRangedMob(npcType)));
                    continue;
                }
            }

            Vector3f rotation = new Vector3f(0.0f, (float) (angle + Math.PI), 0.0f);
            Pair<Ref<EntityStore>, com.hypixel.hytale.server.core.universe.world.npc.INonPlayerCharacter> result =
                    spawnCometNPC(store, npcPlugin, npcType, npcType, toSpawn, rotation, themeId, tier, zoneLevel, false);

            if (result != null && result.first() != null) {
                waveData.spawnedMobs.add(result.first());
                successPositions.add(toSpawn);
            }
        }

        // Retry failed spawns
        if (!failedSpawns.isEmpty() && !successPositions.isEmpty() && world != null) {
            for (FailedSpawnInfo f : failedSpawns) {
                Vector3d base = successPositions.get(RANDOM.nextInt(successPositions.size()));
                double dx = (RANDOM.nextBoolean() ? 1 : -1) * (0.5 + RANDOM.nextDouble());
                double dz = (RANDOM.nextBoolean() ? 1 : -1) * (0.5 + RANDOM.nextDouble());
                Vector3d retryPos = CometSpawnUtil.findValidMobSpawn(world,
                        new Vector3d(base.x + dx, base.y, base.z + dz), 11);
                if (retryPos != null) {
                    Pair<Ref<EntityStore>, com.hypixel.hytale.server.core.universe.world.npc.INonPlayerCharacter> res =
                            spawnCometNPC(store, npcPlugin, f.npcType, f.npcType, retryPos, f.rotation, themeId, tier, zoneLevel, false);
                    if (res != null && res.first() != null) {
                        waveData.spawnedMobs.add(res.first());
                        successPositions.add(retryPos);
                    }
                }
            }
        }

        waveData.initialSpawnCount = waveData.spawnedMobs.size();
        waveData.previousRemainingCount = waveData.initialSpawnCount;
        LOGGER.info("Spawned " + waveData.initialSpawnCount + " mobs for wave " + waveData.currentWave);

        // Start tracking and force immediate title update
        waveData.lastTimerUpdate = 0;
        updateWaveCountdown(store, playerRef, waveData);
    }

    /**
     * Spawn a boss wave at a specific wave index.
     */
    private void spawnBossWaveAtIndex(Store<EntityStore> store, Ref<EntityStore> playerRef,
            WaveData waveData, int waveIndex) {
        NPCPlugin npcPlugin = NPCPlugin.get();
        if (npcPlugin == null) {
            LOGGER.severe("NPCPlugin not available for boss wave!");
            return;
        }

        Vector3i blockPos = waveData.blockPos;
        CometTier tier = cometTiers.getOrDefault(blockPos, CometTier.UNCOMMON);
        String themeId = cometThemes.get(blockPos);
        if (themeId == null) themeId = "skeleton";
        int zoneLevel = Math.max(0, cometZones.getOrDefault(blockPos, 0));

        // Get bosses for this specific wave
        java.util.List<String> bosses = WaveThemeProvider.getBossesForWave(tier, themeId, waveIndex);
        if (bosses == null || bosses.isEmpty()) {
            java.util.List<String> fallbackBosses = WaveThemeProvider.getBossesForTheme(tier, themeId);
            if (fallbackBosses != null && !fallbackBosses.isEmpty()) {
                bosses = fallbackBosses;
                LOGGER.warning("No bosses configured for theme '" + themeId + "' wave index " + waveIndex
                        + "; falling back to theme-level bosses at tier " + tier.getName());
            } else {
                LOGGER.warning("No bosses configured for theme '" + themeId + "' wave index " + waveIndex
                        + " at tier " + tier.getName());
                resolveWaveWithoutSpawns(store, playerRef, waveData,
                        "Boss wave " + waveData.currentWave + " has no configured bosses.");
                return;
            }
        }

        LOGGER.info("Spawning " + bosses.size() + " boss(es) for wave " + waveData.currentWave);
        waveData.previousRemainingCount = bosses.size();
        waveData.initialSpawnCount = 0;

        com.hypixel.hytale.server.core.universe.world.World world = null;
        try {
            world = ((com.hypixel.hytale.server.core.universe.world.storage.EntityStore) store.getExternalData())
                    .getWorld();
        } catch (Exception e) {
            LOGGER.warning("Could not get World for boss spawn validation: " + e.getMessage());
        }

        Vector3d centerPos = new Vector3d(blockPos.x + 0.5, blockPos.y + 1, blockPos.z + 0.5);
        Vector3f rotation = new Vector3f(0.0f, 0.0f, 0.0f);
        List<Vector3d> successPositions = new ArrayList<>();
        List<String> failedBosses = new ArrayList<>();

        for (int b = 0; b < bosses.size(); b++) {
            String bossType = bosses.get(b);
            double ox = (bosses.size() > 1 && b == 1) ? 1.5 : 0;
            Vector3d pos = new Vector3d(centerPos.x + ox, centerPos.y, centerPos.z);
            Vector3d toSpawn = pos;

            if (world != null) {
                Vector3d v = CometSpawnUtil.findValidMobSpawn(world, pos, 11);
                if (v != null) {
                    toSpawn = v;
                } else {
                    failedBosses.add(bossType);
                    continue;
                }
            }

            Pair<Ref<EntityStore>, com.hypixel.hytale.server.core.universe.world.npc.INonPlayerCharacter> result =
                    spawnCometNPC(store, npcPlugin, bossType, bossType, toSpawn, rotation, themeId, tier, zoneLevel, true);

            if (result != null && result.first() != null) {
                waveData.spawnedMobs.add(result.first());
                successPositions.add(toSpawn);
            }
        }

        // Retry failed boss spawns
        if (!failedBosses.isEmpty() && !successPositions.isEmpty() && world != null) {
            for (String bossType : failedBosses) {
                Vector3d base = successPositions.get(RANDOM.nextInt(successPositions.size()));
                double dx = (RANDOM.nextBoolean() ? 1 : -1) * (0.5 + RANDOM.nextDouble());
                double dz = (RANDOM.nextBoolean() ? 1 : -1) * (0.5 + RANDOM.nextDouble());
                Vector3d retryPos = CometSpawnUtil.findValidMobSpawn(world,
                        new Vector3d(base.x + dx, base.y, base.z + dz), 11);
                if (retryPos != null) {
                    Pair<Ref<EntityStore>, com.hypixel.hytale.server.core.universe.world.npc.INonPlayerCharacter> res =
                            spawnCometNPC(store, npcPlugin, bossType, bossType, retryPos, rotation, themeId, tier, zoneLevel, true);
                    if (res != null && res.first() != null) {
                        waveData.spawnedMobs.add(res.first());
                        successPositions.add(retryPos);
                    }
                }
            }
        }

        waveData.initialSpawnCount = waveData.spawnedMobs.size();
        waveData.previousRemainingCount = waveData.initialSpawnCount;
        LOGGER.info("Spawned " + waveData.initialSpawnCount + " boss(es) for wave " + waveData.currentWave);

        // Start tracking and force immediate title update
        waveData.lastTimerUpdate = 0;
        updateWaveCountdown(store, playerRef, waveData);
    }

    private void resolveWaveWithoutSpawns(Store<EntityStore> store, Ref<EntityStore> playerRef, WaveData waveData,
            String reason) {
        if (waveData == null) {
            return;
        }

        LOGGER.warning(reason + " Continuing progression without spawned entities.");
        waveData.spawnedMobs.clear();
        waveData.initialSpawnCount = 0;
        waveData.remainingCount = 0;
        waveData.previousRemainingCount = 0;
        waveData.lastTimerUpdate = 0;

        Store<EntityStore> effectiveStore = store != null ? store : waveData.store;
        if (effectiveStore == null) {
            LOGGER.warning("Unable to progress empty wave at " + waveData.blockPos + ": store is unavailable.");
            return;
        }

        Ref<EntityStore> effectivePlayerRef = playerRef != null ? playerRef : waveData.playerRef;
        updateWaveCountdown(effectiveStore, effectivePlayerRef, waveData);
    }

    /**
     * Complete the wave: drop loot, break block, and optionally show title to the
     * player.
     *
     * @param playerRef can be null if the player is dead; loot and block break
     *                  still run,
     *                  only the completion title is skipped.
     */
    private void completeWave(Store<EntityStore> store, PlayerRef playerRef, WaveData waveData) {
        Vector3i blockPos = waveData.blockPos;
        CometTier tier = cometTiers.getOrDefault(blockPos, CometTier.UNCOMMON);
        LOGGER.info("[CometWaveManager] completeWave: Tier=" + tier.getName() + " for comet at " + blockPos);

        activeWaves.remove(blockPos);

        // Always drop items and break the block (even if player is dead)
        java.util.List<String> droppedItems = dropRewardsAndBreakBlock(store, blockPos, waveData, tier);
        LOGGER.info("[CometWaveManager] Rewards dropped: " + (droppedItems != null ? droppedItems.size() : "null")
                + " items.");

        // Show completion title only when player is available (e.g. not dead)
        if (playerRef != null) {
            // 1. Show "Wave Complete!" as main title
            CometConfig cfg = CometConfig.getInstance();
            String titleTemplate = (cfg != null ? cfg.msgWaveCompleteTitle : "Wave Complete!");
            String subtitleTemplate = (cfg != null ? cfg.msgWaveCompleteSubtitle : "Loot Dropped!");

            Message primaryTitle = Message.raw(titleTemplate);
            Message secondaryTitle = Message.raw(subtitleTemplate);

            EventTitleUtil.hideEventTitleFromPlayer(playerRef, 0.0F);
            EventTitleUtil.showEventTitleToPlayer(
                    playerRef,
                    primaryTitle,
                    secondaryTitle,
                    true, // isMajor
                    null,
                    8.0F,
                    0.2F,
                    0.5F);

            // 2. Show Loot in Chat
            String headerPrefix = (cfg != null ? cfg.msgWaveCompleteChatHeaderPrefix : "[Comet] ");
            String headerText = (cfg != null ? cfg.msgWaveCompleteChatHeader : "Wave Complete! Your rewards:");

            Message header = Message.empty()
                .insert(Message.raw(headerPrefix).color("#FFAA00"))
                .insert(Message.raw(headerText).color("#FFFFFF"));
            playerRef.sendMessage(header);
            String itemPrefix = (cfg != null ? cfg.msgWaveCompleteChatItemPrefix : " - ");
            for (String item : droppedItems) {
                Message itemMsg = Message.empty()
                    .insert(Message.raw(itemPrefix).color("#AAAAAA"))
                    .insert(Message.raw(item).color("#FFFFFF"));
                playerRef.sendMessage(itemMsg);
            }

            // 3. Schedule removal of title after 8 seconds
            try {
                com.hypixel.hytale.server.core.universe.world.World world = ((com.hypixel.hytale.server.core.universe.world.storage.EntityStore) store
                        .getExternalData()).getWorld();
                final PlayerRef finalPlayerRef = playerRef;
                com.hypixel.hytale.server.core.HytaleServer.SCHEDULED_EXECUTOR.schedule(() -> {
                    try {
                        world.execute(() -> {
                            EventTitleUtil.hideEventTitleFromPlayer(finalPlayerRef, 0.0F);
                        });
                    } catch (Exception e) {
                        LOGGER.warning("Error hiding success messages: " + e.getMessage());
                    }
                }, 8L, java.util.concurrent.TimeUnit.SECONDS);
            } catch (Exception e) {
                LOGGER.warning("Error scheduling message hide: " + e.getMessage());
            }
        } else {
            LOGGER.info("Skipping completion title (player not available); loot dropped at " + blockPos);
        }
    }

    /**
     * Generate structured rewards for a tier using config settings.
     * Combines zone base pools with tier inheritance and optional theme overrides.
     */
    private void generateTierRewards(CometTier tier, String themeId, int zoneId,
            java.util.List<com.hypixel.hytale.server.core.inventory.ItemStack> allItems,
            java.util.List<String> droppedItemIds) {
        rewardService.generateTierRewards(tier, themeId, zoneId, RANDOM, allItems, droppedItemIds, LOGGER);
    }

    /**
     * Drop rewards from droplist and break the comet block
     * 
     * @return List of item IDs that were dropped
     */
    private java.util.List<String> dropRewardsAndBreakBlock(Store<EntityStore> store, Vector3i blockPos,
            WaveData waveData, CometTier tier) {
        java.util.List<String> droppedItemIds = new java.util.ArrayList<>();
        try {
            com.hypixel.hytale.server.core.universe.world.World world = ((com.hypixel.hytale.server.core.universe.world.storage.EntityStore) store
                    .getExternalData()).getWorld();

            java.util.List<com.hypixel.hytale.server.core.inventory.ItemStack> allItems = new java.util.ArrayList<>();

            // Get theme ID for potential reward override
            String themeId = cometThemes.get(blockPos);

            // Determine zone for this comet, if known
            Integer zoneId = cometZones.get(blockPos);
            if (zoneId == null) {
                zoneId = 0;
            }

            // Generate structured rewards based on zone + tier (see REWARD_SYSTEM.md)
            generateTierRewards(tier, themeId, zoneId, allItems, droppedItemIds);

            // Add guaranteed 5 Shards (all tiers)
            String shardId = tier.getShardId();
            allItems.add(new com.hypixel.hytale.server.core.inventory.ItemStack(shardId, 5));
            droppedItemIds.add(shardId + " x5");

            // Spawn only the chest; themed object (coffin/portal/volcano) was already there from event start. Default: break comet and place chest.
            boolean chestSpawned = CometLootChestService.getInstance().spawnChestOnlyAfterWaveComplete(world, blockPos, allItems, themeId);
            if (chestSpawned) {
                LOGGER.info("[CometWaveManager] Spawned timed reward chest at " + blockPos +
                        " with " + allItems.size() + " item stacks (expires 20s after close).");
            } else {
                // Fallback path: drop entities directly and break the block.
                com.hypixel.hytale.math.vector.Vector3d dropPosition = new com.hypixel.hytale.math.vector.Vector3d(
                        blockPos.x + 0.5D, blockPos.y + 0.5D, blockPos.z + 0.5D);

                LOGGER.info("[CometWaveManager] Chest spawn failed; generating direct item drops for " + allItems.size()
                        + " item stacks.");
                com.hypixel.hytale.component.Holder<com.hypixel.hytale.server.core.universe.world.storage.EntityStore>[] itemEntityHolders = com.hypixel.hytale.server.core.modules.entity.item.ItemComponent
                        .generateItemDrops(
                                store,
                                allItems,
                                dropPosition,
                                com.hypixel.hytale.math.vector.Vector3f.ZERO);

                if (itemEntityHolders != null && itemEntityHolders.length > 0) {
                    for (com.hypixel.hytale.component.Holder<com.hypixel.hytale.server.core.universe.world.storage.EntityStore> holder : itemEntityHolders) {
                        if (holder != null) {
                            store.addEntity(holder, com.hypixel.hytale.component.AddReason.SPAWN);
                        }
                    }
                    LOGGER.info("[CometWaveManager] Dropped " + itemEntityHolders.length + " item entities at " + blockPos);
                } else {
                    LOGGER.warning(
                            "[CometWaveManager] No item entity holders generated for " + allItems.size() + " items!");
                }

                // Break the comet block after fallback drops.
                world.breakBlock(blockPos.x, blockPos.y, blockPos.z, 0);
                LOGGER.info("Broke comet block at " + blockPos + " after dropping rewards");
            }

            // Remove map marker and clean up all tracking so the marker disappears and
            // CometMarkerProvider stops including it
            removeCometMapMarker(world, blockPos);
            waveState.clearForBlock(blockPos);

            // Unregister from despawn tracker
            CometDespawnTracker.getInstance().unregisterComet(blockPos);

            // Note: Title will auto-hide after its duration (5 seconds) set in
            // completeWave()
            // No need to manually schedule a hide - the EventTitleUtil handles it
            // automatically

        } catch (Exception e) {
            LOGGER.severe("Error dropping rewards and breaking block: " + e.getMessage());
            e.printStackTrace();
        }

        return droppedItemIds;
    }

    /**
     * Register tier for a comet block (called when block is spawned)
     * 
     * @param world     The world the comet is in
     * @param blockPos  The block position of the comet
     * @param tier      The tier of the comet
     * @param ownerUUID The UUID of the player who owns this comet (for marker
     *                  visibility)
     */
    public void registerCometTier(com.hypixel.hytale.server.core.universe.world.World world, Vector3i blockPos,
            CometTier tier, java.util.UUID ownerUUID) {
        waveState.registerCometTier(blockPos, tier, ownerUUID, LOGGER);

        // Add map marker for this comet to the specific player only
        addCometMapMarker(world, blockPos, tier, ownerUUID);
    }

    /**
     * Register the originating zone ID for a comet block.
     */
    public void registerCometZone(Vector3i blockPos, int zoneId) {
        waveState.registerCometZone(blockPos, zoneId);
    }

    /**
     * Add a map marker for a comet to the specified world (only visible to owner)
     * 
     * @param world     The world
     * @param blockPos  The comet block position
     * @param tier      The comet tier
     * @param ownerUUID The UUID of the player who owns this comet (marker only
     *                  visible to them)
     */
    private void addCometMapMarker(com.hypixel.hytale.server.core.universe.world.World world, Vector3i blockPos,
            CometTier tier, java.util.UUID ownerUUID) {
        waveUiService.addCometMapMarker(world, blockPos, tier, ownerUUID, LOGGER);
    }

    /**
     * Remove map marker for a comet when it's broken
     * Sends removal packet only to the owner player
     */
    public void removeCometMapMarker(com.hypixel.hytale.server.core.universe.world.World world, Vector3i blockPos) {
        java.util.UUID ownerUUID = cometOwners.get(blockPos);
        waveUiService.removeCometMapMarker(world, blockPos, ownerUUID, LOGGER);
    }

    /**
     * Handle block break when we have a Store (e.g. from BreakBlockEvent). Cleans
     * up tracking and removes the map marker.
     */
    public void handleBlockBreak(Store<EntityStore> store, Vector3i blockPos) {
        // Remove map marker first (needs ownerUUID which is still in cometOwners)
        try {
            com.hypixel.hytale.server.core.universe.world.World world = ((com.hypixel.hytale.server.core.universe.world.storage.EntityStore) store
                    .getExternalData()).getWorld();
            removeCometMapMarker(world, blockPos);
        } catch (Exception e) {
            LOGGER.warning("Failed to get world for map marker removal: " + e.getMessage());
        }
        // Remove from active comets and tracking
        waveState.clearForBlock(blockPos);
    }

    /**
     * Handle block break when we have World but no Store (e.g. comet despawns due
     * to timer in CometDespawnTracker or CometFallingSystem).
     * Removes the map marker and all tracking so the marker disappears and
     * CometMarkerProvider stops including it.
     */
    public void handleBlockBreak(com.hypixel.hytale.server.core.universe.world.World world, Vector3i blockPos) {
        if (world == null)
            return;
        removeCometMapMarker(world, blockPos);
        waveState.clearForBlock(blockPos);
    }

    /**
     * Set a forced theme for a specific comet block (string-based)
     * 
     * @param blockPos The block position
     * @param themeId  The theme ID (string)
     */
    public void forceTheme(Vector3i blockPos, String themeId) {
        waveState.forceTheme(blockPos, themeId, LOGGER);
    }

    /**
     * Get theme ID from name (case insensitive) - now returns string
     * 
     * @param name Theme name
     * @return Theme ID (string), or null if not found
     */
    public String getThemeIdByName(String name) {
        return WaveThemeProvider.findThemeByName(name);
    }

    /**
     * Get all valid configured theme names.
     */
    public String[] getThemeNames() {
        return WaveThemeProvider.getAllThemeNames();
    }

    /**
     * Returns true if the given entity ref is a mob spawned as part of an active comet wave.
     * Used to decide whether to suppress loot drops when disableWaveMobLoot is enabled.
     */
    public boolean isCometWaveMob(Store<EntityStore> store, Ref<EntityStore> mobRef) {
        if (store == null || mobRef == null || !mobRef.isValid()) return false;
        java.util.UUID mobUuid = getEntityUuid(store, mobRef);
        for (WaveData waveData : activeWaves.values()) {
            for (Ref<EntityStore> ref : waveData.spawnedMobs) {
                if (ref == null) continue;
                if (ref == mobRef || (ref.isValid() && ref.equals(mobRef))) return true;
                if (mobUuid != null && ref.isValid()) {
                    Store<EntityStore> compareStore = waveData.store != null ? waveData.store : ref.getStore();
                    if (mobUuid.equals(getEntityUuid(compareStore, ref))) return true;
                }
            }
        }
        return false;
    }

    public void handleMobDeath(
            com.hypixel.hytale.component.Ref<com.hypixel.hytale.server.core.universe.world.storage.EntityStore> mobRef) {
        LOGGER.info("[CometWaveManager] handleMobDeath called! Checking " + activeWaves.size() + " active waves...");
        Store<EntityStore> eventStore = (mobRef != null && mobRef.isValid()) ? mobRef.getStore() : null;
        java.util.UUID deadMobUuid = getEntityUuid(eventStore, mobRef);

        // Check all active waves to see if this mob belongs to any of them
        for (Map.Entry<com.hypixel.hytale.math.vector.Vector3i, WaveData> entry : activeWaves.entrySet()) {
            WaveData waveData = entry.getValue();
            LOGGER.info("[CometWaveManager] Checking wave at " + entry.getKey() + " with " + waveData.spawnedMobs.size()
                    + " mobs in list");

            // Check if this mob is in the list (by reference or by checking if ref matches)
            boolean found = false;
            for (int i = waveData.spawnedMobs.size() - 1; i >= 0; i--) {
                Ref<EntityStore> ref = waveData.spawnedMobs.get(i);
                // Check if refs match (same entity) - use == for reference equality or check if
                // they point to same entity
                boolean sameRef = ref != null && (ref == mobRef || ref.equals(mobRef) ||
                        (mobRef != null && ref.isValid() && mobRef.isValid() && ref.getIndex() == mobRef.getIndex()));

                boolean sameUuid = false;
                if (!sameRef && deadMobUuid != null && ref != null) {
                    Store<EntityStore> compareStore = waveData.store != null ? waveData.store
                            : (ref.isValid() ? ref.getStore() : eventStore);
                    java.util.UUID trackedUuid = getEntityUuid(compareStore, ref);
                    sameUuid = deadMobUuid.equals(trackedUuid);
                }

                if (sameRef || sameUuid) {
                    found = true;
                    waveData.spawnedMobs.remove(i);
                    LOGGER.info("[CometWaveManager] Mob died for wave at " + entry.getKey() + " (removed from list, " +
                            waveData.spawnedMobs.size() + " remaining)");
                    break;
                }
            }

            if (found) {
                // Update countdown after mob death. Use player's store if valid, else mob's
                // (e.g. player dead/DC) so completion and loot still run when boss is killed.
                com.hypixel.hytale.component.Store<com.hypixel.hytale.server.core.universe.world.storage.EntityStore> store = waveData.playerRef != null
                        && waveData.playerRef.isValid()
                                ? waveData.playerRef.getStore()
                                : ((mobRef != null && mobRef.isValid()) ? mobRef.getStore() : waveData.store);
                if (store != null) {
                    updateWaveCountdown(store, waveData.playerRef, waveData);
                } else {
                    LOGGER.warning("Could not update wave countdown after mob death at " + entry.getKey()
                            + ": store unavailable");
                }
                return; // Found and handled, exit
            }
        }

        LOGGER.info("[CometWaveManager] Mob death not found in any active wave");
    }

    public void cleanup() {
        activeComets.clear();
        activeWaves.clear();
        cometTiers.clear();
        cometOwners.clear();
        cometThemes.clear();
    }

}
