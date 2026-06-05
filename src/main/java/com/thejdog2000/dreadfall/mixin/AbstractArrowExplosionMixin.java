package com.thejdog2000.dreadfall.mixin;

import com.thejdog2000.dreadfall.DreadfallMod;
import com.thejdog2000.dreadfall.config.DreadfallConfigManager;
import com.thejdog2000.dreadfall.config.MobRuntimeConfig;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.monster.skeleton.AbstractSkeleton;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.HitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Projectile.class)
public abstract class AbstractArrowExplosionMixin {
    @Inject(method = "onHit", at = @At("TAIL"))
    private void dreadfall$explodeSkeletonArrow(HitResult hitResult, CallbackInfo callbackInfo) {
        if (!((Object) this instanceof AbstractArrow arrow)) {
            return;
        }
        Entity owner = arrow.getOwner();
        if (!(owner instanceof AbstractSkeleton)) {
            return;
        }

        DreadfallConfigManager configManager = DreadfallMod.getConfigManager();
        if (configManager == null) {
            return;
        }

        String ownerId = BuiltInRegistries.ENTITY_TYPE.getKey(owner.getType()).toString();
        configManager.getMobConfig(ownerId)
                .filter(MobRuntimeConfig::enabled)
                .map(config -> config.projectiles().explosiveArrows())
                .filter(MobRuntimeConfig.ExplosiveArrowSettings::enabled)
                .filter(settings -> settings.power() > 0.0)
                .ifPresent(settings -> explodeArrow(arrow, owner, ownerId, settings, configManager));
    }

    private void explodeArrow(AbstractArrow arrow, Entity owner, String ownerId, MobRuntimeConfig.ExplosiveArrowSettings settings, DreadfallConfigManager configManager) {
        Level level = arrow.level();
        if (!(level instanceof ServerLevel)) {
            return;
        }

        Level.ExplosionInteraction interaction = settings.damagesBlocks()
                ? Level.ExplosionInteraction.MOB
                : Level.ExplosionInteraction.NONE;
        level.explode(owner, arrow.getX(), arrow.getY(), arrow.getZ(), (float) settings.power(), settings.causesFire(), interaction);
        arrow.discard();

        if (configManager.isDebugLoggingEnabled()) {
            DreadfallMod.LOGGER.info("Exploded skeleton arrow owner={} power={} fire={} block_damage={} at {},{},{}",
                    ownerId, settings.power(), settings.causesFire(), settings.damagesBlocks(),
                    arrow.getBlockX(), arrow.getBlockY(), arrow.getBlockZ());
        }
    }
}
