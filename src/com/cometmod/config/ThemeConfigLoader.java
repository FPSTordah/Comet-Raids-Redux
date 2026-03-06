package com.cometmod.config;

import com.cometmod.CometConfig;
import com.cometmod.config.defaults.DefaultThemes;
import com.cometmod.config.model.ThemeConfig;
import com.cometmod.config.model.TierStatScalingConfig;
import com.cometmod.config.parser.ThemeConfigParser;
import com.cometmod.config.parser.ThemeConfigWriter;

import java.io.File;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.Map;
import java.util.logging.Logger;

import static com.cometmod.config.parser.ConfigJson.extractJsonObject;

/**
 * Loads and saves themes.json. Split from CometConfig so theme config evolution is in one place.
 */
public final class ThemeConfigLoader {

    private static final Logger LOGGER = Logger.getLogger(ThemeConfigLoader.class.getName());
    private static final String THEMES_FILE_NAME = "themes.json";
    private static final String LEGACY_THEMES_FILE_NAME = "comet_themes_and_monster_groups.json";

    private ThemeConfigLoader() {}

    public static File getThemesFile(File baseConfigFile) {
        File parent = baseConfigFile != null ? baseConfigFile.getParentFile() : null;
        if (parent == null) return new File(THEMES_FILE_NAME);
        return new File(parent, THEMES_FILE_NAME);
    }

    /**
     * Load themes from themes.json (and legacy file if present).
     */
    public static void loadThemes(CometConfig config, File baseConfigFile) {
        if (config == null) return;
        File themesFile = getThemesFile(baseConfigFile);
        File parent = baseConfigFile != null ? baseConfigFile.getParentFile() : null;
        File legacyThemesFile = parent != null ? new File(parent, LEGACY_THEMES_FILE_NAME) : new File(LEGACY_THEMES_FILE_NAME);
        if (!themesFile.exists() || !themesFile.isFile()) {
            if (legacyThemesFile.exists() && legacyThemesFile.isFile()) {
                try {
                    String themesContent = new String(java.nio.file.Files.readAllBytes(legacyThemesFile.toPath()));
                    Map<String, ThemeConfig> fromLegacy = ThemeConfigParser.parseThemes(themesContent);
                    if (!fromLegacy.isEmpty()) {
                        config.setThemes(fromLegacy);
                        config.setThemeList(new ArrayList<>(config.getThemes().values()));
                        config.setTierStatScaling(ThemeConfigParser.parseTierStatScaling(themesContent));
                        config.setThemesLoaded(true);
                    }
                    saveThemes(config, themesFile);
                    renameToMigrated(legacyThemesFile);
                    LOGGER.info("Migrated themes from " + LEGACY_THEMES_FILE_NAME + " to " + THEMES_FILE_NAME);
                } catch (Exception e) {
                    LOGGER.warning("Failed to migrate legacy themes file: " + e.getMessage());
                    saveThemes(config, themesFile);
                }
            } else {
                saveThemes(config, themesFile);
            }
            return;
        }
        try {
            String themesJson = new String(java.nio.file.Files.readAllBytes(themesFile.toPath()));
            String themesBlock = extractJsonObject(themesJson, "themes");
            if (themesBlock == null || themesBlock.isBlank()) {
                LOGGER.warning("Themes file is missing top-level 'themes' object: " + themesFile.getAbsolutePath() + " — loading default themes and repairing file.");
                Map<String, ThemeConfig> defaultThemes = DefaultThemes.generateDefaults();
                config.setThemes(defaultThemes);
                config.setThemeList(new ArrayList<>(defaultThemes.values()));
                config.setThemesLoaded(true);
                if (config.getTierStatScaling() == null) config.setTierStatScaling(new TierStatScalingConfig());
                saveThemes(config, themesFile);
                LOGGER.info("Repaired themes.json with " + defaultThemes.size() + " default themes.");
                return;
            }
            Map<String, ThemeConfig> externalThemes = ThemeConfigParser.parseThemes(themesJson);
            if (externalThemes.isEmpty()) {
                LOGGER.warning("Themes file has no parsed themes — loading default themes and repairing file.");
                Map<String, ThemeConfig> defaultThemes = DefaultThemes.generateDefaults();
                config.setThemes(defaultThemes);
                config.setThemeList(new ArrayList<>(defaultThemes.values()));
                config.setThemesLoaded(true);
                if (config.getTierStatScaling() == null) config.setTierStatScaling(new TierStatScalingConfig());
                saveThemes(config, themesFile);
                LOGGER.info("Repaired themes.json with " + defaultThemes.size() + " default themes.");
                return;
            }
            config.setThemes(externalThemes);
            config.setThemeList(new ArrayList<>(externalThemes.values()));
            config.setThemesLoaded(true);
            TierStatScalingConfig fromFile = ThemeConfigParser.parseTierStatScaling(themesJson);
            if (fromFile != null) config.setTierStatScaling(fromFile);
            LOGGER.info("Loaded themes from: " + themesFile.getAbsolutePath() + " (" + externalThemes.size() + " themes)");
            for (Map.Entry<String, ThemeConfig> e : externalThemes.entrySet()) {
                String sb = e.getValue().getSpawnBlock();
                LOGGER.info("  Theme '" + e.getKey() + "' spawnBlock=" + (sb != null ? "'" + sb + "'" : "null"));
            }
        } catch (Exception e) {
            LOGGER.warning("Failed to load themes file '" + themesFile.getAbsolutePath() + "': " + e.getMessage());
        }
        if (config.getThemes() == null || config.getThemes().isEmpty()) {
            LOGGER.warning("No themes loaded; waves will not spawn mobs. Check themes.json.");
        }
    }

    public static void saveThemes(CometConfig config, File themesFile) {
        if (config == null || themesFile == null) return;
        try {
            File parentDir = themesFile.getParentFile();
            if (parentDir != null && !parentDir.exists()) parentDir.mkdirs();
            String json = ThemeConfigWriter.generateThemesOnlyConfig(config.getThemes(), config.getTierStatScaling());
            try (FileWriter writer = new FileWriter(themesFile)) {
                writer.write(json);
                writer.flush();
            }
        } catch (Exception e) {
            LOGGER.warning("Failed to save themes file '" + themesFile.getAbsolutePath() + "': " + e.getMessage());
        }
    }

    private static void renameToMigrated(File file) {
        try {
            if (file == null || !file.exists()) return;
            File parent = file.getParentFile();
            File target = parent != null ? new File(parent, file.getName() + ".migrated") : new File(file.getName() + ".migrated");
            if (file.renameTo(target)) {
                LOGGER.info("Renamed legacy file to " + target.getName());
            }
        } catch (Exception e) {
            LOGGER.warning("Could not rename legacy file: " + e.getMessage());
        }
    }
}
