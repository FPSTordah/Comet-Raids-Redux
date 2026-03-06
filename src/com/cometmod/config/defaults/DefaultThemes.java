package com.cometmod.config.defaults;

import com.cometmod.config.model.BossEntry;
import com.cometmod.config.model.MobEntry;
import com.cometmod.config.model.ThemeConfig;
import com.cometmod.config.model.TierSettings;
import com.cometmod.config.model.WaveEntry;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Generates default theme configurations based on the original hardcoded
 * values.
 * This is used when no config file exists or when themes section is missing.
 */
public class DefaultThemes {

    /**
     * Generate all default themes matching the original CometWaveManager hardcoded
     * values
     * 
     * @return Map of theme ID to ThemeConfig
     */
    public static Map<String, ThemeConfig> generateDefaults() {
        Map<String, ThemeConfig> themes = new LinkedHashMap<>();

        // Default spawnBlock: skeleton/ghoul/undead -> coffin (one default; edit in themes.json to another or remove)
        final String defaultCoffin = "Comet_Furniture_Village_Coffin";
        final String defaultVoidPortal = "Comet_VoidInvasion_Portal";

        // Tier 1 Native Themes (available at tiers 1, 2)
        themes.put("skeleton", createTheme("skeleton", "Skeleton Horde",
                Arrays.asList(1, 2),
                Arrays.asList(
                        new MobEntry("Skeleton_Soldier", 3),
                        new MobEntry("Skeleton_Archer", 1),
                        new MobEntry("Skeleton_Archmage", 1)),
                Arrays.asList(
                        new BossEntry("Bear_Polar"),
                        new BossEntry("Wolf_Black")),
                defaultCoffin));

        themes.put("goblin", createTheme("goblin", "Goblin Gang",
                Arrays.asList(1, 2),
                Arrays.asList(
                        new MobEntry("Goblin_Scrapper", 2),
                        new MobEntry("Goblin_Miner", 2),
                        new MobEntry("Goblin_Lobber", 1)),
                Arrays.asList(
                        new BossEntry("Bear_Polar"),
                        new BossEntry("Wolf_Black"))));

        themes.put("spider", createTheme("spider", "Spider Swarm",
                Arrays.asList(1, 2),
                Arrays.asList(
                        new MobEntry("Spider", 5)),
                Arrays.asList(
                        new BossEntry("Spider_Broodmother"))));

        // Tier 2 Native Themes (available at tiers 1, 2, 3)
        themes.put("trork", createTheme("trork", "Trork Warband",
                Arrays.asList(1, 2, 3),
                Arrays.asList(
                        new MobEntry("Trork_Warrior", 1),
                        new MobEntry("Trork_Hunter", 1),
                        new MobEntry("Trork_Mauler", 1),
                        new MobEntry("Trork_Shaman", 1),
                        new MobEntry("Trork_Brawler", 1)),
                Arrays.asList(
                        new BossEntry("Trork_Chieftain"))));

        themes.put("skeleton_sand", createTheme("skeleton_sand", "Sand Skeleton Legion",
                Arrays.asList(1, 2, 3),
                Arrays.asList(
                        new MobEntry("Skeleton_Sand_Archer", 1),
                        new MobEntry("Skeleton_Sand_Assassin", 1),
                        new MobEntry("Skeleton_Sand_Guard", 1),
                        new MobEntry("Skeleton_Sand_Mage", 1),
                        new MobEntry("Skeleton_Sand_Ranger", 1)),
                Arrays.asList(
                        new BossEntry("Bear_Grizzly"),
                        new BossEntry("Skeleton_Burnt_Alchemist")),
                defaultCoffin));

        themes.put("sabertooth", createTheme("sabertooth", "Sabertooth Pack",
                Arrays.asList(1, 2, 3),
                Arrays.asList(
                        new MobEntry("Tiger_Sabertooth", 4)),
                Arrays.asList(
                        new BossEntry("Bear_Grizzly"),
                        new BossEntry("Skeleton_Burnt_Alchemist"))));

        // Tier 3 Native Themes (available at tiers 2, 3, 4, 5)
        themes.put("outlander", createTheme("outlander", "Outlander Cult",
                Arrays.asList(2, 3, 4, 5),
                Arrays.asList(
                        new MobEntry("Outlander_Berserker", 2),
                        new MobEntry("Outlander_Cultist", 1),
                        new MobEntry("Outlander_Hunter", 1),
                        new MobEntry("Outlander_Stalker", 1),
                        new MobEntry("Outlander_Brute", 1)),
                Arrays.asList(
                        new BossEntry("Werewolf"),
                        new BossEntry("Yeti"))));

        themes.put("leopard", createTheme("leopard", "Snow Leopard Pride",
                Arrays.asList(2, 3, 4, 5),
                Arrays.asList(
                        new MobEntry("Leopard_Snow", 5)),
                Arrays.asList(
                        new BossEntry("Werewolf"))));

        // Tier 4 Native Themes (available at tiers 3, 4, 5)
        themes.put("toad", createTheme("toad", "Magma Toads",
                Arrays.asList(3, 4, 5),
                Arrays.asList(
                        new MobEntry("Toad_Rhino_Magma", 3)),
                Arrays.asList(
                        new BossEntry("Shadow_Knight"),
                        new BossEntry("Zombie_Aberrant"))));

        themes.put("skeleton_burnt", createTheme("skeleton_burnt", "Burnt Legion",
                Arrays.asList(3, 4, 5),
                Arrays.asList(
                        new MobEntry("Skeleton_Burnt_Archer", 1),
                        new MobEntry("Skeleton_Burnt_Gunner", 1),
                        new MobEntry("Skeleton_Burnt_Knight", 1),
                        new MobEntry("Skeleton_Burnt_Lancer", 2)),
                Arrays.asList(
                        new BossEntry("Skeleton_Burnt_Praetorian")),
                defaultCoffin));

        // Void - Special: available at tiers 1, 2, 3 (excluded from 4)
        themes.put("void", createTheme("void", "Voidspawn",
                Arrays.asList(1, 2, 3),
                Arrays.asList(
                        new MobEntry("Crawler_Void", 2),
                        new MobEntry("Spectre_Void", 2)),
                Arrays.asList(
                        new BossEntry("Spawn_Void")),
                defaultVoidPortal));

        // Legendary Themes (tier 4+)
        themes.put("ice", createTheme("ice", "Legendary Ice",
                Arrays.asList(3, 4, 5),
                Arrays.asList(
                        new MobEntry("Yeti", 1),
                        new MobEntry("Bear_Polar", 2),
                        new MobEntry("Golem_Crystal_Frost", 1),
                        new MobEntry("Leopard_Snow", 2)),
                Arrays.asList(
                        new BossEntry("Spirit_Frost"))));

        themes.put("lava", createTheme("lava", "Legendary Lava",
                Arrays.asList(3, 4, 5),
                Arrays.asList(
                        new MobEntry("Emberwulf", 1),
                        new MobEntry("Golem_Firesteel", 2),
                        new MobEntry("Spirit_Ember", 1)),
                Arrays.asList(
                        new BossEntry("Toad_Rhino_Magma"))));

        themes.put("earth", createTheme("earth", "Legendary Earth",
                Arrays.asList(3, 4, 5),
                Arrays.asList(
                        new MobEntry("Golem_Crystal_Earth", 1),
                        new MobEntry("Bear_Grizzly", 2),
                        new MobEntry("Hyena", 4)),
                Arrays.asList(
                        new BossEntry("Hedera"))));

        themes.put("undead_rare", createTheme("undead_rare", "Rare Undead",
                Arrays.asList(1, 2, 3),
                Arrays.asList(
                        new MobEntry("Pig_Undead", 2),
                        new MobEntry("Cow_Undead", 1),
                        new MobEntry("Chicken_Undead", 2)),
                Arrays.asList(
                        new BossEntry("Golem_Crystal_Thunder")),
                defaultCoffin));

        themes.put("undead_legendary", createTheme("undead_legendary", "Legendary Undead",
                Arrays.asList(3, 4, 5),
                Arrays.asList(
                        new MobEntry("Pig_Undead", 8),
                        new MobEntry("Cow_Undead", 4),
                        new MobEntry("Chicken_Undead", 6),
                        new MobEntry("Hound_Bleached", 3)),
                Arrays.asList(
                        new BossEntry("Wraith")),
                defaultCoffin));

        themes.put("zombie", createTheme("zombie", "Zombie Aberration",
                Arrays.asList(3, 4, 5),
                Arrays.asList(
                        new MobEntry("Zombie_Aberrant_Small", 5)),
                Arrays.asList(
                        new BossEntry("Zombie_Aberrant")),
                defaultCoffin));

        // Additional variety themes
        themes.put("frostbound_pack", createTheme("frostbound_pack", "Frostbound Pack",
                Arrays.asList(2, 3, 4, 5),
                Arrays.asList(
                        new MobEntry("Leopard_Snow", 2),
                        new MobEntry("Yeti", 1),
                        new MobEntry("Skeleton_Archer", 1),
                        new MobEntry("Hyena", 1)),
                Arrays.asList(
                        new BossEntry("Spirit_Frost"),
                        new BossEntry("Werewolf"))));

        themes.put("ashen_vanguard", createTheme("ashen_vanguard", "Ashen Vanguard",
                Arrays.asList(3, 4, 5),
                Arrays.asList(
                        new MobEntry("Skeleton_Burnt_Knight", 2),
                        new MobEntry("Skeleton_Burnt_Lancer", 2),
                        new MobEntry("Skeleton_Burnt_Gunner", 1),
                        new MobEntry("Emberwulf", 1)),
                Arrays.asList(
                        new BossEntry("Skeleton_Burnt_Praetorian"),
                        new BossEntry("Toad_Rhino_Magma"))));

        themes.put("dune_stalkers", createTheme("dune_stalkers", "Dune Stalkers",
                Arrays.asList(2, 3, 4),
                Arrays.asList(
                        new MobEntry("Skeleton_Sand_Assassin", 2),
                        new MobEntry("Skeleton_Sand_Ranger", 2),
                        new MobEntry("Skeleton_Sand_Guard", 1),
                        new MobEntry("Skeleton_Sand_Mage", 1),
                        new MobEntry("Tiger_Sabertooth", 1)),
                Arrays.asList(
                        new BossEntry("Bear_Grizzly"),
                        new BossEntry("Skeleton_Burnt_Alchemist"))));

        themes.put("void_reavers", createTheme("void_reavers", "Void Reavers",
                Arrays.asList(2, 3, 4),
                Arrays.asList(
                        new MobEntry("Crawler_Void", 3),
                        new MobEntry("Spectre_Void", 2),
                        new MobEntry("Eye_Void", 1),
                        new MobEntry("Skeleton_Archmage", 1)),
                Arrays.asList(
                        new BossEntry("Spawn_Void"),
                        new BossEntry("Wraith")),
                defaultVoidPortal));

        themes.put("plague_horde", createTheme("plague_horde", "Plague Horde",
                Arrays.asList(2, 3, 4, 5),
                Arrays.asList(
                        new MobEntry("Pig_Undead", 3),
                        new MobEntry("Chicken_Undead", 3),
                        new MobEntry("Cow_Undead", 2),
                        new MobEntry("Hound_Bleached", 2)),
                Arrays.asList(
                        new BossEntry("Wraith"),
                        new BossEntry("Golem_Crystal_Thunder_Comet")),
                defaultCoffin));

        themes.put("trork_siege", createTheme("trork_siege", "Trork Siege",
                Arrays.asList(2, 3, 4),
                Arrays.asList(
                        new MobEntry("Trork_Warrior", 2),
                        new MobEntry("Trork_Brawler", 2),
                        new MobEntry("Trork_Hunter", 1),
                        new MobEntry("Trork_Mauler", 1),
                        new MobEntry("Trork_Shaman", 1)),
                Arrays.asList(
                        new BossEntry("Trork_Chieftain"),
                        new BossEntry("Bear_Grizzly"))));

        themes.put("ember_hunters", createTheme("ember_hunters", "Ember Hunters",
                Arrays.asList(3, 4, 5),
                Arrays.asList(
                        new MobEntry("Emberwulf", 2),
                        new MobEntry("Spirit_Ember", 1),
                        new MobEntry("Golem_Firesteel", 1),
                        new MobEntry("Skeleton_Burnt_Archer", 1),
                        new MobEntry("Hyena", 2)),
                Arrays.asList(
                        new BossEntry("Toad_Rhino_Magma"),
                        new BossEntry("Zombie_Aberrant"))));

        themes.put("crystal_marauders", createTheme("crystal_marauders", "Crystal Marauders",
                Arrays.asList(3, 4, 5),
                Arrays.asList(
                        new MobEntry("Golem_Crystal_Earth", 1),
                        new MobEntry("Golem_Crystal_Frost", 1),
                        new MobEntry("Bear_Grizzly", 1),
                        new MobEntry("Hyena", 2),
                        new MobEntry("Outlander_Hunter", 1)),
                Arrays.asList(
                        new BossEntry("Hedera"),
                        new BossEntry("Spirit_Frost"))));

        themes.put("wraithborn_legion", createTheme("wraithborn_legion", "Wraithborn Legion",
                Arrays.asList(3, 4, 5),
                Arrays.asList(
                        new MobEntry("Skeleton_Archmage", 2),
                        new MobEntry("Spectre_Void", 2),
                        new MobEntry("Outlander_Cultist", 1),
                        new MobEntry("Outlander_Stalker", 1)),
                Arrays.asList(
                        new BossEntry("Wraith"),
                        new BossEntry("Outlander_Brute"))));

        themes.put("predator_clan", createTheme("predator_clan", "Predator Clan",
                Arrays.asList(1, 2, 3, 4),
                Arrays.asList(
                        new MobEntry("Tiger_Sabertooth", 2),
                        new MobEntry("Leopard_Snow", 2),
                        new MobEntry("Hyena", 2),
                        new MobEntry("Goblin_Lobber", 1)),
                Arrays.asList(
                        new BossEntry("Wolf_Black"),
                        new BossEntry("Bear_Polar"))));

        return themes;
    }

