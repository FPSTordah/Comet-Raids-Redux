package com.cometmod.config.parser;

import com.cometmod.config.model.BossEntry;
import com.cometmod.config.model.MobEntry;
import com.cometmod.config.model.RewardEntry;
import com.cometmod.config.model.ThemeConfig;
import com.cometmod.config.model.TierStatScalingConfig;
import com.cometmod.config.model.TierRewards;
import com.cometmod.config.model.TierSettings;
import com.cometmod.config.model.ZoneSpawnChances;
import com.cometmod.config.model.TierInheritanceWeights;

import java.util.List;
import java.util.Map;

/**
 * JSON writer for generating config files.
 * Creates properly formatted JSON without external dependencies.
 */
public class ThemeConfigWriter {

    private static final String INDENT = "  ";

    /**
     * Generate complete config JSON with all settings
     */
    public static String generateFullConfig(
            int minDelaySeconds, int maxDelaySeconds, double spawnChance,
            double despawnTimeMinutes, int minSpawnDistance, int maxSpawnDistance,
            Map<String, ThemeConfig> themes, Map<Integer, TierSettings> tierSettings,
            Map<Integer, TierRewards> rewardSettings) {
        // Call overloaded method with defaults for new parameters
        return generateFullConfig(minDelaySeconds, maxDelaySeconds, spawnChance,
                despawnTimeMinutes, minSpawnDistance, maxSpawnDistance,
                true, false, null, // naturalSpawnsEnabled, globalComets, disabledWorlds
                themes, tierSettings, rewardSettings, null);
    }

    /**
     * Generate a themes-only config intended for user-editable theme/monster-group
     * management.
     */
    public static String generateThemesAndMonsterGroupsConfig(Map<String, ThemeConfig> themes) {
        return generateThemesAndMonsterGroupsConfig(themes, new TierStatScalingConfig());
    }

    /**
     * Generate a themes-only config intended for user-editable theme/monster-group
     * management, including global tier stat scaling.
     */
    public static String generateThemesAndMonsterGroupsConfig(
            Map<String, ThemeConfig> themes,
            TierStatScalingConfig tierStatScaling) {
        if (themes == null) {
            themes = java.util.Collections.emptyMap();
        }
        if (tierStatScaling == null) {
            tierStatScaling = new TierStatScalingConfig();
        }

        StringBuilder sb = new StringBuilder();
        sb.append("{\n");
        sb.append(INDENT).append("\"tierStatScaling\": {\n");
        sb.append(INDENT).append(INDENT).append("\"enabled\": ").append(tierStatScaling.isEnabled()).append(",\n");
        sb.append(INDENT).append(INDENT).append("\"percentPerTier\": ").append(tierStatScaling.getPercentPerTier())
                .append(",\n");
        sb.append(INDENT).append(INDENT).append("\"zonePercentPerLevel\": ")
                .append(tierStatScaling.getZonePercentPerLevel()).append(",\n");
        sb.append(INDENT).append(INDENT).append("\"applyHp\": ").append(tierStatScaling.isApplyHp()).append(",\n");
        sb.append(INDENT).append(INDENT).append("\"applyDamage\": ").append(tierStatScaling.isApplyDamage())
                .append(",\n");
        sb.append(INDENT).append(INDENT).append("\"applySpeed\": ").append(tierStatScaling.isApplySpeed())
                .append(",\n");
        sb.append(INDENT).append(INDENT).append("\"applyScale\": ").append(tierStatScaling.isApplyScale()).append("\n");
        sb.append(INDENT).append("},\n");
        sb.append("\n");
        sb.append(INDENT).append("\"themes\": {\n");

        int index = 0;
        for (Map.Entry<String, ThemeConfig> entry : themes.entrySet()) {
            index++;
            writeTheme(sb, entry.getKey(), entry.getValue(), index < themes.size());
        }

        sb.append(INDENT).append("}\n");
        sb.append("}\n");
        return sb.toString();
    }

