package com.thejdog2000.dreadfall.command;

import com.mojang.brigadier.CommandDispatcher;
import com.thejdog2000.dreadfall.config.ConfigValidationException;
import com.thejdog2000.dreadfall.config.DreadfallConfigManager;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.permissions.Permissions;

import java.time.Instant;

public final class DreadfallCommands {
    private DreadfallCommands() {
    }

    public static void register(DreadfallConfigManager configManager) {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            registerRoot(dispatcher, "dreadfall", configManager);
            registerRoot(dispatcher, "ma", configManager);
        });
    }

    private static void registerRoot(CommandDispatcher<CommandSourceStack> dispatcher, String rootCommand, DreadfallConfigManager configManager) {
        dispatcher.register(Commands.literal(rootCommand)
                .requires(source -> source.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER))
                .then(Commands.literal("reload")
                        .executes(context -> reload(context.getSource(), configManager)))
                .then(Commands.literal("status")
                        .executes(context -> status(context.getSource(), configManager))));
    }

    private static int reload(CommandSourceStack source, DreadfallConfigManager configManager) {
        try {
            configManager.loadOrCreate();
            source.sendSuccess(() -> Component.literal("Dreadfall configs reloaded."), true);
            return 1;
        } catch (ConfigValidationException exception) {
            source.sendFailure(Component.literal("Dreadfall config reload failed: " + exception.getMessage()));
            return 0;
        }
    }

    private static int status(CommandSourceStack source, DreadfallConfigManager configManager) {
        Instant lastLoadedAt = configManager.getLastLoadedAt();
        String loaded = lastLoadedAt == null ? "not loaded" : lastLoadedAt.toString();
        source.sendSuccess(() -> Component.literal("Dreadfall config directory: " + configManager.getConfigDirectory()), false);
        source.sendSuccess(() -> Component.literal("Dreadfall config files: " + String.join(", ", configManager.getConfigFiles())), false);
        source.sendSuccess(() -> Component.literal("Dreadfall last loaded: " + loaded), false);
        return 1;
    }
}
