package com.cometmod;

import com.cometmod.*;
import com.cometmod.commands.*;
import com.cometmod.services.*;
import com.cometmod.spawn.*;
import com.cometmod.systems.*;
import com.cometmod.wave.*;


import static com.cometmod.config.parser.ConfigJson.extractBooleanValue;
import static com.cometmod.config.parser.ConfigJson.extractDoubleValue;
import static com.cometmod.config.parser.ConfigJson.extractIntValue;
import static com.cometmod.config.parser.ConfigJson.extractJsonObject;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.cometmod.config.model.BossEntry;
import com.cometmod.config.defaults.DefaultThemes;
import com.cometmod.config.model.MobEntry;
import com.cometmod.config.validation.ConfigValidationReport;
import com.cometmod.config.validation.ConfigValidator;
import com.cometmod.config.model.ThemeConfig;
import com.cometmod.config.parser.ThemeConfigParser;
import com.cometmod.config.parser.ThemeConfigWriter;
import com.cometmod.config.model.TierSettings;
import com.cometmod.config.model.TierStatScalingConfig;
import com.cometmod.config.model.TierRewards;
import com.cometmod.config.model.RewardEntry;
import com.cometmod.config.model.ZoneSpawnChances;
import com.cometmod.config.model.TierInheritanceWeights;
import com.hypixel.hytale.common.plugin.PluginIdentifier;
import com.hypixel.hytale.common.semver.SemverRange;
import com.hypixel.hytale.server.core.plugin.PluginManager;

/**
 * Configuration manager for Comet Mod settings.
 * Handles spawn settings, themes, and tier configurations.
 * Config file is the single source of truth - no fallback to hardcoded after
 * first run.
 */
public class CometConfig {

    private static final Logger LOGGER = Logger.getLogger("CometConfig");
    private static final String CONFIG_FILE_NAME = "comet_config.json";
    private static final String THEMES_CONFIG_FILE_NAME = "comet_themes_and_monster_groups.json";
    private static final String ENDGAME_QOL_GROUP = "Config";
    private static final String ENDGAME_QOL_NAME = "Endgame&QoL";
    private static final String ENDGAME_QOL_PLUGIN_CLASS = "endgame.plugin.EndgameQoL";

    // Singleton instance for global access
    private static CometConfig instance;
    private static volatile boolean tier5Enabled = detectTier5Availability();

    // Spawn settings (existing)
    public int minDelaySeconds = 120;
    public int maxDelaySeconds = 300;
    public double spawnChance = 0.4;
    public double despawnTimeMinutes = 30.0;
    public int minSpawnDistance = 30;
    public int maxSpawnDistance = 50;

    // Natural spawns toggle - if false, comets only spawn from fixed spawn points
    public boolean naturalSpawnsEnabled = true;

    // Global comets setting - if true, any player can trigger any comet (not just the owner)
    public boolean globalComets = false;

    // Theme configurations (new)
    private Map<String, ThemeConfig> themes = new LinkedHashMap<>();
    private List<ThemeConfig> themeList = new ArrayList<>(); // Ordered list for random selection
    private TierStatScalingConfig tierStatScaling = new TierStatScalingConfig();

    // Tier settings (new)
    private Map<Integer, TierSettings> tierSettings = new LinkedHashMap<>();

    // Reward settings (new)
    private Map<Integer, TierRewards> rewardSettings = new LinkedHashMap<>();

    // Zone spawn chances (configurable tier probabilities per zone)
    private Map<String, ZoneSpawnChances> zoneSpawnChances = new LinkedHashMap<>();

    // Per-zone base loot pools (zone identity)
    private Map<String, TierRewards> zoneBaseLootPools = new LinkedHashMap<>();

    // Per-current-tier lower-tier inclusion chances
    private Map<Integer, TierInheritanceWeights> tierInheritanceWeights = new LinkedHashMap<>();

    // Optional WorldProtect integration: control comet spawning inside protected regions
    private boolean protectedZoneSpawnRulesEnabled = false;
    private boolean protectedZoneDefaultInProtectedRegion = true;
    private Map<String, Boolean> protectedZoneRegionOverrides = new LinkedHashMap<>();

