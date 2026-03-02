package com.cometmod.integration;

import com.hypixel.hytale.math.util.ChunkUtil;
import com.hypixel.hytale.server.core.universe.world.World;

import java.lang.reflect.Method;
import java.util.Locale;
import java.util.logging.Logger;

/**
 * Optional Hyfaction integration using reflection.
 */
public final class HyfactionClaimGuard {

    private static final Logger LOGGER = Logger.getLogger("HyfactionClaimGuard");

    private static Method claimManagerGetInstanceMethod;
    private static Method getChunkMethod;
    private static Method isSafeZoneMethod;
    private static Method isWarZoneMethod;
    private static boolean warnedMissingApi;
    private static boolean warnedApiFailure;

    private HyfactionClaimGuard() {
    }

    public static boolean isAvailable() {
        return getClaimManager() != null;
    }

    public static boolean isClaimedAt(World world, int blockX, int blockY, int blockZ) {
        if (world == null) {
            return false;
        }

        Object claimManager = getClaimManager();
        if (claimManager == null) {
            return false;
        }

        int chunkX = ChunkUtil.chunkCoordinate(blockX);
        int chunkZ = ChunkUtil.chunkCoordinate(blockZ);

        try {
            String worldName = world.getName();
            if (isClaimedInWorldKey(claimManager, worldName, chunkX, chunkZ)) {
                return true;
            }

            String lowerName = worldName.toLowerCase(Locale.ROOT);
            return !lowerName.equals(worldName) && isClaimedInWorldKey(claimManager, lowerName, chunkX, chunkZ);
        } catch (Exception e) {
            if (!warnedApiFailure) {
                warnedApiFailure = true;
                LOGGER.warning("Hyfaction API call failed, claim checks disabled for this run: " + e.getMessage());
            }
            return false;
        }
    }

    private static boolean isClaimedInWorldKey(Object claimManager, String worldKey, int chunkX, int chunkZ)
            throws Exception {
        if (getChunkMethod == null) {
            getChunkMethod = claimManager.getClass().getMethod("getChunk", String.class, int.class, int.class);
        }
        Object chunk = getChunkMethod.invoke(claimManager, worldKey, chunkX, chunkZ);
        if (chunk != null) {
            return true;
        }

        if (isSafeZoneMethod == null) {
            isSafeZoneMethod = claimManager.getClass().getMethod("isSafeZone", String.class, int.class, int.class);
        }
        Object safeZone = isSafeZoneMethod.invoke(claimManager, worldKey, chunkX, chunkZ);
        if (safeZone instanceof Boolean && ((Boolean) safeZone)) {
            return true;
        }

        if (isWarZoneMethod == null) {
            isWarZoneMethod = claimManager.getClass().getMethod("isWarZone", String.class, int.class, int.class);
        }
        Object warZone = isWarZoneMethod.invoke(claimManager, worldKey, chunkX, chunkZ);
        return warZone instanceof Boolean && ((Boolean) warZone);
    }

    private static Object getClaimManager() {
        try {
            if (claimManagerGetInstanceMethod == null) {
                Class<?> managerClass = Class.forName("com.kaws.hyfaction.claim.ClaimManager");
                claimManagerGetInstanceMethod = managerClass.getMethod("getInstance");
            }
            return claimManagerGetInstanceMethod.invoke(null);
        } catch (Exception e) {
            if (!warnedMissingApi) {
                warnedMissingApi = true;
                LOGGER.warning("Hyfaction not detected. Hyfaction claim checks are inactive.");
            }
            return null;
        }
    }
}
