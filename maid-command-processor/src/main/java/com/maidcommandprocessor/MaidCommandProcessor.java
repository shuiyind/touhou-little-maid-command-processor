package com.maidcommandprocessor;

import com.maidcommandprocessor.ai.MaidToolRegistry;
import com.maidcommandprocessor.config.MaidCommandConfig;
import com.maidcommandprocessor.config.ModRegistryManager;
import com.maidcommandprocessor.handler.CommandExecutorModule;
import com.maidcommandprocessor.handler.PermissionModule;
import com.maidcommandprocessor.integration.LittleMaidToolRegistry;
import com.maidcommandprocessor.registry.CommandRegistry;
import com.mojang.logging.LogUtils;
import net.minecraft.server.MinecraftServer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.server.ServerAboutToStartEvent;
import org.slf4j.Logger;

@Mod(MaidCommandProcessor.MOD_ID)
public class MaidCommandProcessor {
    public static final String MOD_ID = "maid_command_processor";
    public static final Logger LOGGER = LogUtils.getLogger();
    
    public static final MaidCommandConfig config = new MaidCommandConfig();
    public static MinecraftServer server;
    
    private static MaidCommandProcessor instance;
    
    public MaidCommandProcessor(IEventBus modEventBus, net.neoforged.fml.ModContainer modContainer) {
        instance = this;
        
        modEventBus.addListener(this::commonSetup);
        modEventBus.addListener(this::onConfigLoad);
        modEventBus.addListener(this::onConfigUnload);
        
        NeoForge.EVENT_BUS.addListener(this::serverAboutToStart);
        NeoForge.EVENT_BUS.register(this);
        
        modContainer.registerConfig(ModConfig.Type.COMMON, config.spec);
        
        // Classes with NeoForge events should be registered to NeoForge.EVENT_BUS
        NeoForge.EVENT_BUS.register(CommandExecutorModule.class);
        NeoForge.EVENT_BUS.register(CommandRegistry.class);
        
        // PermissionModule and LittleMaidToolRegistry don't have @SubscribeEvent methods, 
        // so we don't need to register them
        
        LOGGER.info("Maid Command Processor mod loaded successfully");
        LOGGER.info("Module initialization complete");
    }
    
    private void commonSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
            LOGGER.info("Maid Command Processor - Performing common setup");
            CommandExecutorModule.initialize();
            PermissionModule.initialize();
            LittleMaidToolRegistry.initialize();
            MaidToolRegistry.registerTools();
            ModRegistryManager.initialize();
        });
    }
    
    @SubscribeEvent
    private void serverAboutToStart(ServerAboutToStartEvent event) {
        server = event.getServer();
        LittleMaidToolRegistry.registerTools();
    }
    
    private void onConfigLoad(ModConfigEvent.Loading event) {
        LOGGER.info("Config loaded for Maid Command Processor");
    }
    
    private void onConfigUnload(ModConfigEvent.Unloading event) {
        LOGGER.info("Config unloaded for Maid Command Processor");
    }
    
    public static MaidCommandProcessor getInstance() {
        return instance;
    }
    
    public MinecraftServer getMinecraftServer() {
        return instance.server;
    }
}