    // Bench recipes (new)

    // Track if config was loaded successfully
    private boolean themesLoaded = false;

    /**
     * Get the singleton instance (loaded config)
     */
    public static CometConfig getInstance() {
        return instance;
    }

    /**
     * Get config file location
     */
    private static File getConfigFile() {
        CometModPlugin plugin = CometModPlugin.getInstance();
        if (plugin != null) {
            try {
                java.nio.file.Path pluginFile = plugin.getFile();
                if (pluginFile != null) {
                    java.nio.file.Path pluginDir = pluginFile.getParent();
                    if (pluginDir != null) {
                        String dirName = pluginDir.getFileName().toString();
                        java.nio.file.Path modFolder;

                        if ("Mods".equals(dirName) || "mods".equals(dirName)) {
                            modFolder = pluginDir.resolve("CometMod");
                        } else {
                            modFolder = pluginDir;
                        }

                        File modFolderFile = modFolder.toFile();
                        if (!modFolderFile.exists()) {
                            modFolderFile.mkdirs();
                        }

                        File configFile = modFolder.resolve(CONFIG_FILE_NAME).toFile();
                        LOGGER.info("Using plugin directory for config: " + configFile.getAbsolutePath());
                        return configFile;
                    }
                }
            } catch (Exception e) {
                LOGGER.warning("Could not get plugin directory, using fallback: " + e.getMessage());
            }
        }

        // Fallback paths
        String appData = System.getenv("APPDATA");
        if (appData != null) {
            File modFolder = new File(appData + File.separator + "Hytale" + File.separator +
                    "UserData" + File.separator + "Mods" + File.separator + "CometMod");
            if (!modFolder.exists()) {
                modFolder.mkdirs();
            }
            return new File(modFolder, CONFIG_FILE_NAME);
        }

        File currentDir = new File(System.getProperty("user.dir"));
        File modFolder1 = new File(currentDir, "Mods" + File.separator + "CometMod");
        if (modFolder1.exists() || modFolder1.getParentFile().exists()) {
            modFolder1.mkdirs();
            return new File(modFolder1, CONFIG_FILE_NAME);
        }

        File fallbackModFolder = new File(currentDir, "CometMod");
        fallbackModFolder.mkdirs();
        LOGGER.warning("Could not find mod directory, saving config to: " + fallbackModFolder.getAbsolutePath());
        return new File(fallbackModFolder, CONFIG_FILE_NAME);
    }

    private static File getThemesConfigFile(File configFile) {
        File parent = configFile != null ? configFile.getParentFile() : null;
        if (parent == null) {
            return new File(THEMES_CONFIG_FILE_NAME);
        }
        return new File(parent, THEMES_CONFIG_FILE_NAME);
    }

    /**
     * Load configuration from file, or create with defaults if file doesn't exist.
     * Config file is the single source of truth after creation.
     */
    public static CometConfig load() {
        refreshTier5Availability();
        File configFile = getConfigFile();
        CometConfig config = new CometConfig();

        if (configFile.exists() && configFile.isFile()) {
            try {
                String content = new String(java.nio.file.Files.readAllBytes(configFile.toPath()));
                logValidationReport("comet_config.json", ConfigValidator.validateCometConfig(content));
                config = parseJson(content);
                loadThemesFromSeparateFile(config, configFile);
                LOGGER.info("Loaded Comet Mod configuration from: " + configFile.getAbsolutePath());

                // Warn if no themes
                if (config.themes.isEmpty()) {
                    LOGGER.warning("  WARNING: No themes defined in config! Waves will not spawn mobs!");
                }

            } catch (Exception e) {
                LOGGER.warning("Failed to load config file, using defaults: " + e.getMessage());
                e.printStackTrace();
                config = createDefaultConfig();
                saveThemesConfig(config, getThemesConfigFile(configFile));
                config.save();
            }
        } else {
            LOGGER.info("Config file not found, creating default config at: " + configFile.getAbsolutePath());
            config = createDefaultConfig();
            saveThemesConfig(config, getThemesConfigFile(configFile));
            config.save();
        }

        instance = config;
        return config;
    }

