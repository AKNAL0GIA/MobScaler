package com.example.mobscaler.config;

import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.server.ServerLifecycleHooks;
import com.example.mobscaler.events.EntityHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;

public class IndividualMobManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(IndividualMobManager.class);
    private static final Map<String, IndividualMobConfig> individualMobConfigs = new HashMap<>();
    private static final Map<String, IndividualMobAttributes> modConfigs = new HashMap<>();

    // ResourceLocation IDs для модификаторов атрибутов (1.21.1 API)
    private static final ResourceLocation HEALTH_MODIFIER_ID = ResourceLocation.fromNamespaceAndPath("mobscaler", "health");
    private static final ResourceLocation ARMOR_MODIFIER_ID = ResourceLocation.fromNamespaceAndPath("mobscaler", "armor");
    private static final ResourceLocation DAMAGE_MODIFIER_ID = ResourceLocation.fromNamespaceAndPath("mobscaler", "damage");
    private static final ResourceLocation SPEED_MODIFIER_ID = ResourceLocation.fromNamespaceAndPath("mobscaler", "speed");
    private static final ResourceLocation KNOCKBACK_RESISTANCE_ID = ResourceLocation.fromNamespaceAndPath("mobscaler", "knockback_resistance");
    private static final ResourceLocation ATTACK_KNOCKBACK_ID = ResourceLocation.fromNamespaceAndPath("mobscaler", "attack_knockback");
    private static final ResourceLocation ATTACK_SPEED_ID = ResourceLocation.fromNamespaceAndPath("mobscaler", "attack_speed");
    private static final ResourceLocation FOLLOW_RANGE_ID = ResourceLocation.fromNamespaceAndPath("mobscaler", "follow_range");
    private static final ResourceLocation FLYING_SPEED_ID = ResourceLocation.fromNamespaceAndPath("mobscaler", "flying_speed");
    private static final ResourceLocation ARMOR_TOUGHNESS_ID = ResourceLocation.fromNamespaceAndPath("mobscaler", "armor_toughness");
    private static final ResourceLocation LUCK_ID = ResourceLocation.fromNamespaceAndPath("mobscaler", "luck");
    private static final ResourceLocation SWIM_SPEED_ID = ResourceLocation.fromNamespaceAndPath("mobscaler", "swim_speed");
    private static final ResourceLocation REACH_DISTANCE_ID = ResourceLocation.fromNamespaceAndPath("mobscaler", "reach_distance");
    private static final ResourceLocation ENTITY_REACH_ID = ResourceLocation.fromNamespaceAndPath("mobscaler", "entity_reach");
    private static final ResourceLocation BURNING_TIME_ID = ResourceLocation.fromNamespaceAndPath("mobscaler", "burning_time");
    private static final ResourceLocation EXPLOSION_KNOCKBACK_RESISTANCE_ID = ResourceLocation.fromNamespaceAndPath("mobscaler", "explosion_knockback_resistance");
    private static final ResourceLocation FALL_DAMAGE_ID = ResourceLocation.fromNamespaceAndPath("mobscaler", "fall_damage_multiplier");
    private static final ResourceLocation OXYGEN_BONUS_ID = ResourceLocation.fromNamespaceAndPath("mobscaler", "oxygen_bonus");
    private static final ResourceLocation SAFE_FALL_DISTANCE_ID = ResourceLocation.fromNamespaceAndPath("mobscaler", "safe_fall_distance");
    private static final ResourceLocation WATER_MOVEMENT_EFFICIENCY_ID = ResourceLocation.fromNamespaceAndPath("mobscaler", "water_movement_efficiency");
    
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
        @SuppressWarnings("unchecked")
        Holder<net.minecraft.world.entity.ai.attributes.Attribute>[] attributesToCheck = new Holder[] {
            Attributes.MAX_HEALTH, Attributes.ARMOR, Attributes.ATTACK_DAMAGE,
            Attributes.MOVEMENT_SPEED, Attributes.KNOCKBACK_RESISTANCE,
            Attributes.ATTACK_KNOCKBACK, Attributes.ATTACK_SPEED,
            Attributes.FOLLOW_RANGE, Attributes.FLYING_SPEED,
            Attributes.ARMOR_TOUGHNESS, Attributes.LUCK,
            Attributes.WATER_MOVEMENT_EFFICIENCY,
            Attributes.BLOCK_INTERACTION_RANGE,
            Attributes.ENTITY_INTERACTION_RANGE,
            Attributes.BURNING_TIME,
            Attributes.FALL_DAMAGE_MULTIPLIER,
            Attributes.EXPLOSION_KNOCKBACK_RESISTANCE,
            Attributes.OXYGEN_BONUS,
            Attributes.SAFE_FALL_DISTANCE,
            net.neoforged.neoforge.common.NeoForgeMod.SWIM_SPEED
        };

        for (Holder<net.minecraft.world.entity.ai.attributes.Attribute> attribute : attributesToCheck) {
            AttributeInstance attr = entity.getAttribute(attribute);
            if (attr != null && attr.getModifiers().isEmpty()) {
                LOGGER.warn("No modifiers found for attribute {} after application", attribute.value().getDescriptionId());
            }
        }
    }

    private static void removeAllMobscalerModifiers(LivingEntity entity) {

        // Список всех атрибутов для проверки
        List<Holder<net.minecraft.world.entity.ai.attributes.Attribute>> attributesList = new ArrayList<>(Arrays.asList(
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
            Attributes.LUCK,
            Attributes.JUMP_STRENGTH,
            Attributes.WATER_MOVEMENT_EFFICIENCY,
            Attributes.BLOCK_INTERACTION_RANGE,
            Attributes.ENTITY_INTERACTION_RANGE,
            Attributes.BURNING_TIME,
            Attributes.FALL_DAMAGE_MULTIPLIER,
            Attributes.EXPLOSION_KNOCKBACK_RESISTANCE,
            Attributes.OXYGEN_BONUS,
            Attributes.SAFE_FALL_DISTANCE,
            Attributes.BLOCK_BREAK_SPEED,
            Attributes.MINING_EFFICIENCY,
            Attributes.STEP_HEIGHT,
            Attributes.SUBMERGED_MINING_SPEED,
            Attributes.SNEAKING_SPEED,
            Attributes.MOVEMENT_EFFICIENCY,
            Attributes.GRAVITY
        ));

        // Добавляем NeoForge атрибуты напрямую
        attributesList.add(net.neoforged.neoforge.common.NeoForgeMod.SWIM_SPEED);
        attributesList.add(net.neoforged.neoforge.common.NeoForgeMod.NAMETAG_DISTANCE);

        // Список всех ResourceLocation ID модификаторов нашего мода
        ResourceLocation[] modifierIds = {
            HEALTH_MODIFIER_ID,
            ARMOR_MODIFIER_ID,
            DAMAGE_MODIFIER_ID,
            SPEED_MODIFIER_ID,
            KNOCKBACK_RESISTANCE_ID,
            ATTACK_KNOCKBACK_ID,
            ATTACK_SPEED_ID,
            FOLLOW_RANGE_ID,
            FLYING_SPEED_ID,
            SWIM_SPEED_ID,
            REACH_DISTANCE_ID,
            ENTITY_REACH_ID,
            BURNING_TIME_ID,
            EXPLOSION_KNOCKBACK_RESISTANCE_ID,
            FALL_DAMAGE_ID,
            OXYGEN_BONUS_ID,
            SAFE_FALL_DISTANCE_ID,
            WATER_MOVEMENT_EFFICIENCY_ID
        };

        // Удаляем все модификаторы по ID
        for (Holder<net.minecraft.world.entity.ai.attributes.Attribute> attribute : attributesList) {
            AttributeInstance attr = entity.getAttribute(attribute);
            if (attr != null) {
                // Удаляем модификаторы по ResourceLocation ID
                for (ResourceLocation id : modifierIds) {
                    AttributeModifier modifier = attr.getModifier(id);
                    if (modifier != null) {
                        attr.removeModifier(id);
                    }
                }

                // Удаляем модификаторы по имени - собираем ID и затем удаляем
                java.util.Collection<AttributeModifier> modifiers = attr.getModifiers();
                java.util.List<ResourceLocation> toRemove = new java.util.ArrayList<>();

                // Сначала собираем ID всех модификаторов с именем, начинающимся с "mobscaler_"
                for (AttributeModifier modifier : modifiers) {
                    if (modifier.id().toString().startsWith("mobscaler_")) {
                        toRemove.add(modifier.id());
                    }
                }

                // Теперь удаляем модификаторы по собранным ID
                for (ResourceLocation id : toRemove) {
                    attr.removeModifier(id);
                }

                // Проверяем, что все модификаторы удалены
                java.util.Collection<AttributeModifier> remainingModifiers = attr.getModifiers();
                boolean stillHasMobscalerModifiers = false;
                for (AttributeModifier modifier : remainingModifiers) {
                    if (modifier.id().toString().startsWith("mobscaler_")) {
                        stillHasMobscalerModifiers = true;
                        LOGGER.warn("Failed to remove mobscaler modifier: {} (ID: {}) from attribute: {}",
                            modifier.id(), modifier.id(), attribute);
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
        applyArmorToughnessModifier(entity, attributes.getCaveArmorToughnessAddition(), attributes.getCaveArmorToughnessMultiplier(), 1.0);
        applyLuckModifier(entity, attributes.getCaveLuckAddition(), attributes.getCaveLuckMultiplier(), 1.0);
        applySwimSpeedModifier(entity, attributes.getCaveSwimSpeedAddition(), attributes.getCaveSwimSpeedMultiplier(), 1.0);
        applyBlockReachModifier(entity, attributes.getCaveBlockReachAddition(), attributes.getCaveBlockReachMultiplier(), 1.0);
        applyEntityReachModifier(entity, attributes.getCaveEntityReachAddition(), attributes.getCaveEntityReachMultiplier(), 1.0);
        applyBurningTimeModifier(entity, attributes.getCaveBurningTimeAddition(), attributes.getCaveBurningTimeMultiplier(), 1.0);
        applyExplosionKnockbackResistanceModifier(entity, attributes.getCaveExplosionKnockbackResistanceAddition(), attributes.getCaveExplosionKnockbackResistanceMultiplier(), 1.0);
        applyFallDamageModifier(entity, attributes.getCaveFallDamageMultiplier(), 1.0);
        applyOxygenBonusModifier(entity, attributes.getCaveOxygenBonusAddition(), attributes.getCaveOxygenBonusMultiplier(), 1.0);
        applySafeFallDistanceModifier(entity, attributes.getCaveSafeFallDistanceAddition(), attributes.getCaveSafeFallDistanceMultiplier(), 1.0);
        applyWaterMovementEfficiencyModifier(entity, attributes.getCaveWaterMovementEfficiencyAddition(), attributes.getCaveWaterMovementEfficiencyMultiplier(), 1.0);
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
        applyArmorToughnessModifier(entity, attributes.getNightArmorToughnessAddition(), attributes.getNightArmorToughnessMultiplier(), 1.0);
        applyLuckModifier(entity, attributes.getNightLuckAddition(), attributes.getNightLuckMultiplier(), 1.0);
        applySwimSpeedModifier(entity, attributes.getNightSwimSpeedAddition(), attributes.getNightSwimSpeedMultiplier(), 1.0);
        applyBlockReachModifier(entity, attributes.getNightBlockReachAddition(), attributes.getNightBlockReachMultiplier(), 1.0);
        applyEntityReachModifier(entity, attributes.getNightEntityReachAddition(), attributes.getNightEntityReachMultiplier(), 1.0);
        applyBurningTimeModifier(entity, attributes.getNightBurningTimeAddition(), attributes.getNightBurningTimeMultiplier(), 1.0);
        applyExplosionKnockbackResistanceModifier(entity, attributes.getNightExplosionKnockbackResistanceAddition(), attributes.getNightExplosionKnockbackResistanceMultiplier(), 1.0);
        applyFallDamageModifier(entity, attributes.getNightFallDamageMultiplier(), 1.0);
        applyOxygenBonusModifier(entity, attributes.getNightOxygenBonusAddition(), attributes.getNightOxygenBonusMultiplier(), 1.0);
        applySafeFallDistanceModifier(entity, attributes.getNightSafeFallDistanceAddition(), attributes.getNightSafeFallDistanceMultiplier(), 1.0);
        applyWaterMovementEfficiencyModifier(entity, attributes.getNightWaterMovementEfficiencyAddition(), attributes.getNightWaterMovementEfficiencyMultiplier(), 1.0);
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
        applyArmorToughnessModifier(entity, attributes.getArmorToughnessAddition(), attributes.getArmorToughnessMultiplier(), 1.0);
        applyLuckModifier(entity, attributes.getLuckAddition(), attributes.getLuckMultiplier(), 1.0);
        applySwimSpeedModifier(entity, attributes.getSwimSpeedAddition(), attributes.getSwimSpeedMultiplier(), 1.0);
        applyBlockReachModifier(entity, attributes.getBlockReachAddition(), attributes.getBlockReachMultiplier(), 1.0);
        applyEntityReachModifier(entity, attributes.getEntityReachAddition(), attributes.getEntityReachMultiplier(), 1.0);
        applyBurningTimeModifier(entity, attributes.getBurningTimeAddition(), attributes.getBurningTimeMultiplier(), 1.0);
        applyExplosionKnockbackResistanceModifier(entity, attributes.getExplosionKnockbackResistanceAddition(), attributes.getExplosionKnockbackResistanceMultiplier(), 1.0);
        applyFallDamageModifier(entity, attributes.getFallDamageMultiplier(), 1.0);
        applyOxygenBonusModifier(entity, attributes.getOxygenBonusAddition(), attributes.getOxygenBonusMultiplier(), 1.0);
        applySafeFallDistanceModifier(entity, attributes.getSafeFallDistanceAddition(), attributes.getSafeFallDistanceMultiplier(), 1.0);
        applyWaterMovementEfficiencyModifier(entity, attributes.getWaterMovementEfficiencyAddition(), attributes.getWaterMovementEfficiencyMultiplier(), 1.0);
    }

    private static void applyHealthModifier(LivingEntity entity, double addition, double multiplier, double difficultyMultiplier) {
        AttributeInstance attr = entity.getAttribute(Attributes.MAX_HEALTH);
        if (attr != null) {
            double base = attr.getBaseValue();
            // Используем общую формулу: (base + addition) * multiplier * difficultyMultiplier
            double newMax = (base + addition) * multiplier * difficultyMultiplier;


            if (attr.getModifier(HEALTH_MODIFIER_ID) != null) {
                attr.removeModifier(HEALTH_MODIFIER_ID);
            }
            attr.addPermanentModifier(createModifier(HEALTH_MODIFIER_ID, "health", newMax - base));
            entity.setHealth(Math.min((float)newMax, entity.getMaxHealth()));
        }
    }

    private static void applyArmorModifier(LivingEntity entity, double addition, double multiplier, double difficultyMultiplier) {
        AttributeInstance attr = entity.getAttribute(Attributes.ARMOR);
        if (attr != null) {
            double base = attr.getBaseValue();
            double newValue = (base + addition) * multiplier * difficultyMultiplier;


            if (attr.getModifier(ARMOR_MODIFIER_ID) != null) {
                attr.removeModifier(ARMOR_MODIFIER_ID);
            }
            attr.addPermanentModifier(createModifier(ARMOR_MODIFIER_ID, "armor", newValue - base));
        }
    }

    private static void applyDamageModifier(LivingEntity entity, double addition, double multiplier, double difficultyMultiplier) {
        AttributeInstance attr = entity.getAttribute(Attributes.ATTACK_DAMAGE);
        if (attr != null) {
            double base = attr.getBaseValue();
            double newValue = (base + addition) * multiplier * difficultyMultiplier;


            if (attr.getModifier(DAMAGE_MODIFIER_ID) != null) {
                attr.removeModifier(DAMAGE_MODIFIER_ID);
            }
            attr.addPermanentModifier(createModifier(DAMAGE_MODIFIER_ID, "damage", newValue - base));
        }
    }

    private static void applySpeedModifier(LivingEntity entity, double addition, double multiplier, double difficultyMultiplier) {
        AttributeInstance attr = entity.getAttribute(Attributes.MOVEMENT_SPEED);
        if (attr != null) {
            double base = attr.getBaseValue();
            double newValue = (base + addition) * multiplier * difficultyMultiplier;


            if (attr.getModifier(SPEED_MODIFIER_ID) != null) {
                attr.removeModifier(SPEED_MODIFIER_ID);
            }
            attr.addPermanentModifier(createModifier(SPEED_MODIFIER_ID, "speed", newValue - base));
        }
    }

    private static void applyKnockbackResistanceModifier(LivingEntity entity, double addition, double multiplier, double difficultyMultiplier) {
        AttributeInstance attr = entity.getAttribute(Attributes.KNOCKBACK_RESISTANCE);
        if (attr != null) {
            double base = attr.getBaseValue();
            double newValue = (base + addition) * multiplier * difficultyMultiplier;


            if (attr.getModifier(KNOCKBACK_RESISTANCE_ID) != null) {
                attr.removeModifier(KNOCKBACK_RESISTANCE_ID);
            }
            attr.addPermanentModifier(createModifier(KNOCKBACK_RESISTANCE_ID, "knockback_resistance", newValue - base));
        }
    }

    private static void applyAttackKnockbackModifier(LivingEntity entity, double addition, double multiplier, double difficultyMultiplier) {
        AttributeInstance attr = entity.getAttribute(Attributes.ATTACK_KNOCKBACK);
        if (attr != null) {
            double base = attr.getBaseValue();
            double newValue = (base + addition) * multiplier * difficultyMultiplier;


            if (attr.getModifier(ATTACK_KNOCKBACK_ID) != null) {
                attr.removeModifier(ATTACK_KNOCKBACK_ID);
            }
            attr.addPermanentModifier(createModifier(ATTACK_KNOCKBACK_ID, "attack_knockback", newValue - base));
        }
    }

    private static void applyAttackSpeedModifier(LivingEntity entity, double addition, double multiplier, double difficultyMultiplier) {
        AttributeInstance attr = entity.getAttribute(Attributes.ATTACK_SPEED);
        if (attr != null) {
            double base = attr.getBaseValue();
            double newValue = (base + addition) * multiplier * difficultyMultiplier;


            if (attr.getModifier(ATTACK_SPEED_ID) != null) {
                attr.removeModifier(ATTACK_SPEED_ID);
            }
            attr.addPermanentModifier(createModifier(ATTACK_SPEED_ID, "attack_speed", newValue - base));
        }
    }

    private static void applyFollowRangeModifier(LivingEntity entity, double addition, double multiplier, double difficultyMultiplier) {
        AttributeInstance attr = entity.getAttribute(Attributes.FOLLOW_RANGE);
        if (attr != null) {
            double base = attr.getBaseValue();
            double newValue = (base + addition) * multiplier * difficultyMultiplier;


            if (attr.getModifier(FOLLOW_RANGE_ID) != null) {
                attr.removeModifier(FOLLOW_RANGE_ID);
            }
            attr.addPermanentModifier(createModifier(FOLLOW_RANGE_ID, "follow_range", newValue - base));
        }
    }

    private static void applyFlyingSpeedModifier(LivingEntity entity, double addition, double multiplier, double difficultyMultiplier) {
        AttributeInstance attr = entity.getAttribute(Attributes.FLYING_SPEED);
        if (attr != null) {
            double base = attr.getBaseValue();
            double newValue = (base + addition) * multiplier * difficultyMultiplier;


            if (attr.getModifier(FLYING_SPEED_ID) != null) {
                attr.removeModifier(FLYING_SPEED_ID);
            }
            attr.addPermanentModifier(createModifier(FLYING_SPEED_ID, "flying_speed", newValue - base));
        }
    }

    private static void applyBlockReachModifier(LivingEntity entity, double addition, double multiplier, double difficultyMultiplier) {
        AttributeInstance attr = entity.getAttribute(Attributes.BLOCK_INTERACTION_RANGE);
        if (attr != null) {
            double base = attr.getBaseValue();
            double newValue = (base + addition) * multiplier * difficultyMultiplier;


            if (attr.getModifier(REACH_DISTANCE_ID) != null) {
                attr.removeModifier(REACH_DISTANCE_ID);
            }
            attr.addPermanentModifier(createModifier(REACH_DISTANCE_ID, "block_reach", newValue - base));
        }
    }

    private static void applyEntityReachModifier(LivingEntity entity, double addition, double multiplier, double difficultyMultiplier) {
        AttributeInstance attr = entity.getAttribute(Attributes.ENTITY_INTERACTION_RANGE);
        if (attr != null) {
            double base = attr.getBaseValue();
            double newValue = (base + addition) * multiplier * difficultyMultiplier;


            if (attr.getModifier(ENTITY_REACH_ID) != null) {
                attr.removeModifier(ENTITY_REACH_ID);
            }
            attr.addPermanentModifier(createModifier(ENTITY_REACH_ID, "entity_reach", newValue - base));
        }
    }

    private static void applyBurningTimeModifier(LivingEntity entity, double addition, double multiplier, double difficultyMultiplier) {
        AttributeInstance attr = entity.getAttribute(Attributes.BURNING_TIME);
        if (attr != null) {
            double base = attr.getBaseValue();
            double newValue = (base + addition) * multiplier * difficultyMultiplier;

            if (attr.getModifier(BURNING_TIME_ID) != null) {
                attr.removeModifier(BURNING_TIME_ID);
            }
            attr.addPermanentModifier(createModifier(BURNING_TIME_ID, "burning_time", newValue - base));
        }
    }

    private static void applyExplosionKnockbackResistanceModifier(LivingEntity entity, double addition, double multiplier, double difficultyMultiplier) {
        AttributeInstance attr = entity.getAttribute(Attributes.EXPLOSION_KNOCKBACK_RESISTANCE);
        if (attr != null) {
            double base = attr.getBaseValue();
            double newValue = (base + addition) * multiplier * difficultyMultiplier;

            if (attr.getModifier(EXPLOSION_KNOCKBACK_RESISTANCE_ID) != null) {
                attr.removeModifier(EXPLOSION_KNOCKBACK_RESISTANCE_ID);
            }
            attr.addPermanentModifier(createModifier(EXPLOSION_KNOCKBACK_RESISTANCE_ID, "explosion_knockback_resistance", newValue - base));
        }
    } 

    private static void applyFallDamageModifier(LivingEntity entity, double multiplier, double difficultyMultiplier) {
        AttributeInstance attr = entity.getAttribute(Attributes.FALL_DAMAGE_MULTIPLIER);
        if (attr != null) {
            double base = attr.getBaseValue();
            double newValue = base * multiplier * difficultyMultiplier;

            if (attr.getModifier(FALL_DAMAGE_ID) != null) {
                attr.removeModifier(FALL_DAMAGE_ID);
            }
            attr.addPermanentModifier(createModifier(FALL_DAMAGE_ID, "fall_damage_multiplier", newValue - base));
        }
    }
    
    private static void applyOxygenBonusModifier(LivingEntity entity, double addition, double multiplier, double difficultyMultiplier) {
        AttributeInstance attr = entity.getAttribute(Attributes.OXYGEN_BONUS);
        if (attr != null) {
            double base = attr.getBaseValue();
            double newValue = (base + addition) * multiplier * difficultyMultiplier;

            if (attr.getModifier(OXYGEN_BONUS_ID) != null) {
                attr.removeModifier(OXYGEN_BONUS_ID);
            }
            attr.addPermanentModifier(createModifier(OXYGEN_BONUS_ID, "oxygen_bonus", newValue - base));
        }
    }

    private  static void applySafeFallDistanceModifier(LivingEntity entity, double addition, double multiplier, double difficultyMultiplier) {
        AttributeInstance attr = entity.getAttribute(Attributes.SAFE_FALL_DISTANCE);
        if (attr != null) {
            double base = attr.getBaseValue();
            double newValue = (base + addition) * multiplier * difficultyMultiplier;

            if (attr.getModifier(SAFE_FALL_DISTANCE_ID) != null) {
                attr.removeModifier(SAFE_FALL_DISTANCE_ID);
            }
            attr.addPermanentModifier(createModifier(SAFE_FALL_DISTANCE_ID, "safe_fall_distance", newValue - base));
        }
    }

    private static void applyWaterMovementEfficiencyModifier(LivingEntity entity, double addition, double multiplier, double difficultyMultiplier) {
        AttributeInstance attr = entity.getAttribute(Attributes.WATER_MOVEMENT_EFFICIENCY);
        if (attr != null) {
            double base = attr.getBaseValue();
            double newValue = (base + addition) * multiplier * difficultyMultiplier;

            if (attr.getModifier(WATER_MOVEMENT_EFFICIENCY_ID) != null) {
                attr.removeModifier(WATER_MOVEMENT_EFFICIENCY_ID);
            }
            attr.addPermanentModifier(createModifier(WATER_MOVEMENT_EFFICIENCY_ID, "water_movement_efficiency", newValue - base));
        }
    }

    private  static void applyLuckModifier(LivingEntity entity, double addition, double multiplier, double difficultyMultiplier) {
        AttributeInstance attr = entity.getAttribute(Attributes.LUCK);
        if (attr != null) {
            double base = attr.getBaseValue();
            double newValue = (base + addition) * multiplier * difficultyMultiplier;

            if (attr.getModifier(LUCK_ID) != null) {
                attr.removeModifier(LUCK_ID);
            }
            attr.addPermanentModifier(createModifier(LUCK_ID, "luck", newValue - base));
        }
    }

    private static void applyArmorToughnessModifier(LivingEntity entity, double addition, double multiplier, double difficultyMultiplier) {
        AttributeInstance attr = entity.getAttribute(Attributes.ARMOR_TOUGHNESS);
        if (attr != null) {
            double base = attr.getBaseValue();
            double newValue = (base + addition) * multiplier * difficultyMultiplier;

            if (attr.getModifier(ARMOR_TOUGHNESS_ID) != null) {
                attr.removeModifier(ARMOR_TOUGHNESS_ID);
            }
            attr.addPermanentModifier(createModifier(ARMOR_TOUGHNESS_ID, "armor_toughness", newValue - base));
        }
    }

    private static AttributeModifier createModifier(ResourceLocation id, String name, double value) {
        // Always use ADD_VALUE, as we already applied the multiplier in the formula
        return new AttributeModifier(
            id,
            value,
            AttributeModifier.Operation.ADD_VALUE
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
        List<Holder<net.minecraft.world.entity.ai.attributes.Attribute>> attributesList = new ArrayList<>(Arrays.asList(
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

        // Добавляем NeoForge атрибуты
        attributesList.add(net.neoforged.neoforge.common.NeoForgeMod.SWIM_SPEED);

        for (Holder<net.minecraft.world.entity.ai.attributes.Attribute> attribute : attributesList) {
            AttributeInstance attr = entity.getAttribute(attribute);
            if (attr != null) {
                for (AttributeModifier modifier : attr.getModifiers()) {
                    if (modifier.id().toString().startsWith("mobscaler_")) {
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
            ResourceLocation entityType = ResourceLocation.fromNamespaceAndPath(namespace, path);
            
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
        List<Holder<net.minecraft.world.entity.ai.attributes.Attribute>> attributesList = new ArrayList<>(Arrays.asList(
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

        // Добавляем NeoForge атрибуты
        attributesList.add(net.neoforged.neoforge.common.NeoForgeMod.SWIM_SPEED);

        for (Holder<net.minecraft.world.entity.ai.attributes.Attribute> attribute : attributesList) {
            AttributeInstance attr = entity.getAttribute(attribute);
            if (attr != null) {
                java.util.Collection<AttributeModifier> modifiers = attr.getModifiers();
                if (!modifiers.isEmpty()) {
                    for (AttributeModifier modifier : modifiers) {
                        LOGGER.debug("  Modifier: ID={}, Value={}, Operation={}",
                            modifier.id(), modifier.amount(), modifier.operation());
                    }
                }
            }
        }
    }
    
    // Агрессивное удаление всех модификаторов - используется при отказе стандартного метода
    private static void removeAllMobscalerModifiersAggressively(LivingEntity entity) {

        List<Holder<net.minecraft.world.entity.ai.attributes.Attribute>> attributesList = new ArrayList<>(Arrays.asList(
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

        // Добавляем NeoForge атрибуты
        attributesList.add(net.neoforged.neoforge.common.NeoForgeMod.SWIM_SPEED);

        for (Holder<net.minecraft.world.entity.ai.attributes.Attribute> attribute : attributesList) {
            AttributeInstance attr = entity.getAttribute(attribute);
            if (attr != null) {
                // Получаем все модификаторы
                java.util.Collection<AttributeModifier> allModifiers = new java.util.ArrayList<>(attr.getModifiers());

                // Удаляем каждый модификатор
                for (AttributeModifier modifier : allModifiers) {
                    // Если это модификатор нашего мода, удаляем его
                    if (modifier.id().toString().startsWith("mobscaler_") ||
                        modifier.id().equals(HEALTH_MODIFIER_ID) ||
                        modifier.id().equals(ARMOR_MODIFIER_ID) ||
                        modifier.id().equals(DAMAGE_MODIFIER_ID) ||
                        modifier.id().equals(SPEED_MODIFIER_ID) ||
                        modifier.id().equals(KNOCKBACK_RESISTANCE_ID) ||
                        modifier.id().equals(ATTACK_KNOCKBACK_ID) ||
                        modifier.id().equals(ATTACK_SPEED_ID) ||
                        modifier.id().equals(FOLLOW_RANGE_ID) ||
                        modifier.id().equals(FLYING_SPEED_ID) ||
                        modifier.id().equals(SWIM_SPEED_ID) ||
                        modifier.id().equals(REACH_DISTANCE_ID) ||
                        modifier.id().equals(ENTITY_REACH_ID)) {

                        attr.removeModifier(modifier.id());

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
            AttributeInstance attr = entity.getAttribute(net.neoforged.neoforge.common.NeoForgeMod.SWIM_SPEED);
            if (attr != null) {
                double base = attr.getBaseValue();
                double newValue = (base + addition) * multiplier * difficultyMultiplier;


                if (attr.getModifier(SWIM_SPEED_ID) != null) {
                    attr.removeModifier(SWIM_SPEED_ID);
                }
                attr.addPermanentModifier(createModifier(SWIM_SPEED_ID, "swim_speed", newValue - base));
            }
        } catch (Exception e) {
            LOGGER.error("Error applying swim speed modifier", e);
        }
    }

} 