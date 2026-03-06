package com.cometmod.wave;

import com.cometmod.wave.CometTier;

/**
 * Wave run constants and helpers (timeouts, etc.). Split from CometWaveManager so wave-run
 * logic can evolve here (countdown, spawn, completion) while CometWaveManager keeps
 * registration and comet state.
 */
public final class CometWaveRunner {

    private CometWaveRunner() {}

    public static final long TIER1_TIMEOUT_MS = 90_000L;   // Uncommon
    public static final long TIER2_TIMEOUT_MS = 150_000L;  // Rare
    public static final long TIER3_TIMEOUT_MS = 180_000L;  // Epic
    public static final long TIER4_TIMEOUT_MS = 240_000L;  // Legendary
    public static final long TIER5_TIMEOUT_MS = 300_000L;  // Mythic

    public static long getTierTimeoutMs(CometTier tier) {
        if (tier == null) return TIER1_TIMEOUT_MS;
        switch (tier) {
            case UNCOMMON: return TIER1_TIMEOUT_MS;
            case RARE: return TIER2_TIMEOUT_MS;
            case EPIC: return TIER3_TIMEOUT_MS;
            case LEGENDARY: return TIER4_TIMEOUT_MS;
            case MYTHIC: return TIER5_TIMEOUT_MS;
            default: return TIER1_TIMEOUT_MS;
        }
    }
}
