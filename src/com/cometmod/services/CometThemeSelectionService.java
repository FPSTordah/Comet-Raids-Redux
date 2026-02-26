package com.cometmod.services;

import com.cometmod.*;
import com.cometmod.commands.*;
import com.cometmod.services.*;
import com.cometmod.spawn.*;
import com.cometmod.systems.*;
import com.cometmod.wave.*;


import com.hypixel.hytale.math.vector.Vector3i;

import java.util.Map;
import java.util.logging.Logger;

/**
 * Encapsulates theme selection rules for comet waves.
 */
public class CometThemeSelectionService {

    public String selectThemeId(
            Vector3i blockPos,
            CometTier tier,
            Map<Vector3i, String> forcedThemes,
            Logger logger) {

        String themeId;
        if (forcedThemes.containsKey(blockPos)) {
            themeId = forcedThemes.get(blockPos);
            logger.info("Using forced theme for comet at " + blockPos + ": " + WaveThemeProvider.getThemeName(themeId));
            return themeId;
        }

        themeId = WaveThemeProvider.selectTheme(tier);
        if (themeId != null) {
            return themeId;
        }

        // If random selection fails, fall back to the first configured theme deterministically.
        String[] allThemeIds = WaveThemeProvider.getAllThemeIds();
        if (allThemeIds.length > 0) {
            logger.warning("Theme selection failed for tier " + tier.getName()
                    + "; falling back to first configured theme: " + allThemeIds[0]);
            return allThemeIds[0];
        }

        logger.severe("Theme selection failed and no configured themes are available.");
        return null;
    }
}
