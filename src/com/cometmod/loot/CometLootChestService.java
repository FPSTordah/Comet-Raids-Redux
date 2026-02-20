package com.cometmod.loot;

import com.hypixel.hytale.assetstore.map.BlockTypeAssetMap;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.math.util.ChunkUtil;
import com.hypixel.hytale.math.vector.Vector3i;
import com.hypixel.hytale.protocol.packets.interface_.Page;
import com.hypixel.hytale.server.core.HytaleServer;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockType;
import com.hypixel.hytale.server.core.entity.InteractionContext;
import com.hypixel.hytale.server.core.entity.UUIDComponent;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.entity.entities.player.windows.ContainerBlockWindow;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.inventory.container.SimpleItemContainer;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.chunk.WorldChunk;
import com.hypixel.hytale.server.core.universe.world.meta.BlockState;
import com.hypixel.hytale.server.core.universe.world.meta.state.ItemContainerState;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

/**
 * Manages shared comet reward chests.
 *
 * Behavior:
 * - Chest is spawned with generated rewards.
 * - Timer starts when the chest is touched/opened.
 * - If untouched for 20 seconds, chest is removed.
 * - Any touch before expiry resets the timer.
 */
public final class CometLootChestService {

    private static final Logger LOGGER = Logger.getLogger("CometLootChestService");
    private static final String CUSTOM_CHEST_ID = "Comet_Reward_Chest";
    private static final String FALLBACK_CHEST_ID = "Furniture_Dungeon_Chest_Legendary_Large";
    private static final int CONTAINER_SIZE = 27;
    private static final long CHEST_EXPIRY_SECONDS = 20L;
    private static final CometLootChestService INSTANCE = new CometLootChestService();

    private final Set<String> managedChests = ConcurrentHashMap.newKeySet();
    private final Map<String, ScheduledFuture<?>> expiryTasks = new ConcurrentHashMap<>();

    private CometLootChestService() {
    }

    public static CometLootChestService getInstance() {
        return INSTANCE;
    }

    public boolean spawnRewardChest(World world, Vector3i blockPos, List<ItemStack> rewards) {
        if (world == null || blockPos == null || rewards == null) {
            return false;
        }

        Vector3i pos = copyKey(blockPos);
        String key = keyOf(pos);
        try {
            BlockType chestBlockType = findPreferredChestBlockType();
            if (chestBlockType == null) {
                LOGGER.warning("No valid chest block type found (custom or fallback).");
                return false;
            }

            WorldChunk chunk = world.getChunkIfInMemory(ChunkUtil.indexChunkFromBlock(pos.x, pos.z));
            if (chunk == null) {
                LOGGER.warning("Chunk not loaded for reward chest at " + pos + ".");
                return false;
            }

            BlockTypeAssetMap<String, BlockType> blockTypeMap = BlockType.getAssetMap();
            int blockTypeIndex = blockTypeMap.getIndex(chestBlockType.getId());
            if (blockTypeIndex < 0) {
                LOGGER.warning("Unable to resolve block type index for chest id: " + chestBlockType.getId());
                return false;
            }

            int localX = pos.x & 31;
            int localZ = pos.z & 31;
            boolean placed = chunk.setBlock(localX, pos.y, localZ, blockTypeIndex, chestBlockType, 0, 0, 157);
            if (!placed) {
                // Retry once after clearing the current block.
                world.breakBlock(pos.x, pos.y, pos.z, 0);
                chunk = world.getChunkIfInMemory(ChunkUtil.indexChunkFromBlock(pos.x, pos.z));
                if (chunk == null) {
                    LOGGER.warning("Chunk unavailable after retry for reward chest at " + pos + ".");
                    return false;
                }
                placed = chunk.setBlock(localX, pos.y, localZ, blockTypeIndex, chestBlockType, 0, 0, 157);
            }
            if (!placed) {
                LOGGER.warning("Failed to place reward chest block at " + pos + ".");
                return false;
            }

            ContainerBuildResult buildResult = buildContainer(rewards);
            if (buildResult.addedCount <= 0) {
                LOGGER.warning("No valid reward items could be inserted into chest at " + pos + ".");
                return false;
            }

            ItemContainerState containerState = new ItemContainerState();
            containerState.initialize(chestBlockType);
            // Important: attach state to chunk before setters that call markNeedsSave().
            containerState.setPosition(chunk, pos);
            chunk.setState(localX, pos.y, localZ, containerState, true);
            containerState.setCustom(true);
            containerState.setAllowViewing(true);
            containerState.setDroplist(null);
            containerState.setItemContainer(buildResult.container);

            managedChests.add(key);
            cancelExpiryTask(key);
            LOGGER.info("Spawned reward chest at " + pos + " with " + buildResult.addedCount + " item stacks.");
            return true;
        } catch (Exception e) {
            LOGGER.warning("Failed to spawn reward chest at " + pos + ": " + e.getMessage());
            return false;
        }
    }

