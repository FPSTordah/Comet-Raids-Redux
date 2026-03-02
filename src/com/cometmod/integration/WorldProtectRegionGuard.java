package com.cometmod.integration;

import com.cometmod.CometConfig;
import com.hypixel.hytale.server.core.universe.world.World;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

/**
 * Optional integration layer for WorldProtect.
 * Uses reflection so CometMod can run even when WorldProtect is not installed.
 */
public final class WorldProtectRegionGuard {

    private static final Logger LOGGER = Logger.getLogger("WorldProtectRegionGuard");

    private static Method regionServiceAccessor;
    private static Method primaryRegionAccessor;
    private static Method listAllRegionsAccessor;
    private static Method regionIdAccessor;
    private static boolean warnedMissingApi;
    private static boolean warnedApiFailure;
    private static long lastRegionSyncAttemptMs;
    private static final long REGION_SYNC_INTERVAL_MS = 30_000L;

    private WorldProtectRegionGuard() {
    }

    public static boolean isAvailable() {
        return getRegionService() != null;
    }

    /**
     * Returns true if comet spawn is allowed at the location.
     * If integration is disabled or WorldProtect is unavailable, returns true.
     */
    public static boolean canSpawnAt(World world, int x, int y, int z, CometConfig config) {
        if (config == null) {
            return true;
        }

        if (!config.isProtectedZoneSpawnRulesEnabled()) {
            return true;
        }

        syncKnownRegionsNow(config);

        String regionId = getPrimaryRegionIdAt(world, x, y, z);
        if (regionId == null || regionId.isBlank() || isGlobalRegion(regionId)) {
            return true; // not in a concrete protected region
        }

        return config.isProtectedRegionSpawnAllowed(regionId);
    }

    /**
     * Returns true when the given location is inside a concrete WorldProtect region.
     * Global/virtual regions do not count as claimed.
     */
    public static boolean isClaimedAt(World world, int x, int y, int z) {
        String regionId = getPrimaryRegionIdAt(world, x, y, z);
        return regionId != null && !regionId.isBlank() && !isGlobalRegion(regionId);
    }

    /**
     * Get the primary WorldProtect region id at a location, or null if none.
     */
    public static String getPrimaryRegionIdAt(World world, int x, int y, int z) {
        if (world == null) {
            return null;
        }

        Object regionService = getRegionService();
        if (regionService == null) {
            return null;
        }

        try {
            if (primaryRegionAccessor == null) {
                primaryRegionAccessor = regionService.getClass().getMethod(
                        "getPrimaryRegionAt",
                        String.class, int.class, int.class, int.class);
            }

            Object region = primaryRegionAccessor.invoke(regionService, world.getName(), x, y, z);
            if (region == null) {
                return null;
            }

            if (regionIdAccessor == null) {
                regionIdAccessor = region.getClass().getMethod("id");
            }

            Object regionId = regionIdAccessor.invoke(region);
            return (regionId instanceof String) ? (String) regionId : null;
        } catch (Exception e) {
            if (!warnedApiFailure) {
                warnedApiFailure = true;
                LOGGER.warning("WorldProtect API call failed, protected-zone comet rules disabled for this run: " + e.getMessage());
            }
            return null;
        }
    }

    private static Object getRegionService() {
        try {
            if (regionServiceAccessor == null) {
                Class<?> servicesClass = Class.forName("dev.worldprotect.worldprotect.util.WorldProtectServices");
                regionServiceAccessor = servicesClass.getMethod("regionService");
            }
            return regionServiceAccessor.invoke(null);
        } catch (Exception e) {
            if (!warnedMissingApi) {
                warnedMissingApi = true;
                LOGGER.warning("WorldProtect not detected. Protected-zone comet rules are inactive.");
            }
            return null;
        }
    }

    private static boolean isGlobalRegion(String regionId) {
        return "__global__".equalsIgnoreCase(regionId) || "global".equalsIgnoreCase(regionId);
    }

    /**
     * Trigger region sync with internal throttling.
     */
    public static void syncKnownRegions(CometConfig config) {
        syncKnownRegionsNow(config);
    }

    private static void syncKnownRegionsNow(CometConfig config) {
        long now = System.currentTimeMillis();
        if (now - lastRegionSyncAttemptMs < REGION_SYNC_INTERVAL_MS) {
            return;
        }
        lastRegionSyncAttemptMs = now;

        Object regionService = getRegionService();
        if (regionService == null) {
            return;
        }

        try {
            if (listAllRegionsAccessor == null) {
                listAllRegionsAccessor = regionService.getClass().getMethod("listAllRegionsAllWorlds");
            }

            Object allRegions = listAllRegionsAccessor.invoke(regionService);
            if (!(allRegions instanceof Iterable<?> iterable)) {
                return;
            }

            List<String> regionIds = new ArrayList<>();
            for (Object region : iterable) {
                if (region == null) {
                    continue;
                }

                if (regionIdAccessor == null) {
                    regionIdAccessor = region.getClass().getMethod("id");
                }

                Object idObj = regionIdAccessor.invoke(region);
                if (idObj instanceof String id && !isGlobalRegion(id)) {
                    regionIds.add(id);
                }
            }

            config.syncProtectedRegionOverrides(regionIds);
        } catch (Exception e) {
            if (!warnedApiFailure) {
                warnedApiFailure = true;
                LOGGER.warning("WorldProtect region sync failed: " + e.getMessage());
            }
        }
    }
}
