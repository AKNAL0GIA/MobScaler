package com.example.mobscaler.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import net.neoforged.neoforge.common.ModConfigSpec;

import java.io.*;
import java.lang.reflect.Type;
import java.nio.file.*;
import java.util.*;

public class MobScalerConfig {
    public static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();
    public static final ModConfigSpec SPEC;

    // Difficulty multipliers for health
    public static final ModConfigSpec.DoubleValue HEALTH_PEACEFUL;
    public static final ModConfigSpec.DoubleValue HEALTH_EASY;
    public static final ModConfigSpec.DoubleValue HEALTH_NORMAL;
    public static final ModConfigSpec.DoubleValue HEALTH_HARD;

    // Difficulty multipliers for damage
    public static final ModConfigSpec.DoubleValue DAMAGE_PEACEFUL;
    public static final ModConfigSpec.DoubleValue DAMAGE_EASY;
    public static final ModConfigSpec.DoubleValue DAMAGE_NORMAL;
    public static final ModConfigSpec.DoubleValue DAMAGE_HARD;

    // GUI settings
    public static final ModConfigSpec.IntValue GUI_SCALE_ON_OPEN;

    // Debug settings
    public static final ModConfigSpec.BooleanValue DEBUG_LOGGING_ENABLED;

    // Settings for dimensions
    public static final Map<String, DimensionConfig> DIMENSIONS = new HashMap<>();

    static {
        // Initialize difficulty multipliers
        HEALTH_PEACEFUL = BUILDER
                .comment("Health multiplier for peaceful difficulty")
                .defineInRange("difficulty.health.peaceful", 0.7, 0.0, 100.0);

        HEALTH_EASY = BUILDER
                .comment("Health multiplier for easy difficulty")
                .defineInRange("difficulty.health.easy", 1, 0.0, 100.0);

        HEALTH_NORMAL = BUILDER
                .comment("Health multiplier for normal difficulty")
                .defineInRange("difficulty.health.normal", 1.2, 0.0, 100.0);

        HEALTH_HARD = BUILDER
                .comment("Health multiplier for hard difficulty")
                .defineInRange("difficulty.health.hard", 1.5, 0.0, 100.0);

        DAMAGE_PEACEFUL = BUILDER
                .comment("Damage multiplier for peaceful difficulty")
                .defineInRange("difficulty.damage.peaceful", 0.5, 0.0, 100.0);

        DAMAGE_EASY = BUILDER
                .comment("Damage multiplier for easy difficulty")
                .defineInRange("difficulty.damage.easy", 1.2, 0.0, 100.0);

        DAMAGE_NORMAL = BUILDER
                .comment("Damage multiplier for normal difficulty")
                .defineInRange("difficulty.damage.normal", 1.4, 0.0, 100.0);

        DAMAGE_HARD = BUILDER
                .comment("Damage multiplier for hard difficulty")
                .defineInRange("difficulty.damage.hard", 1.7, 0.0, 100.0);

        // GUI settings
        GUI_SCALE_ON_OPEN = BUILDER
                .comment("GUI scale to use when opening the MobScaler configuration screen (1-4, 0 = auto)")
                .defineInRange("gui.scale_on_open", 3, 0, 4);

        // Debug settings
        DEBUG_LOGGING_ENABLED = BUILDER
                .comment("Enable detailed debug logging for entity attribute modifications (can cause lag)")
                .define("debug.logging_enabled", false);

        // Create specification
        SPEC = BUILDER.build();
    }

    /**
     * Configuration initialization:
     * - Loads dimension parameters from dimensions.json file (or creates default if
     * file doesn't exist)
     */
    public static void init() {
        // Load dimension configuration
        DimensionConfigManager.loadConfigs();
        DIMENSIONS.putAll(DimensionConfigManager.getDimensionConfigs());

        // Load mod and individual mob configurations
        IndividualMobConfigManager.loadConfigs();
    }

    /**
     * Internal class for managing dimension parameters through external JSON file.
     * File location: config/mobscaler/dimensions.json
     */
    public static class DimensionConfigManager {
        private static final Gson GSON = new Gson();
        private static final Path CONFIG_PATH = Paths.get("config", "mobscaler", "dimensions.json");
        private static Map<String, DimensionConfig> dimensionConfigs = new HashMap<>();

        public static void loadConfigs() {
            if (!Files.exists(CONFIG_PATH)) {
                dimensionConfigs = getDefaultDimensionConfigs();
                saveConfigs();
            } else {
                try (Reader reader = new FileReader(CONFIG_PATH.toFile())) {
                    Type type = new TypeToken<Map<String, DimensionConfig>>() {
                    }.getType();
                    dimensionConfigs = GSON.fromJson(reader, type);
                } catch (IOException e) {
                    e.printStackTrace();
                    dimensionConfigs = getDefaultDimensionConfigs();
                }
            }
        }

