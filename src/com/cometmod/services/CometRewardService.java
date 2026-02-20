package com.cometmod.services;

import com.cometmod.*;
import com.cometmod.commands.*;
import com.cometmod.services.*;
import com.cometmod.spawn.*;
import com.cometmod.systems.*;
import com.cometmod.wave.*;


import com.cometmod.config.model.TierRewards;
import com.hypixel.hytale.server.core.inventory.ItemStack;

import java.util.List;
import java.util.Random;
import java.util.logging.Logger;

/**
 * Encapsulates reward table resolution and item generation.
 */
public class CometRewardService {

    public void generateTierRewards(
            CometTier tier,
            String themeId,
            Random random,
            List<ItemStack> allItems,
            List<String> droppedItemIds,
            Logger logger) {

        int tierNum = toTierNum(tier);
        TierRewards rewards = null;

        if (themeId != null && WaveThemeProvider.hasRewardOverride(themeId, tier)) {
            rewards = WaveThemeProvider.getRewardOverride(themeId, tier);
            if (rewards != null) {
                logger.info("Using theme reward override for '" + themeId + "' tier " + tierNum);
            }
        }

        if (rewards == null) {
            CometConfig config = CometConfig.getInstance();
            if (config != null) {
                rewards = config.getTierRewards(tierNum);
                logger.info("Using config-based rewards for tier " + tierNum);
            }
        }

        if (rewards == null) {
            rewards = TierRewards.getDefaultForTier(tierNum);
            logger.info("Using default rewards for tier " + tierNum + " (config not available)");
        }

        rewards.generateRewards(random, allItems, droppedItemIds);
        logger.info("Generated " + allItems.size() + " reward items for tier " + tier.getName());
    }

    private int toTierNum(CometTier tier) {
        switch (tier) {
            case UNCOMMON:
                return 1;
            case RARE:
                return 2;
            case EPIC:
                return 3;
            case LEGENDARY:
                return 4;
            default:
                return 1;
        }
    }
}
