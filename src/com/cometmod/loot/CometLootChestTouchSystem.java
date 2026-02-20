package com.cometmod.loot;

import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.EntityEventSystem;
import com.hypixel.hytale.protocol.InteractionType;
import com.hypixel.hytale.server.core.event.events.ecs.UseBlockEvent;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.logging.Logger;

/**
 * Resets reward chest expiry timer whenever a tracked chest is touched/opened.
 */
public class CometLootChestTouchSystem extends EntityEventSystem<EntityStore, UseBlockEvent.Pre> {

    private static final Logger LOGGER = Logger.getLogger("CometLootChestTouchSystem");

    public CometLootChestTouchSystem() {
        super(UseBlockEvent.Pre.class);
    }

    @Override
    @Nullable
    public Query<EntityStore> getQuery() {
        return Query.any();
    }

    @Override
    public void handle(int index, @Nonnull ArchetypeChunk<EntityStore> archetypeChunk,
            @Nonnull Store<EntityStore> store,
            @Nonnull CommandBuffer<EntityStore> commandBuffer,
            @Nonnull UseBlockEvent.Pre event) {

        if (event.getInteractionType() != InteractionType.Use) {
            return;
        }

        com.hypixel.hytale.math.vector.Vector3i blockPos = event.getTargetBlock();
        CometLootChestService chestService = CometLootChestService.getInstance();
        if (!chestService.isManagedChest(blockPos)) {
            return;
        }

        try {
            Object external = store.getExternalData();
            if (!(external instanceof EntityStore)) {
                return;
            }
            World world = ((EntityStore) external).getWorld();
            chestService.touchChest(world, blockPos);
        } catch (Exception e) {
            LOGGER.warning("Failed to reset chest expiry timer: " + e.getMessage());
        }
    }
}
