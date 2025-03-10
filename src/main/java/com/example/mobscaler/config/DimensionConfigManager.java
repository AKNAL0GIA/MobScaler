package com.example.mobscaler.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
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
         Map<String, DimensionConfig> defaults = new HashMap<>();
         defaults.put("minecraft:overworld", new DimensionConfig(
             false, // enableNightScaling
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
             Arrays.asList("examplemod"),
             Arrays.asList("examplemobid")
         ));
         defaults.put("minecraft:the_nether", new DimensionConfig(
             false, // enableNightScaling
             // Дневные настройки
             5.0, 1.5,  // health
             5.0, 1.5,  // armor
             3.0, 1.5,  // damage
             0.0, 1.0,  // speed
             0.0, 1.0,  // knockback resistance
             0.0, 1.0,  // attack knockback
             0.0, 1.0,  // attack speed
             0.0, 1.0,  // follow range
             0.0, 1.0,  // flying speed
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
             new ArrayList<>(),
             Arrays.asList("minecraft:ender_dragon")
         ));
         defaults.put("minecraft:the_end", new DimensionConfig(
             false, // enableNightScaling
             // Дневные настройки
             10.0, 2.0,  // health
             10.0, 2.0,  // armor
             5.0, 2.0,   // damage
             0.0, 1.0,   // speed
             0.0, 1.0,   // knockback resistance
             0.0, 1.0,   // attack knockback
             0.0, 1.0,   // attack speed
             0.0, 1.0,   // follow range
             0.0, 1.0,   // flying speed
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
             new ArrayList<>(),
             Arrays.asList("minecraft:wither")
         ));
         return defaults;
    }
}
