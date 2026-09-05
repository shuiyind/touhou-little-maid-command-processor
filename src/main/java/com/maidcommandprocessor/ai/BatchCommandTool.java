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
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;

import java.util.List;

public class BatchCommandTool implements ITool<BatchCommandTool.BatchResult> {
    
    public static final String TOOL_ID = "batch_command";
    private static final String TOOL_DESC = "Execute multiple Minecraft commands at once. This is more efficient for executing several related commands together.";
    
    public static final String PARAM_COMMANDS = "commands";
    public static final String PARAM_DESCRIPTION = "description";
    
    private static final Codec<BatchResult> CODEC = RecordCodecBuilder.create(instance -> instance.group(
        Codec.STRING.fieldOf(PARAM_COMMANDS).forGetter(BatchResult::commands),
        Codec.STRING.fieldOf(PARAM_DESCRIPTION).forGetter(BatchResult::description)
    ).apply(instance, BatchResult::new));
    
    public record BatchResult(String commands, String description) {}
    
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
            .setTitle("Batch Commands")
            .setDescription("Execute multiple Minecraft commands at once");
        
        StringParameter commandsParam = StringParameter.create()
            .setTitle("Commands")
            .setDescription("Multiple Minecraft commands separated by semicolons (;). Each command should include the slash. E.g., '/give @p diamond; /give @p emerald; /weather clear'");
        
        param.addProperties(PARAM_COMMANDS, commandsParam, true);
        
        StringParameter descParam = StringParameter.create()
            .setTitle("Description")
            .setDescription("Brief description of what these commands do");
        
        param.addProperties(PARAM_DESCRIPTION, descParam, false);
        
        return param;
    }
    
    @Override
    public Codec<BatchResult> codec() {
        return CODEC;
    }
    
    @Override
    public LLMCallback onCall(String toolId, BatchResult result, LLMCallback callback) {
        MaidCommandProcessor.LOGGER.info("BatchCommandTool called with commands: {}, description: {}", result.commands(), result.description());
        
        if (callback.getMaid() == null) {
            return callback.addToolResult(toolId, "Error: Maid is not valid");
        }
        
        EntityMaid maid = callback.getMaid();
        net.minecraft.world.entity.LivingEntity ownerEntity = maid.getOwner();
        
        if (!(ownerEntity instanceof ServerPlayer owner)) {
            return callback.addToolResult(toolId, "Error: Owner is not a valid player");
        }
        
        try {
            String[] commandArray = result.commands().split(";");
            java.util.List<String> commands = new java.util.ArrayList<>();
            for (String cmd : commandArray) {
                String trimmed = cmd.trim();
                if (!trimmed.isEmpty()) {
                    commands.add(trimmed);
                }
            }
            
            if (commands.isEmpty()) {
                return callback.addToolResult(toolId, "No valid commands found");
            }
            
            int successCount = CommandExecutorModule.executeBatchCommands(
                owner.createCommandSourceStack(),
                commands,
                owner,
                maid
            );
            
            String response = "Successfully executed " + successCount + "/" + commands.size() + " batch commands: " + result.description();
            MaidCommandProcessor.LOGGER.info(response);
            return callback.addToolResult(toolId, response);
        } catch (Exception e) {
            String error_msg = "Error executing batch commands: " + e.getMessage();
            MaidCommandProcessor.LOGGER.error(error_msg, e);
            return callback.addToolResult(toolId, error_msg);
        }
    }
    
    @Override
    public Component invocationSummaryComponent(BatchResult result) {
        return Component.literal("Executing batch commands: " + result.commands());
    }
}
