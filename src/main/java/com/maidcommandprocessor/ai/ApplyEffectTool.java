package com.maidcommandprocessor.ai;

import com.github.tartaricacid.touhoulittlemaid.ai.agent.tool.ITool;
import com.github.tartaricacid.touhoulittlemaid.ai.manager.entity.LLMCallback;
import com.github.tartaricacid.touhoulittlemaid.ai.service.function.schema.parameter.ObjectParameter;
import com.github.tartaricacid.touhoulittlemaid.ai.service.function.schema.parameter.Parameter;
import com.github.tartaricacid.touhoulittlemaid.ai.service.function.schema.parameter.StringParameter;
import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import com.maidcommandprocessor.MaidCommandProcessor;
import com.maidcommandprocessor.handler.CommandExecutorModule;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.List;
import java.util.Optional;

public class ApplyEffectTool implements ITool<ApplyEffectTool.ApplyEffectResult> {
    
    public static final String TOOL_ID = "apply_effect";
    private static final String TOOL_DESC = "Apply status effects (buffs/debuffs) to players or entities. Supports adding speed, strength, slowness, weakness, and other Minecraft effects.";
    
    public static final String PARAM_TARGET = "target";
    public static final String PARAM_EFFECT_TYPE = "effect_type";
    public static final String PARAM_DURATION = "duration";
    public static final String PARAM_AMPLIFIER = "amplifier";
    public static final String PARAM_SHOW_PARTICLES = "show_particles";
    public static final String PARAM_DESCRIPTION = "description";
    
    private static final Codec<ApplyEffectResult> CODEC = RecordCodecBuilder.create(instance -> instance.group(
        Codec.STRING.fieldOf(PARAM_TARGET).forGetter(ApplyEffectResult::target),
        Codec.STRING.fieldOf(PARAM_EFFECT_TYPE).forGetter(ApplyEffectResult::effectType),
        Codec.INT.optionalFieldOf(PARAM_DURATION, 60).forGetter(ApplyEffectResult::duration),
        Codec.INT.optionalFieldOf(PARAM_AMPLIFIER, 0).forGetter(ApplyEffectResult::amplifier),
        Codec.BOOL.optionalFieldOf(PARAM_SHOW_PARTICLES, true).forGetter(ApplyEffectResult::showParticles),
        Codec.STRING.optionalFieldOf(PARAM_DESCRIPTION, "").forGetter(ApplyEffectResult::description)
    ).apply(instance, ApplyEffectResult::new));
    
    public record ApplyEffectResult(String target, String effectType, int duration, int amplifier, boolean showParticles, String description) {}
    
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
            .setTitle("Apply Status Effect")
            .setDescription("Apply buffs or debuffs to players or entities");
        
        StringParameter targetParam = StringParameter.create()
            .setTitle("Target")
            .setDescription("Target entity: @p (self), @e[type=entity_type] (specific entity), or player name");
        
        param.addProperties(PARAM_TARGET, targetParam, true);
        
        StringParameter effectTypeParam = StringParameter.create()
            .setTitle("Effect Type")
            .setDescription("Effect type: speed, strength, slowness, weakness, regeneration, absorption, night_vision, etc.");
        
        param.addProperties(PARAM_EFFECT_TYPE, effectTypeParam, true);
        
        StringParameter durationParam = StringParameter.create()
            .setTitle("Duration (real-time seconds)")
            .setDescription("How long the effect lasts in REAL TIME seconds (default: 60 seconds). E.g., 60=1分钟, 3600=1小时, 86400=24小时");
        
        param.addProperties(PARAM_DURATION, durationParam, false);
        
        StringParameter amplifierParam = StringParameter.create()
            .setTitle("Amplifier (0-255)")
            .setDescription("Effect strength: 0=I, 1=II, 2=III, etc. (default: 0)");
        
        param.addProperties(PARAM_AMPLIFIER, amplifierParam, false);
        
        StringParameter particlesParam = StringParameter.create()
            .setTitle("Show Particles")
            .setDescription("Whether to show particle effects (default: true)");
        
        param.addProperties(PARAM_SHOW_PARTICLES, particlesParam, false);
        
        StringParameter descParam = StringParameter.create()
            .setTitle("Description")
            .setDescription("Brief description of why this effect is being applied");
        
        param.addProperties(PARAM_DESCRIPTION, descParam, false);
        
        return param;
    }
    
    @Override
    public Codec<ApplyEffectResult> codec() {
        return CODEC;
    }
    
    @Override
    public LLMCallback onCall(String toolId, ApplyEffectResult result, LLMCallback callback) {
        if (callback.getMaid() == null) {
            return callback.addToolResult(toolId, "Error: Maid is not valid");
        }
        
        EntityMaid maid = callback.getMaid();
        LivingEntity ownerEntity = maid.getOwner();
        
        if (!(ownerEntity instanceof ServerPlayer owner)) {
            return callback.addToolResult(toolId, "Error: Owner is not a valid player");
        }
        
        MaidCommandProcessor.LOGGER.info("ApplyEffectTool called with target: {}, effect_type: {}, duration: {}, amplifier: {}", 
            result.target(), result.effectType(), result.duration(), result.amplifier());
        
        try {
            // 构建效果命令
            String command = buildEffectCommand(result, owner);
            
            MaidCommandProcessor.LOGGER.info("Applying effect: {} to {} with command: {}", 
                result.effectType(), result.target(), command);
            
            // 执行命令
            int result_code = CommandExecutorModule.executeCommand(
                owner.createCommandSourceStack(),
                command,
                owner,
                maid
            );
            
            if (result_code == 1) {
                String response = String.format(
                    "Successfully applied '%s' effect to %s for %d seconds (amplifier: %d)\n%s",
                    result.effectType(),
                    result.target(),
                    result.duration(),
                    result.amplifier(),
                    result.description()
                );
                MaidCommandProcessor.LOGGER.info(response);
                return callback.addToolResult(toolId, response);
            } else {
                String response = String.format(
                    "Failed to apply '%s' effect to %s",
                    result.effectType(),
                    result.target()
                );
                MaidCommandProcessor.LOGGER.warn(response);
                return callback.addToolResult(toolId, response);
            }
        } catch (Exception e) {
            String errorMsg = "Error applying effect: " + e.getMessage();
            MaidCommandProcessor.LOGGER.error(errorMsg, e);
            return callback.addToolResult(toolId, errorMsg);
        }
    }
    
    private String buildEffectCommand(ApplyEffectResult result, ServerPlayer owner) {
        // 构建 /effect give 命令
        StringBuilder command = new StringBuilder();
        command.append("/effect give ");
        
        // 目标
        if (result.target().startsWith("@")) {
            command.append(result.target());
        } else {
            // 如果是玩家名称，转换为 @s 或具体玩家选择器
            command.append("@s");
        }
        
        // 效果类型（添加 minecraft: 前缀）
        String effectId = result.effectType();
        if (!effectId.contains(":")) {
            effectId = "minecraft:" + effectId;
        }
        command.append(" ").append(effectId);
        
        // 持续时间（转换为游戏刻：1秒=20刻）
        int ticks = result.duration() * 20;
        command.append(" ").append(ticks);
        
        // 强度
        command.append(" ").append(result.amplifier());
        
        // 是否显示粒子效果
        if (result.showParticles()) {
            command.append(" true");
        } else {
            command.append(" false");
        }
        
        return command.toString();
    }
    
    @Override
    public Component invocationSummaryComponent(ApplyEffectResult result) {
        return Component.literal("Applying effect: " + result.effectType());
    }
}