    /**
     * Generate complete config JSON with all settings including zone spawn chances
     */
    public static String generateFullConfig(
            int minDelaySeconds, int maxDelaySeconds, double spawnChance,
            double despawnTimeMinutes, int minSpawnDistance, int maxSpawnDistance,
            Map<String, ThemeConfig> themes, Map<Integer, TierSettings> tierSettings,
            Map<Integer, TierRewards> rewardSettings, Map<String, ZoneSpawnChances> zoneSpawnChances) {
        // Call overloaded method with defaults for new parameters
        return generateFullConfig(minDelaySeconds, maxDelaySeconds, spawnChance,
                despawnTimeMinutes, minSpawnDistance, maxSpawnDistance,
                true, false, null, // naturalSpawnsEnabled, globalComets, disabledWorlds
                themes, tierSettings, rewardSettings, zoneSpawnChances);
    }

    /**
     * Generate complete config JSON with all settings including natural spawns toggle
     */
    public static String generateFullConfig(
            int minDelaySeconds, int maxDelaySeconds, double spawnChance,
            double despawnTimeMinutes, int minSpawnDistance, int maxSpawnDistance,
            boolean naturalSpawnsEnabled, boolean globalComets,
            Map<String, ThemeConfig> themes, Map<Integer, TierSettings> tierSettings,
            Map<Integer, TierRewards> rewardSettings, Map<String, ZoneSpawnChances> zoneSpawnChances) {
        return generateFullConfig(
                minDelaySeconds, maxDelaySeconds, spawnChance,
                despawnTimeMinutes, minSpawnDistance, maxSpawnDistance,
                naturalSpawnsEnabled, globalComets, null,
                themes, tierSettings, rewardSettings, zoneSpawnChances);
    }

    /**
     * Generate complete config JSON with all settings including disabled world list.
     */
    public static String generateFullConfig(
            int minDelaySeconds, int maxDelaySeconds, double spawnChance,
            double despawnTimeMinutes, int minSpawnDistance, int maxSpawnDistance,
            boolean naturalSpawnsEnabled, boolean globalComets, List<String> disabledWorlds,
            Map<String, ThemeConfig> themes, Map<Integer, TierSettings> tierSettings,
            Map<Integer, TierRewards> rewardSettings, Map<String, ZoneSpawnChances> zoneSpawnChances) {
        return generateFullConfig(
                minDelaySeconds, maxDelaySeconds, spawnChance,
                despawnTimeMinutes, minSpawnDistance, maxSpawnDistance,
                naturalSpawnsEnabled, globalComets, disabledWorlds,
                themes, tierSettings, rewardSettings, zoneSpawnChances,
                null, null, false, true, null);
    }

