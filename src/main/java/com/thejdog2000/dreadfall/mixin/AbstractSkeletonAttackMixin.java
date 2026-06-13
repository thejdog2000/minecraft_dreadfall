package com.thejdog2000.dreadfall.mixin;

import com.thejdog2000.dreadfall.DreadfallMod;
import com.thejdog2000.dreadfall.config.DreadfallConfigManager;
import com.thejdog2000.dreadfall.config.MobRuntimeConfig;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.monster.skeleton.AbstractSkeleton;
import net.minecraft.world.entity.projectile.Projectile;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Optional;

@Mixin(AbstractSkeleton.class)
public abstract class AbstractSkeletonAttackMixin {
    @Inject(method = "getAttackInterval", at = @At("HEAD"), cancellable = true)
    private void dreadfall$getAttackInterval(CallbackInfoReturnable<Integer> callbackInfo) {
        configuredExplosiveArrows()
                .filter(MobRuntimeConfig.ExplosiveArrowSettings::enabled)
                .flatMap(MobRuntimeConfig.ExplosiveArrowSettings::minAttackIntervalTicks)
                .ifPresent(callbackInfo::setReturnValue);
    }

    @Inject(method = "getHardAttackInterval", at = @At("HEAD"), cancellable = true)
    private void dreadfall$getHardAttackInterval(CallbackInfoReturnable<Integer> callbackInfo) {
        configuredExplosiveArrows()
                .filter(MobRuntimeConfig.ExplosiveArrowSettings::enabled)
                .flatMap(MobRuntimeConfig.ExplosiveArrowSettings::minAttackIntervalTicks)
                .ifPresent(callbackInfo::setReturnValue);
    }

    @ModifyArg(
            method = "performRangedAttack",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/entity/projectile/Projectile;spawnProjectileUsingShoot(Lnet/minecraft/world/entity/projectile/Projectile;Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/item/ItemStack;DDDFF)Lnet/minecraft/world/entity/projectile/Projectile;"
            ),
            index = 7
    )
    private float dreadfall$modifyArrowInaccuracy(float vanillaInaccuracy) {
        return configuredExplosiveArrows()
                .filter(MobRuntimeConfig.ExplosiveArrowSettings::enabled)
                .map(settings -> vanillaInaccuracy * settings.inaccuracyMultiplier())
                .orElse((double) vanillaInaccuracy)
                .floatValue();
    }

    private Optional<MobRuntimeConfig.ExplosiveArrowSettings> configuredExplosiveArrows() {
        DreadfallConfigManager configManager = DreadfallMod.getConfigManager();
        if (configManager == null) {
            return Optional.empty();
        }

        AbstractSkeleton skeleton = (AbstractSkeleton) (Object) this;
        String mobId = BuiltInRegistries.ENTITY_TYPE.getKey(skeleton.getType()).toString();
        return configManager.getMobConfig(mobId)
                .filter(MobRuntimeConfig::enabled)
                .map(config -> config.projectiles().explosiveArrows());
    }
}
