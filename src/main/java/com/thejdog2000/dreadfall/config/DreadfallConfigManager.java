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
import java.util.List;
import java.util.Map;
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

            lastLoadedAt = Instant.now();
            DreadfallMod.LOGGER.info("Loaded Dreadfall configs from {}.", configDirectory);
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
            Map<String, Object> mobs = requireMap(world, "mobs", "overworld_settings.yml worlds." + worldEntry.getKey());
            for (String mobId : mobs.keySet()) {
                validateMobId(mobId, "overworld_settings.yml worlds." + worldEntry.getKey() + ".mobs");
                Map<String, Object> mob = requireMapValue(mobs.get(mobId), "overworld_settings.yml " + mobId);
                int minGroupSize = requireInteger(mob, "min_group_size", "overworld_settings.yml " + mobId);
                int maxGroupSize = requireInteger(mob, "max_group_size", "overworld_settings.yml " + mobId);
                if (minGroupSize < 1 || maxGroupSize < minGroupSize) {
                    throw new ConfigValidationException("Invalid group size for " + mobId + " in overworld_settings.yml.");
                }
            }
        }
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
        }
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
