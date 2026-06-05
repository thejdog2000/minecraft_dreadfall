package com.thejdog2000.dreadfall.mixin;

import net.minecraft.world.entity.projectile.hurtingprojectile.LargeFireball;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(LargeFireball.class)
public interface LargeFireballAccessor {
    @Accessor("explosionPower")
    void dreadfall$setExplosionPower(int explosionPower);
}

