package com.example.mobscaler.events;

import com.example.mobscaler.config.DimensionConfig;
import com.example.mobscaler.config.MobScalerConfig;
import com.example.mobscaler.config.PlayerConfigManager;
import com.example.mobscaler.config.PlayerConfig.PlayerModifiers;
import com.example.mobscaler.config.CaveConfigManager;
import com.example.mobscaler.config.CaveConfig;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.List;

public class EntityHandler {
    private static final UUID HEALTH_MODIFIER_UUID = UUID.fromString("a9c8745e-1234-5678-90ab-cdef12345678");
    private static final UUID ARMOR_MODIFIER_UUID = UUID.fromString("b8d7654f-4321-5678-90ab-cdef654321ba");
    private static final UUID DAMAGE_MODIFIER_UUID = UUID.fromString("c3d9f8a1-2468-1357-9abc-def456789012");
    private static final UUID SPEED_MODIFIER_UUID = UUID.fromString("d2e4f6a8-3579-2468-90ab-cdef13579024");
    private static final UUID KNOCKBACK_RESISTANCE_UUID = UUID.fromString("e1f3d5b7-9753-1357-90ab-cdef24680135");
    private static final UUID ATTACK_KNOCKBACK_UUID = UUID.fromString("f0e2d4c6-8642-0246-90ab-cdef35791246");
    private static final UUID ATTACK_SPEED_UUID = UUID.fromString("a1b3c5d7-7531-9753-90ab-cdef46802357");
    private static final UUID FOLLOW_RANGE_UUID = UUID.fromString("b2c4d6e8-6420-8642-90ab-cdef57913468");
    private static final UUID FLYING_SPEED_UUID = UUID.fromString("c3d5e7f9-5319-7531-90ab-cdef68024579");

    // Храним последнее состояние ночи для каждого измерения
    private static final Map<String, Boolean> lastNightState = new HashMap<>();

    @SubscribeEvent
    public static void onEntitySpawn(EntityJoinLevelEvent event) {
        if (!event.getLevel().isClientSide() && event.getEntity() instanceof LivingEntity entity) {
            Level world = event.getLevel();
            ResourceLocation dimensionId = world.dimension().location();
            String dimKey = dimensionId.toString();
            
            if (entity instanceof Player player) {
                if (isPlayerBlocked(player, dimKey)) {
                    return;
                }
                // Применяем модификаторы для игрока
                PlayerModifiers playerMods = PlayerConfigManager.getPlayerConfig().getModifiersForDimension(dimKey);
                boolean isNight = isNight(world);
                if (isNight && playerMods.isNightScalingEnabled()) {
                    applyNightModifiers(entity, playerMods, 1.0, 1.0);
                } else {
                    applyDayModifiers(entity, playerMods, 1.0, 1.0);
                }
            } else {
                double healthMultiplier = getDifficultyMultiplier(world.getDifficulty(), true);
                double damageMultiplier = getDifficultyMultiplier(world.getDifficulty(), false);
                
                // Проверяем пещерные усиления
                CaveConfig caveConfig = CaveConfigManager.getCaveConfigs().get(dimKey);
                if (caveConfig != null && caveConfig.isCaveModeEnabled()) {
                    double entityY = entity.getY();
                    double caveHeight = caveConfig.getCaveHeight();
                    
                    // Проверяем, находится ли сущность в пещере
                    if (entityY <= caveHeight) {
                        // Проверяем, не заблокирована ли сущность
                        ResourceLocation entityId = EntityType.getKey(entity.getType());
                        if (!isEntityBlocked(caveConfig, entityId)) {
                            // Применяем пещерные модификаторы с учетом сложности
                            applyCaveModifiers(entity, caveConfig, healthMultiplier, damageMultiplier);
                            return; // Пропускаем обычные модификаторы
                        }
                    }
                }
                
                // Применяем обычные модификаторы для мобов
                DimensionConfig config = MobScalerConfig.DIMENSIONS.get(dimKey);
                if (config == null) {
                    config = new DimensionConfig(
                        false, // enableNightScaling
                        // Дневные настройки
                        0.0, 1.0, // health
                        0.0, 1.0, // armor
                        0.0, 1.0, // damage
                        0.0, 1.0, // speed
                        0.0, 1.0, // knockback resistance
                        0.0, 1.0, // attack knockback
                        0.0, 1.0, // attack speed
                        0.0, 1.0, // follow range
                        0.0, 1.0, // flying speed
                        // Ночные настройки
                        0.0, 1.0, // night health
                        0.0, 1.0, // night armor
                        0.0, 1.0, // night damage
                        0.0, 1.0, // night speed
                        0.0, 1.0, // night knockback resistance
                        0.0, 1.0, // night attack knockback
                        0.0, 1.0, // night attack speed
                        0.0, 1.0, // night follow range
                        0.0, 1.0, // night flying speed
                        new ArrayList<>(),
                        new ArrayList<>()
                    );
                    MobScalerConfig.DIMENSIONS.put(dimKey, config);
                }
                
                ResourceLocation entityId = EntityType.getKey(entity.getType());
                if (isEntityBlocked(config, entityId)) return;
                
                boolean isNight = isNight(world);
                if (isNight && config.isNightScalingEnabled()) {
                    applyNightModifiers(entity, config, healthMultiplier, damageMultiplier);
                } else {
                    applyDayModifiers(entity, config, healthMultiplier, damageMultiplier);
                }
            }
        }
    }

