package com.thejdog2000.dreadfall.config;

import com.thejdog2000.dreadfall.DreadfallMod;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;

public final class DreadfallConfigManager {
    private static final List<String> CONFIG_FILES = List.of(
            "overworld_settings.yml",
            "nightmare_settings.yml",
            "mobs_settings.yml"
    );
    private static final Pattern IDENTIFIER_PATTERN = Pattern.compile("[a-z0-9_.-]+:[a-z0-9_./-]+");
    private static final Set<String> SUPPORTED_VANILLA_MOBS = Set.of(
            "minecraft:blaze",
            "minecraft:cave_spider",
            "minecraft:creeper",
            "minecraft:drowned",
            "minecraft:elder_guardian",
            "minecraft:enderman",
            "minecraft:endermite",
            "minecraft:evoker",
            "minecraft:ghast",
            "minecraft:guardian",
            "minecraft:hoglin",
            "minecraft:husk",
            "minecraft:magma_cube",
            "minecraft:phantom",
            "minecraft:piglin",
            "minecraft:piglin_brute",
            "minecraft:pillager",
            "minecraft:ravager",
            "minecraft:shulker",
            "minecraft:silverfish",
            "minecraft:skeleton",
            "minecraft:slime",
            "minecraft:spider",
            "minecraft:stray",
            "minecraft:vex",
            "minecraft:vindicator",
            "minecraft:warden",
            "minecraft:witch",
            "minecraft:wither_skeleton",
            "minecraft:zoglin",
            "minecraft:zombie",
            "minecraft:zombie_villager",
            "minecraft:zombified_piglin"
    );
    private static final Set<String> REQUIRED_STRENGTH_TIERS = Set.of(
            "very_weak",
            "weak",
            "normal",
            "strong",
            "very_strong"
    );
    private static final Set<String> REQUIRED_UNBREAKABLE_BLOCKS = Set.of(
            "minecraft:bedrock",
            "minecraft:obsidian"
    );

    private final Path configDirectory;
    private final Yaml yaml;
    private Instant lastLoadedAt;
    private Map<String, MobRuntimeConfig> mobConfigs = Map.of();
    private List<OverworldMobSpawnConfig> overworldSpawns = List.of();
    private OverworldSpawnRuntimeConfig overworldSpawnRuntime = inactiveOverworldSpawnRuntime();
    private BlockBreakRuntimeConfig blockBreaking = new BlockBreakRuntimeConfig(false, 40, 1.5, "normal", Map.of("normal", 120), Set.of(), Map.of());
    private BlockPlaceRuntimeConfig blockPlacing = new BlockPlaceRuntimeConfig(false, 60, 3, List.of("minecraft:dirt"));
    private boolean debugLoggingEnabled;

    public DreadfallConfigManager(Path configDirectory) {
        this.configDirectory = configDirectory;
        LoaderOptions loaderOptions = new LoaderOptions();
        loaderOptions.setAllowDuplicateKeys(false);

        DumperOptions dumperOptions = new DumperOptions();
        dumperOptions.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);

