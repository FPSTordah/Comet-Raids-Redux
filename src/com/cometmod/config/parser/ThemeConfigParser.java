package com.cometmod.config.parser;

import static com.cometmod.config.parser.ConfigJson.extractArrayFromPosition;
import static com.cometmod.config.parser.ConfigJson.extractArrayObjects;
import static com.cometmod.config.parser.ConfigJson.extractBooleanValue;
import static com.cometmod.config.parser.ConfigJson.extractDoubleValue;
import static com.cometmod.config.parser.ConfigJson.extractIntArray;
import static com.cometmod.config.parser.ConfigJson.extractIntValue;
import static com.cometmod.config.parser.ConfigJson.extractJsonArray;
import static com.cometmod.config.parser.ConfigJson.extractJsonObject;
import static com.cometmod.config.parser.ConfigJson.extractObjectFromPosition;
import static com.cometmod.config.parser.ConfigJson.extractStringArray;
import static com.cometmod.config.parser.ConfigJson.extractStringValue;

import com.cometmod.config.defaults.DefaultThemes;
import com.cometmod.config.model.BossEntry;
import com.cometmod.config.model.MobEntry;
import com.cometmod.config.model.RewardEntry;
import com.cometmod.config.model.ThemeConfig;
import com.cometmod.config.model.TierStatScalingConfig;
import com.cometmod.config.model.TierRewards;
import com.cometmod.config.model.TierSettings;
import com.cometmod.config.model.WaveEntry;
import com.cometmod.config.model.ZoneSpawnChances;
import com.cometmod.config.model.TierInheritanceWeights;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * JSON parser for theme configurations.
 * Handles nested objects and arrays for theme definitions.
 * 
 * Note: This is a simple parser without external dependencies.
 * For production use, consider using a JSON library like Gson.
 */
public class ThemeConfigParser {

    private static final Logger LOGGER = Logger.getLogger("ThemeConfigParser");

    /**
     * Parse themes from a JSON string
     * 
     * @param json The full config JSON content
     * @return Map of theme ID to ThemeConfig
     */
    public static Map<String, ThemeConfig> parseThemes(String json) {
        Map<String, ThemeConfig> themes = new LinkedHashMap<>();

        try {
            // Find the "themes" object
            String themesBlock = extractJsonObject(json, "themes");
            if (themesBlock == null || themesBlock.isEmpty()) {
                return DefaultThemes.generateDefaults();
            }

            // Parse each theme within the themes block
            // Look for pattern: "themeId": { ... }
            Pattern themePattern = Pattern.compile("\"([a-zA-Z0-9_]+)\"\\s*:\\s*\\{");
            Matcher matcher = themePattern.matcher(themesBlock);

            while (matcher.find()) {
                String themeId = matcher.group(1);
                int startPos = matcher.end() - 1; // Position of opening brace
                String themeJson = extractObjectFromPosition(themesBlock, startPos);

                if (themeJson != null) {
                    ThemeConfig theme = parseTheme(themeId, themeJson);
                    if (theme != null) {
                        themes.put(themeId, theme);
                    }
                }
            }
        } catch (Exception e) {
            LOGGER.warning("Error parsing themes: " + e.getMessage());
            e.printStackTrace();
        }

        if (themes.isEmpty()) {
            LOGGER.warning("No themes parsed, falling back to defaults");
            return DefaultThemes.generateDefaults();
        }

        return themes;
    }

