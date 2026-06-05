package com.thejdog2000.dreadfall;

import com.thejdog2000.dreadfall.behavior.MobSpawnApplier;
import com.thejdog2000.dreadfall.behavior.MobBlockBreaker;
import com.thejdog2000.dreadfall.behavior.MobBlockPlacer;
import com.thejdog2000.dreadfall.command.DreadfallCommands;
import com.thejdog2000.dreadfall.config.ConfigValidationException;
import com.thejdog2000.dreadfall.config.DreadfallConfigManager;
import com.thejdog2000.dreadfall.spawn.OverworldSpawnRegistrar;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;

public final class DreadfallMod implements ModInitializer {
    public static final String MOD_ID = "dreadfall";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
    private static DreadfallConfigManager configManager;

    @Override
    public void onInitialize() {
        Path configDirectory = FabricLoader.getInstance().getConfigDir().resolve(MOD_ID);
        configManager = new DreadfallConfigManager(configDirectory);

        try {
            configManager.loadOrCreate();
        } catch (ConfigValidationException exception) {
            LOGGER.error("Dreadfall config validation failed. The mod will load, but gameplay systems will stay inactive until config is fixed.", exception);
        }

        OverworldSpawnRegistrar.register(configManager);
        MobSpawnApplier.register(configManager);
        MobBlockBreaker.register(configManager);
        MobBlockPlacer.register(configManager);
        DreadfallCommands.register(configManager);
        LOGGER.info("Dreadfall initialized for Minecraft 26.1.2.");
    }

    public static DreadfallConfigManager getConfigManager() {
        return configManager;
    }
}
