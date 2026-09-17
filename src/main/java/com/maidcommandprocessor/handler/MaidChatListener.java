package com.maidcommandprocessor.handler;

import com.maidcommandprocessor.MaidCommandProcessor;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;

/**
 * 女仆聊天监听器 - 由 CommandQueueModule 统一处理 tick 事件
 * 保留此类以维持事件订阅兼容性
 */
@EventBusSubscriber(modid = MaidCommandProcessor.MOD_ID)
public class MaidChatListener {
    
    @SubscribeEvent
    public static void onMaidTick(com.github.tartaricacid.touhoulittlemaid.api.event.MaidTickEvent event) {
        // Tick 事件处理已统一移至 CommandQueueModule
        // 此方法保留以避免事件订阅丢失
    }
}