    /**
     * Parse a single theme from its JSON block
     */
    private static ThemeConfig parseTheme(String id, String json) {
        try {
            ThemeConfig theme = new ThemeConfig();
            theme.setId(id);

            // Parse displayName
            String displayName = extractStringValue(json, "displayName");
            theme.setDisplayName(displayName != null ? displayName : id);

            // Parse useTierSuffix (default true)
            Boolean useTierSuffix = extractBooleanValue(json, "useTierSuffix");
            theme.setUseTierSuffix(useTierSuffix != null ? useTierSuffix : true);

            // Parse randomBossSelection (default false)
            Boolean randomBossSelection = extractBooleanValue(json, "randomBossSelection");
            theme.setRandomBossSelection(randomBossSelection != null ? randomBossSelection : false);

            // Parse naturalSpawn (default true) - if false, theme won't spawn naturally.
            Boolean naturalSpawn = extractBooleanValue(json, "naturalSpawn");
            theme.setNaturalSpawn(naturalSpawn != null ? naturalSpawn : true);

            // Parse tiers array
            List<Integer> tiers = extractIntArray(json, "tiers");
            theme.setTiers(tiers);

            // Parse mobs array
            List<MobEntry> mobs = parseMobs(json);
            theme.setMobs(mobs);

            // Parse bosses array
            List<BossEntry> bosses = parseBosses(json);
            theme.setBosses(bosses);

            // Parse multi-wave array.
            List<WaveEntry> waves = parseWaves(json);
            if (waves.isEmpty()) {
                waves = synthesizeWavesFromTheme(theme);
            }
            theme.setWaves(waves);

            // Parse rewardOverride if present (per-tier custom loot)
            parseRewardOverride(json, theme);

            return theme;
        } catch (Exception e) {
            LOGGER.warning("Error parsing theme '" + id + "': " + e.getMessage());
            return null;
        }
    }

    /**
     * Parse global tier stat scaling from JSON.
     *
     * Format:
     * "tierStatScaling": {
     *   "enabled": true,
     *   "percentPerTier": 5.0,
     *   "applyHp": true,
     *   "applyDamage": true,
     *   "applySpeed": true,
     *   "applyScale": false
     * }
     */
    public static TierStatScalingConfig parseTierStatScaling(String json) {
        TierStatScalingConfig scaling = new TierStatScalingConfig();
        try {
            String block = extractJsonObject(json, "tierStatScaling");
            if (block == null) {
                return scaling;
            }

            Boolean enabled = extractBooleanValue(block, "enabled");
            if (enabled != null) {
                scaling.setEnabled(enabled);
            }

            Double percentPerTier = extractDoubleValue(block, "percentPerTier");
            if (percentPerTier != null) {
                scaling.setPercentPerTier(percentPerTier);
            }

            Double zonePercentPerLevel = extractDoubleValue(block, "zonePercentPerLevel");
            if (zonePercentPerLevel != null) {
                scaling.setZonePercentPerLevel(zonePercentPerLevel);
            }

            Boolean applyHp = extractBooleanValue(block, "applyHp");
            if (applyHp != null) {
                scaling.setApplyHp(applyHp);
            }

            Boolean applyDamage = extractBooleanValue(block, "applyDamage");
            if (applyDamage != null) {
                scaling.setApplyDamage(applyDamage);
            }

            Boolean applySpeed = extractBooleanValue(block, "applySpeed");
            if (applySpeed != null) {
                scaling.setApplySpeed(applySpeed);
            }

            Boolean applyScale = extractBooleanValue(block, "applyScale");
            if (applyScale != null) {
                scaling.setApplyScale(applyScale);
            }
        } catch (Exception e) {
            LOGGER.warning("Error parsing tierStatScaling: " + e.getMessage());
        }
        return scaling;
    }

    /**
     * Parse rewardOverride section from theme JSON.
     * Format:
     * "rewardOverride": {
     *   "2": { "drops": [...], "bonusDrops": [...] },
     *   "3": { "drops": [...], "bonusDrops": [...] }
     * }
     */
    private static void parseRewardOverride(String json, ThemeConfig theme) {
        try {
            String rewardBlock = extractJsonObject(json, "rewardOverride");
            if (rewardBlock == null) {
                return; // No reward override configured
            }

            Map<Integer, TierRewards> rewardOverride = new LinkedHashMap<>();

            // Parse each tier's rewards: "1": {...}, "2": {...}, etc.
            for (int tier = 1; tier <= 5; tier++) {
                String tierKey = String.valueOf(tier);
                String tierJson = extractJsonObject(rewardBlock, tierKey);

                if (tierJson != null) {
                    TierRewards tr = new TierRewards();

                    // Parse drops array
                    List<RewardEntry> drops = parseRewardEntries(tierJson, "drops");
                    tr.setDrops(drops);

                    // Parse bonusDrops array
                    List<RewardEntry> bonusDrops = parseRewardEntries(tierJson, "bonusDrops");
                    tr.setBonusDrops(bonusDrops);

                    rewardOverride.put(tier, tr);
                }
            }

            if (!rewardOverride.isEmpty()) {
                theme.setRewardOverride(rewardOverride);
            }
        } catch (Exception e) {
            LOGGER.warning("Error parsing reward override: " + e.getMessage());
        }
    }

