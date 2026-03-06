package com.cometmod.integration;

import com.hypixel.hytale.math.util.ChunkUtil;
import com.hypixel.hytale.server.core.universe.world.World;

import java.lang.reflect.Method;
import java.util.Locale;
import java.util.logging.Logger;

/**
 * Optional HyperFactions integration using reflection.
 */
public final class HyperFactionsClaimGuard {

    private static final Logger LOGGER = Logger.getLogger(HyperFactionsClaimGuard.class.getName());

    private static Method isAvailableMethod;
    private static Method isClaimedMethod;
    private static boolean warnedMissingApi;
    private static boolean warnedApiFailure;

    private HyperFactionsClaimGuard() {
    }

    public static boolean isAvailable() {
        return isApiAvailable();
    }

    public static boolean isClaimedAt(World world, int blockX, int blockY, int blockZ) {
        if (world == null) {
            return false;
        }

        if (!isApiAvailable()) {
            return false;
        }

        try {
            int chunkX = ChunkUtil.chunkCoordinate(blockX);
            int chunkZ = ChunkUtil.chunkCoordinate(blockZ);
            String worldName = world.getName();
            if (invokeIsClaimed(worldName, chunkX, chunkZ)) {
                return true;
            }

            String lowerName = worldName.toLowerCase(Locale.ROOT);
            return !lowerName.equals(worldName) && invokeIsClaimed(lowerName, chunkX, chunkZ);
        } catch (Exception e) {
            if (!warnedApiFailure) {
                warnedApiFailure = true;
                LOGGER.warning("HyperFactions API call failed, claim checks disabled for this run: " + e.getMessage());
            }
            return false;
        }
    }

    private static boolean invokeIsClaimed(String worldKey, int chunkX, int chunkZ) throws Exception {
        Object claimed = isClaimedMethod.invoke(null, worldKey, chunkX, chunkZ);
        return claimed instanceof Boolean && ((Boolean) claimed);
    }

    private static boolean isApiAvailable() {
        try {
            if (isAvailableMethod == null || isClaimedMethod == null) {
                Class<?> apiClass = Class.forName("com.hyperfactions.api.HyperFactionsAPI");
                isAvailableMethod = apiClass.getMethod("isAvailable");
                isClaimedMethod = apiClass.getMethod("isClaimed", String.class, int.class, int.class);
            }

            Object available = isAvailableMethod.invoke(null);
            return available instanceof Boolean && ((Boolean) available);
        } catch (Exception e) {
            if (!warnedMissingApi) {
                warnedMissingApi = true;
                LOGGER.warning("HyperFactions not detected. HyperFactions claim checks are inactive.");
            }
            return false;
        }
    }
}