    /**
     * Generate complete config JSON including WorldProtect spawn rules.
     */
    public static String generateFullConfig(
            int minDelaySeconds, int maxDelaySeconds, double spawnChance,
            double despawnTimeMinutes, int minSpawnDistance, int maxSpawnDistance,
            boolean naturalSpawnsEnabled, boolean globalComets, List<String> disabledWorlds,
            Map<String, ThemeConfig> themes, Map<Integer, TierSettings> tierSettings,
            Map<Integer, TierRewards> rewardSettings, Map<String, ZoneSpawnChances> zoneSpawnChances,
            Map<String, TierRewards> zoneBaseLootPools, Map<Integer, TierInheritanceWeights> tierInheritanceWeights,
            boolean protectedZoneRulesEnabled, boolean defaultInProtectedRegion,
            Map<String, Boolean> regionOverrides) {

        StringBuilder sb = new StringBuilder();
        sb.append("{\n");

        // Spawn settings section
        sb.append(INDENT).append("\"spawnSettings\": {\n");
        sb.append(INDENT).append(INDENT).append("\"naturalSpawnsEnabled\": ").append(naturalSpawnsEnabled).append(",\n");
        sb.append(INDENT).append(INDENT).append("\"minDelaySeconds\": ").append(minDelaySeconds).append(",\n");
        sb.append(INDENT).append(INDENT).append("\"maxDelaySeconds\": ").append(maxDelaySeconds).append(",\n");
        sb.append(INDENT).append(INDENT).append("\"spawnChance\": ").append(spawnChance).append(",\n");
        sb.append(INDENT).append(INDENT).append("\"despawnTimeMinutes\": ").append(despawnTimeMinutes).append(",\n");
        sb.append(INDENT).append(INDENT).append("\"minSpawnDistance\": ").append(minSpawnDistance).append(",\n");
        sb.append(INDENT).append(INDENT).append("\"maxSpawnDistance\": ").append(maxSpawnDistance).append(",\n");
        sb.append(INDENT).append(INDENT).append("\"disabledWorlds\": ");
        appendStringArrayInline(sb, sanitizeWorldNames(disabledWorlds));
        sb.append(",\n");
        sb.append(INDENT).append(INDENT).append("\"globalComets\": ").append(globalComets).append("\n");
        sb.append(INDENT).append("},\n\n");

        // Zone spawn chances section
        writeZoneSpawnChances(sb, zoneSpawnChances);

        // Zone base loot pools section
        writeZoneBaseLootPools(sb, zoneBaseLootPools);

        // Tier inheritance weights section
        writeTierInheritanceWeights(sb, tierInheritanceWeights);

        // WorldProtect comet spawn rules
        writeProtectedZoneSpawnRules(sb, protectedZoneRulesEnabled, defaultInProtectedRegion, regionOverrides);

        // Tier settings section
        sb.append(INDENT).append("\"tierSettings\": {\n");
        int tierCount = 0;
        for (Map.Entry<Integer, TierSettings> entry : tierSettings.entrySet()) {
            tierCount++;
            TierSettings ts = entry.getValue();
            sb.append(INDENT).append(INDENT).append("\"").append(entry.getKey()).append("\": {\n");
            sb.append(INDENT).append(INDENT).append(INDENT).append("\"timeoutSeconds\": ")
                    .append(ts.getTimeoutSeconds()).append(",\n");
            sb.append(INDENT).append(INDENT).append(INDENT).append("\"minRadius\": ").append(ts.getMinRadius())
                    .append(",\n");
            sb.append(INDENT).append(INDENT).append(INDENT).append("\"maxRadius\": ").append(ts.getMaxRadius())
                    .append("\n");
            sb.append(INDENT).append(INDENT).append("}");
            if (tierCount < tierSettings.size()) {
                sb.append(",");
            }
            sb.append("\n");
        }
        sb.append(INDENT).append("},\n\n");

        // Reward settings section
        sb.append(INDENT).append("\"rewardSettings\": {\n");
        int rewardCount = 0;
        for (Map.Entry<Integer, TierRewards> entry : rewardSettings.entrySet()) {
            rewardCount++;
            TierRewards tr = entry.getValue();
            sb.append(INDENT).append(INDENT).append("\"").append(entry.getKey()).append("\": {\n");

            // Drops
            sb.append(INDENT).append(INDENT).append(INDENT).append("\"drops\": [\n");
            List<RewardEntry> drops = tr.getDrops();
            for (int i = 0; i < drops.size(); i++) {
                RewardEntry drop = drops.get(i);
                sb.append(INDENT).append(INDENT).append(INDENT).append(INDENT).append("{ \"id\": \"")
                        .append(drop.getId()).append("\", \"minCount\": ").append(drop.getMinCount())
                        .append(", \"maxCount\": ").append(drop.getMaxCount()).append(", \"chance\": ")
                        .append(drop.getChance()).append(", \"displayName\": \"")
                        .append(escapeString(drop.getDisplayName())).append("\" }");
                if (i < drops.size() - 1)
                    sb.append(",");
                sb.append("\n");
            }
            sb.append(INDENT).append(INDENT).append(INDENT).append("],\n");

            // Bonus Drops
            sb.append(INDENT).append(INDENT).append(INDENT).append("\"bonusDrops\": [\n");
            List<RewardEntry> bonusDrops = tr.getBonusDrops();
            for (int i = 0; i < bonusDrops.size(); i++) {
                RewardEntry bonus = bonusDrops.get(i);
                sb.append(INDENT).append(INDENT).append(INDENT).append(INDENT).append("{ \"id\": \"")
                        .append(bonus.getId()).append("\", \"minCount\": ").append(bonus.getMinCount())
                        .append(", \"maxCount\": ").append(bonus.getMaxCount()).append(", \"chance\": ")
                        .append(bonus.getChance()).append(", \"displayName\": \"")
                        .append(escapeString(bonus.getDisplayName())).append("\" }");
                if (i < bonusDrops.size() - 1)
                    sb.append(",");
                sb.append("\n");
            }
            sb.append(INDENT).append(INDENT).append(INDENT).append("]\n");

            sb.append(INDENT).append(INDENT).append("}");
            if (rewardCount < rewardSettings.size()) {
                sb.append(",");
            }
            sb.append("\n");
        }
        sb.append(INDENT).append("}\n");

        sb.append("}\n");
        return sb.toString();
    }