    /**
     * Parse mobs array from theme JSON
     */
    private static List<MobEntry> parseMobs(String json) {
        List<MobEntry> mobs = new ArrayList<>();

        try {
            String mobsArray = extractJsonArray(json, "mobs");
            if (mobsArray == null)
                return mobs;

            // Parse each mob object in the array
            List<String> mobObjects = extractArrayObjects(mobsArray);
            for (String mobJson : mobObjects) {
                MobEntry mob = new MobEntry();

                String id = extractStringValue(mobJson, "id");
                if (id != null)
                    mob.setId(id);

                // Try to parse count as integer first
                Integer count = extractIntValue(mobJson, "count");
                if (count != null) {
                    mob.setCount(count);
                } else {
                    // Try to parse as tier-based count object: "count": { "1": 4, "2": 5 }
                    String countObj = extractJsonObject(mobJson, "count");
                    if (countObj != null) {
                        Map<Integer, Integer> tierCounts = new LinkedHashMap<>();
                        for (int tier = 1; tier <= 5; tier++) {
                            Integer tierCount = extractIntValue(countObj, String.valueOf(tier));
                            if (tierCount != null) {
                                tierCounts.put(tier, tierCount);
                            }
                        }
                        if (!tierCounts.isEmpty()) {
                            mob.setTierCounts(tierCounts);
                            // Set simple count to max value as fallback
                            int maxCount = tierCounts.values().stream().mapToInt(Integer::intValue).max().orElse(1);
                            mob.setCount(maxCount);
                        }
                    }
                }

                if (id != null && !id.isEmpty()) {
                    mobs.add(mob);
                }
            }
        } catch (Exception e) {
            LOGGER.warning("Error parsing mobs: " + e.getMessage());
        }

        return mobs;
    }

    /**
     * Parse waves array from theme JSON for multi-wave support.
     * Format:
     * "waves": [
     *   { "type": "normal", "mobs": [...] },
     *   { "type": "normal", "mobs": [...] },
     *   { "type": "boss", "bosses": [...], "randomBossSelection": true }
     * ]
     */
    private static List<WaveEntry> parseWaves(String json) {
        List<WaveEntry> waves = new ArrayList<>();

        try {
            String wavesArray = extractJsonArray(json, "waves");
            if (wavesArray == null) {
                return waves;
            }

            // Parse each wave object in the array
            List<String> waveObjects = extractArrayObjects(wavesArray);
            for (String waveJson : waveObjects) {
                WaveEntry wave = new WaveEntry();

                // Parse type (default to "normal")
                String type = extractStringValue(waveJson, "type");
                if (type != null) {
                    wave.setType(type);
                }

                // Parse mobs array (for normal waves)
                List<MobEntry> waveMobs = parseMobs(waveJson);
                wave.setMobs(waveMobs);

                // Parse bosses array (for boss waves)
                List<BossEntry> waveBosses = parseBosses(waveJson);
                wave.setBosses(waveBosses);

                // Parse randomBossSelection (default false)
                Boolean randomBossSelection = extractBooleanValue(waveJson, "randomBossSelection");
                wave.setRandomBossSelection(randomBossSelection != null ? randomBossSelection : false);

                waves.add(wave);
            }

        } catch (Exception e) {
            LOGGER.warning("Error parsing waves: " + e.getMessage());
        }

        return waves;
    }