        this.yaml = new Yaml(loaderOptions, dumperOptions);
    }

    public void loadOrCreate() throws ConfigValidationException {
        try {
            Files.createDirectories(configDirectory);
            for (String configFile : CONFIG_FILES) {
                createDefaultIfMissing(configFile);
            }

            Map<String, Object> overworldSettings = loadYamlMap("overworld_settings.yml");
            Map<String, Object> nightmareSettings = loadYamlMap("nightmare_settings.yml");
            Map<String, Object> mobsSettings = loadYamlMap("mobs_settings.yml");

            validateOverworldSettings(overworldSettings);
            validateNightmareSettings(nightmareSettings);
            validateMobsSettings(mobsSettings);
            overworldSpawns = parseOverworldSpawns(overworldSettings);
            overworldSpawnRuntime = parseOverworldSpawnRuntime(overworldSettings);
            blockBreaking = parseBlockBreaking(mobsSettings);
            blockPlacing = parseBlockPlacing(mobsSettings);
            debugLoggingEnabled = parseDebugLogging(mobsSettings);
            mobConfigs = parseMobConfigs(mobsSettings);

            lastLoadedAt = Instant.now();
            DreadfallMod.LOGGER.info("Loaded Dreadfall configs from {}. mobs={}, overworld_spawns={}, block_breaking={}, block_placing={}, debug={}",
                    configDirectory, mobConfigs.size(), overworldSpawns.size(), blockBreaking.enabled(), blockPlacing.enabled(), debugLoggingEnabled);
        } catch (IOException exception) {
            throw new ConfigValidationException("Could not load Dreadfall configs: " + exception.getMessage(), exception);
        }
    }

    public Path getConfigDirectory() {
        return configDirectory;
    }

    public Instant getLastLoadedAt() {
        return lastLoadedAt;
    }

    public List<String> getConfigFiles() {
        return CONFIG_FILES;
    }

    public Optional<MobRuntimeConfig> getMobConfig(String mobId) {
        return Optional.ofNullable(mobConfigs.get(mobId));
    }

    public int getMobConfigCount() {
        return mobConfigs.size();
    }

    public List<OverworldMobSpawnConfig> getOverworldSpawns() {
        return overworldSpawns;
    }

    public OverworldSpawnRuntimeConfig getOverworldSpawnRuntime() {
        return overworldSpawnRuntime;
    }

    public BlockBreakRuntimeConfig getBlockBreaking() {
        return blockBreaking;
    }

    public BlockPlaceRuntimeConfig getBlockPlacing() {
        return blockPlacing;
    }

    public boolean isDebugLoggingEnabled() {
        return debugLoggingEnabled;
    }

    private void createDefaultIfMissing(String configFile) throws IOException {
        Path target = configDirectory.resolve(configFile);
        if (Files.exists(target)) {
            return;
        }

        String resourcePath = "/default_config/" + configFile;
        try (InputStream inputStream = DreadfallConfigManager.class.getResourceAsStream(resourcePath)) {
            if (inputStream == null) {
                throw new IOException("Missing bundled default config " + resourcePath);
            }
            Files.copy(inputStream, target);
        }
    }

    private Map<String, Object> loadYamlMap(String configFile) throws IOException, ConfigValidationException {
        Path configPath = configDirectory.resolve(configFile);
        try (InputStream inputStream = Files.newInputStream(configPath)) {
            Object loaded = yaml.load(inputStream);
            if (!(loaded instanceof Map<?, ?> map)) {
                throw new ConfigValidationException(configFile + " must contain a YAML object at the root.");
            }
            return castMap(configFile, map);
        }
    }

    private void validateOverworldSettings(Map<String, Object> root) throws ConfigValidationException {
        requireInteger(root, "version", "overworld_settings.yml");
        Map<String, Object> worlds = requireMap(root, "worlds", "overworld_settings.yml");
        for (Map.Entry<String, Object> worldEntry : worlds.entrySet()) {
            Map<String, Object> world = requireMapValue(worldEntry.getValue(), "overworld_settings.yml worlds." + worldEntry.getKey());
            Map<String, Object> activeSpawning = optionalMap(world, "active_spawning", "overworld_settings.yml worlds." + worldEntry.getKey());
            if (optionalInteger(activeSpawning, "pulse_interval_ticks").orElse(100) < 1) {
                throw new ConfigValidationException("active_spawning.pulse_interval_ticks must be at least 1.");
            }
            if (optionalInteger(activeSpawning, "spawn_attempts_per_player").orElse(6) < 1) {
                throw new ConfigValidationException("active_spawning.spawn_attempts_per_player must be at least 1.");
            }
            if (optionalInteger(activeSpawning, "per_player_mob_cap").orElse(30) < 1) {
                throw new ConfigValidationException("active_spawning.per_player_mob_cap must be at least 1.");
            }
            validateSpawnProfile(optionalMap(activeSpawning, "daytime", "overworld_settings.yml worlds." + worldEntry.getKey() + ".active_spawning"),
                    "active_spawning.daytime");
            validateSpawnProfile(optionalMap(activeSpawning, "nighttime", "overworld_settings.yml worlds." + worldEntry.getKey() + ".active_spawning"),
                    "active_spawning.nighttime");
            Map<String, Object> mobs = requireMap(world, "mobs", "overworld_settings.yml worlds." + worldEntry.getKey());
            for (String mobId : mobs.keySet()) {
                validateMobId(mobId, "overworld_settings.yml worlds." + worldEntry.getKey() + ".mobs");
                Map<String, Object> mob = requireMapValue(mobs.get(mobId), "overworld_settings.yml " + mobId);
                int minGroupSize = requireInteger(mob, "min_group_size", "overworld_settings.yml " + mobId);
                int maxGroupSize = requireInteger(mob, "max_group_size", "overworld_settings.yml " + mobId);
                int minY = requireInteger(mob, "min_y", "overworld_settings.yml " + mobId);
                int maxY = requireInteger(mob, "max_y", "overworld_settings.yml " + mobId);
                requireInteger(mob, "weight", "overworld_settings.yml " + mobId);
                if (minGroupSize < 1 || maxGroupSize < minGroupSize) {
                    throw new ConfigValidationException("Invalid group size for " + mobId + " in overworld_settings.yml.");
                }
                if (maxY < minY) {
                    throw new ConfigValidationException("Invalid y range for " + mobId + " in overworld_settings.yml.");
                }
                validateBiomeFilters(mob, "overworld_settings.yml " + mobId);
            }
        }
    }

    private void validateBiomeFilters(Map<String, Object> mob, String path) throws ConfigValidationException {
        Object biomesValue = mob.get("biomes");
        if (biomesValue == null) {
            return;
        }

        Map<String, Object> biomes = requireMapValue(biomesValue, path + ".biomes");
        for (String biomeId : optionalStringList(biomes, "allow")) {
            validateIdentifier(biomeId, path + ".biomes.allow");
        }
        for (String biomeId : optionalStringList(biomes, "deny")) {
            validateIdentifier(biomeId, path + ".biomes.deny");
        }
    }

    private List<OverworldMobSpawnConfig> parseOverworldSpawns(Map<String, Object> root) throws ConfigValidationException {
        if (!optionalBoolean(root, "enabled").orElse(true)) {
            return List.of();
        }

        Map<String, Object> worlds = requireMap(root, "worlds", "overworld_settings.yml");
        Map<String, Object> overworld = optionalMap(worlds, "overworld", "overworld_settings.yml.worlds");
        if (!optionalBoolean(overworld, "enabled").orElse(true)) {
            return List.of();
        }

        Map<String, Object> mobs = requireMap(overworld, "mobs", "overworld_settings.yml.worlds.overworld");
        java.util.ArrayList<OverworldMobSpawnConfig> parsed = new java.util.ArrayList<>();
        for (Map.Entry<String, Object> mobEntry : mobs.entrySet()) {
            String mobId = mobEntry.getKey();
            Map<String, Object> mob = requireMapValue(mobEntry.getValue(), "overworld_settings.yml " + mobId);
            Map<String, Object> biomes = optionalMap(mob, "biomes", "overworld_settings.yml " + mobId);
            parsed.add(new OverworldMobSpawnConfig(
                    mobId,
                    optionalBoolean(mob, "enabled").orElse(true),
                    requireInteger(mob, "weight", "overworld_settings.yml " + mobId),
                    requireInteger(mob, "min_group_size", "overworld_settings.yml " + mobId),
                    requireInteger(mob, "max_group_size", "overworld_settings.yml " + mobId),
                    requireInteger(mob, "min_y", "overworld_settings.yml " + mobId),
                    requireInteger(mob, "max_y", "overworld_settings.yml " + mobId),
                    optionalStringList(biomes, "allow"),
                    optionalStringList(biomes, "deny")
            ));
        }
        return List.copyOf(parsed);
    }

    private OverworldSpawnRuntimeConfig parseOverworldSpawnRuntime(Map<String, Object> root) throws ConfigValidationException {
        if (!optionalBoolean(root, "enabled").orElse(true)) {
            return inactiveOverworldSpawnRuntime();
        }

        Map<String, Object> worlds = requireMap(root, "worlds", "overworld_settings.yml");
        Map<String, Object> overworld = optionalMap(worlds, "overworld", "overworld_settings.yml.worlds");
        if (!optionalBoolean(overworld, "enabled").orElse(true)) {
            return inactiveOverworldSpawnRuntime();
        }

        Map<String, Object> activeSpawning = optionalMap(overworld, "active_spawning", "overworld_settings.yml.worlds.overworld");
        int minRadius = optionalInteger(activeSpawning, "min_spawn_radius").orElse(24);
        int maxRadius = optionalInteger(activeSpawning, "max_spawn_radius").orElse(56);
        if (minRadius < 1 || maxRadius < minRadius) {
            throw new ConfigValidationException("Invalid active_spawning radius range in overworld_settings.yml.");
        }

        return new OverworldSpawnRuntimeConfig(
                optionalBoolean(activeSpawning, "enabled").orElse(true),
                parseSpawnProfile("daytime", overworld, activeSpawning, minRadius, maxRadius, 0.25, 40, 6, 240, 2),
                parseSpawnProfile("nighttime", overworld, activeSpawning, minRadius, maxRadius, 2.5, 160, 45, 60, 12)
        );
    }

    private void validateSpawnProfile(Map<String, Object> profile, String path) throws ConfigValidationException {
        if (profile.isEmpty()) {
            return;
        }
        if (optionalInteger(profile, "pulse_interval_ticks").orElse(100) < 1) {
            throw new ConfigValidationException(path + ".pulse_interval_ticks must be at least 1.");
        }
        if (optionalInteger(profile, "spawn_attempts_per_player").orElse(1) < 1) {
            throw new ConfigValidationException(path + ".spawn_attempts_per_player must be at least 1.");
        }
        if (optionalInteger(profile, "per_player_mob_cap").orElse(1) < 1) {
            throw new ConfigValidationException(path + ".per_player_mob_cap must be at least 1.");
        }
        if (optionalInteger(profile, "global_spawn_cap").orElse(1) < 1) {
            throw new ConfigValidationException(path + ".global_spawn_cap must be at least 1.");
        }
    }

    private OverworldSpawnRuntimeConfig.SpawnProfile parseSpawnProfile(
            String name,
            Map<String, Object> overworld,
            Map<String, Object> activeSpawning,
            int minRadius,
            int maxRadius,
            double defaultPacingMultiplier,
            int defaultGlobalCap,
            int defaultPerPlayerCap,
            int defaultPulseIntervalTicks,
            int defaultSpawnAttemptsPerPlayer
    ) throws ConfigValidationException {
        Map<String, Object> profile = optionalMap(activeSpawning, name, "overworld_settings.yml.worlds.overworld.active_spawning");
        return new OverworldSpawnRuntimeConfig.SpawnProfile(
                name,
                optionalNumber(profile, "spawn_pacing_multiplier")
                        .or(() -> optionalNumber(overworld, "spawn_pacing_multiplier"))
                        .orElse(defaultPacingMultiplier),
                optionalInteger(profile, "global_spawn_cap")
                        .or(() -> optionalInteger(overworld, "global_spawn_cap"))
                        .orElse(defaultGlobalCap),
                optionalInteger(profile, "per_player_mob_cap")
                        .or(() -> optionalInteger(activeSpawning, "per_player_mob_cap"))
                        .orElse(defaultPerPlayerCap),
                optionalInteger(profile, "pulse_interval_ticks")
                        .or(() -> optionalInteger(activeSpawning, "pulse_interval_ticks"))
                        .orElse(defaultPulseIntervalTicks),
                optionalInteger(profile, "spawn_attempts_per_player")
                        .or(() -> optionalInteger(activeSpawning, "spawn_attempts_per_player"))
                        .orElse(defaultSpawnAttemptsPerPlayer),
                optionalInteger(profile, "min_spawn_radius")
                        .or(() -> optionalInteger(activeSpawning, "min_spawn_radius"))
                        .orElse(minRadius),
                optionalInteger(profile, "max_spawn_radius")
                        .or(() -> optionalInteger(activeSpawning, "max_spawn_radius"))
                        .orElse(maxRadius)
        );
    }

    private static OverworldSpawnRuntimeConfig inactiveOverworldSpawnRuntime() {
        OverworldSpawnRuntimeConfig.SpawnProfile inactiveProfile = new OverworldSpawnRuntimeConfig.SpawnProfile("inactive", 1.0, 1, 1, 100, 1, 24, 56);
        return new OverworldSpawnRuntimeConfig(false, inactiveProfile, inactiveProfile);
    }

    private void validateNightmareSettings(Map<String, Object> root) throws ConfigValidationException {
        requireInteger(root, "version", "nightmare_settings.yml");
        Map<String, Object> scaling = requireMap(root, "scaling", "nightmare_settings.yml");
        requireNumber(scaling, "max_multiplier", "nightmare_settings.yml scaling");
        requireNumber(scaling, "days_to_max", "nightmare_settings.yml scaling");
    }

    private void validateMobsSettings(Map<String, Object> root) throws ConfigValidationException {
        requireInteger(root, "version", "mobs_settings.yml");
        Map<String, Object> global = requireMap(root, "global", "mobs_settings.yml");
        validateGlobalBlockBreaking(global);

        Map<String, Object> mobs = requireMap(root, "mobs", "mobs_settings.yml");
        for (Map.Entry<String, Object> mobEntry : mobs.entrySet()) {
            String mobId = mobEntry.getKey();
            validateMobId(mobId, "mobs_settings.yml mobs");
            Map<String, Object> mob = requireMapValue(mobEntry.getValue(), "mobs_settings.yml " + mobId);
            validateExplosionSettings(mob, "mobs_settings.yml " + mobId);
            validateProjectileSettings(mob, "mobs_settings.yml " + mobId);
            validateEquipmentSettings(mob, "mobs_settings.yml " + mobId);
        }
    }

    private void validateProjectileSettings(Map<String, Object> mob, String path) throws ConfigValidationException {
        Map<String, Object> projectiles = optionalMap(mob, "projectiles", path);
        if (projectiles.isEmpty()) {
            return;
        }

        Map<String, Object> explosiveArrows = optionalMap(projectiles, "explosive_arrows", path + ".projectiles");
        if (explosiveArrows.isEmpty()) {
            return;
        }

        double power = requireNumber(explosiveArrows, "power", path + ".projectiles.explosive_arrows");
        if (power < 0.0) {
            throw new ConfigValidationException(path + ".projectiles.explosive_arrows.power must be >= 0.");
        }
    }

    private void validateEquipmentSettings(Map<String, Object> mob, String path) throws ConfigValidationException {
        Object equipmentValue = mob.get("equipment");
        if (equipmentValue == null) {
            return;
        }

        Map<String, Object> equipment = requireMapValue(equipmentValue, path + ".equipment");
        validateEquipmentSlot(equipment, "main_hand", path);
        validateEquipmentSlot(equipment, "off_hand", path);

        Object armorValue = equipment.get("armor");
        if (armorValue instanceof Map<?, ?> armorMap) {
            Map<String, Object> armor = castMap(path + ".equipment.armor", armorMap);
            validateEquipmentSlot(armor, "helmet", path + ".equipment.armor");
            validateEquipmentSlot(armor, "chestplate", path + ".equipment.armor");
            validateEquipmentSlot(armor, "leggings", path + ".equipment.armor");
            validateEquipmentSlot(armor, "boots", path + ".equipment.armor");
        }
    }

    private void validateEquipmentSlot(Map<String, Object> equipment, String slot, String path) throws ConfigValidationException {
        Object slotValue = equipment.get(slot);
        if (slotValue == null) {
            return;
        }

        Map<String, Object> slotConfig = requireMapValue(slotValue, path + "." + slot);
        validateIdentifier(requireString(slotConfig, "item", path + "." + slot), path + "." + slot + ".item");
        requireNumber(slotConfig, "chance", path + "." + slot);
        requireNumber(slotConfig, "drop_chance", path + "." + slot);
    }

    private Map<String, MobRuntimeConfig> parseMobConfigs(Map<String, Object> root) throws ConfigValidationException {
        Map<String, Object> mobs = requireMap(root, "mobs", "mobs_settings.yml");
        Map<String, MobRuntimeConfig> parsed = new HashMap<>();

        for (Map.Entry<String, Object> mobEntry : mobs.entrySet()) {
            String mobId = mobEntry.getKey();
            Map<String, Object> mob = requireMapValue(mobEntry.getValue(), "mobs_settings.yml " + mobId);
            parsed.put(mobId, parseMobConfig(mobId, mob));
        }

        return Map.copyOf(parsed);
    }

    private BlockBreakRuntimeConfig parseBlockBreaking(Map<String, Object> root) throws ConfigValidationException {
        Map<String, Object> global = requireMap(root, "global", "mobs_settings.yml");
        Map<String, Object> blockBreaking = requireMap(global, "block_breaking", "mobs_settings.yml global");
        Map<String, Object> strengthTiers = requireMap(blockBreaking, "strength_tiers", "mobs_settings.yml global.block_breaking");

        Map<String, Integer> breakTicks = new HashMap<>();
        for (String tier : strengthTiers.keySet()) {
            Map<String, Object> tierSettings = requireMapValue(strengthTiers.get(tier), "mobs_settings.yml strength_tiers." + tier);
            breakTicks.put(tier, requireInteger(tierSettings, "break_ticks", "mobs_settings.yml strength_tiers." + tier));
        }

        Map<String, String> overrides = new HashMap<>();
        Map<String, Object> blockOverrides = requireMap(blockBreaking, "block_overrides", "mobs_settings.yml global.block_breaking");
        for (Map.Entry<String, Object> override : blockOverrides.entrySet()) {
            overrides.put(override.getKey(), String.valueOf(override.getValue()));
        }

        return new BlockBreakRuntimeConfig(
                optionalBoolean(blockBreaking, "enabled").orElse(false),
                requireInteger(blockBreaking, "stuck_check_ticks", "mobs_settings.yml global.block_breaking"),
                requireNumber(blockBreaking, "not_closer_distance_epsilon", "mobs_settings.yml global.block_breaking"),
                requireString(blockBreaking, "default_strength", "mobs_settings.yml global.block_breaking"),
                Map.copyOf(breakTicks),
                Set.copyOf(requireStringList(blockBreaking, "unbreakable_blocks", "mobs_settings.yml global.block_breaking")),
                Map.copyOf(overrides)
        );
    }

    private BlockPlaceRuntimeConfig parseBlockPlacing(Map<String, Object> root) throws ConfigValidationException {
        Map<String, Object> global = requireMap(root, "global", "mobs_settings.yml");
        Map<String, Object> blockPlacing = requireMap(global, "block_placing", "mobs_settings.yml global");
        List<String> allowedBlocks = requireStringList(blockPlacing, "allowed_blocks", "mobs_settings.yml global.block_placing");
        for (String blockId : allowedBlocks) {
            validateIdentifier(blockId, "mobs_settings.yml global.block_placing.allowed_blocks");
        }

        return new BlockPlaceRuntimeConfig(
                optionalBoolean(blockPlacing, "enabled").orElse(false),
                requireInteger(blockPlacing, "cooldown_ticks", "mobs_settings.yml global.block_placing"),
                requireInteger(blockPlacing, "max_attempts_per_stuck_event", "mobs_settings.yml global.block_placing"),
                List.copyOf(allowedBlocks)
        );
    }

    private boolean parseDebugLogging(Map<String, Object> root) throws ConfigValidationException {
        Map<String, Object> global = requireMap(root, "global", "mobs_settings.yml");
        Map<String, Object> debug = optionalMap(global, "debug", "mobs_settings.yml global");
        return optionalBoolean(debug, "enabled").orElse(false);
    }

    private MobRuntimeConfig parseMobConfig(String mobId, Map<String, Object> mob) throws ConfigValidationException {
        Map<String, Object> attributes = optionalMap(mob, "attributes", "mobs_settings.yml " + mobId);
        Map<String, Object> aggro = optionalMap(mob, "aggro", "mobs_settings.yml " + mobId);
        Map<String, Object> sunlight = optionalMap(mob, "sunlight", "mobs_settings.yml " + mobId);
        Map<String, Object> explosions = optionalMap(mob, "explosions", "mobs_settings.yml " + mobId);
        Map<String, Object> projectiles = optionalMap(mob, "projectiles", "mobs_settings.yml " + mobId);
        Map<String, Object> blockBreaking = optionalMap(mob, "block_breaking", "mobs_settings.yml " + mobId);
        Map<String, Object> blockPlacing = optionalMap(mob, "block_placing", "mobs_settings.yml " + mobId);

        return new MobRuntimeConfig(
                mobId,
                optionalBoolean(mob, "enabled").orElse(true),
                new MobRuntimeConfig.AttributeSettings(
                        optionalDouble(attributes, "max_health"),
                        optionalDouble(attributes, "movement_speed"),
                        optionalDouble(attributes, "attack_damage"),
                        optionalDouble(attributes, "follow_range")
                ),
                new MobRuntimeConfig.AggroSettings(
                        optionalBoolean(aggro, "enabled").orElse(true),
                        optionalDouble(aggro, "detection_range")
                ),
                new MobRuntimeConfig.SunlightSettings(
                        optionalBoolean(sunlight, "burns_in_daylight")
                ),
                new MobRuntimeConfig.ExplosionSettings(
                        optionalBoolean(explosions, "enabled").orElse(false),
                        optionalDouble(explosions, "radius_multiplier"),
                        optionalInteger(explosions, "fuse_ticks"),
                        optionalDouble(explosions, "fireball_power_multiplier")
                ),
                parseProjectileSettings(projectiles, mobId),
                optionalBoolean(blockBreaking, "enabled").orElse(false),
                optionalBoolean(blockPlacing, "enabled").orElse(false),
                parseEquipment(mob, mobId)
        );
    }

    private MobRuntimeConfig.ProjectileSettings parseProjectileSettings(Map<String, Object> projectiles, String mobId) throws ConfigValidationException {
        Map<String, Object> explosiveArrows = optionalMap(projectiles, "explosive_arrows", "mobs_settings.yml " + mobId + ".projectiles");
        return new MobRuntimeConfig.ProjectileSettings(
                new MobRuntimeConfig.ExplosiveArrowSettings(
                        optionalBoolean(explosiveArrows, "enabled").orElse(false),
                        optionalDouble(explosiveArrows, "power").orElse(0.0),
                        optionalBoolean(explosiveArrows, "causes_fire").orElse(false),
                        optionalBoolean(explosiveArrows, "damages_blocks").orElse(true)
                )
        );
    }

    private Map<String, MobRuntimeConfig.EquipmentSettings> parseEquipment(Map<String, Object> mob, String mobId) throws ConfigValidationException {
        Object equipmentValue = mob.get("equipment");
        if (equipmentValue == null) {
            return Map.of();
        }

        Map<String, Object> equipment = requireMapValue(equipmentValue, "mobs_settings.yml " + mobId + ".equipment");
        Map<String, MobRuntimeConfig.EquipmentSettings> parsed = new HashMap<>();
        parseEquipmentSlot(parsed, equipment, "main_hand", "mobs_settings.yml " + mobId + ".equipment");
        parseEquipmentSlot(parsed, equipment, "off_hand", "mobs_settings.yml " + mobId + ".equipment");

        Object armorValue = equipment.get("armor");
        if (armorValue instanceof Map<?, ?> armorMap) {
            Map<String, Object> armor = castMap("mobs_settings.yml " + mobId + ".equipment.armor", armorMap);
            parseEquipmentSlot(parsed, armor, "helmet", "mobs_settings.yml " + mobId + ".equipment.armor");
            parseEquipmentSlot(parsed, armor, "chestplate", "mobs_settings.yml " + mobId + ".equipment.armor");
            parseEquipmentSlot(parsed, armor, "leggings", "mobs_settings.yml " + mobId + ".equipment.armor");
            parseEquipmentSlot(parsed, armor, "boots", "mobs_settings.yml " + mobId + ".equipment.armor");
        }

        return Map.copyOf(parsed);
    }

    private void parseEquipmentSlot(Map<String, MobRuntimeConfig.EquipmentSettings> parsed, Map<String, Object> equipment, String slot, String path) throws ConfigValidationException {
        Object slotValue = equipment.get(slot);
        if (slotValue == null) {
            return;
        }

        Map<String, Object> slotConfig = requireMapValue(slotValue, path + "." + slot);
        parsed.put(slot, new MobRuntimeConfig.EquipmentSettings(
                requireString(slotConfig, "item", path + "." + slot),
                requireNumber(slotConfig, "chance", path + "." + slot),
                (float) requireNumber(slotConfig, "drop_chance", path + "." + slot)
        ));
    }

    private void validateGlobalBlockBreaking(Map<String, Object> global) throws ConfigValidationException {
        Map<String, Object> blockBreaking = requireMap(global, "block_breaking", "mobs_settings.yml global");
        Map<String, Object> strengthTiers = requireMap(blockBreaking, "strength_tiers", "mobs_settings.yml global.block_breaking");
        if (!strengthTiers.keySet().containsAll(REQUIRED_STRENGTH_TIERS)) {
            throw new ConfigValidationException("mobs_settings.yml global.block_breaking.strength_tiers must define " + REQUIRED_STRENGTH_TIERS + ".");
        }

        String defaultStrength = requireString(blockBreaking, "default_strength", "mobs_settings.yml global.block_breaking");
        if (!strengthTiers.containsKey(defaultStrength)) {
            throw new ConfigValidationException("mobs_settings.yml default_strength must match a defined strength tier.");
        }

        List<String> unbreakableBlocks = requireStringList(blockBreaking, "unbreakable_blocks", "mobs_settings.yml global.block_breaking");
        for (String blockId : unbreakableBlocks) {
            validateIdentifier(blockId, "mobs_settings.yml unbreakable_blocks");
        }
        if (!Set.copyOf(unbreakableBlocks).containsAll(REQUIRED_UNBREAKABLE_BLOCKS)) {
            throw new ConfigValidationException("mobs_settings.yml unbreakable_blocks must include " + REQUIRED_UNBREAKABLE_BLOCKS + ".");
        }

        Map<String, Object> blockOverrides = requireMap(blockBreaking, "block_overrides", "mobs_settings.yml global.block_breaking");
        for (Map.Entry<String, Object> override : blockOverrides.entrySet()) {
            validateIdentifier(override.getKey(), "mobs_settings.yml block_overrides");
            if (!(override.getValue() instanceof String strength) || !strengthTiers.containsKey(strength)) {
                throw new ConfigValidationException("Block override for " + override.getKey() + " must use a defined strength tier.");
            }
        }
    }

    private void validateExplosionSettings(Map<String, Object> mob, String path) throws ConfigValidationException {
        Object explosionsValue = mob.get("explosions");
        if (explosionsValue == null) {
            return;
        }

        Map<String, Object> explosions = requireMapValue(explosionsValue, path + ".explosions");
        Object explodeOnDeath = explosions.get("explode_on_death");
        if (Boolean.TRUE.equals(explodeOnDeath)) {
            throw new ConfigValidationException(path + ".explosions.explode_on_death is intentionally unsupported for phase one.");
        }
    }

    private void validateMobId(String mobId, String path) throws ConfigValidationException {
        validateIdentifier(mobId, path);
        if (!SUPPORTED_VANILLA_MOBS.contains(mobId)) {
            throw new ConfigValidationException(path + " contains unsupported mob id " + mobId + ". Phase one supports vanilla hostile mobs only.");
        }
    }

    private void validateIdentifier(String identifier, String path) throws ConfigValidationException {
        if (!IDENTIFIER_PATTERN.matcher(identifier).matches()) {
            throw new ConfigValidationException(path + " contains invalid identifier " + identifier + ".");
        }
    }

    private Map<String, Object> requireMap(Map<String, Object> root, String key, String path) throws ConfigValidationException {
        Object value = root.get(key);
        return requireMapValue(value, path + "." + key);
    }

    private Map<String, Object> optionalMap(Map<String, Object> root, String key, String path) throws ConfigValidationException {
        Object value = root.get(key);
        if (value == null) {
            return Map.of();
        }
        return requireMapValue(value, path + "." + key);
    }

    private Map<String, Object> requireMapValue(Object value, String path) throws ConfigValidationException {
        if (!(value instanceof Map<?, ?> map)) {
            throw new ConfigValidationException(path + " must be a YAML object.");
        }
        return castMap(path, map);
    }

    private String requireString(Map<String, Object> root, String key, String path) throws ConfigValidationException {
        Object value = root.get(key);
        if (!(value instanceof String stringValue) || stringValue.isBlank()) {
            throw new ConfigValidationException(path + "." + key + " must be a non-empty string.");
        }
        return stringValue;
    }

    private int requireInteger(Map<String, Object> root, String key, String path) throws ConfigValidationException {
        Object value = root.get(key);
        if (value instanceof Integer integerValue) {
            return integerValue;
        }
        throw new ConfigValidationException(path + "." + key + " must be an integer.");
    }

    private double requireNumber(Map<String, Object> root, String key, String path) throws ConfigValidationException {
        Object value = root.get(key);
        if (value instanceof Number number) {
            return number.doubleValue();
        }
        throw new ConfigValidationException(path + "." + key + " must be a number.");
    }

    private Optional<Double> optionalDouble(Map<String, Object> root, String key) {
        Object value = root.get(key);
        if (value instanceof Number number) {
            return Optional.of(number.doubleValue());
        }
        return Optional.empty();
    }

    private Optional<Double> optionalNumber(Map<String, Object> root, String key) {
        return optionalDouble(root, key);
    }

    private Optional<Integer> optionalInteger(Map<String, Object> root, String key) {
        Object value = root.get(key);
        if (value instanceof Integer integer) {
            return Optional.of(integer);
        }
        return Optional.empty();
    }

    private Optional<Boolean> optionalBoolean(Map<String, Object> root, String key) {
        Object value = root.get(key);
        if (value instanceof Boolean booleanValue) {
            return Optional.of(booleanValue);
        }
        return Optional.empty();
    }

    private List<String> requireStringList(Map<String, Object> root, String key, String path) throws ConfigValidationException {
        Object value = root.get(key);
        if (!(value instanceof List<?> list)) {
            throw new ConfigValidationException(path + "." + key + " must be a list.");
        }

        for (Object item : list) {
            if (!(item instanceof String)) {
                throw new ConfigValidationException(path + "." + key + " must contain only strings.");
            }
        }

        @SuppressWarnings("unchecked")
        List<String> strings = (List<String>) list;
        return strings;
    }

    private List<String> optionalStringList(Map<String, Object> root, String key) throws ConfigValidationException {
        Object value = root.get(key);
        if (value == null) {
            return List.of();
        }
        if (!(value instanceof List<?> list)) {
            throw new ConfigValidationException(key + " must be a list.");
        }
        for (Object item : list) {
            if (!(item instanceof String)) {
                throw new ConfigValidationException(key + " must contain only strings.");
            }
        }
        @SuppressWarnings("unchecked")
        List<String> strings = (List<String>) list;
        return strings;
    }

    private Map<String, Object> castMap(String path, Map<?, ?> map) throws ConfigValidationException {
        for (Object key : map.keySet()) {
            if (!(key instanceof String)) {
                throw new ConfigValidationException(path + " contains a non-string key.");
            }
        }

        @SuppressWarnings("unchecked")
        Map<String, Object> cast = (Map<String, Object>) map;
        return cast;
    }
}
