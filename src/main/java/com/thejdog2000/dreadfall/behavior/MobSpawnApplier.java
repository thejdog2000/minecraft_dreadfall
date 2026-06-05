package com.thejdog2000.dreadfall.behavior;

import com.thejdog2000.dreadfall.DreadfallMod;
import com.thejdog2000.dreadfall.config.DreadfallConfigManager;
import com.thejdog2000.dreadfall.config.MobRuntimeConfig;
import net.fabricmc.fabric.api.event.lifecycle.v1.EntityLoadData;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
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
            if (!(entity instanceof Mob mob)) {
                return;
            }
            if (entity instanceof EntityLoadData loadData && loadData.isLoadedFromDisk()) {
                return;
            }
            applier.apply(mob);
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
    }
}
