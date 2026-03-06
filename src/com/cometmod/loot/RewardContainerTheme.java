package com.cometmod.loot;

import com.cometmod.CometConfig;
import com.cometmod.config.model.ThemeConfig;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockType;

import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;

/**
 * Maps wave theme IDs to reward container behavior: coffin, void portal,
 * volcano (prefab + chest on wall), or default chest.
 * If a theme defines "cometReplacement" in themes.json,
 * that value is used; otherwise legacy theme-id sets apply.
 */
public final class RewardContainerTheme {

    private static final Set<String> COFFIN_THEMES = Collections.unmodifiableSet(new HashSet<>(Arrays.asList(
            "skeleton", "skeleton_sand", "skeleton_burnt", "undead_rare", "undead_legendary"
    )));
    private static final Set<String> VOID_THEMES = Collections.unmodifiableSet(new HashSet<>(Arrays.asList(
            "void", "void_reavers"
    )));
    private static final Set<String> FIRE_THEMES = Collections.unmodifiableSet(new HashSet<>(Arrays.asList(
            "lava", "toad"
    )));

    /** Our block IDs (use these so USE runs our Comet_Themed_Activate interaction). */
    public static final String COMET_COFFIN_BLOCK_ID = "Comet_Coffin";
    public static final String COMET_PORTAL_BLOCK_ID = "Comet_Portal";
    public static final String COMET_VOLCANO_ANCHOR_BLOCK_ID = "Comet_Volcano_Anchor";

    /** Vanilla block IDs kept for isThemedActivationBlock (legacy / fallback). */
    private static final String[] COFFIN_BLOCK_IDS = {
            "Furniture_Human_Ruins_Coffin",
            "Furniture_Temple_Dark_Coffin",
            "Furniture_Village_Coffin",
            "Furniture_Ancient_Coffin"
    };
    private static final String VOID_PORTAL_BLOCK_ID = "VoidInvasion_Portal";

    private static final Random RANDOM = new Random();

    private RewardContainerTheme() {
    }

    /** Returns the theme's cometReplacement from config (lowercase), or null if not set. */
    @Nullable
    private static String getCometReplacementFromConfig(String themeId) {
        if (themeId == null || themeId.isBlank()) return null;
        CometConfig config = CometConfig.getInstance();
        if (config == null) return null;
        ThemeConfig theme = config.getTheme(themeId);
        if (theme == null) return null;
        String cr = theme.getCometReplacement();
        return (cr != null && !cr.isBlank()) ? cr.trim().toLowerCase() : null;
    }

    /** When cometReplacement is set in config, only that value counts; otherwise use legacy theme-id sets. */
    public static boolean isCoffinTheme(String themeId) {
        String fromConfig = getCometReplacementFromConfig(themeId);
        if (fromConfig != null) return "coffin".equals(fromConfig);
        return themeId != null && COFFIN_THEMES.contains(themeId);
    }

    public static boolean isVoidTheme(String themeId) {
        String fromConfig = getCometReplacementFromConfig(themeId);
        if (fromConfig != null) return "portal".equals(fromConfig);
        return themeId != null && VOID_THEMES.contains(themeId);
    }

    public static boolean isFireTheme(String themeId) {
        String fromConfig = getCometReplacementFromConfig(themeId);
        if (fromConfig != null) return "volcano".equals(fromConfig);
        return themeId != null && FIRE_THEMES.contains(themeId);
    }

    /** True if this theme uses a visual replacement at spawn (coffin, portal, volcano) instead of the comet block. Disabled for now. */
    public static boolean themeUsesReplacement(String themeId) {
        return false;
    }

    /**
     * Resolve the block ID to use for a single-block reward container for this theme,
     * or null if this theme uses a special path (e.g. volcano prefab).
     */
    @Nullable
    public static String getBlockIdForTheme(String themeId) {
        if (themeId == null || themeId.isBlank()) {
            return null;
        }
        if (isCoffinTheme(themeId)) {
            return COMET_COFFIN_BLOCK_ID;
        }
        if (isVoidTheme(themeId)) {
            return COMET_PORTAL_BLOCK_ID;
        }
        if (isFireTheme(themeId)) {
            return null; // volcano uses prefab (includes Comet_Volcano_Anchor)
        }
        return null; // default: caller uses Comet_Reward_Chest / fallback
    }

    /** True if this block ID is a themed comet replacement (coffin, portal, volcano) that the player can USE to start the wave. */
    public static boolean isThemedActivationBlock(String blockId) {
        if (blockId == null || blockId.isBlank()) return false;
        String id = blockId.trim();
        String idNoJson = id.endsWith(".json") ? id.substring(0, id.length() - 5) : id;
        if (COMET_COFFIN_BLOCK_ID.equalsIgnoreCase(id) || COMET_COFFIN_BLOCK_ID.equalsIgnoreCase(idNoJson)) return true;
        if (COMET_PORTAL_BLOCK_ID.equalsIgnoreCase(id) || COMET_PORTAL_BLOCK_ID.equalsIgnoreCase(idNoJson)) return true;
        if (COMET_VOLCANO_ANCHOR_BLOCK_ID.equalsIgnoreCase(id) || COMET_VOLCANO_ANCHOR_BLOCK_ID.equalsIgnoreCase(idNoJson)) return true;
        for (String coffinId : COFFIN_BLOCK_IDS) {
            if (coffinId.equalsIgnoreCase(id) || coffinId.equalsIgnoreCase(idNoJson)) return true;
            if (id.endsWith("/" + coffinId) || idNoJson.endsWith("/" + coffinId)) return true;
        }
        if (VOID_PORTAL_BLOCK_ID.equalsIgnoreCase(id) || VOID_PORTAL_BLOCK_ID.equalsIgnoreCase(idNoJson)) return true;
        return id.contains(VOID_PORTAL_BLOCK_ID) || idNoJson.contains(VOID_PORTAL_BLOCK_ID);
    }
}