    private static List<WaveEntry> synthesizeWavesFromTheme(ThemeConfig theme) {
        List<WaveEntry> waves = new ArrayList<>();

        if (theme.getMobs() != null && !theme.getMobs().isEmpty()) {
            WaveEntry normalWave = new WaveEntry(WaveEntry.WaveType.NORMAL);
            normalWave.setMobs(theme.getMobs());
            waves.add(normalWave);
        }

        if (theme.getBosses() != null && !theme.getBosses().isEmpty()) {
            WaveEntry bossWave = new WaveEntry(WaveEntry.WaveType.BOSS);
            bossWave.setBosses(theme.getBosses());
            bossWave.setRandomBossSelection(theme.useRandomBossSelection());
            waves.add(bossWave);
        }

        return waves;
    }

    /**
     * Parse bosses array from theme JSON
     */
    private static List<BossEntry> parseBosses(String json) {
        List<BossEntry> bosses = new ArrayList<>();

        try {
            String bossesArray = extractJsonArray(json, "bosses");
            if (bossesArray == null)
                return bosses;

            // Check if bosses are simple strings or objects
            if (bossesArray.contains("{")) {
                // Object format: { "id": "Boss" }
                List<String> bossObjects = extractArrayObjects(bossesArray);
                for (String bossJson : bossObjects) {
                    BossEntry boss = new BossEntry();

                    String id = extractStringValue(bossJson, "id");
                    if (id != null)
                        boss.setId(id);

                    if (id != null && !id.isEmpty()) {
                        bosses.add(boss);
                    }
                }
            } else {
                // Simple string format: ["Boss1", "Boss2"]
                List<String> bossNames = extractStringArray(bossesArray);
                for (String name : bossNames) {
                    if (name != null && !name.isEmpty()) {
                        bosses.add(new BossEntry(name));
                    }
                }
            }
        } catch (Exception e) {
            LOGGER.warning("Error parsing bosses: " + e.getMessage());
        }

        return bosses;
    }

    /**
     * Parse tier settings from config JSON
     */
    public static Map<Integer, TierSettings> parseTierSettings(String json) {
        Map<Integer, TierSettings> settings = new LinkedHashMap<>();

        try {
            String tierBlock = extractJsonObject(json, "tierSettings");
            if (tierBlock == null) {
                return DefaultThemes.getDefaultTierSettings();
            }

            // Parse each tier: "1": { ... }, "2": { ... }
            for (int tier = 1; tier <= 5; tier++) {
                String tierKey = String.valueOf(tier);
                String tierJson = extractJsonObject(tierBlock, tierKey);

                if (tierJson != null) {
                    TierSettings ts = new TierSettings();

                    Integer timeout = extractIntValue(tierJson, "timeoutSeconds");
                    if (timeout != null)
                        ts.setTimeoutSeconds(timeout);

                    Double minRadius = extractDoubleValue(tierJson, "minRadius");
                    if (minRadius != null)
                        ts.setMinRadius(minRadius);

                    Double maxRadius = extractDoubleValue(tierJson, "maxRadius");
                    if (maxRadius != null)
                        ts.setMaxRadius(maxRadius);

                    settings.put(tier, ts);
                } else {
                    // Use defaults for this tier
                    settings.put(tier, TierSettings.getDefaultForTier(tier));
                }
            }
        } catch (Exception e) {
            LOGGER.warning("Error parsing tier settings: " + e.getMessage());
            return DefaultThemes.getDefaultTierSettings();
        }

        return settings;
    }

