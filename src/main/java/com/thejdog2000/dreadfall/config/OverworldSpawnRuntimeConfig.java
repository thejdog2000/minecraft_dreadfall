package com.thejdog2000.dreadfall.config;

public record OverworldSpawnRuntimeConfig(
        boolean enabled,
        double spawnPacingMultiplier,
        int globalSpawnCap,
        int perPlayerMobCap,
        int pulseIntervalTicks,
        int spawnAttemptsPerPlayer,
        int minSpawnRadius,
        int maxSpawnRadius
) {
}
