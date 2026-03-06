package com.cometmod.spawn;

import com.cometmod.CometConfig;
import com.cometmod.CometModPlugin;
import com.cometmod.wave.CometTier;
import com.cometmod.config.model.ThemeConfig;
import com.cometmod.integration.ClaimProtectionGuard;
import com.cometmod.wave.CometWaveManager;
import com.cometmod.wave.WaveThemeProvider;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.math.shape.Box;
import com.hypixel.hytale.math.util.ChunkUtil;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.math.vector.Vector3i;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockType;
import com.hypixel.hytale.server.core.modules.collision.CollisionModule;
import com.hypixel.hytale.server.core.modules.collision.CollisionResult;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.chunk.WorldChunk;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import javax.annotation.Nullable;
import java.util.UUID;
import java.util.logging.Logger;

/**
 * Uses the game's native CollisionModule.validatePosition to avoid spawning
 * mobs inside blocks (suffocation). Same logic as ActionSpawn, NPCTestCommand,
 * and SpawningContext.
 */
public final class CometSpawnUtil {

    private static final Logger LOGGER = Logger.getLogger(CometSpawnUtil.class.getName());

    /** Fallback block IDs to try when preferred/comet block is not loaded (e.g. mod assets missing). */
    private static final String[] FALLBACK_BLOCK_IDS = {
        "Stone", "Blocks/Stone", "Dirt", "Blocks/Dirt", "Grass", "Blocks/Grass",
        "Cobblestone", "Blocks/Cobblestone", "Planks", "Blocks/Planks"
    };

    /**
     * Resolve a BlockType for placing the comet spawn block. Tries preferredId (and preferredId + ".json"),
     * then tier default, then a list of common vanilla-style IDs, then any loaded block with a valid index.
     * Never returns null if the asset map has at least one placeable block.
     */
    @Nullable
    public static BlockType resolveCometSpawnBlockType(String preferredId, String tierDefaultId, java.util.function.Consumer<String> onFallback) {
        if (tierDefaultId == null || tierDefaultId.isBlank()) {
            tierDefaultId = CometTier.UNCOMMON.getBlockId("Comet_Stone");
        }
        BlockType bt = tryGetBlockType(preferredId);
        if (bt != null) return bt;
        bt = tryGetBlockTypeByVariants(preferredId);
        if (bt != null) return bt;
        if (onFallback != null && preferredId != null && !preferredId.equals(tierDefaultId)) {
            onFallback.accept("spawnBlock '" + preferredId + "' not found (tried path variants and map scan); trying tier default.");
        }
        bt = tryGetBlockType(tierDefaultId);
        if (bt != null) return bt;
        if (onFallback != null) onFallback.accept(tierDefaultId + " not found; trying fallback blocks.");
        for (String id : FALLBACK_BLOCK_IDS) {
            bt = tryGetBlockType(id);
            if (bt != null) {
                if (onFallback != null) onFallback.accept("Using fallback block: " + id);
                return bt;
            }
        }
        try {
            java.util.Map<String, BlockType> map = BlockType.getAssetMap().getAssetMap();
            if (map != null) {
                for (BlockType candidate : map.values()) {
                    if (candidate == null) continue;
                    String id = candidate.getId();
                    if (id == null || id.isEmpty()) continue;
                    int idx = BlockType.getAssetMap().getIndex(id);
                    if (idx >= 0) {
                        if (onFallback != null) onFallback.accept("Using first available block: " + id);
                        return candidate;
                    }
                }
            }
        } catch (Exception e) {
            LOGGER.warning("Error scanning block map for fallback: " + e.getMessage());
        }
        return null;
    }

    /** Default portal particle variant when placing a comet portal block (non–Comet_Stone_* block with Comet_* id). */
    private static final String PORTAL_EXPLOSION_PARTICLE_DEFAULT = "Comet_Explosion_Large_Portal_Blue";

    /**
     * Returns the explosion particle system ID to use when a comet block is placed. Comet portal blocks
     * (block id starts with "Comet_" but is not Comet_Stone_*) use a portal-colored variant; others use the tier default.
     */
    public static String getExplosionParticleSystemForPlacedBlock(BlockType blockType, CometTier tier) {
        if (blockType != null) {
            String id = blockType.getId();
            if (id != null && id.startsWith("Comet_") && !id.startsWith("Comet_Stone_")) {
                return PORTAL_EXPLOSION_PARTICLE_DEFAULT;
            }
        }
        return tier != null ? tier.getExplosionParticleSystem() : CometTier.UNCOMMON.getExplosionParticleSystem();
    }


