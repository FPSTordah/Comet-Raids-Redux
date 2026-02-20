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

/**
 * Placeholder command kept for source compatibility.
 */
public class CometSetSpawnCommand extends AbstractWorldCommand {

    public CometSetSpawnCommand() {
        super("setspawn", "Adds a fixed spawn point (placeholder in this source snapshot)");
    }

    @Override
    protected void execute(@Nonnull CommandContext context, @Nonnull World world, @Nonnull Store<EntityStore> store) {
        context.sendMessage(Message.raw(
                "Fixed spawn editing is unavailable in this source snapshot. Edit fixed_spawns.json manually."));
    }
}
