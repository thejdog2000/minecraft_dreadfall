package com.thejdog2000.dreadfall.mixin;

import com.thejdog2000.dreadfall.DreadfallMod;
import com.thejdog2000.dreadfall.config.DreadfallConfigManager;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.monster.zombie.Zombie;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Zombie.class)
public abstract class ZombieSunlightMixin {
    @Inject(method = "isSunSensitive", at = @At("HEAD"), cancellable = true)
    private void dreadfall$overrideSunSensitivity(CallbackInfoReturnable<Boolean> callbackInfo) {
        DreadfallConfigManager configManager = DreadfallMod.getConfigManager();
        if (configManager == null) {
            return;
        }

        Zombie zombie = (Zombie) (Object) this;
        String mobId = BuiltInRegistries.ENTITY_TYPE.getKey(zombie.getType()).toString();
        configManager.getMobConfig(mobId)
                .flatMap(config -> config.sunlight().burnsInDaylight())
                .ifPresent(burnsInDaylight -> {
                    if (!burnsInDaylight) {
                        callbackInfo.setReturnValue(false);
                    }
                });
    }
}

