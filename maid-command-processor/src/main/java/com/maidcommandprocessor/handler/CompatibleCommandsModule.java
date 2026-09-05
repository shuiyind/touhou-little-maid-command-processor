package com.maidcommandprocessor.handler;

import com.maidcommandprocessor.MaidCommandProcessor;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

public class CompatibleCommandsModule {
    
    @SubscribeEvent
    public static void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        MaidCommandProcessor.LOGGER.debug(
            "Player {} logged in",
            event.getEntity().getName().getString()
        );
    }
}
