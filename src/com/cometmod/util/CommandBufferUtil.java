package com.cometmod.util;

import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import javax.annotation.Nullable;
import java.lang.reflect.Method;
import java.util.logging.Logger;

/**
 * Centralized reflection access for Store.takeCommandBuffer and CommandBuffer.consume.
 * Use this instead of duplicating reflection in CometFallingSystem, CometSpawnCommand, CometWaveManager, CometSpawnTask.
 */
public final class CommandBufferUtil {

    private static final Logger LOGGER = Logger.getLogger(CommandBufferUtil.class.getName());

    private CommandBufferUtil() {}

    /**
     * Take the command buffer from the store (reflection). Returns null on failure.
     */
    @Nullable
    @SuppressWarnings("unchecked")
    public static CommandBuffer<EntityStore> take(Store<EntityStore> store) {
        if (store == null) return null;
        try {
            Method takeMethod = store.getClass().getDeclaredMethod("takeCommandBuffer");
            takeMethod.setAccessible(true);
            return (CommandBuffer<EntityStore>) takeMethod.invoke(store);
        } catch (Exception e) {
            LOGGER.warning("Could not get command buffer: " + e.getMessage());
            return null;
        }
    }

    /**
     * Consume the command buffer (reflection). No-op if buffer is null or consumption fails.
     */
    public static void consume(@Nullable CommandBuffer<EntityStore> commandBuffer) {
        if (commandBuffer == null) return;
        try {
            Method consumeMethod = commandBuffer.getClass().getDeclaredMethod("consume");
            consumeMethod.setAccessible(true);
            consumeMethod.invoke(commandBuffer);
        } catch (Exception e) {
            LOGGER.warning("Could not consume command buffer: " + e.getMessage());
        }
    }
}
