package com.maidcommandprocessor.integration;

import com.maidcommandprocessor.MaidCommandProcessor;
import com.maidcommandprocessor.ai.AINegotiationEngine;
import com.maidcommandprocessor.handler.CommandExecutorModule;
import com.maidcommandprocessor.handler.CommandQueueModule;
import com.maidcommandprocessor.handler.PermissionModule;
import com.maidcommandprocessor.handler.PermissionModule.PermissionLevel;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import com.github.tartaricacid.touhoulittlemaid.api.event.MaidTickEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@EventBusSubscriber(modid = MaidCommandProcessor.MOD_ID)
public class LittleMaidIntegration {
    
    private static boolean littleMaidPresent = false;
    private static final Map<UUID, List<String>> maidChatHistory = new ConcurrentHashMap<>();
    private static final Map<UUID, Long> lastInteractionTime = new ConcurrentHashMap<>();
    private static final Set<UUID> activeMaidChatPlayers = ConcurrentHashMap.newKeySet();
    
    public static class MaidEntityInfo {
        private final UUID entityId;
        private final String entityName;
        private final ServerPlayer owner;
        private final List<String> chatHistory;
        
        public MaidEntityInfo(UUID entityId, String entityName, ServerPlayer owner) {
            this.entityId = entityId;
            this.entityName = entityName;
            this.owner = owner;
            this.chatHistory = new ArrayList<>();
        }
        
        public UUID getEntityId() { return entityId; }
        public String getEntityName() { return entityName; }
        public ServerPlayer getOwner() { return owner; }
        public List<String> getChatHistory() { return chatHistory; }
        public void addChatEntry(String entry) { 
            chatHistory.add(entry);
            if (chatHistory.size() > 50) {
                chatHistory.remove(0);
            }
        }
    }
    
    public static void checkAndConnect() {
        try {
            Class.forName("com.github.littlemaid.LittleMaid");
            littleMaidPresent = true;
            MaidCommandProcessor.LOGGER.info("Little Maid mod detected and connected");
        } catch (ClassNotFoundException e) {
            littleMaidPresent = false;
            MaidCommandProcessor.LOGGER.info("Little Maid mod not found, running in standalone mode");
        }
    }
    
    public static boolean isLittleMaidPresent() {
        return littleMaidPresent;
    }
    
    @SubscribeEvent
    public static void onPlayerInteract(PlayerInteractEvent.RightClickItem event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        
        MaidCommandProcessor.LOGGER.debug(
            "Player {} right-clicked with item",
            player.getName().getString()
        );
        
        lastInteractionTime.put(event.getEntity().getUUID(), System.currentTimeMillis());
    }
    
    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        MaidCommandProcessor.LOGGER.info(
            "Player {} logged in, permission level: {}",
            event.getEntity().getName().getString(),
            PermissionModule.getPlayerPermission((ServerPlayer) event.getEntity()).getName()
        );
    }
    
    @SubscribeEvent
    public static void onMaidTick(MaidTickEvent event) {
        MaidCommandProcessor.LOGGER.debug(
            "Maid tick event triggered for {}",
            event.getMaid().getUUID()
        );
        
        CommandQueueModule.executePendingCommands();
    }
    
    public static AINegotiationEngine.CommandIntent analyzeChatInput(
            String chatText,
            ServerPlayer player,
            Entity maidEntity) {
        
        if (!MaidCommandProcessor.config.enableChatResponse()) {
            return null;
        }
        
        PermissionLevel playerPermission = PermissionModule.getPlayerPermission(player);
        
        MaidCommandProcessor.LOGGER.info(
            "Analyzing chat input from {} for maid {}: {}",
            player.getName().getString(),
            maidEntity.getUUID(),
            chatText
        );
        
        AINegotiationEngine.CommandIntent intent = AINegotiationEngine.parseIntent(
            chatText,
            playerPermission
        );
        
        if (intent != null && intent.isExecuteCommand()) {
            MaidCommandProcessor.LOGGER.info(
                "Parsed intent: {} -> {}",
                chatText,
                intent.getCommand()
            );
        }
        
        return intent;
    }
    
    public static void executeCommandForMaid(
            ServerPlayer player,
            Entity maidEntity,
            String command) {
        
        CommandExecutorModule.executeCommand(
            player.createCommandSourceStack(),
            command,
            player,
            maidEntity
        );
    }
    
    public static void addChatHistory(UUID maidId, String entry) {
        List<String> history = maidChatHistory.computeIfAbsent(maidId, k -> new ArrayList<String>());
        history.add(entry);
    }
    
    public static List<String> getChatHistory(UUID maidId) {
        return maidChatHistory.getOrDefault(maidId, new ArrayList<String>());
    }
    
    public static void clearChatHistory(UUID maidId) {
        maidChatHistory.remove(maidId);
    }
    
    public static long getLastInteractionTime(UUID maidId) {
        return lastInteractionTime.getOrDefault(maidId, 0L);
    }
    
    public static void updateInteractionTime(UUID maidId) {
        lastInteractionTime.put(maidId, System.currentTimeMillis());
    }
}
