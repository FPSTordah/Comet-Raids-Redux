package com.cometmod.integration;

import com.hypixel.hytale.common.plugin.PluginIdentifier;
import com.hypixel.hytale.math.util.ChunkUtil;
import com.hypixel.hytale.server.core.plugin.PluginBase;
import com.hypixel.hytale.server.core.plugin.PluginManager;
import com.hypixel.hytale.server.core.universe.world.World;

import java.lang.reflect.Method;
import java.util.Locale;
import java.util.Optional;
import java.util.logging.Logger;

/**
 * Optional UltimateFaction integration using reflection.
 */
public final class UltimateFactionClaimGuard {

    private static final Logger LOGGER = Logger.getLogger("UltimateFactionClaimGuard");
    private static final PluginIdentifier ULTIMATE_FACTION_ID = new PluginIdentifier("com.tinky", "Ultimate Faction");

    private static Method getClaimStoreMethod;
    private static Method claimStoreGetClaimMethod;
    private static boolean warnedMissingApi;
    private static boolean warnedApiFailure;

    private UltimateFactionClaimGuard() {
    }

    public static boolean isAvailable() {
        return getClaimStore() != null;
    }

    public static boolean isClaimedAt(World world, int blockX, int blockY, int blockZ) {
        if (world == null) {
            return false;
        }

        Object claimStore = getClaimStore();
        if (claimStore == null) {
            return false;
        }

        int chunkX = ChunkUtil.chunkCoordinate(blockX);
        int chunkZ = ChunkUtil.chunkCoordinate(blockZ);

        try {
            if (claimStoreGetClaimMethod == null) {
                claimStoreGetClaimMethod = claimStore.getClass().getMethod("getClaim", String.class, int.class, int.class);
            }

            if (isClaimPresent(claimStore, world.getName(), chunkX, chunkZ)) {
                return true;
            }

            String lowerName = world.getName().toLowerCase(Locale.ROOT);
            if (!lowerName.equals(world.getName()) && isClaimPresent(claimStore, lowerName, chunkX, chunkZ)) {
                return true;
            }

            return false;
        } catch (Exception e) {
            if (!warnedApiFailure) {
                warnedApiFailure = true;
                LOGGER.warning("UltimateFaction API call failed, claim checks disabled for this run: " + e.getMessage());
            }
            return false;
        }
    }

    private static boolean isClaimPresent(Object claimStore, String worldKey, int chunkX, int chunkZ) throws Exception {
        Object result = claimStoreGetClaimMethod.invoke(claimStore, worldKey, chunkX, chunkZ);
        if (result instanceof Optional<?> optional) {
            return optional.isPresent();
        }

        if (result == null) {
            return false;
        }

        Method isPresentMethod = result.getClass().getMethod("isPresent");
        Object present = isPresentMethod.invoke(result);
        return present instanceof Boolean && ((Boolean) present);
    }

    private static Object getClaimStore() {
        try {
            PluginManager pluginManager = PluginManager.get();
            if (pluginManager == null) {
                return null;
            }

            PluginBase plugin = pluginManager.getPlugin(ULTIMATE_FACTION_ID);
            if (plugin == null) {
                if (!warnedMissingApi) {
                    warnedMissingApi = true;
                    LOGGER.warning("UltimateFaction not detected. UltimateFaction claim checks are inactive.");
                }
                return null;
            }

            if (getClaimStoreMethod == null) {
                getClaimStoreMethod = plugin.getClass().getMethod("getClaimStore");
            }
            return getClaimStoreMethod.invoke(plugin);
        } catch (Exception e) {
            if (!warnedApiFailure) {
                warnedApiFailure = true;
                LOGGER.warning("UltimateFaction lookup failed, claim checks disabled for this run: " + e.getMessage());
            }
            return null;
        }
    }
}
