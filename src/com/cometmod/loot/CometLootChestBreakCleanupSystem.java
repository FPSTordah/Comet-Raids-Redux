package com.cometmod.loot;

import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.EntityEventSystem;
import com.hypixel.hytale.server.core.event.events.ecs.BreakBlockEvent;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.meta.BlockState;
import com.hypixel.hytale.server.core.universe.world.meta.state.ItemContainerState;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Logger;

/**
 * Cleans loot chest timers/tracking when a managed chest is broken manually.
 */
public class CometLootChestBreakCleanupSystem extends EntityEventSystem<EntityStore, BreakBlockEvent> {

    private static final Logger LOGGER = Logger.getLogger("CometLootChestBreakCleanupSystem");

    public CometLootChestBreakCleanupSystem() {
        super(BreakBlockEvent.class);
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
            @Nonnull BreakBlockEvent event) {

        com.hypixel.hytale.math.vector.Vector3i blockPos = event.getTargetBlock();
        CometLootChestService chestService = CometLootChestService.getInstance();
        if (!chestService.isManagedChest(blockPos)) {
            return;
        }

        try {
            Object external = store.getExternalData();
            if (external instanceof EntityStore) {
                World world = ((EntityStore) external).getWorld();
                BlockState state = world.getState(blockPos.x, blockPos.y, blockPos.z, true);
                if (state instanceof ItemContainerState) {
                    ItemContainerState containerState = (ItemContainerState) state;
                    Map<UUID, com.hypixel.hytale.server.core.entity.entities.player.windows.ContainerBlockWindow> windows = containerState
                            .getWindows();
                    if (windows != null && !windows.isEmpty()) {
                        windows.clear();
                    }
                }
            }
        } catch (Exception e) {
            LOGGER.warning("Failed to clear chest windows on break: " + e.getMessage());
        } finally {
            chestService.handleChestBroken(blockPos);
        }
    }
}
