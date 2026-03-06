package com.cometmod.integration;

import com.cometmod.CometConfig;
import com.hypixel.hytale.server.core.universe.world.World;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

/**
 * Generic claim integration gate for comet spawning using built-in providers only.
 */
public final class ClaimProtectionGuard {

    private static final Logger LOGGER = Logger.getLogger(ClaimProtectionGuard.class.getName());
    private static final Set<String> warnedUnknownProviders = ConcurrentHashMap.newKeySet();
    private static final List<String> SUPPORTED_PROVIDER_KEYS = List.of(
            "worldprotect",
            "hyperfactions",
            "hyfaction",
            "simpleclaims",
            "wiflowsclaims",
            "ultimatefaction",
            "elbaphfactions");

    private ClaimProtectionGuard() {
    }

    public static boolean canSpawnAt(World world, int x, int y, int z, CometConfig config) {
        if (config == null) {
            return true;
        }

        boolean claimProtectActive = config.isClaimProtectEnabled();
        if (claimProtectActive) {
            for (String providerKey : resolveProviderKeys(config)) {
                if (isClaimedByProvider(providerKey, world, x, y, z)) {
                    return false;
                }
            }
        }

        return WorldProtectRegionGuard.canSpawnAt(world, x, y, z, config);
    }

    public static List<String> getResolvedProviderKeys(CometConfig config) {
        return new ArrayList<>(resolveProviderKeys(config));
    }

    private static boolean isClaimedByProvider(String providerKey, World world, int x, int y, int z) {
        return switch (providerKey) {
            case "worldprotect" -> WorldProtectRegionGuard.isClaimedAt(world, x, y, z);
            case "hyperfactions" -> HyperFactionsClaimGuard.isClaimedAt(world, x, y, z);
            case "hyfaction" -> HyfactionClaimGuard.isClaimedAt(world, x, y, z);
            case "simpleclaims" -> SimpleClaimsClaimGuard.isClaimedAt(world, x, y, z);
            case "wiflowsclaims" -> WiFlowsClaimsClaimGuard.isClaimedAt(world, x, y, z);
            case "ultimatefaction" -> UltimateFactionClaimGuard.isClaimedAt(world, x, y, z);
            case "elbaphfactions" -> ElbaphFactionsClaimGuard.isClaimedAt(world, x, y, z);
            default -> {
                warnUnknownProviderOnce(providerKey);
                yield false;
            }
        };
    }

    private static List<String> resolveProviderKeys(CometConfig config) {
        if (config.isClaimProtectAutoDetectProviders()) {
            List<String> detected = new ArrayList<>();
            for (String providerKey : SUPPORTED_PROVIDER_KEYS) {
                if (isProviderAvailable(providerKey)) {
                    detected.add(providerKey);
                }
            }
            return detected;
        }

        Set<String> keys = new LinkedHashSet<>();
        for (String providerName : config.getClaimProtectProviders()) {
            if (providerName == null || providerName.isBlank()) {
                continue;
            }
            String providerKey = canonicalProviderKey(providerName);
            if (providerKey != null) {
                keys.add(providerKey);
            }
        }
        return new ArrayList<>(keys);
    }

    private static boolean isProviderAvailable(String providerKey) {
        return switch (providerKey) {
            case "worldprotect" -> WorldProtectRegionGuard.isAvailable();
            case "hyperfactions" -> HyperFactionsClaimGuard.isAvailable();
            case "hyfaction" -> HyfactionClaimGuard.isAvailable();
            case "simpleclaims" -> SimpleClaimsClaimGuard.isAvailable();
            case "wiflowsclaims" -> WiFlowsClaimsClaimGuard.isAvailable();
            case "ultimatefaction" -> UltimateFactionClaimGuard.isAvailable();
            case "elbaphfactions" -> ElbaphFactionsClaimGuard.isAvailable();
            default -> false;
        };
    }

    private static String canonicalProviderKey(String providerName) {
        String normalized = providerName
                .trim()
                .toLowerCase(Locale.ROOT)
                .replace(" ", "")
                .replace("-", "")
                .replace("_", "");

        if (normalized.isEmpty()) {
            return null;
        }

        return switch (normalized) {
            case "worldprotect", "worldguard", "wp" -> "worldprotect";
            case "hyperfactions", "hyperfaction" -> "hyperfactions";
            case "hyfaction", "hyfactions" -> "hyfaction";
            case "simpleclaim", "simpleclaims" -> "simpleclaims";
            case "wiflowclaims", "wiflowclaim", "wiflowsclaim", "wiflowsclaims", "wiflows" -> "wiflowsclaims";
            case "ultimatefaction", "ultimatefactions", "ultimate", "tinkyfactions", "hytalefactions" -> "ultimatefaction";
            case "elbaphfaction", "elbaphfactions", "elbaph" -> "elbaphfactions";
            default -> normalized;
        };
    }

    private static void warnUnknownProviderOnce(String providerKey) {
        if (warnedUnknownProviders.add(providerKey)) {
            LOGGER.warning("ClaimProtect provider '" + providerKey
                    + "' is not supported. Built-in: worldprotect, hyperfactions, hyfaction, simpleclaims, wiflowsclaims, ultimatefaction, elbaphfactions.");
        }
    }
}