    /**
     * Single path for placing the comet block and registering it (zone, tier, theme, despawn tracker).
     * Used by CometFallingSystem and CometSpawnCommand. Caller is responsible for scheduling despawn when desired.
     *
     * @return The placed block's ID (e.g. "Comet_Stone_Uncommon") on success, or null on failure
     */
    @Nullable
    public static String placeAndRegisterCometBlock(World world, Vector3i blockPos, Store<EntityStore> store,
            CometTier tier, String themeId, UUID ownerUUID, int zoneId) {
        CometConfig config = CometConfig.getInstance();
        if (config != null && !ClaimProtectionGuard.canSpawnAt(world, blockPos.x, blockPos.y, blockPos.z, config)) {
            LOGGER.info("Comet block placement blocked by claim/protected-zone rules at " + blockPos);
            return null;
        }
        if (themeId == null || themeId.isBlank()) {
            themeId = WaveThemeProvider.selectTheme(tier);
        }
        ThemeConfig theme = (config != null && themeId != null && !themeId.isBlank()) ? config.getTheme(themeId) : null;
        String preferredId = (theme != null && theme.getSpawnBlock() != null && !theme.getSpawnBlock().isBlank())
                ? theme.getSpawnBlock().trim()
                : tier.getBlockId("Comet_Stone");
        String tierDefaultId = tier.getBlockId("Comet_Stone");
        LOGGER.info("Comet spawn: themeId=" + themeId + ", theme=" + (theme != null ? "ok" : "null") + ", spawnBlock=" + (theme != null && theme.getSpawnBlock() != null ? "'" + theme.getSpawnBlock() + "'" : "null") + ", preferredId='" + preferredId + "'");
        if (preferredId != null && !preferredId.startsWith("Comet_Stone_")) {
            LOGGER.fine("Theme " + themeId + " spawnBlock " + preferredId + ": if this block has no Use, players activate by hitting the block.");
        }
        BlockType blockType = resolveCometSpawnBlockType(preferredId, tierDefaultId, msg -> LOGGER.warning(msg));
        if (blockType == null) {
            LOGGER.severe("No block type available for comet spawn (asset map empty?). Cannot place.");
            return null;
        }
        // Inject Use for clean-slate blocks so F (Use) fires UseBlockEvent.Pre and activates the comet
        blockType = ensureBlockHasUseForComet(blockType);
        String blockIdName = blockType.getId();
        LOGGER.info("Placed comet block: '" + blockIdName + "' at " + blockPos);
        long chunkIndex = ChunkUtil.indexChunkFromBlock(blockPos.x, blockPos.z);
        WorldChunk chunk = world.getChunkIfInMemory(chunkIndex);
        if (chunk == null) chunk = world.getChunk(chunkIndex);
        if (chunk == null) {
            LOGGER.warning("Could not get chunk for comet block at " + blockPos);
            return null;
        }
        int localX = blockPos.x & 31;
        int localZ = blockPos.z & 31;
        int blockId = BlockType.getAssetMap().getIndex(blockType.getId());
        if (blockId == -1) {
            LOGGER.warning(blockIdName + " block index not found!");
            return null;
        }
        chunk.setBlock(localX, blockPos.y, localZ, blockId, blockType, 0, 0, 0);
        chunk.markNeedsSaving();

        CometWaveManager waveManager = CometModPlugin.getWaveManager();
        if (waveManager != null) {
            waveManager.registerCometZone(blockPos, zoneId);
            waveManager.registerCometTier(world, blockPos, tier, ownerUUID);
            if (themeId != null) waveManager.forceTheme(blockPos, themeId);
        }
        if (store != null) {
            Vector3d blockCenter = new Vector3d(blockPos.x + 0.5, blockPos.y + 0.5, blockPos.z + 0.5);
            try {
                String explosionSystemId = getExplosionParticleSystemForPlacedBlock(blockType, tier);
                com.hypixel.hytale.server.core.universe.world.ParticleUtil.spawnParticleEffect(
                        explosionSystemId, blockCenter, store);
            } catch (Exception e) {
                LOGGER.warning("Failed to spawn explosion particle system: " + e.getMessage());
            }
            // Do not spawn the beam from code for custom blocks (e.g. coffin): there is no API to remove it when the
            // block is broken, so it would persist as an orphan. Use a block that defines the beam in its asset
            // (Comet_Stone_* or Comet_* portals) if you want a beam that disappears with the block.
        }
        CometDespawnTracker.getInstance().registerComet(blockPos, tier.getName());
        return blockIdName;
    }

