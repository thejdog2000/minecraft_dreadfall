package com.thejdog2000.dreadfall.spawn;

import com.thejdog2000.dreadfall.DreadfallMod;
import com.thejdog2000.dreadfall.behavior.MobSpawnApplier;
import com.thejdog2000.dreadfall.config.DreadfallConfigManager;
import com.thejdog2000.dreadfall.config.OverworldMobSpawnConfig;
import com.thejdog2000.dreadfall.config.OverworldSpawnRuntimeConfig;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;

public final class DreadfallSpawnDirector {
    private final DreadfallConfigManager configManager;

    private DreadfallSpawnDirector(DreadfallConfigManager configManager) {
        this.configManager = configManager;
    }

    public static void register(DreadfallConfigManager configManager) {
        DreadfallSpawnDirector director = new DreadfallSpawnDirector(configManager);
        ServerTickEvents.END_SERVER_TICK.register(director::tickServer);
    }

    private void tickServer(MinecraftServer server) {
        OverworldSpawnRuntimeConfig runtime = configManager.getOverworldSpawnRuntime();
        if (!runtime.enabled() || server.getTickCount() % runtime.pulseIntervalTicks() != 0) {
            return;
        }
        if (server.getWorldData().getDifficulty() == Difficulty.PEACEFUL) {
            return;
        }

        ServerLevel level = server.getLevel(Level.OVERWORLD);
        if (level == null || level.players().isEmpty()) {
            return;
        }

        int globalCount = countMonsters(level, null, 0);
        int globalCap = Math.max(1, (int) Math.round(runtime.globalSpawnCap() * runtime.spawnPacingMultiplier()));
        if (globalCount >= globalCap) {
            return;
        }

        for (ServerPlayer player : level.players()) {
            int perPlayerCap = Math.max(1, (int) Math.round(runtime.perPlayerMobCap() * runtime.spawnPacingMultiplier()));
            int nearbyCount = countMonsters(level, player, runtime.maxSpawnRadius() + 16);
            if (nearbyCount >= perPlayerCap) {
                continue;
            }

            int budget = Math.min(perPlayerCap - nearbyCount, globalCap - globalCount);
            int spawned = spawnPulse(level, player, runtime, budget);
            globalCount += spawned;
            if (spawned > 0 && configManager.isDebugLoggingEnabled()) {
                DreadfallMod.LOGGER.info("Dreadfall spawn pulse player={} spawned={} nearby_before={} global_now={}/{}",
                        player.getScoreboardName(), spawned, nearbyCount, globalCount, globalCap);
            }
            if (globalCount >= globalCap) {
                return;
            }
        }
    }

    private int spawnPulse(ServerLevel level, ServerPlayer player, OverworldSpawnRuntimeConfig runtime, int budget) {
        int spawned = 0;
        int attempts = Math.min(budget, runtime.spawnAttemptsPerPlayer());
        for (int attempt = 0; attempt < attempts; attempt++) {
            Optional<OverworldMobSpawnConfig> spawnConfig = chooseSpawn();
            if (spawnConfig.isEmpty()) {
                return spawned;
            }

            Optional<EntityType<?>> entityType = BuiltInRegistries.ENTITY_TYPE.getOptional(Identifier.parse(spawnConfig.get().mobId()));
            if (entityType.isEmpty()) {
                continue;
            }

            int groupSize = randomInt(spawnConfig.get().minGroupSize(), spawnConfig.get().maxGroupSize());
            for (int index = 0; index < groupSize && spawned < budget; index++) {
                if (trySpawn(level, player, runtime, spawnConfig.get(), entityType.get())) {
                    spawned++;
                }
            }
        }
        return spawned;
    }

    private Optional<OverworldMobSpawnConfig> chooseSpawn() {
        List<OverworldMobSpawnConfig> spawns = configManager.getOverworldSpawns().stream()
                .filter(OverworldMobSpawnConfig::enabled)
                .filter(config -> config.weight() > 0)
                .toList();
        int totalWeight = spawns.stream().mapToInt(OverworldMobSpawnConfig::weight).sum();
        if (totalWeight <= 0) {
            return Optional.empty();
        }

        int roll = ThreadLocalRandom.current().nextInt(totalWeight);
        for (OverworldMobSpawnConfig spawn : spawns) {
            roll -= spawn.weight();
            if (roll < 0) {
                return Optional.of(spawn);
            }
        }
        return Optional.empty();
    }

    private boolean trySpawn(ServerLevel level, ServerPlayer player, OverworldSpawnRuntimeConfig runtime, OverworldMobSpawnConfig spawnConfig, EntityType<?> entityType) {
        BlockPos position = choosePosition(level, player, runtime, spawnConfig, entityType);
        Entity entity = entityType.create(level, spawned -> {
        }, position, EntitySpawnReason.NATURAL, true, false);
        if (entity == null) {
            return false;
        }

        entity.snapTo(position.getX() + 0.5D, position.getY(), position.getZ() + 0.5D, ThreadLocalRandom.current().nextFloat() * 360.0F, 0.0F);
        if (entity instanceof Mob mob) {
            MobSpawnApplier.applyConfiguredSettings(configManager, mob);
        }
        return level.addFreshEntity(entity);
    }

    private BlockPos choosePosition(ServerLevel level, ServerPlayer player, OverworldSpawnRuntimeConfig runtime, OverworldMobSpawnConfig spawnConfig, EntityType<?> entityType) {
        ThreadLocalRandom random = ThreadLocalRandom.current();
        double angle = random.nextDouble(Math.PI * 2.0D);
        int radius = randomInt(runtime.minSpawnRadius(), runtime.maxSpawnRadius());
        int x = player.blockPosition().getX() + (int) Math.round(Math.cos(angle) * radius);
        int z = player.blockPosition().getZ() + (int) Math.round(Math.sin(angle) * radius);
        int surfaceY = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
        int y = surfaceY;

        if (entityType == EntityType.GHAST || entityType == EntityType.BLAZE) {
            y = Math.max(surfaceY + 8, player.blockPosition().getY() + random.nextInt(-4, 13));
        }

        y = Math.max(spawnConfig.minY(), Math.min(spawnConfig.maxY(), y));
        return new BlockPos(x, y, z);
    }

    private int countMonsters(ServerLevel level, ServerPlayer player, int radius) {
        AABB box = player == null
                ? new AABB(-30_000_000, level.getMinY(), -30_000_000, 30_000_000, level.getMaxY(), 30_000_000)
                : player.getBoundingBox().inflate(radius, 96.0D, radius);
        return level.getEntities((Entity) null, box, entity -> entity instanceof Mob && entity.getType().getCategory() == MobCategory.MONSTER).size();
    }

    private int randomInt(int minInclusive, int maxInclusive) {
        if (maxInclusive <= minInclusive) {
            return minInclusive;
        }
        return ThreadLocalRandom.current().nextInt(minInclusive, maxInclusive + 1);
    }
}
