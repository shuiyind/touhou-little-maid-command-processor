package com.maidcommandprocessor.ai;

import com.maidcommandprocessor.MaidCommandProcessor;
import com.maidcommandprocessor.config.MaidCommandConfig;
import com.maidcommandprocessor.handler.PermissionModule;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

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
    
    private static final Map<String, IntentType> commandPatterns = new ConcurrentHashMap<>();
    private static final Map<String, String> responsePatterns = new ConcurrentHashMap<>();
    
    // 多语言关键词映射
    private static final Map<String, List<String>> languageKeywords = new HashMap<>();
    private static final Map<String, Map<String, String>> intentCommands = new ConcurrentHashMap<>();
    
    static {
        initializePatterns();
        initializeMultilingualKeywords();
    }
    
    private static void initializePatterns() {
        // Command patterns (Chinese)
        commandPatterns.put("天气", IntentType.COMMAND_EXECUTION);
        commandPatterns.put("时间", IntentType.COMMAND_EXECUTION);
        commandPatterns.put("给予", IntentType.ITEM_GIVE);
        commandPatterns.put("传送", IntentType.COMMAND_EXECUTION);
        commandPatterns.put("死亡", IntentType.COMMAND_EXECUTION);
        
        // Response patterns (Chinese)
        responsePatterns.put("好的", "好的，主人！");
        responsePatterns.put("明白了", "明白了，主人！");
        responsePatterns.put("知道", "知道啦，主人！");
    }
    
    private static void initializeMultilingualKeywords() {
        // 天气相关
        List<String> weatherKeywords = Arrays.asList(
            "天气", "晴天", "雨天", "雷暴",  // Chinese
            "weather", "sunny", "rainy", "thunder",  // English
            "天気", "晴れ", "雨", "雷雨"  // Japanese
        );
        for (String kw : weatherKeywords) {
            commandPatterns.put(kw, IntentType.COMMAND_EXECUTION);
        }
        
        // 时间相关
        List<String> timeKeywords = Arrays.asList(
            "时间", "白天", "晚上", "夜晚",  // Chinese
            "time", "day", "night",  // English
            "時間", "昼", "夜"  // Japanese
        );
        for (String kw : timeKeywords) {
            commandPatterns.put(kw, IntentType.COMMAND_EXECUTION);
        }
        
        // 给予相关
        List<String> giveKeywords = Arrays.asList(
            "给予", "给", "道具", "装备",  // Chinese
            "give", "item", "equipment", "weapon",  // English
            "与える", "アイテム", "装備"  // Japanese
        );
        for (String kw : giveKeywords) {
            commandPatterns.put(kw, IntentType.ITEM_GIVE);
        }
        
        // 传送相关
        List<String> teleportKeywords = Arrays.asList(
            "传送", "移动", "去",  // Chinese
            "teleport", "move", "go",  // English
            "テレポート", "移動"  // Japanese
        );
        for (String kw : teleportKeywords) {
            commandPatterns.put(kw, IntentType.COMMAND_EXECUTION);
        }
        
        // 响应模式（多语言）
        responsePatterns.put("好的", "好的，主人！");
        responsePatterns.put("了解", "了解了，主人！");
        responsePatterns.put("ok", "OK, master!");
        responsePatterns.put("okay", "Okay, master!");
        responsePatterns.put("わかった", "わかりました、主人！");
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
        // 天气命令（支持多语言）
        if (text.contains("晴天") || text.contains("晴朗") || text.contains("sunny")) return "weather clear";
        if (text.contains("雨天") || text.contains("下雨") || text.contains("rainy") || text.contains("rain")) return "weather rain";
        if (text.contains("雷暴") || text.contains("雷雨") || text.contains("thunder")) return "weather thunder";
        
        // 时间命令（支持多语言）
        if (text.contains("白天") || text.contains("day")) return "time set day";
        if (text.contains("晚上") || text.contains("夜晚") || text.contains("night")) return "time set night";
        
        // 传送命令
        if (text.contains("传送") || text.contains("teleport")) return "tp @s @s";
        
        // 死亡/清除命令
        if (text.contains("死亡") || text.contains("kill")) return "kill @s";
        
        return "tell @p Command executed";
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
