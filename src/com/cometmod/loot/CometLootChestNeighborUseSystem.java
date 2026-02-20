package com.cometmod.loot;

import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.EntityEventSystem;
import com.hypixel.hytale.math.vector.Vector3i;
import com.hypixel.hytale.protocol.InteractionType;
import com.hypixel.hytale.server.core.event.events.ecs.UseBlockEvent;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Allows interacting with managed reward chests from adjacent blocks so
 * oversized chest models still open reliably from either side.
 */
public class CometLootChestNeighborUseSystem extends EntityEventSystem<EntityStore, UseBlockEvent.Pre> {

    private static final int[][] HORIZONTAL_NEIGHBORS = {
            {1, 0, 0},
            {-1, 0, 0},
            {0, 0, 1},
            {0, 0, -1}
    };

    public CometLootChestNeighborUseSystem() {
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

        Vector3i target = event.getTargetBlock();
        CometLootChestService chestService = CometLootChestService.getInstance();
        if (target == null || chestService.isManagedChest(target)) {
            return;
        }

        Object external = store.getExternalData();
        if (!(external instanceof EntityStore)) {
            return;
        }
        World world = ((EntityStore) external).getWorld();
        if (world == null) {
            return;
        }

        for (int[] neighbor : HORIZONTAL_NEIGHBORS) {
            Vector3i candidate = new Vector3i(target.x + neighbor[0], target.y + neighbor[1], target.z + neighbor[2]);
            if (!chestService.isManagedChest(candidate)) {
                continue;
            }
            if (chestService.openManagedChest(world, commandBuffer, event.getContext(), candidate)) {
                event.setCancelled(true);
                return;
            }
        }
    }
}
