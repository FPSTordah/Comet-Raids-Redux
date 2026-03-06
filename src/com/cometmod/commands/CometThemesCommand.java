package com.cometmod.commands;

import com.cometmod.loot.RewardContainerTheme;
import com.cometmod.wave.WaveThemeProvider;

import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractWorldCommand;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import com.hypixel.hytale.component.Store;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;

/**
 * Lists available wave themes so users can choose one when spawning a comet.
 * Usage: /comet themes
 */
public class CometThemesCommand extends AbstractWorldCommand {

    public CometThemesCommand() {
        super("themes", "Lists available wave themes. Use --theme <name> with /comet spawn to choose a theme.");
    }

    @Override
    protected void execute(@Nonnull CommandContext context,
            @Nonnull World world,
            @Nonnull Store<EntityStore> store) {

        String[] themeIds = WaveThemeProvider.getAllThemeIds();
        if (themeIds == null || themeIds.length == 0) {
            context.sendMessage(Message.raw("No themes configured. Add themes in themes.json."));
            return;
        }

        List<String> lines = new ArrayList<>();
        lines.add("Available themes (use with /comet spawn --theme <name>). Theme affects which mobs spawn:");
        for (String themeId : themeIds) {
            String displayName = WaveThemeProvider.getThemeName(themeId);
            String cometReplacement = cometReplacementLabel(themeId);
            lines.add("  " + displayName + " (" + themeId + ") — " + cometReplacement);
        }
        context.sendMessage(Message.raw(String.join("\n", lines)));
    }

    /** Label for what replaces the comet (chest always spawns as usual). */
    private static String cometReplacementLabel(String themeId) {
        if (RewardContainerTheme.isCoffinTheme(themeId)) return "Comet → Coffin";
        if (RewardContainerTheme.isVoidTheme(themeId)) return "Comet → Portal";
        if (RewardContainerTheme.isFireTheme(themeId)) return "Comet → Volcano";
        return "Default (chest)";
    }
}