    private static ThemeConfig createTheme(String id, String displayName,
            List<Integer> tiers, List<MobEntry> mobs, List<BossEntry> bosses) {
        return createTheme(id, displayName, tiers, mobs, bosses, null);
    }

    private static ThemeConfig createTheme(String id, String displayName,
            List<Integer> tiers, List<MobEntry> mobs, List<BossEntry> bosses, String spawnBlock) {
        ThemeConfig theme = new ThemeConfig(id, displayName, tiers, mobs, bosses, true);
        List<WaveEntry> waves = new ArrayList<>();

        WaveEntry normalWave = new WaveEntry(WaveEntry.WaveType.NORMAL);
        normalWave.setMobs(mobs);
        waves.add(normalWave);

        WaveEntry bossWave = new WaveEntry(WaveEntry.WaveType.BOSS);
        bossWave.setBosses(bosses);
        waves.add(bossWave);

        theme.setWaves(waves);
        if (spawnBlock != null && !spawnBlock.isBlank()) {
            theme.setSpawnBlock(spawnBlock.trim());
        }
        return theme;
    }

    /**
     * Get default tier settings for all 5 tiers
     * 
     * @return Map of tier number (1-5) to TierSettings
     */
    public static Map<Integer, TierSettings> getDefaultTierSettings() {
        Map<Integer, TierSettings> settings = new LinkedHashMap<>();
        settings.put(1, TierSettings.TIER1_DEFAULTS);
        settings.put(2, TierSettings.TIER2_DEFAULTS);
        settings.put(3, TierSettings.TIER3_DEFAULTS);
        settings.put(4, TierSettings.TIER4_DEFAULTS);
        settings.put(5, TierSettings.TIER5_DEFAULTS);
        return settings;
    }
}
