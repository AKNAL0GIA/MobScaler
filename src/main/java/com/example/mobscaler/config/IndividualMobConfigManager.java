package com.example.mobscaler.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class IndividualMobConfigManager {
    private static final Map<String, IndividualMobConfig> individualMobConfigs = new HashMap<>();
    private static final Map<String, IndividualMobAttributes> modConfigs = new HashMap<>();
    private static final Logger LOGGER = LoggerFactory.getLogger(IndividualMobConfigManager.class);
    private static final Path CONFIG_DIR = Paths.get("config", "mobscaler");
    private static final Path INDIVIDUAL_MOBS_CONFIG = CONFIG_DIR.resolve("individual_mobs.json");
    private static final Path MOD_MOBS_CONFIG = CONFIG_DIR.resolve("mod_mobs.json");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private static boolean getBoolean(JsonObject obj, String key, boolean defaultValue) {
        return obj.has(key) ? obj.get(key).getAsBoolean() : defaultValue;
    }

    private static double getDouble(JsonObject obj, String key, double defaultValue) {
        return obj.has(key) ? obj.get(key).getAsDouble() : defaultValue;
    }

    public static void loadConfigs() {
        // Создаем директорию конфигурации, если она не существует
        File configDir = new File("config/mobscaler");
        if (!configDir.exists()) {
            configDir.mkdirs();
        }

        // Загружаем конфигурации для модов
        File modConfigFile = new File(configDir, "mod_mobs.json");
        if (!modConfigFile.exists()) {
            // Создаем файл с настройками по умолчанию
            try (FileWriter writer = new FileWriter(modConfigFile)) {
                Gson gson = new GsonBuilder().setPrettyPrinting().create();
                JsonObject root = new JsonObject();
                JsonObject mods = new JsonObject();
                
                // Создаем настройки по умолчанию для vanilla мобов
                JsonObject vanillaConfig = new JsonObject();
                JsonObject vanillaAttributes = getDefaultModAttributes();
                vanillaConfig.add("attributes", vanillaAttributes);
                
                root.add("mods", mods);
                gson.toJson(root, writer);
            } catch (IOException e) {
                LOGGER.error("Failed to create mod mobs configuration file", e);
                return;
            }
        }

        // Загружаем конфигурации для отдельных мобов
        File individualConfigFile = new File(configDir, "individual_mobs.json");
        if (!individualConfigFile.exists()) {
            // Создаем файл с настройками по умолчанию
            try (FileWriter writer = new FileWriter(individualConfigFile)) {
                Gson gson = new GsonBuilder().setPrettyPrinting().create();
                JsonObject defaultConfig = getDefaultConfig();
                gson.toJson(defaultConfig, writer);
            } catch (IOException e) {
                LOGGER.error("Failed to create individual mobs configuration file", e);
                return;
            }
        }

        // Читаем конфигурации
        try {
            // Загружаем конфигурации для модов
            JsonObject modConfig = JsonParser.parseReader(new FileReader(modConfigFile)).getAsJsonObject();
            JsonObject modsJson = modConfig.getAsJsonObject("mods");
            
            if (modsJson == null) {
                LOGGER.warn("Invalid mod configuration file structure, recreating with defaults");
                modsJson = new JsonObject();
                JsonObject vanillaConfig = new JsonObject();
                JsonObject vanillaAttributes = getDefaultModAttributes();
                vanillaConfig.add("attributes", vanillaAttributes);
                modsJson.add("minecraft", vanillaConfig);
                modConfig.add("mods", modsJson);
                
                // Сохраняем исправленную конфигурацию
                try (FileWriter writer = new FileWriter(modConfigFile)) {
                    GSON.toJson(modConfig, writer);
                }
            }
            
            
            // Очищаем старые настройки мода
            modConfigs.clear();
            
            for (Map.Entry<String, JsonElement> entry : modsJson.entrySet()) {
                String modId = entry.getKey();
                JsonObject modJson = entry.getValue().getAsJsonObject();
                JsonObject attributesJson = modJson.getAsJsonObject("attributes");
                
                if (attributesJson == null) {
                    LOGGER.warn("Invalid configuration for mod {}, using defaults", modId);
                    attributesJson = getDefaultModAttributes();
                    modJson.add("attributes", attributesJson);
                }
                
                IndividualMobAttributes attributes = loadModConfig(attributesJson);
                modConfigs.put(modId, attributes);
            }

            // Загружаем конфигурации для отдельных мобов
            JsonObject individualConfig = JsonParser.parseReader(new FileReader(individualConfigFile)).getAsJsonObject();
            JsonObject mobs = individualConfig.getAsJsonObject("mobs");
            
            if (mobs == null) {
                LOGGER.warn("Invalid individual mobs configuration file structure, recreating with defaults");
                mobs = new JsonObject();
                individualConfig.add("mobs", mobs);
                
                // Сохраняем исправленную конфигурацию
                try (FileWriter writer = new FileWriter(individualConfigFile)) {
                    GSON.toJson(individualConfig, writer);
                }
            }
            
            // Очищаем старые индивидуальные настройки
            individualMobConfigs.clear();
            
            for (Map.Entry<String, JsonElement> entry : mobs.entrySet()) {
                String entityId = entry.getKey();
                JsonObject mobJson = entry.getValue().getAsJsonObject();
                IndividualMobConfig config = loadMobConfig(mobJson);
                individualMobConfigs.put(entityId, config);
            }
        } catch (Exception e) {
            LOGGER.error("Error loading configurations", e);
            // В случае ошибки создаем пустые конфигурации
            modConfigs.clear();
            individualMobConfigs.clear();
        }

        // Передаем конфигурации в EntityHandler
        modConfigs.forEach((modId, config) -> {
            com.example.mobscaler.events.EntityHandler.addModConfig(modId, config);
        });
        
        individualMobConfigs.forEach((entityId, config) -> {
            com.example.mobscaler.events.EntityHandler.addIndividualMobConfig(entityId, config);
        });
        
    }

    public static void saveIndividualConfigs() {
        try {
            Files.createDirectories(CONFIG_DIR);
            try (Writer writer = Files.newBufferedWriter(INDIVIDUAL_MOBS_CONFIG)) {
                JsonObject root = new JsonObject();
                JsonObject mobs = new JsonObject();
                
                for (Map.Entry<String, IndividualMobConfig> entry : individualMobConfigs.entrySet()) {
                    JsonObject mobConfig = new JsonObject();
                    mobConfig.addProperty("isBlacklisted", entry.getValue().isBlacklisted());
                    
                    // Сохраняем атрибуты
                    IndividualMobAttributes attributes = entry.getValue().getAttributes();
                    JsonObject attributesJson = new JsonObject();
                    
                    // Сохраняем флаги
                    attributesJson.addProperty("enableNightScaling", attributes.getEnableNightScaling());
                    attributesJson.addProperty("enableCaveScaling", attributes.getEnableCaveScaling());
                    attributesJson.addProperty("caveHeight", attributes.getCaveHeight());
                    attributesJson.addProperty("enableGravity", attributes.isGravityEnabled());
                    attributesJson.addProperty("gravityMultiplier", attributes.getGravityMultiplier()); 
                    // Сохраняем базовые атрибуты
                    attributesJson.addProperty("healthAddition", attributes.getHealthAddition());
                    attributesJson.addProperty("healthMultiplier", attributes.getHealthMultiplier());
                    attributesJson.addProperty("armorAddition", attributes.getArmorAddition());
                    attributesJson.addProperty("armorMultiplier", attributes.getArmorMultiplier());
                    attributesJson.addProperty("damageAddition", attributes.getDamageAddition());
                    attributesJson.addProperty("damageMultiplier", attributes.getDamageMultiplier());
                    attributesJson.addProperty("speedAddition", attributes.getSpeedAddition());
                    attributesJson.addProperty("speedMultiplier", attributes.getSpeedMultiplier());
                    attributesJson.addProperty("knockbackResistanceAddition", attributes.getKnockbackResistanceAddition());
                    attributesJson.addProperty("knockbackResistanceMultiplier", attributes.getKnockbackResistanceMultiplier());
                    attributesJson.addProperty("attackKnockbackAddition", attributes.getAttackKnockbackAddition());
                    attributesJson.addProperty("attackKnockbackMultiplier", attributes.getAttackKnockbackMultiplier());
                    attributesJson.addProperty("attackSpeedAddition", attributes.getAttackSpeedAddition());
                    attributesJson.addProperty("attackSpeedMultiplier", attributes.getAttackSpeedMultiplier());
                    attributesJson.addProperty("followRangeAddition", attributes.getFollowRangeAddition());
                    attributesJson.addProperty("followRangeMultiplier", attributes.getFollowRangeMultiplier());
                    attributesJson.addProperty("flyingSpeedAddition", attributes.getFlyingSpeedAddition());
                    attributesJson.addProperty("flyingSpeedMultiplier", attributes.getFlyingSpeedMultiplier());
                    
                    // Добавляем дополнительные атрибуты
                    attributesJson.addProperty("armorToughnessAddition", attributes.getArmorToughnessAddition());
                    attributesJson.addProperty("armorToughnessMultiplier", attributes.getArmorToughnessMultiplier());
                    attributesJson.addProperty("luckAddition", attributes.getLuckAddition());
                    attributesJson.addProperty("luckMultiplier", attributes.getLuckMultiplier());
                    attributesJson.addProperty("swimSpeedAddition", attributes.getSwimSpeedAddition());
                    attributesJson.addProperty("swimSpeedMultiplier", attributes.getSwimSpeedMultiplier());
                    attributesJson.addProperty("reachDistanceAddition", attributes.getReachDistanceAddition());
                    attributesJson.addProperty("reachDistanceMultiplier", attributes.getReachDistanceMultiplier());
                    
                    // Сохраняем ночные атрибуты
                    attributesJson.addProperty("nightHealthAddition", attributes.getNightHealthAddition());
                    attributesJson.addProperty("nightHealthMultiplier", attributes.getNightHealthMultiplier());
                    attributesJson.addProperty("nightArmorAddition", attributes.getNightArmorAddition());
                    attributesJson.addProperty("nightArmorMultiplier", attributes.getNightArmorMultiplier());
                    attributesJson.addProperty("nightDamageAddition", attributes.getNightDamageAddition());
                    attributesJson.addProperty("nightDamageMultiplier", attributes.getNightDamageMultiplier());
                    attributesJson.addProperty("nightSpeedAddition", attributes.getNightSpeedAddition());
                    attributesJson.addProperty("nightSpeedMultiplier", attributes.getNightSpeedMultiplier());
                    attributesJson.addProperty("nightKnockbackResistanceAddition", attributes.getNightKnockbackResistanceAddition());
                    attributesJson.addProperty("nightKnockbackResistanceMultiplier", attributes.getNightKnockbackResistanceMultiplier());
                    attributesJson.addProperty("nightAttackKnockbackAddition", attributes.getNightAttackKnockbackAddition());
                    attributesJson.addProperty("nightAttackKnockbackMultiplier", attributes.getNightAttackKnockbackMultiplier());
                    attributesJson.addProperty("nightAttackSpeedAddition", attributes.getNightAttackSpeedAddition());
                    attributesJson.addProperty("nightAttackSpeedMultiplier", attributes.getNightAttackSpeedMultiplier());
                    attributesJson.addProperty("nightFollowRangeAddition", attributes.getNightFollowRangeAddition());
                    attributesJson.addProperty("nightFollowRangeMultiplier", attributes.getNightFollowRangeMultiplier());
                    attributesJson.addProperty("nightFlyingSpeedAddition", attributes.getNightFlyingSpeedAddition());
                    attributesJson.addProperty("nightFlyingSpeedMultiplier", attributes.getNightFlyingSpeedMultiplier());
                    
                    // Добавляем ночные дополнительные атрибуты
                    attributesJson.addProperty("nightArmorToughnessAddition", attributes.getNightArmorToughnessAddition());
                    attributesJson.addProperty("nightArmorToughnessMultiplier", attributes.getNightArmorToughnessMultiplier());
                    attributesJson.addProperty("nightLuckAddition", attributes.getNightLuckAddition());
                    attributesJson.addProperty("nightLuckMultiplier", attributes.getNightLuckMultiplier());
                    attributesJson.addProperty("nightSwimSpeedAddition", attributes.getNightSwimSpeedAddition());
                    attributesJson.addProperty("nightSwimSpeedMultiplier", attributes.getNightSwimSpeedMultiplier());
                    attributesJson.addProperty("nightReachDistanceAddition", attributes.getNightReachDistanceAddition());
                    attributesJson.addProperty("nightReachDistanceMultiplier", attributes.getNightReachDistanceMultiplier());
                    
                    // Сохраняем пещерные атрибуты
                    attributesJson.addProperty("caveHealthAddition", attributes.getCaveHealthAddition());
                    attributesJson.addProperty("caveHealthMultiplier", attributes.getCaveHealthMultiplier());
                    attributesJson.addProperty("caveArmorAddition", attributes.getCaveArmorAddition());
                    attributesJson.addProperty("caveArmorMultiplier", attributes.getCaveArmorMultiplier());
                    attributesJson.addProperty("caveDamageAddition", attributes.getCaveDamageAddition());
                    attributesJson.addProperty("caveDamageMultiplier", attributes.getCaveDamageMultiplier());
                    attributesJson.addProperty("caveSpeedAddition", attributes.getCaveSpeedAddition());
                    attributesJson.addProperty("caveSpeedMultiplier", attributes.getCaveSpeedMultiplier());
                    attributesJson.addProperty("caveKnockbackResistanceAddition", attributes.getCaveKnockbackResistanceAddition());
                    attributesJson.addProperty("caveKnockbackResistanceMultiplier", attributes.getCaveKnockbackResistanceMultiplier());
                    attributesJson.addProperty("caveAttackKnockbackAddition", attributes.getCaveAttackKnockbackAddition());
                    attributesJson.addProperty("caveAttackKnockbackMultiplier", attributes.getCaveAttackKnockbackMultiplier());
                    attributesJson.addProperty("caveAttackSpeedAddition", attributes.getCaveAttackSpeedAddition());
                    attributesJson.addProperty("caveAttackSpeedMultiplier", attributes.getCaveAttackSpeedMultiplier());
                    attributesJson.addProperty("caveFollowRangeAddition", attributes.getCaveFollowRangeAddition());
                    attributesJson.addProperty("caveFollowRangeMultiplier", attributes.getCaveFollowRangeMultiplier());
                    attributesJson.addProperty("caveFlyingSpeedAddition", attributes.getCaveFlyingSpeedAddition());
                    attributesJson.addProperty("caveFlyingSpeedMultiplier", attributes.getCaveFlyingSpeedMultiplier());
                    
                    // Добавляем пещерные дополнительные атрибуты
                    attributesJson.addProperty("caveArmorToughnessAddition", attributes.getCaveArmorToughnessAddition());
                    attributesJson.addProperty("caveArmorToughnessMultiplier", attributes.getCaveArmorToughnessMultiplier());
                    attributesJson.addProperty("caveLuckAddition", attributes.getCaveLuckAddition());
                    attributesJson.addProperty("caveLuckMultiplier", attributes.getCaveLuckMultiplier());
                    attributesJson.addProperty("caveSwimSpeedAddition", attributes.getCaveSwimSpeedAddition());
                    attributesJson.addProperty("caveSwimSpeedMultiplier", attributes.getCaveSwimSpeedMultiplier());
                    attributesJson.addProperty("caveReachDistanceAddition", attributes.getCaveReachDistanceAddition());
                    attributesJson.addProperty("caveReachDistanceMultiplier", attributes.getCaveReachDistanceMultiplier());
                    
                    mobConfig.add("attributes", attributesJson);
                    mobs.add(entry.getKey(), mobConfig);
                }
                
                root.add("mobs", mobs);
                GSON.toJson(root, writer);
            }
        } catch (IOException e) {
            LOGGER.error("Failed to save individual mob configs", e);
        }
    }

    public static void saveModConfigs() {
        try {
            Files.createDirectories(CONFIG_DIR);
            JsonObject root = new JsonObject();
            JsonObject mods = new JsonObject();
            
            for (Map.Entry<String, IndividualMobAttributes> entry : modConfigs.entrySet()) {
                String modId = entry.getKey();
                IndividualMobAttributes attributes = entry.getValue();
                
                JsonObject modConfig = new JsonObject();
                JsonObject attributesJson = new JsonObject();
                
                // Сохраняем флаги
                attributesJson.addProperty("enableNightScaling", attributes.getEnableNightScaling());
                attributesJson.addProperty("enableCaveScaling", attributes.getEnableCaveScaling());
                attributesJson.addProperty("enableGravity", attributes.isGravityEnabled());
                attributesJson.addProperty("gravityMultiplier", attributes.getGravityMultiplier());
                // Сохраняем базовые атрибуты
                attributesJson.addProperty("healthAddition", attributes.getHealthAddition());
                attributesJson.addProperty("healthMultiplier", attributes.getHealthMultiplier());
                attributesJson.addProperty("armorAddition", attributes.getArmorAddition());
                attributesJson.addProperty("armorMultiplier", attributes.getArmorMultiplier());
                attributesJson.addProperty("damageAddition", attributes.getDamageAddition());
                attributesJson.addProperty("damageMultiplier", attributes.getDamageMultiplier());
                attributesJson.addProperty("speedAddition", attributes.getSpeedAddition());
                attributesJson.addProperty("speedMultiplier", attributes.getSpeedMultiplier());
                attributesJson.addProperty("knockbackResistanceAddition", attributes.getKnockbackResistanceAddition());
                attributesJson.addProperty("knockbackResistanceMultiplier", attributes.getKnockbackResistanceMultiplier());
                attributesJson.addProperty("attackKnockbackAddition", attributes.getAttackKnockbackAddition());
                attributesJson.addProperty("attackKnockbackMultiplier", attributes.getAttackKnockbackMultiplier());
                attributesJson.addProperty("attackSpeedAddition", attributes.getAttackSpeedAddition());
                attributesJson.addProperty("attackSpeedMultiplier", attributes.getAttackSpeedMultiplier());
                attributesJson.addProperty("followRangeAddition", attributes.getFollowRangeAddition());
                attributesJson.addProperty("followRangeMultiplier", attributes.getFollowRangeMultiplier());
                attributesJson.addProperty("flyingSpeedAddition", attributes.getFlyingSpeedAddition());
                attributesJson.addProperty("flyingSpeedMultiplier", attributes.getFlyingSpeedMultiplier());
                
                // Добавляем дополнительные атрибуты
                attributesJson.addProperty("armorToughnessAddition", attributes.getArmorToughnessAddition());
                attributesJson.addProperty("armorToughnessMultiplier", attributes.getArmorToughnessMultiplier());
                attributesJson.addProperty("luckAddition", attributes.getLuckAddition());
                attributesJson.addProperty("luckMultiplier", attributes.getLuckMultiplier());
                attributesJson.addProperty("swimSpeedAddition", attributes.getSwimSpeedAddition());
                attributesJson.addProperty("swimSpeedMultiplier", attributes.getSwimSpeedMultiplier());
                attributesJson.addProperty("reachDistanceAddition", attributes.getReachDistanceAddition());
                attributesJson.addProperty("reachDistanceMultiplier", attributes.getReachDistanceMultiplier());
                
                // Сохраняем ночные атрибуты
                attributesJson.addProperty("nightHealthAddition", attributes.getNightHealthAddition());
                attributesJson.addProperty("nightHealthMultiplier", attributes.getNightHealthMultiplier());
                attributesJson.addProperty("nightArmorAddition", attributes.getNightArmorAddition());
                attributesJson.addProperty("nightArmorMultiplier", attributes.getNightArmorMultiplier());
                attributesJson.addProperty("nightDamageAddition", attributes.getNightDamageAddition());
                attributesJson.addProperty("nightDamageMultiplier", attributes.getNightDamageMultiplier());
                attributesJson.addProperty("nightSpeedAddition", attributes.getNightSpeedAddition());
                attributesJson.addProperty("nightSpeedMultiplier", attributes.getNightSpeedMultiplier());
                attributesJson.addProperty("nightKnockbackResistanceAddition", attributes.getNightKnockbackResistanceAddition());
                attributesJson.addProperty("nightKnockbackResistanceMultiplier", attributes.getNightKnockbackResistanceMultiplier());
                attributesJson.addProperty("nightAttackKnockbackAddition", attributes.getNightAttackKnockbackAddition());
                attributesJson.addProperty("nightAttackKnockbackMultiplier", attributes.getNightAttackKnockbackMultiplier());
                attributesJson.addProperty("nightAttackSpeedAddition", attributes.getNightAttackSpeedAddition());
                attributesJson.addProperty("nightAttackSpeedMultiplier", attributes.getNightAttackSpeedMultiplier());
                attributesJson.addProperty("nightFollowRangeAddition", attributes.getNightFollowRangeAddition());
                attributesJson.addProperty("nightFollowRangeMultiplier", attributes.getNightFollowRangeMultiplier());
                attributesJson.addProperty("nightFlyingSpeedAddition", attributes.getNightFlyingSpeedAddition());
                attributesJson.addProperty("nightFlyingSpeedMultiplier", attributes.getNightFlyingSpeedMultiplier());
                
                // Добавляем ночные дополнительные атрибуты
                attributesJson.addProperty("nightArmorToughnessAddition", attributes.getNightArmorToughnessAddition());
                attributesJson.addProperty("nightArmorToughnessMultiplier", attributes.getNightArmorToughnessMultiplier());
                attributesJson.addProperty("nightLuckAddition", attributes.getNightLuckAddition());
                attributesJson.addProperty("nightLuckMultiplier", attributes.getNightLuckMultiplier());
                attributesJson.addProperty("nightSwimSpeedAddition", attributes.getNightSwimSpeedAddition());
                attributesJson.addProperty("nightSwimSpeedMultiplier", attributes.getNightSwimSpeedMultiplier());
                attributesJson.addProperty("nightReachDistanceAddition", attributes.getNightReachDistanceAddition());
                attributesJson.addProperty("nightReachDistanceMultiplier", attributes.getNightReachDistanceMultiplier());
                
                // Сохраняем пещерные атрибуты
                attributesJson.addProperty("caveHealthAddition", attributes.getCaveHealthAddition());
                attributesJson.addProperty("caveHealthMultiplier", attributes.getCaveHealthMultiplier());
                attributesJson.addProperty("caveArmorAddition", attributes.getCaveArmorAddition());
                attributesJson.addProperty("caveArmorMultiplier", attributes.getCaveArmorMultiplier());
                attributesJson.addProperty("caveDamageAddition", attributes.getCaveDamageAddition());
                attributesJson.addProperty("caveDamageMultiplier", attributes.getCaveDamageMultiplier());
                attributesJson.addProperty("caveSpeedAddition", attributes.getCaveSpeedAddition());
                attributesJson.addProperty("caveSpeedMultiplier", attributes.getCaveSpeedMultiplier());
                attributesJson.addProperty("caveKnockbackResistanceAddition", attributes.getCaveKnockbackResistanceAddition());
                attributesJson.addProperty("caveKnockbackResistanceMultiplier", attributes.getCaveKnockbackResistanceMultiplier());
                attributesJson.addProperty("caveAttackKnockbackAddition", attributes.getCaveAttackKnockbackAddition());
                attributesJson.addProperty("caveAttackKnockbackMultiplier", attributes.getCaveAttackKnockbackMultiplier());
                attributesJson.addProperty("caveAttackSpeedAddition", attributes.getCaveAttackSpeedAddition());
                attributesJson.addProperty("caveAttackSpeedMultiplier", attributes.getCaveAttackSpeedMultiplier());
                attributesJson.addProperty("caveFollowRangeAddition", attributes.getCaveFollowRangeAddition());
                attributesJson.addProperty("caveFollowRangeMultiplier", attributes.getCaveFollowRangeMultiplier());
                attributesJson.addProperty("caveFlyingSpeedAddition", attributes.getCaveFlyingSpeedAddition());
                attributesJson.addProperty("caveFlyingSpeedMultiplier", attributes.getCaveFlyingSpeedMultiplier());
                
                // Добавляем пещерные дополнительные атрибуты
                attributesJson.addProperty("caveArmorToughnessAddition", attributes.getCaveArmorToughnessAddition());
                attributesJson.addProperty("caveArmorToughnessMultiplier", attributes.getCaveArmorToughnessMultiplier());
                attributesJson.addProperty("caveLuckAddition", attributes.getCaveLuckAddition());
                attributesJson.addProperty("caveLuckMultiplier", attributes.getCaveLuckMultiplier());
                attributesJson.addProperty("caveSwimSpeedAddition", attributes.getCaveSwimSpeedAddition());
                attributesJson.addProperty("caveSwimSpeedMultiplier", attributes.getCaveSwimSpeedMultiplier());
                attributesJson.addProperty("caveReachDistanceAddition", attributes.getCaveReachDistanceAddition());
                attributesJson.addProperty("caveReachDistanceMultiplier", attributes.getCaveReachDistanceMultiplier());
                
                modConfig.add("attributes", attributesJson);
                mods.add(modId, modConfig);
            }
            
            root.add("mods", mods);
            
            try (Writer writer = Files.newBufferedWriter(MOD_MOBS_CONFIG)) {
                GSON.toJson(root, writer);
            }
        } catch (IOException e) {
            LOGGER.error("Failed to save mod configs", e);
        }
    }

    public static void updateIndividualMobConfig(String entityId, IndividualMobConfig config) {
        individualMobConfigs.put(entityId, config);
        com.example.mobscaler.events.EntityHandler.addIndividualMobConfig(entityId, config);
        saveIndividualConfigs();
    }

    public static void updateModConfig(String modId, IndividualMobAttributes config) {
        modConfigs.put(modId, config);
        com.example.mobscaler.events.EntityHandler.addModConfig(modId, config);
        saveModConfigs();
    }

    public static Map<String, IndividualMobConfig> getIndividualMobConfigs() {
        return individualMobConfigs;
    }

    public static Map<String, IndividualMobAttributes> getModConfigs() {
        return modConfigs;
    }

    private static JsonObject getDefaultConfig() {
        JsonObject config = new JsonObject();
        JsonObject mobs = new JsonObject();
        
        // Пример конфигурации для коровы
        JsonObject cowAttributes = new JsonObject();
        
        // Настройки
        cowAttributes.addProperty("enableNightScaling", false);
        cowAttributes.addProperty("enableCaveScaling", false);
        cowAttributes.addProperty("caveHeight", -5.0);
        cowAttributes.addProperty("gravityMultiplier", 1.0);
        // Обычные атрибуты
        cowAttributes.addProperty("healthAddition", 0.0);
        cowAttributes.addProperty("healthMultiplier", 1.0);
        cowAttributes.addProperty("armorAddition", 0.0);
        cowAttributes.addProperty("armorMultiplier", 1.0);
        cowAttributes.addProperty("damageAddition", 0.0);
        cowAttributes.addProperty("damageMultiplier", 1.0);
        cowAttributes.addProperty("speedAddition", 0.0);
        cowAttributes.addProperty("speedMultiplier", 1.0);
        cowAttributes.addProperty("knockbackResistanceAddition", 0.0);
        cowAttributes.addProperty("knockbackResistanceMultiplier", 1.0);
        cowAttributes.addProperty("attackKnockbackAddition", 0.0);
        cowAttributes.addProperty("attackKnockbackMultiplier", 1.0);
        cowAttributes.addProperty("attackSpeedAddition", 0.0);
        cowAttributes.addProperty("attackSpeedMultiplier", 1.0);
        cowAttributes.addProperty("followRangeAddition", 0.0);
        cowAttributes.addProperty("followRangeMultiplier", 1.0);
        cowAttributes.addProperty("flyingSpeedAddition", 0.0);
        cowAttributes.addProperty("flyingSpeedMultiplier", 1.0);
        cowAttributes.addProperty("reachDistanceAddition", 0.0);
        cowAttributes.addProperty("reachDistanceMultiplier", 1.0);
        
        // Ночные атрибуты
        cowAttributes.addProperty("nightHealthAddition", 0.0);
        cowAttributes.addProperty("nightHealthMultiplier", 1.0);
        cowAttributes.addProperty("nightArmorAddition", 0.0);
        cowAttributes.addProperty("nightArmorMultiplier", 1.0);
        cowAttributes.addProperty("nightDamageAddition", 0.0);
        cowAttributes.addProperty("nightDamageMultiplier", 1.0);
        cowAttributes.addProperty("nightSpeedAddition", 0.0);
        cowAttributes.addProperty("nightSpeedMultiplier", 1.0);
        cowAttributes.addProperty("nightKnockbackResistanceAddition", 0.0);
        cowAttributes.addProperty("nightKnockbackResistanceMultiplier", 1.0);
        cowAttributes.addProperty("nightAttackKnockbackAddition", 0.0);
        cowAttributes.addProperty("nightAttackKnockbackMultiplier", 1.0);
        cowAttributes.addProperty("nightAttackSpeedAddition", 0.0);
        cowAttributes.addProperty("nightAttackSpeedMultiplier", 1.0);
        cowAttributes.addProperty("nightFollowRangeAddition", 0.0);
        cowAttributes.addProperty("nightFollowRangeMultiplier", 1.0);
        cowAttributes.addProperty("nightFlyingSpeedAddition", 0.0);
        cowAttributes.addProperty("nightFlyingSpeedMultiplier", 1.0);
        cowAttributes.addProperty("nightReachDistanceAddition", 0.0);
        cowAttributes.addProperty("nightReachDistanceMultiplier", 1.0);
        // Пещерные атрибуты
        cowAttributes.addProperty("caveHealthAddition", 0.0);
        cowAttributes.addProperty("caveHealthMultiplier", 1.0);
        cowAttributes.addProperty("caveArmorAddition", 0.0);
        cowAttributes.addProperty("caveArmorMultiplier", 1.0);
        cowAttributes.addProperty("caveDamageAddition", 0.0);
        cowAttributes.addProperty("caveDamageMultiplier", 1.0);
        cowAttributes.addProperty("caveSpeedAddition", 0.0);
        cowAttributes.addProperty("caveSpeedMultiplier", 1.0);
        cowAttributes.addProperty("caveKnockbackResistanceAddition", 0.0);
        cowAttributes.addProperty("caveKnockbackResistanceMultiplier", 1.0);
        cowAttributes.addProperty("caveAttackKnockbackAddition", 0.0);
        cowAttributes.addProperty("caveAttackKnockbackMultiplier", 1.0);
        cowAttributes.addProperty("caveAttackSpeedAddition", 0.0);
        cowAttributes.addProperty("caveAttackSpeedMultiplier", 1.0);
        cowAttributes.addProperty("caveFollowRangeAddition", 0.0);
        cowAttributes.addProperty("caveFollowRangeMultiplier", 1.0);
        cowAttributes.addProperty("caveFlyingSpeedAddition", 0.0);
        cowAttributes.addProperty("caveFlyingSpeedMultiplier", 1.0);
        cowAttributes.addProperty("caveReachDistanceAddition", 0.0); 
        cowAttributes.addProperty("caveReachDistanceMultiplier", 1.0);
        config.add("mobs", mobs);

        return config;
    }

    private static IndividualMobConfig loadMobConfig(JsonObject mobJson) {
        boolean blacklisted = mobJson.has("isBlacklisted") ? mobJson.get("isBlacklisted").getAsBoolean() : false;
        JsonObject attributesJson = mobJson.getAsJsonObject("attributes");

        IndividualMobAttributes attributes = new IndividualMobAttributes(
            // Настройки
            getBoolean(attributesJson, "enableNightScaling", false),
            getBoolean(attributesJson, "enableCaveScaling", false),
            getDouble(attributesJson, "caveHeight", -5.0),
            getDouble(attributesJson, "gravityMultiplier", 1.0),
            getBoolean(attributesJson, "enableGravity", false),
            // Обычные атрибуты
            getDouble(attributesJson, "healthAddition", 0.0),
            getDouble(attributesJson, "healthMultiplier", 1.0),
            getDouble(attributesJson, "armorAddition", 0.0),
            getDouble(attributesJson, "armorMultiplier", 1.0),
            getDouble(attributesJson, "damageAddition", 0.0),
            getDouble(attributesJson, "damageMultiplier", 1.0),
            getDouble(attributesJson, "speedAddition", 0.0),
            getDouble(attributesJson, "speedMultiplier", 1.0),
            getDouble(attributesJson, "knockbackResistanceAddition", 0.0),
            getDouble(attributesJson, "knockbackResistanceMultiplier", 1.0),
            getDouble(attributesJson, "attackKnockbackAddition", 0.0),
            getDouble(attributesJson, "attackKnockbackMultiplier", 1.0),
            getDouble(attributesJson, "attackSpeedAddition", 0.0),
            getDouble(attributesJson, "attackSpeedMultiplier", 1.0),
            getDouble(attributesJson, "followRangeAddition", 0.0),
            getDouble(attributesJson, "followRangeMultiplier", 1.0),
            getDouble(attributesJson, "flyingSpeedAddition", 0.0),
            getDouble(attributesJson, "flyingSpeedMultiplier", 1.0),
            getDouble(attributesJson, "armorToughnessAddition", 0.0),
            getDouble(attributesJson, "armorToughnessMultiplier", 1.0),
            getDouble(attributesJson, "luckAddition", 0.0),
            getDouble(attributesJson, "luckMultiplier", 1.0),
            getDouble(attributesJson, "swimSpeedAddition", 0.0),
            getDouble(attributesJson, "swimSpeedMultiplier", 1.0),
            getDouble(attributesJson, "reachDistanceAddition", 0.0),
            getDouble(attributesJson, "reachDistanceMultiplier", 1.0),
            // Ночные атрибуты
            getDouble(attributesJson, "nightHealthAddition", 0.0),
            getDouble(attributesJson, "nightHealthMultiplier", 1.0),
            getDouble(attributesJson, "nightArmorAddition", 0.0),
            getDouble(attributesJson, "nightArmorMultiplier", 1.0),
            getDouble(attributesJson, "nightDamageAddition", 0.0),
            getDouble(attributesJson, "nightDamageMultiplier", 1.0),
            getDouble(attributesJson, "nightSpeedAddition", 0.0),
            getDouble(attributesJson, "nightSpeedMultiplier", 1.0),
            getDouble(attributesJson, "nightKnockbackResistanceAddition", 0.0),
            getDouble(attributesJson, "nightKnockbackResistanceMultiplier", 1.0),
            getDouble(attributesJson, "nightAttackKnockbackAddition", 0.0),
            getDouble(attributesJson, "nightAttackKnockbackMultiplier", 1.0),
            getDouble(attributesJson, "nightAttackSpeedAddition", 0.0),
            getDouble(attributesJson, "nightAttackSpeedMultiplier", 1.0),
            getDouble(attributesJson, "nightFollowRangeAddition", 0.0),
            getDouble(attributesJson, "nightFollowRangeMultiplier", 1.0),
            getDouble(attributesJson, "nightFlyingSpeedAddition", 0.0),
            getDouble(attributesJson, "nightFlyingSpeedMultiplier", 1.0),
            getDouble(attributesJson, "nightArmorToughnessAddition", 0.0),
            getDouble(attributesJson, "nightArmorToughnessMultiplier", 1.0),
            getDouble(attributesJson, "nightLuckAddition", 0.0),
            getDouble(attributesJson, "nightLuckMultiplier", 1.0),
            getDouble(attributesJson, "nightSwimSpeedAddition", 0.0),
            getDouble(attributesJson, "nightSwimSpeedMultiplier", 1.0),
            getDouble(attributesJson, "nightReachDistanceAddition", 0.0),
            getDouble(attributesJson, "nightReachDistanceMultiplier", 1.0),
            // Пещерные атрибуты
            getDouble(attributesJson, "caveHealthAddition", 0.0),
            getDouble(attributesJson, "caveHealthMultiplier", 1.0),
            getDouble(attributesJson, "caveArmorAddition", 0.0),
            getDouble(attributesJson, "caveArmorMultiplier", 1.0),
            getDouble(attributesJson, "caveDamageAddition", 0.0),
            getDouble(attributesJson, "caveDamageMultiplier", 1.0),
            getDouble(attributesJson, "caveSpeedAddition", 0.0),
            getDouble(attributesJson, "caveSpeedMultiplier", 1.0),
            getDouble(attributesJson, "caveKnockbackResistanceAddition", 0.0),
            getDouble(attributesJson, "caveKnockbackResistanceMultiplier", 1.0),
            getDouble(attributesJson, "caveAttackKnockbackAddition", 0.0),
            getDouble(attributesJson, "caveAttackKnockbackMultiplier", 1.0),
            getDouble(attributesJson, "caveAttackSpeedAddition", 0.0),
            getDouble(attributesJson, "caveAttackSpeedMultiplier", 1.0),
            getDouble(attributesJson, "caveFollowRangeAddition", 0.0),
            getDouble(attributesJson, "caveFollowRangeMultiplier", 1.0),
            getDouble(attributesJson, "caveFlyingSpeedAddition", 0.0),
            getDouble(attributesJson, "caveFlyingSpeedMultiplier", 1.0),
            getDouble(attributesJson, "caveArmorToughnessAddition", 0.0),
            getDouble(attributesJson, "caveArmorToughnessMultiplier", 1.0),
            getDouble(attributesJson, "caveLuckAddition", 0.0),
            getDouble(attributesJson, "caveLuckMultiplier", 1.0),
            getDouble(attributesJson, "caveSwimSpeedAddition", 0.0),
            getDouble(attributesJson, "caveSwimSpeedMultiplier", 1.0),
            getDouble(attributesJson, "caveReachDistanceAddition", 0.0),
            getDouble(attributesJson, "caveReachDistanceMultiplier", 1.0),
            blacklisted
        );

        return new IndividualMobConfig(blacklisted, attributes);
    }

    private static IndividualMobAttributes loadModConfig(JsonObject attributesJson) {
        return new IndividualMobAttributes(
            // Настройки
            getBoolean(attributesJson, "enableNightScaling", false),
            getBoolean(attributesJson, "enableCaveScaling", false),
            getDouble(attributesJson, "caveHeight", -5.0),
            getDouble(attributesJson, "gravityMultiplier", 1.0),
            getBoolean(attributesJson, "enableGravity", false),
            // Обычные атрибуты
            getDouble(attributesJson, "healthAddition", 0.0),
            getDouble(attributesJson, "healthMultiplier", 1.0),
            getDouble(attributesJson, "armorAddition", 0.0),
            getDouble(attributesJson, "armorMultiplier", 1.0),
            getDouble(attributesJson, "damageAddition", 0.0),
            getDouble(attributesJson, "damageMultiplier", 1.0),
            getDouble(attributesJson, "speedAddition", 0.0),
            getDouble(attributesJson, "speedMultiplier", 1.0),
            getDouble(attributesJson, "knockbackResistanceAddition", 0.0),
            getDouble(attributesJson, "knockbackResistanceMultiplier", 1.0),
            getDouble(attributesJson, "attackKnockbackAddition", 0.0),
            getDouble(attributesJson, "attackKnockbackMultiplier", 1.0),
            getDouble(attributesJson, "attackSpeedAddition", 0.0),
            getDouble(attributesJson, "attackSpeedMultiplier", 1.0),
            getDouble(attributesJson, "followRangeAddition", 0.0),
            getDouble(attributesJson, "followRangeMultiplier", 1.0),
            getDouble(attributesJson, "flyingSpeedAddition", 0.0),
            getDouble(attributesJson, "flyingSpeedMultiplier", 1.0),
            getDouble(attributesJson, "armorToughnessAddition", 0.0),
            getDouble(attributesJson, "armorToughnessMultiplier", 1.0),
            getDouble(attributesJson, "luckAddition", 0.0),
            getDouble(attributesJson, "luckMultiplier", 1.0),
            getDouble(attributesJson, "swimSpeedAddition", 0.0),
            getDouble(attributesJson, "swimSpeedMultiplier", 1.0),
            getDouble(attributesJson, "reachDistanceAddition", 0.0),
            getDouble(attributesJson, "reachDistanceMultiplier", 1.0),
            // Ночные атрибуты
            getDouble(attributesJson, "nightHealthAddition", 0.0),
            getDouble(attributesJson, "nightHealthMultiplier", 1.0),
            getDouble(attributesJson, "nightArmorAddition", 0.0),
            getDouble(attributesJson, "nightArmorMultiplier", 1.0),
            getDouble(attributesJson, "nightDamageAddition", 0.0),
            getDouble(attributesJson, "nightDamageMultiplier", 1.0),
            getDouble(attributesJson, "nightSpeedAddition", 0.0),
            getDouble(attributesJson, "nightSpeedMultiplier", 1.0),
            getDouble(attributesJson, "nightKnockbackResistanceAddition", 0.0),
            getDouble(attributesJson, "nightKnockbackResistanceMultiplier", 1.0),
            getDouble(attributesJson, "nightAttackKnockbackAddition", 0.0),
            getDouble(attributesJson, "nightAttackKnockbackMultiplier", 1.0),
            getDouble(attributesJson, "nightAttackSpeedAddition", 0.0),
            getDouble(attributesJson, "nightAttackSpeedMultiplier", 1.0),
            getDouble(attributesJson, "nightFollowRangeAddition", 0.0),
            getDouble(attributesJson, "nightFollowRangeMultiplier", 1.0),
            getDouble(attributesJson, "nightFlyingSpeedAddition", 0.0),
            getDouble(attributesJson, "nightFlyingSpeedMultiplier", 1.0),
            getDouble(attributesJson, "nightArmorToughnessAddition", 0.0),
            getDouble(attributesJson, "nightArmorToughnessMultiplier", 1.0),
            getDouble(attributesJson, "nightLuckAddition", 0.0),
            getDouble(attributesJson, "nightLuckMultiplier", 1.0),
            getDouble(attributesJson, "nightSwimSpeedAddition", 0.0),
            getDouble(attributesJson, "nightSwimSpeedMultiplier", 1.0),
            getDouble(attributesJson, "nightReachDistanceAddition", 0.0),
            getDouble(attributesJson, "nightReachDistanceMultiplier", 1.0),
            // Пещерные атрибуты
            getDouble(attributesJson, "caveHealthAddition", 0.0),
            getDouble(attributesJson, "caveHealthMultiplier", 1.0),
            getDouble(attributesJson, "caveArmorAddition", 0.0),
            getDouble(attributesJson, "caveArmorMultiplier", 1.0),
            getDouble(attributesJson, "caveDamageAddition", 0.0),
            getDouble(attributesJson, "caveDamageMultiplier", 1.0),
            getDouble(attributesJson, "caveSpeedAddition", 0.0),
            getDouble(attributesJson, "caveSpeedMultiplier", 1.0),
            getDouble(attributesJson, "caveKnockbackResistanceAddition", 0.0),
            getDouble(attributesJson, "caveKnockbackResistanceMultiplier", 1.0),
            getDouble(attributesJson, "caveAttackKnockbackAddition", 0.0),
            getDouble(attributesJson, "caveAttackKnockbackMultiplier", 1.0),
            getDouble(attributesJson, "caveAttackSpeedAddition", 0.0),
            getDouble(attributesJson, "caveAttackSpeedMultiplier", 1.0),
            getDouble(attributesJson, "caveFollowRangeAddition", 0.0),
            getDouble(attributesJson, "caveFollowRangeMultiplier", 1.0),
            getDouble(attributesJson, "caveFlyingSpeedAddition", 0.0),
            getDouble(attributesJson, "caveFlyingSpeedMultiplier", 1.0),
            getDouble(attributesJson, "caveArmorToughnessAddition", 0.0),
            getDouble(attributesJson, "caveArmorToughnessMultiplier", 1.0),
            getDouble(attributesJson, "caveLuckAddition", 0.0),
            getDouble(attributesJson, "caveLuckMultiplier", 1.0),
            getDouble(attributesJson, "caveSwimSpeedAddition", 0.0),
            getDouble(attributesJson, "caveSwimSpeedMultiplier", 1.0),
            getDouble(attributesJson, "caveReachDistanceAddition", 0.0),
            getDouble(attributesJson, "caveReachDistanceMultiplier", 1.0),
            false // blacklisted
        );
    }

    private static JsonObject getDefaultModAttributes() {
        JsonObject attributes = new JsonObject();
        
        // Настройки
        attributes.addProperty("enableNightScaling", false);
        attributes.addProperty("enableCaveScaling", false);
        attributes.addProperty("caveHeight", -5.0);
        attributes.addProperty("gravityMultiplier", 1.0);
        // Обычные атрибуты
        attributes.addProperty("healthAddition", 0.0);
        attributes.addProperty("healthMultiplier", 1.0);
        attributes.addProperty("armorAddition", 0.0);
        attributes.addProperty("armorMultiplier", 1.0);
        attributes.addProperty("damageAddition", 0.0);
        attributes.addProperty("damageMultiplier", 1.0);
        attributes.addProperty("speedAddition", 0.0);
        attributes.addProperty("speedMultiplier", 1.0);
        attributes.addProperty("knockbackResistanceAddition", 0.0);
        attributes.addProperty("knockbackResistanceMultiplier", 1.0);
        attributes.addProperty("attackKnockbackAddition", 0.0);
        attributes.addProperty("attackKnockbackMultiplier", 1.0);
        attributes.addProperty("attackSpeedAddition", 0.0);
        attributes.addProperty("attackSpeedMultiplier", 1.0);
        attributes.addProperty("followRangeAddition", 0.0);
        attributes.addProperty("followRangeMultiplier", 1.0);
        attributes.addProperty("flyingSpeedAddition", 0.0);
        attributes.addProperty("flyingSpeedMultiplier", 1.0);

        // Ночные атрибуты
        attributes.addProperty("nightHealthAddition", 0.0);
        attributes.addProperty("nightHealthMultiplier", 1.0);
        attributes.addProperty("nightArmorAddition", 0.0);
        attributes.addProperty("nightArmorMultiplier", 1.0);
        attributes.addProperty("nightDamageAddition", 0.0);
        attributes.addProperty("nightDamageMultiplier", 1.0);
        attributes.addProperty("nightSpeedAddition", 0.0);
        attributes.addProperty("nightSpeedMultiplier", 1.0);
        attributes.addProperty("nightKnockbackResistanceAddition", 0.0);
        attributes.addProperty("nightKnockbackResistanceMultiplier", 1.0);
        attributes.addProperty("nightAttackKnockbackAddition", 0.0);
        attributes.addProperty("nightAttackKnockbackMultiplier", 1.0);
        attributes.addProperty("nightAttackSpeedAddition", 0.0);
        attributes.addProperty("nightAttackSpeedMultiplier", 1.0);
        attributes.addProperty("nightFollowRangeAddition", 0.0);
        attributes.addProperty("nightFollowRangeMultiplier", 1.0);
        attributes.addProperty("nightFlyingSpeedAddition", 0.0);
        attributes.addProperty("nightFlyingSpeedMultiplier", 1.0);

        // Пещерные атрибуты
        attributes.addProperty("caveHealthAddition", 0.0);
        attributes.addProperty("caveHealthMultiplier", 1.0);
        attributes.addProperty("caveArmorAddition", 0.0);
        attributes.addProperty("caveArmorMultiplier", 1.0);
        attributes.addProperty("caveDamageAddition", 0.0);
        attributes.addProperty("caveDamageMultiplier", 1.0);
        attributes.addProperty("caveSpeedAddition", 0.0);
        attributes.addProperty("caveSpeedMultiplier", 1.0);
        attributes.addProperty("caveKnockbackResistanceAddition", 0.0);
        attributes.addProperty("caveKnockbackResistanceMultiplier", 1.0);
        attributes.addProperty("caveAttackKnockbackAddition", 0.0);
        attributes.addProperty("caveAttackKnockbackMultiplier", 1.0);
        attributes.addProperty("caveAttackSpeedAddition", 0.0);
        attributes.addProperty("caveAttackSpeedMultiplier", 1.0);
        attributes.addProperty("caveFollowRangeAddition", 0.0);
        attributes.addProperty("caveFollowRangeMultiplier", 1.0);
        attributes.addProperty("caveFlyingSpeedAddition", 0.0);
        attributes.addProperty("caveFlyingSpeedMultiplier", 1.0);

        return attributes;
    }
} 