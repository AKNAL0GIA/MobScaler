package com.example.mobscaler.events;

import com.example.mobscaler.config.DimensionConfig;
import com.example.mobscaler.config.MobScalerConfig;
import com.example.mobscaler.config.PlayerConfigManager;
import com.example.mobscaler.config.PlayerConfig.PlayerModifiers;
import com.example.mobscaler.config.IndividualMobConfig;
import com.example.mobscaler.config.IndividualMobAttributes;
import com.example.mobscaler.config.IndividualMobManager;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.common.NeoForgeMod;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.living.LivingFallEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import net.neoforged.neoforge.event.level.LevelEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.server.ServerLifecycleHooks;
import net.minecraft.core.Holder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class EntityHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(EntityHandler.class);
    private static final String MOD_ID = "mobscaler";

    // ============================================================
    // ResourceLocation для модификаторов EntityHandler (стандартные)
    // В 1.21.1 UUID заменены на ResourceLocation
    // ============================================================
    private static final ResourceLocation HEALTH_MODIFIER = ResourceLocation.fromNamespaceAndPath(MOD_ID, "health");
    private static final ResourceLocation ARMOR_MODIFIER = ResourceLocation.fromNamespaceAndPath(MOD_ID, "armor");
    private static final ResourceLocation DAMAGE_MODIFIER = ResourceLocation.fromNamespaceAndPath(MOD_ID, "damage");
    private static final ResourceLocation SPEED_MODIFIER = ResourceLocation.fromNamespaceAndPath(MOD_ID, "speed");
    private static final ResourceLocation KNOCKBACK_RESISTANCE_MOD = ResourceLocation.fromNamespaceAndPath(MOD_ID, "knockback_resistance");
    private static final ResourceLocation ATTACK_KNOCKBACK_MOD = ResourceLocation.fromNamespaceAndPath(MOD_ID, "attack_knockback");
    private static final ResourceLocation ATTACK_SPEED_MOD = ResourceLocation.fromNamespaceAndPath(MOD_ID, "attack_speed");
    private static final ResourceLocation FOLLOW_RANGE_MOD = ResourceLocation.fromNamespaceAndPath(MOD_ID, "follow_range");
    private static final ResourceLocation FLYING_SPEED_MOD = ResourceLocation.fromNamespaceAndPath(MOD_ID, "flying_speed");
    private static final ResourceLocation ARMOR_TOUGHNESS_MOD = ResourceLocation.fromNamespaceAndPath(MOD_ID, "armor_toughness");
    private static final ResourceLocation LUCK_MODIFIER = ResourceLocation.fromNamespaceAndPath(MOD_ID, "luck");
    private static final ResourceLocation GRAVITY_MODIFIER = ResourceLocation.fromNamespaceAndPath(MOD_ID, "gravity");
    private static final ResourceLocation SWIM_SPEED_MODIFIER = ResourceLocation.fromNamespaceAndPath(MOD_ID, "swim_speed");
    private static final ResourceLocation BLOCK_INTERACTION_RANGE_MOD = ResourceLocation.fromNamespaceAndPath(MOD_ID, "block_reach");
    private static final ResourceLocation ENTITY_INTERACTION_RANGE_MOD = ResourceLocation.fromNamespaceAndPath(MOD_ID, "entity_reach");

    // Дополнительные атрибуты (ванильные, добавлены в 1.21+)
    private static final ResourceLocation BURNING_TIME_MOD = ResourceLocation.fromNamespaceAndPath(MOD_ID, "burning_time");
    private static final ResourceLocation EXPLOSION_KNOCKBACK_RESISTANCE_MOD = ResourceLocation.fromNamespaceAndPath(MOD_ID, "explosion_knockback_resistance");
    private static final ResourceLocation FALL_DAMAGE_MULTIPLIER_MOD = ResourceLocation.fromNamespaceAndPath(MOD_ID, "fall_damage_multiplier");
    private static final ResourceLocation JUMP_STRENGTH_MOD = ResourceLocation.fromNamespaceAndPath(MOD_ID, "jump_strength");
    private static final ResourceLocation MINING_EFFICIENCY_MOD = ResourceLocation.fromNamespaceAndPath(MOD_ID, "mining_efficiency");
    private static final ResourceLocation MOVEMENT_EFFICIENCY_MOD = ResourceLocation.fromNamespaceAndPath(MOD_ID, "movement_efficiency");
    private static final ResourceLocation OXYGEN_BONUS_MOD = ResourceLocation.fromNamespaceAndPath(MOD_ID, "oxygen_bonus");
    private static final ResourceLocation SAFE_FALL_DISTANCE_MOD = ResourceLocation.fromNamespaceAndPath(MOD_ID, "safe_fall_distance");
    private static final ResourceLocation BLOCK_BREAK_SPEED_MOD = ResourceLocation.fromNamespaceAndPath(MOD_ID, "block_break_speed");
    private static final ResourceLocation SNEAKING_SPEED_MOD = ResourceLocation.fromNamespaceAndPath(MOD_ID, "sneaking_speed");
    private static final ResourceLocation STEP_HEIGHT_MOD = ResourceLocation.fromNamespaceAndPath(MOD_ID, "step_height");
    private static final ResourceLocation SUBMERGED_MINING_SPEED_MOD = ResourceLocation.fromNamespaceAndPath(MOD_ID, "submerged_mining_speed");
    private static final ResourceLocation WATER_MOVEMENT_EFFICIENCY_MOD = ResourceLocation.fromNamespaceAndPath(MOD_ID, "water_movement_efficiency");

    // Фиксированный модификатор для временной компенсации урона
    private static final ResourceLocation DAMAGE_RESET_MOD = ResourceLocation.fromNamespaceAndPath(MOD_ID, "reset_damage");

    // Пороговое значение гравитации, ниже которого урон от падения отключается
    private static final double NO_FALL_DAMAGE_GRAVITY_THRESHOLD = 0.6;

    // Храним последнее состояние ночи для каждого измерения (только для игроков)
    private static final Map<String, Boolean> lastNightState = new HashMap<>();

    // ============================================================
    // Все стандартные атрибуты (для removeAllModifiers)
    // ============================================================
    private static final List<Holder<Attribute>> ALL_ATTRIBUTES = new ArrayList<>();
    static {
        ALL_ATTRIBUTES.add(Attributes.MAX_HEALTH);
        ALL_ATTRIBUTES.add(Attributes.ARMOR);
        ALL_ATTRIBUTES.add(Attributes.ATTACK_DAMAGE);
        ALL_ATTRIBUTES.add(Attributes.MOVEMENT_SPEED);
        ALL_ATTRIBUTES.add(Attributes.KNOCKBACK_RESISTANCE);
        ALL_ATTRIBUTES.add(Attributes.ATTACK_KNOCKBACK);
        ALL_ATTRIBUTES.add(Attributes.ATTACK_SPEED);
        ALL_ATTRIBUTES.add(Attributes.FOLLOW_RANGE);
        ALL_ATTRIBUTES.add(Attributes.FLYING_SPEED);
        ALL_ATTRIBUTES.add(Attributes.ARMOR_TOUGHNESS);
        ALL_ATTRIBUTES.add(Attributes.LUCK);
        // Ванильные атрибуты (1.21+)
        ALL_ATTRIBUTES.add(Attributes.GRAVITY);
        ALL_ATTRIBUTES.add(Attributes.BLOCK_INTERACTION_RANGE);
        ALL_ATTRIBUTES.add(Attributes.ENTITY_INTERACTION_RANGE);
        ALL_ATTRIBUTES.add(Attributes.BURNING_TIME);
        ALL_ATTRIBUTES.add(Attributes.EXPLOSION_KNOCKBACK_RESISTANCE);
        ALL_ATTRIBUTES.add(Attributes.FALL_DAMAGE_MULTIPLIER);
        ALL_ATTRIBUTES.add(Attributes.JUMP_STRENGTH);
        ALL_ATTRIBUTES.add(Attributes.MINING_EFFICIENCY);
        ALL_ATTRIBUTES.add(Attributes.MOVEMENT_EFFICIENCY);
        ALL_ATTRIBUTES.add(Attributes.OXYGEN_BONUS);
        ALL_ATTRIBUTES.add(Attributes.SAFE_FALL_DISTANCE);
        ALL_ATTRIBUTES.add(Attributes.BLOCK_BREAK_SPEED);
        ALL_ATTRIBUTES.add(Attributes.SNEAKING_SPEED);
        ALL_ATTRIBUTES.add(Attributes.STEP_HEIGHT);
        ALL_ATTRIBUTES.add(Attributes.SUBMERGED_MINING_SPEED);
        ALL_ATTRIBUTES.add(Attributes.WATER_MOVEMENT_EFFICIENCY);
        // NeoForge атрибуты
        ALL_ATTRIBUTES.add(NeoForgeMod.SWIM_SPEED);
    }

    /**
     * Возвращает полный список атрибутов включая NeoForge-атрибуты.
     */
    private static List<Holder<Attribute>> getAllAttributes() {
        return ALL_ATTRIBUTES;
    }

    // ============================================================
    // ResourceLocation для модификаторов IndividualMobManager
    // ============================================================
    private static final ResourceLocation INDIVIDUAL_HEALTH = ResourceLocation.fromNamespaceAndPath(MOD_ID, "individual_health");
    private static final ResourceLocation INDIVIDUAL_ARMOR = ResourceLocation.fromNamespaceAndPath(MOD_ID, "individual_armor");
    private static final ResourceLocation INDIVIDUAL_DAMAGE = ResourceLocation.fromNamespaceAndPath(MOD_ID, "individual_damage");
    private static final ResourceLocation INDIVIDUAL_SPEED = ResourceLocation.fromNamespaceAndPath(MOD_ID, "individual_speed");
    private static final ResourceLocation INDIVIDUAL_KNOCKBACK_RES = ResourceLocation.fromNamespaceAndPath(MOD_ID, "individual_knockback_resistance");
    private static final ResourceLocation INDIVIDUAL_ATTACK_KNOCKBACK = ResourceLocation.fromNamespaceAndPath(MOD_ID, "individual_attack_knockback");
    private static final ResourceLocation INDIVIDUAL_ATTACK_SPEED = ResourceLocation.fromNamespaceAndPath(MOD_ID, "individual_attack_speed");
    private static final ResourceLocation INDIVIDUAL_FOLLOW_RANGE = ResourceLocation.fromNamespaceAndPath(MOD_ID, "individual_follow_range");
    private static final ResourceLocation INDIVIDUAL_FLYING_SPEED = ResourceLocation.fromNamespaceAndPath(MOD_ID, "individual_flying_speed");
    private static final ResourceLocation INDIVIDUAL_SWIM_SPEED = ResourceLocation.fromNamespaceAndPath(MOD_ID, "individual_swim_speed");
    private static final ResourceLocation INDIVIDUAL_BLOCK_INTERACTION_RANGE = ResourceLocation.fromNamespaceAndPath(MOD_ID, "individual_block_reach");
    private static final ResourceLocation INDIVIDUAL_ENTITY_INTERACTION_RANGE = ResourceLocation.fromNamespaceAndPath(MOD_ID, "individual_entity_reach");
    // Дополнительные individual
    private static final ResourceLocation INDIVIDUAL_BURNING_TIME = ResourceLocation.fromNamespaceAndPath(MOD_ID, "individual_burning_time");
    private static final ResourceLocation INDIVIDUAL_EXPLOSION_KNOCKBACK_RESISTANCE = ResourceLocation.fromNamespaceAndPath(MOD_ID, "individual_explosion_knockback_resistance");
    private static final ResourceLocation INDIVIDUAL_FALL_DAMAGE_MULTIPLIER = ResourceLocation.fromNamespaceAndPath(MOD_ID, "individual_fall_damage_multiplier");
    private static final ResourceLocation INDIVIDUAL_JUMP_STRENGTH = ResourceLocation.fromNamespaceAndPath(MOD_ID, "individual_jump_strength");
    private static final ResourceLocation INDIVIDUAL_OXYGEN_BONUS = ResourceLocation.fromNamespaceAndPath(MOD_ID, "individual_oxygen_bonus");
    private static final ResourceLocation INDIVIDUAL_SAFE_FALL_DISTANCE = ResourceLocation.fromNamespaceAndPath(MOD_ID, "individual_safe_fall_distance");
    private static final ResourceLocation INDIVIDUAL_SNEAKING_SPEED = ResourceLocation.fromNamespaceAndPath(MOD_ID, "individual_sneaking_speed");
    private static final ResourceLocation INDIVIDUAL_WATER_MOVEMENT_EFFICIENCY = ResourceLocation.fromNamespaceAndPath(MOD_ID, "individual_water_movement_efficiency");

    // Все модификаторы мода для удаления
    private static final ResourceLocation[] ALL_MODIFIER_IDS = {
        HEALTH_MODIFIER, ARMOR_MODIFIER, DAMAGE_MODIFIER, SPEED_MODIFIER,
        KNOCKBACK_RESISTANCE_MOD, ATTACK_KNOCKBACK_MOD, ATTACK_SPEED_MOD,
        FOLLOW_RANGE_MOD, FLYING_SPEED_MOD, ARMOR_TOUGHNESS_MOD, LUCK_MODIFIER,
        GRAVITY_MODIFIER, SWIM_SPEED_MODIFIER, BLOCK_INTERACTION_RANGE_MOD, ENTITY_INTERACTION_RANGE_MOD, DAMAGE_RESET_MOD,
        BURNING_TIME_MOD, EXPLOSION_KNOCKBACK_RESISTANCE_MOD, FALL_DAMAGE_MULTIPLIER_MOD,
        JUMP_STRENGTH_MOD, MINING_EFFICIENCY_MOD, MOVEMENT_EFFICIENCY_MOD, OXYGEN_BONUS_MOD,
        SAFE_FALL_DISTANCE_MOD, BLOCK_BREAK_SPEED_MOD, SNEAKING_SPEED_MOD, STEP_HEIGHT_MOD,
        SUBMERGED_MINING_SPEED_MOD, WATER_MOVEMENT_EFFICIENCY_MOD,
        INDIVIDUAL_HEALTH, INDIVIDUAL_ARMOR, INDIVIDUAL_DAMAGE, INDIVIDUAL_SPEED,
        INDIVIDUAL_KNOCKBACK_RES, INDIVIDUAL_ATTACK_KNOCKBACK, INDIVIDUAL_ATTACK_SPEED,
        INDIVIDUAL_FOLLOW_RANGE, INDIVIDUAL_FLYING_SPEED, INDIVIDUAL_SWIM_SPEED,
        INDIVIDUAL_BLOCK_INTERACTION_RANGE, INDIVIDUAL_ENTITY_INTERACTION_RANGE,
        INDIVIDUAL_BURNING_TIME, INDIVIDUAL_EXPLOSION_KNOCKBACK_RESISTANCE,
        INDIVIDUAL_FALL_DAMAGE_MULTIPLIER, INDIVIDUAL_JUMP_STRENGTH,
        INDIVIDUAL_OXYGEN_BONUS, INDIVIDUAL_SAFE_FALL_DISTANCE,
        INDIVIDUAL_SNEAKING_SPEED, INDIVIDUAL_WATER_MOVEMENT_EFFICIENCY
    };

    // ============================================================
    // EVENT: Спавн сущности
    // ============================================================
    @SubscribeEvent
    public static void onEntitySpawn(EntityJoinLevelEvent event) {
        if (!event.getLevel().isClientSide() && event.getEntity() instanceof LivingEntity entity) {
            Level world = event.getLevel();
            ResourceLocation dimensionId = world.dimension() != null ? world.dimension().location() : null;
            if (dimensionId == null) {
                LOGGER.warn("Dimension ID is null for entity: {}", entity.getType());
                return;
            }
            String dimKey = dimensionId.toString();
            boolean isNight = isNight(world);

            double healthMultiplier = getDifficultyMultiplier(world.getDifficulty(), true);
            double damageMultiplier = getDifficultyMultiplier(world.getDifficulty(), false);

            if (entity instanceof Player player) {
                handlePlayerModifiers(player, dimKey, isNight);
            } else {
                handleMobModifiers(entity, world, dimKey, isNight, healthMultiplier, damageMultiplier);
            }
        }
    }

    // ============================================================
    // EVENT: Тик мира (смена дня/ночь)
    // ============================================================
    @SubscribeEvent
    public static void onWorldTick(LevelTickEvent.Post event) {
        if (!event.getLevel().isClientSide()) {
            Level world = event.getLevel();
            if (world.dimension() == null) return;

            ResourceLocation dimensionId = world.dimension().location();
            String dimKey = dimensionId.toString();
            boolean isNight = isNight(world);
            Boolean lastState = lastNightState.get(dimKey);

            // Если состояние ночи изменилось — обновляем только игроков
            if (lastState == null || lastState != isNight) {
                if (isDebugLogging()) {
                    LOGGER.debug("Time state changed in dimension {}: isNight={}, time={}",
                        dimKey, isNight, world.getDayTime() % 24000);
                }

                lastNightState.put(dimKey, isNight);

                // Обновляем игроков
                for (Player player : world.players()) {
                    if (isPlayerBlocked(player, dimKey)) {
                        if (isDebugLogging()) {
                            LOGGER.debug("Player {} is blocked in dimension {}, skipping",
                                player.getName().getString(), dimKey);
                        }
                        continue;
                    }

                    PlayerModifiers playerMods = PlayerConfigManager.getPlayerConfig().getModifiersForDimension(dimKey);
                    if (playerMods == null) {
                        if (isDebugLogging()) {
                            LOGGER.debug("No player modifiers for dimension {}, skipping player {}",
                                dimKey, player.getName().getString());
                        }
                        continue;
                    }

                    if (!playerMods.isNightScalingEnabled()) {
                        if (isDebugLogging()) {
                            LOGGER.debug("Night scaling disabled for player {}, skipping day/night update",
                                player.getName().getString());
                        }
                        continue;
                    }

                    handlePlayerModifiers(player, dimKey, isNight);
                }
            }
        }
    }

    // ============================================================
    // EVENT: Выгрузка мира — очистка кэшей измерения
    // ============================================================
    @SubscribeEvent
    public static void onWorldUnload(LevelEvent.Unload event) {
        if (!(event.getLevel() instanceof net.minecraft.server.level.ServerLevel serverLevel)) return;
        ResourceLocation dimensionId = serverLevel.dimension() != null
            ? serverLevel.dimension().location() : null;
        if (dimensionId == null) return;
        String dimKey = dimensionId.toString();

        lastNightState.remove(dimKey);

        if (isDebugLogging()) {
            LOGGER.debug("Cleared night state cache for unloaded dimension: {}", dimKey);
        }
    }

    // ============================================================
    // EVENT: Падение сущности
    // ============================================================
    @SubscribeEvent
    public static void onEntityFall(LivingFallEvent event) {
        LivingEntity entity = event.getEntity();
        try {
            AttributeInstance attr = entity.getAttribute(Attributes.GRAVITY);
            if (attr == null) return;

            double baseGravity = attr.getBaseValue();
            if (baseGravity <= 0.0) return; // защита от деления на ноль

            double currentGravity = attr.getValue();
            double gravityRatio = currentGravity / baseGravity;

            if (gravityRatio < NO_FALL_DAMAGE_GRAVITY_THRESHOLD) {
                // Полностью отменяем урон
                event.setCanceled(true);
                if (isDebugLogging()) {
                LOGGER.debug("Canceled fall damage for entity {} due to low gravity: {}",
                    entity.getType().getDescriptionId(), gravityRatio);}
            } else {
                // Масштабируем урон пропорционально гравитации
                float originalDamage = event.getDamageMultiplier();
                float scaledDamage = (float) (originalDamage * gravityRatio);
                event.setDamageMultiplier(scaledDamage);
                if (isDebugLogging()) {
                LOGGER.debug("Scaled fall damage for entity {} by gravity ratio: {} (original: {}, scaled: {})",
                    entity.getType().getDescriptionId(), gravityRatio, originalDamage, scaledDamage);
            }
        }
        } catch (Exception e) {

            LOGGER.error("Error handling fall damage for entity {}", entity.getType(), e);
        }
    }

    // ============================================================
    // Утилиты
    // ============================================================

    private static boolean isPlayerBlocked(Player player, String dimensionId) {
        return PlayerConfigManager.getPlayerConfig().isPlayerBlocked(player.getName().getString(), dimensionId);
    }

    /**
     * Проверяет, заблокирована ли сущность в данном измерении.
     * Использует только общие чёрные списки (mod + entity).
     */
    private static boolean isEntityBlocked(DimensionConfig config, ResourceLocation entityId) {
        String modId = entityId.getNamespace();
        String entityIdStr = entityId.toString();
        return config.getModBlacklist().contains(modId) || config.getEntityBlacklist().contains(entityIdStr);
    }

    public static boolean isNight(Level world) {
        long currentTime = world.getDayTime();
        long timeOfDay = currentTime % 24000;
        return timeOfDay >= 13000 && timeOfDay < 23000;
    }

    public static double getDifficultyMultiplier(Difficulty difficulty, boolean isHealth) {
        return switch (difficulty) {
            case PEACEFUL -> isHealth ? MobScalerConfig.HEALTH_PEACEFUL.get() : MobScalerConfig.DAMAGE_PEACEFUL.get();
            case EASY -> isHealth ? MobScalerConfig.HEALTH_EASY.get() : MobScalerConfig.DAMAGE_EASY.get();
            case NORMAL -> isHealth ? MobScalerConfig.HEALTH_NORMAL.get() : MobScalerConfig.DAMAGE_NORMAL.get();
            case HARD -> isHealth ? MobScalerConfig.HEALTH_HARD.get() : MobScalerConfig.DAMAGE_HARD.get();
        };
    }

    // ============================================================
    // Проверка наличия модификаторов мода на сущности
    // ============================================================

    /**
     * Проверяет, включено ли детальное дебаг-логирование (наша настройка + SLF4J).
     */
    private static boolean isDebugLogging() {
        return MobScalerConfig.isDebugLoggingEnabled() && LOGGER.isDebugEnabled();
    }

    /**
     * Проверяет, есть ли уже на сущности хотя бы один модификатор мода.
     * Используется для предотвращения повторной обработки при EntityJoinLevelEvent
     * (срабатывает не только при спавне, но и при телепортации/загрузке чанка).
     */
    private static boolean hasMobscalerModifiers(LivingEntity entity) {
        // Проверяем основной маркер — модификатор здоровья
        AttributeInstance healthAttr = entity.getAttribute(Attributes.MAX_HEALTH);
        if (healthAttr != null && (healthAttr.hasModifier(HEALTH_MODIFIER) || healthAttr.hasModifier(INDIVIDUAL_HEALTH))) {
            return true;
        }
        return false;
    }

    // ============================================================
    // Обработка игрока
    // ============================================================
    public static void handlePlayerModifiers(Player player, String dimKey, boolean isNight) {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Handling player modifiers for {} in dimension {}, isNight: {}",
                player.getName().getString(), dimKey, isNight);
        }

        if (isPlayerBlocked(player, dimKey)) {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Player {} is blocked in dimension {}", player.getName().getString(), dimKey);
            }
            return;
        }

        PlayerModifiers playerMods = PlayerConfigManager.getPlayerConfig().getModifiersForDimension(dimKey);
        if (playerMods == null) {
            LOGGER.warn("No player modifiers found for dimension {}", dimKey);
            return;
        }

        boolean useNightModifiers = isNight && playerMods.isNightScalingEnabled();

        // Сохраняем текущий процент здоровья
        float healthPercent = player.getHealth() / player.getMaxHealth();

        // Обновляем все атрибуты игрока
        smoothlyUpdateAttribute(player, Attributes.MAX_HEALTH, HEALTH_MODIFIER, "health",
            useNightModifiers ? playerMods.getNightHealthAddition() : playerMods.getHealthAddition(),
            useNightModifiers ? playerMods.getNightHealthMultiplier() : playerMods.getHealthMultiplier(),
            1.0);

        smoothlyUpdateAttribute(player, Attributes.ARMOR, ARMOR_MODIFIER, "armor",
            useNightModifiers ? playerMods.getNightArmorAddition() : playerMods.getArmorAddition(),
            useNightModifiers ? playerMods.getNightArmorMultiplier() : playerMods.getArmorMultiplier(),
            1.0);

        smoothlyUpdateAttribute(player, Attributes.ATTACK_DAMAGE, DAMAGE_MODIFIER, "damage",
            useNightModifiers ? playerMods.getNightDamageAddition() : playerMods.getDamageAddition(),
            useNightModifiers ? playerMods.getNightDamageMultiplier() : playerMods.getDamageMultiplier(),
            1.0);

        smoothlyUpdateAttribute(player, Attributes.MOVEMENT_SPEED, SPEED_MODIFIER, "speed",
            useNightModifiers ? playerMods.getNightSpeedAddition() : playerMods.getSpeedAddition(),
            useNightModifiers ? playerMods.getNightSpeedMultiplier() : playerMods.getSpeedMultiplier(),
            1.0);

        smoothlyUpdateAttribute(player, Attributes.KNOCKBACK_RESISTANCE, KNOCKBACK_RESISTANCE_MOD, "knockback_resistance",
            useNightModifiers ? playerMods.getNightKnockbackResistanceAddition() : playerMods.getKnockbackResistanceAddition(),
            useNightModifiers ? playerMods.getNightKnockbackResistanceMultiplier() : playerMods.getKnockbackResistanceMultiplier(),
            1.0);

        smoothlyUpdateAttribute(player, Attributes.ATTACK_KNOCKBACK, ATTACK_KNOCKBACK_MOD, "attack_knockback",
            useNightModifiers ? playerMods.getNightAttackKnockbackAddition() : playerMods.getAttackKnockbackAddition(),
            useNightModifiers ? playerMods.getNightAttackKnockbackMultiplier() : playerMods.getAttackKnockbackMultiplier(),
            1.0);

        smoothlyUpdateAttribute(player, Attributes.ATTACK_SPEED, ATTACK_SPEED_MOD, "attack_speed",
            useNightModifiers ? playerMods.getNightAttackSpeedAddition() : playerMods.getAttackSpeedAddition(),
            useNightModifiers ? playerMods.getNightAttackSpeedMultiplier() : playerMods.getAttackSpeedMultiplier(),
            1.0);

        smoothlyUpdateAttribute(player, Attributes.FOLLOW_RANGE, FOLLOW_RANGE_MOD, "follow_range",
            useNightModifiers ? playerMods.getNightFollowRangeAddition() : playerMods.getFollowRangeAddition(),
            useNightModifiers ? playerMods.getNightFollowRangeMultiplier() : playerMods.getFollowRangeMultiplier(),
            1.0);

        smoothlyUpdateAttribute(player, Attributes.FLYING_SPEED, FLYING_SPEED_MOD, "flying_speed",
            useNightModifiers ? playerMods.getNightFlyingSpeedAddition() : playerMods.getFlyingSpeedAddition(),
            useNightModifiers ? playerMods.getNightFlyingSpeedMultiplier() : playerMods.getFlyingSpeedMultiplier(),
            1.0);

        smoothlyUpdateAttribute(player, Attributes.ARMOR_TOUGHNESS, ARMOR_TOUGHNESS_MOD, "armor_toughness",
            useNightModifiers ? playerMods.getNightArmorToughnessAddition() : playerMods.getArmorToughnessAddition(),
            useNightModifiers ? playerMods.getNightArmorToughnessMultiplier() : playerMods.getArmorToughnessMultiplier(),
            1.0);

        smoothlyUpdateAttribute(player, Attributes.LUCK, LUCK_MODIFIER, "luck",
            useNightModifiers ? playerMods.getNightLuckAddition() : playerMods.getLuckAddition(),
            useNightModifiers ? playerMods.getNightLuckMultiplier() : playerMods.getLuckMultiplier(),
            1.0);

        // Extended player attributes (1.21+)
        smoothlyUpdateAttribute(player, Attributes.BURNING_TIME, BURNING_TIME_MOD, "burning_time",
            useNightModifiers ? playerMods.getNightBurningTimeAddition() : playerMods.getBurningTimeAddition(),
            useNightModifiers ? playerMods.getNightBurningTimeMultiplier() : playerMods.getBurningTimeMultiplier(),
            1.0);

        smoothlyUpdateAttribute(player, Attributes.EXPLOSION_KNOCKBACK_RESISTANCE, EXPLOSION_KNOCKBACK_RESISTANCE_MOD, "explosion_knockback_resistance",
            useNightModifiers ? playerMods.getNightExplosionKnockbackResistanceAddition() : playerMods.getExplosionKnockbackResistanceAddition(),
            useNightModifiers ? playerMods.getNightExplosionKnockbackResistanceMultiplier() : playerMods.getExplosionKnockbackResistanceMultiplier(),
            1.0);

        smoothlyUpdateAttribute(player, Attributes.FALL_DAMAGE_MULTIPLIER, FALL_DAMAGE_MULTIPLIER_MOD, "fall_damage_multiplier",
            useNightModifiers ? playerMods.getNightFallDamageMultiplier() : playerMods.getFallDamageMultiplier(),
            useNightModifiers ? playerMods.getNightFallDamageMultiplier() : playerMods.getFallDamageMultiplier(),
            1.0);

        smoothlyUpdateAttribute(player, Attributes.JUMP_STRENGTH, JUMP_STRENGTH_MOD, "jump_strength",
            useNightModifiers ? playerMods.getNightJumpStrengthAddition() : playerMods.getJumpStrengthAddition(),
            useNightModifiers ? playerMods.getNightJumpStrengthMultiplier() : playerMods.getJumpStrengthMultiplier(),
            1.0);

        smoothlyUpdateAttribute(player, Attributes.MINING_EFFICIENCY, MINING_EFFICIENCY_MOD, "mining_efficiency",
            useNightModifiers ? playerMods.getNightMiningEfficiencyAddition() : playerMods.getMiningEfficiencyAddition(),
            useNightModifiers ? playerMods.getNightMiningEfficiencyMultiplier() : playerMods.getMiningEfficiencyMultiplier(),
            1.0);

        smoothlyUpdateAttribute(player, Attributes.MOVEMENT_EFFICIENCY, MOVEMENT_EFFICIENCY_MOD, "movement_efficiency",
            useNightModifiers ? playerMods.getNightMovementEfficiencyAddition() : playerMods.getMovementEfficiencyAddition(),
            useNightModifiers ? playerMods.getNightMovementEfficiencyMultiplier() : playerMods.getMovementEfficiencyMultiplier(),
            1.0);

        smoothlyUpdateAttribute(player, Attributes.OXYGEN_BONUS, OXYGEN_BONUS_MOD, "oxygen_bonus",
            useNightModifiers ? playerMods.getNightOxygenBonusAddition() : playerMods.getOxygenBonusAddition(),
            useNightModifiers ? playerMods.getNightOxygenBonusMultiplier() : playerMods.getOxygenBonusMultiplier(),
            1.0);

        smoothlyUpdateAttribute(player, Attributes.SAFE_FALL_DISTANCE, SAFE_FALL_DISTANCE_MOD, "safe_fall_distance",
            useNightModifiers ? playerMods.getNightSafeFallDistanceAddition() : playerMods.getSafeFallDistanceAddition(),
            useNightModifiers ? playerMods.getNightSafeFallDistanceMultiplier() : playerMods.getSafeFallDistanceMultiplier(),
            1.0);

        smoothlyUpdateAttribute(player, Attributes.BLOCK_BREAK_SPEED, BLOCK_BREAK_SPEED_MOD, "block_break_speed",
            useNightModifiers ? playerMods.getNightBlockBreakSpeedAddition() : playerMods.getBlockBreakSpeedAddition(),
            useNightModifiers ? playerMods.getNightBlockBreakSpeedMultiplier() : playerMods.getBlockBreakSpeedMultiplier(),
            1.0);

        smoothlyUpdateAttribute(player, Attributes.BLOCK_INTERACTION_RANGE, BLOCK_INTERACTION_RANGE_MOD, "block_reach",
            useNightModifiers ? playerMods.getNightBlockReachAddition() : playerMods.getBlockReachAddition(),
            useNightModifiers ? playerMods.getNightBlockReachMultiplier() : playerMods.getBlockReachMultiplier(),
            1.0);

        smoothlyUpdateAttribute(player, Attributes.ENTITY_INTERACTION_RANGE, ENTITY_INTERACTION_RANGE_MOD, "entity_reach",
            useNightModifiers ? playerMods.getNightEntityReachAddition() : playerMods.getEntityReachAddition(),
            useNightModifiers ? playerMods.getNightEntityReachMultiplier() : playerMods.getEntityReachMultiplier(),
            1.0);

        smoothlyUpdateAttribute(player, Attributes.STEP_HEIGHT, STEP_HEIGHT_MOD, "step_height",
            useNightModifiers ? playerMods.getNightStepHeightAddition() : playerMods.getStepHeightAddition(),
            useNightModifiers ? playerMods.getNightStepHeightMultiplier() : playerMods.getStepHeightMultiplier(),
            1.0);

        smoothlyUpdateAttribute(player, Attributes.SUBMERGED_MINING_SPEED, SUBMERGED_MINING_SPEED_MOD, "submerged_mining_speed",
            useNightModifiers ? playerMods.getNightSubmergedMiningSpeedAddition() : playerMods.getSubmergedMiningSpeedAddition(),
            useNightModifiers ? playerMods.getNightSubmergedMiningSpeedMultiplier() : playerMods.getSubmergedMiningSpeedMultiplier(),
            1.0);

        smoothlyUpdateAttribute(player, Attributes.WATER_MOVEMENT_EFFICIENCY, WATER_MOVEMENT_EFFICIENCY_MOD, "water_movement_efficiency",
            useNightModifiers ? playerMods.getNightWaterMovementEfficiencyAddition() : playerMods.getWaterMovementEfficiencyAddition(),
            useNightModifiers ? playerMods.getNightWaterMovementEfficiencyMultiplier() : playerMods.getWaterMovementEfficiencyMultiplier(),
            1.0);

        // Гравитация через NeoForgeMod
        applyPlayerGravityModifier(player, playerMods.getGravityMultiplier());

        // Восстанавливаем здоровье в том же проценте от нового максимума
        player.setHealth(Math.min(player.getMaxHealth(), player.getMaxHealth() * healthPercent));

        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Player health restored to {}% ({}/{})",
                Math.round(healthPercent * 100),
                String.format("%.1f", player.getHealth()),
                String.format("%.1f", player.getMaxHealth()));
        }
    }

    /**
     * Применяет множитель гравитации для игрока.
     */
    private static void applyPlayerGravityModifier(Player player, double gravityMultiplier) {
        try {
            AttributeInstance gravityAttr = player.getAttribute(Attributes.GRAVITY);
            if (gravityAttr == null) {
                LOGGER.warn("Gravity attribute недоступна для игрока");
                return;
            }

            smoothlyUpdateAttribute(player, Attributes.GRAVITY, GRAVITY_MODIFIER, "gravity",
                0.0,
                gravityMultiplier,
                1.0);
        } catch (Exception e) {
            LOGGER.error("Error applying gravity modifier for player {}", player.getName().getString(), e);
        }
    }

    // ============================================================
    // smoothlyUpdateAttribute — обновление атрибута с сохранением чужих модификаторов
    // ============================================================
    private static void smoothlyUpdateAttribute(LivingEntity entity,
                                                Holder<Attribute> attribute,
                                                ResourceLocation modifierId,
                                                String name,
                                                double addition,
                                                double multiplier,
                                                double difficultyMultiplier) {
        AttributeInstance attrInstance = entity.getAttribute(attribute);
        if (attrInstance == null) return;

        double baseValue = attrInstance.getBaseValue();
        double newValue = (baseValue + addition) * multiplier * difficultyMultiplier;
        double changeAmount = newValue - baseValue;

        boolean hasModifier = attrInstance.hasModifier(modifierId);

        if (Math.abs(changeAmount) > 0.001) {
            // Значимое изменение — обновляем или создаём модификатор
            AttributeModifier modifier = new AttributeModifier(modifierId, changeAmount, AttributeModifier.Operation.ADD_VALUE);
            attrInstance.addOrReplacePermanentModifier(modifier);
        } else if (hasModifier) {
            // Изменение незначительно — удаляем модификатор
            attrInstance.removeModifier(modifierId);
        }
    }

    // ============================================================
    // Обработка моба
    // ============================================================

    /**
     * Обрабатывает модификаторы моба (без принудительной перезаписи).
     * Используется при спавне сущности.
     */
    public static void handleMobModifiers(LivingEntity entity, Level world, String dimKey,
                                          boolean isNight, double healthMultiplier, double damageMultiplier) {
        handleMobModifiers(entity, world, dimKey, isNight, healthMultiplier, damageMultiplier, false);
    }

    /**
     * Обрабатывает модификаторы моба с флагом принудительной перезаписи.
     * @param force если true — модификаторы перезаписываются даже если уже присутствуют
     */
    public static void handleMobModifiers(LivingEntity entity, Level world, String dimKey,
                                          boolean isNight, double healthMultiplier, double damageMultiplier,
                                          boolean force) {
        if (entity.getType() == null) {
            LOGGER.warn("Entity type is null for entity: {}", entity);
            return;
        }

        ResourceLocation entityId = EntityType.getKey(entity.getType());
        String entityIdStr = entityId.toString();
        String modId = entityId.getNamespace();
        DimensionConfig dimConfig = MobScalerConfig.DIMENSIONS.get(dimKey);

        if (dimConfig == null) {
            LOGGER.warn("Dimension config is null for dimension: {}", dimKey);
            return;
        }

        // Проверяем блокировку до удаления модификаторов
        if (isEntityBlocked(dimConfig, entityId)) {
            if (isDebugLogging()) {
            LOGGER.debug("Entity {} is blocked in dimension {}", entityIdStr, dimKey);
            return;}
        }

        // Проверяем, есть ли уже наши модификаторы — если да и не force, не обрабатываем повторно
        // EntityJoinLevelEvent срабатывает не только при спавне, но и при телепортации/загрузке чанка
        if (!force && hasMobscalerModifiers(entity)) {
            return;
        }

        // Логируем атрибуты до удаления (только если включён debug)
        if (isDebugLogging()) {
            logAllAttributes(entity, "BEFORE REMOVING MODIFIERS");
        }

        // Удаляем ВСЕ модификаторы перед применением новых
        removeAllModifiers(entity);

        // Логируем атрибуты после удаления (только если включён debug)
        if (isDebugLogging()) {
            logAllAttributes(entity, "AFTER REMOVING MODIFIERS");
        }

        // Приоритет: IndividualMobConfig > ModConfig > Standard
        IndividualMobConfig mobConfig = IndividualMobManager.getIndividualMobConfig(entityIdStr);
        if (mobConfig != null) {
            if (isDebugLogging()) {
                LOGGER.debug("Applying individual modifiers for entity: {} in dimension: {}", entityIdStr, dimKey);
            }
            IndividualMobManager.applyModifiers(entity, healthMultiplier, damageMultiplier);
        } else {
            IndividualMobAttributes modConfig = IndividualMobManager.getModConfig(modId);
            if (modConfig != null) {
                if (isDebugLogging()) {
                    LOGGER.debug("Applying mod modifiers for: {} in dimension: {}", modId, dimKey);
                }
                IndividualMobManager.applyModifiers(entity, healthMultiplier, damageMultiplier);
            } else {
                if (isDebugLogging()) {
                    LOGGER.debug("Applying standard modifiers for entity: {} in dimension: {}", entityIdStr, dimKey);
                }
                applyStandardModifiers(entity, dimConfig, isNight, healthMultiplier, damageMultiplier);
            }
        }

        // Логируем атрибуты после применения (только если включён debug)
        if (isDebugLogging()) {
            logAllAttributes(entity, "AFTER APPLYING MODIFIERS");
        }

        // Устанавливаем здоровье на максимум
        AttributeInstance healthAttr = entity.getAttribute(Attributes.MAX_HEALTH);
        if (healthAttr != null) {
            entity.setHealth(entity.getMaxHealth());
        }
    }

    // ============================================================
    // Стандартные модификаторы мобов (cave / night / default)
    // ============================================================
    public static void applyStandardModifiers(LivingEntity entity, DimensionConfig config,
                                              boolean isNight, double healthMultiplier, double damageMultiplier) {
        if (entity == null || config == null) {
            LOGGER.warn("Entity or config is null, cannot apply standard modifiers");
            return;
        }

        boolean isCave = entity.getY() <= config.getCaveHeight();

        // Приоритет: пещерные > ночные > обычные
        if (isCave && config.getEnableCaveScaling()) {
            if (isDebugLogging()) {
            LOGGER.debug("Applying cave modifiers for entity: {}", entity.getType());
            }
            applyHealthModifier(entity, config.getCaveHealthAddition(), config.getCaveHealthMultiplier(), healthMultiplier);
            applyArmorModifier(entity, config.getCaveArmorAddition(), config.getCaveArmorMultiplier(), 1.0);
            applyArmorToughnessModifier(entity, config.getCaveArmorToughnessAddition(), config.getCaveArmorToughnessMultiplier(), 1.0);
            if (config.isGravityEnabled()) {
                applyGravityModifier(entity, config.getGravityMultiplier());
            }
            applyLuckModifier(entity, config.getCaveLuckAddition(), config.getCaveLuckMultiplier(), 1.0);
            applyDamageModifier(entity, config.getCaveDamageAddition(), config.getCaveDamageMultiplier(), damageMultiplier);
            applySpeedModifier(entity, config.getCaveSpeedAddition(), config.getCaveSpeedMultiplier(), 1.0);
            applyKnockbackResistanceModifier(entity, config.getCaveKnockbackResistanceAddition(), config.getCaveKnockbackResistanceMultiplier(), 1.0);
            applyAttackKnockbackModifier(entity, config.getCaveAttackKnockbackAddition(), config.getCaveAttackKnockbackMultiplier(), 1.0);
            applyAttackSpeedModifier(entity, config.getCaveAttackSpeedAddition(), config.getCaveAttackSpeedMultiplier(), 1.0);
            applyFollowRangeModifier(entity, config.getCaveFollowRangeAddition(), config.getCaveFollowRangeMultiplier(), 1.0);
            applyFlyingSpeedModifier(entity, config.getCaveFlyingSpeedAddition(), config.getCaveFlyingSpeedMultiplier(), 1.0);
            applyBlockInteractionRangeModifier(entity, config.getCaveBlockReachAddition(), config.getCaveBlockReachMultiplier(), 1.0);
            applyEntityInteractionRangeModifier(entity, config.getCaveEntityReachAddition(), config.getCaveEntityReachMultiplier(), 1.0);
            applySwimSpeedModifier(entity, config.getCaveSwimSpeedAddition(), config.getCaveSwimSpeedMultiplier(), 1.0);
            // Extended mob attributes (cave)
            applyBurningTimeModifier(entity, config.getCaveBurningTimeAddition(), config.getCaveBurningTimeMultiplier(), 1.0);
            applyExplosionKnockbackResistanceModifier(entity, config.getCaveExplosionKnockbackResistanceAddition(), config.getCaveExplosionKnockbackResistanceMultiplier(), 1.0);
            applyFallDamageMultiplierModifier(entity, config.getCaveFallDamageMultiplier(), 1.0);
            applyOxygenBonusModifier(entity, config.getCaveOxygenBonusAddition(), config.getCaveOxygenBonusMultiplier(), 1.0);
            applySafeFallDistanceModifier(entity, config.getCaveSafeFallDistanceAddition(), config.getCaveSafeFallDistanceMultiplier(), 1.0);
            applyWaterMovementEfficiencyModifier(entity, config.getCaveWaterMovementEfficiencyAddition(), config.getCaveWaterMovementEfficiencyMultiplier(), 1.0);

        } else if (isNight && config.getEnableNightScaling()) {
            if (isDebugLogging()) {
            LOGGER.debug("Applying night modifiers for entity: {}", entity.getType());
            }
            applyHealthModifier(entity, config.getNightHealthAddition(), config.getNightHealthMultiplier(), healthMultiplier);
            applyArmorModifier(entity, config.getNightArmorAddition(), config.getNightArmorMultiplier(), 1.0);
            applyArmorToughnessModifier(entity, config.getNightArmorToughnessAddition(), config.getNightArmorToughnessMultiplier(), 1.0);
            if (config.isGravityEnabled()) {
                applyGravityModifier(entity, config.getGravityMultiplier());
            }
            applyLuckModifier(entity, config.getNightLuckAddition(), config.getNightLuckMultiplier(), 1.0);
            applyDamageModifier(entity, config.getNightDamageAddition(), config.getNightDamageMultiplier(), damageMultiplier);
            applySpeedModifier(entity, config.getNightSpeedAddition(), config.getNightSpeedMultiplier(), 1.0);
            applyKnockbackResistanceModifier(entity, config.getNightKnockbackResistanceAddition(), config.getNightKnockbackResistanceMultiplier(), 1.0);
            applyAttackKnockbackModifier(entity, config.getNightAttackKnockbackAddition(), config.getNightAttackKnockbackMultiplier(), 1.0);
            applyAttackSpeedModifier(entity, config.getNightAttackSpeedAddition(), config.getNightAttackSpeedMultiplier(), 1.0);
            applyFollowRangeModifier(entity, config.getNightFollowRangeAddition(), config.getNightFollowRangeMultiplier(), 1.0);
            applyFlyingSpeedModifier(entity, config.getNightFlyingSpeedAddition(), config.getNightFlyingSpeedMultiplier(), 1.0);
            applyBlockInteractionRangeModifier(entity, config.getNightBlockReachAddition(), config.getNightBlockReachMultiplier(), 1.0);
            applyEntityInteractionRangeModifier(entity, config.getNightEntityReachAddition(), config.getNightEntityReachMultiplier(), 1.0);
            applySwimSpeedModifier(entity, config.getNightSwimSpeedAddition(), config.getNightSwimSpeedMultiplier(), 1.0);
            // Extended mob attributes (night)
            applyBurningTimeModifier(entity, config.getNightBurningTimeAddition(), config.getNightBurningTimeMultiplier(), 1.0);
            applyExplosionKnockbackResistanceModifier(entity, config.getNightExplosionKnockbackResistanceAddition(), config.getNightExplosionKnockbackResistanceMultiplier(), 1.0);
            applyFallDamageMultiplierModifier(entity, config.getNightFallDamageMultiplier(), 1.0);
            applyOxygenBonusModifier(entity, config.getNightOxygenBonusAddition(), config.getNightOxygenBonusMultiplier(), 1.0);
            applySafeFallDistanceModifier(entity, config.getNightSafeFallDistanceAddition(), config.getNightSafeFallDistanceMultiplier(), 1.0);
            applyWaterMovementEfficiencyModifier(entity, config.getNightWaterMovementEfficiencyAddition(), config.getNightWaterMovementEfficiencyMultiplier(), 1.0);

        } else {
            if (isDebugLogging()) {
            LOGGER.debug("Applying default modifiers for entity: {}", entity.getType());
            }
            applyHealthModifier(entity, config.getHealthAddition(), config.getHealthMultiplier(), healthMultiplier);
            applyArmorModifier(entity, config.getArmorAddition(), config.getArmorMultiplier(), 1.0);
            applyArmorToughnessModifier(entity, config.getArmorToughnessAddition(), config.getArmorToughnessMultiplier(), 1.0);
            if (config.isGravityEnabled()) {
                applyGravityModifier(entity, config.getGravityMultiplier());
            }
            applyLuckModifier(entity, config.getLuckAddition(), config.getLuckMultiplier(), 1.0);
            applyDamageModifier(entity, config.getDamageAddition(), config.getDamageMultiplier(), damageMultiplier);
            applySpeedModifier(entity, config.getSpeedAddition(), config.getSpeedMultiplier(), 1.0);
            applyKnockbackResistanceModifier(entity, config.getKnockbackResistanceAddition(), config.getKnockbackResistanceMultiplier(), 1.0);
            applyAttackKnockbackModifier(entity, config.getAttackKnockbackAddition(), config.getAttackKnockbackMultiplier(), 1.0);
            applyAttackSpeedModifier(entity, config.getAttackSpeedAddition(), config.getAttackSpeedMultiplier(), 1.0);
            applyFollowRangeModifier(entity, config.getFollowRangeAddition(), config.getFollowRangeMultiplier(), 1.0);
            applyFlyingSpeedModifier(entity, config.getFlyingSpeedAddition(), config.getFlyingSpeedMultiplier(), 1.0);
            applyBlockInteractionRangeModifier(entity, config.getBlockReachAddition(), config.getBlockReachMultiplier(), 1.0);
            applyEntityInteractionRangeModifier(entity, config.getEntityReachAddition(), config.getEntityReachMultiplier(), 1.0);
            applySwimSpeedModifier(entity, config.getSwimSpeedAddition(), config.getSwimSpeedMultiplier(), 1.0);
            // Extended mob attributes (default)
            applyBurningTimeModifier(entity, config.getBurningTimeAddition(), config.getBurningTimeMultiplier(), 1.0);
            applyExplosionKnockbackResistanceModifier(entity, config.getExplosionKnockbackResistanceAddition(), config.getExplosionKnockbackResistanceMultiplier(), 1.0);
            applyFallDamageMultiplierModifier(entity, config.getFallDamageMultiplier(), 1.0);
            applyOxygenBonusModifier(entity, config.getOxygenBonusAddition(), config.getOxygenBonusMultiplier(), 1.0);
            applySafeFallDistanceModifier(entity, config.getSafeFallDistanceAddition(), config.getSafeFallDistanceMultiplier(), 1.0);
            applyWaterMovementEfficiencyModifier(entity, config.getWaterMovementEfficiencyAddition(), config.getWaterMovementEfficiencyMultiplier(), 1.0);
        }
    }

    // ============================================================
    // Индивидуальные apply*Modifier методы (для стандартных мобов)
    // ============================================================

    private static void applyHealthModifier(LivingEntity entity, double addition, double multiplier, double difficultyMultiplier) {
        AttributeInstance attr = entity.getAttribute(Attributes.MAX_HEALTH);
        if (attr != null) {
            double base = attr.getBaseValue();
            double newMax = (base + addition) * multiplier * difficultyMultiplier;
            attr.removeModifier(HEALTH_MODIFIER);
            attr.addOrReplacePermanentModifier(new AttributeModifier(HEALTH_MODIFIER, newMax - base, AttributeModifier.Operation.ADD_VALUE));
        }
    }

    private static void applyArmorModifier(LivingEntity entity, double addition, double multiplier, double difficultyMultiplier) {
        AttributeInstance attr = entity.getAttribute(Attributes.ARMOR);
        if (attr != null) {
            double base = attr.getBaseValue();
            double newValue = (base + addition) * multiplier * difficultyMultiplier;
            attr.removeModifier(ARMOR_MODIFIER);
            attr.addOrReplacePermanentModifier(new AttributeModifier(ARMOR_MODIFIER, newValue - base, AttributeModifier.Operation.ADD_VALUE));
        }
    }

    private static void applyArmorToughnessModifier(LivingEntity entity, double addition, double multiplier, double difficultyMultiplier) {
        AttributeInstance attr = entity.getAttribute(Attributes.ARMOR_TOUGHNESS);
        if (attr != null) {
            double base = attr.getBaseValue();
            double newValue = (base + addition) * multiplier * difficultyMultiplier;
            attr.removeModifier(ARMOR_TOUGHNESS_MOD);
            attr.addOrReplacePermanentModifier(new AttributeModifier(ARMOR_TOUGHNESS_MOD, newValue - base, AttributeModifier.Operation.ADD_VALUE));
        }
    }

    private static void applyGravityModifier(LivingEntity entity, double multiplier) {
        try {
            AttributeInstance attr = entity.getAttribute(Attributes.GRAVITY);
            if (attr == null) return;

            double base = attr.getBaseValue();
            double newValue = base * multiplier;
            double difference = newValue - base;

            attr.removeModifier(GRAVITY_MODIFIER);
            attr.addOrReplacePermanentModifier(new AttributeModifier(GRAVITY_MODIFIER, difference, AttributeModifier.Operation.ADD_VALUE));

            if (isDebugLogging()) {
                LOGGER.debug("Applied gravity modifier to {}: base={}, multiplier={}, new={}",
                    entity.getType().getDescriptionId(), base, multiplier, newValue);
            }
        } catch (Exception e) {
            LOGGER.error("Error applying gravity modifier for entity {}", entity.getType(), e);
        }
    }

    private static void applyLuckModifier(LivingEntity entity, double addition, double multiplier, double difficultyMultiplier) {
        AttributeInstance attr = entity.getAttribute(Attributes.LUCK);
        if (attr != null) {
            double base = attr.getBaseValue();
            double newValue = (base + addition) * multiplier * difficultyMultiplier;
            attr.removeModifier(LUCK_MODIFIER);
            attr.addOrReplacePermanentModifier(new AttributeModifier(LUCK_MODIFIER, newValue - base, AttributeModifier.Operation.ADD_VALUE));
        }
    }

    private static void applyDamageModifier(LivingEntity entity, double addition, double multiplier, double difficultyMultiplier) {
        AttributeInstance attr = entity.getAttribute(Attributes.ATTACK_DAMAGE);
        if (attr != null) {
            double base = attr.getBaseValue();
            double newValue = (base + addition) * multiplier * difficultyMultiplier;
            attr.removeModifier(DAMAGE_MODIFIER);
            attr.addOrReplacePermanentModifier(new AttributeModifier(DAMAGE_MODIFIER, newValue - base, AttributeModifier.Operation.ADD_VALUE));
        }
    }

    private static void applySpeedModifier(LivingEntity entity, double addition, double multiplier, double difficultyMultiplier) {
        AttributeInstance attr = entity.getAttribute(Attributes.MOVEMENT_SPEED);
        if (attr != null) {
            double base = attr.getBaseValue();
            double newValue = (base + addition) * multiplier * difficultyMultiplier;
            attr.removeModifier(SPEED_MODIFIER);
            attr.addOrReplacePermanentModifier(new AttributeModifier(SPEED_MODIFIER, newValue - base, AttributeModifier.Operation.ADD_VALUE));
        }
    }

    private static void applyKnockbackResistanceModifier(LivingEntity entity, double addition, double multiplier, double difficultyMultiplier) {
        AttributeInstance attr = entity.getAttribute(Attributes.KNOCKBACK_RESISTANCE);
        if (attr != null) {
            double base = attr.getBaseValue();
            double newValue = (base + addition) * multiplier * difficultyMultiplier;
            attr.removeModifier(KNOCKBACK_RESISTANCE_MOD);
            attr.addOrReplacePermanentModifier(new AttributeModifier(KNOCKBACK_RESISTANCE_MOD, newValue - base, AttributeModifier.Operation.ADD_VALUE));
        }
    }

    private static void applyAttackKnockbackModifier(LivingEntity entity, double addition, double multiplier, double difficultyMultiplier) {
        AttributeInstance attr = entity.getAttribute(Attributes.ATTACK_KNOCKBACK);
        if (attr != null) {
            double base = attr.getBaseValue();
            double newValue = (base + addition) * multiplier * difficultyMultiplier;
            attr.removeModifier(ATTACK_KNOCKBACK_MOD);
            attr.addOrReplacePermanentModifier(new AttributeModifier(ATTACK_KNOCKBACK_MOD, newValue - base, AttributeModifier.Operation.ADD_VALUE));
        }
    }

    private static void applyAttackSpeedModifier(LivingEntity entity, double addition, double multiplier, double difficultyMultiplier) {
        AttributeInstance attr = entity.getAttribute(Attributes.ATTACK_SPEED);
        if (attr != null) {
            double base = attr.getBaseValue();
            double newValue = (base + addition) * multiplier * difficultyMultiplier;
            attr.removeModifier(ATTACK_SPEED_MOD);
            attr.addOrReplacePermanentModifier(new AttributeModifier(ATTACK_SPEED_MOD, newValue - base, AttributeModifier.Operation.ADD_VALUE));
        }
    }

    private static void applyFollowRangeModifier(LivingEntity entity, double addition, double multiplier, double difficultyMultiplier) {
        AttributeInstance attr = entity.getAttribute(Attributes.FOLLOW_RANGE);
        if (attr != null) {
            double base = attr.getBaseValue();
            double newValue = (base + addition) * multiplier * difficultyMultiplier;
            attr.removeModifier(FOLLOW_RANGE_MOD);
            attr.addOrReplacePermanentModifier(new AttributeModifier(FOLLOW_RANGE_MOD, newValue - base, AttributeModifier.Operation.ADD_VALUE));
        }
    }

    private static void applyFlyingSpeedModifier(LivingEntity entity, double addition, double multiplier, double difficultyMultiplier) {
        AttributeInstance attr = entity.getAttribute(Attributes.FLYING_SPEED);
        if (attr != null) {
            double base = attr.getBaseValue();
            double newValue = (base + addition) * multiplier * difficultyMultiplier;
            attr.removeModifier(FLYING_SPEED_MOD);
            attr.addOrReplacePermanentModifier(new AttributeModifier(FLYING_SPEED_MOD, newValue - base, AttributeModifier.Operation.ADD_VALUE));
        }
    }

    private static void applyBlockInteractionRangeModifier(LivingEntity entity, double addition, double multiplier, double difficultyMultiplier) {
        try {
            AttributeInstance attr = entity.getAttribute(Attributes.BLOCK_INTERACTION_RANGE);
            if (attr == null) return;

            double base = attr.getBaseValue();
            double newValue = (base + addition) * multiplier * difficultyMultiplier;
            attr.removeModifier(BLOCK_INTERACTION_RANGE_MOD);
            attr.addOrReplacePermanentModifier(new AttributeModifier(BLOCK_INTERACTION_RANGE_MOD, newValue - base, AttributeModifier.Operation.ADD_VALUE));
        } catch (Exception e) {
            LOGGER.error("Error applying block interaction range modifier for entity {}", entity.getType(), e);
        }
    }

    private static void applyEntityInteractionRangeModifier(LivingEntity entity, double addition, double multiplier, double difficultyMultiplier) {
        try {
            AttributeInstance attr = entity.getAttribute(Attributes.ENTITY_INTERACTION_RANGE);
            if (attr == null) return;

            double base = attr.getBaseValue();
            double newValue = (base + addition) * multiplier * difficultyMultiplier;
            attr.removeModifier(ENTITY_INTERACTION_RANGE_MOD);
            attr.addOrReplacePermanentModifier(new AttributeModifier(ENTITY_INTERACTION_RANGE_MOD, newValue - base, AttributeModifier.Operation.ADD_VALUE));
        } catch (Exception e) {
            LOGGER.error("Error applying entity interaction range modifier for entity {}", entity.getType(), e);
        }
    }

    private static void applySwimSpeedModifier(LivingEntity entity, double addition, double multiplier, double difficultyMultiplier) {
        try {
            AttributeInstance attr = entity.getAttribute(NeoForgeMod.SWIM_SPEED);
            if (attr == null) return;

            double base = attr.getBaseValue();
            double newValue = (base + addition) * multiplier * difficultyMultiplier;
            attr.removeModifier(SWIM_SPEED_MODIFIER);
            attr.addOrReplacePermanentModifier(new AttributeModifier(SWIM_SPEED_MODIFIER, newValue - base, AttributeModifier.Operation.ADD_VALUE));
        } catch (Exception e) {
            LOGGER.error("Error applying swim speed modifier for entity {}", entity.getType(), e);
        }
    }

    // Extended mob attribute modifiers (1.21+)
    private static void applyBurningTimeModifier(LivingEntity entity, double addition, double multiplier, double difficultyMultiplier) {
        AttributeInstance attr = entity.getAttribute(Attributes.BURNING_TIME);
        if (attr != null) {
            double base = attr.getBaseValue();
            double newValue = (base + addition) * multiplier * difficultyMultiplier;
            attr.removeModifier(BURNING_TIME_MOD);
            attr.addOrReplacePermanentModifier(new AttributeModifier(BURNING_TIME_MOD, newValue - base, AttributeModifier.Operation.ADD_VALUE));
        }
    }

    private static void applyExplosionKnockbackResistanceModifier(LivingEntity entity, double addition, double multiplier, double difficultyMultiplier) {
        AttributeInstance attr = entity.getAttribute(Attributes.EXPLOSION_KNOCKBACK_RESISTANCE);
        if (attr != null) {
            double base = attr.getBaseValue();
            double newValue = (base + addition) * multiplier * difficultyMultiplier;
            attr.removeModifier(EXPLOSION_KNOCKBACK_RESISTANCE_MOD);
            attr.addOrReplacePermanentModifier(new AttributeModifier(EXPLOSION_KNOCKBACK_RESISTANCE_MOD, newValue - base, AttributeModifier.Operation.ADD_VALUE));
        }
    }

    private static void applyFallDamageMultiplierModifier(LivingEntity entity, double multiplier, double difficultyMultiplier) {
        AttributeInstance attr = entity.getAttribute(Attributes.FALL_DAMAGE_MULTIPLIER);
        if (attr != null) {
            double base = attr.getBaseValue();
            double newValue = base * multiplier * difficultyMultiplier;
            attr.removeModifier(FALL_DAMAGE_MULTIPLIER_MOD);
            attr.addOrReplacePermanentModifier(new AttributeModifier(FALL_DAMAGE_MULTIPLIER_MOD, newValue - base, AttributeModifier.Operation.ADD_VALUE));
        }
    }

    private static void applyOxygenBonusModifier(LivingEntity entity, double addition, double multiplier, double difficultyMultiplier) {
        AttributeInstance attr = entity.getAttribute(Attributes.OXYGEN_BONUS);
        if (attr != null) {
            double base = attr.getBaseValue();
            double newValue = (base + addition) * multiplier * difficultyMultiplier;
            attr.removeModifier(OXYGEN_BONUS_MOD);
            attr.addOrReplacePermanentModifier(new AttributeModifier(OXYGEN_BONUS_MOD, newValue - base, AttributeModifier.Operation.ADD_VALUE));
        }
    }

    private static void applySafeFallDistanceModifier(LivingEntity entity, double addition, double multiplier, double difficultyMultiplier) {
        AttributeInstance attr = entity.getAttribute(Attributes.SAFE_FALL_DISTANCE);
        if (attr != null) {
            double base = attr.getBaseValue();
            double newValue = (base + addition) * multiplier * difficultyMultiplier;
            attr.removeModifier(SAFE_FALL_DISTANCE_MOD);
            attr.addOrReplacePermanentModifier(new AttributeModifier(SAFE_FALL_DISTANCE_MOD, newValue - base, AttributeModifier.Operation.ADD_VALUE));
        }
    }

    private static void applyWaterMovementEfficiencyModifier(LivingEntity entity, double addition, double multiplier, double difficultyMultiplier) {
        AttributeInstance attr = entity.getAttribute(Attributes.WATER_MOVEMENT_EFFICIENCY);
        if (attr != null) {
            double base = attr.getBaseValue();
            double newValue = (base + addition) * multiplier * difficultyMultiplier;
            attr.removeModifier(WATER_MOVEMENT_EFFICIENCY_MOD);
            attr.addOrReplacePermanentModifier(new AttributeModifier(WATER_MOVEMENT_EFFICIENCY_MOD, newValue - base, AttributeModifier.Operation.ADD_VALUE));
        }
    }

    // ============================================================
    // removeAllModifiers — полная очистка всех модификаторов мода
    // ============================================================
    private static void removeAllModifiers(LivingEntity entity) {
        if (isDebugLogging()) {
            LOGGER.debug("Removing modifiers for: {}", entity.getType().getDescriptionId());
        }

        // Сохраняем базовые значения атрибутов
        Map<Holder<Attribute>, Double> baseValues = new HashMap<>();
        for (Holder<Attribute> attribute : getAllAttributes()) {
            AttributeInstance attr = entity.getAttribute(attribute);
            if (attr != null) {
                baseValues.put(attribute, attr.getBaseValue());
            }
        }

        // Удаляем все модификаторы мода по ResourceLocation
        for (Holder<Attribute> attribute : getAllAttributes()) {
            AttributeInstance attr = entity.getAttribute(attribute);
            if (attr == null) continue;

            for (ResourceLocation modifierId : ALL_MODIFIER_IDS) {
                attr.removeModifier(modifierId);
            }
        }

        // Удаляем ВСЕ модификаторы с именем "mobscaler:*" (на случай пропущенных)
        for (Holder<Attribute> attribute : getAllAttributes()) {
            AttributeInstance attr = entity.getAttribute(attribute);
            if (attr == null) continue;

            Collection<AttributeModifier> modifiers = attr.getModifiers();
            List<ResourceLocation> toRemove = new ArrayList<>();
            for (AttributeModifier modifier : modifiers) {
                if (modifier.id().getNamespace().equals(MOD_ID)) {
                    toRemove.add(modifier.id());
                }
            }
            for (ResourceLocation id : toRemove) {
                attr.removeModifier(id);
            }
        }

        // Сбрасываем здоровье к базовому
        AttributeInstance healthAttr = entity.getAttribute(Attributes.MAX_HEALTH);
        if (healthAttr != null) {
            double baseValue = healthAttr.getBaseValue();
            entity.setHealth((float) baseValue);
            if (isDebugLogging()) {
                LOGGER.debug("Reset health to: {}", String.format("%.1f", baseValue));
            }
        }

        // Компенсируем урон — добавляем временный модификатор, возвращающий к базовому
        AttributeInstance damageAttr = entity.getAttribute(Attributes.ATTACK_DAMAGE);
        if (damageAttr != null) {
            Double baseValue = baseValues.get(Attributes.ATTACK_DAMAGE);
            if (baseValue != null) {
                double currentValue = damageAttr.getValue();
                if (Math.abs(currentValue - baseValue) > 0.001) {
                    double diff = baseValue - currentValue;

                    // Сначала удаляем старый компенсационный модификатор если есть
                    damageAttr.removeModifier(DAMAGE_RESET_MOD);

                    AttributeModifier tempModifier = new AttributeModifier(
                        DAMAGE_RESET_MOD, diff, AttributeModifier.Operation.ADD_VALUE
                    );
                    damageAttr.addTransientModifier(tempModifier);

                    if (isDebugLogging()) {
                        LOGGER.debug("Added compensation modifier to reset damage: base={}, current={}, diff={}",
                            String.format("%.1f", baseValue),
                            String.format("%.1f", currentValue),
                            String.format("%.1f", diff));
                    }
                }
            }
        }
    }

    // ============================================================
    // Логирование атрибутов (debug)
    // ============================================================
    private static void logAllAttributes(LivingEntity entity, String stage) {
        if (!LOGGER.isDebugEnabled()) return;

        LOGGER.debug("--- {} for entity: {} ---", stage, entity.getType());
        @SuppressWarnings("unchecked")
        Holder<Attribute>[] standardAttrs = new Holder[]{
            Attributes.MAX_HEALTH,
            Attributes.ARMOR,
            Attributes.ARMOR_TOUGHNESS,
            Attributes.ATTACK_DAMAGE,
            Attributes.MOVEMENT_SPEED,
            Attributes.KNOCKBACK_RESISTANCE,
            Attributes.ATTACK_KNOCKBACK,
            Attributes.ATTACK_SPEED,
            Attributes.FOLLOW_RANGE,
            Attributes.FLYING_SPEED,
            Attributes.LUCK
        };

        for (Holder<Attribute> attribute : standardAttrs) {
            AttributeInstance attr = entity.getAttribute(attribute);
            if (attr != null && (!attr.getModifiers().isEmpty() || attribute.equals(Attributes.MAX_HEALTH))) {
                LOGGER.debug("Attribute {}: base={}, value={}",
                    attribute.value().getDescriptionId(),
                    String.format("%.1f", attr.getBaseValue()),
                    String.format("%.1f", attr.getValue()));

                for (AttributeModifier modifier : attr.getModifiers()) {
                    if (Math.abs(modifier.amount()) > 0.001) {
                        LOGGER.debug("  Modifier: id={}, amount={}",
                            modifier.id(),
                            String.format("%.1f", modifier.amount()));
                    }
                }
            }
        }

        // Гравитация
        try {
            AttributeInstance attr = entity.getAttribute(Attributes.GRAVITY);
            if (attr != null) {
                LOGGER.debug("Gravity attribute: base={}, value={}",
                    String.format("%.3f", attr.getBaseValue()),
                    String.format("%.3f", attr.getValue()));

                for (AttributeModifier modifier : attr.getModifiers()) {
                    if (Math.abs(modifier.amount()) > 0.0001) {
                        LOGGER.debug("  Modifier: id={}, amount={}",
                            modifier.id(),
                            String.format("%.3f", modifier.amount()));
                    }
                }
            }
        } catch (Exception e) {
            // Игнорируем ошибки при логировании
        }
    }

    // ============================================================
    // Публичные API для управления конфигурациями
    // ============================================================

    /**
     * Принудительно обновляет модификаторы ВСЕХ живых мобов на сервере.
     * Вызывается при изменении/сохранении конфига через команду.
     */
    public static void reloadAllMobs() {
        net.minecraft.server.MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) {
            LOGGER.warn("Server not available, cannot reload mob modifiers");
            return;
        }

        int totalCount = 0;
        for (Level level : server.getAllLevels()) {
            int count = reloadMobsInLevel(level);
            totalCount += count;
        }
        LOGGER.info("Reloaded modifiers for {} mobs across all dimensions", totalCount);
    }

    /**
     * Обновляет модификаторы всех мобов в конкретном измерении.
     */
    public static void reloadMobsInDimension(String dimKey) {
        net.minecraft.server.MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) return;

        String[] parts = dimKey.split(":");
        ResourceLocation dimLocation = ResourceLocation.fromNamespaceAndPath(parts[0], parts[1]);

        for (Level level : server.getAllLevels()) {
            if (level.dimension() != null && level.dimension().location().equals(dimLocation)) {
                int count = reloadMobsInLevel(level);
                LOGGER.info("Reloaded modifiers for {} mobs in dimension {}", count, dimKey);
                return;
            }
        }
        LOGGER.warn("Dimension {} not found, cannot reload", dimKey);
    }

    /**
     * Внутренний метод: обновляет всех мобов в одном мире.
     */
    private static int reloadMobsInLevel(Level level) {
        if (level.isClientSide()) return 0;

        AABB worldBounds = new AABB(
            level.getWorldBorder().getMinX(), level.getMinBuildHeight(), level.getWorldBorder().getMinZ(),
            level.getWorldBorder().getMaxX(), level.getMaxBuildHeight(), level.getWorldBorder().getMaxZ()
        );

        int count = 0;
        for (LivingEntity entity : level.getEntitiesOfClass(LivingEntity.class, worldBounds)) {
            if (entity instanceof Player) continue;

            ResourceLocation entityId = EntityType.getKey(entity.getType());
            DimensionConfig dimConfig = MobScalerConfig.DIMENSIONS.get(
                level.dimension() != null ? level.dimension().location().toString() : "");
            if (dimConfig != null && isEntityBlocked(dimConfig, entityId)) continue;

            boolean isNight = isNight(level);
            double healthMultiplier = getDifficultyMultiplier(level.getDifficulty(), true);
            double damageMultiplier = getDifficultyMultiplier(level.getDifficulty(), false);
            String dimKey = level.dimension() != null ? level.dimension().location().toString() : "";

            handleMobModifiers(entity, level, dimKey, isNight, healthMultiplier, damageMultiplier, true);
            count++;
        }
        return count;
    }

    public static void addIndividualMobConfig(String entityId, IndividualMobConfig config) {
        LOGGER.debug("Adding individual config for entity: {}", entityId);
        IndividualMobManager.addIndividualMobConfig(entityId, config);
    }

    public static void removeIndividualMobConfig(String entityId) {
        IndividualMobManager.removeIndividualMobConfig(entityId);
    }

    public static void addModConfig(String modId, IndividualMobAttributes config) {
        if (config == null) {
            LOGGER.warn("Attempted to add null mod config for: {}", modId);
            return;
        }
        if (isDebugLogging()) {
        LOGGER.debug("Adding mod config for: {} with attributes: enableNightScaling={}, enableCaveScaling={}, healthMultiplier={}, damageMultiplier={}",
            modId,
            config.getEnableNightScaling(),
            config.getEnableCaveScaling(),
            config.getHealthMultiplier(),
            config.getDamageMultiplier()
        );
    }
        IndividualMobManager.addModConfig(modId, config);
    }

    public static void removeModConfig(String modId) {
        if (isDebugLogging()) {
        LOGGER.debug("Removing mod config for: {}", modId);
        }
        IndividualMobManager.removeModConfig(modId);
    }

    public static void removeAllModifiersForEntityType(String entityId) {
        if (entityId == null) return;
        if (isDebugLogging()) {
        LOGGER.debug("Removing all modifiers for entity type: {}", entityId);
        }
        String[] parts = entityId.split(":");
        if (parts.length != 2) return;

        String namespace = parts[0];
        String path = parts[1];
        ResourceLocation entityType = ResourceLocation.fromNamespaceAndPath(namespace, path);

        net.minecraft.server.MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) return;

        for (Level level : server.getAllLevels()) {
            AABB worldBounds = new AABB(
                level.getWorldBorder().getMinX(), Double.NEGATIVE_INFINITY, level.getWorldBorder().getMinZ(),
                level.getWorldBorder().getMaxX(), Double.POSITIVE_INFINITY, level.getWorldBorder().getMaxZ()
            );

            int count = 0;
            for (LivingEntity entity : level.getEntitiesOfClass(LivingEntity.class, worldBounds)) {
                if (EntityType.getKey(entity.getType()).equals(entityType)) {
                    count++;
                    removeAllModifiers(entity);
                    if (isDebugLogging()) {
                    LOGGER.debug("Removed all modifiers from entity #{} of type: {} in world: {}",
                        count, entityId, level.dimension().location());
                    }
                }
            }
            if (isDebugLogging()) {
            LOGGER.debug("Found and reset {} entities of type {} in world {}", count, entityId, level.dimension().location());}
        }
    }
}
