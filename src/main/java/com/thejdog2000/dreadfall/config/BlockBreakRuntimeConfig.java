package com.thejdog2000.dreadfall.config;

import java.util.Map;
import java.util.Set;

public record BlockBreakRuntimeConfig(
        boolean enabled,
        int stuckCheckTicks,
        double notCloserDistanceEpsilon,
        String defaultStrength,
        Map<String, Integer> strengthBreakTicks,
        Set<String> unbreakableBlocks,
        Map<String, String> blockOverrides
) {
    public int breakTicksFor(String blockId) {
        String strength = blockOverrides.getOrDefault(blockId, defaultStrength);
        return strengthBreakTicks.getOrDefault(strength, strengthBreakTicks.getOrDefault(defaultStrength, 120));
    }

    public boolean canBreak(String blockId) {
        return !unbreakableBlocks.contains(blockId);
    }
}

