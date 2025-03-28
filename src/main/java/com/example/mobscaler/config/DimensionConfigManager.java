package com.example.mobscaler.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.registries.ForgeRegistry;
import net.minecraftforge.registries.RegistryManager;
import java.io.*;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

public class DimensionConfigManager {
    private static final Gson GSON = new GsonBuilder()
        .setPrettyPrinting()
        .disableHtmlEscaping()
        .serializeNulls()
        .create();
    private static final Path CONFIG_PATH = Paths.get("config", "mobscaler", "dimensions.json");
    private static Map<String, DimensionConfig> dimensionConfigs = new HashMap<>();

    public static void loadConfigs() {
         if (!Files.exists(CONFIG_PATH)) {
             // Файл не существует – создаём дефолтные настройки и сохраняем
             dimensionConfigs = getDefaultDimensionConfigs();
             saveConfigs();
         } else {
             try {
                 String content = Files.readString(CONFIG_PATH);
                 if (content.contains("\"healthAddition\"") && !content.contains("\"speedAddition\"")) {
                     // Старый формат конфига - создаем новый
                     dimensionConfigs = getDefaultDimensionConfigs();
                     // Сохраняем старый файл как backup
                     Files.move(CONFIG_PATH, CONFIG_PATH.resolveSibling("dimensions.json.old"));
                     saveConfigs();
                 } else {
                     try (Reader reader = new FileReader(CONFIG_PATH.toFile())) {
                         Type type = new TypeToken<Map<String, DimensionConfig>>(){}.getType();
                         Map<String, DimensionConfig> loadedConfigs = GSON.fromJson(reader, type);
                         if (loadedConfigs != null) {
                             dimensionConfigs = loadedConfigs;
                         } else {
                             // Если конфиг поврежден или пуст, создаем новый
                             dimensionConfigs = getDefaultDimensionConfigs();
                             saveConfigs();
                         }
                     }
                 }
             } catch (IOException e) {
                 e.printStackTrace();
                 // В случае ошибки создаем новый конфиг
                 dimensionConfigs = getDefaultDimensionConfigs();
                 saveConfigs();
             }
         }
         // Добавляем измерения из реестра
         addRegistryDimensions();
    }

    public static Map<String, DimensionConfig> getDimensionConfigs() {
        return dimensionConfigs;
    }

    public static void saveConfigs() {
         try {
            Files.createDirectories(CONFIG_PATH.getParent());
            try (Writer writer = new FileWriter(CONFIG_PATH.toFile())) {
                GSON.toJson(dimensionConfigs, writer);
            }
         } catch(IOException e) {
             e.printStackTrace();
         }
    }

