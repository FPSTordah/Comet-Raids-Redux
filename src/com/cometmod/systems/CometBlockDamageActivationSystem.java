package com.cometmod.systems;

import com.cometmod.CometConfig;
import com.cometmod.wave.CometWaveManager;

import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.EntityEventSystem;
import com.hypixel.hytale.server.core.event.events.ecs.DamageBlockEvent;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Fallback activation path for comet blocks that don't expose a Use interaction
 * (e.g. some portal/furniture assets). When the player damages a registered
 * comet block and the comet is still untouched, we treat that as an activation:
 * trigger the wave and cancel the damage so the block is not broken.
 */
public class CometBlockDamageActivationSystem extends EntityEventSystem<EntityStore, DamageBlockEvent> {

    private final CometWaveManager waveManager;
    private static final java.util.logging.Logger LOGGER = java.util.logging.Logger.getLogger(CometBlockDamageActivationSystem.class.getName());

    public CometBlockDamageActivationSystem(CometWaveManager waveManager) {
        super(DamageBlockEvent.class);
        this.waveManager = waveManager;
    }

    @Override
    @Nullable
    public Query<EntityStore> getQuery() {
        return Query.any();
    }

    @Override
    public void handle(int index,
                       @Nonnull ArchetypeChunk<EntityStore> archetypeChunk,
                       @Nonnull Store<EntityStore> store,
                       @Nonnull CommandBuffer<EntityStore> commandBuffer,
                       @Nonnull DamageBlockEvent event) {

        com.hypixel.hytale.math.vector.Vector3i blockPos = event.getTargetBlock();
        if (blockPos == null) {
            return;
        }

        // Position-based lookup: any block at or very near a registered comet can activate it
        com.hypixel.hytale.math.vector.Vector3i registeredPos = waveManager.getRegisteredBlockPos(blockPos.x, blockPos.y, blockPos.z);
        boolean exactMatch = (registeredPos != null);
        if (registeredPos == null) {
            registeredPos = waveManager.getRegisteredBlockPosNear(blockPos.x, blockPos.y, blockPos.z, CometConfig.COMET_USE_NEAR_RADIUS);
        }
        if (registeredPos == null) {
            return; // Not part of a comet
        }

        // Only activate if comet is untouched; once active/completed, let normal damage/break logic run
        CometWaveManager.CometState state = waveManager.getCometState(registeredPos);
        if (state != CometWaveManager.CometState.UNTOUCHED) {
            return;
        }

        if (CometConfig.DEBUG) {
            LOGGER.info("[CometDebug] DamageBlockEvent at " + blockPos
                    + " mapped to comet at " + registeredPos
                    + " (" + (exactMatch ? "exact" : "near") + "), activating comet and cancelling damage");
        }

        // Resolve the player entity that damaged the block
        Ref<EntityStore> playerRef = archetypeChunk.getReferenceTo(index);
        if (playerRef == null || !playerRef.isValid()) {
            return;
        }

        try {
            // Cancel damage so the block is not broken on first hit
            event.setCancelled(true);
        } catch (Exception e) {
            LOGGER.warning("Failed to cancel DamageBlockEvent: " + e.getMessage());
        }

        try {
            waveManager.handleCometActivation(store, playerRef, registeredPos);
        } catch (Exception e) {
            LOGGER.severe("[CometBlockDamageActivationSystem] Error in handleCometActivation: " + e.getMessage());
            e.printStackTrace();
        }
    }
}

