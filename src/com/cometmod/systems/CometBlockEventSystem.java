package com.cometmod.systems;

import com.cometmod.*;
import com.cometmod.commands.*;
import com.cometmod.services.*;
import com.cometmod.spawn.*;
import com.cometmod.systems.*;
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
    private static final java.util.logging.Logger LOGGER = java.util.logging.Logger.getLogger("CometBlockEventSystem");
    
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
        String blockTypeId = event.getBlockType().getId();

        // Check if the block is a Comet_Stone variant (any tier)
        if (!blockTypeId.startsWith("Comet_Stone_")) {
            return;
        }

        // Only handle Use (f key) interactions - same as chests
        if (event.getInteractionType() != com.hypixel.hytale.protocol.InteractionType.Use) {
            return;
        }

        // Get the exact block position
        com.hypixel.hytale.math.vector.Vector3i blockPos = event.getTargetBlock();
        
        // Check if block exists - if not, it was already broken (completed wave)
        try {
            com.hypixel.hytale.server.core.universe.world.World world = 
                ((com.hypixel.hytale.server.core.universe.world.storage.EntityStore)store.getExternalData()).getWorld();
            com.hypixel.hytale.server.core.universe.world.meta.BlockState blockState = 
                world.getState(blockPos.x, blockPos.y, blockPos.z, true);
            
            if (blockState == null) {
                return; // Block doesn't exist, don't activate
            }
        } catch (Exception e) {
            LOGGER.warning("[CometBlockEventSystem] Error checking block state: " + e.getMessage());
        }
        
        // Get player entity ref from context
        Ref<EntityStore> playerRef = event.getContext().getEntity();
        if (playerRef == null || !playerRef.isValid()) {
            LOGGER.warning("[CometBlockEventSystem] PlayerRef is null or invalid!");
            return;
        }

        // Handle comet activation
        try {
            waveManager.handleCometActivation(store, playerRef, blockPos);
        } catch (Exception e) {
            LOGGER.severe("[CometBlockEventSystem] Error in handleCometActivation: " + e.getMessage());
            e.printStackTrace();
        }
        
        // Cancel the event to prevent default interaction (opening a container UI if it were a chest)
        event.setCancelled(true);
    }
}
