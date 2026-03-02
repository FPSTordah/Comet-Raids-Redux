package com.cometmod.integration;

import com.hypixel.hytale.math.util.ChunkUtil;
import com.hypixel.hytale.server.core.universe.world.World;

import java.lang.reflect.Method;
import java.util.Locale;
import java.util.logging.Logger;

/**
 * Optional WiFlowsClaims integration using reflection.
 */
public final class WiFlowsClaimsClaimGuard {

    private static final Logger LOGGER = Logger.getLogger("WiFlowsClaimsClaimGuard");

    private static Method getClaimOwnerMethod;
    private static boolean warnedMissingApi;
    private static boolean warnedApiFailure;

    private WiFlowsClaimsClaimGuard() {
    }

    public static boolean isAvailable() {
        return resolveApiMethod() != null;
    }

    public static boolean isClaimedAt(World world, int blockX, int blockY, int blockZ) {
        if (world == null) {
            return false;
        }

        Method method = resolveApiMethod();
        if (method == null) {
            return false;
        }

        int chunkX = ChunkUtil.chunkCoordinate(blockX);
        int chunkZ = ChunkUtil.chunkCoordinate(blockZ);

        try {
            String worldName = world.getName();
            if (isClaimedInWorldKey(method, worldName, chunkX, chunkZ)) {
                return true;
            }

            String lowerName = worldName.toLowerCase(Locale.ROOT);
            return !lowerName.equals(worldName) && isClaimedInWorldKey(method, lowerName, chunkX, chunkZ);
        } catch (Exception e) {
            if (!warnedApiFailure) {
                warnedApiFailure = true;
                LOGGER.warning("WiFlowsClaims API call failed, claim checks disabled for this run: " + e.getMessage());
            }
            return false;
        }
    }

    private static boolean isClaimedInWorldKey(Method method, String worldKey, int chunkX, int chunkZ) throws Exception {
        Object owner = method.invoke(null, worldKey, chunkX, chunkZ);
        return owner != null;
    }

    private static Method resolveApiMethod() {
        try {
            if (getClaimOwnerMethod == null) {
                Class<?> accessClass = Class.forName("com.wiflowsclaims.WiFlowsClaimsAccess");
                getClaimOwnerMethod = accessClass.getMethod("getClaimOwner", String.class, int.class, int.class);
            }
            return getClaimOwnerMethod;
        } catch (Exception e) {
            if (!warnedMissingApi) {
                warnedMissingApi = true;
                LOGGER.warning("WiFlowsClaims not detected. WiFlowsClaims claim checks are inactive.");
            }
            return null;
        }
    }
}
