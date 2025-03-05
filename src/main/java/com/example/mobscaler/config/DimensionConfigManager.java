package com.example.mobscaler.config;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.io.*;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

public class DimensionConfigManager {
    private static final Gson GSON = new Gson();
    private static final Path CONFIG_PATH = Paths.get("config", "mobscaler", "dimensions.json");
    private static Map<String, DimensionConfig> dimensionConfigs = new HashMap<>();

    public static void loadConfigs() {
         if (!Files.exists(CONFIG_PATH)) {
             // Файл не существует – создаём дефолтные настройки и сохраняем
             dimensionConfigs = getDefaultDimensionConfigs();
             saveConfigs();
         } else {
             try (Reader reader = new FileReader(CONFIG_PATH.toFile())) {
                 Type type = new TypeToken<Map<String, DimensionConfig>>(){}.getType();
                 dimensionConfigs = GSON.fromJson(reader, type);
             } catch (IOException e) {
                 e.printStackTrace();
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
             0, 1.0,
             0, 1.0,
             0, 1.0,
             Arrays.asList("examplemod"),
             Arrays.asList("examplemobid")
         ));
         defaults.put("minecraft:the_nether", new DimensionConfig(
             5, 1.5,
             5, 1.5,
             3, 1.5,
             new ArrayList<>(),
             Arrays.asList("minecraft:ender_dragon")
         ));
         defaults.put("minecraft:the_end", new DimensionConfig(
             10, 2.0,
             10, 2.0,
             5, 2.0,
             new ArrayList<>(),
             Arrays.asList("minecraft:wither")
         ));
         return defaults;
    }
}