    /**
     * Write a single theme to the StringBuilder
     */
    private static void writeTheme(StringBuilder sb, String id, ThemeConfig theme, boolean hasMore) {
        String i2 = INDENT + INDENT;
        String i3 = INDENT + INDENT + INDENT;
        String i4 = INDENT + INDENT + INDENT + INDENT;

        sb.append(i2).append("\"").append(id).append("\": {\n");
        sb.append(i3).append("\"displayName\": \"").append(escapeString(theme.getDisplayName())).append("\",\n");

        // Write tiers array
        sb.append(i3).append("\"tiers\": [");
        List<Integer> tiers = theme.getTiers();
        for (int i = 0; i < tiers.size(); i++) {
            sb.append(tiers.get(i));
            if (i < tiers.size() - 1)
                sb.append(", ");
        }
        sb.append("],\n");

        // Write mobs array
        // Write mobs array
        sb.append(i3).append("\"mobs\": [\n");
        List<MobEntry> mobs = theme.getMobs();
        for (int i = 0; i < mobs.size(); i++) {
            MobEntry mob = mobs.get(i);
            sb.append(i4).append("{ \"id\": \"").append(escapeString(mob.getId())).append("\", ");
            sb.append("\"count\": ").append(mob.getCount());

            sb.append(" }");
            if (i < mobs.size() - 1)
                sb.append(",");
            sb.append("\n");
        }
        sb.append(i3).append("],\n");

        // Write bosses array
        sb.append(i3).append("\"bosses\": [\n");
        List<BossEntry> bosses = theme.getBosses();
        for (int i = 0; i < bosses.size(); i++) {
            BossEntry boss = bosses.get(i);
            sb.append(i4).append("{ \"id\": \"").append(escapeString(boss.getId())).append("\"");

            sb.append(" }");
            if (i < bosses.size() - 1)
                sb.append(",");
            sb.append("\n");
        }
        sb.append(i3).append("]");

        sb.append("\n").append(i2).append("}");
        if (hasMore)
            sb.append(",");
        sb.append("\n");
    }

