package com.cometmod.spawn;

import com.cometmod.*;
import com.cometmod.commands.*;
import com.cometmod.services.*;
import com.cometmod.spawn.*;
import com.cometmod.systems.*;
import com.cometmod.wave.*;


import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.WorldMapTracker;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.component.Store;
import com.cometmod.config.model.ZoneSpawnChances;
import com.cometmod.integration.ClaimProtectionGuard;
import com.cometmod.integration.WorldProtectRegionGuard;

import java.util.*;
import java.util.concurrent.ScheduledFuture;
import java.util.function.Consumer;
import java.util.logging.Logger;

public class CometSpawnTask {
    
    private static final Logger LOGGER = Logger.getLogger("CometSpawnTask");
    
    // Retry configuration for world thread initialization
    private static final int MAX_RETRY_ATTEMPTS = 5;
    private static final long INITIAL_RETRY_DELAY_MS = 100L; // Start with 100ms delay
    private static final int SPAWN_LOCATION_ATTEMPTS = 16;
    private static final int EXTRA_LOCATION_RECHECKS = 2;
    private static final double MIN_RECHECK_RELOCATION_DISTANCE = 64.0;

    public enum SpawnResult {
        SPAWNED,
        NO_SAFE_LOCATION,
        SKIPPED_NO_TIER,
        SKIPPED_NO_WORLD,
        SKIPPED_WORLD_DISABLED,
        ERROR
    }
    
    // Spawn timing configuration
    private int minDelaySeconds = 120;  // 2 minutes minimum
    private int maxDelaySeconds = 300;  // 5 minutes maximum
    private double spawnChance = 0.4;   // 40% chance to spawn each check
    private int minSpawnDistance = 30;  // Minimum distance from player (blocks)
    private int maxSpawnDistance = 50;  // Maximum distance from player (blocks)
    
    // Getters and setters for UI configuration
    public int getMinDelaySeconds() { return minDelaySeconds; }
    public void setMinDelaySeconds(int minDelaySeconds) {
        this.minDelaySeconds = minDelaySeconds;
    }
    
    public int getMaxDelaySeconds() { return maxDelaySeconds; }
    public void setMaxDelaySeconds(int maxDelaySeconds) {
        this.maxDelaySeconds = maxDelaySeconds;
    }
    
    public double getSpawnChance() { return spawnChance; }
    public void setSpawnChance(double spawnChance) {
        this.spawnChance = Math.max(0.0, Math.min(1.0, spawnChance));
    }
    
    public int getMinSpawnDistance() { return minSpawnDistance; }
    public void setMinSpawnDistance(int minSpawnDistance) {
        this.minSpawnDistance = minSpawnDistance;
    }
    
    public int getMaxSpawnDistance() { return maxSpawnDistance; }
    public void setMaxSpawnDistance(int maxSpawnDistance) {
        this.maxSpawnDistance = maxSpawnDistance;
    }
    
    private final Set<Player> trackedPlayers = new HashSet<>();
    private ScheduledFuture<?> future;
    private final World world;
    private final Store<EntityStore> store;
    
    public CometSpawnTask(World world, Store<EntityStore> store) {
        this.world = world;
        this.store = store;
    }
    
    public void addPlayer(Player player) {
        synchronized (trackedPlayers) {
            trackedPlayers.add(player);
        }
    }

    public void removePlayer(Player player) {
        synchronized (trackedPlayers) {
            trackedPlayers.remove(player);
        }
    }

    public void setDelayRangeSeconds(int min, int max) {
        this.minDelaySeconds = min;
        this.maxDelaySeconds = max;
    }
    
    public void start() {
        if (future != null && !future.isCancelled()) {
            return;
        }
        scheduleNextSpawn();
    }

    public void stop() {
        if (future != null) {
            future.cancel(false);
            future = null;
        }
    }

    public ScheduledFuture<?> getFuture() {
        return future;
    }

    private void scheduleNextSpawn() {
        Random random = new Random();
        long delaySeconds = minDelaySeconds + random.nextInt(maxDelaySeconds - minDelaySeconds + 1);
        
        future = com.hypixel.hytale.server.core.HytaleServer.SCHEDULED_EXECUTOR.schedule(
            this::checkAndSpawn,
            delaySeconds,
            java.util.concurrent.TimeUnit.SECONDS
        );
    }
    
