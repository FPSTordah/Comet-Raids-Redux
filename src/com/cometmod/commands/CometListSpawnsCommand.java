package com.cometmod.commands;

import com.cometmod.*;
import com.cometmod.commands.*;
import com.cometmod.services.*;
import com.cometmod.spawn.*;
import com.cometmod.systems.*;
import com.cometmod.wave.*;


import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractWorldCommand;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import javax.annotation.Nonnull;
import java.util.List;

/**
 * Read-only listing command backed by FixedSpawnManager.
 */
public class CometListSpawnsCommand extends AbstractWorldCommand {

    public CometListSpawnsCommand() {
        super("listspawns", "Lists configured fixed spawn points");
    }

    @Override
    protected void execute(@Nonnull CommandContext context, @Nonnull World world, @Nonnull Store<EntityStore> store) {
        FixedSpawnManager manager = CometModPlugin.getFixedSpawnManager();
        if (manager == null) {
            context.sendMessage(Message.raw("Fixed spawn manager is not initialized."));
            return;
        }

        List<FixedSpawnManager.SpawnPoint> points = manager.getSpawnPoints();
        if (points.isEmpty()) {
            context.sendMessage(Message.raw("No fixed spawn points configured."));
            return;
        }

        context.sendMessage(Message.raw("Fixed spawn points: " + points.size()));
        for (int i = 0; i < points.size(); i++) {
            FixedSpawnManager.SpawnPoint p = points.get(i);
            context.sendMessage(Message.raw((i + 1) + ". " + p.getName() + " @ " + p.getX() + ", " + p.getY() + ", " + p.getZ()));
        }
    }
}