    public static boolean isTier5Enabled() {
        return tier5Enabled;
    }

    public static synchronized void refreshTier5Availability() {
        boolean previous = tier5Enabled;
        tier5Enabled = detectTier5Availability();

        if (tier5Enabled && !previous) {
            LOGGER.info("Tier 5/Mythic enabled (Endgame&QoL detected).");
        } else if (!tier5Enabled && previous) {
            LOGGER.warning("Tier 5/Mythic disabled (Endgame&QoL not detected).");
        }
    }

    public static CometTier clampUnavailableTier(CometTier requestedTier) {
        if (requestedTier == null) {
            return CometTier.UNCOMMON;
        }
        if (requestedTier == CometTier.MYTHIC && !tier5Enabled) {
            return CometTier.LEGENDARY;
        }
        return requestedTier;
    }

    private static boolean detectTier5Availability() {
        try {
            PluginManager pluginManager = PluginManager.get();
            if (pluginManager != null) {
                PluginIdentifier id = new PluginIdentifier(ENDGAME_QOL_GROUP, ENDGAME_QOL_NAME);
                if (pluginManager.hasPlugin(id, SemverRange.WILDCARD)) {
                    return true;
                }
            }
        } catch (Throwable ignored) {
            // Fallback below.
        }

        try {
            Class.forName(ENDGAME_QOL_PLUGIN_CLASS, false, CometConfig.class.getClassLoader());
            return true;
        } catch (Throwable ignored) {
            return false;
        }
    }

    private static void logValidationReport(String fileName, ConfigValidationReport report) {
        if (report == null || report.isClean()) {
            return;
        }

        for (String warning : report.getWarnings()) {
            LOGGER.warning("[ConfigValidation][" + fileName + "] " + warning);
        }
        for (String error : report.getErrors()) {
            LOGGER.warning("[ConfigValidation][" + fileName + "] ERROR: " + error);
        }
    }

    /**
     * Create a config with default values
     */
    private static CometConfig createDefaultConfig() {
        CometConfig config = new CometConfig();
        config.themes = DefaultThemes.generateDefaults();
        config.themeList = new ArrayList<>(config.themes.values());
        config.tierSettings = DefaultThemes.getDefaultTierSettings();
        config.rewardSettings = getDefaultRewardSettings();
        config.zoneSpawnChances = ZoneSpawnChances.generateDefaults();
        config.zoneBaseLootPools = getDefaultZoneBaseLootPools();
        config.tierInheritanceWeights = getDefaultTierInheritanceWeights();
        config.tierStatScaling = new TierStatScalingConfig();
        config.themesLoaded = true;
        return config;
    }

    /**
     * Reload configuration from file
     */
    public static CometConfig reload() {
        LOGGER.info("Reloading configuration from file...");
        return load();
    }