        public static Map<String, DimensionConfig> getDimensionConfigs() {
            return dimensionConfigs;
        }

        /**
         * Saves dimension configuration to file.
         */
        public static void saveConfigs() {
            try {
                Files.createDirectories(CONFIG_PATH.getParent());
                try (Writer writer = new FileWriter(CONFIG_PATH.toFile())) {
                    Gson gson = new GsonBuilder()
                            .setPrettyPrinting()
                            .disableHtmlEscaping()
                            .create();
                    gson.toJson(dimensionConfigs, writer);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        /**
         * Default settings for standard dimensions.
         */
        private static Map<String, DimensionConfig> getDefaultDimensionConfigs() {
            Map<String, DimensionConfig> defaults = new HashMap<>();
            defaults.put("minecraft:overworld", new DimensionConfig(
                    false, // enableNightScaling
                    false, // enableCaveScaling
                    -5.0, // caveHeight
                    false, // enableGravity
                    1.0, // gravityMultiplier
                    // Day settings
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
                    0.0, 1.0, // block reach distance
                    0.0, 1.0, // entity reach distance
                    0.0, 1.0, // burning time
                    0.0, 1.0, // explosion knockback resistance
                    1.0, // fall damage multiplier
                    0.0, 1.0, // oxygen bonus
                    0.0, 1.0, // safe fall distance
                    0.0, 1.0, // waterMovementEfficiency
                    
                    // Night settings
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
                    0.0, 1.0, // night block reach distance
                    0.0, 1.0, // night entity reach distance
                    0.0, 1.0, // night burning time
                    0.0, 1.0, // night explosion knockback resistance
                    1.0, // night fall damage multiplier
                    0.0, 1.0, // night oxygen bonus
                    0.0, 1.0, // night safe fall distance
                    0.0, 1.0, // night waterMovementEfficiency
                    // Cave settings
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
                    0.0, 1.0, // cave block reach distance
                    0.0, 1.0, // cave entity reach distance
                    0.0, 1.0, // cave burning time
                    0.0, 1.0, // cave explosion knockback resistance
                    1.0, // cave fall damage multiplier
                    0.0, 1.0, // cave oxygen bonus
                    0.0, 1.0, // cave safe fall distance
                    0.0, 1.0, // cave waterMovementEfficiency
                    // Общие черные списки
                    new ArrayList<String>(), // modBlacklist
                    new ArrayList<String>() // entityBlacklist
            ));
            defaults.put("minecraft:the_nether", new DimensionConfig(
                    false, // enableNightScaling
                    false, // enableCaveScaling
                    30.0, // caveHeight
                    false, // enableGravity
                    1.0, // gravityMultiplier
                    // Day settings
                    4.0, 1.3, // health
                    4.0, 1.3, // armor
                    4.0, 1.3, // damage
                    0.0, 1.0, // speed
                    0.0, 1.0, // knockback resistance
                    0.0, 1.0, // attack knockback
                    0.0, 1.0, // attack speed
                    0.0, 1.0, // follow range
                    0.0, 1.0, // flying speed
                    0.0, 1.0, // armor toughness
                    0.0, 1.0, // luck
                    0.0, 1.0, // swim speed
                    0.0, 1.0, // block reach distance
                    0.0, 1.0, // entity reach distance
                    0.0, 1.0, // burning time
                    0.0, 1.0, // explosion knockback resistance
                    1.0, // fall damage multiplier
                    0.0, 1.0, // oxygen bonus
                    0.0, 1.0, // safe fall distance
                    0.0, 1.0, // waterMovementEfficiency

                    // Night settings
                    0.0, 1.3, // night health
                    0.0, 1.3, // night armor
                    0.0, 1.3, // night damage
                    0.0, 1.0, // night speed
                    0.0, 1.0, // night knockback resistance
                    0.0, 1.0, // night attack knockback
                    0.0, 1.0, // night attack speed
                    0.0, 1.0, // night follow range
                    0.0, 1.0, // night flying speed
                    0.0, 1.0, // night armor toughness
                    0.0, 1.0, // night luck
                    0.0, 1.0, // night swim speed
                    0.0, 1.0, // night block reach distance
                    0.0, 1.0, // night entity reach distance
                    0.0, 1.0, // night burning time
                    0.0, 1.0, // night explosion knockback resistance
                    1.0, // night fall damage multiplier
                    0.0, 1.0, // night oxygen bonus
                    0.0, 1.0, // night safe fall distance
                    0.0, 1.0, // night waterMovementEfficiency
                    // Cave settings
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
                    0.0, 1.0, // cave block reach distance
                    0.0, 1.0, // cave entity reach distance
                    0.0, 1.0, // cave burning time
                    0.0, 1.0, // cave explosion knockback resistance
                    1.0, // cave fall damage multiplier
                    0.0, 1.0, // cave oxygen bonus
                    0.0, 1.0, // cave safe fall distance
                    0.0, 1.0, // cave waterMovementEfficiency
                    // Общие черные списки
                    new ArrayList<String>(), // modBlacklist
                    Arrays.asList("minecraft:ender_dragon") // entityBlacklist
            ));
            defaults.put("minecraft:the_end", new DimensionConfig(
                    false, // enableNightScaling
                    false, // enableCaveScaling
                    0.0, // caveHeight
                    false, // enableGravity
                    1.0, // gravityMultiplier
                    // Day settings
                    10.0, 2.0, // health
                    10.0, 2.0, // armor
                    5.0, 2.0, // damage
                    0.0, 1.0, // speed
                    0.0, 1.0, // knockback resistance
                    0.0, 1.0, // attack knockback
                    0.0, 1.0, // attack speed
                    0.0, 1.0, // follow range
                    0.0, 1.0, // flying speed
                    0.0, 1.0, // armor toughness
                    0.0, 1.0, // luck
                    0.0, 1.0, // swim speed
                    0.0, 1.0, // block reach distance
                    0.0, 1.0, // entity reach distance
                    0.0, 1.0, // burning time
                    0.0, 1.0, // explosion knockback resistance
                    1.0, // fall damage multiplier
                    0.0, 1.0, // oxygen bonus
                    0.0, 1.0, // safe fall distance
                    0.0, 1.0, // waterMovementEfficiency
                    // Night settings
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
                    0.0, 1.0, // night block reach distance
                    0.0, 1.0, // night entity reach distance
                    0.0, 1.0, // night burning time
                    0.0, 1.0, // night explosion knockback resistance
                    1.0, // night fall damage multiplier
                    0.0, 1.0, // night oxygen bonus
                    0.0, 1.0, // night safe fall distance
                    0.0, 1.0, // night waterMovementEfficiency
                    
                    // Cave settings
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
                    0.0, 1.0, // cave block reach distance
                    0.0, 1.0, // cave entity reach distance
                    0.0, 1.0, // cave burning time
                    0.0, 1.0, // cave explosion knockback resistance
                    1.0, // cave fall damage multiplier
                    0.0, 1.0, // cave oxygen bonus
                    0.0, 1.0, // cave safe fall distance
                    0.0, 1.0, // cave waterMovementEfficiency
                    new ArrayList<String>(), // modBlacklist
                    Arrays.asList("minecraft:wither") // entityBlacklist
            ));
            return defaults;
        }
    }

    public static double getDifficultyValue(String path) {
        if (path == null)
            return 1.0;

        return switch (path) {
            case "difficulty.health.peaceful" -> HEALTH_PEACEFUL.get();
            case "difficulty.health.easy" -> HEALTH_EASY.get();
            case "difficulty.health.normal" -> HEALTH_NORMAL.get();
            case "difficulty.health.hard" -> HEALTH_HARD.get();
            case "difficulty.damage.peaceful" -> DAMAGE_PEACEFUL.get();
            case "difficulty.damage.easy" -> DAMAGE_EASY.get();
            case "difficulty.damage.normal" -> DAMAGE_NORMAL.get();
            case "difficulty.damage.hard" -> DAMAGE_HARD.get();
            default -> 1.0;
        };
    }

    public static void setDifficultyValue(String path, double value) {
        if (path == null)
            return;

        switch (path) {
            case "difficulty.health.peaceful" -> HEALTH_PEACEFUL.set(value);
            case "difficulty.health.easy" -> HEALTH_EASY.set(value);
            case "difficulty.health.normal" -> HEALTH_NORMAL.set(value);
            case "difficulty.health.hard" -> HEALTH_HARD.set(value);
            case "difficulty.damage.peaceful" -> DAMAGE_PEACEFUL.set(value);
            case "difficulty.damage.easy" -> DAMAGE_EASY.set(value);
            case "difficulty.damage.normal" -> DAMAGE_NORMAL.set(value);
            case "difficulty.damage.hard" -> DAMAGE_HARD.set(value);
        }
    }

    public static void save() {
        // Конфигурация Forge сохраняется автоматически при изменении значений
        // Но мы можем добавить дополнительную логику сохранения здесь, если потребуется
    }

    /**
     * Возвращает GUI scale для открытия экрана MobScaler.
     * 0 = автоматический (не изменять текущий scale).
     */
    public static int getGuiScaleOnOpen() {
        return GUI_SCALE_ON_OPEN.get();
    }

    /**
     * Проверяет, включено ли подробное дебаг-логирование.
     */
    public static boolean isDebugLoggingEnabled() {
        return DEBUG_LOGGING_ENABLED.get();
    }

    /**
     * Включает или отключает подробное дебаг-логирование.
     */
    public static void setDebugLoggingEnabled(boolean enabled) {
        DEBUG_LOGGING_ENABLED.set(enabled);
    }
}