    public void touchChest(World world, Vector3i blockPos) {
        if (world == null || blockPos == null) {
            return;
        }
        Vector3i pos = copyKey(blockPos);
        String key = keyOf(pos);
        if (!managedChests.contains(key)) {
            return;
        }
        scheduleExpiry(world, key, pos);
    }

    public boolean openManagedChest(World world,
            CommandBuffer<EntityStore> commandBuffer,
            InteractionContext context,
            Vector3i blockPos) {
        if (world == null || commandBuffer == null || context == null || blockPos == null) {
            return false;
        }

        Vector3i pos = copyKey(blockPos);
        String key = keyOf(pos);
        if (!managedChests.contains(key)) {
            return false;
        }

        Ref<EntityStore> playerRef = context.getEntity();
        if (playerRef == null || !playerRef.isValid()) {
            return false;
        }

        Store<EntityStore> store = playerRef.getStore();
        if (store == null) {
            return false;
        }

        Player player = commandBuffer.getComponent(playerRef, Player.getComponentType());
        if (player == null) {
            return false;
        }

        BlockState state = world.getState(pos.x, pos.y, pos.z, true);
        if (!(state instanceof ItemContainerState)) {
            return false;
        }
        ItemContainerState containerState = (ItemContainerState) state;
        if (!containerState.isAllowViewing() || !containerState.canOpen(playerRef, commandBuffer)) {
            return false;
        }

        UUIDComponent uuidComponent = commandBuffer.getComponent(playerRef, UUIDComponent.getComponentType());
        if (uuidComponent == null) {
            return false;
        }
        UUID playerUuid = uuidComponent.getUuid();

        Map<UUID, ContainerBlockWindow> windows = containerState.getWindows();
        if (windows == null || containerState.getItemContainer() == null) {
            return false;
        }

        if (windows.containsKey(playerUuid)) {
            touchChest(world, pos);
            return true;
        }

        WorldChunk chunk = world.getChunkIfInMemory(ChunkUtil.indexChunkFromBlock(pos.x, pos.z));
        if (chunk == null) {
            return false;
        }

        BlockType blockType = world.getBlockType(pos.x, pos.y, pos.z);
        if (blockType == null) {
            return false;
        }

        ContainerBlockWindow window = new ContainerBlockWindow(
                pos.x,
                pos.y,
                pos.z,
                chunk.getRotationIndex(pos.x, pos.y, pos.z),
                blockType,
                containerState.getItemContainer());

        if (windows.putIfAbsent(playerUuid, window) != null) {
            touchChest(world, pos);
            return true;
        }

        boolean opened = player.getPageManager().setPageWithWindows(playerRef, store, Page.Bench, true, window);
        if (!opened) {
            windows.remove(playerUuid, window);
            return false;
        }

        if (windows.size() == 1) {
            world.setBlockInteractionState(pos, blockType, "OpenWindow");
        }

        window.registerCloseEvent(event -> {
            windows.remove(playerUuid, window);
            BlockType currentType = world.getBlockType(pos);
            if (currentType != null && windows.isEmpty()) {
                world.setBlockInteractionState(pos, currentType, "CloseWindow");
            }
        });

        containerState.onOpen(playerRef, world, store);
        touchChest(world, pos);
        return true;
    }

    public boolean isManagedChest(Vector3i blockPos) {
        if (blockPos == null) {
            return false;
        }
        return managedChests.contains(keyOf(blockPos));
    }

    public void handleChestBroken(Vector3i blockPos) {
        if (blockPos == null) {
            return;
        }
        String key = keyOf(blockPos);
        managedChests.remove(key);
        cancelExpiryTask(key);
    }

    public void clear() {
        for (ScheduledFuture<?> task : expiryTasks.values()) {
            if (task != null) {
                task.cancel(false);
            }
        }
        expiryTasks.clear();
        managedChests.clear();
    }

    private void scheduleExpiry(World world, String key, Vector3i pos) {
        cancelExpiryTask(key);
        ScheduledFuture<?> expiryTask = HytaleServer.SCHEDULED_EXECUTOR.schedule(() -> {
            try {
                world.execute(() -> expireChest(world, key, pos));
            } catch (Exception e) {
                LOGGER.warning("Failed to execute chest expiry for " + pos + ": " + e.getMessage());
            }
        }, CHEST_EXPIRY_SECONDS, TimeUnit.SECONDS);
        expiryTasks.put(key, expiryTask);
    }

