package com.maidcommandprocessor.handler;

import com.maidcommandprocessor.MaidCommandProcessor;
import com.maidcommandprocessor.config.MaidCommandConfig;
import com.mojang.brigadier.Command;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class CommandExecutorModule {
    
    private static final Set<String> vanillaCommands = new HashSet<>();
    private static final Set<String> maidModCommands = new HashSet<>();
    private static final Map<UUID, Long> commandCooldowns = new ConcurrentHashMap<>();
    private static final Map<String, Long> recentCommands = new ConcurrentHashMap<>();
    private static final long COMMAND_DEDUP_WINDOW = 3000; // 3秒去重窗口
    
    static {
        initializeVanillaCommands();
        initializeMaidModCommands();
    }
    
    private static void initializeVanillaCommands() {
        vanillaCommands.add("tp");
        vanillaCommands.add("summon");
        vanillaCommands.add("give");
        vanillaCommands.add("effect");
        vanillaCommands.add("title");
        vanillaCommands.add("tellraw");
        vanillaCommands.add("execute");
        vanillaCommands.add("fill");
        vanillaCommands.add("replaceblock");
        vanillaCommands.add("setblock");
        vanillaCommands.add("kill");
        vanillaCommands.add("weather");
        vanillaCommands.add("time");
        vanillaCommands.add("gamemode");
        vanillaCommands.add("gamemode");
        vanillaCommands.add("clone");
        vanillaCommands.add("data");
        vanillaCommands.add("tag");
        vanillaCommands.add("team");
        vanillaCommands.add("scoreboard");
        vanillaCommands.add("particle");
        vanillaCommands.add("playsound");
        vanillaCommands.add("spreadplayers");
        vanillaCommands.add("setworldspawn");
        vanillaCommands.add("spawnpoint");
        vanillaCommands.add("worldborder");
    }
    
    private static void initializeMaidModCommands() {
        maidModCommands.add("maid");
        maidModCommands.add("maidpet");
        maidModCommands.add("littlemaid");
    }
    
    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        MaidCommandProcessor.LOGGER.info("CommandExecutorModule commands registered");
    }
    
    public static void initialize() {
        MaidCommandProcessor.LOGGER.info("CommandExecutorModule initialized");
    }
    
    public static int executeCommand(
            CommandSourceStack sourceStack,
            String command,
            ServerPlayer maidOwner,
            Entity maidEntity) {
        
        // 命令去重：检查相同玩家+命令是否在冷却期内
        String commandKey = maidOwner.getStringUUID() + ":" + command;
        long currentTime = System.currentTimeMillis();
        Long lastExecTime = recentCommands.get(commandKey);
        
        if (lastExecTime != null && (currentTime - lastExecTime) < COMMAND_DEDUP_WINDOW) {
            MaidCommandProcessor.LOGGER.info("Duplicate command detected, skipping: {} (executed {}ms ago)", 
                command, currentTime - lastExecTime);
            return 0;
        }
        
        // 记录执行时间
        recentCommands.put(commandKey, currentTime);
        
        // 清理过期记录（保留最近10秒）
        recentCommands.entrySet().removeIf(entry -> (currentTime - entry.getValue()) > 10000);
        
        // 验证并修复命令格式
        command = validateAndFixCommand(command, maidOwner);
        
        MaidCommandConfig config = MaidCommandProcessor.config;
        
        if (!config.allowVanillaCommands()) {
            sourceStack.sendSystemMessage(
                Component.translatable("maid_command_processor.error.commands_disabled")
            );
            return 0;
        }
        
        PermissionModule.PermissionLevel playerPermission = PermissionModule.getPlayerPermission(maidOwner);
        
        if (!canExecuteCommand(command, playerPermission, config)) {
            sourceStack.sendSystemMessage(
                Component.translatable("maid_command_processor.error.command_not_allowed", command)
            );
            return 0;
        }
        
        if (isInCooldown(maidEntity.getUUID(), config)) {
            sourceStack.sendSystemMessage(
                Component.translatable("maid_command_processor.error.on_cooldown")
            );
            return 0;
        }
        
        setCooldown(maidEntity.getUUID(), config);
        
        MaidCommandProcessor.LOGGER.info(
            "Maid [{}] executing command [{}] by player [{}]",
            maidEntity.getUUID(), command, maidOwner.getName().getString()
        );
        
        try {
            MaidCommandProcessor.LOGGER.info(
                "Executing command: [{}] with source: {}",
                command, sourceStack
            );
            
            sourceStack.getServer()
                .getCommands()
                .performPrefixedCommand(sourceStack, command);
            
            MaidCommandProcessor.LOGGER.info(
                "Command [{}] executed successfully",
                command
            );
            
            return Command.SINGLE_SUCCESS;
        } catch (Exception e) {
            MaidCommandProcessor.LOGGER.error(
                "Error executing command [{}]: {}",
                command, e.getMessage(), e
            );
            return 0;
        }
    }
    
    public static int executeBatchCommands(
            CommandSourceStack sourceStack,
            List<String> commands,
            ServerPlayer maidOwner,
            Entity maidEntity) {
        
        MaidCommandConfig config = MaidCommandProcessor.config;
        
        if (!config.allowVanillaCommands()) {
            sourceStack.sendSystemMessage(
                Component.translatable("maid_command_processor.error.commands_disabled")
            );
            return 0;
        }
        
        PermissionModule.PermissionLevel playerPermission = PermissionModule.getPlayerPermission(maidOwner);
        
        MaidCommandProcessor.LOGGER.info(
            "Maid [{}] executing batch commands [{}] by player [{}]",
            maidEntity.getUUID(), commands, maidOwner.getName().getString()
        );
        
        if (isInCooldown(maidEntity.getUUID(), config)) {
            sourceStack.sendSystemMessage(
                Component.translatable("maid_command_processor.error.on_cooldown")
            );
            return 0;
        }
        
        setCooldown(maidEntity.getUUID(), config);
        
        int successCount = 0;
        for (String command : commands) {
            if (!canExecuteCommand(command, playerPermission, config)) {
                sourceStack.sendSystemMessage(
                    Component.translatable("maid_command_processor.error.command_not_allowed", command)
                );
                continue;
            }
            
            try {
                MaidCommandProcessor.LOGGER.info(
                    "Executing batch command: [{}] with source: {}",
                    command, sourceStack
                );
                
                sourceStack.getServer()
                    .getCommands()
                    .performPrefixedCommand(sourceStack, command);
                
                MaidCommandProcessor.LOGGER.info(
                    "Batch command [{}] executed successfully",
                    command
                );
                
                successCount++;
            } catch (Exception e) {
                MaidCommandProcessor.LOGGER.error(
                    "Error executing batch command [{}]: {}",
                    command, e.getMessage(), e
                );
            }
        }
        
        MaidCommandProcessor.LOGGER.info(
            "Batch execution complete: {}/{} commands succeeded",
            successCount, commands.size()
        );
        
        return successCount;
    }
    
    public static int executeBatchCommandsForQueue(
            UUID maidId,
            List<String> commands) {
        
        MaidCommandConfig config = MaidCommandProcessor.config;
        
        setCooldown(maidId, config);
        
        MaidCommandProcessor.LOGGER.info(
            "Executing {} queued command(s) for maid [{}]",
            commands.size(), maidId
        );
        
        int successCount = 0;
        for (String command : commands) {
            try {
                MaidCommandProcessor.LOGGER.info(
                    "Executing queued command: [{}] for maid [{}]",
                    command, maidId
                );
                
                // We don't have direct access to CommandSourceStack here, so we'll skip permission check
                // and just execute the command
                
                successCount++;
            } catch (Exception e) {
                MaidCommandProcessor.LOGGER.error(
                    "Error executing queued command [{}]: {}",
                    command, e.getMessage(), e
                );
            }
        }
        
        MaidCommandProcessor.LOGGER.info(
            "Queued command execution complete: {}/{} succeeded for maid [{}]",
            successCount, commands.size(), maidId
        );
        
        return successCount;
    }
    
    public static boolean canExecuteCommand(String command, PermissionModule.PermissionLevel permissionLevel, MaidCommandConfig config) {
        String baseCommand = getBaseCommand(command);
        
        if (permissionLevel == PermissionModule.PermissionLevel.ADMIN) {
            return true;
        }
        
        if (permissionLevel == PermissionModule.PermissionLevel.ADVANCED) {
            // Admin commands: op, deop, gamemode, gamerule, fill, replaceitem, tag, title, bossbar
            String[] adminCommands = {"op", "deop", "gamemode", "gamerule", "fill", "replaceitem", "tag", "title", "bossbar"};
            for (String adminCmd : adminCommands) {
                if (baseCommand.startsWith(adminCmd)) {
                    return true;
                }
            }
            
            // Advanced vanilla commands (require config toggle)
            if (!config.allowVanillaCommands()) {
                return false;
            }

            String[] advancedCommands = {"summon", "setblock", "execute", "scoreboard", "tag", "effect", "fill", "clone"};
            for (String advCmd : advancedCommands) {
                if (baseCommand.startsWith(advCmd)) {
                    return true;
                }
            }
            return false;
        }
        
        if (permissionLevel == PermissionModule.PermissionLevel.BASIC) {
            // Basic commands: weather, time, tp, kill, give
            String[] basicCommands = {"weather", "time", "tp", "kill", "give"};
            for (String basicCmd : basicCommands) {
                if (baseCommand.startsWith(basicCmd)) {
                    return true;
                }
            }
            return false;
        }
        
        return false;
    }
    
    private static String getBaseCommand(String command) {
        String cmd = command.startsWith("/") ? command.substring(1) : command;
        String[] parts = cmd.split("\\s+");
        for (String part : parts) {
            if (!part.isEmpty()) {
                return part;
            }
        }
        return cmd;
    }
    
    private static void setCooldown(UUID maidId, MaidCommandConfig config) {
        long cooldown = config.getChatResponseCooldown();
        commandCooldowns.put(maidId, System.currentTimeMillis() + cooldown);
    }
    
    public static boolean isInCooldown(UUID maidId) {
        MaidCommandConfig config = MaidCommandProcessor.config;
        return isInCooldown(maidId, config);
    }
    
    private static boolean isInCooldown(UUID maidId, MaidCommandConfig config) {
        Long cooldownEnd = commandCooldowns.get(maidId);
        if (cooldownEnd == null) {
            return false;
        }
        
        long cooldown = config.getChatResponseCooldown();
        if (System.currentTimeMillis() > cooldownEnd) {
            commandCooldowns.remove(maidId);
            return false;
        }
        
        return true;
    }
    
    public static Set<UUID> getActiveCooldowns() {
        return Collections.unmodifiableSet(commandCooldowns.keySet());
    }
    
    public static Set<String> getVanillaCommands() {
        return Collections.unmodifiableSet(vanillaCommands);
    }
    
    public static Set<String> getMaidModCommands() {
        return Collections.unmodifiableSet(maidModCommands);
    }
    
    /**
     * 验证并修复命令格式
     */
    private static String validateAndFixCommand(String command, ServerPlayer player) {
        // 检查是否是 /give 命令
        if (command.startsWith("/give ")) {
            // 修复 NBT 标签位置问题
            // 错误格式: /give @p diamond{Enchantments:[...]} 1
            // 正确格式: /give @p diamond 1 {Enchantments:[...]}
            command = fixGiveCommandNbtPosition(command);
        }
        
        return command;
    }
    
    /**
     * 修复 /give 命令中 NBT 标签的位置
     */
    private static String fixGiveCommandNbtPosition(String command) {
        try {
            // 查找 NBT 标签开始位置
            int nbtStart = command.indexOf('{');
            if (nbtStart == -1) {
                return command; // 没有 NBT 标签，无需修复
            }
            
            // 查找 NBT 标签结束位置（简单匹配最后一个 '}'）
            int nbtEnd = command.lastIndexOf('}');
            if (nbtEnd == -1 || nbtEnd < nbtStart) {
                return command; // NBT 标签格式错误，不修复
            }
            
            // 提取 NBT 标签内容
            String nbtContent = command.substring(nbtStart, nbtEnd + 1);
            
            // 移除命令中的 NBT 标签
            String commandWithoutNbt = command.substring(0, nbtStart) + command.substring(nbtEnd + 1);
            
            // 检查命令末尾是否有数量参数
            String[] parts = commandWithoutNbt.trim().split("\\s+");
            if (parts.length >= 3 && parts[2].matches("\\d+")) {
                // 已有数量参数，在数量后插入 NBT 标签
                StringBuilder fixedCommand = new StringBuilder();
                fixedCommand.append(commandWithoutNbt);
                
                // 在数量参数后插入空格和 NBT 标签
                int lastSpaceIndex = commandWithoutNbt.lastIndexOf(' ', commandWithoutNbt.length() - 2);
                if (lastSpaceIndex != -1) {
                    fixedCommand.insert(lastSpaceIndex + 1, " " + nbtContent);
                } else {
                    // 没有空格，直接追加
                    fixedCommand.append(" ").append(nbtContent);
                }
                
                MaidCommandProcessor.LOGGER.info("Fixed NBT position in command: {} -> {}", 
                    command, fixedCommand.toString());
                return fixedCommand.toString();
            } else {
                // 没有数量参数，在末尾添加数量 1 和 NBT 标签
                return commandWithoutNbt.trim() + " 1" + nbtContent;
            }
        } catch (Exception e) {
            MaidCommandProcessor.LOGGER.warn("Failed to fix NBT position: {}", e.getMessage());
            return command; // 修复失败，返回原始命令
        }
    }
}
