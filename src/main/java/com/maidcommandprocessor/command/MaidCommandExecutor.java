package com.maidcommandprocessor.command;

import com.maidcommandprocessor.MaidCommandProcessor;
import com.maidcommandprocessor.config.MaidCommandConfig;
import com.maidcommandprocessor.handler.CommandExecutorModule;
import com.maidcommandprocessor.handler.PermissionModule;
import com.maidcommandprocessor.handler.PermissionModule.PermissionLevel;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

public class MaidCommandExecutor {
    
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
            Commands.literal("maidcmd")
                .then(Commands.literal("exec")
                    .then(Commands.argument("command", StringArgumentType.greedyString())
                        .executes(MaidCommandExecutor::executeCommand)))
                .then(Commands.literal("check")
                    .executes(MaidCommandExecutor::checkPermission))
                .then(Commands.literal("list")
                    .executes(MaidCommandExecutor::listCommands))
        );
    }
    
    private static int executeCommand(CommandContext<CommandSourceStack> context) {
        String command = StringArgumentType.getString(context, "command");
        CommandSourceStack source = context.getSource();
        
        MaidCommandProcessor.LOGGER.info("MaidCommandExecutor: Executing command: {}", command);
        
        try {
            source.getServer().getCommands().performPrefixedCommand(source, command);
            
            source.sendSystemMessage(
                Component.translatable("maid_command_processor.executor.success", command)
            );
            
            return Command.SINGLE_SUCCESS;
        } catch (Exception e) {
            MaidCommandProcessor.LOGGER.error("MaidCommandExecutor error: {}", e.getMessage());
            source.sendSystemMessage(
                Component.translatable("maid_command_processor.executor.error", e.getMessage())
            );
            return 0;
        }
    }
    
    private static int checkPermission(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        ServerPlayer player;
        try {
            player = source.getPlayerOrException();
        } catch (Exception e) {
            source.sendSystemMessage(Component.translatable("maid_command_processor.executor.not_player"));
            return 0;
        }
        
        PermissionLevel permission = PermissionModule.getPlayerPermission(player);
        
        source.sendSystemMessage(Component.translatable(
            "maid_command_processor.executor.permission",
            player.getName().getString(),
            permission.getName(),
            permission.getLevel()
        ));
        
        return Command.SINGLE_SUCCESS;
    }
    
    private static int listCommands(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        
        source.sendSystemMessage(Component.translatable("maid_command_processor.executor.vanilla_commands"));
        
        for (String cmd : CommandExecutorModule.getVanillaCommands()) {
            source.sendSystemMessage(Component.literal("  - " + cmd));
        }
        
        source.sendSystemMessage(Component.translatable("maid_command_processor.executor.maid_commands"));
        
        for (String cmd : CommandExecutorModule.getMaidModCommands()) {
            source.sendSystemMessage(Component.literal("  - " + cmd));
        }
        
        return Command.SINGLE_SUCCESS;
    }
}