    private void checkAndSpawn() {
        try {
            CometConfig config = CometConfig.getInstance();
            if (config != null && config.isProtectedZoneSpawnRulesEnabled()) {
                WorldProtectRegionGuard.syncKnownRegions(config);
            }

            // Check if natural spawns are enabled
            if (config != null && !config.naturalSpawnsEnabled) {
                scheduleNextSpawn();
                return;
            }

            Random random = new Random();

            if (random.nextDouble() > spawnChance) {
                scheduleNextSpawn();
                return;
            }

            List<Player> playersToCheck;
            synchronized (trackedPlayers) {
                if (trackedPlayers.isEmpty()) {
                    scheduleNextSpawn();
                    return;
                }
                playersToCheck = new ArrayList<>();
                for (Player trackedPlayer : trackedPlayers) {
                    if (trackedPlayer == null) {
                        continue;
                    }
                    World trackedWorld = trackedPlayer.getWorld();
                    if (trackedWorld == null) {
                        continue;
                    }
                    if (config != null && !config.isRaidEnabledInWorld(trackedWorld)) {
                        continue;
                    }
                    playersToCheck.add(trackedPlayer);
                }
            }

            if (!playersToCheck.isEmpty()) {
                Player targetPlayer = playersToCheck.get(random.nextInt(playersToCheck.size()));
                spawnForPlayer(targetPlayer);
            }

            scheduleNextSpawn();
            
        } catch (Exception e) {
            LOGGER.warning("Error in comet spawn check: " + e.getMessage());
            e.printStackTrace();
            // Schedule next spawn even on error
            scheduleNextSpawn();
        }
    }
    
    public void spawnForPlayer(Player player) {
        spawnForPlayer(player, null);
    }

    public void spawnForPlayerForTest(Player player, Consumer<SpawnResult> callback) {
        spawnForPlayer(player, callback);
    }

    private void spawnForPlayer(Player player, Consumer<SpawnResult> callback) {
        spawnForPlayerWithRetry(player, 0, callback);
    }

    private void spawnForPlayerWithRetry(Player player, int attempt, Consumer<SpawnResult> callback) {
        try {
            // Get player's current zone
            WorldMapTracker tracker = player.getWorldMapTracker();
            WorldMapTracker.ZoneDiscoveryInfo zoneInfo = tracker != null ? tracker.getCurrentZone() : null;
            
            String zoneName = zoneInfo != null ? zoneInfo.zoneName() : null;
            String regionName = zoneInfo != null ? zoneInfo.regionName() : null;
            
            // Parse zone ID from region name first (e.g., "Zone4_Tier4" -> 4), then
            // from zone name.
            int parsedZoneId = parseZoneId(regionName);
            if (parsedZoneId < 0 && zoneName != null) {
                parsedZoneId = parseZoneId(zoneName);
            }
            final int zoneId = parsedZoneId;
            
            CometTier tier = selectTierForZone(zoneId);
            if (tier == null) {
                LOGGER.info("Skipping natural comet spawn: no configured tier chances for zone " + zoneId);
                finishSpawn(callback, SpawnResult.SKIPPED_NO_TIER);
                return;
            }
            World currentWorld = player.getWorld();
            if (currentWorld == null) {
                LOGGER.warning("Player " + player.getDisplayName() + " is not in any world, cannot spawn comet");
                finishSpawn(callback, SpawnResult.SKIPPED_NO_WORLD);
                return;
            }

            CometConfig config = CometConfig.getInstance();
            if (config != null && !config.isRaidEnabledInWorld(currentWorld)) {
                finishSpawn(callback, SpawnResult.SKIPPED_WORLD_DISABLED);
                return;
            }
            
            // Try to execute spawn logic on world thread with retry mechanism
            try {
                currentWorld.execute(() -> {
                    SpawnResult result = executeSpawnLogic(player, tier, zoneId);
                    finishSpawn(callback, result);
                });
                // Success! No retry needed
            } catch (Exception e) {
                // Check if this is an IllegalThreadStateException (may be wrapped)
                Throwable cause = e;
                boolean isThreadStateException = false;
                
                // Check exception and its cause chain
                while (cause != null) {
                    if (cause instanceof java.lang.IllegalThreadStateException) {
                        isThreadStateException = true;
                        break;
                    }
                    cause = cause.getCause();
                }
                
                // Also check exception message as fallback (in case exception is wrapped differently)
                if (!isThreadStateException && e.getMessage() != null && 
                    e.getMessage().contains("World thread is not accepting tasks")) {
                    isThreadStateException = true;
                }
                
                if (isThreadStateException) {
                    if (attempt < MAX_RETRY_ATTEMPTS) {
                        long delayMs = INITIAL_RETRY_DELAY_MS * (1L << attempt);
                        com.hypixel.hytale.server.core.HytaleServer.SCHEDULED_EXECUTOR.schedule(
                            () -> spawnForPlayerWithRetry(player, attempt + 1, callback),
                            delayMs,
                            java.util.concurrent.TimeUnit.MILLISECONDS
                        );
                    } else {
                        finishSpawn(callback, SpawnResult.ERROR);
                    }
                } else {
                    LOGGER.warning("Error spawning comet: " + e.getMessage());
                    finishSpawn(callback, SpawnResult.ERROR);
                }
            }
            
        } catch (Exception e) {
            LOGGER.warning("Error in spawnForPlayer: " + e.getMessage());
            e.printStackTrace();
            finishSpawn(callback, SpawnResult.ERROR);
        }
    }

