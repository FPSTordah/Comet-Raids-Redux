package com.cometmod.commands;

import com.cometmod.*;
import com.cometmod.commands.*;
import com.cometmod.services.*;
import com.cometmod.spawn.*;
import com.cometmod.systems.*;
import com.cometmod.wave.*;


import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractWorldCommand;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.WorldMapTracker;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.component.Store;
import com.cometmod.config.model.ZoneSpawnChances;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.logging.Logger;

/**
 * Command to check which zone the player is currently in
 */
public class CometZoneCommand extends AbstractWorldCommand {
    
    private static final Logger LOGGER = Logger.getLogger(CometZoneCommand.class.getName());
    
    public CometZoneCommand() {
        super("zone", "Check which zone you are currently in");
        requirePermission(CometPermissions.ZONE);
    }
    
    @Override
    protected void execute(@Nonnull CommandContext context, 
                          @Nonnull World world, 
                          @Nonnull Store<EntityStore> store) {
        if (!context.isPlayer()) {
            context.sendMessage(Message.raw("This command can only be used by players!"));
            return;
        }
        
        try {
            Player player = context.senderAs(Player.class);
            
            // Get player's current zone
            WorldMapTracker tracker = player.getWorldMapTracker();
            
            // Force update zone info by getting player position and triggering zone check
            // The WorldMapTracker updates zone info periodically, but we can check it directly
            com.hypixel.hytale.component.Ref<com.hypixel.hytale.server.core.universe.world.storage.EntityStore> playerRef = 
                player.getReference();
            if (playerRef != null && playerRef.isValid()) {
                com.hypixel.hytale.server.core.modules.entity.component.TransformComponent transform = 
                    store.getComponent(playerRef, com.hypixel.hytale.server.core.modules.entity.component.TransformComponent.getComponentType());
                if (transform != null) {
                    com.hypixel.hytale.math.vector.Vector3d pos = transform.getPosition();
                    LOGGER.info("Player position: " + pos + " - checking zone info");
                }
            }
            
            WorldMapTracker.ZoneDiscoveryInfo zoneInfo = tracker != null ? tracker.getCurrentZone() : null;
            
            if (zoneInfo == null) {
                context.sendMessage(Message.raw("You are not in any zone (or zone not detected yet)."));
                context.sendMessage(Message.raw("Try moving around a bit - zone detection may take a moment."));
                LOGGER.info("Player " + player.getDisplayName() + " has no zone info");
                return;
            }
            
            String zoneName = zoneInfo.zoneName();
            String regionName = zoneInfo.regionName();
            
            // Parse zone ID from region name first (e.g., "Zone4_Tier4" -> 4), then fallback to zone name
            int zoneId = parseZoneId(regionName);
            if (zoneId == 0) {
                zoneId = parseZoneId(zoneName);
            }
            
            LOGGER.info("Zone parsing - Zone name: '" + zoneName + "', Region: '" + regionName + "', Parsed Zone ID: " + zoneId);
            
            // Get tier distribution info for this zone
            String tierInfo = getTierInfoForZone(zoneId);
            
            // Display zone information
            context.sendMessage(Message.raw("Current Zone: " + zoneName));
            context.sendMessage(Message.raw("Region: " + regionName));
            context.sendMessage(Message.raw("Zone ID: " + zoneId));
            context.sendMessage(Message.raw("Comet Tier Distribution: " + tierInfo));
            
            LOGGER.info("Player " + player.getDisplayName() + " is in zone: " + zoneName + ", region: " + regionName + " (ID: " + zoneId + ")");
            
        } catch (Exception e) {
            LOGGER.warning("Error in zone command: " + e.getMessage());
            e.printStackTrace();
            context.sendMessage(Message.raw("Error: " + e.getMessage()));
        }
    }
    
    /**
     * Parse zone ID from zone name or region name
     * Looks for "Zone" followed by a number (e.g., "Zone1" -> 1, "Zone4_Tier4" -> 4)
     */
    private int parseZoneId(String name) {
        if (name == null || name.isEmpty()) {
            return 0;
        }
        
        // Look for "Zone" followed by digits (case-insensitive)
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("(?i)zone(\\d+)");
        java.util.regex.Matcher matcher = pattern.matcher(name);
        
        if (matcher.find()) {
            try {
                return Integer.parseInt(matcher.group(1));
            } catch (NumberFormatException e) {
                // Fall through
            }
        }
        
        // Fallback: try to extract first number sequence
        java.util.regex.Pattern fallbackPattern = java.util.regex.Pattern.compile("\\d+");
        java.util.regex.Matcher fallbackMatcher = fallbackPattern.matcher(name);
        
        if (fallbackMatcher.find()) {
            try {
                return Integer.parseInt(fallbackMatcher.group());
            } catch (NumberFormatException e) {
                // Fall through
            }
        }
        
        return 0;
    }
    
    /**
     * Get tier distribution info for a zone
     */
    private String getTierInfoForZone(int zoneId) {
        CometConfig config = CometConfig.getInstance();
        if (config == null) {
            return "Config unavailable";
        }

        ZoneSpawnChances chances = config.getZoneSpawnChances(zoneId);
        if (chances == null) {
            return "No spawn chances configured for this zone";
        }

        List<String> parts = new ArrayList<>();
        if (CometConfig.isTier5Enabled()) {
            appendTierPart(parts, "Uncommon", chances.getTier1());
            appendTierPart(parts, "Rare", chances.getTier2());
            appendTierPart(parts, "Epic", chances.getTier3());
            appendTierPart(parts, "Legendary", chances.getTier4());
            appendTierPart(parts, "Mythic", chances.getTier5());
            return parts.isEmpty() ? "No active tiers in this zone" : String.join(", ", parts);
        }

        double t1 = chances.getTier1();
        double t2 = chances.getTier2();
        double t3 = chances.getTier3();
        double t4 = chances.getTier4();
        double totalWithoutMythic = t1 + t2 + t3 + t4;

        if (totalWithoutMythic <= 0.0) {
            return "No active tiers in this zone (Tier 5 disabled)";
        }

        appendTierPart(parts, "Uncommon", t1 / totalWithoutMythic);
        appendTierPart(parts, "Rare", t2 / totalWithoutMythic);
        appendTierPart(parts, "Epic", t3 / totalWithoutMythic);
        appendTierPart(parts, "Legendary", t4 / totalWithoutMythic);

        String info = parts.isEmpty() ? "No active tiers in this zone" : String.join(", ", parts);
        if (chances.getTier5() > 0.0) {
            info += " (Mythic disabled: Endgame&QoL missing)";
        }
        return info;
    }

    private void appendTierPart(List<String> parts, String name, double chance) {
        if (chance <= 0.0) {
            return;
        }
        parts.add(String.format(Locale.ROOT, "%.0f%% %s", chance * 100.0, name));
    }
}
