package com.maidcommandprocessor.ai;

import com.github.tartaricacid.touhoulittlemaid.ai.agent.tool.ITool;
import com.github.tartaricacid.touhoulittlemaid.ai.manager.entity.LLMCallback;
import com.github.tartaricacid.touhoulittlemaid.ai.service.function.schema.parameter.ObjectParameter;
import com.github.tartaricacid.touhoulittlemaid.ai.service.function.schema.parameter.Parameter;
import com.github.tartaricacid.touhoulittlemaid.ai.service.function.schema.parameter.StringParameter;
import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import com.maidcommandprocessor.MaidCommandProcessor;
import com.maidcommandprocessor.handler.PermissionModule;
import com.maidcommandprocessor.handler.PermissionModule.PermissionLevel;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.List;
import java.util.Optional;

public class PermissionTool implements ITool<PermissionTool.PermissionResult> {
    
    public static final String TOOL_ID = "manage_permission";
    private static final String TOOL_DESC = "Manage player permissions. Allows granting, revoking, or setting player permissions. Only ADMIN (Server Owner) can manage permissions.";
    
    public static final String PARAM_ACTION = "action";
    public static final String PARAM_PLAYER = "player";
    public static final String PARAM_LEVEL = "level";
    public static final String PARAM_DESCRIPTION = "description";
    
    private static final Codec<PermissionResult> CODEC = RecordCodecBuilder.create(instance -> instance.group(
        Codec.STRING.optionalFieldOf(PARAM_ACTION, "check").forGetter(PermissionResult::action),
        Codec.STRING.optionalFieldOf(PARAM_PLAYER, "").forGetter(PermissionResult::player),
        Codec.STRING.optionalFieldOf(PARAM_LEVEL, "").forGetter(PermissionResult::level),
        Codec.STRING.optionalFieldOf(PARAM_DESCRIPTION, "").forGetter(PermissionResult::description)
    ).apply(instance, PermissionResult::new));
    
    public record PermissionResult(String action, String player, String level, String description) {}
    
    @Override
    public String id() {
        return TOOL_ID;
    }
    
    @Override
    public String summary(EntityMaid maid) {
        return TOOL_DESC;
    }
    
    @Override
    public Parameter parameters(ObjectParameter parent, EntityMaid maid) {
        ObjectParameter param = ObjectParameter.create()
            .setTitle("Manage Player Permissions")
            .setDescription("Grant, revoke, or set player permissions. Only ADMIN can use this tool.");
        
        StringParameter actionParam = StringParameter.create()
            .setTitle("Action")
            .setDescription("Action to perform: grant (raise one level), revoke (remove permission), set (set specific level), check (query permission)");
        
        param.addProperties(PARAM_ACTION, actionParam, true);
        
        StringParameter playerParam = StringParameter.create()
            .setTitle("Player Name")
            .setDescription("Target player name. Empty for self or current player.");
        
        param.addProperties(PARAM_PLAYER, playerParam, false);
        
        StringParameter levelParam = StringParameter.create()
            .setTitle("Permission Level")
            .setDescription("Permission level: BASIC (初级管理), ADVANCED (管理员), ADMIN (服务器之主). Only used with 'set' action.");
        
        param.addProperties(PARAM_LEVEL, levelParam, false);
        
        StringParameter descParam = StringParameter.create()
            .setTitle("Description")
            .setDescription("Brief description of why this permission change is happening.");
        
        param.addProperties(PARAM_DESCRIPTION, descParam, false);
        
        return param;
    }
    
    @Override
    public Codec<PermissionResult> codec() {
        return CODEC;
    }
    
    @Override
    public LLMCallback onCall(String toolId, PermissionResult result, LLMCallback callback) {
        MaidCommandProcessor.LOGGER.info("PermissionTool called with action: {}, player: {}, level: {}", 
            result.action(), result.player(), result.level());
        
        if (callback.getMaid() == null) {
            return callback.addToolResult(toolId, "Error: Maid is not valid");
        }
        
        EntityMaid maid = callback.getMaid();
        LivingEntity ownerEntity = maid.getOwner();
        
        if (!(ownerEntity instanceof ServerPlayer owner)) {
            return callback.addToolResult(toolId, "Error: Owner is not a valid player");
        }
        
        PermissionLevel ownerLevel = PermissionModule.getPlayerPermission(owner);
        MaidCommandProcessor.LOGGER.info("Owner [{}] permission level: {}", owner.getName().getString(), ownerLevel.getName());
        
        // Only ADMIN can manage permissions
        if (ownerLevel != PermissionLevel.ADMIN) {
            return callback.addToolResult(toolId, 
                "Permission error: Only SERVER OWNER (ADMIN) can manage permissions. Your current level: " + ownerLevel.getName());
        }
        
        String action = result.action().toLowerCase();
        String targetName = result.player().isEmpty() ? owner.getName().getString() : result.player();
        
        MinecraftServer server = MaidCommandProcessor.server;
        if (server == null) {
            return callback.addToolResult(toolId, "Error: Server not available");
        }
        
        ServerPlayer targetPlayer = server.getPlayerList().getPlayerByName(targetName);
        if (targetPlayer == null) {
            return callback.addToolResult(toolId, "Player '" + targetName + "' not found or offline");
        }
        
        try {
            switch (action) {
                case "grant":
                    return handleGrant(toolId, owner, targetPlayer, result.description(), callback);
                    
                case "revoke":
                    return handleRevoke(toolId, owner, targetPlayer, result.description(), callback);
                    
                case "set":
                    return handleSet(toolId, owner, targetPlayer, result.level(), result.description(), callback);
                    
                case "check":
                    return handleCheck(toolId, targetPlayer, callback);
                    
                default:
                    return callback.addToolResult(toolId, 
                        "Unknown action: " + action + ". Valid actions: grant, revoke, set, check");
            }
        } catch (Exception e) {
            String errorMsg = "Error managing permission for '" + targetName + "': " + e.getMessage();
            MaidCommandProcessor.LOGGER.error(errorMsg, e);
            return callback.addToolResult(toolId, errorMsg);
        }
    }
    
