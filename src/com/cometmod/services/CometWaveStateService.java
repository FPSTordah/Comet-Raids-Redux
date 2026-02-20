package com.cometmod.services;

import com.cometmod.*;
import com.cometmod.commands.*;
import com.cometmod.services.*;
import com.cometmod.spawn.*;
import com.cometmod.systems.*;
import com.cometmod.wave.*;


import com.hypixel.hytale.math.vector.Vector3i;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

/**
 * Central state holder for comet ownership/tier/theme tracking.
 */
public class CometWaveStateService {

    private final Map<Vector3i, CometWaveManager.CometState> activeComets = new ConcurrentHashMap<>();
    private final Map<Vector3i, CometTier> cometTiers = new ConcurrentHashMap<>();
    private final Map<Vector3i, UUID> cometOwners = new ConcurrentHashMap<>();
    private final Map<Vector3i, String> cometThemes = new ConcurrentHashMap<>();
    private final Map<Vector3i, String> forcedThemes = new ConcurrentHashMap<>();

    public CometWaveManager.CometState getCometState(Vector3i blockPos) {
        CometWaveManager.CometState state = activeComets.get(blockPos);
        return state != null ? state : CometWaveManager.CometState.UNTOUCHED;
    }

    public Map<Vector3i, CometWaveManager.CometState> getActiveCometsSnapshot() {
        return new HashMap<>(activeComets);
    }

    public Map<Vector3i, CometTier> getCometTiersSnapshot() {
        return new HashMap<>(cometTiers);
    }

    public Map<Vector3i, UUID> getCometOwnersSnapshot() {
        return new HashMap<>(cometOwners);
    }

    public boolean hasActiveCometNear(int x, int y, int z, int distance) {
        for (Vector3i pos : activeComets.keySet()) {
            if (distance(pos, x, y, z) <= distance) {
                return true;
            }
        }
        for (Vector3i pos : cometTiers.keySet()) {
            if (distance(pos, x, y, z) <= distance) {
                return true;
            }
        }
        return false;
    }

    public void registerCometTier(Vector3i blockPos, CometTier tier, UUID ownerUUID, Logger logger) {
        cometTiers.put(blockPos, tier);
        if (ownerUUID != null) {
            cometOwners.put(blockPos, ownerUUID);
            logger.info("Registered tier " + tier.getName() + " for comet at " + blockPos + " (owner: " + ownerUUID + ")");
        } else {
            logger.info("Registered tier " + tier.getName() + " for comet at " + blockPos + " (no owner)");
        }
    }

    public UUID getOwner(Vector3i blockPos) {
        return cometOwners.get(blockPos);
    }

    public CometTier getTierOrDefault(Vector3i blockPos, CometTier fallback) {
        return cometTiers.getOrDefault(blockPos, fallback);
    }

    public void setTheme(Vector3i blockPos, String themeId) {
        cometThemes.put(blockPos, themeId);
    }

    public String getTheme(Vector3i blockPos) {
        return cometThemes.get(blockPos);
    }

    public void forceTheme(Vector3i blockPos, String themeId, Logger logger) {
        forcedThemes.put(blockPos, themeId);
        logger.info("Forced theme '" + themeId + "' for comet at " + blockPos);
    }

    public String getForcedTheme(Vector3i blockPos) {
        return forcedThemes.get(blockPos);
    }

    public boolean hasForcedTheme(Vector3i blockPos) {
        return forcedThemes.containsKey(blockPos);
    }

    public void clearForcedTheme(Vector3i blockPos) {
        forcedThemes.remove(blockPos);
    }

    public void clearForBlock(Vector3i blockPos) {
        activeComets.remove(blockPos);
        cometTiers.remove(blockPos);
        cometOwners.remove(blockPos);
        cometThemes.remove(blockPos);
        forcedThemes.remove(blockPos);
    }

    public Map<Vector3i, CometWaveManager.CometState> activeComets() {
        return activeComets;
    }

    public Map<Vector3i, CometTier> cometTiers() {
        return cometTiers;
    }

    public Map<Vector3i, UUID> cometOwners() {
        return cometOwners;
    }

    public Map<Vector3i, String> cometThemes() {
        return cometThemes;
    }

    public Map<Vector3i, String> forcedThemes() {
        return forcedThemes;
    }

    private static double distance(Vector3i pos, int x, int y, int z) {
        double dx = pos.x - x;
        double dy = pos.y - y;
        double dz = pos.z - z;
        return Math.sqrt(dx * dx + dy * dy + dz * dz);
    }
}
