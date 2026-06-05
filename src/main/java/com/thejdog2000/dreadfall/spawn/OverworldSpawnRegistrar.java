package com.thejdog2000.dreadfall.spawn;

import com.thejdog2000.dreadfall.DreadfallMod;
import com.thejdog2000.dreadfall.config.DreadfallConfigManager;
import com.thejdog2000.dreadfall.config.OverworldMobSpawnConfig;
import net.fabricmc.fabric.api.biome.v1.BiomeModifications;
import net.fabricmc.fabric.api.biome.v1.BiomeSelectionContext;
import net.fabricmc.fabric.api.biome.v1.BiomeSelectors;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.biome.Biome;

import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public final class OverworldSpawnRegistrar {
    private OverworldSpawnRegistrar() {
    }

    public static void register(DreadfallConfigManager configManager) {
        for (OverworldMobSpawnConfig spawnConfig : configManager.getOverworldSpawns()) {
            if (!spawnConfig.enabled() || spawnConfig.weight() <= 0) {
                continue;
            }

            Optional<EntityType<?>> entityType = BuiltInRegistries.ENTITY_TYPE.getOptional(Identifier.parse(spawnConfig.mobId()));
            if (entityType.isEmpty()) {
                DreadfallMod.LOGGER.warn("Skipping overworld spawn for unknown entity type {}.", spawnConfig.mobId());
                continue;
            }

            Predicate<BiomeSelectionContext> selector = BiomeSelectors.foundInOverworld()
                    .and(createBiomeFilter(spawnConfig));
            BiomeModifications.addSpawn(
                    selector,
                    entityType.get().getCategory(),
                    entityType.get(),
                    spawnConfig.weight(),
                    spawnConfig.minGroupSize(),
                    spawnConfig.maxGroupSize()
            );
            DreadfallMod.LOGGER.info("Registered overworld spawn for {} with weight {}.", spawnConfig.mobId(), spawnConfig.weight());
        }
    }

    private static Predicate<BiomeSelectionContext> createBiomeFilter(OverworldMobSpawnConfig spawnConfig) {
        Set<ResourceKey<Biome>> allowed = toBiomeKeys(spawnConfig.allowedBiomes());
        Set<ResourceKey<Biome>> denied = toBiomeKeys(spawnConfig.deniedBiomes());

        return context -> {
            ResourceKey<Biome> biomeKey = context.getBiomeKey();
            if (!allowed.isEmpty() && !allowed.contains(biomeKey)) {
                return false;
            }
            return !denied.contains(biomeKey);
        };
    }

    private static Set<ResourceKey<Biome>> toBiomeKeys(Iterable<String> biomeIds) {
        return java.util.stream.StreamSupport.stream(biomeIds.spliterator(), false)
                .map(Identifier::parse)
                .map(identifier -> ResourceKey.create(Registries.BIOME, identifier))
                .collect(Collectors.toUnmodifiableSet());
    }
}