    /**
     * Parse reward settings from config JSON
     */
    public static Map<Integer, TierRewards> parseRewardSettings(String json) {
        Map<Integer, TierRewards> rewards = new LinkedHashMap<>();

        try {
            String rewardBlock = extractJsonObject(json, "rewardSettings");
            if (rewardBlock == null) {
                // Return defaults
                for (int tier = 1; tier <= 5; tier++) {
                    rewards.put(tier, TierRewards.getDefaultForTier(tier));
                }
                return rewards;
            }

            // Parse each tier: "1": { ... }, "2": { ... }
            for (int tier = 1; tier <= 5; tier++) {
                String tierKey = String.valueOf(tier);
                String tierJson = extractJsonObject(rewardBlock, tierKey);

                if (tierJson != null) {
                    TierRewards tr = new TierRewards();

                    // Parse drops array
                    List<RewardEntry> drops = parseRewardEntries(tierJson, "drops");
                    tr.setDrops(drops);

                    // Parse bonusDrops array
                    List<RewardEntry> bonusDrops = parseRewardEntries(tierJson, "bonusDrops");
                    tr.setBonusDrops(bonusDrops);

                    rewards.put(tier, tr);
                } else {
                    // Use defaults for this tier
                    rewards.put(tier, TierRewards.getDefaultForTier(tier));
                }
            }
        } catch (Exception e) {
            LOGGER.warning("Error parsing reward settings: " + e.getMessage());
            // Return defaults
            for (int tier = 1; tier <= 5; tier++) {
                rewards.put(tier, TierRewards.getDefaultForTier(tier));
            }
        }

        return rewards;
    }

    /**
     * Parse reward entries (drops or bonusDrops) from tier JSON
     */
    private static List<RewardEntry> parseRewardEntries(String json, String key) {
        List<RewardEntry> entries = new ArrayList<>();

        try {
            String entriesArray = extractJsonArray(json, key);
            if (entriesArray == null)
                return entries;

            // Parse each reward object in the array
            List<String> rewardObjects = extractArrayObjects(entriesArray);
            for (String rewardJson : rewardObjects) {
                RewardEntry reward = new RewardEntry();

                String id = extractStringValue(rewardJson, "id");
                if (id != null)
                    reward.setId(id);

                Integer minCount = extractIntValue(rewardJson, "minCount");
                if (minCount != null)
                    reward.setMinCount(minCount);

                Integer maxCount = extractIntValue(rewardJson, "maxCount");
                if (maxCount != null)
                    reward.setMaxCount(maxCount);

                Double chance = extractDoubleValue(rewardJson, "chance");
                if (chance != null)
                    reward.setChance(chance);

                String displayName = extractStringValue(rewardJson, "displayName");
                if (displayName != null)
                    reward.setDisplayName(displayName);

                if (id != null && !id.isEmpty()) {
                    entries.add(reward);
                }
            }
        } catch (Exception e) {
            LOGGER.warning("Error parsing reward entries for '" + key + "': " + e.getMessage());
        }

        return entries;
    }

    /**
     * Parse zone spawn chances from config JSON.
     * Format:
     * "zoneSpawnChances": {
     *   "0": { "tier1": 1.0, "tier2": 0.0, "tier3": 0.0, "tier4": 0.0 },
     *   "1": { "tier1": 0.8, "tier2": 0.2, "tier3": 0.0, "tier4": 0.0 }
     * }
     */
    public static Map<String, ZoneSpawnChances> parseZoneSpawnChances(String json) {
        Map<String, ZoneSpawnChances> zoneChances = new LinkedHashMap<>();

        try {
            String zoneBlock = extractJsonObject(json, "zoneSpawnChances");
            if (zoneBlock == null) {
                return zoneChances;
            }

            // Parse each zone entry: "0": { ... }, "1": { ... }
            Pattern zonePattern = Pattern.compile("\"([a-zA-Z0-9_]+)\"\\s*:\\s*\\{");
            Matcher matcher = zonePattern.matcher(zoneBlock);

            while (matcher.find()) {
                String zoneKey = matcher.group(1);
                int startPos = matcher.end() - 1;
                String zoneJson = extractObjectFromPosition(zoneBlock, startPos);

                if (zoneJson != null) {
                    ZoneSpawnChances chances = new ZoneSpawnChances();

                    Double tier1 = extractDoubleValue(zoneJson, "tier1");
                    if (tier1 != null) chances.setTier1(tier1);

                    Double tier2 = extractDoubleValue(zoneJson, "tier2");
                    if (tier2 != null) chances.setTier2(tier2);

                    Double tier3 = extractDoubleValue(zoneJson, "tier3");
                    if (tier3 != null) chances.setTier3(tier3);

                    Double tier4 = extractDoubleValue(zoneJson, "tier4");
                    if (tier4 != null) chances.setTier4(tier4);

                    Double tier5 = extractDoubleValue(zoneJson, "tier5");
                    if (tier5 != null) chances.setTier5(tier5);

                    zoneChances.put(zoneKey, chances);
                }
            }
        } catch (Exception e) {
            LOGGER.warning("Error parsing zone spawn chances: " + e.getMessage());
            e.printStackTrace();
        }

        if (zoneChances.isEmpty()) {
            LOGGER.warning("No zone spawn chances parsed");
        }

        return zoneChances;
    }

