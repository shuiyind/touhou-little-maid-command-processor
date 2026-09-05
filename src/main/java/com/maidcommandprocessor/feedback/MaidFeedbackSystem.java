package com.maidcommandprocessor.feedback;

import com.maidcommandprocessor.MaidCommandProcessor;
import com.maidcommandprocessor.config.MaidCommandConfig;
import com.maidcommandprocessor.voice.VoiceOutputModule;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class MaidFeedbackSystem {
    
    public enum FeedbackType {
        SUCCESS,
        FAILURE,
        ERROR,
        COOLDOWN,
        NO_PERMISSION,
        CHAT_RESPONSE
    }
    
    public static class FeedbackMessage {
        private final FeedbackType type;
        private final String text;
        private final Component component;
        private final boolean showActionBar;
        private final boolean speak;
        
        public FeedbackMessage(FeedbackType type, String text, Component component,
                             boolean showActionBar, boolean speak) {
            this.type = type;
            this.text = text;
            this.component = component;
            this.showActionBar = showActionBar;
            this.speak = speak;
        }
        
        public FeedbackType getType() { return type; }
        public String getText() { return text; }
        public Component getComponent() { return component; }
        public boolean shouldShowActionBar() { return showActionBar; }
        public boolean shouldSpeak() { return speak; }
    }
    
    private static final Map<UUID, List<FeedbackMessage>> maidFeedbackQueue = new ConcurrentHashMap<>();
    private static final Map<UUID, Long> lastFeedbackTime = new ConcurrentHashMap<>();
    
    public static void sendFeedback(
            ServerPlayer player,
            Entity maidEntity,
            FeedbackType type,
            String message) {
        
        sendFeedback(player, maidEntity, type, message, false, false);
    }
    
    public static void sendFeedback(
            ServerPlayer player,
            Entity maidEntity,
            FeedbackType type,
            String message,
            boolean showActionBar,
            boolean speak) {
        
        MaidCommandConfig config = MaidCommandProcessor.config;
        
        // Check feedback cooldown
        long now = System.currentTimeMillis();
        Long lastTime = lastFeedbackTime.get(maidEntity.getUUID());
        if (lastTime != null && (now - lastTime) < 500) {
            return; // Too frequent
        }
        
        lastFeedbackTime.put(maidEntity.getUUID(), now);
        
        // Create feedback message
        Component component = Component.translatable(message);
        FeedbackMessage feedback = new FeedbackMessage(
            type,
            message,
            component,
            showActionBar,
            speak && config.enableVoiceOutput.get()
        );
        
        // Add to queue
        maidFeedbackQueue.computeIfAbsent(maidEntity.getUUID(), k -> new ArrayList<>()).add(feedback);
        
        // Send to player
        switch (type) {
            case SUCCESS -> player.sendSystemMessage(
                Component.literal(String.format(config.getSuccessResponse(), message))
            );
            case FAILURE -> player.sendSystemMessage(
                Component.literal(String.format(config.getFailureResponse(), message))
            );
            case ERROR -> player.sendSystemMessage(
                Component.literal(String.format(config.getErrorResponse(), message))
            );
            case COOLDOWN -> player.sendSystemMessage(
                Component.literal(config.getCooldownResponse())
            );
            case NO_PERMISSION -> player.sendSystemMessage(
                Component.literal(config.getNoPermissionResponse())
            );
            case CHAT_RESPONSE -> player.sendSystemMessage(component);
        }
        
        // Show action bar if requested
        if (showActionBar) {
            player.sendSystemMessage(Component.literal("[" + message + "]"));
        }
        
        // Speak if requested
        if (speak && config.enableVoiceOutput.get()) {
            speakFeedback(message);
        }
        
        MaidCommandProcessor.LOGGER.info(
            "Sent {} feedback to {} for maid {}",
            type, player.getName().getString(), maidEntity.getUUID()
        );
    }
    
    public static void speakFeedback(String message) {
        MaidCommandProcessor.LOGGER.info("Speaking feedback: {}", message);
        
        // Use voice output module
        VoiceOutputModule.speak(message);
    }
    
    public static List<FeedbackMessage> getFeedbackQueue(UUID maidId) {
        return maidFeedbackQueue.getOrDefault(maidId, Collections.emptyList());
    }
    
    public static void clearFeedbackQueue(UUID maidId) {
        maidFeedbackQueue.remove(maidId);
        MaidCommandProcessor.LOGGER.info("Cleared feedback queue for maid {}", maidId);
    }
    
    public static void clearAllFeedbackQueues() {
        maidFeedbackQueue.clear();
        lastFeedbackTime.clear();
        MaidCommandProcessor.LOGGER.info("Cleared all feedback queues");
    }
    
    public static void processQueuedFeedback(UUID maidId) {
        List<FeedbackMessage> queue = maidFeedbackQueue.get(maidId);
        if (queue != null && !queue.isEmpty()) {
            MaidCommandProcessor.LOGGER.info(
                "Processing {} feedback messages for maid {}",
                queue.size(), maidId
            );
            // In a real implementation, this would process the queue
            // For now, just log
        }
    }
}
