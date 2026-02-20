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
            String legacyFallbackThemeId,
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

        logger.warning("Config-based theme selection failed, using legacy fallback theme id: " + legacyFallbackThemeId);
        return legacyFallbackThemeId;
    }
}
