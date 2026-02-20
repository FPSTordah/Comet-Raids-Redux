package com.cometmod.config.validation;

import static com.cometmod.config.parser.ConfigJson.extractArrayObjects;
import static com.cometmod.config.parser.ConfigJson.extractBooleanValue;
import static com.cometmod.config.parser.ConfigJson.extractDoubleValue;
import static com.cometmod.config.parser.ConfigJson.extractIntValue;
import static com.cometmod.config.parser.ConfigJson.extractJsonArray;
import static com.cometmod.config.parser.ConfigJson.extractJsonObject;
import static com.cometmod.config.parser.ConfigJson.extractStringArray;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Lightweight schema validator for mod JSON configs.
 */
public final class ConfigValidator {

    private static final Pattern THEME_ENTRY_PATTERN = Pattern.compile("\"([a-zA-Z0-9_]+)\"\\s*:");
    private static final Pattern SCHEDULE_TIME_PATTERN = Pattern.compile("^([01]\\d|2[0-3]):[0-5]\\d$");

    private ConfigValidator() {
    }

    public static ConfigValidationReport validateCometConfig(String json) {
        ConfigValidationReport report = new ConfigValidationReport();
        if (json == null || json.trim().isEmpty()) {
            report.error("comet_config.json is empty. Restore a valid config file.");
            return report;
        }

        validateTopLevelBlocks(json, report);
        validateSpawnSettings(json, report);
        validateThemesSchema(json, report);
        validateTierBlocks(json, report);

        return report;
    }

    public static ConfigValidationReport validateFixedSpawns(String json) {
        ConfigValidationReport report = new ConfigValidationReport();
        if (json == null || json.trim().isEmpty()) {
            report.error("fixed_spawns.json is empty. Add {\"spawns\":[]} or restore the file.");
            return report;
        }

        String spawnsArray = extractJsonArray(json, "spawns");
        if (spawnsArray == null) {
            report.error("fixed_spawns.json is missing required key: spawns[]");
            return report;
        }

        List<String> spawns = extractArrayObjects(spawnsArray);
        for (int i = 0; i < spawns.size(); i++) {
            String spawn = spawns.get(i);
            int idx = i + 1;

            Integer x = extractIntValue(spawn, "x");
            Integer y = extractIntValue(spawn, "y");
            Integer z = extractIntValue(spawn, "z");
            if (x == null || y == null || z == null) {
                report.error("fixed_spawns.json spawns[" + i + "] must define integer x, y, z.");
            }

            Integer cooldown = extractIntValue(spawn, "cooldownSeconds");
            String scheduledTimes = extractJsonArray(spawn, "scheduledTimes");
            if (cooldown == null && scheduledTimes == null) {
                report.warn("fixed_spawns.json spawns[" + i + "] has neither cooldownSeconds nor scheduledTimes; it will never trigger.");
            }

            if (cooldown != null && cooldown <= 0) {
                report.warn("fixed_spawns.json spawns[" + i + "] cooldownSeconds should be > 0.");
            }

            if (scheduledTimes != null) {
                List<String> times = extractStringArray(scheduledTimes);
                if (times.isEmpty()) {
                    report.warn("fixed_spawns.json spawns[" + i + "] scheduledTimes is empty; consider removing it or adding HH:mm values.");
                }
                for (String t : times) {
                    if (!SCHEDULE_TIME_PATTERN.matcher(t).matches()) {
                        report.warn("fixed_spawns.json spawns[" + i + "] invalid time '" + t + "'. Expected HH:mm (24-hour).");
                    }
                }
            }

            Boolean enabled = extractBooleanValue(spawn, "enabled");
            if (enabled == null) {
                report.info("fixed_spawns.json spawns[" + i + "] does not set enabled; runtime defaults will apply.");
            }

            if (x != null && y != null && z != null) {
                report.info("fixed_spawns.json spawn #" + idx + " parsed at " + x + "," + y + "," + z);
            }
        }

        return report;
    }

