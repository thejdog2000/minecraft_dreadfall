package com.thejdog2000.dreadfall.config;

import java.util.List;

public record OverworldMobSpawnConfig(
        String mobId,
        boolean enabled,
        int weight,
        int minGroupSize,
        int maxGroupSize,
        List<String> allowedBiomes,
        List<String> deniedBiomes
) {
}

