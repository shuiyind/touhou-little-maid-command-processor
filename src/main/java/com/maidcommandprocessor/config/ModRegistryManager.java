package com.maidcommandprocessor.config;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.maidcommandprocessor.MaidCommandProcessor;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.enchantment.Enchantment;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ModRegistryManager {
    
    private static final Gson GSON = new Gson();
    private static final String CONFIG_PATH = "maid_command_processor/mod_compatibility.json";
    
    private static Map<String, ModInfo> standardMods = new HashMap<>();
    private static List<String> globalEnchants = new ArrayList<>();
    private static Map<String, List<String>> dynamicItems = new HashMap<>();
    private static Map<String, List<String>> dynamicEnchants = new HashMap<>();
    
    private static long lastDynamicCacheUpdate = 0;
    private static final long DYNAMIC_CACHE_TTL = 5 * 60 * 1000;
    
    public static class ModInfo {
        public String modName;
        public List<String> items;
        public List<String> enchants;
        public String description;
        
        public boolean hasItems() {
            return items != null && !items.isEmpty();
        }
        
        public boolean hasEnchants() {
            return enchants != null && !enchants.isEmpty();
        }
    }
    
    public static void loadStandardConfig() {
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(
                    ModRegistryManager.class.getClassLoader().getResourceAsStream(CONFIG_PATH),
                    StandardCharsets.UTF_8))) {
            
            StringBuilder json = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                json.append(line);
            }
            
            JsonObject root = GSON.fromJson(json.toString(), JsonObject.class);
            
            if (root.has("mods")) {
                JsonObject mods = root.getAsJsonObject("mods");
                for (String modId : mods.keySet()) {
                    ModInfo info = new ModInfo();
                    JsonObject modData = mods.get(modId).getAsJsonObject();
                    
                    info.modName = modData.has("mod_name") ? modData.get("mod_name").getAsString() : modId;
                    info.description = modData.has("description") ? modData.get("description").getAsString() : "";
                    
                    if (modData.has("items")) {
                        JsonArray itemsArray = modData.getAsJsonArray("items");
                        info.items = new ArrayList<>();
                        for (JsonElement item : itemsArray) {
                            info.items.add(item.getAsString());
                        }
                    } else {
                        info.items = new ArrayList<>();
                    }
                    
                    if (modData.has("enchants")) {
                        JsonArray enchantsArray = modData.getAsJsonArray("enchants");
                        info.enchants = new ArrayList<>();
                        for (JsonElement enchant : enchantsArray) {
                            info.enchants.add(enchant.getAsString());
                        }
                    } else {
                        info.enchants = new ArrayList<>();
                    }
                    
                    standardMods.put(modId, info);
                }
            }
            
            if (root.has("global_enchants")) {
                JsonArray enchantsArray = root.getAsJsonArray("global_enchants");
                globalEnchants = new ArrayList<>();
                for (JsonElement enchant : enchantsArray) {
                    globalEnchants.add(enchant.getAsString());
                }
            }
            
            MaidCommandProcessor.LOGGER.info("Loaded standard compatibility config with {} mods", standardMods.size());
        } catch (Exception e) {
            MaidCommandProcessor.LOGGER.warn("Failed to load standard compatibility config: {}", e.getMessage());
        }
    }
    
    public static void refreshDynamicCache() {
        MinecraftServer server = MaidCommandProcessor.server;
        if (server == null) return;
        
        try {
            var itemRegistry = server.registryAccess().registry(Registries.ITEM).orElse(null);
            if (itemRegistry != null) {
                dynamicItems.clear();
                for (var entry : itemRegistry.entrySet()) {
                    ResourceLocation resourceId = entry.getKey().location();
                    String modId = resourceId.getNamespace();
                    String itemId = resourceId.getPath();
                    
                    if (!modId.equals("minecraft") && !standardMods.containsKey(modId)) {
                        dynamicItems.computeIfAbsent(modId, k -> new ArrayList<>()).add(itemId);
                    }
                }
            }
            
            var enchantRegistry = server.registryAccess().registry(Registries.ENCHANTMENT).orElse(null);
            if (enchantRegistry != null) {
                dynamicEnchants.clear();
                for (var entry : enchantRegistry.entrySet()) {
                    ResourceLocation resourceId = entry.getKey().location();
                    String modId = resourceId.getNamespace();
                    String enchantId = resourceId.getPath();
                    
                    if (!modId.equals("minecraft") && !globalEnchants.contains(modId + ":" + enchantId)) {
                        dynamicEnchants.computeIfAbsent(modId, k -> new ArrayList<>()).add(enchantId);
                    }
                }
            }
            
            lastDynamicCacheUpdate = System.currentTimeMillis();
            MaidCommandProcessor.LOGGER.info("Refreshed dynamic cache: {} mods with items, {} mods with enchants", 
                dynamicItems.size(), dynamicEnchants.size());
        } catch (Exception e) {
            MaidCommandProcessor.LOGGER.error("Failed to refresh dynamic cache: {}", e.getMessage(), e);
        }
    }
    
    public static boolean isStandardMod(String modId) {
        return standardMods.containsKey(modId);
    }
    
    public static ModInfo getStandardModInfo(String modId) {
        return standardMods.get(modId);
    }
    
    public static List<String> getStandardItems(String modId) {
        ModInfo info = standardMods.get(modId);
        return info != null ? info.items : new ArrayList<>();
    }
    
    public static List<String> getStandardEnchants(String modId) {
        ModInfo info = standardMods.get(modId);
        return info != null ? info.enchants : new ArrayList<>();
    }
    
    public static List<String> getGlobalEnchants() {
        return new ArrayList<>(globalEnchants);
    }
    
    public static boolean isDynamicItem(String modId, String itemId) {
        ensureDynamicCache();
        
        if (standardMods.containsKey(modId)) {
            ModInfo info = standardMods.get(modId);
            return info != null && info.items.contains(itemId);
        }
        
        List<String> items = dynamicItems.get(modId);
        return items != null && items.contains(itemId);
    }
    
    public static boolean isDynamicEnchant(String modId, String enchantId) {
        ensureDynamicCache();
        
        if (globalEnchants.contains(modId + ":" + enchantId)) {
            return true;
        }
        
        List<String> enchants = dynamicEnchants.get(modId);
        return enchants != null && enchants.contains(enchantId);
    }
    
    public static List<String> getAllDynamicItems() {
        ensureDynamicCache();
        List<String> allItems = new ArrayList<>();
        for (List<String> items : dynamicItems.values()) {
            allItems.addAll(items);
        }
        return allItems;
    }
    
    public static List<String> getDynamicItemsByMod(String modId) {
        ensureDynamicCache();
        return dynamicItems.getOrDefault(modId, new ArrayList<>());
    }
    
    private static void ensureDynamicCache() {
        if (System.currentTimeMillis() - lastDynamicCacheUpdate > DYNAMIC_CACHE_TTL) {
            refreshDynamicCache();
        }
    }
    
    public static List<String> getAllMods() {
        List<String> allMods = new ArrayList<>();
        allMods.addAll(standardMods.keySet());
        
        ensureDynamicCache();
        for (String modId : dynamicItems.keySet()) {
            if (!allMods.contains(modId)) {
                allMods.add(modId);
            }
        }
        
        return allMods;
    }
    
    public static boolean checkItemExists(String itemId) {
        MinecraftServer server = MaidCommandProcessor.server;
        if (server == null) {
            MaidCommandProcessor.LOGGER.warn("Server is null when checking item: {}", itemId);
            return false;
        }
        
        ResourceLocation resourceId = ResourceLocation.tryParse(itemId);
        if (resourceId == null) {
            MaidCommandProcessor.LOGGER.warn("Invalid resource ID: {}", itemId);
            return false;
        }
        
        String modId = resourceId.getNamespace();
        String itemName = resourceId.getPath();
        
        MaidCommandProcessor.LOGGER.info("Checking item existence: {} (modId: {}, name: {})", itemId, modId, itemName);
        
        try {
            // 尝试从服务端注册表查询
            var itemRegistry = server.registryAccess().registry(Registries.ITEM).orElse(null);
            if (itemRegistry != null) {
                boolean exists = itemRegistry.containsKey(resourceId);
                MaidCommandProcessor.LOGGER.info("Item {} from server registry: {}", itemId, exists);
                
                // 如果服务端注册表找不到，尝试检查是否在标准清单中
                if (!exists && isStandardMod(modId)) {
                    ModInfo info = getStandardModInfo(modId);
                    if (info != null && info.items.contains(itemName)) {
                        MaidCommandProcessor.LOGGER.info("Item {} found in standard config for mod {}", itemId, modId);
                        return true;
                    }
                }
                
                return exists;
            } else {
                MaidCommandProcessor.LOGGER.warn("Item registry not found for: {}", itemId);
            }
        } catch (Exception e) {
            MaidCommandProcessor.LOGGER.error("Error checking item existence: {}: {}", itemId, e.getMessage(), e);
        }
        
        return false;
    }
    
    public static boolean checkEnchantExists(String enchantId) {
        MinecraftServer server = MaidCommandProcessor.server;
        if (server == null) return false;
        
        ResourceLocation resourceId = ResourceLocation.tryParse(enchantId);
        if (resourceId == null) return false;
        
        try {
            var enchantRegistry = server.registryAccess().registry(Registries.ENCHANTMENT).orElse(null);
            if (enchantRegistry != null) {
                return enchantRegistry.containsKey(resourceId);
            }
        } catch (Exception e) {
            MaidCommandProcessor.LOGGER.error("Error checking enchantment existence: {}", e.getMessage(), e);
        }
        
        return false;
    }
    
    public static void initialize() {
        loadStandardConfig();
        refreshDynamicCache();
    }
}