    private void finishSpawn(Consumer<SpawnResult> callback, SpawnResult result) {
        if (callback != null) {
            callback.accept(result);
        }
    }

    private SpawnResult executeSpawnLogic(Player player, CometTier tier, int zoneId) {
        try {
            World currentWorld = player.getWorld();
            if (currentWorld == null) return SpawnResult.SKIPPED_NO_WORLD;
            CometConfig config = CometConfig.getInstance();
            if (config != null && !config.isRaidEnabledInWorld(currentWorld)) return SpawnResult.SKIPPED_WORLD_DISABLED;

            Store<EntityStore> currentStore = currentWorld.getEntityStore().getStore();
            if (currentStore == null) return SpawnResult.ERROR;
            
            com.hypixel.hytale.component.Ref<EntityStore> playerRef = player.getReference();
            if (playerRef == null || !playerRef.isValid()) return SpawnResult.ERROR;

            com.hypixel.hytale.server.core.modules.entity.component.TransformComponent transform =
                currentStore.getComponent(playerRef,
                    com.hypixel.hytale.server.core.modules.entity.component.TransformComponent.getComponentType());
            if (transform == null) return SpawnResult.ERROR;

            com.hypixel.hytale.math.vector.Vector3d playerPos = transform.getPosition();
            Random random = new Random();
            com.hypixel.hytale.math.vector.Vector3i targetBlockPos =
                    findSpawnTargetWithRechecks(currentWorld, playerPos, config, random);

            if (targetBlockPos == null) return SpawnResult.NO_SAFE_LOCATION;

            // Register zone for this comet at its landing position
            CometWaveManager waveManager = CometModPlugin.getWaveManager();
            if (waveManager != null) {
                waveManager.registerCometZone(targetBlockPos, zoneId);
            }
            
            CometFallingSystem fallingSystem = CometModPlugin.getFallingSystem();
            if (fallingSystem == null) {
                fallingSystem = new CometFallingSystem(currentWorld);
                CometModPlugin.setFallingSystem(fallingSystem);
            }

            com.hypixel.hytale.component.CommandBuffer<EntityStore> commandBuffer = null;
            try {
                java.lang.reflect.Method takeCommandBufferMethod = currentStore.getClass().getDeclaredMethod("takeCommandBuffer");
                takeCommandBufferMethod.setAccessible(true);
                commandBuffer = (com.hypixel.hytale.component.CommandBuffer<EntityStore>) takeCommandBufferMethod.invoke(currentStore);
            } catch (Exception e) {
                return SpawnResult.ERROR;
            }

            java.util.UUID ownerUUID = player.getUuid();
            fallingSystem.spawnFallingComet(playerRef, targetBlockPos, tier, null, currentStore, currentWorld, ownerUUID, zoneId);

            try {
                java.lang.reflect.Method consumeMethod = commandBuffer.getClass().getDeclaredMethod("consume");
                consumeMethod.setAccessible(true);
                consumeMethod.invoke(commandBuffer);
            } catch (Exception e) {
                // Ignore
            }
            
            try {
                CometConfig cfg = CometConfig.getInstance();
                String template = (cfg != null ? cfg.msgCometFallingChatCoords
                        : "%tier% Comet falling! Target: X=%x%, Y=%y%, Z=%z%");
                String text = template
                        .replace("%tier%", tier.getName())
                        .replace("%x%", Integer.toString(targetBlockPos.x))
                        .replace("%y%", Integer.toString(targetBlockPos.y))
                        .replace("%z%", Integer.toString(targetBlockPos.z));

                com.hypixel.hytale.server.core.Message coordMessage =
                    com.hypixel.hytale.server.core.Message.raw(text);
                player.sendMessage(coordMessage);
            } catch (Exception e) {
                // Ignore
            }

            try {
                com.hypixel.hytale.server.core.universe.PlayerRef playerRefComponent =
                    currentStore.getComponent(playerRef, com.hypixel.hytale.server.core.universe.PlayerRef.getComponentType());
                if (playerRefComponent != null) {
                    CometConfig cfg = CometConfig.getInstance();
                    String titleTemplate = (cfg != null ? cfg.msgCometFallingTitle : "%tier% Comet Falling!");
                    String subtitleTemplate = (cfg != null ? cfg.msgCometFallingSubtitle : "Watch the sky!");

                    String titleText = titleTemplate.replace("%tier%", tier.getName());
                    String subtitleText = subtitleTemplate.replace("%tier%", tier.getName());

                    com.hypixel.hytale.server.core.Message primaryTitle =
                        com.hypixel.hytale.server.core.Message.raw(titleText);
                    com.hypixel.hytale.server.core.Message secondaryTitle =
                        com.hypixel.hytale.server.core.Message.raw(subtitleText);

                    com.hypixel.hytale.server.core.util.EventTitleUtil.showEventTitleToPlayer(
                        playerRefComponent, primaryTitle, secondaryTitle,
                        true, null, 3.0F, 0.1F, 0.5F
                    );

                    com.hypixel.hytale.server.core.HytaleServer.SCHEDULED_EXECUTOR.schedule(() -> {
                        try {
                            World playerWorld = player.getWorld();
                            if (playerWorld != null) {
                                playerWorld.execute(() -> {
                                    com.hypixel.hytale.server.core.util.EventTitleUtil.hideEventTitleFromPlayer(playerRefComponent, 0.0F);
                                });
                            }
                        } catch (Exception e) {
                            // Ignore
                        }
                    }, 3L, java.util.concurrent.TimeUnit.SECONDS);
                }
            } catch (Exception e) {
                // Ignore
            }
            
            return SpawnResult.SPAWNED;
        } catch (Exception e) {
            LOGGER.warning("Error spawning comet: " + e.getMessage());
            return SpawnResult.ERROR;
        }
    }

