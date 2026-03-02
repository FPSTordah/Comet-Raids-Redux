package com.cometmod.config.validation;

import static com.cometmod.config.parser.ConfigJson.extractArrayObjects;
import static com.cometmod.config.parser.ConfigJson.extractArrayFromPosition;
import static com.cometmod.config.parser.ConfigJson.extractBooleanValue;
import static com.cometmod.config.parser.ConfigJson.extractDoubleValue;
import static com.cometmod.config.parser.ConfigJson.extractIntValue;
import static com.cometmod.config.parser.ConfigJson.extractJsonArray;
import static com.cometmod.config.parser.ConfigJson.extractJsonObject;
import static com.cometmod.config.parser.ConfigJson.extractObjectFromPosition;
import static com.cometmod.config.parser.ConfigJson.extractStringArray;

import java.util.List;
import java.util.regex.Pattern;

/**
 * Lightweight schema validator for mod JSON configs.
 */
public final class ConfigValidator {

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
        validateClaimProtect(json, report);
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

        String disabledWorlds = extractJsonArray(spawnSettings, "disabledWorlds");
        if (spawnSettings.contains("\"disabledWorlds\"") && disabledWorlds == null) {
            report.error("spawnSettings.disabledWorlds must be an array of world-name strings.");
        } else if (disabledWorlds != null) {
            List<String> worldNames = extractStringArray(disabledWorlds);
            for (int i = 0; i < worldNames.size(); i++) {
                String worldName = worldNames.get(i);
                if (worldName == null || worldName.trim().isEmpty()) {
                    report.warn("spawnSettings.disabledWorlds[" + i + "] is blank and will be ignored.");
                }
            }
        }
    }

    private static void validateClaimProtect(String json, ConfigValidationReport report) {
        String claimProtect = extractJsonObject(json, "claimProtect");
        if (claimProtect == null) {
            return;
        }

        String providers = extractJsonArray(claimProtect, "providers");
        if (claimProtect.contains("\"providers\"") && providers == null) {
            report.error("claimProtect.providers must be an array of provider-name strings.");
            return;
        }

        if (providers != null) {
            List<String> providerNames = extractStringArray(providers);
            for (int i = 0; i < providerNames.size(); i++) {
                String providerName = providerNames.get(i);
                if (providerName == null || providerName.trim().isEmpty()) {
                    report.warn("claimProtect.providers[" + i + "] is blank and will be ignored.");
                }
            }
        }
    }

    private static void validateThemesSchema(String json, ConfigValidationReport report) {
        String themes = extractJsonObject(json, "themes");
        if (themes == null || themes.length() < 2) {
            return;
        }

        int i = 1; // skip opening '{'
        int end = themes.length() - 1; // ignore closing '}'

        while (i < end) {
            i = skipWhitespaceAndCommas(themes, i, end);
            if (i >= end) {
                break;
            }

            if (themes.charAt(i) != '"') {
                i++;
                continue;
            }

            int keyEnd = findStringEnd(themes, i + 1);
            if (keyEnd < 0) {
                break;
            }

            String key = themes.substring(i + 1, keyEnd);
            i = keyEnd + 1;

            while (i < end && Character.isWhitespace(themes.charAt(i))) {
                i++;
            }
            if (i >= end || themes.charAt(i) != ':') {
                continue;
            }
            i++; // skip colon

            while (i < end && Character.isWhitespace(themes.charAt(i))) {
                i++;
            }
            if (i >= end) {
                break;
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

            int nextValuePos = skipJsonValue(themes, i);
            i = nextValuePos > i ? nextValuePos : i + 1;
        }
    }

    private static int skipWhitespaceAndCommas(String text, int start, int end) {
        int i = start;
        while (i < end) {
            char c = text.charAt(i);
            if (Character.isWhitespace(c) || c == ',') {
                i++;
                continue;
            }
            break;
        }
        return i;
    }

    private static int findStringEnd(String text, int start) {
        boolean escaped = false;
        for (int i = start; i < text.length(); i++) {
            char c = text.charAt(i);
            if (escaped) {
                escaped = false;
                continue;
            }
            if (c == '\\') {
                escaped = true;
                continue;
            }
            if (c == '"') {
                return i;
            }
        }
        return -1;
    }

    private static int skipJsonValue(String json, int valueStart) {
        if (valueStart < 0 || valueStart >= json.length()) {
            return valueStart;
        }

        char valueType = json.charAt(valueStart);
        if (valueType == '{') {
            String object = extractObjectFromPosition(json, valueStart);
            return object != null ? valueStart + object.length() : valueStart + 1;
        }
        if (valueType == '[') {
            String array = extractArrayFromPosition(json, valueStart);
            return array != null ? valueStart + array.length() : valueStart + 1;
        }
        if (valueType == '"') {
            int stringEnd = findStringEnd(json, valueStart + 1);
            return stringEnd >= 0 ? stringEnd + 1 : valueStart + 1;
        }

        int i = valueStart;
        while (i < json.length()) {
            char c = json.charAt(i);
            if (c == ',' || c == '}') {
                break;
            }
            i++;
        }
        return i;
    }

    private static void validateTierBlocks(String json, ConfigValidationReport report) {
        String tierSettings = extractJsonObject(json, "tierSettings");
        String rewardSettings = extractJsonObject(json, "rewardSettings");

        for (int tier = 1; tier <= 5; tier++) {
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
