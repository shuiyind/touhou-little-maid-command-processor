package com.maidcommandprocessor.integration;

import com.maidcommandprocessor.MaidCommandProcessor;
import com.maidcommandprocessor.config.MaidCommandConfig;
import com.maidcommandprocessor.handler.CommandExecutorModule;
import com.maidcommandprocessor.handler.PermissionModule;
import com.maidcommandprocessor.handler.PermissionModule.PermissionLevel;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class AIChatIntegration {
    
    private static final Map<String, CommandPattern> commandPatterns = new LinkedHashMap<>();
    // CodeQl[empty-container] - itemPatterns is populated in initializePatterns()
    @SuppressWarnings("unused")
    private static final Map<String, ItemPattern> itemPatterns = new LinkedHashMap<>();
    
    static {
        initializePatterns();
    }
    
    public static class CommandPattern {
        private final String pattern;
        private final String command;
        private final String description;
        private final PermissionLevel requiredPermission;
        
        public CommandPattern(String pattern, String command, String description, PermissionLevel requiredPermission) {
            this.pattern = pattern;
            this.command = command;
            this.description = description;
            this.requiredPermission = requiredPermission;
        }
        
        public String getPattern() { return pattern; }
        public String getCommand() { return command; }
        public String getDescription() { return description; }
        public PermissionLevel getRequiredPermission() { return requiredPermission; }
    }
    
    public static class ItemPattern {
        private final String pattern;
        private final String itemName;
        private final String description;
        
        public ItemPattern(String pattern, String itemName, String description) {
            this.pattern = pattern;
            this.itemName = itemName;
            this.description = description;
        }
        
        public String getPattern() { return pattern; }
        public String getItemName() { return itemName; }
        public String getDescription() { return description; }
    }
    
    private static void initializePatterns() {
        commandPatterns.put("weather", new CommandPattern(
            "(?:让|make|变成|change to|天气|weather)\\s*(?:晴|天晴|晴天|clear|sun)",
            "weather clear",
            "Change weather to clear/sunny",
            PermissionLevel.BASIC
        ));
        
        commandPatterns.put("weather_rain", new CommandPattern(
            "(?:让|make|变成|change to|天气|weather)\\s*(?:雨|下雨|rain)",
            "weather rain",
            "Change weather to rain",
            PermissionLevel.BASIC
        ));
        
        commandPatterns.put("time_day", new CommandPattern(
            "(?:让|make|变成|change to|时间|time)\\s*(?:白天|day|白天模式)",
            "time set day",
            "Set time to day",
            PermissionLevel.BASIC
        ));
        
        commandPatterns.put("time_night", new CommandPattern(
            "(?:让|make|变成|change to|时间|time)\\s*(?:晚上|night|夜晚|黑夜)",
            "time set night",
            "Set time to night",
            PermissionLevel.BASIC
        ));
        
        commandPatterns.put("give_item", new CommandPattern(
            "(?:给|give)\\s*(?:我|me)?\\s*(?:好的|good|高级|advanced|强力|powerful)?\\s*(?:装备|equipment|物品|item)",
            "give @p diamond_armor",
            "Give good equipment",
            PermissionLevel.ADVANCED
        ));
        
        commandPatterns.put("execute_command", new CommandPattern(
            "(?:执行|execute|让|make)\\s*(?:.*?)\\s*(?:的|'s|的|s)?\\s*(?:指令|command|命令|cmd)",
            "COMMAND_PLACEHOLDER",
            "Execute arbitrary command",
            PermissionLevel.ADVANCED
        ));
        
        itemPatterns.put("diamond_sword", new ItemPattern(
            "(?:钻石|diamond)\\s*(?:剑|sword|武器)",
            "diamond_sword",
            "Diamond sword pattern"
        ));
        
        itemPatterns.put("iron_armor", new ItemPattern(
            "(?:铁|iron)\\s*(?:甲|armor|装备)",
            "iron_armor",
            "Iron armor pattern"
        ));
        
        itemPatterns.put("gold_food", new ItemPattern(
            "(?:金|gold)\\s*(?:食物|food|面包)",
            "gold_ingot",
            "Gold food pattern"
        ));
        
        MaidCommandProcessor.LOGGER.info("Initialized {} command patterns and {} item patterns", 
            commandPatterns.size(), itemPatterns.size());
    }
    
    public static ChatResponse processChatMessage(
            String chatText,
            ServerPlayer player,
            Entity maidEntity) {
        
        MaidCommandConfig config = MaidCommandProcessor.config;
        
        if (!config.enableChatResponse()) {
            return new ChatResponse(false, "AI Chat integration disabled", null);
        }
        
        PermissionLevel playerPermission = PermissionModule.getPlayerPermission(player);
        
        for (Map.Entry<String, CommandPattern> entry : commandPatterns.entrySet()) {
            CommandPattern pattern = entry.getValue();
            
            if (playerPermission.getLevel() < pattern.getRequiredPermission().getLevel()) {
                continue;
            }
            
            Matcher matcher = Pattern.compile(pattern.getPattern(), Pattern.CASE_INSENSITIVE).matcher(chatText);
            
            if (matcher.matches()) {
                String command = pattern.getCommand();
                
                if (command.equals("give @p diamond_armor")) {
                    command = generateGoodEquipmentCommand(player);
                }
                
                MaidCommandProcessor.LOGGER.info(
                    "Matched pattern [{}] for chat: {}",
                    entry.getKey(), chatText
                );
                
                return new ChatResponse(true, pattern.getDescription(), command);
            }
        }
        
        return new ChatResponse(false, "No matching command found", null);
    }
    
    private static String generateGoodEquipmentCommand(ServerPlayer player) {
        String playerName = player.getName().getString();
        return "give " + playerName + " diamond_armor";
    }
    
    public static Optional<String> extractItemName(String chatText) {
        // Simple pattern matching for item names
        // In a real implementation, this would use AI or a more sophisticated parsing
        
        String[] itemKeywords = {"diamond", "iron", "gold", "netherite", "sword", "armor", "pickaxe"};
        
        for (String keyword : itemKeywords) {
            if (chatText.toLowerCase().contains(keyword)) {
                return Optional.of(keyword);
            }
        }
        
        return Optional.empty();
    }
    
    public static List<CommandPattern> getCommandPatterns() {
        return Collections.unmodifiableList(new ArrayList<>(commandPatterns.values()));
    }
    
    public static List<ItemPattern> getItemPatterns() {
        return Collections.unmodifiableList(new ArrayList<>(itemPatterns.values()));
    }
    
    public static class ChatResponse {
        private final boolean success;
        private final String message;
        private final String command;
        
        public ChatResponse(boolean success, String message, String command) {
            this.success = success;
            this.message = message;
            this.command = command;
        }
        
        public boolean isSuccess() { return success; }
        public String getMessage() { return message; }
        public String getCommand() { return command; }
    }
}
