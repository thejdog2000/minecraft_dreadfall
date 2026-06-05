package com.thejdog2000.dreadfall.mixin;

import net.minecraft.world.entity.monster.Creeper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(Creeper.class)
public interface CreeperAccessor {
    @Accessor("maxSwell")
    void dreadfall$setMaxSwell(int maxSwell);

    @Accessor("explosionRadius")
    void dreadfall$setExplosionRadius(int explosionRadius);
}

