package com.example.mobscaler.config;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.server.ServerLifecycleHooks;
import com.example.mobscaler.events.EntityHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.UUID;
import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.function.Supplier;
import java.lang.reflect.Field;

public class IndividualMobManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(IndividualMobManager.class);
    private static final Map<String, IndividualMobConfig> individualMobConfigs = new HashMap<>();
    private static final Map<String, IndividualMobAttributes> modConfigs = new HashMap<>();

    // UUIDs для модификаторов атрибутов
    private static final UUID HEALTH_MODIFIER_UUID = UUID.fromString("d5d0d878-b3c2-4194-a263-6516c85b1071");
    private static final UUID ARMOR_MODIFIER_UUID = UUID.fromString("d5d0d878-b3c2-4194-a263-6516c85b1072");
    private static final UUID DAMAGE_MODIFIER_UUID = UUID.fromString("d5d0d878-b3c2-4194-a263-6516c85b1073");
    private static final UUID SPEED_MODIFIER_UUID = UUID.fromString("d5d0d878-b3c2-4194-a263-6516c85b1074");
    private static final UUID KNOCKBACK_RESISTANCE_UUID = UUID.fromString("d5d0d878-b3c2-4194-a263-6516c85b1075");
    private static final UUID ATTACK_KNOCKBACK_UUID = UUID.fromString("d5d0d878-b3c2-4194-a263-6516c85b1076");
    private static final UUID ATTACK_SPEED_UUID = UUID.fromString("d5d0d878-b3c2-4194-a263-6516c85b1077");
    private static final UUID FOLLOW_RANGE_UUID = UUID.fromString("d5d0d878-b3c2-4194-a263-6516c85b1078");
    private static final UUID FLYING_SPEED_UUID = UUID.fromString("d5d0d878-b3c2-4194-a263-6516c85b1079");
    private static final UUID SWIM_SPEED_UUID = UUID.fromString("d5d0d878-b3c2-4194-a263-6516c85b1080");
    private static final UUID REACH_DISTANCE_UUID = UUID.fromString("d5d0d878-b3c2-4194-a263-6516c85b1082");
    
    public static void applyModifiers(LivingEntity entity, double healthMultiplier, double damageMultiplier) {
        ResourceLocation entityId = EntityType.getKey(entity.getType());
        String entityIdStr = entityId.toString();
        String modId = entityId.getNamespace();
        Level level = entity.level();
        
        
        // Проверяем наличие индивидуальных настроек
        IndividualMobConfig mobConfig = individualMobConfigs.get(entityIdStr);
        if (mobConfig != null) {
            if (!mobConfig.isBlacklisted()) {
                // Удаляем существующие модификаторы
                removeAllMobscalerModifiers(entity);
                
                // Применяем индивидуальные модификаторы
                applyDefaultModifiers(entity, mobConfig.getAttributes(), healthMultiplier, damageMultiplier);
                
                // Применяем ночные модификаторы для индивидуальных настроек
                boolean isNight = EntityHandler.isNight(level);
                boolean isCave = entity.getY() <= mobConfig.getAttributes().getCaveHeight();
                if (isNight && mobConfig.getAttributes().getEnableNightScaling()) {
                    applyNightModifiers(entity, mobConfig.getAttributes(), healthMultiplier, damageMultiplier);
                }
                
                // Применяем пещерные модификаторы для индивидуальных настроек
                if (isCave && mobConfig.getAttributes().getEnableCaveScaling()) {
                    applyCaveModifiers(entity, mobConfig.getAttributes(), healthMultiplier, damageMultiplier);
                }
                
            } else {
            }
        } else {
            // Проверяем наличие настроек мода
            IndividualMobAttributes modAttributes = modConfigs.get(modId);
            if (modAttributes != null) {
               
                
                // Удаляем существующие модификаторы
                removeAllMobscalerModifiers(entity);
                
                // Применяем стандартные модификаторы из настроек мода
                applyDefaultModifiers(entity, modAttributes, healthMultiplier, damageMultiplier);


                // Проверяем условия для ночного и пещерного режимов для настроек мода
                boolean isNight = EntityHandler.isNight(level);
                boolean isCave = entity.getY() <= modAttributes.getCaveHeight();
                
                

                // Применяем ночные модификаторы из настроек мода (если не в пещере)
                if (isNight && modAttributes.getEnableNightScaling()) {
                    applyNightModifiers(entity, modAttributes, healthMultiplier, damageMultiplier);
                } else if (isNight && !modAttributes.getEnableNightScaling()) {
                }
                
                // Применяем пещерные модификаторы из настроек мода (если в пещере)
                if (isCave && modAttributes.getEnableCaveScaling()) {
                    applyCaveModifiers(entity, modAttributes, healthMultiplier, damageMultiplier);
                } else if (isCave && !modAttributes.getEnableCaveScaling()) {
                }
            } else {
                // Если нет ни индивидуальных настроек, ни настроек мода, применяем базовые настройки измерения
                IndividualMobAttributes attributes = IndividualMobAttributes.getDefault();
                
                // Удаляем существующие модификаторы
                removeAllMobscalerModifiers(entity);
                
                // Применяем базовые настройки измерения
                applyDefaultModifiers(entity, attributes, healthMultiplier, damageMultiplier);


                // Проверяем условия для ночного и пещерного режимов для базовых настроек
                boolean isNight = EntityHandler.isNight(level);
                boolean isCave = entity.getY() <= attributes.getCaveHeight();
                

                // Применяем ночные модификаторы для базовых настроек (если не в пещере)
                if (isNight && attributes.getEnableNightScaling()) {
                    applyNightModifiers(entity, attributes, healthMultiplier, damageMultiplier);
                } else if (isNight && !attributes.getEnableNightScaling()) {
                }
                
                // Применяем пещерные модификаторы для базовых настроек (если в пещере)
                if (isCave && attributes.getEnableCaveScaling()) {
                    applyCaveModifiers(entity, attributes, healthMultiplier, damageMultiplier);
                } else if (isCave && !attributes.getEnableCaveScaling()) {
                }
            }
        }

        // Проверяем, что модификаторы были применены
        for (net.minecraft.world.entity.ai.attributes.Attribute attribute : new net.minecraft.world.entity.ai.attributes.Attribute[] {
            Attributes.MAX_HEALTH, Attributes.ARMOR, Attributes.ATTACK_DAMAGE,
            Attributes.MOVEMENT_SPEED, Attributes.KNOCKBACK_RESISTANCE,
            Attributes.ATTACK_KNOCKBACK, Attributes.ATTACK_SPEED,
            Attributes.FOLLOW_RANGE, Attributes.FLYING_SPEED
        }) {
            AttributeInstance attr = entity.getAttribute(attribute);
            if (attr != null) {
                java.util.Collection<AttributeModifier> appliedModifiers = attr.getModifiers();
                if (appliedModifiers.isEmpty()) {
                    LOGGER.warn("No modifiers found for attribute {} after application: {}", attribute, attr.getValue());
                } else {
                }
            }
        }
        
        // Проверяем модификаторы для новых атрибутов ForgeMod
        try {
            Class<?> forgeModClass = Class.forName("net.minecraftforge.common.ForgeMod");
            
            // Проверяем SWIM_SPEED
            checkAttributeModifiers(entity, forgeModClass, "SWIM_SPEED");
            
            // Проверяем REACH_DISTANCE
            checkAttributeModifiers(entity, forgeModClass, "REACH_DISTANCE");
            
        } catch (Exception e) {
            LOGGER.error("Error checking ForgeMod attribute modifiers: {}", e.getMessage());
        }
    }

    // Вспомогательный метод для проверки модификаторов атрибутов ForgeMod
    private static void checkAttributeModifiers(LivingEntity entity, Class<?> forgeModClass, String attributeName) {
        try {
            java.lang.reflect.Field field = forgeModClass.getDeclaredField(attributeName);
            field.setAccessible(true);
            Object supplier = field.get(null);
            
            if (supplier instanceof java.util.function.Supplier<?>) {
                net.minecraft.world.entity.ai.attributes.Attribute attribute = 
                    (net.minecraft.world.entity.ai.attributes.Attribute) ((java.util.function.Supplier<?>) supplier).get();
                if (attribute != null) {
                    AttributeInstance attr = entity.getAttribute(attribute);
                    if (attr != null) {
                        java.util.Collection<AttributeModifier> appliedModifiers = attr.getModifiers();
                        if (appliedModifiers.isEmpty()) {
                            LOGGER.warn("No modifiers found for ForgeMod attribute {} after application: {}", attributeName, attr.getValue());
                        } else {
                        }
                    }
                }
            }
        } catch (Exception e) {
            LOGGER.error("Error checking modifiers for ForgeMod attribute {}: {}", attributeName, e.getMessage());
        }
    }

    private static void removeAllMobscalerModifiers(LivingEntity entity) {
        
        // Список всех атрибутов для проверки
        List<net.minecraft.world.entity.ai.attributes.Attribute> attributesList = new ArrayList<>(Arrays.asList(
            Attributes.MAX_HEALTH,
            Attributes.ARMOR,
            Attributes.ATTACK_DAMAGE,
            Attributes.MOVEMENT_SPEED,
            Attributes.KNOCKBACK_RESISTANCE,
            Attributes.ATTACK_KNOCKBACK,
            Attributes.ATTACK_SPEED,
            Attributes.FOLLOW_RANGE,
            Attributes.FLYING_SPEED
        ));
        
        // Добавляем атрибуты из ForgeMod через рефлексию
        try {
            Class<?> forgeModClass = Class.forName("net.minecraftforge.common.ForgeMod");
            
            // Получаем SWIM_SPEED
            Field swimSpeedField = forgeModClass.getDeclaredField("SWIM_SPEED");
            if (swimSpeedField.getType().isAssignableFrom(Supplier.class)) {
                Object supplier = swimSpeedField.get(null);
                if (supplier instanceof Supplier) {
                    net.minecraft.world.entity.ai.attributes.Attribute swimAttribute = 
                        (net.minecraft.world.entity.ai.attributes.Attribute) ((Supplier<?>) supplier).get();
                    attributesList.add(swimAttribute);
                }
            }
            
            // Получаем REACH_DISTANCE
            Field reachDistanceField = forgeModClass.getDeclaredField("REACH_DISTANCE");
            if (reachDistanceField.getType().isAssignableFrom(Supplier.class)) {
                Object supplier = reachDistanceField.get(null);
                if (supplier instanceof Supplier) {
                    net.minecraft.world.entity.ai.attributes.Attribute reachAttribute = 
                        (net.minecraft.world.entity.ai.attributes.Attribute) ((Supplier<?>) supplier).get();
                    attributesList.add(reachAttribute);
                }
            }
            
        } catch (Exception e) {
            LOGGER.error("Error getting Forge attributes for remove modifiers: " + e.getMessage());
        }
        
        net.minecraft.world.entity.ai.attributes.Attribute[] attributes = 
            attributesList.toArray(new net.minecraft.world.entity.ai.attributes.Attribute[0]);

        // Список всех UUID модификаторов нашего мода
        UUID[] modifierUuids = {
            HEALTH_MODIFIER_UUID,
            ARMOR_MODIFIER_UUID,
            DAMAGE_MODIFIER_UUID,
            SPEED_MODIFIER_UUID,
            KNOCKBACK_RESISTANCE_UUID,
            ATTACK_KNOCKBACK_UUID,
            ATTACK_SPEED_UUID,
            FOLLOW_RANGE_UUID,
            FLYING_SPEED_UUID,
            SWIM_SPEED_UUID,
            REACH_DISTANCE_UUID
        };

        // Удаляем все модификаторы по UUID и имени
        for (net.minecraft.world.entity.ai.attributes.Attribute attribute : attributes) {
            AttributeInstance attr = entity.getAttribute(attribute);
            if (attr != null) {
                // Удаляем модификаторы по UUID
                for (UUID uuid : modifierUuids) {
                    AttributeModifier modifier = attr.getModifier(uuid);
                    if (modifier != null) {
                        attr.removeModifier(uuid);
                    }
                }
                
                // Удаляем модификаторы по имени - ИСПРАВЛЕНИЕ: собираем ID и затем удаляем
                java.util.Collection<AttributeModifier> modifiers = attr.getModifiers();
                java.util.List<UUID> toRemove = new java.util.ArrayList<>();
                
                // Сначала собираем UUID всех модификаторов с именем, начинающимся с "mobscaler_"
                for (AttributeModifier modifier : modifiers) {
                    if (modifier.getName().startsWith("mobscaler_")) {
                        toRemove.add(modifier.getId());
                    }
                }
                
                // Теперь удаляем модификаторы по собранным UUID
                for (UUID id : toRemove) {
                    attr.removeModifier(id);
                }
                
                // Проверяем, что все модификаторы удалены
                java.util.Collection<AttributeModifier> remainingModifiers = attr.getModifiers();
                boolean stillHasMobscalerModifiers = false;
                for (AttributeModifier modifier : remainingModifiers) {
                    if (modifier.getName().startsWith("mobscaler_")) {
                        stillHasMobscalerModifiers = true;
                        LOGGER.warn("Failed to remove mobscaler modifier: {} (ID: {}) from attribute: {}", 
                            modifier.getName(), modifier.getId(), attribute);
                    }
                }
                
                if (stillHasMobscalerModifiers) {
                    LOGGER.warn("Some mobscaler modifiers remain on attribute {} after removal attempt", attribute);
                } else if (!remainingModifiers.isEmpty()) {
                } else {
                }
            }
        }

        // Если это здоровье, устанавливаем его равным базовому значению
        AttributeInstance healthAttr = entity.getAttribute(Attributes.MAX_HEALTH);
        if (healthAttr != null) {
            double baseValue = healthAttr.getBaseValue();
            entity.setHealth((float)baseValue);
        }
    }

    private static void applyCaveModifiers(LivingEntity entity, IndividualMobAttributes attributes, double healthMultiplier, double damageMultiplier) {
        applyHealthModifier(entity, attributes.getCaveHealthAddition(), attributes.getCaveHealthMultiplier(), healthMultiplier);
        applyArmorModifier(entity, attributes.getCaveArmorAddition(), attributes.getCaveArmorMultiplier(), 1.0);
        applyDamageModifier(entity, attributes.getCaveDamageAddition(), attributes.getCaveDamageMultiplier(), damageMultiplier);
        applySpeedModifier(entity, attributes.getCaveSpeedAddition(), attributes.getCaveSpeedMultiplier(), 1.0);
        applyKnockbackResistanceModifier(entity, attributes.getCaveKnockbackResistanceAddition(), attributes.getCaveKnockbackResistanceMultiplier(), 1.0);
        applyAttackKnockbackModifier(entity, attributes.getCaveAttackKnockbackAddition(), attributes.getCaveAttackKnockbackMultiplier(), 1.0);
        applyAttackSpeedModifier(entity, attributes.getCaveAttackSpeedAddition(), attributes.getCaveAttackSpeedMultiplier(), 1.0);
        applyFollowRangeModifier(entity, attributes.getCaveFollowRangeAddition(), attributes.getCaveFollowRangeMultiplier(), 1.0);
        applyFlyingSpeedModifier(entity, attributes.getCaveFlyingSpeedAddition(), attributes.getCaveFlyingSpeedMultiplier(), 1.0);
        applySwimSpeedModifier(entity, attributes.getCaveSwimSpeedAddition(), attributes.getCaveSwimSpeedMultiplier(), 1.0);
        applyReachDistanceModifier(entity, attributes.getCaveReachDistanceAddition(), attributes.getCaveReachDistanceMultiplier(), 1.0);
    }

    private static void applyNightModifiers(LivingEntity entity, IndividualMobAttributes attributes, double healthMultiplier, double damageMultiplier) {
        applyHealthModifier(entity, attributes.getNightHealthAddition(), attributes.getNightHealthMultiplier(), healthMultiplier);
        applyArmorModifier(entity, attributes.getNightArmorAddition(), attributes.getNightArmorMultiplier(), 1.0);
        applyDamageModifier(entity, attributes.getNightDamageAddition(), attributes.getNightDamageMultiplier(), damageMultiplier);
        applySpeedModifier(entity, attributes.getNightSpeedAddition(), attributes.getNightSpeedMultiplier(), 1.0);
        applyKnockbackResistanceModifier(entity, attributes.getNightKnockbackResistanceAddition(), attributes.getNightKnockbackResistanceMultiplier(), 1.0);
        applyAttackKnockbackModifier(entity, attributes.getNightAttackKnockbackAddition(), attributes.getNightAttackKnockbackMultiplier(), 1.0);
        applyAttackSpeedModifier(entity, attributes.getNightAttackSpeedAddition(), attributes.getNightAttackSpeedMultiplier(), 1.0);
        applyFollowRangeModifier(entity, attributes.getNightFollowRangeAddition(), attributes.getNightFollowRangeMultiplier(), 1.0);
        applyFlyingSpeedModifier(entity, attributes.getNightFlyingSpeedAddition(), attributes.getNightFlyingSpeedMultiplier(), 1.0);
        applySwimSpeedModifier(entity, attributes.getNightSwimSpeedAddition(), attributes.getNightSwimSpeedMultiplier(), 1.0);
        applyReachDistanceModifier(entity, attributes.getNightReachDistanceAddition(), attributes.getNightReachDistanceMultiplier(), 1.0);
    }

    private static void applyDefaultModifiers(LivingEntity entity, IndividualMobAttributes attributes, double healthMultiplier, double damageMultiplier) {
        applyHealthModifier(entity, attributes.getHealthAddition(), attributes.getHealthMultiplier(), healthMultiplier);
        applyArmorModifier(entity, attributes.getArmorAddition(), attributes.getArmorMultiplier(), 1.0);
        applyDamageModifier(entity, attributes.getDamageAddition(), attributes.getDamageMultiplier(), damageMultiplier);
        applySpeedModifier(entity, attributes.getSpeedAddition(), attributes.getSpeedMultiplier(), 1.0);
        applyKnockbackResistanceModifier(entity, attributes.getKnockbackResistanceAddition(), attributes.getKnockbackResistanceMultiplier(), 1.0);
        applyAttackKnockbackModifier(entity, attributes.getAttackKnockbackAddition(), attributes.getAttackKnockbackMultiplier(), 1.0);
        applyAttackSpeedModifier(entity, attributes.getAttackSpeedAddition(), attributes.getAttackSpeedMultiplier(), 1.0);
        applyFollowRangeModifier(entity, attributes.getFollowRangeAddition(), attributes.getFollowRangeMultiplier(), 1.0);
        applyFlyingSpeedModifier(entity, attributes.getFlyingSpeedAddition(), attributes.getFlyingSpeedMultiplier(), 1.0);
        applySwimSpeedModifier(entity, attributes.getSwimSpeedAddition(), attributes.getSwimSpeedMultiplier(), 1.0);
        applyReachDistanceModifier(entity, attributes.getReachDistanceAddition(), attributes.getReachDistanceMultiplier(), 1.0);
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
            entity.setHealth(Math.min((float)newMax, entity.getMaxHealth()));
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

    private static AttributeModifier createModifier(UUID uuid, String name, double value) {
        // Всегда используем ADDITION, так как мы уже применили множитель в формуле
        return new AttributeModifier(
            uuid,
            "mobscaler_" + name,
            value,
            AttributeModifier.Operation.ADDITION
        );
    }

    public static IndividualMobConfig getIndividualMobConfig(String entityId) {
        IndividualMobConfig config = individualMobConfigs.get(entityId);
        if (config != null) {
        } else {
        }
        return config;
    }

    public static void removeModConfig(String modId) {
            
        // Удаляем настройки мода
        IndividualMobAttributes oldConfig = modConfigs.remove(modId);
        LOGGER.debug("Removed mod config: {}, old config existed: {}", modId, oldConfig != null);
        
        // Проверяем, инициализирован ли сервер
        net.minecraft.server.MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server != null) {
            for (Level level : server.getAllLevels()) {
                AABB worldBounds = new AABB(
                    level.getWorldBorder().getMinX(), Double.NEGATIVE_INFINITY, level.getWorldBorder().getMinZ(),
                    level.getWorldBorder().getMaxX(), Double.POSITIVE_INFINITY, level.getWorldBorder().getMaxZ()
                );
                
                // Получаем множители сложности
                double healthMultiplier = EntityHandler.getDifficultyMultiplier(level.getDifficulty(), true);
                double damageMultiplier = EntityHandler.getDifficultyMultiplier(level.getDifficulty(), false);
                String dimKey = level.dimension().location().toString();
                
                LOGGER.debug("Searching for entities from mod {} in world {}", modId, dimKey);
                
                int count = 0;
                // Получаем все живые сущности в мире
                for (LivingEntity entity : level.getEntitiesOfClass(LivingEntity.class, worldBounds)) {
                    String entityModId = EntityType.getKey(entity.getType()).getNamespace();
                    
                    // Проверяем все сущности, которые могли быть изменены этим модом
                    if (entityModId.equals(modId) || hasModifierFromMod(entity)) {
                        count++;
                        
                        // Полностью удаляем все модификаторы
                        removeAllMobscalerModifiers(entity);
                        
                        // Заново применяем стандартные настройки измерения
                        EntityHandler.handleMobModifiers(entity, level, dimKey, EntityHandler.isNight(level), healthMultiplier, damageMultiplier);
                    }
                }
                
                LOGGER.debug("Found and reset {} entities from mod {} in world {}", count, modId, dimKey);
            }
        } else {
            LOGGER.debug("Server is not initialized yet, skipping entity updates for mod: {}", modId);
        }
    }

    // Проверяем, есть ли у сущности модификаторы от нашего мода
    private static boolean hasModifierFromMod(LivingEntity entity) {
        List<net.minecraft.world.entity.ai.attributes.Attribute> attributesList = new ArrayList<>(Arrays.asList(
            Attributes.MAX_HEALTH,
            Attributes.ARMOR,
            Attributes.ATTACK_DAMAGE,
            Attributes.MOVEMENT_SPEED,
            Attributes.KNOCKBACK_RESISTANCE,
            Attributes.ATTACK_KNOCKBACK,
            Attributes.ATTACK_SPEED,
            Attributes.FOLLOW_RANGE,
            Attributes.FLYING_SPEED
        ));
        
        // Добавляем атрибуты из ForgeMod через рефлексию
        try {
            Class<?> forgeModClass = Class.forName("net.minecraftforge.common.ForgeMod");
            
            // Получаем SWIM_SPEED
            Field swimSpeedField = forgeModClass.getDeclaredField("SWIM_SPEED");
            if (swimSpeedField.getType().isAssignableFrom(Supplier.class)) {
                Object supplier = swimSpeedField.get(null);
                if (supplier instanceof Supplier) {
                    net.minecraft.world.entity.ai.attributes.Attribute swimAttribute = 
                        (net.minecraft.world.entity.ai.attributes.Attribute) ((Supplier<?>) supplier).get();
                    attributesList.add(swimAttribute);
                }
            }
            
            // Получаем REACH_DISTANCE
            Field reachDistanceField = forgeModClass.getDeclaredField("REACH_DISTANCE");
            if (reachDistanceField.getType().isAssignableFrom(Supplier.class)) {
                Object supplier = reachDistanceField.get(null);
                if (supplier instanceof Supplier) {
                    net.minecraft.world.entity.ai.attributes.Attribute reachAttribute = 
                        (net.minecraft.world.entity.ai.attributes.Attribute) ((Supplier<?>) supplier).get();
                    attributesList.add(reachAttribute);
                }
            }
            
        } catch (Exception e) {
            LOGGER.error("Error getting Forge attributes for checking modifiers: " + e.getMessage());
        }

        for (net.minecraft.world.entity.ai.attributes.Attribute attribute : attributesList) {
            AttributeInstance attr = entity.getAttribute(attribute);
            if (attr != null) {
                for (AttributeModifier modifier : attr.getModifiers()) {
                    if (modifier.getName().startsWith("mobscaler_")) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    public static void addIndividualMobConfig(String entityId, IndividualMobConfig config) {
        // Если конфиг null, удаляем существующую конфигурацию
        if (config == null) {
            removeIndividualMobConfig(entityId);
            return;
        }

        // Удаляем старую конфигурацию, если она существует
        IndividualMobConfig oldConfig = individualMobConfigs.get(entityId);
        if (oldConfig != null) {
            LOGGER.debug("Removing old config for entity: {}", entityId);
            removeIndividualMobConfig(entityId);
        }

        // Добавляем новую конфигурацию
        individualMobConfigs.put(entityId, config);
        LOGGER.debug("Added new individual config for entity: {}", entityId);
    }

    public static void removeIndividualMobConfig(String entityId) {
        // Удаляем конфигурацию
        IndividualMobConfig oldConfig = individualMobConfigs.remove(entityId);
        LOGGER.debug("Removed individual config for entity: {}, old config existed: {}", entityId, oldConfig != null);
        
        // Удаляем модификаторы у всех существующих мобов этого типа
        String[] parts = entityId.split(":");
        if (parts.length == 2) {
            String namespace = parts[0];
            String path = parts[1];
            ResourceLocation entityType = new ResourceLocation(namespace, path);
            
            // Получаем все сущности в мире
            try {
                // Проверяем, инициализирован ли сервер
                net.minecraft.server.MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
                if (server != null) {
                    for (Level level : server.getAllLevels()) {
                        AABB worldBounds = new AABB(
                            level.getWorldBorder().getMinX(), Double.NEGATIVE_INFINITY, level.getWorldBorder().getMinZ(),
                            level.getWorldBorder().getMaxX(), Double.POSITIVE_INFINITY, level.getWorldBorder().getMaxZ()
                        );
                        
                        // Получаем множители сложности
                        double healthMultiplier = EntityHandler.getDifficultyMultiplier(level.getDifficulty(), true);
                        double damageMultiplier = EntityHandler.getDifficultyMultiplier(level.getDifficulty(), false);
                        String dimKey = level.dimension().location().toString();
                        
                        
                        int count = 0;
                        for (LivingEntity entity : level.getEntitiesOfClass(LivingEntity.class, worldBounds)) {
                            if (EntityType.getKey(entity.getType()).equals(entityType)) {
                                count++;

                                // Удаляем все модификаторы
                                removeAllMobscalerModifiers(entity);
                                
                                // Заново применяем стандартные настройки измерения
                                EntityHandler.handleMobModifiers(entity, level, dimKey, EntityHandler.isNight(level), healthMultiplier, damageMultiplier);
                            }
                        }
                        
                        LOGGER.debug("Found and reset {} entities of type {} in world {}", count, entityId, dimKey);
                    }
                } else {
                    LOGGER.debug("Server is not initialized yet, skipping entity updates for entity: {}", entityId);
                }
            } catch (Exception e) {
                LOGGER.error("Error while removing modifiers for entity type {}: {}", entityId, e.getMessage(), e);
            }
        }
    }
    
    public static void addModConfig(String modId, IndividualMobAttributes config) {
        if (config == null) {
            return;
        }

        // Удаляем старую конфигурацию, если она существует
        IndividualMobAttributes oldConfig = modConfigs.get(modId);
        if (oldConfig != null) {
            LOGGER.debug("Removing old config for mod: {}", modId);
            removeModConfig(modId);
        }

        // Проверяем и удаляем старые настройки мода с похожими ID
        for (Map.Entry<String, IndividualMobAttributes> entry : new HashMap<>(modConfigs).entrySet()) {
            String oldModId = entry.getKey();
            // Проверяем, является ли старый ID тем же модом, но с другим именем
            if (oldModId.startsWith(modId) && !oldModId.equals(modId)) {
                // Если нашли старые настройки для этого мода, удаляем их
                removeModConfig(oldModId);
            }
        }

        // Добавляем новые настройки
        modConfigs.put(modId, config);


        // Проверяем, инициализирован ли сервер
        net.minecraft.server.MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server != null) {
            // Сначала очищаем ВСЕ сущности с модификаторами "mobscaler_" в мире
            clearAllMobscalerModifiers(server);
            
            // Затем применяем настройки к мобам этого мода
            for (Level level : server.getAllLevels()) {
                AABB worldBounds = new AABB(
                    level.getWorldBorder().getMinX(), Double.NEGATIVE_INFINITY, level.getWorldBorder().getMinZ(),
                    level.getWorldBorder().getMaxX(), Double.POSITIVE_INFINITY, level.getWorldBorder().getMaxZ()
                );
                
                // Получаем множители сложности
                double healthMultiplier = EntityHandler.getDifficultyMultiplier(level.getDifficulty(), true);
                double damageMultiplier = EntityHandler.getDifficultyMultiplier(level.getDifficulty(), false);
                String dimKey = level.dimension().location().toString();
                
                int count = 0;
                for (LivingEntity entity : level.getEntitiesOfClass(LivingEntity.class, worldBounds)) {
                    String entityModId = EntityType.getKey(entity.getType()).getNamespace();
                    if (entityModId.equals(modId)) {
                        count++;
                        
                        // Применяем новые настройки через EntityHandler для правильной обработки измерений
                        EntityHandler.handleMobModifiers(entity, level, dimKey, EntityHandler.isNight(level), healthMultiplier, damageMultiplier);
                    }
                }
                
                LOGGER.debug("Found and updated {} entities from mod {} in world {}", count, modId, dimKey);
            }
        } else {
            LOGGER.debug("Server is not initialized yet, skipping entity updates for mod: {}", modId);
        }
    }

    // Очищает все модификаторы "mobscaler_" со всех сущностей в мире
    private static void clearAllMobscalerModifiers(net.minecraft.server.MinecraftServer server) {
        LOGGER.debug("Clearing all mobscaler modifiers from all entities in the world");
        
        int totalCount = 0;
        int failedCount = 0;
        
        // Проходим по всем измерениям сервера
        for (Level level : server.getAllLevels()) {
            AABB worldBounds = new AABB(
                level.getWorldBorder().getMinX(), Double.NEGATIVE_INFINITY, level.getWorldBorder().getMinZ(),
                level.getWorldBorder().getMaxX(), Double.POSITIVE_INFINITY, level.getWorldBorder().getMaxZ()
            );
            
            // Получаем множители сложности для данного измерения
            double healthMultiplier = EntityHandler.getDifficultyMultiplier(level.getDifficulty(), true);
            double damageMultiplier = EntityHandler.getDifficultyMultiplier(level.getDifficulty(), false);
            String dimKey = level.dimension().location().toString();
            
            int count = 0;
            // Проверяем все живые сущности в измерении
            for (LivingEntity entity : level.getEntitiesOfClass(LivingEntity.class, worldBounds)) {
                // Проверяем, имеет ли сущность модификаторы от нашего мода
                if (hasModifierFromMod(entity)) {
                    count++;
                    totalCount++;
                    String entityId = EntityType.getKey(entity.getType()).toString();
                    
                    // Сначала пытаемся удалить все модификаторы
                    removeAllMobscalerModifiers(entity);
                    
                    // Проверяем, остались ли модификаторы после удаления
                    if (hasModifierFromMod(entity)) {
                        failedCount++;
                        LOGGER.warn("Failed to remove all mobscaler modifiers from entity: {}", entityId);
                        
                        // Повторная попытка удаления с более детальной информацией
                        logAllModifiers(entity);
                        removeAllMobscalerModifiersAggressively(entity);
                    }
                    
                    // Применяем стандартные настройки измерения
                    EntityHandler.handleMobModifiers(entity, level, dimKey, EntityHandler.isNight(level), healthMultiplier, damageMultiplier);
                }
            }
            
            LOGGER.debug("Found and processed {} entities with mobscaler modifiers in dimension {}", count, dimKey);
        }
        
        LOGGER.debug("Total entities processed: {}, Failed to remove modifiers: {}", totalCount, failedCount);
    }

    // Логирует все модификаторы атрибутов сущности для диагностики
    private static void logAllModifiers(LivingEntity entity) {
        List<net.minecraft.world.entity.ai.attributes.Attribute> attributesList = new ArrayList<>(Arrays.asList(
            Attributes.MAX_HEALTH,
            Attributes.ARMOR,
            Attributes.ATTACK_DAMAGE,
            Attributes.MOVEMENT_SPEED,
            Attributes.KNOCKBACK_RESISTANCE,
            Attributes.ATTACK_KNOCKBACK,
            Attributes.ATTACK_SPEED,
            Attributes.FOLLOW_RANGE,
            Attributes.FLYING_SPEED
        ));
        
        // Добавляем атрибуты из ForgeMod через рефлексию
        try {
            Class<?> forgeModClass = Class.forName("net.minecraftforge.common.ForgeMod");
            
            // Получаем SWIM_SPEED
            Field swimSpeedField = forgeModClass.getDeclaredField("SWIM_SPEED");
            if (swimSpeedField.getType().isAssignableFrom(Supplier.class)) {
                Object supplier = swimSpeedField.get(null);
                if (supplier instanceof Supplier) {
                    net.minecraft.world.entity.ai.attributes.Attribute swimAttribute = 
                        (net.minecraft.world.entity.ai.attributes.Attribute) ((Supplier<?>) supplier).get();
                    attributesList.add(swimAttribute);
                }
            }
            
            // Получаем REACH_DISTANCE
            Field reachDistanceField = forgeModClass.getDeclaredField("REACH_DISTANCE");
            if (reachDistanceField.getType().isAssignableFrom(Supplier.class)) {
                Object supplier = reachDistanceField.get(null);
                if (supplier instanceof Supplier) {
                    net.minecraft.world.entity.ai.attributes.Attribute reachAttribute = 
                        (net.minecraft.world.entity.ai.attributes.Attribute) ((Supplier<?>) supplier).get();
                    attributesList.add(reachAttribute);
                }
            }
            
        } catch (Exception e) {
            LOGGER.error("Error getting Forge attributes for logging: " + e.getMessage());
        }
        
        for (net.minecraft.world.entity.ai.attributes.Attribute attribute : attributesList) {
            AttributeInstance attr = entity.getAttribute(attribute);
            if (attr != null) {
                java.util.Collection<AttributeModifier> modifiers = attr.getModifiers();
                if (!modifiers.isEmpty()) {
                    for (AttributeModifier modifier : modifiers) {
                        LOGGER.debug("  Modifier: Name={}, ID={}, Value={}, Operation={}", 
                            modifier.getName(), modifier.getId(), modifier.getAmount(), modifier.getOperation());
                    }
                }
            }
        }
    }
    
    // Агрессивное удаление всех модификаторов - используется при отказе стандартного метода
    private static void removeAllMobscalerModifiersAggressively(LivingEntity entity) {
        
        List<net.minecraft.world.entity.ai.attributes.Attribute> attributesList = new ArrayList<>(Arrays.asList(
            Attributes.MAX_HEALTH,
            Attributes.ARMOR,
            Attributes.ATTACK_DAMAGE,
            Attributes.MOVEMENT_SPEED,
            Attributes.KNOCKBACK_RESISTANCE,
            Attributes.ATTACK_KNOCKBACK,
            Attributes.ATTACK_SPEED,
            Attributes.FOLLOW_RANGE,
            Attributes.FLYING_SPEED
        ));
        
        // Добавляем атрибуты из ForgeMod через рефлексию
        try {
            Class<?> forgeModClass = Class.forName("net.minecraftforge.common.ForgeMod");
            
            // Получаем SWIM_SPEED
            Field swimSpeedField = forgeModClass.getDeclaredField("SWIM_SPEED");
            if (swimSpeedField.getType().isAssignableFrom(Supplier.class)) {
                Object supplier = swimSpeedField.get(null);
                if (supplier instanceof Supplier) {
                    net.minecraft.world.entity.ai.attributes.Attribute swimAttribute = 
                        (net.minecraft.world.entity.ai.attributes.Attribute) ((Supplier<?>) supplier).get();
                    attributesList.add(swimAttribute);
                }
            }
            
            // Получаем REACH_DISTANCE
            Field reachDistanceField = forgeModClass.getDeclaredField("REACH_DISTANCE");
            if (reachDistanceField.getType().isAssignableFrom(Supplier.class)) {
                Object supplier = reachDistanceField.get(null);
                if (supplier instanceof Supplier) {
                    net.minecraft.world.entity.ai.attributes.Attribute reachAttribute = 
                        (net.minecraft.world.entity.ai.attributes.Attribute) ((Supplier<?>) supplier).get();
                    attributesList.add(reachAttribute);
                }
            }

        } catch (Exception e) {
            LOGGER.error("Error getting Forge attributes for aggressive remove: " + e.getMessage());
        }
        
        for (net.minecraft.world.entity.ai.attributes.Attribute attribute : attributesList) {
            AttributeInstance attr = entity.getAttribute(attribute);
            if (attr != null) {
                // Получаем все модификаторы
                java.util.Collection<AttributeModifier> allModifiers = new java.util.ArrayList<>(attr.getModifiers());
                
                // Удаляем каждый модификатор
                for (AttributeModifier modifier : allModifiers) {
                    // Если это модификатор нашего мода или у нас есть подозрение на то,
                    // что это наш модификатор, удаляем его
                    if (modifier.getName().startsWith("mobscaler_") || 
                        modifier.getId().equals(HEALTH_MODIFIER_UUID) ||
                        modifier.getId().equals(ARMOR_MODIFIER_UUID) ||
                        modifier.getId().equals(DAMAGE_MODIFIER_UUID) ||
                        modifier.getId().equals(SPEED_MODIFIER_UUID) ||
                        modifier.getId().equals(KNOCKBACK_RESISTANCE_UUID) ||
                        modifier.getId().equals(ATTACK_KNOCKBACK_UUID) ||
                        modifier.getId().equals(ATTACK_SPEED_UUID) ||
                        modifier.getId().equals(FOLLOW_RANGE_UUID) ||
                        modifier.getId().equals(FLYING_SPEED_UUID) ||
                        modifier.getId().equals(SWIM_SPEED_UUID) ||
                        modifier.getId().equals(REACH_DISTANCE_UUID)) {
                        
                        attr.removeModifier(modifier.getId());
                    
                    }
                }
            }
        }
        
        // Устанавливаем здоровье равным базовому значению
        AttributeInstance healthAttr = entity.getAttribute(Attributes.MAX_HEALTH);
        if (healthAttr != null) {
            double baseValue = healthAttr.getBaseValue();
            entity.setHealth((float)baseValue);
        }
    }

    public static void clearAllIndividualMobConfigs() {
        // Проверяем, инициализирован ли сервер
        net.minecraft.server.MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server != null) {
            // Сохраняем копию ключей, чтобы избежать ConcurrentModificationException
            java.util.List<String> entityIds = new java.util.ArrayList<>(individualMobConfigs.keySet());
            
            // Удаляем каждую конфигурацию по отдельности, чтобы сбросить модификаторы у существующих мобов
            for (String entityId : entityIds) {
                removeIndividualMobConfig(entityId);
            }
            
            // Для уверенности очищаем всю карту
            individualMobConfigs.clear();
        } else {
            individualMobConfigs.clear();
        }
    }

    public static IndividualMobAttributes getModConfig(String modId) {
        return modConfigs.get(modId);
    }

    // Методы для получения копий конфигураций
    public static Map<String, IndividualMobConfig> getIndividualMobConfigs() {
        return new HashMap<>(individualMobConfigs);
    }
    
    public static Map<String, IndividualMobAttributes> getModConfigs() {
        return new HashMap<>(modConfigs);
    }

    public static void saveConfigs() {
        try {
            // Сохраняем конфигурации через IndividualMobConfigManager
            IndividualMobConfigManager.saveModConfigs();
            IndividualMobConfigManager.saveIndividualConfigs();

        } catch (Exception e) {
            LOGGER.error("Ошибка при сохранении конфигураций: ", e);
        }
    }

    public static void clearAllModConfigs() {
        // Проверяем, инициализирован ли сервер
        net.minecraft.server.MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server != null) {
            // Сохраняем копию ключей, чтобы избежать ConcurrentModificationException
            java.util.List<String> modIds = new java.util.ArrayList<>(modConfigs.keySet());
            
            // Удаляем каждую конфигурацию по отдельности
            for (String modId : modIds) {
                removeModConfig(modId);
            }
            
            // Для уверенности очищаем всю карту
            modConfigs.clear();
        } else {
            modConfigs.clear();
        }
    }

    // Добавляем новые методы для новых атрибутов
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
} 