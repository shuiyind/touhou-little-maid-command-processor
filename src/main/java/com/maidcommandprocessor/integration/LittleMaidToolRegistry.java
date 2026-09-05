package com.maidcommandprocessor.integration;

import com.maidcommandprocessor.MaidCommandProcessor;
import com.maidcommandprocessor.config.MaidCommandConfig;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class LittleMaidToolRegistry {
    
    private static final Map<String, MaidTool> registeredTools = new ConcurrentHashMap<>();
    private static boolean initialized = false;
    
    public static class MaidTool {
        private final String name;
        private final String description;
        private final String command;
        private final String example;
        private final boolean enabled;
        
        public MaidTool(String name, String description, String command, String example, boolean enabled) {
            this.name = name;
            this.description = description;
            this.command = command;
            this.example = example;
            this.enabled = enabled;
        }
        
        public String getName() { return name; }
        public String getDescription() { return description; }
        public String getCommand() { return command; }
        public String getExample() { return example; }
        public boolean isEnabled() { return enabled; }
    }
    
    public static void initialize() {
        MaidCommandConfig config = MaidCommandProcessor.config;
        
        // Register vanilla commands as tools
        registerTool(
            "weather_command",
            "Change weather",
            "weather clear",
            "Make it sunny",
            config.allowVanillaCommands.get()
        );
        
        registerTool(
            "time_command",
            "Change time",
            "time set day",
            "Change time to day",
            config.allowVanillaCommands.get()
        );
        
        registerTool(
            "give_command",
            "Give items",
            "give @p diamond",
            "Give me diamonds",
            config.allowVanillaCommands.get()
        );
        
        registerTool(
            "tp_command",
            "Teleport",
            "tp @s 100 64 100",
            "Teleport to coordinates",
            config.allowVanillaCommands.get()
        );
        
        registerTool(
            "kill_command",
            "Kill entities",
            "kill @e[type=creeper]",
            "Kill all creepers",
            config.allowVanillaCommands.get()
        );
        
        registerTool(
            "summon_command",
            "Summon entities",
            "summon cow",
            "Summon a cow",
            config.allowVanillaCommands.get()
        );
        
        registerTool(
            "setblock_command",
            "Place blocks",
            "setblock ~ ~ ~ diamond_block",
            "Place a diamond block",
            config.allowVanillaCommands.get()
        );
        
        registerTool(
            "execute_command",
            "Execute commands",
            "execute as @a run say Hello",
            "Execute command for all players",
            config.allowVanillaCommands.get()
        );
        
        initialized = true;
        MaidCommandProcessor.LOGGER.info("LittleMaid Tool Registry initialized");
        MaidCommandProcessor.LOGGER.info("Registered {} tools", registeredTools.size());
    }
    
    public static void registerTool(String name, String description, String command, String example, boolean enabled) {
        MaidTool tool = new MaidTool(name, description, command, example, enabled);
        registeredTools.put(name, tool);
        MaidCommandProcessor.LOGGER.info("Registered tool: {} - {}", name, description);
    }
    
    public static void registerTools() {
        if (initialized) {
            MaidCommandProcessor.LOGGER.info("LittleMaid tools already registered, skipping duplicate registration");
            return;
        }
        
        MaidCommandProcessor.LOGGER.info("Registering tools with LittleMaid AI system");
        
        // In a real implementation, this would register tools with LittleMaid's AI system
        // For now, we just log the tools
        for (Map.Entry<String, MaidTool> entry : registeredTools.entrySet()) {
            MaidTool tool = entry.getValue();
            if (tool.isEnabled()) {
                MaidCommandProcessor.LOGGER.info(
                    "Tool available: {} - {} (Example: {})",
                    tool.getName(), tool.getDescription(), tool.getExample()
                );
            }
        }
        
        initialized = true;
    }
    
    public static MaidTool getTool(String name) {
        return registeredTools.get(name);
    }
    
    public static List<MaidTool> getEnabledTools() {
        List<MaidTool> enabledTools = new ArrayList<>();
        for (MaidTool tool : registeredTools.values()) {
            if (tool.isEnabled()) {
                enabledTools.add(tool);
            }
        }
        return enabledTools;
    }
    
    public static Map<String, MaidTool> getAllTools() {
        return registeredTools;
    }
    
    public static boolean isInitialized() {
        return initialized;
    }
}
