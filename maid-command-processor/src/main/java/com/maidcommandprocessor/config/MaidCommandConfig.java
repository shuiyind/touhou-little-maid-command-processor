package com.maidcommandprocessor.config;

import net.neoforged.neoforge.common.ModConfigSpec;

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
    
    public MaidCommandConfig() {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();
        
        // Permission settings
        builder.comment("Permission settings").push("permission");
        requirePermission = builder
            .comment("Require permission for maid command execution")
            .define("requirePermission", false);
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
}
