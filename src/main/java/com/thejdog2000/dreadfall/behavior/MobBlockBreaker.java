package com.thejdog2000.dreadfall.behavior;

import com.thejdog2000.dreadfall.config.BlockBreakRuntimeConfig;
import com.thejdog2000.dreadfall.config.DreadfallConfigManager;
import com.thejdog2000.dreadfall.config.MobRuntimeConfig;
import com.thejdog2000.dreadfall.DreadfallMod;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.block.state.BlockState;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

public final class MobBlockBreaker {
    private final DreadfallConfigManager configManager;
    private final Map<UUID, BreakState> breakStates = new HashMap<>();

    private MobBlockBreaker(DreadfallConfigManager configManager) {
        this.configManager = configManager;
    }

    public static void register(DreadfallConfigManager configManager) {
        MobBlockBreaker blockBreaker = new MobBlockBreaker(configManager);
        ServerTickEvents.END_LEVEL_TICK.register(blockBreaker::tickLevel);
    }

    private void tickLevel(ServerLevel level) {
        BlockBreakRuntimeConfig blockBreaking = configManager.getBlockBreaking();
        if (!blockBreaking.enabled()) {
            return;
        }

        long gameTime = level.getGameTime();
        for (Entity entity : level.getAllEntities()) {
            if (entity instanceof Mob mob) {
                tickMob(level, mob, blockBreaking, gameTime);
            }
        }
    }

    private void tickMob(ServerLevel level, Mob mob, BlockBreakRuntimeConfig blockBreaking, long gameTime) {
        String mobId = BuiltInRegistries.ENTITY_TYPE.getKey(mob.getType()).toString();
        Optional<MobRuntimeConfig> optionalConfig = configManager.getMobConfig(mobId);
        if (optionalConfig.isEmpty() || !optionalConfig.get().enabled() || !optionalConfig.get().blockBreakingEnabled()) {
            breakStates.remove(mob.getUUID());
            return;
        }

        LivingEntity target = mob.getTarget();
        if (!(target instanceof ServerPlayer)) {
            breakStates.remove(mob.getUUID());
            return;
        }

        BreakState state = breakStates.computeIfAbsent(mob.getUUID(), ignored -> new BreakState(gameTime, mob.distanceToSqr(target)));
        if (gameTime - state.lastCheckTick < blockBreaking.stuckCheckTicks()) {
            return;
        }

        double distance = mob.distanceToSqr(target);
        boolean notGettingCloser = distance + blockBreaking.notCloserDistanceEpsilon() >= state.lastDistanceToTarget;
        state.lastCheckTick = gameTime;
        state.lastDistanceToTarget = distance;

        if (!notGettingCloser) {
            state.clearProgress();
            if (configManager.isDebugLoggingEnabled()) {
                DreadfallMod.LOGGER.info("Block break skipped; mob is closing distance entity_id={} distance_sqr={}", mob.getId(), distance);
            }
            return;
        }

        Optional<BlockPos> candidate = findBreakCandidate(level, mob, target, blockBreaking);
        if (candidate.isEmpty()) {
            state.clearProgress();
            if (configManager.isDebugLoggingEnabled()) {
                DreadfallMod.LOGGER.info("Block break skipped; no eligible candidate entity_id={} mob={} target={}", mob.getId(), mobId, target.getUUID());
            }
            return;
        }

        BlockPos blockPos = candidate.get();
        String blockId = BuiltInRegistries.BLOCK.getKey(level.getBlockState(blockPos).getBlock()).toString();
        if (!blockPos.equals(state.activeBlock)) {
            state.activeBlock = blockPos;
            state.progressTicks = 0;
        }

        state.progressTicks += blockBreaking.stuckCheckTicks();
        int requiredTicks = blockBreaking.breakTicksFor(blockId);
        int progressStage = Math.min(9, Math.max(0, (int) Math.floor((state.progressTicks / (double) requiredTicks) * 10.0)));
        level.destroyBlockProgress(mob.getId(), blockPos, progressStage);
        level.playSound(null, blockPos, SoundEvents.ZOMBIE_ATTACK_WOODEN_DOOR, SoundSource.HOSTILE, 0.35F, randomPitch(0.8F, 0.25F));

        if (state.progressTicks >= requiredTicks) {
            level.destroyBlockProgress(mob.getId(), blockPos, -1);
            level.playSound(null, blockPos, SoundEvents.ZOMBIE_BREAK_WOODEN_DOOR, SoundSource.HOSTILE, 0.7F, randomPitch(0.85F, 0.2F));
            level.destroyBlock(blockPos, true, mob, 512);
            if (configManager.isDebugLoggingEnabled()) {
                DreadfallMod.LOGGER.info("Mob broke block entity_id={} mob={} block={} pos={} required_ticks={}", mob.getId(), mobId, blockId, blockPos, requiredTicks);
            }
            state.clearProgress();
        } else if (configManager.isDebugLoggingEnabled()) {
            DreadfallMod.LOGGER.info("Mob damaging block entity_id={} mob={} block={} pos={} progress_ticks={} required_ticks={}",
                    mob.getId(), mobId, blockId, blockPos, state.progressTicks, requiredTicks);
        }
    }

    private Optional<BlockPos> findBreakCandidate(ServerLevel level, Mob mob, LivingEntity target, BlockBreakRuntimeConfig blockBreaking) {
        BlockPos mobPos = mob.blockPosition();
        BlockPos targetPos = target.blockPosition();
        int dx = Integer.compare(targetPos.getX(), mobPos.getX());
        int dz = Integer.compare(targetPos.getZ(), mobPos.getZ());
        if (dx == 0 && dz == 0) {
            dx = mob.getDirection().getStepX();
            dz = mob.getDirection().getStepZ();
        }

        int dy = Integer.compare(targetPos.getY(), mobPos.getY());
        BlockPos[] candidates = {
                mobPos.offset(dx, 0, dz),
                mobPos.offset(dx, 1, dz),
                mobPos.offset(dx, dy, dz)
        };

        for (BlockPos candidate : candidates) {
            BlockState blockState = level.getBlockState(candidate);
            if (blockState.isAir()) {
                continue;
            }
            String blockId = BuiltInRegistries.BLOCK.getKey(blockState.getBlock()).toString();
            if (blockBreaking.canBreak(blockId)) {
                return Optional.of(candidate);
            }
        }

        return Optional.empty();
    }

    private float randomPitch(float base, float spread) {
        return base + ThreadLocalRandom.current().nextFloat() * spread;
    }

    private static final class BreakState {
        private long lastCheckTick;
        private double lastDistanceToTarget;
        private BlockPos activeBlock;
        private int progressTicks;

        private BreakState(long lastCheckTick, double lastDistanceToTarget) {
            this.lastCheckTick = lastCheckTick;
            this.lastDistanceToTarget = lastDistanceToTarget;
        }

        private void clearProgress() {
            activeBlock = null;
            progressTicks = 0;
        }
    }
}