    /**
     * Parse JSON configuration
     */
    private static CometConfig parseJson(String json) {
        CometConfig config = new CometConfig();

        try {
            // Parse spawn settings (check both old format and new nested format)
            String spawnBlock = extractJsonObject(json, "spawnSettings");
            String parseFrom = (spawnBlock != null) ? spawnBlock : json;

            // Parse spawn settings
            Integer minDelaySeconds = extractIntValue(parseFrom, "minDelaySeconds");
            if (minDelaySeconds != null) config.minDelaySeconds = minDelaySeconds;

            Integer maxDelaySeconds = extractIntValue(parseFrom, "maxDelaySeconds");
            if (maxDelaySeconds != null) config.maxDelaySeconds = maxDelaySeconds;

            Double spawnChance = extractDoubleValue(parseFrom, "spawnChance");
            if (spawnChance != null) config.spawnChance = spawnChance;

            Double despawnMinutes = extractDoubleValue(parseFrom, "despawnTimeMinutes");
            if (despawnMinutes != null) config.despawnTimeMinutes = despawnMinutes;

            Integer minSpawnDistance = extractIntValue(parseFrom, "minSpawnDistance");
            if (minSpawnDistance != null) config.minSpawnDistance = minSpawnDistance;

            Integer maxSpawnDistance = extractIntValue(parseFrom, "maxSpawnDistance");
            if (maxSpawnDistance != null) config.maxSpawnDistance = maxSpawnDistance;

            Boolean globalComets = extractBooleanValue(parseFrom, "globalComets");
            if (globalComets != null) config.globalComets = globalComets;

            Boolean naturalSpawnsEnabled = extractBooleanValue(parseFrom, "naturalSpawnsEnabled");
            if (naturalSpawnsEnabled != null) config.naturalSpawnsEnabled = naturalSpawnsEnabled;

            // Parse themes using ThemeConfigParser
            config.themes = ThemeConfigParser.parseThemes(json);
            config.themeList = new ArrayList<>(config.themes.values());
            config.themesLoaded = !config.themes.isEmpty();

            // Parse tier settings
            config.tierSettings = ThemeConfigParser.parseTierSettings(json);

            // Parse reward settings
            config.rewardSettings = ThemeConfigParser.parseRewardSettings(json);

            // Parse zone spawn chances
            config.zoneSpawnChances = ThemeConfigParser.parseZoneSpawnChances(json);

            // Parse new zone base loot pools
            config.zoneBaseLootPools = ThemeConfigParser.parseZoneBaseLootPools(json);

            // Parse new per-tier inheritance chances
            config.tierInheritanceWeights = ThemeConfigParser.parseTierInheritanceWeights(json);

            if (config.zoneBaseLootPools.isEmpty()) {
                config.zoneBaseLootPools = getDefaultZoneBaseLootPools();
            }
            if (config.tierInheritanceWeights.isEmpty()) {
                config.tierInheritanceWeights = getDefaultTierInheritanceWeights();
            }

            // Parse optional WorldProtect spawn rules
            String worldProtectRules = extractJsonObject(json, "worldProtectSpawnRules");
            if (worldProtectRules != null) {
                Boolean enabled = extractBooleanValue(worldProtectRules, "enabled");
                if (enabled != null) {
                    config.protectedZoneSpawnRulesEnabled = enabled;
                }

                Boolean defaultInProtectedRegion = extractBooleanValue(worldProtectRules, "defaultInWorldProtectRegion");
                if (defaultInProtectedRegion != null) {
                    config.protectedZoneDefaultInProtectedRegion = defaultInProtectedRegion;
                }

                String regionOverrides = extractJsonObject(worldProtectRules, "regionOverrides");
                if (regionOverrides != null) {
                    config.protectedZoneRegionOverrides = parseProtectedZoneRegionOverrides(regionOverrides);
                }
            }

        } catch (Exception e) {
            LOGGER.warning("Error parsing JSON: " + e.getMessage());
            e.printStackTrace();
        }

        return config;
    }