    private static void validateTopLevelBlocks(String json, ConfigValidationReport report) {
        requireObject(json, "spawnSettings", report);
        requireObject(json, "themes", report);
        requireObject(json, "tierSettings", report);
        requireObject(json, "rewardSettings", report);
        requireObject(json, "zoneSpawnChances", report);
    }

    private static void validateSpawnSettings(String json, ConfigValidationReport report) {
        String spawnSettings = extractJsonObject(json, "spawnSettings");
        if (spawnSettings == null) {
            return;
        }

        Integer minDelay = extractIntValue(spawnSettings, "minDelaySeconds");
        Integer maxDelay = extractIntValue(spawnSettings, "maxDelaySeconds");
        Double chance = extractDoubleValue(spawnSettings, "spawnChance");
        Integer minDistance = extractIntValue(spawnSettings, "minSpawnDistance");
        Integer maxDistance = extractIntValue(spawnSettings, "maxSpawnDistance");
        Double despawn = extractDoubleValue(spawnSettings, "despawnTimeMinutes");

        if (minDelay != null && minDelay <= 0) {
            report.warn("spawnSettings.minDelaySeconds should be > 0.");
        }
        if (maxDelay != null && maxDelay <= 0) {
            report.warn("spawnSettings.maxDelaySeconds should be > 0.");
        }
        if (minDelay != null && maxDelay != null && maxDelay < minDelay) {
            report.error("spawnSettings.maxDelaySeconds must be >= minDelaySeconds.");
        }
        if (chance != null && (chance < 0.0 || chance > 1.0)) {
            report.error("spawnSettings.spawnChance must be between 0.0 and 1.0.");
        }
        if (minDistance != null && minDistance < 0) {
            report.warn("spawnSettings.minSpawnDistance should be >= 0.");
        }
        if (maxDistance != null && maxDistance < 0) {
            report.warn("spawnSettings.maxSpawnDistance should be >= 0.");
        }
        if (minDistance != null && maxDistance != null && maxDistance < minDistance) {
            report.error("spawnSettings.maxSpawnDistance must be >= minSpawnDistance.");
        }
        if (despawn != null && despawn <= 0) {
            report.warn("spawnSettings.despawnTimeMinutes should be > 0.");
        }
    }

    private static void validateThemesSchema(String json, ConfigValidationReport report) {
        String themes = extractJsonObject(json, "themes");
        if (themes == null) {
            return;
        }

        Matcher matcher = THEME_ENTRY_PATTERN.matcher(themes);
        while (matcher.find()) {
            String key = matcher.group(1);
            int colon = themes.indexOf(':', matcher.end() - 1);
            if (colon < 0) {
                continue;
            }
            int i = colon + 1;
            while (i < themes.length() && Character.isWhitespace(themes.charAt(i))) {
                i++;
            }
            if (i >= themes.length()) {
                continue;
            }

            char valueStart = themes.charAt(i);
            if (key.startsWith("_")) {
                if (valueStart == '"') {
                    report.info("themes." + key + " recognized as pseudo-comment key.");
                } else {
                    report.warn("themes." + key + " starts with '_' but is not a string comment.");
                }
                continue;
            }

            if (valueStart != '{') {
                report.error("themes." + key + " must be an object. Non-object entries should be prefixed with '_' comments.");
            }
        }
    }

    private static void validateTierBlocks(String json, ConfigValidationReport report) {
        String tierSettings = extractJsonObject(json, "tierSettings");
        String rewardSettings = extractJsonObject(json, "rewardSettings");

        for (int tier = 1; tier <= 4; tier++) {
            if (tierSettings != null && extractJsonObject(tierSettings, String.valueOf(tier)) == null) {
                report.warn("tierSettings." + tier + " missing; default tier settings will be used.");
            }
            if (rewardSettings != null && extractJsonObject(rewardSettings, String.valueOf(tier)) == null) {
                report.warn("rewardSettings." + tier + " missing; default rewards will be used.");
            }
        }
    }

    private static void requireObject(String json, String key, ConfigValidationReport report) {
        if (extractJsonObject(json, key) == null) {
            report.error("Missing required top-level object: " + key);
        }
    }
}
