package com.thejdog2000.dreadfall.config;

import java.util.List;

public record BlockPlaceRuntimeConfig(
        boolean enabled,
        int cooldownTicks,
        int maxAttemptsPerStuckEvent,
        List<String> allowedBlocks
) {
    public String primaryBlockId() {
        return allowedBlocks.isEmpty() ? "minecraft:dirt" : allowedBlocks.getFirst();
    }
}