    @SubscribeEvent
    public static void onWorldTick(TickEvent.LevelTickEvent event) {
        if (!event.level.isClientSide() && event.phase == TickEvent.Phase.END) {
            Level world = event.level;
            ResourceLocation dimensionId = world.dimension().location();
            String dimKey = dimensionId.toString();
            
            // Проверяем, изменилось ли состояние ночи
            Boolean lastState = lastNightState.get(dimKey);
            boolean currentState = isNight(world);
            
            if (lastState != null && lastState != currentState) {
                // Обновляем атрибуты игроков только при изменении состояния ночи
                for (Player player : world.players()) {
                    if (!isPlayerBlocked(player, dimKey)) {
                        PlayerModifiers playerMods = PlayerConfigManager.getPlayerConfig().getModifiersForDimension(dimKey);
                        if (currentState && playerMods.isNightScalingEnabled()) {
                            applyNightModifiers(player, playerMods, 1.0, 1.0);
                        } else {
                            applyDayModifiers(player, playerMods, 1.0, 1.0);
                        }
                    }
                }
                // Обновляем состояние
                lastNightState.put(dimKey, currentState);
            }
        }
    }
    
    private static boolean isPlayerBlocked(Player player, String dimensionId) {
        return PlayerConfigManager.getPlayerConfig().isPlayerBlocked(player.getName().getString(), dimensionId);
    }

    private static boolean isEntityBlocked(Object config, ResourceLocation entityId) {
        List<String> modBlacklist;
        List<String> entityBlacklist;
        
        if (config instanceof DimensionConfig dimConfig) {
            modBlacklist = dimConfig.getModBlacklist();
            entityBlacklist = dimConfig.getEntityBlacklist();
        } else if (config instanceof CaveConfig caveConfig) {
            modBlacklist = caveConfig.getModBlacklist();
            entityBlacklist = caveConfig.getEntityBlacklist();
        } else {
            return false;
        }
        
        String modId = entityId.getNamespace();
        String entityIdStr = entityId.toString();
        
        // Проверяем, не находится ли мод или сущность в черном списке
        boolean isModBlocked = modBlacklist.contains(modId);
        boolean isEntityBlocked = entityBlacklist.contains(entityIdStr);
        
        return isModBlocked || isEntityBlocked;
    }

    private static boolean isNight(Level world) {
        long time = world.getDayTime();
        return time >= 13000 && time < 23000;
    }

    private static double getDifficultyMultiplier(Difficulty difficulty, boolean isHealth) {
        return switch (difficulty) {
            case PEACEFUL -> isHealth ? MobScalerConfig.HEALTH_PEACEFUL.get() : MobScalerConfig.DAMAGE_PEACEFUL.get();
            case EASY -> isHealth ? MobScalerConfig.HEALTH_EASY.get() : MobScalerConfig.DAMAGE_EASY.get();
            case NORMAL -> isHealth ? MobScalerConfig.HEALTH_NORMAL.get() : MobScalerConfig.DAMAGE_NORMAL.get();
            case HARD -> isHealth ? MobScalerConfig.HEALTH_HARD.get() : MobScalerConfig.DAMAGE_HARD.get();
        };
    }

