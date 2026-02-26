package com.cometmod.config.model;

/**
 * Global tier-based stat scaling for comet mobs and bosses.
 *
 * Tier 1 uses vanilla stats (1.0x), then each tier increases by
 * percentPerTier.
 */
public class TierStatScalingConfig {

    private boolean enabled = true;
    private double percentPerTier = 5.0;
    private double zonePercentPerLevel = 2.0;
    private boolean applyHp = true;
    private boolean applyDamage = true;
    private boolean applySpeed = true;
    private boolean applyScale = false;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public double getPercentPerTier() {
        return percentPerTier;
    }

    public void setPercentPerTier(double percentPerTier) {
        this.percentPerTier = percentPerTier;
    }

    public double getZonePercentPerLevel() {
        return zonePercentPerLevel;
    }

    public void setZonePercentPerLevel(double zonePercentPerLevel) {
        this.zonePercentPerLevel = zonePercentPerLevel;
    }

    public boolean isApplyHp() {
        return applyHp;
    }

    public void setApplyHp(boolean applyHp) {
        this.applyHp = applyHp;
    }

    public boolean isApplyDamage() {
        return applyDamage;
    }

    public void setApplyDamage(boolean applyDamage) {
        this.applyDamage = applyDamage;
    }

    public boolean isApplySpeed() {
        return applySpeed;
    }

    public void setApplySpeed(boolean applySpeed) {
        this.applySpeed = applySpeed;
    }

    public boolean isApplyScale() {
        return applyScale;
    }

    public void setApplyScale(boolean applyScale) {
        this.applyScale = applyScale;
    }

    public float getTierMultiplier(int tier) {
        if (!enabled) {
            return 1.0f;
        }
        int clampedTier = Math.max(1, tier);
        float step = (float) (percentPerTier / 100.0);
        float multiplier = 1.0f + ((clampedTier - 1) * step);
        return Math.max(0.0f, multiplier);
    }

    public float getZoneMultiplier(int zoneLevel) {
        if (!enabled) {
            return 1.0f;
        }
        // Zone 1 has no bonus; bonuses start at zone 2.
        int clampedZoneLevel = Math.max(0, zoneLevel - 1);
        float step = (float) (zonePercentPerLevel / 100.0);
        float multiplier = 1.0f + (clampedZoneLevel * step);
        return Math.max(0.0f, multiplier);
    }

    /**
     * Returns multipliers in the order: hp, damage, scale, speed.
     */
    public float[] getMultipliersForTier(int tier) {
        float base = getTierMultiplier(tier);
        return new float[] {
                applyHp ? base : 1.0f,
                applyDamage ? base : 1.0f,
                applyScale ? base : 1.0f,
                applySpeed ? base : 1.0f
        };
    }

    /**
     * Returns multipliers in the order: hp, damage, scale, speed.
     * Combines tier and zone scaling.
     */
    public float[] getMultipliersForTierAndZone(int tier, int zoneLevel) {
        float base = getTierMultiplier(tier) * getZoneMultiplier(zoneLevel);
        return new float[] {
                applyHp ? base : 1.0f,
                applyDamage ? base : 1.0f,
                applyScale ? base : 1.0f,
                applySpeed ? base : 1.0f
        };
    }
}
