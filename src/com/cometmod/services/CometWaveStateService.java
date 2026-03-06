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
    private final Map<Vector3i, Integer> cometZones = new ConcurrentHashMap<>();
    private final Map<Vector3i, String> cometThemes = new ConcurrentHashMap<>();
    private final Map<Vector3i, String> forcedThemes = new ConcurrentHashMap<>();
    /** Maps any block position that is part of a comet asset (e.g. multi-block chest/coffin) to the canonical comet position. */
    private final Map<Vector3i, Vector3i> triggerPosToCanonical = new ConcurrentHashMap<>();

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
        registerTriggerBox(blockPos);
        if (ownerUUID != null) {
            cometOwners.put(blockPos, ownerUUID);
            logger.info("Registered tier " + tier.getName() + " for comet at " + blockPos + " (owner: " + ownerUUID + ")");
        } else {
            logger.info("Registered tier " + tier.getName() + " for comet at " + blockPos + " (no owner)");
        }
    }

    /** Register every block in a box around the comet so any part of a multi-block asset (chest, coffin) triggers the comet. */
    private void registerTriggerBox(Vector3i canonical) {
        int r = CometConfig.COMET_ASSET_BOX_RADIUS;
        for (int dx = -r; dx <= r; dx++) {
            for (int dy = -r; dy <= r; dy++) {
                for (int dz = -r; dz <= r; dz++) {
                    triggerPosToCanonical.put(new Vector3i(canonical.x + dx, canonical.y + dy, canonical.z + dz), canonical);
                }
            }
        }
    }

    public UUID getOwner(Vector3i blockPos) {
        return cometOwners.get(blockPos);
    }

    public CometTier getTierOrDefault(Vector3i blockPos, CometTier fallback) {
        if (blockPos == null) return fallback;
        CometTier t = cometTiers.get(blockPos);
        if (t != null) return t;
        Vector3i canonical = findRegisteredPosition(blockPos.x, blockPos.y, blockPos.z);
        return canonical != null ? cometTiers.getOrDefault(canonical, fallback) : fallback;
    }

    /** Find the registered comet position with these coordinates (map key may be a different Vector3i instance). Checks asset trigger box first so any block of a multi-block asset works. */
    public Vector3i findRegisteredPosition(int x, int y, int z) {
        Vector3i canonical = triggerPosToCanonical.get(new Vector3i(x, y, z));
        if (canonical != null) return canonical;
        for (Vector3i pos : cometTiers.keySet()) {
            if (pos.x == x && pos.y == y && pos.z == z) return pos;
        }
        return null;
    }

    /** Find a registered comet position within manhattan distance of (x,y,z). Used when the clicked block may be adjacent. */
    public Vector3i findRegisteredPositionNear(int x, int y, int z, int maxDistance) {
        if (maxDistance <= 0) return findRegisteredPosition(x, y, z);
        Vector3i exact = findRegisteredPosition(x, y, z);
        if (exact != null) return exact;
        for (Vector3i pos : cometTiers.keySet()) {
            int dx = Math.abs(pos.x - x);
            int dy = Math.abs(pos.y - y);
            int dz = Math.abs(pos.z - z);
            if (dx <= maxDistance && dy <= maxDistance && dz <= maxDistance) return pos;
        }
        return null;
    }

    public void registerCometZone(Vector3i blockPos, int zoneId) {
        cometZones.put(blockPos, zoneId);
    }

    public Integer getZone(Vector3i blockPos) {
        return cometZones.get(blockPos);
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
        cometZones.remove(blockPos);
        triggerPosToCanonical.entrySet().removeIf(e -> e.getValue().x == blockPos.x && e.getValue().y == blockPos.y && e.getValue().z == blockPos.z);
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

    public Map<Vector3i, Integer> cometZones() {
        return cometZones;
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
