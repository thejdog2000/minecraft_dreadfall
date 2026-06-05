package com.thejdog2000.dreadfall.behavior;

import com.thejdog2000.dreadfall.DreadfallMod;
import com.thejdog2000.dreadfall.config.DreadfallConfigManager;
import com.thejdog2000.dreadfall.config.MobRuntimeConfig;
import com.thejdog2000.dreadfall.mixin.CreeperAccessor;
import com.thejdog2000.dreadfall.mixin.LargeFireballAccessor;
import net.fabricmc.fabric.api.event.lifecycle.v1.EntityLoadData;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.projectile.hurtingprojectile.LargeFireball;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.Map;
import java.util.Optional;

public final class MobSpawnApplier {
    private static final Map<String, EquipmentSlot> EQUIPMENT_SLOTS = Map.of(
            "main_hand", EquipmentSlot.MAINHAND,
            "off_hand", EquipmentSlot.OFFHAND,
            "helmet", EquipmentSlot.HEAD,
            "chestplate", EquipmentSlot.CHEST,
            "leggings", EquipmentSlot.LEGS,
            "boots", EquipmentSlot.FEET
    );

    private final DreadfallConfigManager configManager;

    private MobSpawnApplier(DreadfallConfigManager configManager) {
        this.configManager = configManager;
    }

    public static void register(DreadfallConfigManager configManager) {
        MobSpawnApplier applier = new MobSpawnApplier(configManager);
        ServerEntityEvents.ENTITY_LOAD.register((entity, level) -> {
            if (entity instanceof LargeFireball fireball) {
                applier.applyLargeFireball(fireball);
                return;
            }
            if (!(entity instanceof Mob mob)) {
                return;
            }
            if (entity instanceof EntityLoadData loadData && loadData.isLoadedFromDisk()) {
                return;
            }
            applier.apply(mob);
        });
    }

    private void applyLargeFireball(LargeFireball fireball) {
        Entity owner = fireball.getOwner();
        if (owner == null) {
            return;
        }

        String ownerId = BuiltInRegistries.ENTITY_TYPE.getKey(owner.getType()).toString();
        configManager.getMobConfig(ownerId)
                .filter(MobRuntimeConfig::enabled)
                .filter(config -> config.explosions().enabled())
                .flatMap(config -> config.explosions().fireballPowerMultiplier())
                .filter(multiplier -> multiplier > 0.0)
                .map(multiplier -> Math.max(1, (int) Math.round(multiplier)))
                .ifPresent(power -> {
                    ((LargeFireballAccessor) fireball).dreadfall$setExplosionPower(power);
                    if (configManager.isDebugLoggingEnabled()) {
                        DreadfallMod.LOGGER.info("Applied large fireball power={} owner={} entity_id={}", power, ownerId, fireball.getId());
                    }
                });
    }

    private void apply(Mob mob) {
        String mobId = BuiltInRegistries.ENTITY_TYPE.getKey(mob.getType()).toString();
        Optional<MobRuntimeConfig> optionalConfig = configManager.getMobConfig(mobId);
        if (optionalConfig.isEmpty()) {
            return;
        }

        MobRuntimeConfig config = optionalConfig.get();
        if (!config.enabled()) {
            return;
        }

        applyAttributes(mob, config);
        applyEquipment(mob, config);
        applyExplosions(mob, config);
        if (configManager.isDebugLoggingEnabled()) {
            DreadfallMod.LOGGER.info("Applied mob settings mob={} entity_id={} block_breaking={} block_placing={}",
                    mobId, mob.getId(), config.blockBreakingEnabled(), config.blockPlacingEnabled());
        }
    }

    private void applyAttributes(Mob mob, MobRuntimeConfig config) {
        MobRuntimeConfig.AttributeSettings attributes = config.attributes();
        attributes.maxHealth().ifPresent(maxHealth -> {
            setAttribute(mob, Attributes.MAX_HEALTH, maxHealth);
            mob.setHealth(maxHealth.floatValue());
        });
        attributes.movementSpeed().ifPresent(value -> setAttribute(mob, Attributes.MOVEMENT_SPEED, value));
        attributes.attackDamage().ifPresent(value -> setAttribute(mob, Attributes.ATTACK_DAMAGE, value));

        double followRange = attributes.followRange().orElse(0.0);
        if (config.aggro().enabled()) {
            followRange = Math.max(followRange, config.aggro().detectionRange().orElse(0.0));
        }
        if (followRange > 0.0) {
            setAttribute(mob, Attributes.FOLLOW_RANGE, followRange);
        }
    }

    private void setAttribute(Mob mob, net.minecraft.core.Holder<net.minecraft.world.entity.ai.attributes.Attribute> attribute, double value) {
        AttributeInstance instance = mob.getAttribute(attribute);
        if (instance != null) {
            instance.setBaseValue(value);
        }
    }

    private void applyEquipment(Mob mob, MobRuntimeConfig config) {
        for (Map.Entry<String, EquipmentSlot> slotEntry : EQUIPMENT_SLOTS.entrySet()) {
            config.equipmentFor(slotEntry.getKey()).ifPresent(equipment -> applyEquipmentSlot(mob, slotEntry.getValue(), equipment));
        }
    }

    private void applyEquipmentSlot(Mob mob, EquipmentSlot slot, MobRuntimeConfig.EquipmentSettings equipment) {
        if (mob.getRandom().nextDouble() > equipment.chance()) {
            return;
        }

        Identifier itemId = Identifier.tryParse(equipment.itemId());
        if (itemId == null) {
            DreadfallMod.LOGGER.warn("Skipping invalid configured item id {}.", equipment.itemId());
            return;
        }

        Optional<Item> item = BuiltInRegistries.ITEM.getOptional(itemId);
        if (item.isEmpty()) {
            DreadfallMod.LOGGER.warn("Skipping unknown configured item id {}.", equipment.itemId());
            return;
        }

        mob.setItemSlot(slot, new ItemStack(item.get()));
        mob.setDropChance(slot, equipment.dropChance());
        if (configManager.isDebugLoggingEnabled()) {
            DreadfallMod.LOGGER.info("Equipped mob entity_id={} slot={} item={} drop_chance={}",
                    mob.getId(), slot.getName(), equipment.itemId(), equipment.dropChance());
        }
    }

    private void applyExplosions(Mob mob, MobRuntimeConfig config) {
        if (!config.explosions().enabled()) {
            return;
        }

        if (mob instanceof Creeper) {
            CreeperAccessor accessor = (CreeperAccessor) mob;
            config.explosions().fuseTicks()
                    .filter(fuseTicks -> fuseTicks > 0)
                    .ifPresent(fuseTicks -> {
                        accessor.dreadfall$setMaxSwell(fuseTicks);
                        if (configManager.isDebugLoggingEnabled()) {
                            DreadfallMod.LOGGER.info("Applied creeper fuse_ticks={} entity_id={}", fuseTicks, mob.getId());
                        }
                    });
            config.explosions().radiusMultiplier()
                    .filter(multiplier -> multiplier > 0.0)
                    .map(multiplier -> Math.max(1, (int) Math.round(3.0 * multiplier)))
                    .ifPresent(radius -> {
                        accessor.dreadfall$setExplosionRadius(radius);
                        if (configManager.isDebugLoggingEnabled()) {
                            DreadfallMod.LOGGER.info("Applied creeper explosion_radius={} entity_id={}", radius, mob.getId());
                        }
                    });
        }
    }
}