    private static Map<String, DimensionConfig> getDefaultDimensionConfigs() {
        Map<String, DimensionConfig> configs = new HashMap<>();
        
        // Добавляем конфигурацию для обычного мира
        configs.put("minecraft:overworld", new DimensionConfig(
            true, // enableNightScaling
            true, // enableCaveScaling
            40.0, // caveHeight
            false, // enableGravity
            1.0, // gravityMultiplier
            // Дневные настройки
            0.0, 1.0, // health
            0.0, 1.0, // armor
            0.0, 1.0, // damage
            0.0, 1.0, // speed
            0.0, 1.0, // knockback resistance
            0.0, 1.0, // attack knockback
            0.0, 1.0, // attack speed
            0.0, 1.0, // follow range
            0.0, 1.0, // flying speed
            0.0, 1.0, // armor toughness
            0.0, 1.0, // luck
            0.0, 1.0, // swim speed
            0.0, 1.0, // reach distance
            // Ночные настройки
            2.0, 1.2, // night health
            1.0, 1.1, // night armor
            1.0, 1.2, // night damage
            0.0, 1.1, // night speed
            0.0, 1.0, // night knockback resistance
            0.0, 1.0, // night attack knockback
            0.0, 1.1, // night attack speed
            0.0, 1.2, // night follow range
            0.0, 1.0, // night flying speed
            0.0, 1.0, // night armor toughness
            0.0, 1.0, // night luck
            0.0, 1.0, // night swim speed
            0.0, 1.0, // night reach distance
            // Пещерные настройки
            4.0, 1.3, // cave health
            2.0, 1.2, // cave armor
            2.0, 1.3, // cave damage
            0.0, 1.2, // cave speed
            0.1, 1.1, // cave knockback resistance
            0.0, 1.1, // cave attack knockback
            0.0, 1.2, // cave attack speed
            0.0, 1.3, // cave follow range
            0.0, 1.0, // cave flying speed
            0.0, 1.0, // cave armor toughness
            0.0, 1.0, // cave luck
            0.0, 1.0, // cave swim speed
            0.0, 1.0, // cave reach distance
            // Черные списки
            new ArrayList<String>(), // modBlacklist
            new ArrayList<String>()  // entityBlacklist
        ));
        
        // Добавляем конфигурацию для Нижнего мира
        configs.put("minecraft:the_nether", new DimensionConfig(
            false, // enableNightScaling
            true, // enableCaveScaling
            40.0, // caveHeight
            false, // enableGravity
            1.0, // gravityMultiplier
            // Дневные настройки
            2.0, 1.2, // health
            1.0, 1.1, // armor
            1.0, 1.2, // damage
            0.0, 1.1, // speed
            0.0, 1.0, // knockback resistance
            0.0, 1.0, // attack knockback
            0.0, 1.1, // attack speed
            0.0, 1.2, // follow range
            0.0, 1.0, // flying speed
            0.0, 1.0, // armor toughness
            0.0, 1.0, // luck
            0.0, 1.0, // swim speed
            0.0, 1.0, // reach distance
            // Ночные настройки
            0.0, 1.0, // night health
            0.0, 1.0, // night armor
            0.0, 1.0, // night damage
            0.0, 1.0, // night speed
            0.0, 1.0, // night knockback resistance
            0.0, 1.0, // night attack knockback
            0.0, 1.0, // night attack speed
            0.0, 1.0, // night follow range
            0.0, 1.0, // night flying speed
            0.0, 1.0, // night armor toughness
            0.0, 1.0, // night luck
            0.0, 1.0, // night swim speed
            0.0, 1.0, // night reach distance
            // Пещерные настройки
            4.0, 1.3, // cave health
            2.0, 1.2, // cave armor
            2.0, 1.3, // cave damage
            0.0, 1.2, // cave speed
            0.1, 1.1, // cave knockback resistance
            0.0, 1.1, // cave attack knockback
            0.0, 1.2, // cave attack speed
            0.0, 1.3, // cave follow range
            0.0, 1.0, // cave flying speed
            0.0, 1.0, // cave armor toughness
            0.0, 1.0, // cave luck
            0.0, 1.0, // cave swim speed
            0.0, 1.0, // cave reach distance
            // Черные списки
            new ArrayList<String>(), // modBlacklist
            new ArrayList<String>()  // entityBlacklist
        ));
        
        // Добавляем конфигурацию для Края
        configs.put("minecraft:the_end", new DimensionConfig(
            false, // enableNightScaling
            false, // enableCaveScaling
            0.0, // caveHeight
            false, // enableGravity
            1.0, // gravityMultiplier
            // Дневные настройки
            4.0, 1.3, // health
            2.0, 1.2, // armor
            2.0, 1.3, // damage
            0.0, 1.2, // speed
            0.1, 1.1, // knockback resistance
            0.0, 1.1, // attack knockback
            0.0, 1.2, // attack speed
            0.0, 1.3, // follow range
            0.0, 1.0, // flying speed
            0.0, 1.0, // armor toughness
            0.0, 1.0, // luck
            0.0, 1.0, // swim speed
            0.0, 1.0, // reach distance
            // Ночные настройки
            0.0, 1.0, // night health
            0.0, 1.0, // night armor
            0.0, 1.0, // night damage
            0.0, 1.0, // night speed
            0.0, 1.0, // night knockback resistance
            0.0, 1.0, // night attack knockback
            0.0, 1.0, // night attack speed
            0.0, 1.0, // night follow range
            0.0, 1.0, // night flying speed
            0.0, 1.0, // night armor toughness
            0.0, 1.0, // night luck
            0.0, 1.0, // night swim speed
            0.0, 1.0, // night reach distance
            // Пещерные настройки
            0.0, 1.0, // cave health
            0.0, 1.0, // cave armor
            0.0, 1.0, // cave damage
            0.0, 1.0, // cave speed
            0.0, 1.0, // cave knockback resistance
            0.0, 1.0, // cave attack knockback
            0.0, 1.0, // cave attack speed
            0.0, 1.0, // cave follow range
            0.0, 1.0, // cave flying speed
            0.0, 1.0, // cave armor toughness
            0.0, 1.0, // cave luck
            0.0, 1.0, // cave swim speed
            0.0, 1.0, // cave reach distance
            // Черные списки
            new ArrayList<String>(), // modBlacklist
            new ArrayList<String>()  // entityBlacklist
        ));
        
        return configs;
    }

