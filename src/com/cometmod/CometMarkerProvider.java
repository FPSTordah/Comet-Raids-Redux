package com.cometmod;

import com.cometmod.*;
import com.cometmod.commands.*;
import com.cometmod.services.*;
import com.cometmod.spawn.*;
import com.cometmod.systems.*;
import com.cometmod.wave.*;


import com.hypixel.hytale.protocol.Transform;
import com.hypixel.hytale.protocol.Position;
import com.hypixel.hytale.protocol.Direction;
import com.hypixel.hytale.protocol.FormattedMessage;
import com.hypixel.hytale.protocol.packets.worldmap.MapMarker;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.worldmap.WorldMapManager;
import com.hypixel.hytale.server.core.universe.world.worldmap.markers.MarkersCollector;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.math.vector.Vector3i;

import javax.annotation.Nonnull;
import java.util.logging.Logger;

public class CometMarkerProvider implements WorldMapManager.MarkerProvider {

    private static final Logger LOGGER = Logger.getLogger("CometMarkerProvider");
    public static final CometMarkerProvider INSTANCE = new CometMarkerProvider();

    private CometMarkerProvider() {
    }
    
    @Override
    public void update(@Nonnull World world, @Nonnull Player viewingPlayer, @Nonnull MarkersCollector collector) {

        // Get the wave manager to access active comets
        CometWaveManager waveManager = CometModPlugin.getWaveManager();
        if (waveManager == null) {
            return;
        }

        try {
            java.util.UUID viewingPlayerUUID = viewingPlayer.getUuid();
            
            // Get active comets from wave manager
            java.util.Map<Vector3i, CometWaveManager.CometState> activeComets = 
                waveManager.getActiveComets();
            java.util.Map<Vector3i, CometTier> cometTiers = 
                waveManager.getCometTiers();
            java.util.Map<Vector3i, java.util.UUID> cometOwners = 
                waveManager.getCometOwners();
            
            if (activeComets == null || activeComets.isEmpty()) {
                return;  // No logging - this is called constantly
            }
            
            // Iterate through all active comets
            for (java.util.Map.Entry<Vector3i, CometWaveManager.CometState> entry : activeComets.entrySet()) {
                Vector3i blockPos = entry.getKey();
                CometWaveManager.CometState state = entry.getValue();
                
                // Check ownership - only show markers for comets owned by this player
                java.util.UUID ownerUUID = cometOwners != null ? cometOwners.get(blockPos) : null;
                if (ownerUUID != null && !ownerUUID.equals(viewingPlayerUUID)) {
                    // This comet belongs to someone else, don't show marker
                    continue;
                }
                
                // Get tier for this comet
                CometTier tier = cometTiers != null ? cometTiers.get(blockPos) : null;
                if (tier == null) {
                    tier = CometTier.UNCOMMON; // Default tier
                }
                
                // Convert block position to world position (center of block)
                Vector3d markerPos = blockPos.toVector3d();
                
                // Create marker ID (unique per comet position)
                String markerId = "Comet-" + blockPos.x + "," + blockPos.y + "," + blockPos.z;
                
                // Create marker name with tier and state info
                String markerName = "Comet (" + tier.getName() + ")";
                if (state == CometWaveManager.CometState.WAVE_ACTIVE) {
                    markerName += " - Active";
                } else if (state == CometWaveManager.CometState.COMPLETED) {
                    markerName += " - Completed";
                }
                
                // Determine icon based on tier
                String iconPath = getIconPathForTier(tier);
                
                // Create Transform using PROTOCOL classes (like MapTrail does!)
                Position position = new Position(markerPos.x, markerPos.y, markerPos.z);
                Direction direction = new Direction();  // Zero rotation
                Transform transform = new Transform(position, direction);
                
                // Create FormattedMessage for the marker name
                FormattedMessage nameMsg = new FormattedMessage();
                nameMsg.rawText = markerName;

                // Create the MapMarker
                MapMarker marker = new MapMarker(
                    markerId,
                    nameMsg,
                    null,       // customName
                    iconPath,
                    transform,
                    null,       // no context menu items
                    null        // no components
                );

                // Add marker via collector (handles view distance internally)
                collector.add(marker);
            }
        } catch (Exception e) {
            LOGGER.warning("Error updating comet markers: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private String getIconPathForTier(CometTier tier) {
        switch (tier) {
            case UNCOMMON:
                return "Comet_Stone_Uncommon.png";
            case EPIC:
                return "Comet_Stone_Epic.png";
            case RARE:
                return "Comet_Stone_Rare.png";
            case LEGENDARY:
                return "Comet_Stone_Legendary.png";
            default:
                return "Comet_Stone_Uncommon.png";
        }
    }
}