    private static BlockType tryGetBlockType(String id) {
        if (id == null || id.isEmpty()) return null;
        BlockType bt = BlockType.getAssetMap().getAsset(id);
        if (bt != null) return bt;
        return BlockType.getAssetMap().getAsset(id + ".json");
    }

    /**
     * Try to resolve a block by path variants (Blocks/, Furniture/, etc.) and by scanning the asset map
     * for any block whose getId() equals or ends with the preferred id (so IDs like "Furniture/Village_Coffin" match "Village_Coffin").
     */
    @Nullable
    private static BlockType tryGetBlockTypeByVariants(String preferredId) {
        if (preferredId == null || preferredId.isEmpty()) return null;
        String[] pathPrefixes = { "Blocks/", "Furniture/", "Block/", "" };
        for (String prefix : pathPrefixes) {
            if (prefix.isEmpty() && !preferredId.contains("/")) continue; // already tried in tryGetBlockType
            String withPrefix = prefix + preferredId;
            BlockType bt = tryGetBlockType(withPrefix);
            if (bt != null) return bt;
        }
        try {
            java.util.Map<String, BlockType> map = BlockType.getAssetMap().getAssetMap();
            if (map == null) return null;
            String preferredLower = preferredId.toLowerCase();
            for (BlockType candidate : map.values()) {
                if (candidate == null) continue;
                String id = candidate.getId();
                if (id == null || id.isEmpty()) continue;
                if (BlockType.getAssetMap().getIndex(id) < 0) continue;
                String idNorm = id.replace(".json", "");
                if (idNorm.equalsIgnoreCase(preferredId) || idNorm.equalsIgnoreCase(preferredLower)) return candidate;
                if (idNorm.endsWith("/" + preferredId) || idNorm.endsWith("/" + preferredLower)) return candidate;
                if (idNorm.replace("/", "_").equalsIgnoreCase(preferredId.replace("/", "_"))) return candidate;
            }
        } catch (Exception e) {
            LOGGER.fine("Scan block map for '" + preferredId + "': " + e.getMessage());
        }
        return null;
    }

    /**
     * Previously attempted to inject Use into clean-slate blocks via clone + loadAssets;
     * that path caused server crash/hang. Now a no-op: blocks without Use use hit-to-activate.
     */
    @Nullable
    private static BlockType ensureBlockHasUseForComet(@Nullable BlockType blockType) {
        return blockType;
    }

    /**
     * Conservative NPC bounding box (entity-local: feet at y=0).
     * 0.7x1.9x0.7 to cover typical humanoid mobs; used when we don't have
     * the actual model bbox.
     */
    private static final Box DEFAULT_NPC_BOX = new Box(-0.35, 0, -0.35, 0.35, 1.9, 0.35);

    /** Offsets (dx, dy, dz) to try when finding a valid mob spawn. */
    private static final int[][] MOB_SPAWN_OFFSETS = {
        { 0, 0, 0 },
        { 1, 0, 0 }, { -1, 0, 0 }, { 0, 0, 1 }, { 0, 0, -1 },
        { 1, 0, 1 }, { -1, 0, 1 }, { 1, 0, -1 }, { -1, 0, -1 },
        { 0, 1, 0 }
    };

    private CometSpawnUtil() {}

    /**
     * Check if a mob can stand at (x,y,z) using the game's CollisionModule:
     * the NPC box is tested for block overlap (validatePosition returns -1
     * when overlapping). Matches ActionSpawn, SpawningContext, NPCTestCommand.
     */
    public static boolean isValidMobSpawn(World world, double x, double y, double z) {
        try {
            CollisionModule cm = CollisionModule.get();
            if (cm == null || cm.isDisabled()) return true; // fallback: allow
            CollisionResult res = new CollisionResult();
            int v = cm.validatePosition(world, DEFAULT_NPC_BOX, new Vector3d(x, y, z), res);
            return v != -1;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Find a valid mob spawn near the preferred position by trying offsets.
     */
    @Nullable
    public static Vector3d findValidMobSpawn(World world, Vector3d preferred, int maxRetries) {
        for (int i = 0; i < Math.min(maxRetries, MOB_SPAWN_OFFSETS.length); i++) {
            int[] o = MOB_SPAWN_OFFSETS[i];
            double x = preferred.x + o[0];
            double y = preferred.y + o[1];
            double z = preferred.z + o[2];
            if (isValidMobSpawn(world, x, y, z))
                return new Vector3d(x, y, z);
        }
        return null;
    }
}
