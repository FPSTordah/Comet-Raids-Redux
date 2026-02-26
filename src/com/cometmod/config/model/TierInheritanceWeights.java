package com.cometmod.config.model;

/**
 * Per-comet-tier inclusion chances for source tier loot pools.
 *
 * Values are probabilities in [0.0, 1.0].
 * Example for current tier 3:
 * - tier3Chance = 1.0 (always include current tier pool)
 * - tier2Chance = 0.25 (occasionally include tier 2 pool)
 * - tier1Chance = 0.10 (rarely include tier 1 pool)
 */
public class TierInheritanceWeights {

    private double tier1Chance;
    private double tier2Chance;
    private double tier3Chance;
    private double tier4Chance;
    private double tier5Chance;

    public TierInheritanceWeights() {
    }

    public TierInheritanceWeights(double tier1Chance, double tier2Chance, double tier3Chance, double tier4Chance, double tier5Chance) {
        this.tier1Chance = clampChance(tier1Chance);
        this.tier2Chance = clampChance(tier2Chance);
        this.tier3Chance = clampChance(tier3Chance);
        this.tier4Chance = clampChance(tier4Chance);
        this.tier5Chance = clampChance(tier5Chance);
    }

    public double getTier1Chance() {
        return tier1Chance;
    }

    public void setTier1Chance(double tier1Chance) {
        this.tier1Chance = clampChance(tier1Chance);
    }

    public double getTier2Chance() {
        return tier2Chance;
    }

    public void setTier2Chance(double tier2Chance) {
        this.tier2Chance = clampChance(tier2Chance);
    }

    public double getTier3Chance() {
        return tier3Chance;
    }

    public void setTier3Chance(double tier3Chance) {
        this.tier3Chance = clampChance(tier3Chance);
    }

    public double getTier4Chance() {
        return tier4Chance;
    }

    public void setTier4Chance(double tier4Chance) {
        this.tier4Chance = clampChance(tier4Chance);
    }

    public double getTier5Chance() {
        return tier5Chance;
    }

    public void setTier5Chance(double tier5Chance) {
        this.tier5Chance = clampChance(tier5Chance);
    }

    public double getChanceForTier(int tier) {
        switch (tier) {
            case 1:
                return tier1Chance;
            case 2:
                return tier2Chance;
            case 3:
                return tier3Chance;
            case 4:
                return tier4Chance;
            case 5:
                return tier5Chance;
            default:
                return 0.0;
        }
    }

    public void setChanceForTier(int tier, double chance) {
        double clamped = clampChance(chance);
        switch (tier) {
            case 1:
                tier1Chance = clamped;
                break;
            case 2:
                tier2Chance = clamped;
                break;
            case 3:
                tier3Chance = clamped;
                break;
            case 4:
                tier4Chance = clamped;
                break;
            case 5:
                tier5Chance = clamped;
                break;
            default:
                break;
        }
    }

    /**
     * Ensure tiers above current tier are disabled.
     */
    public void clampToCurrentTier(int currentTier) {
        if (currentTier < 5) {
            tier5Chance = 0.0;
        }
        if (currentTier < 4) {
            tier4Chance = 0.0;
        }
        if (currentTier < 3) {
            tier3Chance = 0.0;
        }
        if (currentTier < 2) {
            tier2Chance = 0.0;
        }
        if (currentTier < 1) {
            tier1Chance = 0.0;
        }
    }

    private static double clampChance(double value) {
        if (Double.isNaN(value) || Double.isInfinite(value)) {
            return 0.0;
        }
        if (value < 0.0) {
            return 0.0;
        }
        if (value > 1.0) {
            return 1.0;
        }
        return value;
    }

    @Override
    public String toString() {
        return "TierInheritanceWeights{" +
                "tier1=" + tier1Chance +
                ", tier2=" + tier2Chance +
                ", tier3=" + tier3Chance +
                ", tier4=" + tier4Chance +
                ", tier5=" + tier5Chance +
                '}';
    }
}
