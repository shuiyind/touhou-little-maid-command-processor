package com.maidcommandprocessor.ai;

import com.github.tartaricacid.touhoulittlemaid.ai.agent.tool.ITool;
import com.github.tartaricacid.touhoulittlemaid.ai.manager.entity.LLMCallback;
import com.github.tartaricacid.touhoulittlemaid.ai.service.function.schema.parameter.ObjectParameter;
import com.github.tartaricacid.touhoulittlemaid.ai.service.function.schema.parameter.Parameter;
import com.github.tartaricacid.touhoulittlemaid.ai.service.function.schema.parameter.StringParameter;
import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import com.maidcommandprocessor.MaidCommandProcessor;
import com.maidcommandprocessor.config.ModRegistryManager;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.network.chat.Component;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.List;

public class ItemCheckTool implements ITool<ItemCheckTool.CheckResult> {
    
    public static final String TOOL_ID = "check_item";
    private static final String TOOL_DESC = "Check if an item or enchantment exists in the current modpack. Returns success if found, error if not.";
    
    public static final String PARAM_ITEM_ID = "item_id";
    public static final String PARAM_TYPE = "type";
    public static final String PARAM_DESCRIPTION = "description";
    
    private static final Codec<CheckResult> CODEC = RecordCodecBuilder.create(instance -> instance.group(
        Codec.STRING.fieldOf(PARAM_ITEM_ID).forGetter(CheckResult::itemId),
        Codec.STRING.optionalFieldOf(PARAM_TYPE, "item").forGetter(CheckResult::type),
        Codec.STRING.optionalFieldOf(PARAM_DESCRIPTION, "").forGetter(CheckResult::description)
    ).apply(instance, CheckResult::new));
    
    public record CheckResult(String itemId, String type, String description) {}
    
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
            .setTitle("Check Item/Enchantment Existence")
            .setDescription("Verify if an item or enchantment exists in the current modpack before using it.");
        
        StringParameter itemIdParam = StringParameter.create()
            .setTitle("Item/Enchantment ID")
            .setDescription("Full resource ID of the item or enchantment (e.g., 'minecraft:diamond', 'mysticalagriculture:ignium', 'minecraft:sharpness')");
        
        param.addProperties(PARAM_ITEM_ID, itemIdParam, true);
        
        StringParameter typeParam = StringParameter.create()
            .setTitle("Type")
            .setDescription("Type of resource: 'item' (default) or 'enchantment'");
        
        param.addProperties(PARAM_TYPE, typeParam, false);
        
        StringParameter descParam = StringParameter.create()
            .setTitle("Description")
            .setDescription("Brief description of what this check is for.");
        
        param.addProperties(PARAM_DESCRIPTION, descParam, false);
        
        return param;
    }
    
    @Override
    public Codec<CheckResult> codec() {
        return CODEC;
    }
    
    @Override
    public LLMCallback onCall(String toolId, CheckResult result, LLMCallback callback) {
        MaidCommandProcessor.LOGGER.info("ItemCheckTool called with item_id: {}, type: {}", 
            result.itemId(), result.type());
        
        if (callback.getMaid() == null) {
            return callback.addToolResult(toolId, "Error: Maid is not valid");
        }
        
        String itemId = result.itemId();
        String type = result.type().toLowerCase();
        
        boolean exists;
        String resourceType;
        String detailInfo = "";
        
        if ("enchantment".equals(type)) {
            exists = ModRegistryManager.checkEnchantExists(itemId);
            resourceType = "enchantment";
            
            if (exists) {
                String modId = itemId.contains(":") ? itemId.split(":")[0] : "minecraft";
                String enchantName = itemId.split(":")[1];
                detailInfo = getEnchantDetailInfo(modId, enchantName);
            }
        } else {
            exists = ModRegistryManager.checkItemExists(itemId);
            resourceType = "item";
            
            if (exists) {
                String modId = itemId.contains(":") ? itemId.split(":")[0] : "minecraft";
                String itemName = itemId.split(":")[1];
                detailInfo = getItemDetailInfo(modId, itemName);
            }
        }
        
        if (exists) {
            String response = "'" + itemId + "' (" + resourceType + ") found in current modpack.\n" + 
                detailInfo + result.description();
            MaidCommandProcessor.LOGGER.info(response);
            return callback.addToolResult(toolId, response);
        } else {
            String response = "Error: '" + itemId + "' (" + resourceType + ") not found in current modpack.\n" + 
                "Please confirm the related mod is installed.\n" + result.description();
            MaidCommandProcessor.LOGGER.warn(response);
            return callback.addToolResult(toolId, response);
        }
    }
    
    private String getItemDetailInfo(String modId, String itemName) {
        StringBuilder info = new StringBuilder();
        
        if (ModRegistryManager.isStandardMod(modId)) {
            var modInfo = ModRegistryManager.getStandardModInfo(modId);
            if (modInfo != null && modInfo.hasItems()) {
                info.append("Mod: ").append(modInfo.modName).append("\n");
                
                if (modInfo.items.contains(itemName)) {
                    info.append("Status: Standard supported item\n");
                } else {
                    info.append("Status: Dynamic detected item\n");
                }
            }
        } else {
            info.append("Status: Dynamically detected from mod registry\n");
        }
        
        return info.toString();
    }
    
    private String getEnchantDetailInfo(String modId, String enchantName) {
        StringBuilder info = new StringBuilder();
        
        if (ModRegistryManager.isStandardMod(modId)) {
            var modInfo = ModRegistryManager.getStandardModInfo(modId);
            if (modInfo != null && modInfo.hasEnchants()) {
                info.append("Mod: ").append(modInfo.modName).append("\n");
                
                String fullEnchantId = modId + ":" + enchantName;
                if (modInfo.enchants.contains(fullEnchantId) || 
                    ModRegistryManager.getGlobalEnchants().contains(fullEnchantId)) {
                    info.append("Status: Standard supported enchantment\n");
                } else {
                    info.append("Status: Dynamic detected enchantment\n");
                }
            }
        } else {
            info.append("Status: Dynamically detected from mod registry\n");
        }
        
        return info.toString();
    }
    
    @Override
    public Component invocationSummaryComponent(CheckResult result) {
        return Component.literal("Checking: " + result.itemId());
    }
}