    private static void applyDayModifiers(LivingEntity entity, Object config, double healthMultiplier, double damageMultiplier) {
        if (config instanceof DimensionConfig dimConfig) {
            applyHealthModifier(entity, dimConfig.getHealthAddition(), dimConfig.getHealthMultiplier(), healthMultiplier);
            applyArmorModifier(entity, dimConfig.getArmorAddition(), dimConfig.getArmorMultiplier(), healthMultiplier);
            applyDamageModifier(entity, dimConfig.getDamageAddition(), dimConfig.getDamageMultiplier(), damageMultiplier);
            applySpeedModifier(entity, dimConfig.getSpeedAddition(), dimConfig.getSpeedMultiplier(), damageMultiplier);
            applyKnockbackResistanceModifier(entity, dimConfig.getKnockbackResistanceAddition(), dimConfig.getKnockbackResistanceMultiplier(), healthMultiplier);
            applyAttackKnockbackModifier(entity, dimConfig.getAttackKnockbackAddition(), dimConfig.getAttackKnockbackMultiplier(), damageMultiplier);
            applyAttackSpeedModifier(entity, dimConfig.getAttackSpeedAddition(), dimConfig.getAttackSpeedMultiplier(), damageMultiplier);
            applyFollowRangeModifier(entity, dimConfig.getFollowRangeAddition(), dimConfig.getFollowRangeMultiplier(), damageMultiplier);
            applyFlyingSpeedModifier(entity, dimConfig.getFlyingSpeedAddition(), dimConfig.getFlyingSpeedMultiplier(), damageMultiplier);
        } else if (config instanceof PlayerModifiers playerMods) {
            applyHealthModifier(entity, playerMods.getHealthAddition(), playerMods.getHealthMultiplier(), healthMultiplier);
            applyArmorModifier(entity, playerMods.getArmorAddition(), playerMods.getArmorMultiplier(), healthMultiplier);
            applyDamageModifier(entity, playerMods.getDamageAddition(), playerMods.getDamageMultiplier(), damageMultiplier);
            applySpeedModifier(entity, playerMods.getSpeedAddition(), playerMods.getSpeedMultiplier(), damageMultiplier);
            applyKnockbackResistanceModifier(entity, playerMods.getKnockbackResistanceAddition(), playerMods.getKnockbackResistanceMultiplier(), healthMultiplier);
            applyAttackKnockbackModifier(entity, playerMods.getAttackKnockbackAddition(), playerMods.getAttackKnockbackMultiplier(), damageMultiplier);
            applyAttackSpeedModifier(entity, playerMods.getAttackSpeedAddition(), playerMods.getAttackSpeedMultiplier(), damageMultiplier);
            applyFollowRangeModifier(entity, playerMods.getFollowRangeAddition(), playerMods.getFollowRangeMultiplier(), damageMultiplier);
            applyFlyingSpeedModifier(entity, playerMods.getFlyingSpeedAddition(), playerMods.getFlyingSpeedMultiplier(), damageMultiplier);
        }
    }

