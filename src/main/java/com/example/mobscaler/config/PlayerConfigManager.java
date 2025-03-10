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

public class PlayerConfigManager {
    private static final Gson GSON = new GsonBuilder()
        .setPrettyPrinting()
        .disableHtmlEscaping()
        .serializeNulls()
        .create();
    private static final Path CONFIG_PATH = Paths.get("config", "mobscaler", "players.json");
    private static PlayerConfig playerConfig;

    public static void loadConfigs() {
        if (!Files.exists(CONFIG_PATH)) {
            // Файл не существует – создаём дефолтные настройки и сохраняем
            playerConfig = getDefaultPlayerConfig();
            saveConfigs();
        } else {
            try (Reader reader = new FileReader(CONFIG_PATH.toFile())) {
                Type type = new TypeToken<PlayerConfig>(){}.getType();
                PlayerConfig loadedConfig = GSON.fromJson(reader, type);
                if (loadedConfig != null) {
                    playerConfig = loadedConfig;
                } else {
                    // Если конфиг поврежден или пуст, создаем новый
                    playerConfig = getDefaultPlayerConfig();
                    saveConfigs();
                }
            } catch (IOException e) {
                e.printStackTrace();
                // В случае ошибки создаем новый конфиг
                playerConfig = getDefaultPlayerConfig();
                saveConfigs();
            }
        }
    }

    public static PlayerConfig getPlayerConfig() {
        if (playerConfig == null) {
            playerConfig = getDefaultPlayerConfig();
        }
        return playerConfig;
    }

    public static void saveConfigs() {
        try {
            Files.createDirectories(CONFIG_PATH.getParent());
            try (Writer writer = new FileWriter(CONFIG_PATH.toFile())) {
                GSON.toJson(playerConfig, writer);
            }
        } catch(IOException e) {
            e.printStackTrace();
        }
    }

    private static PlayerConfig getDefaultPlayerConfig() {
        Map<String, List<String>> defaultBlacklist = new HashMap<>();
        defaultBlacklist.put("minecraft:overworld", Arrays.asList("ExamplePlayer"));
        defaultBlacklist.put("minecraft:the_nether", new ArrayList<>());
        defaultBlacklist.put("minecraft:the_end", new ArrayList<>());

        Map<String, PlayerConfig.PlayerModifiers> defaultModifiers = new HashMap<>();
        defaultModifiers.put("minecraft:overworld", new PlayerConfig.PlayerModifiers());
        defaultModifiers.put("minecraft:the_nether", new PlayerConfig.PlayerModifiers());
        defaultModifiers.put("minecraft:the_end", new PlayerConfig.PlayerModifiers());

        return new PlayerConfig(defaultBlacklist, defaultModifiers);
    }
} 