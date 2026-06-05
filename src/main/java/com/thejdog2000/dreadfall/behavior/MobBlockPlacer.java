package com.thejdog2000.dreadfall.behavior;

import com.thejdog2000.dreadfall.config.BlockBreakRuntimeConfig;
import com.thejdog2000.dreadfall.config.BlockPlaceRuntimeConfig;
import com.thejdog2000.dreadfall.config.DreadfallConfigManager;
import com.thejdog2000.dreadfall.config.MobRuntimeConfig;
import com.thejdog2000.dreadfall.DreadfallMod;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.block.Block;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public final class MobBlockPlacer {
    private final DreadfallConfigManager configManager;
    private final Map<UUID, PlaceState> placeStates = new HashMap<>();

    private MobBlockPlacer(DreadfallConfigManager configManager) {
        this.configManager = configManager;
    }

    public static void register(DreadfallConfigManager configManager) {
        MobBlockPlacer placer = new MobBlockPlacer(configManager);
        ServerTickEvents.END_LEVEL_TICK.register(placer::tickLevel);
    }

    private void tickLevel(ServerLevel level) {
        BlockPlaceRuntimeConfig blockPlacing = configManager.getBlockPlacing();
        if (!blockPlacing.enabled()) {
            return;
        }

        long gameTime = level.getGameTime();
        BlockBreakRuntimeConfig stuckSettings = configManager.getBlockBreaking();
        for (Entity entity : level.getAllEntities()) {
            if (entity instanceof Mob mob) {
                tickMob(level, mob, blockPlacing, stuckSettings, gameTime);
            }
        }
    }

    private void tickMob(ServerLevel level, Mob mob, BlockPlaceRuntimeConfig blockPlacing, BlockBreakRuntimeConfig stuckSettings, long gameTime) {
        String mobId = BuiltInRegistries.ENTITY_TYPE.getKey(mob.getType()).toString();
        Optional<MobRuntimeConfig> optionalConfig = configManager.getMobConfig(mobId);
        if (optionalConfig.isEmpty() || !optionalConfig.get().enabled() || !optionalConfig.get().blockPlacingEnabled()) {
            placeStates.remove(mob.getUUID());
            return;
        }

        LivingEntity target = mob.getTarget();
        if (!(target instanceof ServerPlayer)) {
            placeStates.remove(mob.getUUID());
            return;
        }

        PlaceState state = placeStates.computeIfAbsent(mob.getUUID(), ignored -> new PlaceState(gameTime, mob.distanceToSqr(target)));
        if (gameTime - state.lastCheckTick < stuckSettings.stuckCheckTicks()) {
            return;
        }

        double distance = mob.distanceToSqr(target);
        boolean notGettingCloser = distance + stuckSettings.notCloserDistanceEpsilon() >= state.lastDistanceToTarget;
        state.lastCheckTick = gameTime;
        state.lastDistanceToTarget = distance;

        if (!notGettingCloser) {
            state.attemptsInStuckEvent = 0;
            if (configManager.isDebugLoggingEnabled()) {
                DreadfallMod.LOGGER.info("Block place skipped; mob is closing distance entity_id={} distance_sqr={}", mob.getId(), distance);
            }
            return;
        }
        if (gameTime - state.lastPlaceTick < blockPlacing.cooldownTicks()) {
            return;
        }
        if (state.attemptsInStuckEvent >= blockPlacing.maxAttemptsPerStuckEvent()) {
            return;
        }

        Optional<BlockPos> candidate = findPlacementCandidate(level, mob, target);
        if (candidate.isEmpty()) {
            if (configManager.isDebugLoggingEnabled()) {
                DreadfallMod.LOGGER.info("Block place skipped; no eligible candidate entity_id={} mob={} target={}", mob.getId(), mobId, target.getUUID());
            }
            return;
        }

        Identifier blockId = Identifier.tryParse(blockPlacing.primaryBlockId());
        if (blockId == null) {
            return;
        }
        Optional<Block> block = BuiltInRegistries.BLOCK.getOptional(blockId);
        if (block.isEmpty()) {
            return;
        }

        if (level.setBlock(candidate.get(), block.get().defaultBlockState(), 3)) {
            state.lastPlaceTick = gameTime;
            state.attemptsInStuckEvent++;
            if (configManager.isDebugLoggingEnabled()) {
                DreadfallMod.LOGGER.info("Mob placed block entity_id={} mob={} block={} pos={} attempts={}/{}",
                        mob.getId(), mobId, blockPlacing.primaryBlockId(), candidate.get(), state.attemptsInStuckEvent, blockPlacing.maxAttemptsPerStuckEvent());
            }
        }
    }

    private Optional<BlockPos> findPlacementCandidate(ServerLevel level, Mob mob, LivingEntity target) {
        BlockPos mobPos = mob.blockPosition();
        BlockPos targetPos = target.blockPosition();
        int dx = Integer.compare(targetPos.getX(), mobPos.getX());
        int dz = Integer.compare(targetPos.getZ(), mobPos.getZ());
        if (dx == 0 && dz == 0) {
            dx = mob.getDirection().getStepX();
            dz = mob.getDirection().getStepZ();
        }

        BlockPos frontBelow = mobPos.offset(dx, -1, dz);
        if (level.getBlockState(frontBelow).isAir()) {
            return Optional.of(frontBelow);
        }

        if (targetPos.getY() > mobPos.getY()) {
            BlockPos front = mobPos.offset(dx, 0, dz);
            if (level.getBlockState(front).isAir()) {
                return Optional.of(front);
            }
        }

        BlockPos below = mobPos.offset(0, -1, 0);
        if (level.getBlockState(below).isAir()) {
            return Optional.of(below);
        }

        return Optional.empty();
    }

    private static final class PlaceState {
        private long lastCheckTick;
        private double lastDistanceToTarget;
        private long lastPlaceTick;
        private int attemptsInStuckEvent;

        private PlaceState(long lastCheckTick, double lastDistanceToTarget) {
            this.lastCheckTick = lastCheckTick;
            this.lastDistanceToTarget = lastDistanceToTarget;
        }
    }
}
