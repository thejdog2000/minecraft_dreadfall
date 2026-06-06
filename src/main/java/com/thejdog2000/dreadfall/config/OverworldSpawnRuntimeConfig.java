package com.thejdog2000.dreadfall.config;

public record OverworldSpawnRuntimeConfig(
        boolean enabled,
        SpawnProfile daytime,
        SpawnProfile nighttime
) {
    public SpawnProfile profileForClockTime(long clockTime) {
        long dayTime = Math.floorMod(clockTime, 24_000L);
        return dayTime >= 12_000L && dayTime < 23_000L ? nighttime : daytime;
    }

    public record SpawnProfile(
            String name,
            double spawnPacingMultiplier,
            int globalSpawnCap,
            int perPlayerMobCap,
            int pulseIntervalTicks,
            int spawnAttemptsPerPlayer,
            int minSpawnRadius,
            int maxSpawnRadius
    ) {
    }
}