    private static void applyNightModifiers(LivingEntity entity, Object config, double healthMultiplier, double damageMultiplier) {
        if (config instanceof DimensionConfig dimConfig) {
            applyHealthModifier(entity, dimConfig.getNightHealthAddition(), dimConfig.getNightHealthMultiplier(), healthMultiplier);
            applyArmorModifier(entity, dimConfig.getNightArmorAddition(), dimConfig.getNightArmorMultiplier(), healthMultiplier);
            applyDamageModifier(entity, dimConfig.getNightDamageAddition(), dimConfig.getNightDamageMultiplier(), damageMultiplier);
            applySpeedModifier(entity, dimConfig.getNightSpeedAddition(), dimConfig.getNightSpeedMultiplier(), damageMultiplier);
            applyKnockbackResistanceModifier(entity, dimConfig.getNightKnockbackResistanceAddition(), dimConfig.getNightKnockbackResistanceMultiplier(), healthMultiplier);
            applyAttackKnockbackModifier(entity, dimConfig.getNightAttackKnockbackAddition(), dimConfig.getNightAttackKnockbackMultiplier(), damageMultiplier);
            applyAttackSpeedModifier(entity, dimConfig.getNightAttackSpeedAddition(), dimConfig.getNightAttackSpeedMultiplier(), damageMultiplier);
            applyFollowRangeModifier(entity, dimConfig.getNightFollowRangeAddition(), dimConfig.getNightFollowRangeMultiplier(), damageMultiplier);
            applyFlyingSpeedModifier(entity, dimConfig.getNightFlyingSpeedAddition(), dimConfig.getNightFlyingSpeedMultiplier(), damageMultiplier);
        } else if (config instanceof PlayerModifiers playerMods) {
            applyHealthModifier(entity, playerMods.getNightHealthAddition(), playerMods.getNightHealthMultiplier(), healthMultiplier);
            applyArmorModifier(entity, playerMods.getNightArmorAddition(), playerMods.getNightArmorMultiplier(), healthMultiplier);
            applyDamageModifier(entity, playerMods.getNightDamageAddition(), playerMods.getNightDamageMultiplier(), damageMultiplier);
            applySpeedModifier(entity, playerMods.getNightSpeedAddition(), playerMods.getNightSpeedMultiplier(), damageMultiplier);
            applyKnockbackResistanceModifier(entity, playerMods.getNightKnockbackResistanceAddition(), playerMods.getNightKnockbackResistanceMultiplier(), healthMultiplier);
            applyAttackKnockbackModifier(entity, playerMods.getNightAttackKnockbackAddition(), playerMods.getNightAttackKnockbackMultiplier(), damageMultiplier);
            applyAttackSpeedModifier(entity, playerMods.getNightAttackSpeedAddition(), playerMods.getNightAttackSpeedMultiplier(), damageMultiplier);
            applyFollowRangeModifier(entity, playerMods.getNightFollowRangeAddition(), playerMods.getNightFollowRangeMultiplier(), damageMultiplier);
            applyFlyingSpeedModifier(entity, playerMods.getNightFlyingSpeedAddition(), playerMods.getNightFlyingSpeedMultiplier(), damageMultiplier);
        }
    }

    private static void applyHealthModifier(LivingEntity entity, double addition, double multiplier, double difficultyMultiplier) {
        AttributeInstance attr = entity.getAttribute(Attributes.MAX_HEALTH);
        if (attr != null) {
            double base = attr.getBaseValue();
            double newMax = (base + addition) * multiplier * difficultyMultiplier;
            if (attr.getModifier(HEALTH_MODIFIER_UUID) != null) {
                attr.removeModifier(HEALTH_MODIFIER_UUID);
            }
            attr.addPermanentModifier(createModifier(HEALTH_MODIFIER_UUID, "health", newMax - base));
            entity.setHealth((float)newMax);
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
        return new AttributeModifier(uuid, "mobscaler_" + name, value, AttributeModifier.Operation.ADDITION);
    }

    private static void applyCaveModifiers(LivingEntity entity, CaveConfig config, double healthMultiplier, double damageMultiplier) {
        // Применяем усиленные модификаторы для пещер
        applyHealthModifier(entity, config.getHealthAddition(), config.getHealthMultiplier() * 1.5, healthMultiplier);
        applyArmorModifier(entity, config.getArmorAddition(), config.getArmorMultiplier() * 1.5, healthMultiplier);
        applyDamageModifier(entity, config.getDamageAddition(), config.getDamageMultiplier() * 1.5, damageMultiplier);
        applySpeedModifier(entity, config.getSpeedAddition(), config.getSpeedMultiplier(), damageMultiplier);
        applyKnockbackResistanceModifier(entity, config.getKnockbackResistanceAddition(), config.getKnockbackResistanceMultiplier(), healthMultiplier);
        applyAttackKnockbackModifier(entity, config.getAttackKnockbackAddition(), config.getAttackKnockbackMultiplier(), damageMultiplier);
        applyAttackSpeedModifier(entity, config.getAttackSpeedAddition(), config.getAttackSpeedMultiplier(), damageMultiplier);
        applyFollowRangeModifier(entity, config.getFollowRangeAddition(), config.getFollowRangeMultiplier(), damageMultiplier);
        applyFlyingSpeedModifier(entity, config.getFlyingSpeedAddition(), config.getFlyingSpeedMultiplier(), damageMultiplier);
    }
}
