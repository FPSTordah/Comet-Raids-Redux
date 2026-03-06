package com.cometmod.systems;

import com.cometmod.CometConfig;
import com.cometmod.services.*;
import com.cometmod.spawn.*;
import com.cometmod.wave.*;


import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.protocol.InteractionType;
import com.hypixel.hytale.server.core.event.events.ecs.UseBlockEvent;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.component.system.EntityEventSystem;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class CometBlockEventSystem extends EntityEventSystem<EntityStore, UseBlockEvent.Pre> {
    
    private final CometWaveManager waveManager;
    private static final java.util.logging.Logger LOGGER = java.util.logging.Logger.getLogger(CometBlockEventSystem.class.getName());
    
    public CometBlockEventSystem(CometWaveManager waveManager) {
        super(UseBlockEvent.Pre.class);
        this.waveManager = waveManager;
    }
    
    @Override
    @Nullable
    public Query<EntityStore> getQuery() {
        // Return Query.any() to match all entities (events are dispatched on the triggering entity)
        return Query.any();
    }
    
    @Override
    public void handle(int index, @Nonnull ArchetypeChunk<EntityStore> archetypeChunk, 
                      @Nonnull Store<EntityStore> store, 
                      @Nonnull CommandBuffer<EntityStore> commandBuffer, 
                      @Nonnull UseBlockEvent.Pre event) {
        com.hypixel.hytale.math.vector.Vector3i blockPos = event.getTargetBlock();
        if (blockPos == null) return;

        if (CometConfig.DEBUG) {
            LOGGER.info("[CometDebug] UseBlockEvent at " + blockPos + " interactionType=" + event.getInteractionType() + " (CometBlockEventSystem, position-based)");
        }

        // Position-based: any block at a registered comet position (or within radius for multi-block assets) activates
        com.hypixel.hytale.math.vector.Vector3i registeredPos = waveManager.getRegisteredBlockPos(blockPos.x, blockPos.y, blockPos.z);
        boolean exactMatch = (registeredPos != null);
        if (registeredPos == null) {
            registeredPos = waveManager.getRegisteredBlockPosNear(blockPos.x, blockPos.y, blockPos.z, CometConfig.COMET_USE_NEAR_RADIUS);
        }
        if (registeredPos == null) {
            if (CometConfig.DEBUG) {
                LOGGER.info("[CometDebug] CometBlockEventSystem: no comet registered at/near " + blockPos + ", ignoring");
            }
            return;
        }
        blockPos = registeredPos;
        // Cancel immediately so vanilla block Use (e.g. OpenContainer) never runs, even if we later return early
        event.setCancelled(true);
        if (CometConfig.DEBUG) {
            LOGGER.info("[CometDebug] CometBlockEventSystem: comet registered at " + blockPos + " (" + (exactMatch ? "exact" : "near") + "), cancelled vanilla Use");
        }

        // Only handle Use (f key) interactions - same as chests
        if (event.getInteractionType() != com.hypixel.hytale.protocol.InteractionType.Use) {
            if (CometConfig.DEBUG) {
                LOGGER.info("[CometDebug] CometBlockEventSystem: skipping activation, interactionType is not Use");
            }
            return;
        }

        // Check if block exists - use getBlockType (getState can be null for furniture/custom blocks)
        try {
            com.hypixel.hytale.server.core.universe.world.World world = 
                ((com.hypixel.hytale.server.core.universe.world.storage.EntityStore)store.getExternalData()).getWorld();
            com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockType blockTypeAt = 
                world.getBlockType(blockPos.x, blockPos.y, blockPos.z);
            if (blockTypeAt == null || blockTypeAt.isUnknown()) {
                if (CometConfig.DEBUG) {
                    LOGGER.info("[CometDebug] CometBlockEventSystem: no block at " + blockPos + ", skipping");
                }
                return; // Block doesn't exist or chunk not loaded, don't activate
            }
        } catch (Exception e) {
            LOGGER.warning("[CometBlockEventSystem] Error checking block: " + e.getMessage());
        }
        
        // Get player entity ref from context
        Ref<EntityStore> playerRef = event.getContext().getEntity();
        if (playerRef == null || !playerRef.isValid()) {
            LOGGER.warning("[CometBlockEventSystem] PlayerRef is null or invalid!");
            if (CometConfig.DEBUG) {
                LOGGER.info("[CometDebug] CometBlockEventSystem: skipping, playerRef null or invalid");
            }
            return;
        }

        if (CometConfig.DEBUG) {
            LOGGER.info("[CometDebug] CometBlockEventSystem: activating comet at " + blockPos + " (position-based Use)");
        }
        // Handle comet activation (vanilla Use already cancelled above when we found the comet)
        try {
            waveManager.handleCometActivation(store, playerRef, blockPos);
        } catch (Exception e) {
            LOGGER.severe("[CometBlockEventSystem] Error in handleCometActivation: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
