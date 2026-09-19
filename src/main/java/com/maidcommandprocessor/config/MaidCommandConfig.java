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
    public final ModConfigSpec.ConfigValue<List<? extends String>> dangerousCommands;
    public final ModConfigSpec.ConfigValue<Integer> minPermissionForDangerous;

    public MaidCommandConfig() {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();

        // Permission settings
        builder.comment("Permission settings\n权限设置").push("permission");
        requirePermission = builder
            .comment("Require permission for maid command execution (default true recommended)\n执行女仆指令前是否需要权限验证（推荐设为 true）")
            .define("requirePermission", true);
        builder.pop();

        // Dangerous command blacklist
        builder.comment("Dangerous command blacklist\n危险指令黑名单").push("dangerous_commands");
        java.util.List<String> defaultDangerousCommands = java.util.Arrays.asList(
            "/kill @a", "/op @a", "/deop @a", "/ban", "/pardon",
            "/gamemode 3", "/gamemode 0", "/difficulty hard", "/difficulty easy",
            "/gamerule doDayNightCycle false", "/gamerule keepInventory true");
        dangerousCommands = builder
            .comment("List of dangerous commands that require higher permission\n需要更高权限才能执行的危险指令列表")
            .defineListAllowEmpty("dangerousCommands", defaultDangerousCommands, true);
        minPermissionForDangerous = builder
            .comment("Minimum permission level to execute dangerous commands\n执行危险指令所需的最低权限等级")
            .defineInRange("minPermissionForDangerous", 2, 1, 3);
        builder.pop();

        // Command compatibility settings
        builder.comment("Command compatibility settings\n指令兼容性设置").push("command_compatibility");
        allowVanillaCommands = builder
            .comment("Allow vanilla Minecraft commands\n允许使用原版 Minecraft 指令")
            .define("allowVanillaCommands", true);
        allowMaidModCommands = builder
            .comment("Allow Little Maid mod commands\n允许使用 Little Maid 模组自带指令")
            .define("allowMaidModCommands", true);
        builder.pop();

        // Response templates
        builder.comment("Response templates\n响应模板设置").push("responses");
        successResponse = builder
            .comment("Success response template ({0} = message)\n成功时的响应模板（{0} 为消息内容）")
            .define("successResponse", "✅ %s");
        failureResponse = builder
            .comment("Failure response template ({0} = message)\n失败时的响应模板（{0} 为消息内容）")
            .define("failureResponse", "❌ %s");
        errorResponse = builder
            .comment("Error response template ({0} = message)\n出错时的响应模板（{0} 为错误信息）")
            .define("errorResponse", "⚠️ Error: %s");
        cooldownResponse = builder
            .comment("Cooldown response\n冷却中的提示回复")
            .define("cooldownResponse", "⏳ Command is on cooldown, please wait\n⏳ 指令冷却中，请稍候");
        noPermissionResponse = builder
            .comment("No permission response\n无权限时的提示回复")
            .define("noPermissionResponse", "🔒 You don't have permission to use this command\n🔒 你没有权限使用此指令");
        builder.pop();

        // Voice settings (use LittleMaid's TTS)
        builder.comment("Voice settings (use LittleMaid's TTS)\n语音输出设置（使用 Little Maid 的 TTS）").push("voice");
        enableVoiceOutput = builder
            .comment("Enable voice output (uses LittleMaid's TTS)\n启用语音输出功能（使用 Little Maid 的语音合成）")
            .define("enableVoiceOutput", false);
        voiceOutputLanguage = builder
            .comment("Voice output language (follows game language)\n语音输出语言（跟随游戏语言设置）")
            .define("voiceOutputLanguage", "zh-CN");
        builder.pop();

        // Chat settings
        builder.comment("Chat settings\n聊天设置").push("chat");
        enableChatResponse = builder
            .comment("Enable chat response from maids\n启用女仆的聊天响应功能")
            .define("enableChatResponse", true);
        chatResponseCooldown = builder
            .comment("Chat response cooldown in milliseconds\n聊天响应冷却时间（毫秒）")
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

    public List<? extends String> getDangerousCommands() {
        return dangerousCommands.get();
    }

    public int getMinPermissionForDangerous() {
        return minPermissionForDangerous.get();
    }
}