    private LLMCallback handleGrant(String toolId, ServerPlayer grantor, ServerPlayer grantee, String description, LLMCallback callback) {
        if (!PermissionModule.canGrantPermission(grantor, grantee)) {
            return callback.addToolResult(toolId, 
                "Cannot grant permission: You are not ADMIN or target player is already at the same/higher level");
        }
        
        PermissionLevel currentLevel = PermissionModule.getPlayerPermission(grantee);
        int currentVal = currentLevel.getLevel();
        
        if (currentVal >= 3) {
            return callback.addToolResult(toolId, 
                "Player '" + grantee.getName().getString() + "' is already at maximum permission level");
        }
        
        PermissionLevel nextLevel = PermissionLevel.values()[currentVal + 1];
        String granteeName = grantee.getName().getString();
        PermissionModule.setPlayerPermission(grantor, grantee, nextLevel);
        
        String response = "Successfully granted permission to '" + granteeName + "': " + 
            currentLevel.getName() + " → " + nextLevel.getName() + "\n" + description;
        MaidCommandProcessor.LOGGER.info(response);
        
        return callback.addToolResult(toolId, response);
    }
    
    private LLMCallback handleRevoke(String toolId, ServerPlayer revoker, ServerPlayer revokee, String description, LLMCallback callback) {
        if (!PermissionModule.canRevokePermission(revoker, revokee)) {
            return callback.addToolResult(toolId, 
                "Cannot revoke permission: You are ADVANCED (管理员) and target must be BASIC (初级管理), or you are ADMIN");
        }
        
        String revokeeName = revokee.getName().getString();
        PermissionLevel currentLevel = PermissionModule.getPlayerPermission(revokee);
        
        if (currentLevel == PermissionLevel.NONE) {
            return callback.addToolResult(toolId, 
                "Player '" + revokeeName + "' already has no permission");
        }
        
        PermissionModule.revokePermission(revoker, revokee);
        
        String response = "Successfully revoked permission from '" + revokeeName + "': " + 
            currentLevel.getName() + " → NONE\n" + description;
        MaidCommandProcessor.LOGGER.info(response);
        
        return callback.addToolResult(toolId, response);
    }
    
    private LLMCallback handleSet(String toolId, ServerPlayer operator, ServerPlayer target, String levelStr, String description, LLMCallback callback) {
        if (levelStr == null || levelStr.isEmpty()) {
            return callback.addToolResult(toolId, "Please specify a permission level: BASIC, ADVANCED, or ADMIN");
        }
        
        PermissionLevel targetLevel;
        try {
            targetLevel = PermissionLevel.valueOf(levelStr.toUpperCase());
        } catch (IllegalArgumentException e) {
            return callback.addToolResult(toolId, 
                "Invalid permission level: '" + levelStr + "'. Valid levels: BASIC, ADVANCED, ADMIN");
        }
        
        String targetName = target.getName().getString();
        PermissionLevel currentLevel = PermissionModule.getPlayerPermission(target);
        
        boolean success = PermissionModule.setPlayerPermission(operator, target, targetLevel);
        
        if (!success) {
            String response = "Failed to set permission for '" + targetName + "': Operator is not ADMIN\n" + description;
            MaidCommandProcessor.LOGGER.warn(response);
            return callback.addToolResult(toolId, response);
        }
        
        String response = "Successfully set permission for '" + targetName + "': " + 
            currentLevel.getName() + " → " + targetLevel.getName() + "\n" + description;
        MaidCommandProcessor.LOGGER.info(response);
        
        return callback.addToolResult(toolId, response);
    }
    
    private LLMCallback handleCheck(String toolId, ServerPlayer target, LLMCallback callback) {
        PermissionLevel level = PermissionModule.getPlayerPermission(target);
        String targetName = target.getName().getString();
        
        // 构建清晰的权限信息消息
        String response = String.format(
            "=== 权限等级 ===\n" +
            "玩家 '%s' 的当前权限: %s (等级 %d)\n\n" +
            "权限说明:\n" +
            "- 无权限 (0): 不能执行任何命令\n" +
            "- 初级管理 (1): 可以使用天气、时间、传送等基础命令\n" +
            "- 管理员 (2): 可以执行 /op、/gamemode 等高级命令\n" +
            "- 服务器之主 (3): 拥有所有权限，可以管理其他玩家权限\n\n" +
            "如需提升权限，请联系服务器主人。",
            targetName,
            level.getName(),
            level.getLevel()
        );
        
        MaidCommandProcessor.LOGGER.info(response);
        
        return callback.addToolResult(toolId, response);
    }
    
    private String getPermissionDescription(PermissionLevel level) {
        switch (level) {
            case NONE:
                return "No permission - cannot execute any commands";
            case BASIC:
                return "初级管理 (Basic) - Can use: weather, time, tp, kill, give";
            case ADVANCED:
                return "管理员 (Advanced) - Can use all basic commands + /op, /deop, /gamemode, /gamerule, etc.";
            case ADMIN:
                return "服务器之主 (Admin) - Can use all commands and manage permissions";
            default:
                return "Unknown permission level";
        }
    }
    
    @Override
    public Component invocationSummaryComponent(PermissionResult result) {
        return Component.literal("Managing permission: " + result.action());
    }
}
