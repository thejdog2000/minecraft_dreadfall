package com.thejdog2000.dreadfall.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.thejdog2000.dreadfall.behavior.MobSpawnApplier;
import com.thejdog2000.dreadfall.config.ConfigValidationException;
import com.thejdog2000.dreadfall.config.DreadfallConfigManager;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.permissions.Permissions;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.Vec3;

import java.time.Instant;
import java.util.Optional;

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
                        .executes(context -> status(context.getSource(), configManager)))
                .then(Commands.literal("testspawn")
                        .then(Commands.argument("mob", StringArgumentType.word())
                                .executes(context -> testSpawn(
                                        context.getSource(),
                                        configManager,
                                        StringArgumentType.getString(context, "mob"))))));
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
        source.sendSuccess(() -> Component.literal("Dreadfall debug logging: " + configManager.isDebugLoggingEnabled()), false);
        source.sendSuccess(() -> Component.literal("Dreadfall configured mob settings: " + configManager.getMobConfigCount()), false);
        source.sendSuccess(() -> Component.literal("Dreadfall configured overworld spawns: " + configManager.getOverworldSpawns().size()), false);
        source.sendSuccess(() -> Component.literal("Dreadfall active overworld spawning: "
                + configManager.getOverworldSpawnRuntime().enabled()
                + " cap=" + configManager.getOverworldSpawnRuntime().globalSpawnCap()
                + " per_player=" + configManager.getOverworldSpawnRuntime().perPlayerMobCap()
                + " interval=" + configManager.getOverworldSpawnRuntime().pulseIntervalTicks()), false);
        source.sendSuccess(() -> Component.literal("Dreadfall last loaded: " + loaded), false);
        return 1;
    }

    private static int testSpawn(CommandSourceStack source, DreadfallConfigManager configManager, String mobId) {
        Identifier identifier = Identifier.tryParse(mobId);
        if (identifier == null) {
            source.sendFailure(Component.literal("Invalid mob id: " + mobId));
            return 0;
        }

        Optional<EntityType<?>> entityType = BuiltInRegistries.ENTITY_TYPE.getOptional(identifier);
        if (entityType.isEmpty()) {
            source.sendFailure(Component.literal("Unknown mob id: " + mobId));
            return 0;
        }

        ServerLevel level = source.getLevel();
        Vec3 position = source.getPosition();
        BlockPos blockPosition = BlockPos.containing(position);
        Entity entity = entityType.get().create(level, spawned -> {
        }, blockPosition, EntitySpawnReason.COMMAND, true, false);
        if (entity == null) {
            source.sendFailure(Component.literal("Could not create entity: " + mobId));
            return 0;
        }

        entity.snapTo(position.x(), position.y(), position.z(), source.getRotation().y, 0.0F);
        if (entity instanceof Mob mob) {
            MobSpawnApplier.applyConfiguredSettings(configManager, mob);
        }

        if (!level.addFreshEntity(entity)) {
            source.sendFailure(Component.literal("Could not add entity to world: " + mobId));
            return 0;
        }

        source.sendSuccess(() -> Component.literal("Spawned Dreadfall test mob: " + mobId), true);
        return 1;
    }
}