    /**
     * Write zone spawn chances section to JSON
     */
    private static void writeZoneSpawnChances(StringBuilder sb, Map<String, ZoneSpawnChances> zoneSpawnChances) {
        // Use defaults if none provided
        if (zoneSpawnChances == null || zoneSpawnChances.isEmpty()) {
            zoneSpawnChances = ZoneSpawnChances.generateDefaults();
        }

        sb.append(INDENT).append("\"zoneSpawnChances\": {\n");
        int zoneCount = 0;
        for (Map.Entry<String, ZoneSpawnChances> entry : zoneSpawnChances.entrySet()) {
            zoneCount++;
            String zoneKey = entry.getKey();
            ZoneSpawnChances chances = entry.getValue();

            sb.append(INDENT).append(INDENT).append("\"").append(zoneKey).append("\": { ");
            sb.append("\"tier1\": ").append(chances.getTier1()).append(", ");
            sb.append("\"tier2\": ").append(chances.getTier2()).append(", ");
            sb.append("\"tier3\": ").append(chances.getTier3()).append(", ");
            sb.append("\"tier4\": ").append(chances.getTier4()).append(", ");
            sb.append("\"tier5\": ").append(chances.getTier5());
            sb.append(" }");

            if (zoneCount < zoneSpawnChances.size()) {
                sb.append(",");
            }
            sb.append("\n");
        }
        sb.append(INDENT).append("},\n\n");
    }

    /**
     * Write per-zone base loot pools section to JSON.
     */
    private static void writeZoneBaseLootPools(StringBuilder sb, Map<String, TierRewards> zoneBaseLootPools) {
        if (zoneBaseLootPools == null) {
            zoneBaseLootPools = java.util.Collections.emptyMap();
        }

        sb.append(INDENT).append("\"zoneBaseLootPools\": {\n");
        int count = 0;
        int size = zoneBaseLootPools.size();
        for (Map.Entry<String, TierRewards> entry : zoneBaseLootPools.entrySet()) {
            count++;
            String zoneKey = entry.getKey();
            TierRewards rewards = entry.getValue();

            sb.append(INDENT).append(INDENT).append("\"").append(escapeString(zoneKey)).append("\": {\n");
            writeTierRewardsBody(sb, rewards, 3);
            sb.append("\n");
            sb.append(INDENT).append(INDENT).append("}");
            if (count < size) {
                sb.append(",");
            }
            sb.append("\n");
        }
        sb.append(INDENT).append("},\n\n");
    }

    private static void writeTierRewardsBody(StringBuilder sb, TierRewards rewards, int indentLevel) {
        String ind = INDENT.repeat(indentLevel);
        String ind2 = INDENT.repeat(indentLevel + 1);
        List<RewardEntry> drops = (rewards != null) ? rewards.getDrops() : java.util.Collections.emptyList();
        List<RewardEntry> bonusDrops = (rewards != null) ? rewards.getBonusDrops() : java.util.Collections.emptyList();

        sb.append(ind).append("\"drops\": [\n");
        for (int i = 0; i < drops.size(); i++) {
            RewardEntry drop = drops.get(i);
            sb.append(ind2).append("{ \"id\": \"")
                    .append(drop.getId()).append("\", \"minCount\": ").append(drop.getMinCount())
                    .append(", \"maxCount\": ").append(drop.getMaxCount()).append(", \"chance\": ")
                    .append(drop.getChance()).append(", \"displayName\": \"")
                    .append(escapeString(drop.getDisplayName())).append("\" }");
            if (i < drops.size() - 1) {
                sb.append(",");
            }
            sb.append("\n");
        }
        sb.append(ind).append("],\n");

        sb.append(ind).append("\"bonusDrops\": [\n");
        for (int i = 0; i < bonusDrops.size(); i++) {
            RewardEntry bonus = bonusDrops.get(i);
            sb.append(ind2).append("{ \"id\": \"")
                    .append(bonus.getId()).append("\", \"minCount\": ").append(bonus.getMinCount())
                    .append(", \"maxCount\": ").append(bonus.getMaxCount()).append(", \"chance\": ")
                    .append(bonus.getChance()).append(", \"displayName\": \"")
                    .append(escapeString(bonus.getDisplayName())).append("\" }");
            if (i < bonusDrops.size() - 1) {
                sb.append(",");
            }
            sb.append("\n");
        }
        sb.append(ind).append("]");
    }

