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
import net.minecraft.core.Registry;
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

    // Множители сложности для здоровья
    public static final DoubleValue HEALTH_PEACEFUL;
    public static final DoubleValue HEALTH_EASY;
    public static final DoubleValue HEALTH_NORMAL;
    public static final DoubleValue HEALTH_HARD;

    // Множители сложности для урона
    public static final DoubleValue DAMAGE_PEACEFUL;
    public static final DoubleValue DAMAGE_EASY;
    public static final DoubleValue DAMAGE_NORMAL;
    public static final DoubleValue DAMAGE_HARD;

    // Настройки для измерений
    public static final Map<String, DimensionConfig> DIMENSIONS = new HashMap<>();

    static {
        // Инициализация множителей сложности
        HEALTH_PEACEFUL = BUILDER
            .comment("Множитель здоровья для мирной сложности")
            .defineInRange("difficulty.health.peaceful", 0.5, 0.0, 10.0);

        HEALTH_EASY = BUILDER
            .comment("Множитель здоровья для легкой сложности")
            .defineInRange("difficulty.health.easy", 0.75, 0.0, 10.0);

        HEALTH_NORMAL = BUILDER
            .comment("Множитель здоровья для нормальной сложности")
            .defineInRange("difficulty.health.normal", 1.0, 0.0, 10.0);

        HEALTH_HARD = BUILDER
            .comment("Множитель здоровья для сложной сложности")
            .defineInRange("difficulty.health.hard", 1.5, 0.0, 10.0);

        DAMAGE_PEACEFUL = BUILDER
            .comment("Множитель урона для мирной сложности")
            .defineInRange("difficulty.damage.peaceful", 0.5, 0.0, 10.0);

        DAMAGE_EASY = BUILDER
            .comment("Множитель урона для легкой сложности")
            .defineInRange("difficulty.damage.easy", 0.75, 0.0, 10.0);

        DAMAGE_NORMAL = BUILDER
            .comment("Множитель урона для нормальной сложности")
            .defineInRange("difficulty.damage.normal", 1.0, 0.0, 10.0);

        DAMAGE_HARD = BUILDER
            .comment("Множитель урона для сложной сложности")
            .defineInRange("difficulty.damage.hard", 1.5, 0.0, 10.0);

        // Создаем спецификацию
        SPEC = BUILDER.build();
    }

    /**
     * Инициализация конфигурации:
     * – Загружаются параметры измерений из файла dimensions.json (или создаётся дефолтный, если файла нет)
     */
    public static void init() {
        // Инициализация конфигурации измерений
        DIMENSIONS.putAll(DimensionConfigManager.getDefaultDimensionConfigs());
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
         * Дефолтные настройки для стандартных измерений.
         */
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
                new ArrayList<String>(),
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
                new ArrayList<String>(),
                Arrays.asList("minecraft:wither")
            ));
            return defaults;
        }

        /**
         * Добавляет в конфигурацию измерения из реестра, которых ещё нет в файле.
         * Эти измерения получат значения по умолчанию.
         */
        private static void addRegistryDimensions() {
            ForgeRegistry<DimensionType> dimRegistry = (ForgeRegistry<DimensionType>) RegistryManager.ACTIVE.getRegistry(Registry.DIMENSION_TYPE_REGISTRY);
            if (dimRegistry != null) {
                for (ResourceLocation id : dimRegistry.getKeys()) {
                    String dimId = id.toString();
                    if (!dimensionConfigs.containsKey(dimId)) {
                        dimensionConfigs.put(dimId, new DimensionConfig(
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
                            new ArrayList<String>(),
                            new ArrayList<String>()
                        ));
                    }
                }
                saveConfigs();
            }
        }
    }
}
