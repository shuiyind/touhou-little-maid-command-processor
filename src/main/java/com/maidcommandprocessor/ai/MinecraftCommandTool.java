package com.maidcommandprocessor.ai;

import com.github.tartaricacid.touhoulittlemaid.ai.agent.tool.ITool;
import com.github.tartaricacid.touhoulittlemaid.ai.manager.entity.LLMCallback;
import com.github.tartaricacid.touhoulittlemaid.ai.service.function.schema.parameter.ObjectParameter;
import com.github.tartaricacid.touhoulittlemaid.ai.service.function.schema.parameter.Parameter;
import com.github.tartaricacid.touhoulittlemaid.ai.service.function.schema.parameter.StringParameter;
import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.maidcommandprocessor.MaidCommandProcessor;
import com.maidcommandprocessor.handler.CommandExecutorModule;
import com.maidcommandprocessor.handler.CommandQueueModule;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;

import java.util.List;

public class MinecraftCommandTool implements ITool<MinecraftCommandTool.CommandResult> {
    
    public static final String TOOL_ID = "minecraft_command";
    private static final String TOOL_DESC = "Execute a Minecraft command. This allows the maid to perform actions like changing weather, giving items, teleporting, etc.";
    
    public static final String PARAM_COMMAND = "command";
    public static final String PARAM_DESCRIPTION = "description";
    
    private static final Codec<CommandResult> CODEC = RecordCodecBuilder.create(instance -> instance.group(
        Codec.STRING.fieldOf(PARAM_COMMAND).forGetter(CommandResult::command),
        Codec.STRING.fieldOf(PARAM_DESCRIPTION).forGetter(CommandResult::description)
    ).apply(instance, CommandResult::new));
    
    public record CommandResult(String command, String description) {}
    
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
            .setTitle("Minecraft Command")
            .setDescription("Execute a Minecraft command");
        
        StringParameter commandParam = StringParameter.create()
            .setTitle("Command")
            .setDescription("The Minecraft command to execute, including the slash. E.g., '/weather clear', '/give @p diamond'");
        
        param.addProperties(PARAM_COMMAND, commandParam, true);
        
        StringParameter descParam = StringParameter.create()
            .setTitle("Description")
            .setDescription("Brief description of what this command does");
        
        param.addProperties(PARAM_DESCRIPTION, descParam, false);
        
        return param;
    }
    
    @Override
    public Codec<CommandResult> codec() {
        return CODEC;
    }
    
    @Override
    public LLMCallback onCall(String toolId, CommandResult result, LLMCallback callback) {
        MaidCommandProcessor.LOGGER.info("MinecraftCommandTool called with command: {}, description: {}", result.command(), result.description());
        
        if (callback.getMaid() == null) {
            return callback.addToolResult(toolId, "Error: Maid is not valid");
        }
        
        EntityMaid maid = callback.getMaid();
        net.minecraft.world.entity.LivingEntity ownerEntity = maid.getOwner();
        
        if (!(ownerEntity instanceof ServerPlayer owner)) {
            return callback.addToolResult(toolId, "Error: Owner is not a valid player");
        }
        
        try {
            MaidCommandProcessor.LOGGER.info(
                "Maid [{}] executing command [{}] by player [{}]",
                maid.getUUID(), result.command(), owner.getName().getString()
            );
            
            MaidCommandProcessor.LOGGER.info(
                "Executing command: [{}] with source: {}",
                result.command(), owner.createCommandSourceStack()
            );
            
            int result_code = CommandExecutorModule.executeCommand(
                owner.createCommandSourceStack(),
                result.command(),
                owner,
                maid
            );
            
            if (result_code > 0) {
                String response = "Successfully executed command: " + result.command() + "\n" + result.description();
                MaidCommandProcessor.LOGGER.info(response);
                return callback.addToolResult(toolId, response);
            } else {
                if (CommandExecutorModule.isInCooldown(maid.getUUID())) {
                    CommandQueueModule.addPendingCommand(
                        maid.getUUID(),
                        result.command(),
                        result.description(),
                        owner,
                        maid
                    );
                    String response = "Command queued for batch execution: " + result.command() + "\n" + result.description();
                    MaidCommandProcessor.LOGGER.info(response);
                    return callback.addToolResult(toolId, response);
                } else {
                    String response = "Command execution failed: " + result.command();
                    MaidCommandProcessor.LOGGER.warn(response);
                    return callback.addToolResult(toolId, response);
                }
            }
        } catch (Exception e) {
            String error_msg = "Error executing command '" + result.command() + "': " + e.getMessage();
            MaidCommandProcessor.LOGGER.error(error_msg, e);
            return callback.addToolResult(toolId, error_msg);
        }
    }
    
    @Override
    public Component invocationSummaryComponent(CommandResult result) {
        return Component.literal("Executing: " + result.command());
    }
}