    /**
     * Save configuration to file
     */
    public void save() {
        File configFile = getConfigFile();

        File parentDir = configFile.getParentFile();
        if (parentDir != null && !parentDir.exists()) {
            parentDir.mkdirs();
        }

        // Ensure we have themes and tier settings to save
        if (themes.isEmpty()) {
            themes = DefaultThemes.generateDefaults();
            themeList = new ArrayList<>(themes.values());
        }
        if (tierSettings.isEmpty()) {
            tierSettings = DefaultThemes.getDefaultTierSettings();
        }
        if (rewardSettings.isEmpty()) {
            rewardSettings = getDefaultRewardSettings();
        }
        if (zoneSpawnChances.isEmpty()) {
            zoneSpawnChances = ZoneSpawnChances.generateDefaults();
        }
        if (tierInheritanceWeights.isEmpty()) {
            tierInheritanceWeights = getDefaultTierInheritanceWeights();
        }
        if (zoneBaseLootPools.isEmpty()) {
            zoneBaseLootPools = getDefaultZoneBaseLootPools();
        }

        try (FileWriter writer = new FileWriter(configFile)) {
            String json = ThemeConfigWriter.generateFullConfig(
                    minDelaySeconds, maxDelaySeconds, spawnChance,
                    despawnTimeMinutes, minSpawnDistance, maxSpawnDistance,
                    naturalSpawnsEnabled, globalComets,
                    themes, tierSettings, rewardSettings, zoneSpawnChances,
                    zoneBaseLootPools, tierInheritanceWeights,
                    protectedZoneSpawnRulesEnabled, protectedZoneDefaultInProtectedRegion,
                    protectedZoneRegionOverrides);
            writer.write(json);
            writer.flush();
            LOGGER.info("Saved Comet Mod configuration to: " + configFile.getAbsolutePath());

            saveThemesConfig(this, getThemesConfigFile(configFile));
        } catch (IOException e) {
            LOGGER.severe("Failed to save config file: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static Map<Integer, TierRewards> getDefaultRewardSettings() {
        Map<Integer, TierRewards> defaults = new LinkedHashMap<>();
        for (int tier = 1; tier <= 5; tier++) {
            defaults.put(tier, TierRewards.getDefaultForTier(tier));
        }
        return defaults;
    }

    private static Map<String, TierRewards> getDefaultZoneBaseLootPools() {
        Map<String, TierRewards> defaults = new LinkedHashMap<>();

        TierRewards zone0 = new TierRewards();
        zone0.addDrop(new RewardEntry("Ingredient_Bar_Copper", 2, 3, 100.0, "Copper Ingots"));
        zone0.addDrop(new RewardEntry("Ingredient_Leather_Light", 1, 2, 100.0, "Light Leather"));
        defaults.put("0", zone0);

        TierRewards zone1 = new TierRewards();
        zone1.addDrop(new RewardEntry("Ingredient_Bar_Iron", 2, 3, 100.0, "Iron Ingots"));
        zone1.addDrop(new RewardEntry("Ingredient_Leather_Medium", 1, 2, 100.0, "Medium Leather"));
        defaults.put("1", zone1);

        TierRewards zone2 = new TierRewards();
        zone2.addDrop(new RewardEntry("Ingredient_Bar_Cobalt", 1, 2, 60.0, "Cobalt Ingots"));
        zone2.addDrop(new RewardEntry("Ingredient_Bar_Thorium", 1, 2, 60.0, "Thorium Ingots"));
        zone2.addDrop(new RewardEntry("Ingredient_Leather_Heavy", 1, 2, 100.0, "Heavy Leather"));
        defaults.put("2", zone2);

        TierRewards zone3 = new TierRewards();
        zone3.addDrop(new RewardEntry("Ingredient_Bar_Adamantite", 1, 2, 100.0, "Adamantite Ingots"));
        zone3.addDrop(new RewardEntry("Ingredient_Fire_Essence", 2, 3, 100.0, "Essence of Fire"));
        zone3.addDrop(new RewardEntry("Ingredient_Fabric_Scrap_Shadoweave", 2, 3, 100.0, "Shadoweave Scraps"));
        defaults.put("3", zone3);

        return defaults;
    }

    private static Map<Integer, TierInheritanceWeights> getDefaultTierInheritanceWeights() {
        Map<Integer, TierInheritanceWeights> defaults = new LinkedHashMap<>();
        defaults.put(1, new TierInheritanceWeights(1.0, 0.0, 0.0, 0.0, 0.0));
        defaults.put(2, new TierInheritanceWeights(0.20, 1.0, 0.0, 0.0, 0.0));
        defaults.put(3, new TierInheritanceWeights(0.10, 0.25, 1.0, 0.0, 0.0));
        defaults.put(4, new TierInheritanceWeights(0.05, 0.12, 0.30, 1.0, 0.0));
        defaults.put(5, new TierInheritanceWeights(0.03, 0.08, 0.18, 0.35, 1.0));
        return defaults;
    }

    private static Map<String, Boolean> parseProtectedZoneRegionOverrides(String jsonObject) {
        Map<String, Boolean> overrides = new LinkedHashMap<>();
        Pattern pairPattern = Pattern.compile("\"([^\"]+)\"\\s*:\\s*(true|false)", Pattern.CASE_INSENSITIVE);
        Matcher matcher = pairPattern.matcher(jsonObject);
        while (matcher.find()) {
            String regionId = matcher.group(1);
            String boolText = matcher.group(2);
            if (regionId == null || regionId.isBlank()) {
                continue;
            }
            overrides.put(regionId.toLowerCase(Locale.ROOT), Boolean.parseBoolean(boolText));
        }
        return overrides;
    }

    private static void loadThemesFromSeparateFile(CometConfig config, File baseConfigFile) {
        if (config == null) {
            return;
        }

        File themesFile = getThemesConfigFile(baseConfigFile);
        if (!themesFile.exists() || !themesFile.isFile()) {
            saveThemesConfig(config, themesFile);
            return;
        }

        try {
            String themesJson = new String(java.nio.file.Files.readAllBytes(themesFile.toPath()));
            String themesBlock = extractJsonObject(themesJson, "themes");
            if (themesBlock == null || themesBlock.isBlank()) {
                LOGGER.warning("Themes file is missing top-level 'themes' object: " + themesFile.getAbsolutePath());
                return;
            }

            Map<String, ThemeConfig> externalThemes = ThemeConfigParser.parseThemes(themesJson);
            if (externalThemes.isEmpty()) {
                LOGGER.warning("Themes file has no parsed themes, keeping themes from comet_config.json");
                return;
            }

            config.themes = externalThemes;
            config.themeList = new ArrayList<>(externalThemes.values());
            config.tierStatScaling = ThemeConfigParser.parseTierStatScaling(themesJson);
            config.themesLoaded = true;
            LOGGER.info("Loaded themes and monster groups from: " + themesFile.getAbsolutePath()
                    + " (" + externalThemes.size() + " themes)");
        } catch (Exception e) {
            LOGGER.warning("Failed to load themes file, keeping themes from comet_config.json: " + e.getMessage());
        }
    }

    private static void saveThemesConfig(CometConfig config, File themesFile) {
        if (config == null || themesFile == null) {
            return;
        }

        try {
            File parentDir = themesFile.getParentFile();
            if (parentDir != null && !parentDir.exists()) {
                parentDir.mkdirs();
            }

            String json = ThemeConfigWriter.generateThemesAndMonsterGroupsConfig(config.themes, config.tierStatScaling);
            try (FileWriter writer = new FileWriter(themesFile)) {
                writer.write(json);
                writer.flush();
            }
        } catch (Exception e) {
            LOGGER.warning("Failed to save themes file '" + themesFile.getAbsolutePath() + "': " + e.getMessage());
        }
    }

    /**
     * Apply spawn settings to the spawn task
     */
    public void applyToSpawnTask(CometSpawnTask spawnTask) {
        if (spawnTask != null) {
            spawnTask.setMinDelaySeconds(minDelaySeconds);
            spawnTask.setMaxDelaySeconds(maxDelaySeconds);
            spawnTask.setSpawnChance(spawnChance);
            spawnTask.setMinSpawnDistance(minSpawnDistance);
            spawnTask.setMaxSpawnDistance(maxSpawnDistance);
        }
    }

    // ========== Theme Access Methods ==========

    /**
     * Get all theme configurations
     */
    public Map<String, ThemeConfig> getThemes() {
        return themes;
    }

    /**
     * Get themes as an ordered list (for random selection)
     */
    public List<ThemeConfig> getThemeList() {
        return themeList;
    }

    /**
     * Get a theme by ID
     */
    public ThemeConfig getTheme(String id) {
        return themes.get(id);
    }

    /**
     * Get all themes available for a specific tier (excludes themes with naturalSpawn: false)
     *
     * @param tier The comet tier (1-4)
     * @return List of themes that can spawn naturally at this tier
     */
    public List<ThemeConfig> getThemesForTier(int tier) {
        List<ThemeConfig> result = new ArrayList<>();
        for (ThemeConfig theme : themeList) {
            // Skip themes with naturalSpawn: false - they can only be spawned manually
            if (!theme.isNaturalSpawn()) {
                continue;
            }
            if (theme.isAvailableForTier(tier)) {
                result.add(theme);
            }
        }
        return result;
    }

    /**
     * Get all theme display names
     */
    public String[] getThemeNames() {
        String[] names = new String[themeList.size()];
        for (int i = 0; i < themeList.size(); i++) {
            names[i] = themeList.get(i).getDisplayName();
        }
        return names;
    }

    /**
     * Get theme count
     */
    public int getThemeCount() {
        return themeList.size();
    }

    /**
     * Check if themes were loaded successfully
     */
    public boolean hasThemes() {
        return themesLoaded && !themes.isEmpty();
    }

    public TierStatScalingConfig getTierStatScaling() {
        return tierStatScaling != null ? tierStatScaling : new TierStatScalingConfig();
    }

    public float[] getTierStatMultipliers(int tier) {
        return getTierStatScaling().getMultipliersForTier(tier);
    }

    public float[] getTierStatMultipliers(int tier, int zoneLevel) {
        return getTierStatScaling().getMultipliersForTierAndZone(tier, zoneLevel);
    }

    // ========== Tier Settings Access Methods ==========

    /**
     * Get tier settings for a specific tier
     * 
     * @param tier The tier number (1-4)
     */
    public TierSettings getTierSettings(int tier) {
        return tierSettings.getOrDefault(tier, TierSettings.getDefaultForTier(tier));
    }

    /**
     * Get all tier settings
     */
    public Map<Integer, TierSettings> getAllTierSettings() {
        return tierSettings;
    }

    /**
     * Get timeout for a tier in milliseconds
     */
    public long getTimeoutMillis(int tier) {
        return getTierSettings(tier).getTimeoutMillis();
    }

    /**
     * Get spawn radius range for a tier
     */
    public double[] getSpawnRadiusRange(int tier) {
        TierSettings ts = getTierSettings(tier);
        return new double[] { ts.getMinRadius(), ts.getMaxRadius() };
    }

    /**
     * Get reward settings for a specific tier
     * 
     * @param tier The tier number (1-4)
     */
    public TierRewards getTierRewards(int tier) {
        return rewardSettings.getOrDefault(tier, TierRewards.getDefaultForTier(tier));
    }

    /**
     * Get all reward settings
     */
    public Map<Integer, TierRewards> getAllRewardSettings() {
        return rewardSettings;
    }

    // ========== Zone Base Pools & Tier Inheritance ==========

    /**
     * Get zone base loot pool for a specific zone.
     */
    public TierRewards getZoneBaseLootPool(int zoneId) {
        String key = String.valueOf(zoneId);
        TierRewards pool = zoneBaseLootPools.get(key);
        if (pool != null) {
            return pool;
        }
        // Support 0-based config keys for worlds using Zone1..ZoneN naming.
        if (zoneId > 0) {
            pool = zoneBaseLootPools.get(String.valueOf(zoneId - 1));
            if (pool != null) {
                return pool;
            }
        }
        return new TierRewards();
    }

    public Map<String, TierRewards> getAllZoneBaseLootPools() {
        return zoneBaseLootPools;
    }

    /**
     * Get per-tier lower-tier inclusion chances.
     */
    public TierInheritanceWeights getTierInheritanceWeights(int tier) {
        TierInheritanceWeights weights = tierInheritanceWeights.get(tier);
        if (weights == null) {
            weights = getDefaultTierInheritanceWeights().get(tier);
        }
        if (weights == null) {
            weights = new TierInheritanceWeights(1.0, 0.0, 0.0, 0.0, 0.0);
        }
        return weights;
    }

    public Map<Integer, TierInheritanceWeights> getAllTierInheritanceWeights() {
        return tierInheritanceWeights;
    }

    // ========== Zone Spawn Chances Access Methods ==========

    /**
     * Get zone spawn chances for a specific zone ID.
     *
     * @param zoneId The zone ID (0, 1, 2, 3, etc.)
     * @return ZoneSpawnChances for that zone, or null if not configured
     */
    public ZoneSpawnChances getZoneSpawnChances(int zoneId) {
        String zoneKey = String.valueOf(zoneId);
        ZoneSpawnChances chances = zoneSpawnChances.get(zoneKey);
        if (chances != null) {
            return chances;
        }
        // Support 0-based config keys for worlds using Zone1..ZoneN naming.
        if (zoneId > 0) {
            return zoneSpawnChances.get(String.valueOf(zoneId - 1));
        }
        return null;
    }

    /**
     * Get all zone spawn chances
     */
    public Map<String, ZoneSpawnChances> getAllZoneSpawnChances() {
        return zoneSpawnChances;
    }

    /**
     * Set zone spawn chances for a specific zone
     */
    public void setZoneSpawnChances(String zoneKey, ZoneSpawnChances chances) {
        zoneSpawnChances.put(zoneKey, chances);
    }

    // ========== Protected Zone Rules (WorldProtect integration) ==========

    public boolean isProtectedZoneSpawnRulesEnabled() {
        return protectedZoneSpawnRulesEnabled;
    }

    public boolean getProtectedZoneDefaultInProtectedRegion() {
        return protectedZoneDefaultInProtectedRegion;
    }

    public Map<String, Boolean> getProtectedZoneRegionOverrides() {
        return Collections.unmodifiableMap(protectedZoneRegionOverrides);
    }

    public synchronized boolean isProtectedRegionSpawnAllowed(String regionId) {
        if (!protectedZoneSpawnRulesEnabled) {
            return true;
        }
        if (regionId == null || regionId.isBlank()) {
            return true;
        }

        Boolean override = protectedZoneRegionOverrides.get(regionId.toLowerCase(Locale.ROOT));
        return (override != null) ? override : protectedZoneDefaultInProtectedRegion;
    }

    /**
     * Synchronize per-region overrides against currently existing WorldProtect regions.
     * New regions are auto-added using defaultInProtectedRegion. Deleted regions are removed.
     *
     * @param activeRegionIds Region ids currently present in WorldProtect.
     * @return true if config changed and was saved.
     */
    public synchronized boolean syncProtectedRegionOverrides(Iterable<String> activeRegionIds) {
        if (activeRegionIds == null) {
            return false;
        }

        Set<String> normalizedActive = new LinkedHashSet<>();
        for (String regionId : activeRegionIds) {
            if (regionId == null || regionId.isBlank()) {
                continue;
            }
            String normalized = regionId.toLowerCase(Locale.ROOT);
            if ("__global__".equals(normalized) || "global".equals(normalized)) {
                continue;
            }
            normalizedActive.add(normalized);
        }

        boolean changed = false;

        for (String regionId : normalizedActive) {
            if (!protectedZoneRegionOverrides.containsKey(regionId)) {
                protectedZoneRegionOverrides.put(regionId, protectedZoneDefaultInProtectedRegion);
                changed = true;
            }
        }

        Set<String> keysToRemove = new LinkedHashSet<>();
        for (String existing : protectedZoneRegionOverrides.keySet()) {
            if (!normalizedActive.contains(existing)) {
                keysToRemove.add(existing);
            }
        }
        if (!keysToRemove.isEmpty()) {
            for (String removeKey : keysToRemove) {
                protectedZoneRegionOverrides.remove(removeKey);
            }
            changed = true;
        }

        if (changed) {
            save();
        }

        return changed;
    }

    /**
     * Add or update a theme in the config
     * Adds theme to all tiers (1-5) by default if no tiers specified
     */
    public void addOrUpdateTheme(ThemeConfig theme) {
        if (theme == null || theme.getId() == null) {
            LOGGER.warning("Cannot add null theme or theme with null ID");
            return;
        }

        // If theme has no tiers specified, add all tiers by default
        if (theme.getTiers() == null || theme.getTiers().isEmpty()) {
            List<Integer> allTiers = new ArrayList<>();
            for (int tier = 1; tier <= 5; tier++) {
                allTiers.add(tier);
            }
            theme.setTiers(allTiers);
        }

        // Add or update in themes map
        themes.put(theme.getId(), theme);

        // Update theme list
        themeList.removeIf(t -> t.getId().equals(theme.getId()));
        themeList.add(theme);

        themesLoaded = true;
        LOGGER.info("Added/updated theme: " + theme.getId());
    }

    /**
     * Remove a theme by ID
     */
    public void removeTheme(String themeId) {
        if (themeId == null) {
            return;
        }

        themes.remove(themeId);
        themeList.removeIf(t -> themeId.equals(t.getId()));

        LOGGER.info("Removed theme: " + themeId);
    }

    /**
     * Reload configuration from disk (renamed to avoid conflict)
     */
    public static void reloadConfig() {
        instance = null;
        load();
    }

}
