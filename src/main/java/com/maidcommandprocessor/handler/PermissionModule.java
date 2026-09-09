package com.maidcommandprocessor.handler;

import com.maidcommandprocessor.MaidCommandProcessor;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.io.*;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class PermissionModule {
    
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final String PERMISSIONS_FILE = "config/maid_command_processor_permissions.json";
    private static volatile MinecraftServer server; // volatile 保证线程可见性
    
    public enum PermissionLevel {
        NONE(0, "无权限"),
        BASIC(1, "初级管理"),
        ADVANCED(2, "管理员"),
        ADMIN(3, "服务器之主");
        
        private final int level;
        private final String name;
        
        PermissionLevel(int level, String name) {
            this.level = level;
            this.name = name;
        }
        
        public int getLevel() { return level; }
        public String getName() { return name; }
    }
    
    // Use UUID as key instead of player name (player name can change)
    private static final Map<UUID, PermissionLevel> playerPermissions = new ConcurrentHashMap<>();
    
    public static void initialize(MinecraftServer srv) {
        server = srv;
        loadPermissions();
        MaidCommandProcessor.LOGGER.info("Permission Module initialized - loaded {} permission entries", playerPermissions.size());
    }
    
    public static PermissionLevel getPlayerPermission(net.minecraft.server.level.ServerPlayer player) {
        UUID playerId = player.getUUID();
        
        // 检查是否有明确设置的权限
        if (playerPermissions.containsKey(playerId)) {
            return playerPermissions.get(playerId);
        }
        
        // 服务器主人（单人游戏）自动获得 ADMIN 权限
        if (server != null && server.isSingleplayer() && server.isSingleplayerOwner(player.getGameProfile())) {
            return PermissionLevel.ADMIN;
        }
        
        // 默认权限为 NONE
        return PermissionLevel.NONE;
    }
    
    public static boolean hasPermission(net.minecraft.server.level.ServerPlayer player, PermissionLevel requiredLevel) {
        PermissionLevel currentLevel = getPlayerPermission(player);
        return currentLevel.getLevel() >= requiredLevel.getLevel();
    }
    
    /**
     * 只有 ADMIN (3级) 可以设置任意玩家的权限（包括添加新用户、升级、降级）
     */
    public static boolean setPlayerPermission(
            net.minecraft.server.level.ServerPlayer operator,
            net.minecraft.server.level.ServerPlayer target,
            PermissionLevel newLevel) {
        
        PermissionLevel operatorLevel = getPlayerPermission(operator);
        
        // 只有 ADMIN 可以设置权限
        if (operatorLevel != PermissionLevel.ADMIN) {
            MaidCommandProcessor.LOGGER.warn(
                "Player [{}] is not ADMIN, cannot change permissions (current: {})",
                operator.getName().getString(),
                operatorLevel.getName()
            );
            return false;
        }
        
        UUID targetId = target.getUUID();
        String targetName = target.getName().getString();
        playerPermissions.put(targetId, newLevel);
        
        // 自动保存权限数据
        savePermissions();
        
        MaidCommandProcessor.LOGGER.info(
            "ADMIN [{}] set permission for [{}] to {}",
            operator.getName().getString(),
            targetName,
            newLevel.getName()
        );
        return true;
    }
    
    /**
     * ADVANCED (2级) 只能撤销 BASIC (1级) 的权限
     * - 不能升级 BASIC 到 ADVANCED
     * - 不能添加新用户到 BASIC
     * - 可以撤销 BASIC 的权限（降级到 NONE）
     */
    public static boolean revokePermission(
            net.minecraft.server.level.ServerPlayer revoker,
            net.minecraft.server.level.ServerPlayer revokee) {
        
        PermissionLevel revokerLevel = getPlayerPermission(revoker);
        PermissionLevel revokeeLevel = getPlayerPermission(revokee);
        
        // 只有 ADVANCED (2级) 可以撤销 BASIC (1级) 的权限
        if (revokerLevel != PermissionLevel.ADVANCED || revokeeLevel != PermissionLevel.BASIC) {
            MaidCommandProcessor.LOGGER.warn(
                "Player [{}] ({}), cannot revoke permission from [{}] ({}) - mismatched levels",
                revoker.getName().getString(),
                revokerLevel.getName(),
                revokee.getName().getString(),
                revokeeLevel.getName()
            );
            return false;
        }
        
        UUID revokeeId = revokee.getUUID();
        String revokeeName = revokee.getName().getString();
        playerPermissions.remove(revokeeId); // 移除权限，回到 NONE
        
        // 自动保存权限数据
        savePermissions();
        
        MaidCommandProcessor.LOGGER.info(
            "ADVANCED [{}] revoked permission from BASIC [{}], now {}",
            revoker.getName().getString(),
            revokeeName,
            PermissionLevel.NONE.getName()
        );
        return true;
    }
    
    /**
     * 检查是否可以撤销权限
     * 只有 ADVANCED (2级) 可以撤销 BASIC (1级) 的权限
     */
    public static boolean canRevokePermission(
            net.minecraft.server.level.ServerPlayer revoker,
            net.minecraft.server.level.ServerPlayer revokee) {
        
        PermissionLevel revokerLevel = getPlayerPermission(revoker);
        PermissionLevel revokeeLevel = getPlayerPermission(revokee);
        
        return revokerLevel == PermissionLevel.ADVANCED && revokeeLevel == PermissionLevel.BASIC;
    }
    
    /**
     * 检查是否可以授予权限（用于升级）
     * 只有 ADMIN (3级) 可以升级玩家权限
     */
    public static boolean canGrantPermission(net.minecraft.server.level.ServerPlayer grantor) {
        
        PermissionLevel grantorLevel = getPlayerPermission(grantor);
        
        return grantorLevel == PermissionLevel.ADMIN;
    }
    
    /**
     * 获取所有玩家的权限列表
     */
    public static Map<String, PermissionLevel> getAllPermissions() {
        // Convert UUID keys to string representation for external use
        Map<String, PermissionLevel> nameMap = new HashMap<>();
        for (Map.Entry<UUID, PermissionLevel> entry : playerPermissions.entrySet()) {
            // Find player name by UUID
            String name = "unknown_" + entry.getKey();
            if (server != null) {
                ServerPlayer player = server.getPlayerList().getPlayer(entry.getKey());
                if (player != null) {
                    name = player.getName().getString();
                }
            }
            nameMap.put(name, entry.getValue());
        }
        return Collections.unmodifiableMap(nameMap);
    }
    
    /**
     * 加载权限数据从文件
     */
    private static void loadPermissions() {
        try {
            Path filePath = Path.of(PERMISSIONS_FILE);
            if (!Files.exists(filePath)) {
                MaidCommandProcessor.LOGGER.info("Permission file not found, starting with empty permissions");
                return;
            }
            
            String json = Files.readString(filePath);
            Type type = new TypeToken<Map<String, String>>() {}.getType();
            Map<String, String> permissionMap = GSON.fromJson(json, type);
            
            if (permissionMap != null) {
                for (Map.Entry<String, String> entry : permissionMap.entrySet()) {
                    try {
                        PermissionLevel level = PermissionLevel.valueOf(entry.getValue());
                        // Try to resolve UUID from stored key
                        UUID playerId;
                        try {
                            playerId = UUID.fromString(entry.getKey());
                        } catch (IllegalArgumentException uuidEx) {
                            // Not a valid UUID, generate one from name for backward compatibility
                            playerId = UUID.fromString("00000000-0000-0000-0000-" + entry.getKey().replace(" ", ""));
                        }
                        playerPermissions.put(playerId, level);
                        MaidCommandProcessor.LOGGER.info("Loaded permission for {}: {}", entry.getKey(), level.getName());
                    } catch (IllegalArgumentException e) {
                        MaidCommandProcessor.LOGGER.warn(
                            "Invalid permission level '{}' for player '{}'",
                            entry.getValue(), entry.getKey()
                        );
                    }
                }
                MaidCommandProcessor.LOGGER.info(
                    "Loaded {} permissions from {}",
                    playerPermissions.size(), PERMISSIONS_FILE
                );
            }
        } catch (Exception e) {
            MaidCommandProcessor.LOGGER.error("Failed to load permissions: {}", e.getMessage(), e);
        }
    }
    
    /**
     * 保存权限数据到文件
     */
    public static void savePermissions() {
        try {
            Path filePath = Path.of(PERMISSIONS_FILE);
            Path parentDir = filePath.getParent();
            if (parentDir != null) {
                Files.createDirectories(parentDir);
            }
            
            // 转换为字符串映射 (UUID -> name for storage)
            Map<String, String> stringMap = new HashMap<>();
            for (Map.Entry<UUID, PermissionLevel> entry : playerPermissions.entrySet()) {
                stringMap.put(entry.getKey().toString(), entry.getValue().name());
            }
            
            String json = GSON.toJson(stringMap);
            Files.writeString(filePath, json);
            
            MaidCommandProcessor.LOGGER.info(
                "Saved {} permissions to {}",
                playerPermissions.size(), PERMISSIONS_FILE
            );
        } catch (Exception e) {
            MaidCommandProcessor.LOGGER.error("Failed to save permissions: {}", e.getMessage(), e);
        }
    }
}
