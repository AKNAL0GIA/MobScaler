package com.example.mobscaler.config;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.RegistryManager;
import net.minecraftforge.registries.ForgeRegistry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.Registry;
import net.minecraft.world.level.dimension.DimensionType;

import java.io.*;
import java.lang.reflect.Type;
import java.nio.file.*;
import java.util.*;

public class MobScalerConfig {
    private static ForgeConfigSpec.Builder BUILDER;
    public static ForgeConfigSpec SPEC;

    // Глобальные настройки сложности (через ForgeConfigSpec)
    public static ForgeConfigSpec.ConfigValue<Double> HEALTH_PEACEFUL;
    public static ForgeConfigSpec.ConfigValue<Double> HEALTH_EASY;
    public static ForgeConfigSpec.ConfigValue<Double> HEALTH_NORMAL;
    public static ForgeConfigSpec.ConfigValue<Double> HEALTH_HARD;

    public static ForgeConfigSpec.ConfigValue<Double> DAMAGE_PEACEFUL;
    public static ForgeConfigSpec.ConfigValue<Double> DAMAGE_EASY;
    public static ForgeConfigSpec.ConfigValue<Double> DAMAGE_NORMAL;
    public static ForgeConfigSpec.ConfigValue<Double> DAMAGE_HARD;

    // Карта измерений, параметры которых загружаются из внешнего JSON-файла
    public static final Map<String, DimensionConfig> DIMENSIONS = new HashMap<>();

    /**
     * Инициализация конфигурации:
     * – Строятся глобальные настройки сложности
     * – Загружаются параметры измерений из файла dimensions.json (или создаётся дефолтный, если файла нет)
     */
    public static void init() {
        BUILDER = new ForgeConfigSpec.Builder();
        setupDifficultyConfig();
        SPEC = BUILDER.build();

        // Загрузить/создать внешний файл с параметрами измерений
        DimensionConfigManager.loadConfigs();
        DIMENSIONS.putAll(DimensionConfigManager.getDimensionConfigs());
    }

    private static void setupDifficultyConfig() {
        BUILDER.push("difficulty");

        HEALTH_PEACEFUL = BUILDER
            .comment("Health multiplier for Peaceful difficulty")
            .defineInRange("health_peaceful", 0.8, 0.1, 10.0);

        HEALTH_EASY = BUILDER
            .defineInRange("health_easy", 1.0, 0.1, 10.0);

        HEALTH_NORMAL = BUILDER
            .defineInRange("health_normal", 1.2, 0.1, 10.0);

        HEALTH_HARD = BUILDER
            .defineInRange("health_hard", 1.5, 0.1, 10.0);

        DAMAGE_PEACEFUL = BUILDER
            .comment("Damage multiplier for Peaceful difficulty")
            .defineInRange("damage_peaceful", 0.7, 0.1, 10.0);

        DAMAGE_EASY = BUILDER
            .defineInRange("damage_easy", 1.0, 0.1, 10.0);

        DAMAGE_NORMAL = BUILDER
            .defineInRange("damage_normal", 1.2, 0.1, 10.0);

        DAMAGE_HARD = BUILDER
            .defineInRange("damage_hard", 1.5, 0.1, 10.0);

        BUILDER.pop();
    }

    /**
     * Регистрирует конфигурацию через Forge.
     */
    public static void register(IEventBus eventBus) {
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, SPEC);
    }

    /**
     * Внутренний класс для управления параметрами измерений через внешний JSON-файл.
     * Файл располагается по пути: config/mobscaler/dimensions.json
     */
    public static class DimensionConfigManager {
        private static final Gson GSON = new Gson();
        private static final Path CONFIG_PATH = Paths.get("config", "mobscaler", "dimensions.json");
        private static Map<String, DimensionConfig> dimensionConfigs = new HashMap<>();

        /**
         * Загружает конфигурацию измерений.
         * Если файла не существует, создаёт его с дефолтными настройками.
         * После загрузки дополнительно добавляет из реестра измерения, которых ещё нет в файле.
         */
        public static void loadConfigs() {
            if (!Files.exists(CONFIG_PATH)) {
                // Файл отсутствует – создать дефолтные настройки
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
            // Добавляем измерения из реестра, если их нет в файле
            addRegistryDimensions();
        }

        public static Map<String, DimensionConfig> getDimensionConfigs() {
            return dimensionConfigs;
        }

        /**
         * Сохраняет конфигурацию измерений в файл.
         */
        public static void saveConfigs() {
            try {
                Files.createDirectories(CONFIG_PATH.getParent());
                try (Writer writer = new FileWriter(CONFIG_PATH.toFile())) {
                    GSON.toJson(dimensionConfigs, writer);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        /**
         * Дефолтные настройки для стандартных измерений.
         */
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

        /**
         * Добавляет в конфигурацию измерения из реестра, которых ещё нет в файле.
         * Эти измерения получат значения по умолчанию.
         */
        private static void addRegistryDimensions() {
            @SuppressWarnings("unchecked")
            ForgeRegistry<DimensionType> dimRegistry = (ForgeRegistry<DimensionType>) RegistryManager.ACTIVE.getRegistry(Registry.DIMENSION_TYPE_REGISTRY);
            if (dimRegistry != null) {
                for (ResourceLocation id : dimRegistry.getKeys()) {
                    String dimId = id.toString();
                    if (!dimensionConfigs.containsKey(dimId)) {
                        dimensionConfigs.put(dimId, new DimensionConfig(
                            0, 1.0,
                            0, 1.0,
                            0, 1.0,
                            new ArrayList<>(),
                            new ArrayList<>()
                        ));
                    }
                }
                // Если добавились новые измерения, сохранить изменения
                saveConfigs();
            }
        }
    }
}
