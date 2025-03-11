package com.example.mobscaler.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

public class CaveConfigManager {
    private static final Gson GSON = new GsonBuilder()
        .setPrettyPrinting()
        .disableHtmlEscaping()
        .serializeNulls()
        .create();
    private static final Path CONFIG_PATH = Paths.get("config", "mobscaler", "caves.json");
    private static Map<String, CaveConfig> caveConfigs = new HashMap<>();

    public static void loadConfigs() {
        if (!Files.exists(CONFIG_PATH)) {
            // Файл не существует – создаём дефолтные настройки и сохраняем
            caveConfigs = getDefaultCaveConfigs();
            saveConfigs();
        } else {
            try (Reader reader = new FileReader(CONFIG_PATH.toFile())) {
                Map<String, CaveConfig> loadedConfigs = GSON.fromJson(reader, 
                    new com.google.gson.reflect.TypeToken<Map<String, CaveConfig>>(){}.getType());
                if (loadedConfigs != null) {
                    caveConfigs = loadedConfigs;
                } else {
                    // Если конфиг поврежден или пуст, создаем новый
                    caveConfigs = getDefaultCaveConfigs();
                    saveConfigs();
                }
            } catch (IOException e) {
                e.printStackTrace();
                // В случае ошибки создаем новый конфиг
                caveConfigs = getDefaultCaveConfigs();
                saveConfigs();
            }
        }
    }

    public static Map<String, CaveConfig> getCaveConfigs() {
        return caveConfigs;
    }

    public static void saveConfigs() {
        try {
            Files.createDirectories(CONFIG_PATH.getParent());
            try (Writer writer = new FileWriter(CONFIG_PATH.toFile())) {
                GSON.toJson(caveConfigs, writer);
            }
        } catch(IOException e) {
            e.printStackTrace();
        }
    }

    private static Map<String, CaveConfig> getDefaultCaveConfigs() {
        Map<String, CaveConfig> defaults = new HashMap<>();
        
        // Конфигурация для обычного мира
        defaults.put("minecraft:overworld", new CaveConfig(
            false,  // enableCaveMode
            -5,     // caveHeight (Y-координата, ниже которой действуют пещерные усиления)
            // Базовые модификаторы для пещер
            5.0, 1.2,   // health
            2.0, 1.2,   // armor
            2.0, 1.2,   // damage
            0.0, 1.2,   // speed
            0.1, 1.2,   // knockback resistance
            0.0, 1.2,   // attack knockback
            0.0, 1.2,   // attack speed
            2.0, 1.2,   // follow range
            0.0, 1.2,   // flying speed
            Arrays.asList("examplemod"),  // modBlacklist
            Arrays.asList("minecraft:bat")  // entityBlacklist
        ));

        // Конфигурация для Нижнего мира (пещерные усиления по умолчанию отключены)
        defaults.put("minecraft:the_nether", new CaveConfig(
            false,  // enableCaveMode
            -50,     // caveHeight
            5.0, 1.2,   // health
            2.0, 1.2,   // armor
            2.0, 1.2,   // damage
            0.0, 1.2,   // speed
            0.1, 1.2,   // knockback resistance
            0.0, 1.2,   // attack knockback
            0.0, 1.0,   // attack speed
            0.0, 1.0,   // follow range
            0.0, 1.0,   // flying speed
            new ArrayList<>(),  // modBlacklist
            new ArrayList<>()   // entityBlacklist
        ));

        // Конфигурация для Края (пещерные усиления по умолчанию отключены)
        defaults.put("minecraft:the_end", new CaveConfig(
            false,  // enableCaveMode
            -5,     // caveHeight
            5.0, 1.2,   // health
            2.0, 1.2,   // armor
            2.0, 1.2,   // damage
            0.0, 1.2,   // speed
            0.1, 1.2,   // knockback resistance
            0.0, 1.2,   // attack knockback
            0.0, 1.0,   // attack speed
            0.0, 1.0,   // follow range
            0.0, 1.0,   // flying speed
            new ArrayList<>(),  // modBlacklist
            new ArrayList<>()   // entityBlacklist
        ));

        return defaults;
    }
} 