    private com.hypixel.hytale.math.vector.Vector3i findSpawnTargetWithRechecks(
            World currentWorld,
            com.hypixel.hytale.math.vector.Vector3d playerPos,
            CometConfig config,
            Random random) {

        int centerX = (int) playerPos.x;
        int centerZ = (int) playerPos.z;
        int startY = (int) playerPos.y;

        for (int recheck = 0; recheck <= EXTRA_LOCATION_RECHECKS; recheck++) {
            com.hypixel.hytale.math.vector.Vector3i target =
                    tryFindSpawnTargetNearCenter(currentWorld, centerX, centerZ, startY, config, random);
            if (target != null) {
                return target;
            }

            if (recheck < EXTRA_LOCATION_RECHECKS) {
                double relocateAngle = random.nextDouble() * 2 * Math.PI;
                double relocateDistance = Math.max(MIN_RECHECK_RELOCATION_DISTANCE, maxSpawnDistance * 2.0);
                centerX = (int) (playerPos.x + Math.cos(relocateAngle) * relocateDistance);
                centerZ = (int) (playerPos.z + Math.sin(relocateAngle) * relocateDistance);
            }
        }

        return null;
    }

    private com.hypixel.hytale.math.vector.Vector3i tryFindSpawnTargetNearCenter(
            World currentWorld,
            int centerX,
            int centerZ,
            int startY,
            CometConfig config,
            Random random) {
        for (int attempt = 0; attempt < SPAWN_LOCATION_ATTEMPTS; attempt++) {
            double angle = random.nextDouble() * 2 * Math.PI;
            double distance = minSpawnDistance + random.nextDouble() * (maxSpawnDistance - minSpawnDistance);

            int spawnX = (int) (centerX + Math.cos(angle) * distance);
            int spawnZ = (int) (centerZ + Math.sin(angle) * distance);
            int spawnY = findGroundLevel(currentWorld, spawnX, spawnZ, startY);

            if (spawnY == -1) continue;
            if (isInWater(currentWorld, spawnX, spawnY, spawnZ) || isInWater(currentWorld, spawnX, spawnY + 1, spawnZ)) continue;

            com.hypixel.hytale.math.vector.Vector3i targetBlockPos =
                    new com.hypixel.hytale.math.vector.Vector3i(spawnX, spawnY + 1, spawnZ);
            if (!ClaimProtectionGuard.canSpawnAt(currentWorld, targetBlockPos.x, targetBlockPos.y, targetBlockPos.z, config)) {
                continue;
            }
            return targetBlockPos;
        }

        return null;
    }

