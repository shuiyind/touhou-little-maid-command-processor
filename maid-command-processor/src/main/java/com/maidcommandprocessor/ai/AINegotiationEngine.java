package com.maidcommandprocessor.ai;

import com.maidcommandprocessor.MaidCommandProcessor;
import com.maidcommandprocessor.config.MaidCommandConfig;
import com.maidcommandprocessor.handler.PermissionModule;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.*;

public class AINegotiationEngine {
    
    // Intent types for maid command execution
    public enum IntentType {
        COMMAND_EXECUTION,
        ITEM_GIVE,
        CHAT_RESPONSE,
        UNKNOWN
    }
    
    // Command intent parsed from AI
    public static class CommandIntent {
        private final IntentType type;
        private final String command;
        private final String response;
        private final ItemStack item;
        
        public CommandIntent(IntentType type, String command, String response, ItemStack item) {
            this.type = type;
            this.command = command;
            this.response = response;
            this.item = item;
        }
        
        public IntentType getType() { return type; }
        public String getCommand() { return command; }
        public String getResponse() { return response; }
        public ItemStack getItem() { return item; }
        
        public boolean isExecuteCommand() {
            return type == IntentType.COMMAND_EXECUTION;
        }
        
        public boolean isItemGive() {
            return type == IntentType.ITEM_GIVE;
        }
    }
    
    private static final Map<String, IntentType> commandPatterns = new HashMap<>();
    private static final Map<String, String> responsePatterns = new HashMap<>();
    
    static {
        initializePatterns();
    }
    
    private static void initializePatterns() {
        // Command patterns
        commandPatterns.put("天气", IntentType.COMMAND_EXECUTION);
        commandPatterns.put("时间", IntentType.COMMAND_EXECUTION);
        commandPatterns.put("给予", IntentType.ITEM_GIVE);
        commandPatterns.put("传送", IntentType.COMMAND_EXECUTION);
        commandPatterns.put("死亡", IntentType.COMMAND_EXECUTION);
        
        // Response patterns
        responsePatterns.put("好的", "好的，主人！");
        responsePatterns.put("明白了", "明白了，主人！");
        responsePatterns.put("知道", "知道啦，主人！");
    }
    
    public static CommandIntent parseIntent(String chatText, PermissionModule.PermissionLevel permissionLevel) {
        MaidCommandConfig config = MaidCommandProcessor.config;
        
        if (!config.enableChatResponse()) {
            return null;
        }
        
        chatText = chatText.toLowerCase();
        
        // Check for command execution
        for (Map.Entry<String, IntentType> entry : commandPatterns.entrySet()) {
            if (chatText.contains(entry.getKey())) {
                MaidCommandProcessor.LOGGER.info(
                    "Parsed command intent from: {}", chatText
                );
                
                if (entry.getValue() == IntentType.COMMAND_EXECUTION) {
                    String command = extractCommand(chatText);
                    return new CommandIntent(
                        IntentType.COMMAND_EXECUTION,
                        command,
                        "好的，主人！正在执行命令：" + command,
                        null
                    );
                } else if (entry.getValue() == IntentType.ITEM_GIVE) {
                    String itemName = extractItemName(chatText);
                    ItemStack item = extractItemStack(itemName);
                    return new CommandIntent(
                        IntentType.ITEM_GIVE,
                        "give @p " + item.getItem().toString() + " 1",
                        "好的，主人！给您：" + item.getDisplayName().getString(),
                        item
                    );
                }
            }
        }
        
        // Check for chat response
        for (Map.Entry<String, String> entry : responsePatterns.entrySet()) {
            if (chatText.contains(entry.getKey())) {
                MaidCommandProcessor.LOGGER.info(
                    "Parsed chat response from: {}", chatText
                );
                return new CommandIntent(
                    IntentType.CHAT_RESPONSE,
                    null,
                    entry.getValue(),
                    null
                );
            }
        }
        
        return null;
    }
    
    private static String extractCommand(String text) {
        if (text.contains("晴天")) return "weather clear";
        if (text.contains("雨天")) return "weather rain";
        if (text.contains("雷暴")) return "weather thunder";
        if (text.contains("白天")) return "time set day";
        if (text.contains("晚上")) return "time set night";
        if (text.contains("传送")) return "tp @s @s";
        if (text.contains("死亡")) return "kill @s";
        return "tell @p 命令已执行";
    }
    
    private static String extractItemName(String text) {
        if (text.contains("钻石")) return "diamond";
        if (text.contains("钻石剑")) return "diamond_sword";
        if (text.contains("钻石盔甲")) return "diamond_chestplate";
        if (text.contains("金")) return "gold_ingot";
        if (text.contains("经验瓶")) return "experience_bottle";
        return "diamond";
    }
    
    private static ItemStack extractItemStack(String itemName) {
        switch (itemName) {
            case "diamond_sword":
                return new ItemStack(Items.DIAMOND_SWORD);
            case "diamond_chestplate":
                return new ItemStack(Items.DIAMOND_CHESTPLATE);
            case "gold_ingot":
                return new ItemStack(Items.GOLD_INGOT);
            case "experience_bottle":
                return new ItemStack(Items.EXPERIENCE_BOTTLE);
            default:
                return new ItemStack(Items.DIAMOND);
        }
    }
    
    public static List<String> getAvailableCommands() {
        return new ArrayList<>(commandPatterns.keySet());
    }
    
    public static List<String> getAvailableResponses() {
        return new ArrayList<>(responsePatterns.keySet());
    }
}
