package com.cometmod.services;

import com.cometmod.*;
import com.cometmod.commands.*;
import com.cometmod.config.model.ThemeConfig;
import com.cometmod.services.*;
import com.cometmod.spawn.*;
import com.cometmod.systems.*;
import com.cometmod.wave.*;


import com.hypixel.hytale.math.vector.Vector3i;

import java.util.List;
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
            // Forced themes are one-time overrides for a specific comet spawn.
            themeId = forcedThemes.remove(blockPos);
            if (themeId != null && !themeId.isBlank()) {
                logger.info("Using forced theme for comet at " + blockPos + ": " + WaveThemeProvider.getThemeName(themeId));
                return themeId;
            }
            logger.warning("Found blank forced theme at " + blockPos + "; falling back to random selection.");
        }

        themeId = WaveThemeProvider.selectTheme(tier);
        if (themeId != null) {
            return themeId;
        }

        // If selection fails, fall back deterministically to first tier-compatible natural theme.
        CometConfig config = CometConfig.getInstance();
        if (config != null) {
            int tierNum = WaveThemeProvider.getTierNumber(tier);
            List<ThemeConfig> tierThemes = config.getThemesForTier(tierNum);
            if (!tierThemes.isEmpty()) {
                String fallbackTheme = tierThemes.get(0).getId();
                logger.warning("Theme selection failed for tier " + tier.getName()
                        + "; falling back to first available tier theme: " + fallbackTheme);
                return fallbackTheme;
            }
        }

        // Last-resort fallback if tier-filtered lookup failed.
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
