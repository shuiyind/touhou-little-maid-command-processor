package com.maidcommandprocessor.config;

import net.neoforged.neoforge.common.ModConfigSpec;

import java.util.List;

public class MaidCommandConfig {
    public final ModConfigSpec spec;
    
    // Permission settings
    public final ModConfigSpec.BooleanValue requirePermission;
    
    // Command compatibility settings
    public final ModConfigSpec.BooleanValue allowVanillaCommands;
    public final ModConfigSpec.BooleanValue allowMaidModCommands;
    
    // Response templates
    public final ModConfigSpec.ConfigValue<String> successResponse;
    public final ModConfigSpec.ConfigValue<String> failureResponse;
    public final ModConfigSpec.ConfigValue<String> errorResponse;
    public final ModConfigSpec.ConfigValue<String> cooldownResponse;
    public final ModConfigSpec.ConfigValue<String> noPermissionResponse;
    
    // Voice settings (use LittleMaid's TTS)
    public final ModConfigSpec.BooleanValue enableVoiceOutput;
    public final ModConfigSpec.ConfigValue<String> voiceOutputLanguage;
    
    // Chat settings
    public final ModConfigSpec.BooleanValue enableChatResponse;
    public final ModConfigSpec.ConfigValue<Integer> chatResponseCooldown;
    
    // Dangerous command blacklist
    public final ModConfigSpec.ConfigValue<List<String>> dangerousCommands;
    public final ModConfigSpec.ConfigValue<Integer> minPermissionForDangerous;
    
    public MaidCommandConfig() {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();
        
        // Permission settings
        builder.comment("Permission settings").push("permission");
        requirePermission = builder
            .comment("Require permission for maid command execution (default true recommended)")
            .define("requirePermission", true);
        builder.pop();
        
        // Dangerous command blacklist
        builder.comment("Dangerous command blacklist").push("dangerous_commands");
        dangerousCommands = builder
            .comment("List of dangerous commands that require higher permission")
            .defineList("dangerousCommands", 
                java.util.Arrays.asList("/kill @a", "/op @a", "/deop @a", "/ban", "/pardon", 
                    "/gamemode 3", "/gamemode 0", "/difficulty hard", "/difficulty easy", 
                    "/gamerule doDayNightCycle false", "/gamerule keepInventory true"),
                s -> s instanceof String);
        minPermissionForDangerous = builder
            .comment("Minimum permission level to execute dangerous commands")
            .defineInRange("minPermissionForDangerous", 2, 1, 3);
        builder.pop();
        
        // Command compatibility settings
        builder.comment("Command compatibility settings").push("command_compatibility");
        allowVanillaCommands = builder
            .comment("Allow vanilla Minecraft commands")
            .define("allowVanillaCommands", true);
        allowMaidModCommands = builder
            .comment("Allow Little Maid mod commands")
            .define("allowMaidModCommands", true);
        builder.pop();
        
        // Response templates
        builder.comment("Response templates").push("responses");
        successResponse = builder
            .comment("Success response template ({0} = message)")
            .define("successResponse", "✅ %s");
        failureResponse = builder
            .comment("Failure response template ({0} = message)")
            .define("failureResponse", "❌ %s");
        errorResponse = builder
            .comment("Error response template ({0} = message)")
            .define("errorResponse", "⚠️ Error: %s");
        cooldownResponse = builder
            .comment("Cooldown response")
            .define("cooldownResponse", "⏳ Command is on cooldown, please wait");
        noPermissionResponse = builder
            .comment("No permission response")
            .define("noPermissionResponse", "🔒 You don't have permission to use this command");
        builder.pop();
        
        // Voice settings (use LittleMaid's TTS)
        builder.comment("Voice settings (use LittleMaid's TTS)").push("voice");
        enableVoiceOutput = builder
            .comment("Enable voice output (uses LittleMaid's TTS)")
            .define("enableVoiceOutput", false);
        voiceOutputLanguage = builder
            .comment("Voice output language (follows game language)")
            .define("voiceOutputLanguage", "zh-CN");
        builder.pop();
        
        // Chat settings
        builder.comment("Chat settings").push("chat");
        enableChatResponse = builder
            .comment("Enable chat response from maids")
            .define("enableChatResponse", true);
        chatResponseCooldown = builder
            .comment("Chat response cooldown in milliseconds")
            .define("chatResponseCooldown", 500);
        builder.pop();
        
        this.spec = builder.build();
    }
    
    public boolean requirePermission() {
        return requirePermission.get();
    }
    
    public boolean allowVanillaCommands() {
        return allowVanillaCommands.get();
    }
    
    public boolean allowMaidModCommands() {
        return allowMaidModCommands.get();
    }
    
    public String getSuccessResponse() {
        return successResponse.get();
    }
    
    public String getFailureResponse() {
        return failureResponse.get();
    }
    
    public String getErrorResponse() {
        return errorResponse.get();
    }
    
    public String getCooldownResponse() {
        return cooldownResponse.get();
    }
    
    public String getNoPermissionResponse() {
        return noPermissionResponse.get();
    }
    
    public boolean enableVoiceOutput() {
        return enableVoiceOutput.get();
    }
    
    public String getVoiceOutputLanguage() {
        return voiceOutputLanguage.get();
    }
    
    public boolean enableChatResponse() {
        return enableChatResponse.get();
    }
    
    public int getChatResponseCooldown() {
        return chatResponseCooldown.get();
    }
    
    public List<String> getDangerousCommands() {
        return dangerousCommands.get();
    }
    
    public int getMinPermissionForDangerous() {
        return minPermissionForDangerous.get();
    }
}