    /**
     * Write per-tier inheritance chances section to JSON.
     */
    private static void writeTierInheritanceWeights(StringBuilder sb,
            Map<Integer, TierInheritanceWeights> tierInheritanceWeights) {
        if (tierInheritanceWeights == null) {
            tierInheritanceWeights = java.util.Collections.emptyMap();
        }

        sb.append(INDENT).append("\"tierInheritanceWeights\": {\n");
        int count = 0;
        int size = tierInheritanceWeights.size();
        for (Map.Entry<Integer, TierInheritanceWeights> entry : tierInheritanceWeights.entrySet()) {
            count++;
            Integer tier = entry.getKey();
            TierInheritanceWeights w = entry.getValue();
            if (w == null) {
                w = new TierInheritanceWeights();
                w.setChanceForTier(tier, 1.0);
            }

            sb.append(INDENT).append(INDENT).append("\"").append(tier).append("\": { ");
            sb.append("\"tier1\": ").append(w.getTier1Chance()).append(", ");
            sb.append("\"tier2\": ").append(w.getTier2Chance()).append(", ");
            sb.append("\"tier3\": ").append(w.getTier3Chance()).append(", ");
            sb.append("\"tier4\": ").append(w.getTier4Chance()).append(", ");
            sb.append("\"tier5\": ").append(w.getTier5Chance()).append(" }");

            if (count < size) {
                sb.append(",");
            }
            sb.append("\n");
        }
        sb.append(INDENT).append("},\n\n");
    }

    /**
     * Write optional WorldProtect rules for comet spawning inside protected regions.
     */
    private static void writeProtectedZoneSpawnRules(StringBuilder sb, boolean enabled, boolean defaultInProtectedRegion,
            Map<String, Boolean> regionOverrides) {
        sb.append(INDENT).append("\"worldProtectSpawnRules\": {\n");
        sb.append(INDENT).append(INDENT).append("\"enabled\": ").append(enabled).append(",\n");
        sb.append(INDENT).append(INDENT).append("\"defaultInWorldProtectRegion\": ")
                .append(defaultInProtectedRegion).append(",\n");
        sb.append(INDENT).append(INDENT).append("\"regionOverrides\": {\n");

        int count = 0;
        int size = (regionOverrides == null) ? 0 : regionOverrides.size();
        if (regionOverrides != null) {
            for (Map.Entry<String, Boolean> entry : regionOverrides.entrySet()) {
                count++;
                sb.append(INDENT).append(INDENT).append(INDENT)
                        .append("\"").append(escapeString(entry.getKey())).append("\": ")
                        .append(entry.getValue());
                if (count < size) {
                    sb.append(",");
                }
                sb.append("\n");
            }
        }

        sb.append(INDENT).append(INDENT).append("}\n");
        sb.append(INDENT).append("},\n\n");
    }

    private static List<String> sanitizeWorldNames(List<String> worldNames) {
        if (worldNames == null || worldNames.isEmpty()) {
            return java.util.Collections.emptyList();
        }

        List<String> sanitized = new java.util.ArrayList<>();
        for (String worldName : worldNames) {
            if (worldName == null) {
                continue;
            }
            String trimmed = worldName.trim();
            if (trimmed.isEmpty() || sanitized.contains(trimmed)) {
                continue;
            }
            sanitized.add(trimmed);
        }
        return sanitized;
    }

    private static void appendStringArrayInline(StringBuilder sb, List<String> values) {
        sb.append("[");
        for (int i = 0; i < values.size(); i++) {
            sb.append("\"").append(escapeString(values.get(i))).append("\"");
            if (i < values.size() - 1) {
                sb.append(", ");
            }
        }
        sb.append("]");
    }

    /**
     * Escape special characters in JSON strings
     */
    private static String escapeString(String s) {
        if (s == null)
            return "";
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }
}
