package com.maidcommandprocessor.handler;

import com.maidcommandprocessor.MaidCommandProcessor;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class CommandQueueModule {
    
    private static final Map<UUID, List<PendingCommand>> pendingCommands = new ConcurrentHashMap<>();
    private static boolean hasPendingCommands = false;
    
    public static class PendingCommand {
        public final String command;
        public final String description;
        public final ServerPlayer player;
        public final Entity maidEntity;
        
        public PendingCommand(String command, String description, ServerPlayer player, Entity maidEntity) {
            this.command = command;
            this.description = description;
            this.player = player;
            this.maidEntity = maidEntity;
        }
    }
    
    public static void addPendingCommand(UUID maidId, String command, String description, ServerPlayer player, Entity maidEntity) {
        pendingCommands.computeIfAbsent(maidId, k -> new ArrayList<>()).add(
            new PendingCommand(command, description, player, maidEntity)
        );
        hasPendingCommands = true;
        MaidCommandProcessor.LOGGER.info(
            "Added pending command for maid [{}]: {} ({})",
            maidId, command, description
        );
    }
    
    public static void executePendingCommands() {
        if (!hasPendingCommands) {
            return;
        }
        
        List<UUID> maidsToProcess = new ArrayList<>(pendingCommands.keySet());
        
        for (UUID maidId : maidsToProcess) {
            List<PendingCommand> commands = pendingCommands.get(maidId);
            if (commands == null || commands.isEmpty()) {
                continue;
            }
            
            MaidCommandProcessor.LOGGER.info(
                "Executing {} pending command(s) for maid [{}]",
                commands.size(), maidId
            );
            
            List<String> commandStrings = new ArrayList<>();
            ServerPlayer player = null;
            Entity maidEntity = null;
            
            for (PendingCommand cmd : commands) {
                commandStrings.add(cmd.command);
                if (player == null) {
                    player = cmd.player;
                    maidEntity = cmd.maidEntity;
                }
            }
            
            if (commandStrings.isEmpty() || player == null || maidEntity == null) {
                continue;
            }
            
            int successCount = CommandExecutorModule.executeBatchCommands(
                player.createCommandSourceStack(),
                commandStrings,
                player,
                maidEntity
            );
            
            MaidCommandProcessor.LOGGER.info(
                "Pending commands executed: {}/{} succeeded for maid [{}]",
                successCount, commandStrings.size(), maidId
            );
            
            pendingCommands.remove(maidId);
        }
        
        if (pendingCommands.isEmpty()) {
            hasPendingCommands = false;
        }
    }
    
    public static boolean hasPending() {
        return hasPendingCommands;
    }
    
    public static int getPendingCount() {
        int count = 0;
        for (List<PendingCommand> commands : pendingCommands.values()) {
            count += commands.size();
        }
        return count;
    }
}