    private int parseZoneId(String name) {
        if (name == null || name.isEmpty()) return -1;

        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("(?i)zone(\\d+)");
        java.util.regex.Matcher matcher = pattern.matcher(name);

        if (matcher.find()) {
            try {
                return Integer.parseInt(matcher.group(1));
            } catch (NumberFormatException e) {
                // Fall through
            }
        }

        java.util.regex.Pattern fallbackPattern = java.util.regex.Pattern.compile("\\d+");
        java.util.regex.Matcher fallbackMatcher = fallbackPattern.matcher(name);

        if (fallbackMatcher.find()) {
            try {
                return Integer.parseInt(fallbackMatcher.group());
            } catch (NumberFormatException e) {
                // Fall through
            }
        }

        return -1;
    }

    private CometTier selectTierForZone(int zoneId) {
        Random random = new Random();
        CometConfig config = CometConfig.getInstance();
        if (config == null) {
            return null;
        }

        ZoneSpawnChances chances = config.getZoneSpawnChances(zoneId);
        if (chances == null) {
            return null;
        }

        if (!CometConfig.isTier5Enabled()) {
            double t1 = chances.getTier1();
            double t2 = chances.getTier2();
            double t3 = chances.getTier3();
            double t4 = chances.getTier4();
            double total = t1 + t2 + t3 + t4;
            if (total <= 0.0) {
                return null;
            }

            double roll = random.nextDouble() * total;
            if (roll < t1) return CometTier.UNCOMMON;
            roll -= t1;
            if (roll < t2) return CometTier.RARE;
            roll -= t2;
            if (roll < t3) return CometTier.EPIC;
            return CometTier.LEGENDARY;
        }

        int selectedTier = chances.selectTier(random);
        switch (selectedTier) {
            case 1: return CometTier.UNCOMMON;
            case 2: return CometTier.RARE;
            case 3: return CometTier.EPIC;
            case 4: return CometTier.LEGENDARY;
            case 5: return CometTier.MYTHIC;
            default: return null;
        }
    }

    private boolean isInWater(World targetWorld, int x, int y, int z) {
        try {
            long chunkIndex = com.hypixel.hytale.math.util.ChunkUtil.indexChunkFromBlock(x, z);
            com.hypixel.hytale.server.core.universe.world.chunk.WorldChunk chunk =
                targetWorld.getChunkIfInMemory(chunkIndex);

            if (chunk == null) chunk = targetWorld.getChunk(chunkIndex);
            if (chunk == null) return false;

            return chunk.getFluidId(x, y, z) != 0;
        } catch (Exception e) {
            return false;
        }
    }

    private int findGroundLevel(World targetWorld, int x, int z, int startY) {
        int searchStartY = 255;
        int minY = Math.max(0, startY - 150);
        
        for (int y = searchStartY; y >= minY; y--) {
            try {
                com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockType blockType = 
                    targetWorld.getBlockType(x, y, z);
                
                if (blockType != null) {
                    int blockId = com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockType
                        .getAssetMap().getIndex(blockType.getId());
                    
                    if (blockId != 0) {
                        Object material = blockType.getMaterial();
                        if (material != null) {
                            String materialStr = material.toString();
                            if (materialStr.equals("Solid") || materialStr.equals("Opaque")) {
                                return y;
                            }
                        }
                    }
                }
            } catch (Exception e) {
                // Continue searching
            }
        }
        
        return -1;
    }
}
