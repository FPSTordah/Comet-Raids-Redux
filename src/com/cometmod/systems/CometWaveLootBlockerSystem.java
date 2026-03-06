package com.cometmod.systems;

import com.cometmod.CometConfig;
import com.cometmod.wave.CometWaveManager;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.dependency.Dependency;
import com.hypixel.hytale.component.dependency.Order;
import com.hypixel.hytale.component.dependency.SystemDependency;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.server.core.asset.type.gameplay.DeathConfig;
import com.hypixel.hytale.server.core.modules.entity.damage.DeathComponent;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.npc.systems.NPCDamageSystems;
import java.util.Set;
import javax.annotation.Nonnull;

/**
 * Runs before NPC DropDeathItems (same approach as BossArena). When a comet wave mob dies and
 * config says disable wave mob loot, sets the death component's items loss mode to NONE so
 * DropDeathItems skips dropping loot from the mob's droplist.
 */
public class CometWaveLootBlockerSystem extends com.hypixel.hytale.server.core.modules.entity.damage.DeathSystems.OnDeathSystem {

    private static final Query<EntityStore> QUERY = Query.and(
            com.hypixel.hytale.server.npc.entities.NPCEntity.getComponentType(),
            DeathComponent.getComponentType());

    private static final Set<Dependency<EntityStore>> DEPENDENCIES = Set.of(
            new SystemDependency(Order.BEFORE, NPCDamageSystems.DropDeathItems.class));

    private final CometWaveManager waveManager;

    public CometWaveLootBlockerSystem(CometWaveManager waveManager) {
        this.waveManager = waveManager;
    }

    @Override
    @Nonnull
    public Query<EntityStore> getQuery() {
        return QUERY;
    }

    @Override
    @Nonnull
    public Set<Dependency<EntityStore>> getDependencies() {
        return DEPENDENCIES;
    }

    @Override
    public void onComponentAdded(@Nonnull Ref<EntityStore> ref, @Nonnull DeathComponent component,
            @Nonnull Store<EntityStore> store, @Nonnull CommandBuffer<EntityStore> commandBuffer) {
        CometConfig config = CometConfig.getInstance();
        if (config == null || !config.disableWaveMobLoot) return;
        if (!waveManager.isCometWaveMob(store, ref)) return;

        component.setItemsLossMode(DeathConfig.ItemsLossMode.NONE);
    }
}
