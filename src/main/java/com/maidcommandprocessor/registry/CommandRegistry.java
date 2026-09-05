package com.maidcommandprocessor.registry;

import com.maidcommandprocessor.MaidCommandProcessor;
import com.maidcommandprocessor.command.MaidCommandExecutor;
import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

public class CommandRegistry {
    
    @SubscribeEvent
    public static void registerCommands(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();
        
        MaidCommandExecutor.register(dispatcher);
        
        MaidCommandProcessor.LOGGER.info("Registered maid command executor commands");
    }
}
