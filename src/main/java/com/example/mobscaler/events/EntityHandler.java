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
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.living.LivingFallEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.server.ServerLifecycleHooks;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class EntityHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(EntityHandler.class);
    private static final UUID HEALTH_MODIFIER_UUID = UUID.fromString("a9c8745e-1234-5678-90ab-cdef12345678");
    private static final UUID ARMOR_MODIFIER_UUID = UUID.fromString("b8d7654f-4321-5678-90ab-cdef654321ba");
    private static final UUID DAMAGE_MODIFIER_UUID = UUID.fromString("c3d9f8a1-2468-1357-9abc-def456789012");
    private static final UUID SPEED_MODIFIER_UUID = UUID.fromString("d2e4f6a8-3579-2468-90ab-cdef13579024");
    private static final UUID KNOCKBACK_RESISTANCE_UUID = UUID.fromString("e1f3d5b7-9753-1357-90ab-cdef24680135");
    private static final UUID ATTACK_KNOCKBACK_UUID = UUID.fromString("f0e2d4c6-8642-0246-90ab-cdef35791246");
    private static final UUID ATTACK_SPEED_UUID = UUID.fromString("a1b3c5d7-7531-9753-90ab-cdef46802357");
    private static final UUID FOLLOW_RANGE_UUID = UUID.fromString("b2c4d6e8-6420-8642-90ab-cdef57913468");
    private static final UUID FLYING_SPEED_UUID = UUID.fromString("c3d5e7f9-5319-7531-90ab-cdef68024579");
    private static final UUID ARMOR_TOUGHNESS_UUID = UUID.fromString("d4e6f8a0-4208-6420-90ab-cdef79135680");
    private static final UUID LUCK_UUID = UUID.fromString("e5f7a0b2-3197-5319-90ab-cdef80246791");
    private static final UUID GRAVITY_UUID = UUID.fromString("f1f2f3f4-5678-90ab-cdef-1234567890ab");
    private static final UUID SWIM_SPEED_UUID = UUID.fromString("a2b4c6d8-7531-9753-90ab-cdef91357802");
    private static final UUID REACH_DISTANCE_UUID = UUID.fromString("c4d6e8f0-5319-7531-90ab-cdef13579024");

    // Пороговое значение гравитации, ниже которого урон от падения отключается
    private static final double NO_FALL_DAMAGE_GRAVITY_THRESHOLD = 0.6;

    // Храним последнее состояние ночи для каждого измерения
    private static final Map<String, Boolean> lastNightState = new HashMap<>();
    private static long lastNightCheck = 0;

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
            
            // Получаем множители сложности
            double healthMultiplier = getDifficultyMultiplier(world.getDifficulty(), true);
            double damageMultiplier = getDifficultyMultiplier(world.getDifficulty(), false);
            
            if (entity instanceof Player player) {
                handlePlayerModifiers(player, dimKey, isNight);
            } else {
                handleMobModifiers(entity, world, dimKey, isNight, healthMultiplier, damageMultiplier);
            }
        }
    }

    @SubscribeEvent
    public static void onWorldTick(TickEvent.LevelTickEvent event) {
        if (!event.level.isClientSide() && event.phase == TickEvent.Phase.END) {
            Level world = event.level;
            ResourceLocation dimensionId = world.dimension().location();
            String dimKey = dimensionId.toString();
            
            // Проверяем только раз в 200 тиков (10 секунд), чтобы снизить нагрузку
            if (world.getGameTime() - lastNightCheck < 200) {
                return;
            }
            
            lastNightCheck = world.getGameTime();
            boolean isNight = isNight(world);
            
            // Проверяем состояние ночи для каждого измерения
            Boolean lastState = lastNightState.get(dimKey);
            
            // Если это первый тик или состояние изменилось
            if (lastState == null || lastState != isNight) {
                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug("Time state changed in dimension {}: isNight={}, time={}", 
                        dimKey, isNight, world.getDayTime() % 24000);
                }
                
                lastNightState.put(dimKey, isNight);
                
                // Для каждого игрока в мире проверяем, нужно ли обновлять его модификаторы
                for (Player player : world.players()) {
                    // Проверяем, не заблокирован ли игрок в данном измерении
                    if (isPlayerBlocked(player, dimKey)) {
                        if (LOGGER.isDebugEnabled()) {
                            LOGGER.debug("Player {} is blocked in dimension {}, skipping", 
                                player.getName().getString(), dimKey);
                        }
                        continue;
                    }
                    
                    // Получаем настройки игрока для текущего измерения
                    PlayerModifiers playerMods = PlayerConfigManager.getPlayerConfig().getModifiersForDimension(dimKey);
                    if (playerMods == null) {
                        LOGGER.warn("No player modifiers found for dimension {}, skipping player {}", 
                            dimKey, player.getName().getString());
                        continue;
                    }
                    
                    // Проверяем, включено ли ночное масштабирование
                    if (!playerMods.isNightScalingEnabled()) {
                        if (LOGGER.isDebugEnabled()) {
                            LOGGER.debug("Night scaling disabled for player {}, skipping update on day/night change", 
                                player.getName().getString());
                        }
                        continue;
                    }
                    
                    // Обновляем только если включено ночное масштабирование
                    if (LOGGER.isDebugEnabled()) {
                        LOGGER.debug("Updating modifiers for player {} due to day/night change. Night scaling enabled", 
                            player.getName().getString());
                    }
                    handlePlayerModifiers(player, dimKey, isNight);
                }
                
                // Обновляем модификаторы для мобов только в ограниченном радиусе вокруг игроков
                double range = 128.0; // Обновляем мобов только в радиусе 128 блоков от игроков
                
                double healthMultiplier = getDifficultyMultiplier(world.getDifficulty(), true);
                double damageMultiplier = getDifficultyMultiplier(world.getDifficulty(), false);
                
                for (Player player : world.players()) {
                    AABB playerArea = new AABB(
                        player.getX() - range, 0, player.getZ() - range,
                        player.getX() + range, world.getMaxBuildHeight(), player.getZ() + range
                    );
                    
                    for (LivingEntity entity : world.getEntitiesOfClass(LivingEntity.class, playerArea)) {
                        if (!(entity instanceof Player)) {
                            handleMobModifiers(entity, world, dimKey, isNight, healthMultiplier, damageMultiplier);
                        }
                    }
                }
            }
        }
    }

    private static boolean isPlayerBlocked(Player player, String dimensionId) {
        return PlayerConfigManager.getPlayerConfig().isPlayerBlocked(player.getName().getString(), dimensionId);
    }

    private static boolean isEntityBlocked(DimensionConfig config, ResourceLocation entityId, boolean isNight, boolean isCave) {
        String modId = entityId.getNamespace();
        String entityIdStr = entityId.toString();
        
        // Используем только общие черные списки для всех условий (день, ночь, пещера)
        return config.getModBlacklist().contains(modId) || config.getEntityBlacklist().contains(entityIdStr);
    }

    public static boolean isNight(Level world) {
        long currentTime = world.getDayTime();
        long timeOfDay = currentTime % 24000;
        
        // Реальная проверка времени суток вместо кэширования
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
        
        // Получаем модификаторы игрока
        PlayerModifiers playerMods = PlayerConfigManager.getPlayerConfig().getModifiersForDimension(dimKey);
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Player modifiers: {}, Night scaling enabled: {}", 
                playerMods != null, playerMods != null ? playerMods.isNightScalingEnabled() : false);
        }
            
        if (playerMods == null) {
            LOGGER.warn("No player modifiers found for dimension {}", dimKey);
            return;
        }
        
        // Сохраняем текущее процентное значение здоровья игрока
        float healthPercent = player.getHealth() / player.getMaxHealth();
        
        // Выбираем нужные модификаторы в зависимости от времени суток
        boolean useNightModifiers = isNight && playerMods.isNightScalingEnabled();
        
        if (LOGGER.isDebugEnabled()) {
            if (isNight && !playerMods.isNightScalingEnabled()) {
                LOGGER.debug("Night scaling disabled for player {}, using day modifiers", 
                    player.getName().getString());
            } else {
                LOGGER.debug("Using {} modifiers for player {}", 
                    useNightModifiers ? "night" : "day", 
                    player.getName().getString());
            }
        }
        
        // Плавно обновляем атрибуты, сохраняя модификаторы от других модов
        // Атрибуты здоровья
        smoothlyUpdateAttribute(player, Attributes.MAX_HEALTH, HEALTH_MODIFIER_UUID, "health",
            useNightModifiers ? playerMods.getNightHealthAddition() : playerMods.getHealthAddition(),
            useNightModifiers ? playerMods.getNightHealthMultiplier() : playerMods.getHealthMultiplier(),
            1.0);
        
        // Атрибуты брони
        smoothlyUpdateAttribute(player, Attributes.ARMOR, ARMOR_MODIFIER_UUID, "armor",
            useNightModifiers ? playerMods.getNightArmorAddition() : playerMods.getArmorAddition(),
            useNightModifiers ? playerMods.getNightArmorMultiplier() : playerMods.getArmorMultiplier(),
            1.0);
        
        // Атрибуты урона
        smoothlyUpdateAttribute(player, Attributes.ATTACK_DAMAGE, DAMAGE_MODIFIER_UUID, "damage",
            useNightModifiers ? playerMods.getNightDamageAddition() : playerMods.getDamageAddition(),
            useNightModifiers ? playerMods.getNightDamageMultiplier() : playerMods.getDamageMultiplier(),
            1.0);
        
        // Атрибуты скорости
        smoothlyUpdateAttribute(player, Attributes.MOVEMENT_SPEED, SPEED_MODIFIER_UUID, "speed",
            useNightModifiers ? playerMods.getNightSpeedAddition() : playerMods.getSpeedAddition(),
            useNightModifiers ? playerMods.getNightSpeedMultiplier() : playerMods.getSpeedMultiplier(),
            1.0);
        
        // Атрибуты сопротивления отбрасыванию
        smoothlyUpdateAttribute(player, Attributes.KNOCKBACK_RESISTANCE, KNOCKBACK_RESISTANCE_UUID, "knockback_resistance",
            useNightModifiers ? playerMods.getNightKnockbackResistanceAddition() : playerMods.getKnockbackResistanceAddition(),
            useNightModifiers ? playerMods.getNightKnockbackResistanceMultiplier() : playerMods.getKnockbackResistanceMultiplier(),
            1.0);
        
        // Атрибуты отбрасывания при атаке
        smoothlyUpdateAttribute(player, Attributes.ATTACK_KNOCKBACK, ATTACK_KNOCKBACK_UUID, "attack_knockback",
            useNightModifiers ? playerMods.getNightAttackKnockbackAddition() : playerMods.getAttackKnockbackAddition(),
            useNightModifiers ? playerMods.getNightAttackKnockbackMultiplier() : playerMods.getAttackKnockbackMultiplier(),
            1.0);
        
        // Атрибуты скорости атаки
        smoothlyUpdateAttribute(player, Attributes.ATTACK_SPEED, ATTACK_SPEED_UUID, "attack_speed",
            useNightModifiers ? playerMods.getNightAttackSpeedAddition() : playerMods.getAttackSpeedAddition(),
            useNightModifiers ? playerMods.getNightAttackSpeedMultiplier() : playerMods.getAttackSpeedMultiplier(),
            1.0);
        
        // Атрибуты дальности следования
        smoothlyUpdateAttribute(player, Attributes.FOLLOW_RANGE, FOLLOW_RANGE_UUID, "follow_range",
            useNightModifiers ? playerMods.getNightFollowRangeAddition() : playerMods.getFollowRangeAddition(),
            useNightModifiers ? playerMods.getNightFollowRangeMultiplier() : playerMods.getFollowRangeMultiplier(),
            1.0);
        
        // Атрибуты скорости полета
        smoothlyUpdateAttribute(player, Attributes.FLYING_SPEED, FLYING_SPEED_UUID, "flying_speed",
            useNightModifiers ? playerMods.getNightFlyingSpeedAddition() : playerMods.getFlyingSpeedAddition(),
            useNightModifiers ? playerMods.getNightFlyingSpeedMultiplier() : playerMods.getFlyingSpeedMultiplier(),
            1.0);
        
        // Атрибуты прочности брони
        smoothlyUpdateAttribute(player, Attributes.ARMOR_TOUGHNESS, ARMOR_TOUGHNESS_UUID, "armor_toughness",
            useNightModifiers ? (playerMods.getNightArmorToughnessAddition() != 0 ? playerMods.getNightArmorToughnessAddition() : 0.0) : 
                               (playerMods.getArmorToughnessAddition() != 0 ? playerMods.getArmorToughnessAddition() : 0.0),
            useNightModifiers ? (playerMods.getNightArmorToughnessMultiplier() != 0 ? playerMods.getNightArmorToughnessMultiplier() : 1.0) : 
                               (playerMods.getArmorToughnessMultiplier() != 0 ? playerMods.getArmorToughnessMultiplier() : 1.0),
            1.0);
        
        // Атрибуты удачи
        smoothlyUpdateAttribute(player, Attributes.LUCK, LUCK_UUID, "luck",
            useNightModifiers ? (playerMods.getNightLuckAddition() != 0 ? playerMods.getNightLuckAddition() : 0.0) :
                               (playerMods.getLuckAddition() != 0 ? playerMods.getLuckAddition() : 0.0),
            useNightModifiers ? (playerMods.getNightLuckMultiplier() != 0 ? playerMods.getNightLuckMultiplier() : 1.0) :
                               (playerMods.getLuckMultiplier() != 0 ? playerMods.getLuckMultiplier() : 1.0),
            1.0);
        
        // Обработка гравитации для игрока
        try {
            // Получаем атрибут гравитации из ForgeMod
            Class<?> forgeModClass = Class.forName("net.minecraftforge.common.ForgeMod");
            java.lang.reflect.Field gravityField = forgeModClass.getDeclaredField("ENTITY_GRAVITY");
            gravityField.setAccessible(true);
            Object gravitySupplier = gravityField.get(null);
            
            // Получаем атрибут гравитации через Supplier
            net.minecraft.world.entity.ai.attributes.Attribute gravityAttribute = null;
            if (gravitySupplier instanceof java.util.function.Supplier<?>) {
                gravityAttribute = (net.minecraft.world.entity.ai.attributes.Attribute) 
                    ((java.util.function.Supplier<?>) gravitySupplier).get();
            }
            
            if (gravityAttribute != null) {
                // Применяем множитель гравитации
                smoothlyUpdateAttribute(player, gravityAttribute, GRAVITY_UUID, "gravity",
                    0.0, // Не используем сложение для гравитации, только множитель
                    playerMods.getGravityMultiplier(), // Используем множитель гравитации из конфигурации
                    1.0);
                
            }
        } catch (Exception e) {
            LOGGER.error("Error applying gravity modifier for player", e);
        }
        
        // Восстанавливаем здоровье игрока в соответствии с сохраненным процентом
        player.setHealth(player.getMaxHealth() * healthPercent);
        
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Player health restored to {}% ({}/{})",
                Math.round(healthPercent * 100),
                String.format("%.1f", player.getHealth()),
                String.format("%.1f", player.getMaxHealth()));
        }
    }
    
    /**
     * Плавно обновляет атрибут, сохраняя модификаторы от других модов
     */
    private static void smoothlyUpdateAttribute(LivingEntity entity, 
                                                net.minecraft.world.entity.ai.attributes.Attribute attribute, 
                                                UUID modifierUuid, 
                                                String name, 
                                                double addition, 
                                                double multiplier, 
                                                double difficultyMultiplier) {
        AttributeInstance attrInstance = entity.getAttribute(attribute);
        if (attrInstance != null) {
            double baseValue = attrInstance.getBaseValue();
            
            // Расчет нового значения с применением всех множителей
            double newValue = (baseValue + addition) * multiplier * difficultyMultiplier;
            double changeAmount = newValue - baseValue;
            
            // Проверяем, достаточно ли значимое изменение
            if (Math.abs(changeAmount) > 0.001) {
                // Проверяем, существует ли уже наш модификатор
                AttributeModifier existingModifier = attrInstance.getModifier(modifierUuid);
                if (existingModifier != null) {
                    // Удаляем существующий модификатор только если значение отличается
                    if (Math.abs(existingModifier.getAmount() - changeAmount) > 0.001) {
                        attrInstance.removeModifier(modifierUuid);
                        attrInstance.addPermanentModifier(new AttributeModifier(modifierUuid, "mobscaler_" + name, changeAmount, AttributeModifier.Operation.ADDITION));
                        
                        if (LOGGER.isDebugEnabled()) {
                            LOGGER.debug("Updated modifier for {}: {} = {}", 
                                entity instanceof Player ? ((Player)entity).getName().getString() : entity.getType().getDescriptionId(),
                                attribute.getDescriptionId(), 
                                String.format("%.2f", changeAmount));
                        }
                    }
                } else {
                    // Если модификатора нет, добавляем новый
                    attrInstance.addPermanentModifier(new AttributeModifier(modifierUuid, "mobscaler_" + name, changeAmount, AttributeModifier.Operation.ADDITION));
                    
                    if (LOGGER.isDebugEnabled()) {
                        LOGGER.debug("Added new modifier for {}: {} = {}", 
                            entity instanceof Player ? ((Player)entity).getName().getString() : entity.getType().getDescriptionId(),
                            attribute.getDescriptionId(), 
                            String.format("%.2f", changeAmount));
                    }
                }
            } else if (attrInstance.getModifier(modifierUuid) != null) {
                // Если изменение незначительное, но модификатор существует, удаляем его
                attrInstance.removeModifier(modifierUuid);
                
                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug("Removed insignificant modifier for {}: {}", 
                        entity instanceof Player ? ((Player)entity).getName().getString() : entity.getType().getDescriptionId(),
                        attribute.getDescriptionId());
                }
            }
        }
    }

    public static void handleMobModifiers(LivingEntity entity, Level world, String dimKey, boolean isNight, double healthMultiplier, double damageMultiplier) {
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

        
        // Проверяем все атрибуты перед удалением модификаторов
        logAllAttributes(entity, "BEFORE REMOVING MODIFIERS");

        // Всегда удаляем все существующие модификаторы перед применением новых
        removeAllModifiers(entity);
        
        // Проверяем все атрибуты после удаления модификаторов
        logAllAttributes(entity, "AFTER REMOVING MODIFIERS");

        // Проверяем блокировку сущности
        boolean isCave = entity.getY() <= dimConfig.getCaveHeight();
        if (isEntityBlocked(dimConfig, entityId, isNight, isCave)) {
            LOGGER.debug("Entity {} is blocked in dimension {}", entityIdStr, dimKey);
            return;
        }

        // Проверяем наличие индивидуальных настроек для моба
        IndividualMobConfig mobConfig = IndividualMobManager.getIndividualMobConfig(entityIdStr);
        if(LOGGER.isDebugEnabled()){
            LOGGER.debug("Individual config for entity {}: {}", entityIdStr, mobConfig != null ? "FOUND" : "NOT FOUND");
        }
        
        if (mobConfig != null) {
            if(LOGGER.isDebugEnabled()){
                LOGGER.debug("Found individual config for entity: {} in dimension: {}", entityIdStr, dimKey);
            }
            // Применяем индивидуальные модификаторы
            if(LOGGER.isDebugEnabled()){
                LOGGER.debug("Applying individual modifiers for entity: {} in dimension: {}", entityIdStr, dimKey);
            }
            IndividualMobManager.applyModifiers(entity, healthMultiplier, damageMultiplier);
        } else {
            // Проверяем наличие настроек мода
            IndividualMobAttributes modConfig = IndividualMobManager.getModConfig(modId);
            if(LOGGER.isDebugEnabled()){
                LOGGER.debug("Mod config for {}: {}", modId, modConfig != null ? "FOUND" : "NOT FOUND");
            }
            
            if (modConfig != null) {
                if(LOGGER.isDebugEnabled()){
                    LOGGER.debug("Found mod config for: {} in dimension: {}", modId, dimKey);
                }
                // Применяем модификаторы мода
                if(LOGGER.isDebugEnabled()){
                    LOGGER.debug("Applying mod modifiers for: {} in dimension: {}", modId, dimKey);
                }
                IndividualMobManager.applyModifiers(entity, healthMultiplier, damageMultiplier);
            } else {
                // Если нет ни индивидуальных настроек, ни настроек мода, применяем стандартные модификаторы
                if(LOGGER.isDebugEnabled()){    
                    LOGGER.debug("No individual or mod config found, applying standard modifiers for entity: {} in dimension: {}", entityIdStr, dimKey);
                }
                applyStandardModifiers(entity, dimConfig, isNight, healthMultiplier, damageMultiplier);
            }
        }

        // Проверяем все атрибуты после применения модификаторов
        logAllAttributes(entity, "AFTER APPLYING MODIFIERS");

        // Устанавливаем максимальное здоровье после применения всех модификаторов
        AttributeInstance healthAttr = entity.getAttribute(Attributes.MAX_HEALTH);
        if (healthAttr != null) {
            float maxHealth = entity.getMaxHealth();
            entity.setHealth(maxHealth);
        }
    }

    private static void applyHealthModifier(LivingEntity entity, double addition, double multiplier, double difficultyMultiplier) {
        AttributeInstance attr = entity.getAttribute(Attributes.MAX_HEALTH);
        if (attr != null) {
            double base = attr.getBaseValue();
            // Используем общую формулу: (base + addition) * multiplier * difficultyMultiplier
            double newMax = (base + addition) * multiplier * difficultyMultiplier;
            
            
            if (attr.getModifier(HEALTH_MODIFIER_UUID) != null) {
                attr.removeModifier(HEALTH_MODIFIER_UUID);
            }
            attr.addPermanentModifier(createModifier(HEALTH_MODIFIER_UUID, "health", newMax - base));
        }
    }
    
    private static void applyArmorModifier(LivingEntity entity, double addition, double multiplier, double difficultyMultiplier) {
        AttributeInstance attr = entity.getAttribute(Attributes.ARMOR);
        if (attr != null) {
            double base = attr.getBaseValue();
            double newValue = (base + addition) * multiplier * difficultyMultiplier;
            if (attr.getModifier(ARMOR_MODIFIER_UUID) != null) {
                attr.removeModifier(ARMOR_MODIFIER_UUID);
            }
            attr.addPermanentModifier(createModifier(ARMOR_MODIFIER_UUID, "armor", newValue - base));
        }
    }
    
    private static void applyArmorToughnessModifier(LivingEntity entity, double addition, double multiplier, double difficultyMultiplier) {
        AttributeInstance attr = entity.getAttribute(Attributes.ARMOR_TOUGHNESS);
        if (attr != null) {
            double base = attr.getBaseValue();
            double newValue = (base + addition) * multiplier * difficultyMultiplier;
            if (attr.getModifier(ARMOR_TOUGHNESS_UUID) != null) {
                attr.removeModifier(ARMOR_TOUGHNESS_UUID);
            }
            attr.addPermanentModifier(createModifier(ARMOR_TOUGHNESS_UUID, "armor_toughness", newValue - base));
        }
    }
    
    private static void applyGravityModifier(LivingEntity entity, double multiplier) {
        // Получаем атрибут гравитации из ForgeMod
        try {
            Class<?> forgeModClass = Class.forName("net.minecraftforge.common.ForgeMod");
            java.lang.reflect.Field gravityField = forgeModClass.getDeclaredField("ENTITY_GRAVITY");
            gravityField.setAccessible(true);
            Object gravitySupplier = gravityField.get(null);
            
            if (gravitySupplier instanceof java.util.function.Supplier<?>) {
                net.minecraft.world.entity.ai.attributes.Attribute gravityAttribute = 
                    (net.minecraft.world.entity.ai.attributes.Attribute) ((java.util.function.Supplier<?>) gravitySupplier).get();
                if (gravityAttribute != null) {
                    AttributeInstance attr = entity.getAttribute(gravityAttribute);
                    if (attr != null) {
                        double base = attr.getBaseValue();
                        double newValue = base * multiplier;
                        double difference = newValue - base;
                        
                        if (attr.getModifier(GRAVITY_UUID) != null) {
                            attr.removeModifier(GRAVITY_UUID);
                        }
                        
                        AttributeModifier modifier = createModifier(GRAVITY_UUID, "gravity", difference);
                        attr.addPermanentModifier(modifier);
                        
                        if(LOGGER.isDebugEnabled()){
                            LOGGER.debug("Applied gravity modifier to {}: base={}, multiplier={}, new={}",
                                entity.getType().getDescriptionId(), base, multiplier, newValue);
                        }
                    }
                }
            }
        } catch (Exception e) {
            LOGGER.error("Error applying gravity modifier", e);
        }
    }
    
    private static void applyLuckModifier(LivingEntity entity, double addition, double multiplier, double difficultyMultiplier) {
        AttributeInstance attr = entity.getAttribute(Attributes.LUCK);
        if (attr != null) {
            double base = attr.getBaseValue();
            double newValue = (base + addition) * multiplier * difficultyMultiplier;
            if (attr.getModifier(LUCK_UUID) != null) {
                attr.removeModifier(LUCK_UUID);
            }
            attr.addPermanentModifier(createModifier(LUCK_UUID, "luck", newValue - base));
        }
    }
    
    private static void applyDamageModifier(LivingEntity entity, double addition, double multiplier, double difficultyMultiplier) {
        AttributeInstance attr = entity.getAttribute(Attributes.ATTACK_DAMAGE);
        if (attr != null) {
            double base = attr.getBaseValue();
            double newValue = (base + addition) * multiplier * difficultyMultiplier;
            if (attr.getModifier(DAMAGE_MODIFIER_UUID) != null) {
                attr.removeModifier(DAMAGE_MODIFIER_UUID);
            }
            attr.addPermanentModifier(createModifier(DAMAGE_MODIFIER_UUID, "damage", newValue - base));
        }
    }

    private static void applySpeedModifier(LivingEntity entity, double addition, double multiplier, double difficultyMultiplier) {
        AttributeInstance attr = entity.getAttribute(Attributes.MOVEMENT_SPEED);
        if (attr != null) {
            double base = attr.getBaseValue();
            double newValue = (base + addition) * multiplier * difficultyMultiplier;
            if (attr.getModifier(SPEED_MODIFIER_UUID) != null) {
                attr.removeModifier(SPEED_MODIFIER_UUID);
            }
            attr.addPermanentModifier(createModifier(SPEED_MODIFIER_UUID, "speed", newValue - base));
        }
    }

    private static void applyKnockbackResistanceModifier(LivingEntity entity, double addition, double multiplier, double difficultyMultiplier) {
        AttributeInstance attr = entity.getAttribute(Attributes.KNOCKBACK_RESISTANCE);
        if (attr != null) {
            double base = attr.getBaseValue();
            double newValue = (base + addition) * multiplier * difficultyMultiplier;
            if (attr.getModifier(KNOCKBACK_RESISTANCE_UUID) != null) {
                attr.removeModifier(KNOCKBACK_RESISTANCE_UUID);
            }
            attr.addPermanentModifier(createModifier(KNOCKBACK_RESISTANCE_UUID, "knockback_resistance", newValue - base));
        }
    }

    private static void applyAttackKnockbackModifier(LivingEntity entity, double addition, double multiplier, double difficultyMultiplier) {
        AttributeInstance attr = entity.getAttribute(Attributes.ATTACK_KNOCKBACK);
        if (attr != null) {
            double base = attr.getBaseValue();
            double newValue = (base + addition) * multiplier * difficultyMultiplier;
            if (attr.getModifier(ATTACK_KNOCKBACK_UUID) != null) {
                attr.removeModifier(ATTACK_KNOCKBACK_UUID);
            }
            attr.addPermanentModifier(createModifier(ATTACK_KNOCKBACK_UUID, "attack_knockback", newValue - base));
        }
    }

    private static void applyAttackSpeedModifier(LivingEntity entity, double addition, double multiplier, double difficultyMultiplier) {
        AttributeInstance attr = entity.getAttribute(Attributes.ATTACK_SPEED);
        if (attr != null) {
            double base = attr.getBaseValue();
            double newValue = (base + addition) * multiplier * difficultyMultiplier;
            if (attr.getModifier(ATTACK_SPEED_UUID) != null) {
                attr.removeModifier(ATTACK_SPEED_UUID);
            }
            attr.addPermanentModifier(createModifier(ATTACK_SPEED_UUID, "attack_speed", newValue - base));
        }
    }

    private static void applyFollowRangeModifier(LivingEntity entity, double addition, double multiplier, double difficultyMultiplier) {
        AttributeInstance attr = entity.getAttribute(Attributes.FOLLOW_RANGE);
        if (attr != null) {
            double base = attr.getBaseValue();
            double newValue = (base + addition) * multiplier * difficultyMultiplier;
            if (attr.getModifier(FOLLOW_RANGE_UUID) != null) {
                attr.removeModifier(FOLLOW_RANGE_UUID);
            }
            attr.addPermanentModifier(createModifier(FOLLOW_RANGE_UUID, "follow_range", newValue - base));
        }
    }

    private static void applyFlyingSpeedModifier(LivingEntity entity, double addition, double multiplier, double difficultyMultiplier) {
        AttributeInstance attr = entity.getAttribute(Attributes.FLYING_SPEED);
        if (attr != null) {
            double base = attr.getBaseValue();
            double newValue = (base + addition) * multiplier * difficultyMultiplier;
            if (attr.getModifier(FLYING_SPEED_UUID) != null) {
                attr.removeModifier(FLYING_SPEED_UUID);
            }
            attr.addPermanentModifier(createModifier(FLYING_SPEED_UUID, "flying_speed", newValue - base));
        }
    }
    
    private static void applyReachDistanceModifier(LivingEntity entity, double addition, double multiplier, double difficultyMultiplier) {
        try {
            Class<?> forgeModClass = Class.forName("net.minecraftforge.common.ForgeMod");
            java.lang.reflect.Field reachDistanceField = forgeModClass.getDeclaredField("REACH_DISTANCE");
            reachDistanceField.setAccessible(true);
            Object reachDistanceSupplier = reachDistanceField.get(null);
            
            if (reachDistanceSupplier instanceof java.util.function.Supplier<?>) {
                net.minecraft.world.entity.ai.attributes.Attribute reachDistanceAttribute = 
                    (net.minecraft.world.entity.ai.attributes.Attribute) ((java.util.function.Supplier<?>) reachDistanceSupplier).get();
                if (reachDistanceAttribute != null) {
                    AttributeInstance attr = entity.getAttribute(reachDistanceAttribute);
                    if (attr != null) {
                        double base = attr.getBaseValue();
                        double newValue = (base + addition) * multiplier * difficultyMultiplier;
                        if (attr.getModifier(REACH_DISTANCE_UUID) != null) {
                            attr.removeModifier(REACH_DISTANCE_UUID);
                        }
                        attr.addPermanentModifier(createModifier(REACH_DISTANCE_UUID, "reach_distance", newValue - base));
                    }
                }
            }
        } catch (Exception e) {
            LOGGER.error("Error applying reach distance modifier", e);
        }
    }
    
    private static void applySwimSpeedModifier(LivingEntity entity, double addition, double multiplier, double difficultyMultiplier) {
        try {
            Class<?> forgeModClass = Class.forName("net.minecraftforge.common.ForgeMod");
            java.lang.reflect.Field swimSpeedField = forgeModClass.getDeclaredField("SWIM_SPEED");
            swimSpeedField.setAccessible(true);
            Object swimSpeedSupplier = swimSpeedField.get(null);
            
            if (swimSpeedSupplier instanceof java.util.function.Supplier<?>) {
                net.minecraft.world.entity.ai.attributes.Attribute swimSpeedAttribute = 
                    (net.minecraft.world.entity.ai.attributes.Attribute) ((java.util.function.Supplier<?>) swimSpeedSupplier).get();
                if (swimSpeedAttribute != null) {
                    AttributeInstance attr = entity.getAttribute(swimSpeedAttribute);
                    if (attr != null) {
                        double base = attr.getBaseValue();
                        double newValue = (base + addition) * multiplier * difficultyMultiplier;
                        if (attr.getModifier(SWIM_SPEED_UUID) != null) {
                            attr.removeModifier(SWIM_SPEED_UUID);
                        }
                        attr.addPermanentModifier(createModifier(SWIM_SPEED_UUID, "swim_speed", newValue - base));
                    }
                }
            }
        } catch (Exception e) {
            LOGGER.error("Error applying swim speed modifier", e);
        }
    }
    
    private static AttributeModifier createModifier(UUID uuid, String name, double value) {
        return new AttributeModifier(uuid, "mobscaler_" + name, value, AttributeModifier.Operation.ADDITION);
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
        LOGGER.debug("Adding mod config for: {} with attributes: enableNightScaling={}, enableCaveScaling={}, healthMultiplier={}, damageMultiplier={}", 
            modId,
            config.getEnableNightScaling(),
            config.getEnableCaveScaling(),
            config.getHealthMultiplier(),
            config.getDamageMultiplier()
        );
        IndividualMobManager.addModConfig(modId, config);
    }

    public static void removeModConfig(String modId) {
        LOGGER.debug("Removing mod config for: {}", modId);
        IndividualMobManager.removeModConfig(modId);
    }

    public static void removeAllModifiersForEntityType(String entityId) {
        if (entityId != null) {
            LOGGER.debug("Removing all modifiers for entity type: {}", entityId);
            String[] parts = entityId.split(":");
            if (parts.length == 2) {
                String namespace = parts[0];
                String path = parts[1];
                ResourceLocation entityType = new ResourceLocation(namespace, path);
                
                // Получаем все сущности в мире
                for (Level level : ServerLifecycleHooks.getCurrentServer().getAllLevels()) {
                    AABB worldBounds = new AABB(
                        level.getWorldBorder().getMinX(), Double.NEGATIVE_INFINITY, level.getWorldBorder().getMinZ(),
                        level.getWorldBorder().getMaxX(), Double.POSITIVE_INFINITY, level.getWorldBorder().getMaxZ()
                    );
                    
                    int count = 0;
                    for (LivingEntity entity : level.getEntitiesOfClass(LivingEntity.class, worldBounds)) {
                        if (EntityType.getKey(entity.getType()).equals(entityType)) {
                            count++;
                            removeAllModifiers(entity);
                            LOGGER.debug("Removed all modifiers from entity #{} of type: {} in world: {}", count, entityId, level.dimension().location());
                        }
                    }
                    
                    LOGGER.debug("Found and reset {} entities of type {} in world {}", count, entityId, level.dimension().location());
                }
            }
        }
    }

    private static void removeAllModifiers(LivingEntity entity) {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Removing modifiers for: {}", entity.getType().getDescriptionId());
        }
        
        // Сохраняем базовые значения атрибутов перед удалением модификаторов
        Map<net.minecraft.world.entity.ai.attributes.Attribute, Double> baseValues = new HashMap<>();
        
        // Сначала собираем все базовые значения
        net.minecraft.world.entity.ai.attributes.Attribute[] attributes = {
            Attributes.MAX_HEALTH,
            Attributes.ARMOR,
            Attributes.ATTACK_DAMAGE,
            Attributes.MOVEMENT_SPEED,
            Attributes.KNOCKBACK_RESISTANCE,
            Attributes.ATTACK_KNOCKBACK,
            Attributes.ATTACK_SPEED,
            Attributes.FOLLOW_RANGE,
            Attributes.FLYING_SPEED,
            Attributes.ARMOR_TOUGHNESS,
        };
        
        for (net.minecraft.world.entity.ai.attributes.Attribute attribute : attributes) {
            AttributeInstance attr = entity.getAttribute(attribute);
            if (attr != null) {
                baseValues.put(attribute, attr.getBaseValue());
            }
        }
        
        // Удаляем модификаторы для стандартных атрибутов
        removeModifier(entity, Attributes.MAX_HEALTH, HEALTH_MODIFIER_UUID);
        removeModifier(entity, Attributes.ARMOR, ARMOR_MODIFIER_UUID);
        removeModifier(entity, Attributes.ATTACK_DAMAGE, DAMAGE_MODIFIER_UUID);
        removeModifier(entity, Attributes.MOVEMENT_SPEED, SPEED_MODIFIER_UUID);
        removeModifier(entity, Attributes.KNOCKBACK_RESISTANCE, KNOCKBACK_RESISTANCE_UUID);
        removeModifier(entity, Attributes.ATTACK_KNOCKBACK, ATTACK_KNOCKBACK_UUID);
        removeModifier(entity, Attributes.ATTACK_SPEED, ATTACK_SPEED_UUID);
        removeModifier(entity, Attributes.FOLLOW_RANGE, FOLLOW_RANGE_UUID);
        removeModifier(entity, Attributes.FLYING_SPEED, FLYING_SPEED_UUID);
        removeModifier(entity, Attributes.ARMOR_TOUGHNESS, ARMOR_TOUGHNESS_UUID);
        
        // Удаляем модификаторы гравитации через рефлексию
        try {
            Class<?> forgeModClass = Class.forName("net.minecraftforge.common.ForgeMod");
            
            // Гравитация
            java.lang.reflect.Field gravityField = forgeModClass.getDeclaredField("ENTITY_GRAVITY");
            gravityField.setAccessible(true);
            Object gravitySupplier = gravityField.get(null);
            
            if (gravitySupplier instanceof java.util.function.Supplier<?>) {
                net.minecraft.world.entity.ai.attributes.Attribute gravityAttribute = 
                    (net.minecraft.world.entity.ai.attributes.Attribute) ((java.util.function.Supplier<?>) gravitySupplier).get();
                if (gravityAttribute != null) {
                    removeModifier(entity, gravityAttribute, GRAVITY_UUID);
                }
            }
            
            
            // Дальность взаимодействия
            java.lang.reflect.Field reachDistanceField = forgeModClass.getDeclaredField("REACH_DISTANCE");
            reachDistanceField.setAccessible(true);
            Object reachDistanceSupplier = reachDistanceField.get(null);
            
            if (reachDistanceSupplier instanceof java.util.function.Supplier<?>) {
                net.minecraft.world.entity.ai.attributes.Attribute reachDistanceAttribute = 
                    (net.minecraft.world.entity.ai.attributes.Attribute) ((java.util.function.Supplier<?>) reachDistanceSupplier).get();
                if (reachDistanceAttribute != null) {
                    removeModifier(entity, reachDistanceAttribute, REACH_DISTANCE_UUID);
                }
            }
            
            // Скорость плавания
            java.lang.reflect.Field swimSpeedField = forgeModClass.getDeclaredField("SWIM_SPEED");
            swimSpeedField.setAccessible(true);
            Object swimSpeedSupplier = swimSpeedField.get(null);
            
            if (swimSpeedSupplier instanceof java.util.function.Supplier<?>) {
                net.minecraft.world.entity.ai.attributes.Attribute swimSpeedAttribute = 
                    (net.minecraft.world.entity.ai.attributes.Attribute) ((java.util.function.Supplier<?>) swimSpeedSupplier).get();
                if (swimSpeedAttribute != null) {
                    removeModifier(entity, swimSpeedAttribute, SWIM_SPEED_UUID);
                }
            }
            
        } catch (Exception e) {
            LOGGER.error("Error removing attribute modifiers through reflection", e);
        }
        
        // Удаляем модификаторы по UUID из IndividualMobManager
        removeModifier(entity, Attributes.MAX_HEALTH, UUID.fromString("d5d0d878-b3c2-4194-a263-6516c85b1071"));
        removeModifier(entity, Attributes.ARMOR, UUID.fromString("d5d0d878-b3c2-4194-a263-6516c85b1072"));
        removeModifier(entity, Attributes.ATTACK_DAMAGE, UUID.fromString("d5d0d878-b3c2-4194-a263-6516c85b1073"));
        removeModifier(entity, Attributes.MOVEMENT_SPEED, UUID.fromString("d5d0d878-b3c2-4194-a263-6516c85b1074"));
        removeModifier(entity, Attributes.KNOCKBACK_RESISTANCE, UUID.fromString("d5d0d878-b3c2-4194-a263-6516c85b1075"));
        removeModifier(entity, Attributes.ATTACK_KNOCKBACK, UUID.fromString("d5d0d878-b3c2-4194-a263-6516c85b1076"));
        removeModifier(entity, Attributes.ATTACK_SPEED, UUID.fromString("d5d0d878-b3c2-4194-a263-6516c85b1077"));
        removeModifier(entity, Attributes.FOLLOW_RANGE, UUID.fromString("d5d0d878-b3c2-4194-a263-6516c85b1078"));
        removeModifier(entity, Attributes.FLYING_SPEED, UUID.fromString("d5d0d878-b3c2-4194-a263-6516c85b1079"));
        
        // Также удаляем модификаторы для новых атрибутов из IndividualMobManager
        try {
            Class<?> forgeModClass = Class.forName("net.minecraftforge.common.ForgeMod");
            
            // Скорость плавания
            java.lang.reflect.Field swimSpeedField = forgeModClass.getDeclaredField("SWIM_SPEED");
            swimSpeedField.setAccessible(true);
            Object swimSpeedSupplier = swimSpeedField.get(null);
            
            if (swimSpeedSupplier instanceof java.util.function.Supplier<?>) {
                net.minecraft.world.entity.ai.attributes.Attribute swimSpeedAttribute = 
                    (net.minecraft.world.entity.ai.attributes.Attribute) ((java.util.function.Supplier<?>) swimSpeedSupplier).get();
                if (swimSpeedAttribute != null) {
                    removeModifier(entity, swimSpeedAttribute, UUID.fromString("d5d0d878-b3c2-4194-a263-6516c85b1080"));
                }
            }
            
            // Дальность взаимодействия
            java.lang.reflect.Field reachDistanceField = forgeModClass.getDeclaredField("REACH_DISTANCE");
            reachDistanceField.setAccessible(true);
            Object reachDistanceSupplier = reachDistanceField.get(null);
            
            if (reachDistanceSupplier instanceof java.util.function.Supplier<?>) {
                net.minecraft.world.entity.ai.attributes.Attribute reachDistanceAttribute = 
                    (net.minecraft.world.entity.ai.attributes.Attribute) ((java.util.function.Supplier<?>) reachDistanceSupplier).get();
                if (reachDistanceAttribute != null) {
                    removeModifier(entity, reachDistanceAttribute, UUID.fromString("d5d0d878-b3c2-4194-a263-6516c85b1082"));
                }
            }
        } catch (Exception e) {
            LOGGER.error("Error removing IndividualMobManager modifiers for new attributes", e);
        }
        
        // Удаляем все модификаторы с именем, содержащим "mobscaler_"
        for (net.minecraft.world.entity.ai.attributes.Attribute attribute : attributes) {
            AttributeInstance attr = entity.getAttribute(attribute);
            if (attr != null) {
                java.util.Collection<AttributeModifier> modifiers = new java.util.ArrayList<>(attr.getModifiers());
                for (AttributeModifier modifier : modifiers) {
                    if (modifier.getName().startsWith("mobscaler_")) {
                        attr.removeModifier(modifier.getId());
                    }
                }
            }
        }
        
        // Сбрасываем здоровье к базовому значению
        AttributeInstance healthAttr = entity.getAttribute(Attributes.MAX_HEALTH);
        if (healthAttr != null) {
            double baseValue = healthAttr.getBaseValue();
            entity.setHealth((float)baseValue);
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Reset health to: {}", String.format("%.1f", baseValue));
            }
        }
        
        // Сбрасываем урон к базовому значению через добавление временного модификатора компенсации
        AttributeInstance damageAttr = entity.getAttribute(Attributes.ATTACK_DAMAGE);
        if (damageAttr != null) {
            double baseValue = baseValues.get(Attributes.ATTACK_DAMAGE);
            double currentValue = damageAttr.getValue();
            
            // Если текущее значение атрибута отличается от базового после удаления модификаторов,
            // добавляем компенсирующий модификатор, чтобы вернуть урон к базовому значению
            if (Math.abs(currentValue - baseValue) > 0.001) {
                UUID tempUUID = UUID.randomUUID();
                double diff = baseValue - currentValue;
                AttributeModifier tempModifier = new AttributeModifier(
                    tempUUID, "mobscaler_reset_damage", diff, AttributeModifier.Operation.ADDITION
                );
                damageAttr.addTransientModifier(tempModifier);
                
                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug("Added compensation modifier to reset damage: base={}, current={}, diff={}", 
                        String.format("%.1f", baseValue), 
                        String.format("%.1f", currentValue), 
                        String.format("%.1f", diff)
                    );
                }
            }
        }
    }

    private static void removeModifier(LivingEntity entity, net.minecraft.world.entity.ai.attributes.Attribute attribute, UUID modifierId) {
        AttributeInstance attr = entity.getAttribute(attribute);
        if (attr != null && attr.getModifier(modifierId) != null) {
            attr.removeModifier(modifierId);
            if (attribute == Attributes.MAX_HEALTH) {
                double baseValue = attr.getBaseValue();
                entity.setHealth((float)baseValue);
            }
        }
    }

    public static void applyStandardModifiers(LivingEntity entity, DimensionConfig config, boolean isNight, double healthMultiplier, double damageMultiplier) {
        if (entity == null || config == null) {
            LOGGER.warn("Entity or config is null, cannot apply standard modifiers");
            return;
        }
        
        LOGGER.debug("Applying standard modifiers for entity: {} with config: {}", entity.getType(), config);
        
        boolean isCave = entity.getY() <= config.getCaveHeight();
        
        // Проверяем приоритеты: пещерные > ночные > обычные
        if (isCave && config.getEnableCaveScaling()) {
            LOGGER.debug("Applying cave modifiers for entity: {}", entity.getType());
            applyHealthModifier(entity, config.getCaveHealthAddition(), config.getCaveHealthMultiplier(), healthMultiplier);
            applyArmorModifier(entity, config.getCaveArmorAddition(), config.getCaveArmorMultiplier(), 1.0);
            applyArmorToughnessModifier(entity, config.getCaveArmorToughnessAddition(), config.getCaveArmorToughnessMultiplier(), 1.0);
            applyGravityModifier(entity, config.getCaveArmorToughnessMultiplier());
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
            applyReachDistanceModifier(entity, config.getCaveReachDistanceAddition(), config.getCaveReachDistanceMultiplier(), 1.0);
            applySwimSpeedModifier(entity, config.getCaveSwimSpeedAddition(), config.getCaveSwimSpeedMultiplier(), 1.0);
        } else if (isNight && config.getEnableNightScaling()) {
            LOGGER.debug("Applying night modifiers for entity: {}", entity.getType());
            applyHealthModifier(entity, config.getNightHealthAddition(), config.getNightHealthMultiplier(), healthMultiplier);
            applyArmorModifier(entity, config.getNightArmorAddition(), config.getNightArmorMultiplier(), 1.0);
            applyArmorToughnessModifier(entity, config.getNightArmorToughnessAddition(), config.getNightArmorToughnessMultiplier(), 1.0);
            applyGravityModifier(entity, config.getNightArmorToughnessMultiplier());
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
            applyReachDistanceModifier(entity, config.getNightReachDistanceAddition(), config.getNightReachDistanceMultiplier(), 1.0);
            applySwimSpeedModifier(entity, config.getNightSwimSpeedAddition(), config.getNightSwimSpeedMultiplier(), 1.0);
        } else {
            LOGGER.debug("Applying default modifiers for entity: {}", entity.getType());
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
            applyReachDistanceModifier(entity, config.getReachDistanceAddition(), config.getReachDistanceMultiplier(), 1.0);
            applySwimSpeedModifier(entity, config.getSwimSpeedAddition(), config.getSwimSpeedMultiplier(), 1.0);
        }

        // Устанавливаем максимальное здоровье после применения всех модификаторов
        AttributeInstance healthAttr = entity.getAttribute(Attributes.MAX_HEALTH);
        if (healthAttr != null) {
            float maxHealth = entity.getMaxHealth();
            entity.setHealth(maxHealth);
            LOGGER.debug("Setting entity health to maximum: {}", maxHealth);
        }
    }

    private static void logAllAttributes(LivingEntity entity, String stage) {
        if (!LOGGER.isDebugEnabled()) return;
        
        // Логируем только важные изменения
        LOGGER.debug("--- {} for entity: {} ---", stage, entity.getType());
        
        net.minecraft.world.entity.ai.attributes.Attribute[] attributes = {
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
        
        // Логируем стандартные атрибуты
        for (net.minecraft.world.entity.ai.attributes.Attribute attribute : attributes) {
            AttributeInstance attr = entity.getAttribute(attribute);
            if (attr != null && (attr.getModifiers().size() > 0 || attribute == Attributes.MAX_HEALTH)) {
                LOGGER.debug("Attribute {}: base={}, value={}", 
                    attribute.getDescriptionId(), 
                    String.format("%.1f", attr.getBaseValue()), 
                    String.format("%.1f", attr.getValue())
                );
                
                // Логируем модификаторы только если они действительно изменяют значение
                for (AttributeModifier modifier : attr.getModifiers()) {
                    if (Math.abs(modifier.getAmount()) > 0.001) {
                        LOGGER.debug("  Modifier: name={}, amount={}", 
                            modifier.getName(),
                            String.format("%.1f", modifier.getAmount())
                        );
                    }
                }
            }
        }
        
        // Логируем атрибут гравитации отдельно, если он доступен
        try {
            Class<?> forgeModClass = Class.forName("net.minecraftforge.common.ForgeMod");
            java.lang.reflect.Field gravityField = forgeModClass.getDeclaredField("ENTITY_GRAVITY");
            gravityField.setAccessible(true);
            Object gravitySupplier = gravityField.get(null);
            
            if (gravitySupplier instanceof java.util.function.Supplier<?>) {
                net.minecraft.world.entity.ai.attributes.Attribute gravityAttribute = 
                    (net.minecraft.world.entity.ai.attributes.Attribute) ((java.util.function.Supplier<?>) gravitySupplier).get();
                if (gravityAttribute != null) {
                    AttributeInstance attr = entity.getAttribute(gravityAttribute);
                    if (attr != null) {
                        LOGGER.debug("Gravity attribute: base={}, value={}", 
                            String.format("%.3f", attr.getBaseValue()), 
                            String.format("%.3f", attr.getValue())
                        );
                        
                        for (AttributeModifier modifier : attr.getModifiers()) {
                            if (Math.abs(modifier.getAmount()) > 0.0001) {
                                LOGGER.debug("  Modifier: name={}, amount={}", 
                                    modifier.getName(),
                                    String.format("%.3f", modifier.getAmount())
                                );
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            // Игнорируем ошибки при логировании
        }
    }

    /**
     * Обработчик события падения сущности.
     * Отменяет урон от падения, если множитель гравитации ниже определенного порога.
     */
    @SubscribeEvent
    public static void onEntityFall(LivingFallEvent event) {
        LivingEntity entity = event.getEntity();
        try {
            // Получаем текущее значение множителя гравитации
            Class<?> forgeModClass = Class.forName("net.minecraftforge.common.ForgeMod");
            java.lang.reflect.Field gravityField = forgeModClass.getDeclaredField("ENTITY_GRAVITY");
            gravityField.setAccessible(true);
            Object gravitySupplier = gravityField.get(null);
            
            if (gravitySupplier instanceof java.util.function.Supplier<?>) {
                net.minecraft.world.entity.ai.attributes.Attribute gravityAttribute = 
                    (net.minecraft.world.entity.ai.attributes.Attribute) ((java.util.function.Supplier<?>) gravitySupplier).get();
                if (gravityAttribute != null) {
                    AttributeInstance attr = entity.getAttribute(gravityAttribute);
                    if (attr != null) {
                        double baseGravity = attr.getBaseValue();
                        double currentGravity = attr.getValue();
                        double gravityRatio = currentGravity / baseGravity; // Текущий множитель гравитации
                        
                        // Если гравитация ниже порога, отменяем урон от падения
                        if (gravityRatio < NO_FALL_DAMAGE_GRAVITY_THRESHOLD) {
                            event.setCanceled(true);
                            LOGGER.debug("Canceled fall damage for entity {} due to low gravity: {}", 
                                entity.getType().getDescriptionId(), gravityRatio);
                        } else {
                            // Можно также масштабировать урон от падения в зависимости от гравитации
                            float originalDamage = event.getDamageMultiplier();
                            float scaledDamage = (float)(originalDamage * (gravityRatio / 1.0));
                            event.setDamageMultiplier(scaledDamage);
                            LOGGER.debug("Scaled fall damage for entity {} by gravity ratio: {} (original: {}, scaled: {})", 
                                entity.getType().getDescriptionId(), gravityRatio, originalDamage, scaledDamage);
                        }
                    }
                }
            }
        } catch (Exception e) {
            LOGGER.error("Error handling fall damage", e);
        }
    }
}
