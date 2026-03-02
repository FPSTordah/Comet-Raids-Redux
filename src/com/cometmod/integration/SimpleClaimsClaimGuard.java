package com.cometmod.integration;

import com.hypixel.hytale.math.util.ChunkUtil;
import com.hypixel.hytale.server.core.universe.world.World;

import java.lang.reflect.Method;
import java.util.Locale;
import java.util.logging.Logger;

/**
 * Optional SimpleClaims integration using reflection.
 */
public final class SimpleClaimsClaimGuard {

    private static final Logger LOGGER = Logger.getLogger("SimpleClaimsClaimGuard");

    private static Method claimManagerGetInstanceMethod;
    private static Method getChunkMethod;
    private static boolean warnedMissingApi;
    private static boolean warnedApiFailure;

    private SimpleClaimsClaimGuard() {
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
            if (getChunkMethod == null) {
                getChunkMethod = claimManager.getClass().getMethod("getChunk", String.class, int.class, int.class);
            }

            String worldName = world.getName();
            if (isClaimedInWorldKey(claimManager, worldName, chunkX, chunkZ)) {
                return true;
            }

            String lowerName = worldName.toLowerCase(Locale.ROOT);
            return !lowerName.equals(worldName) && isClaimedInWorldKey(claimManager, lowerName, chunkX, chunkZ);
        } catch (Exception e) {
            if (!warnedApiFailure) {
                warnedApiFailure = true;
                LOGGER.warning("SimpleClaims API call failed, claim checks disabled for this run: " + e.getMessage());
            }
            return false;
        }
    }

    private static boolean isClaimedInWorldKey(Object claimManager, String worldKey, int chunkX, int chunkZ)
            throws Exception {
        Object chunk = getChunkMethod.invoke(claimManager, worldKey, chunkX, chunkZ);
        return chunk != null;
    }

    private static Object getClaimManager() {
        try {
            if (claimManagerGetInstanceMethod == null) {
                Class<?> managerClass = Class.forName("com.buuz135.simpleclaims.claim.ClaimManager");
                claimManagerGetInstanceMethod = managerClass.getMethod("getInstance");
            }
            return claimManagerGetInstanceMethod.invoke(null);
        } catch (Exception e) {
            if (!warnedMissingApi) {
                warnedMissingApi = true;
                LOGGER.warning("SimpleClaims not detected. SimpleClaims claim checks are inactive.");
            }
            return null;
        }
    }
}