    private void expireChest(World world, String key, Vector3i pos) {
        if (!managedChests.contains(key)) {
            expiryTasks.remove(key);
            return;
        }

        try {
            BlockState state = world.getState(pos.x, pos.y, pos.z, true);
            if (state instanceof ItemContainerState) {
                ItemContainerState containerState = (ItemContainerState) state;
                if (containerState.getWindows() != null && !containerState.getWindows().isEmpty()) {
                    containerState.getWindows().clear();
                }
                if (containerState.getItemContainer() != null) {
                    containerState.getItemContainer().clear();
                }
            }
        } catch (Exception e) {
            LOGGER.warning("Failed to clear chest state before expiry at " + pos + ": " + e.getMessage());
        }

        boolean removed = false;
        try {
            removed = world.breakBlock(pos.x, pos.y, pos.z, 0);
            if (!removed) {
                removed = forceRemoveBlock(world, pos);
            }
        } catch (Exception e) {
            LOGGER.warning("Failed to remove chest block at " + pos + ": " + e.getMessage());
            removed = forceRemoveBlock(world, pos);
        }

        if (!removed) {
            LOGGER.warning("Could not remove reward chest at " + pos + ", retrying expiry.");
            scheduleExpiry(world, key, pos);
            return;
        }

        managedChests.remove(key);
        expiryTasks.remove(key);
        LOGGER.info("Expired untouched reward chest at " + pos + ".");
    }

    private boolean forceRemoveBlock(World world, Vector3i pos) {
        try {
            WorldChunk chunk = world.getChunkIfInMemory(ChunkUtil.indexChunkFromBlock(pos.x, pos.z));
            if (chunk == null) {
                return false;
            }
            int localX = pos.x & 31;
            int localZ = pos.z & 31;
            return chunk.setBlock(localX, pos.y, localZ, BlockType.EMPTY_ID, BlockType.EMPTY, 0, 0, 157);
        } catch (Exception e) {
            LOGGER.warning("Force remove failed for chest at " + pos + ": " + e.getMessage());
            return false;
        }
    }

    private void cancelExpiryTask(String key) {
        ScheduledFuture<?> existingTask = expiryTasks.remove(key);
        if (existingTask != null) {
            existingTask.cancel(false);
        }
    }

    private ContainerBuildResult buildContainer(List<ItemStack> rewards) {
        SimpleItemContainer container = new SimpleItemContainer((short) CONTAINER_SIZE);
        int limit = Math.min(CONTAINER_SIZE, rewards.size());
        int added = 0;
        for (int i = 0; i < limit; i++) {
            ItemStack stack = rewards.get(i);
            if (stack != null) {
                try {
                    container.setItemStackForSlot((short) i, stack);
                    added++;
                } catch (Exception e) {
                    LOGGER.warning("Skipping invalid reward stack in chest slot " + i + ": " + e.getMessage());
                }
            }
        }
        return new ContainerBuildResult(container, added);
    }

    private BlockType findPreferredChestBlockType() {
        BlockTypeAssetMap<String, BlockType> blockTypeAssetMap = BlockType.getAssetMap();
        BlockType customChest = findBlockType(blockTypeAssetMap, CUSTOM_CHEST_ID);
        if (customChest != null) {
            return customChest;
        }
        return findBlockType(blockTypeAssetMap, FALLBACK_CHEST_ID);
    }

    private BlockType findBlockType(BlockTypeAssetMap<String, BlockType> blockTypeAssetMap, String blockTypeId) {
        if (blockTypeAssetMap == null || blockTypeId == null) {
            return null;
        }
        BlockType blockType = blockTypeAssetMap.getAsset(blockTypeId);
        if (blockType != null) {
            return blockType;
        }
        return blockTypeAssetMap.getAsset(blockTypeId + ".json");
    }

    private Vector3i copyKey(Vector3i blockPos) {
        return new Vector3i(blockPos.x, blockPos.y, blockPos.z);
    }

    private String keyOf(Vector3i blockPos) {
        return blockPos.x + ":" + blockPos.y + ":" + blockPos.z;
    }

    private static final class ContainerBuildResult {
        private final SimpleItemContainer container;
        private final int addedCount;

        private ContainerBuildResult(SimpleItemContainer container, int addedCount) {
            this.container = container;
            this.addedCount = addedCount;
        }
    }
}
