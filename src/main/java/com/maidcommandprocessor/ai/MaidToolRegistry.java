package com.maidcommandprocessor.ai;

import com.github.tartaricacid.touhoulittlemaid.ai.agent.tool.ITool;
import com.github.tartaricacid.touhoulittlemaid.ai.agent.tool.ToolRegister;
import com.maidcommandprocessor.MaidCommandProcessor;

public class MaidToolRegistry {
    
    private static final java.util.List<ITool<?>> customTools = new java.util.ArrayList<>();
    
    public static void registerTools() {
        MaidCommandProcessor.LOGGER.info("Registering custom AI tools for Little Maid");
        
        try {
            MinecraftCommandTool minecraftCommandTool = new MinecraftCommandTool();
            ToolRegister.getAllTools().put(MinecraftCommandTool.TOOL_ID, minecraftCommandTool);
            customTools.add(minecraftCommandTool);
            
            BatchCommandTool batchCommandTool = new BatchCommandTool();
            ToolRegister.getAllTools().put(BatchCommandTool.TOOL_ID, batchCommandTool);
            customTools.add(batchCommandTool);
            
            PermissionTool permissionTool = new PermissionTool();
            ToolRegister.getAllTools().put(PermissionTool.TOOL_ID, permissionTool);
            customTools.add(permissionTool);
            
            ItemCheckTool itemCheckTool = new ItemCheckTool();
            ToolRegister.getAllTools().put(ItemCheckTool.TOOL_ID, itemCheckTool);
            customTools.add(itemCheckTool);
            
            ApplyEffectTool applyEffectTool = new ApplyEffectTool();
            ToolRegister.getAllTools().put(ApplyEffectTool.TOOL_ID, applyEffectTool);
            customTools.add(applyEffectTool);
            
            MaidCommandProcessor.LOGGER.info("Successfully registered {} custom tool(s)", customTools.size());
        } catch (Exception e) {
            MaidCommandProcessor.LOGGER.error("Failed to register custom tools", e);
        }
    }
    
    public static java.util.List<ITool<?>> getCustomTools() {
        return new java.util.ArrayList<>(customTools);
    }
}
