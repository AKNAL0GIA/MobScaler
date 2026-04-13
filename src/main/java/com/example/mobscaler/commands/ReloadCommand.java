package com.example.mobscaler.commands;

import com.example.mobscaler.config.MobScalerConfig;
import com.example.mobscaler.config.DimensionConfigManager;
import com.example.mobscaler.config.PlayerConfigManager;
import com.example.mobscaler.config.IndividualMobConfigManager;
import com.example.mobscaler.events.EntityHandler;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.arguments.BoolArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.level.border.WorldBorder;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.MutableComponent;

public class ReloadCommand {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
            Commands.literal("mobscaler")
                .requires(source -> source.hasPermission(2))
                .then(Commands.literal("reload")
                    .executes(ReloadCommand::execute)
                )
                .then(Commands.literal("debug")
                    .then(Commands.argument("enabled", BoolArgumentType.bool())
                        .executes(context -> {
                            boolean enabled = BoolArgumentType.getBool(context, "enabled");
                            MobScalerConfig.setDebugLoggingEnabled(enabled);
                            MobScalerConfig.save();

                            String status = enabled
                                ? Component.literal("enabled").withStyle(ChatFormatting.GREEN).getString()
                                : Component.literal("disabled").withStyle(ChatFormatting.RED).getString();

                            context.getSource().sendSuccess(
                                () -> Component.literal("[MobScaler] Debug logging " + status), true);

                            if (enabled) {
                                context.getSource().sendSuccess(
                                    () -> Component.literal("[MobScaler] Warning: debug logging may cause server lag")
                                        .withStyle(ChatFormatting.YELLOW), true);
                            }

                            return Command.SINGLE_SUCCESS;
                        })
                    )
                    .then(Commands.literal("status")
                        .executes(context -> {
                            boolean enabled = MobScalerConfig.isDebugLoggingEnabled();
                            String status = enabled
                                ? Component.literal("enabled").withStyle(ChatFormatting.GREEN).getString()
                                : Component.literal("disabled").withStyle(ChatFormatting.RED).getString();

                            context.getSource().sendSuccess(
                                () -> Component.literal("[MobScaler] Debug logging is currently " + status), true);
                            return Command.SINGLE_SUCCESS;
                        })
                    )
                )
        );
    }

    private static int execute(CommandContext<CommandSourceStack> context) {
        try {
            // Reload all configurations
            MobScalerConfig.init();
            DimensionConfigManager.loadConfigs();
            PlayerConfigManager.loadConfigs();
            
            // Очищаем все индивидуальные настройки мобов перед загрузкой новых
            com.example.mobscaler.config.IndividualMobManager.clearAllIndividualMobConfigs();
            
            // Загружаем новые конфигурации
            IndividualMobConfigManager.loadConfigs();
            
            // Update all entities in the world
            ServerLevel serverLevel = context.getSource().getLevel();
            if (serverLevel != null) {
                // Update all players
                for (Player player : serverLevel.players()) {
                    EntityHandler.handlePlayerModifiers(player, 
                        serverLevel.dimension().location().toString(), 
                        EntityHandler.isNight(serverLevel));
                }
                
                // Update all other entities
                WorldBorder worldBorder = serverLevel.getWorldBorder();
                double size = worldBorder.getSize() / 2.0;
                AABB worldBounds = new AABB(
                    worldBorder.getCenterX() - size,
                    serverLevel.getMinBuildHeight(),
                    worldBorder.getCenterZ() - size,
                    worldBorder.getCenterX() + size,
                    serverLevel.getMaxBuildHeight(),
                    worldBorder.getCenterZ() + size
                );
                
                for (LivingEntity entity : serverLevel.getEntitiesOfClass(LivingEntity.class, worldBounds)) {
                    if (!(entity instanceof Player)) {
                        EntityHandler.handleMobModifiers(entity,
                            serverLevel,
                            serverLevel.dimension().location().toString(),
                            EntityHandler.isNight(serverLevel),
                            EntityHandler.getDifficultyMultiplier(serverLevel.getDifficulty(), true),
                            EntityHandler.getDifficultyMultiplier(serverLevel.getDifficulty(), false),
                            true);
                    }
                }
            }
            
            // Send success message with colored text
            MutableComponent message = Component.literal("")
                .append(Component.literal("MobScaler").withStyle(ChatFormatting.YELLOW))
                .append(Component.literal(" configuration reloaded "))
                .append(Component.literal("successfully!").withStyle(ChatFormatting.GREEN));
                
                context.getSource().sendSuccess(() -> message, true);
            
        } catch (Exception e) {
            // Send error message with colored text
            MutableComponent errorMessage = Component.literal("")
                .append(Component.literal("MobScaler").withStyle(ChatFormatting.YELLOW))
                .append(Component.literal(" configuration reload "))
                .append(Component.literal("failed!").withStyle(ChatFormatting.RED))
                .append(Component.literal(" Error: " + e.getMessage()).withStyle(ChatFormatting.RED));
                
            context.getSource().sendFailure(errorMessage);
        }
        
        return Command.SINGLE_SUCCESS;
    }
} 