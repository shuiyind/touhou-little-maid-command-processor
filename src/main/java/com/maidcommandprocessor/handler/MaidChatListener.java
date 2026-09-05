package com.maidcommandprocessor.handler;

import com.maidcommandprocessor.MaidCommandProcessor;
import com.github.tartaricacid.touhoulittlemaid.api.event.MaidTickEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;

@EventBusSubscriber(modid = MaidCommandProcessor.MOD_ID)
public class MaidChatListener {
    
    private static long lastLogTime = 0;
    
    @SubscribeEvent
    public static void onMaidTick(MaidTickEvent event) {
        long currentTime = System.currentTimeMillis();
        
        if (currentTime - lastLogTime > 5000) {
            MaidCommandProcessor.LOGGER.info(
                "Maid tick event triggered for {}",
                event.getMaid().getUUID()
            );
            lastLogTime = currentTime;
        }
    }
}
