package com.cometmod.config.model;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Random;

/**
 * Holds tier spawn probabilities for a specific zone.
 * Probabilities should add up to 1.0 (100%).
 *
 * Example:
 *   tier1: 0.8 (80% chance)
 *   tier2: 0.2 (20% chance)
 *   tier3: 0.0 (0% chance)
 *   tier4: 0.0 (0% chance)
 *   tier5: 0.0 (0% chance)
 */
public class ZoneSpawnChances {

    // Probability for each tier (0.0 to 1.0)
    private double tier1 = 0.0;
    private double tier2 = 0.0;
    private double tier3 = 0.0;
    private double tier4 = 0.0;
    private double tier5 = 0.0;

    public ZoneSpawnChances() {
    }

    public ZoneSpawnChances(double tier1, double tier2, double tier3, double tier4, double tier5) {
        this.tier1 = tier1;
        this.tier2 = tier2;
        this.tier3 = tier3;
        this.tier4 = tier4;
        this.tier5 = tier5;
    }

    // Getters and setters
    public double getTier1() { return tier1; }
    public void setTier1(double tier1) { this.tier1 = tier1; }

    public double getTier2() { return tier2; }
    public void setTier2(double tier2) { this.tier2 = tier2; }

    public double getTier3() { return tier3; }
    public void setTier3(double tier3) { this.tier3 = tier3; }

    public double getTier4() { return tier4; }
    public void setTier4(double tier4) { this.tier4 = tier4; }
    public double getTier5() { return tier5; }
    public void setTier5(double tier5) { this.tier5 = tier5; }

    /**
     * Get probability for a specific tier (1-5)
     */
    public double getProbability(int tier) {
        switch (tier) {
            case 1: return tier1;
            case 2: return tier2;
            case 3: return tier3;
            case 4: return tier4;
            case 5: return tier5;
            default: return 0.0;
        }
    }

    /**
     * Set probability for a specific tier (1-5)
     */
    public void setProbability(int tier, double probability) {
        switch (tier) {
            case 1: tier1 = probability; break;
            case 2: tier2 = probability; break;
            case 3: tier3 = probability; break;
            case 4: tier4 = probability; break;
            case 5: tier5 = probability; break;
        }
    }

    /**
     * Select a tier based on the configured probabilities.
     * Uses weighted random selection.
     *
     * @param random Random instance for selection
     * @return Selected tier (1-5)
     */
    public int selectTier(Random random) {
        double roll = random.nextDouble();
        double cumulative = 0.0;

        cumulative += tier1;
        if (roll < cumulative) return 1;

        cumulative += tier2;
        if (roll < cumulative) return 2;

        cumulative += tier3;
        if (roll < cumulative) return 3;

        cumulative += tier4;
        if (roll < cumulative) return 4;

        // Default to tier 5 if nothing else matched
        return 5;
    }

    /**
     * Get the total probability (should be 1.0 for valid config)
     */
    public double getTotalProbability() {
        return tier1 + tier2 + tier3 + tier4 + tier5;
    }

    /**
     * Check if probabilities are valid (sum to approximately 1.0)
     */
    public boolean isValid() {
        double total = getTotalProbability();
        return total >= 0.99 && total <= 1.01;
    }

    /**
     * Normalize probabilities to sum to 1.0
     */
    public void normalize() {
        double total = getTotalProbability();
        if (total > 0) {
            tier1 /= total;
            tier2 /= total;
            tier3 /= total;
            tier4 /= total;
            tier5 /= total;
        }
    }

    @Override
    public String toString() {
        return String.format("ZoneSpawnChances[tier1=%.0f%%, tier2=%.0f%%, tier3=%.0f%%, tier4=%.0f%%, tier5=%.0f%%]",
                tier1 * 100, tier2 * 100, tier3 * 100, tier4 * 100, tier5 * 100);
    }

    /**
     * Generate default zone spawn chances map (zones 0-3).
     */
    public static Map<String, ZoneSpawnChances> generateDefaults() {
        Map<String, ZoneSpawnChances> defaults = new LinkedHashMap<>();
        defaults.put("0", new ZoneSpawnChances(0.8, 0.1, 0.07, 0.03, 0.0));
        defaults.put("1", new ZoneSpawnChances(0.6, 0.2, 0.13, 0.07, 0.0));
        defaults.put("2", new ZoneSpawnChances(0.35, 0.3, 0.2, 0.1, 0.05));
        defaults.put("3", new ZoneSpawnChances(0.1, 0.25, 0.25, 0.2, 0.2));
        return defaults;
    }
}