    /**
     * Parse per-zone base loot pools from config JSON.
     * Format:
     * "zoneBaseLootPools": {
     *   "1": { "drops": [...], "bonusDrops": [...] },
     *   "2": { "drops": [...], "bonusDrops": [...] },
     *   "default": { "drops": [...], "bonusDrops": [...] }
     * }
     */
    public static Map<String, TierRewards> parseZoneBaseLootPools(String json) {
        Map<String, TierRewards> zonePools = new LinkedHashMap<>();

        try {
            String zoneBlock = extractJsonObject(json, "zoneBaseLootPools");
            if (zoneBlock == null || zoneBlock.isEmpty()) {
                return zonePools;
            }

            Pattern zonePattern = Pattern.compile("\"([a-zA-Z0-9_]+)\"\\s*:\\s*\\{");
            Matcher matcher = zonePattern.matcher(zoneBlock);

            while (matcher.find()) {
                String zoneKey = matcher.group(1);
                int startPos = matcher.end() - 1;
                String zoneJson = extractObjectFromPosition(zoneBlock, startPos);
                if (zoneJson == null) {
                    continue;
                }

                TierRewards pool = parseTierRewardsObject(zoneJson);
                zonePools.put(zoneKey, pool);
            }
        } catch (Exception e) {
            LOGGER.warning("Error parsing zone base loot pools: " + e.getMessage());
            e.printStackTrace();
        }

        return zonePools;
    }

    /**
     * Parse per-tier inheritance chances from config JSON.
     * Format:
     * "tierInheritanceWeights": {
     *   "2": { "tier1": 0.2, "tier2": 1.0, "tier3": 0.0, "tier4": 0.0 }
     * }
     */
    public static Map<Integer, TierInheritanceWeights> parseTierInheritanceWeights(String json) {
        Map<Integer, TierInheritanceWeights> weights = new LinkedHashMap<>();

        try {
            String weightsBlock = extractJsonObject(json, "tierInheritanceWeights");
            if (weightsBlock == null || weightsBlock.isEmpty()) {
                return weights;
            }

            for (int tier = 1; tier <= 5; tier++) {
                String tierJson = extractJsonObject(weightsBlock, String.valueOf(tier));
                if (tierJson == null) {
                    continue;
                }

                TierInheritanceWeights tih = new TierInheritanceWeights();

                Double t1 = extractDoubleValue(tierJson, "tier1");
                if (t1 != null) {
                    tih.setTier1Chance(t1);
                }

                Double t2 = extractDoubleValue(tierJson, "tier2");
                if (t2 != null) {
                    tih.setTier2Chance(t2);
                }

                Double t3 = extractDoubleValue(tierJson, "tier3");
                if (t3 != null) {
                    tih.setTier3Chance(t3);
                }

                Double t4 = extractDoubleValue(tierJson, "tier4");
                if (t4 != null) {
                    tih.setTier4Chance(t4);
                }

                Double t5 = extractDoubleValue(tierJson, "tier5");
                if (t5 != null) {
                    tih.setTier5Chance(t5);
                }

                tih.clampToCurrentTier(tier);
                weights.put(tier, tih);
            }
        } catch (Exception e) {
            LOGGER.warning("Error parsing tier inheritance weights: " + e.getMessage());
            e.printStackTrace();
        }

        return weights;
    }

    private static TierRewards parseTierRewardsObject(String json) {
        TierRewards tr = new TierRewards();
        tr.setDrops(parseRewardEntries(json, "drops"));
        tr.setBonusDrops(parseRewardEntries(json, "bonusDrops"));
        return tr;
    }

}
