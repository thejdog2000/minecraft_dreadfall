package com.thejdog2000.dreadfall;

import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class DreadfallMod implements ModInitializer {
    public static final String MOD_ID = "dreadfall";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        LOGGER.info("Dreadfall initialized for Minecraft 26.1.2.");
    }
}

