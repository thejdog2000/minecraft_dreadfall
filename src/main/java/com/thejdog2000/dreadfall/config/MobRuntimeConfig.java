package com.thejdog2000.dreadfall.config;

import java.util.Map;
import java.util.Optional;

public record MobRuntimeConfig(
        String mobId,
        boolean enabled,
        AttributeSettings attributes,
        AggroSettings aggro,
        SunlightSettings sunlight,
        ExplosionSettings explosions,
        boolean blockBreakingEnabled,
        boolean blockPlacingEnabled,
        Map<String, EquipmentSettings> equipment
) {
    public Optional<EquipmentSettings> equipmentFor(String slot) {
        return Optional.ofNullable(equipment.get(slot));
    }

    public record AttributeSettings(
            Optional<Double> maxHealth,
            Optional<Double> movementSpeed,
            Optional<Double> attackDamage,
            Optional<Double> followRange
    ) {
    }

    public record AggroSettings(
            boolean enabled,
            Optional<Double> detectionRange
    ) {
    }

    public record SunlightSettings(
            Optional<Boolean> burnsInDaylight
    ) {
    }

    public record ExplosionSettings(
            boolean enabled,
            Optional<Double> radiusMultiplier,
            Optional<Integer> fuseTicks,
            Optional<Double> fireballPowerMultiplier
    ) {
    }

    public record EquipmentSettings(
            String itemId,
            double chance,
            float dropChance
    ) {
    }
}
