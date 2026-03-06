package com.cometmod.integration;

import com.hypixel.hytale.math.util.ChunkUtil;
import com.hypixel.hytale.server.core.universe.world.World;

import java.lang.reflect.Method;
import java.util.Locale;
import java.util.logging.Logger;

/**
 * Optional ElbaphFactions integration using reflection.
 */
public final class ElbaphFactionsClaimGuard {

    private static final Logger LOGGER = Logger.getLogger(ElbaphFactionsClaimGuard.class.getName());

    private static Method claimManagerGetInstanceMethod;
    private static Method isChunkClaimedMethod;
    private static boolean warnedMissingApi;
    private static boolean warnedApiFailure;

    private ElbaphFactionsClaimGuard() {
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
            if (isChunkClaimedMethod == null) {
                isChunkClaimedMethod = claimManager.getClass().getMethod("isChunkClaimed",
                        String.class, int.class, int.class);
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
                LOGGER.warning("ElbaphFactions API call failed, claim checks disabled for this run: " + e.getMessage());
            }
            return false;
        }
    }

    private static boolean isClaimedInWorldKey(Object claimManager, String worldKey, int chunkX, int chunkZ)
            throws Exception {
        Object claimed = isChunkClaimedMethod.invoke(claimManager, worldKey, chunkX, chunkZ);
        return claimed instanceof Boolean && ((Boolean) claimed);
    }

    private static Object getClaimManager() {
        try {
            if (claimManagerGetInstanceMethod == null) {
                Class<?> managerClass = Class.forName("net.elbaph.factions.factions.ClaimManager");
                claimManagerGetInstanceMethod = managerClass.getMethod("getInstance");
            }
            return claimManagerGetInstanceMethod.invoke(null);
        } catch (Exception e) {
            if (!warnedMissingApi) {
                warnedMissingApi = true;
                LOGGER.warning("ElbaphFactions not detected. ElbaphFactions claim checks are inactive.");
            }
            return null;
        }
    }
}
