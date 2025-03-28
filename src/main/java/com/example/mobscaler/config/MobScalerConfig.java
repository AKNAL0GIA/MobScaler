package com.example.mobscaler.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.RegistryManager;
import net.minecraftforge.registries.ForgeRegistry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraftforge.common.ForgeConfigSpec.DoubleValue;
import net.minecraftforge.fml.common.Mod;
import java.util.HashMap;
import java.util.Map;

import java.io.*;
import java.lang.reflect.Type;
import java.nio.file.*;
import java.util.*;

@Mod.EventBusSubscriber
public class MobScalerConfig {
    public static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();
    public static final ForgeConfigSpec SPEC;

    // Difficulty multipliers for health
    public static final DoubleValue HEALTH_PEACEFUL;
    public static final DoubleValue HEALTH_EASY;
    public static final DoubleValue HEALTH_NORMAL;
    public static final DoubleValue HEALTH_HARD;

    // Difficulty multipliers for damage
    public static final DoubleValue DAMAGE_PEACEFUL;
    public static final DoubleValue DAMAGE_EASY;
    public static final DoubleValue DAMAGE_NORMAL;
    public static final DoubleValue DAMAGE_HARD;

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

        // Create specification
        SPEC = BUILDER.build();
    }

    /**
     * Configuration initialization:
     * - Loads dimension parameters from dimensions.json file (or creates default if file doesn't exist)
     */
    public static void init() {
        // Load dimension configuration
        DimensionConfigManager.loadConfigs();
        DIMENSIONS.putAll(DimensionConfigManager.getDimensionConfigs());
        
        // Load mod and individual mob configurations
        IndividualMobConfigManager.loadConfigs();
    }

    /**
     * Registers configuration through Forge.
     */
    public static void register(IEventBus eventBus) {
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, SPEC);
    }

    /**
     * Internal class for managing dimension parameters through external JSON file.
     * File location: config/mobscaler/dimensions.json
     */
    public static class DimensionConfigManager {
        private static final Gson GSON = new Gson();
        private static final Path CONFIG_PATH = Paths.get("config", "mobscaler", "dimensions.json");
        private static Map<String, DimensionConfig> dimensionConfigs = new HashMap<>();

        /**
         * Loads dimension configuration.
         * If file doesn't exist, creates it with default settings.
         * After loading, additionally adds dimensions from registry that aren't in the file yet.
         */
        public static void loadConfigs() {
            if (!Files.exists(CONFIG_PATH)) {
                // File doesn't exist – create default settings
                dimensionConfigs = getDefaultDimensionConfigs();
                saveConfigs();
            } else {
                try (Reader reader = new FileReader(CONFIG_PATH.toFile())) {
                    Type type = new TypeToken<Map<String, DimensionConfig>>(){}.getType();
                    dimensionConfigs = GSON.fromJson(reader, type);
                } catch (IOException e) {
                    e.printStackTrace();
                    dimensionConfigs = getDefaultDimensionConfigs();
                }
            }
            // Add dimensions from registry if they aren't in the file yet
            addRegistryDimensions();
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
                // Night settings
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
                // Cave settings
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
            defaults.put("minecraft:the_nether", new DimensionConfig(
                false, // enableNightScaling
                false,  // enableCaveScaling
                30.0,  // caveHeight
                false, // enableGravity
                1.0, // gravityMultiplier
                // Day settings
                5.0, 1.3,  // health
                5.0, 1.3,  // armor
                5.0, 1.3,  // damage
                0.0, 1.3,  // speed
                0.0, 1.3,  // knockback resistance
                0.0, 1.3,  // attack knockback
                0.0, 1.3,  // attack speed
                0.0, 1.3,  // follow range
                0.0, 1.3,  // flying speed
                0.0, 1.3,  // armor toughness
                0.0, 1.3,  // luck
                0.0, 1.3,  // swim speed
                0.0, 1.3,  // reach distance
                // Night settings
                0.0, 1.3,  // night health
                0.0, 1.3,  // night armor
                0.0, 1.3,  // night damage
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
                // Cave settings
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
                Arrays.asList("minecraft:ender_dragon")  // entityBlacklist
            ));
            defaults.put("minecraft:the_end", new DimensionConfig(
                false, // enableNightScaling
                false, // enableCaveScaling
                0.0,   // caveHeight
                false, // enableGravity
                1.0, // gravityMultiplier
                // Day settings
                10.0, 2.0,  // health
                10.0, 2.0,  // armor
                5.0, 2.0,   // damage
                0.0, 1.0,   // speed
                0.0, 1.0,   // knockback resistance
                0.0, 1.0,   // attack knockback
                0.0, 1.0,   // attack speed
                0.0, 1.0,   // follow range
                0.0, 1.0,   // flying speed
                0.0, 1.0,   // armor toughness
                0.0, 1.0,   // luck
                0.0, 1.0,   // swim speed
                0.0, 1.0,   // reach distance
                // Night settings
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
                // Cave settings
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
                Arrays.asList("minecraft:wither")  // entityBlacklist
            ));
            return defaults;
        }

        /**
         * Adds dimensions from registry to configuration that aren't in the file yet.
         * These dimensions will get default values.
         */
        private static void addRegistryDimensions() {
            ForgeRegistry<DimensionType> dimRegistry = (ForgeRegistry<DimensionType>) RegistryManager.ACTIVE.getRegistry(net.minecraft.core.registries.Registries.DIMENSION_TYPE);
            if (dimRegistry != null) {
                for (ResourceLocation id : dimRegistry.getKeys()) {
                    String dimId = id.toString();
                    if (!dimensionConfigs.containsKey(dimId)) {
                        dimensionConfigs.put(dimId, new DimensionConfig(
                            false, // enableNightScaling
                            false, // enableCaveScaling
                            -5.0,   // caveHeight
                            false, // enableGravity
                            1.0, // gravityMultiplier
                            // Day settings
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
                            // Night settings
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
                            // Cave settings
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

    public static double getDifficultyValue(String path) {
        if (path == null) return 1.0;
        
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
        if (path == null) return;
        
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
}