    private static void addRegistryDimensions() {
        ForgeRegistry<?> dimRegistry = RegistryManager.ACTIVE.getRegistry(new ResourceLocation("minecraft:dimension_type"));
        if (dimRegistry != null) {
            for (ResourceLocation id : dimRegistry.getKeys()) {
                String dimId = id.toString();
                if (!dimensionConfigs.containsKey(dimId)) {
                    dimensionConfigs.put(dimId, new DimensionConfig(
                        false, // enableNightScaling
                        false, // enableCaveScaling
                        -5.0,  // caveHeight
                        false, // enableGravity
                        1.0, // gravityMultiplier
                        // Дневные настройки
                        0.0, 1.0,  // health
                        0.0, 1.0,  // armor
                        0.0, 1.0,  // damage
                        0.0, 1.0,  // speed
                        0.0, 1.0,  // knockback resistance
                        0.0, 1.0,  // attack knockback
                        0.0, 1.0,  // attack speed
                        0.0, 1.0,  // follow range
                        0.0, 1.0,  // flying speed
                        0.0, 1.0,  // armor toughness
                        0.0, 1.0,  // luck
                        0.0, 1.0,  // swim speed
                        0.0, 1.0,  // reach distance
                        // Ночные настройки
                        0.0, 1.0,  // night health
                        0.0, 1.0,  // night armor
                        0.0, 1.0,  // night damage
                        0.0, 1.0,  // night speed
                        0.0, 1.0,  // night knockback resistance
                        0.0, 1.0,  // night attack knockback
                        0.0, 1.0,  // night attack speed
                        0.0, 1.0,  // night follow range
                        0.0, 1.0,  // night flying speed
                        0.0, 1.0,  // night armor toughness
                        0.0, 1.0,  // night luck
                        0.0, 1.0,  // night swim speed
                        0.0, 1.0,  // night reach distance
                        // Пещерные настройки
                        0.0, 1.0,  // cave health
                        0.0, 1.0,  // cave armor
                        0.0, 1.0,  // cave damage
                        0.0, 1.0,  // cave speed
                        0.0, 1.0,  // cave knockback resistance
                        0.0, 1.0,  // cave attack knockback
                        0.0, 1.0,  // cave attack speed
                        0.0, 1.0,  // cave follow range
                        0.0, 1.0,  // cave flying speed
                        0.0, 1.0,  // cave armor toughness
                        0.0, 1.0,  // cave luck
                        0.0, 1.0,  // cave swim speed
                        0.0, 1.0,  // cave reach distance
                        // Общие черные списки
                        new ArrayList<String>(),  // modBlacklist
                        new ArrayList<String>()   // entityBlacklist
                    ));
                }
            }
            saveConfigs();
        }
    }
}
