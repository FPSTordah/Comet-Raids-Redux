package com.cometmod.loot;

import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.math.vector.Vector3i;
import com.hypixel.hytale.protocol.InteractionType;
import com.hypixel.hytale.server.core.entity.InteractionContext;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.modules.interaction.interaction.CooldownHandler;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.client.SimpleBlockInteraction;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import javax.annotation.Nonnull;

/**
 * Opens comet reward chests that use ItemContainerState.
 */
public class CometOpenRewardChestInteraction extends SimpleBlockInteraction {

    @Nonnull
    public static final BuilderCodec<CometOpenRewardChestInteraction> CODEC;

    static {
        CODEC = BuilderCodec
                .builder(CometOpenRewardChestInteraction.class, CometOpenRewardChestInteraction::new,
                        SimpleBlockInteraction.CODEC)
                .build();
    }

    public CometOpenRewardChestInteraction() {
        super("Comet_OpenRewardChest");
    }

    @Override
    protected void interactWithBlock(@Nonnull World world,
            @Nonnull CommandBuffer<EntityStore> commandBuffer,
            @Nonnull InteractionType interactionType,
            @Nonnull InteractionContext context,
            @Nonnull ItemStack itemStack,
            @Nonnull Vector3i blockPos,
            @Nonnull CooldownHandler cooldownHandler) {
        CometLootChestService.getInstance().openManagedChest(world, commandBuffer, context, blockPos);
    }

    @Override
    protected void simulateInteractWithBlock(@Nonnull InteractionType interactionType,
            @Nonnull InteractionContext context,
            @Nonnull ItemStack itemStack,
            @Nonnull World world,
            @Nonnull Vector3i blockPos) {
        // Server-authoritative opening only.
    }
}
