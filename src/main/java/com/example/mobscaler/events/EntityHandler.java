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
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.List;

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

    // Храним последнее состояние ночи для каждого измерения
    private static final Map<String, Boolean> lastNightState = new HashMap<>();

    @SubscribeEvent
    public static void onEntitySpawn(EntityJoinLevelEvent event) {
        if (!event.getLevel().isClientSide() && event.getEntity() instanceof LivingEntity entity) {
            Level world = event.getLevel();
            ResourceLocation dimensionId = world.dimension().location();
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
            
            boolean isNight = isNight(world);
            
            // Проверяем состояние ночи для каждого измерения
            Boolean lastState = lastNightState.get(dimKey);
            
            // Если это первый тик или состояние изменилось
            if (lastState == null || lastState != isNight) {
                lastNightState.put(dimKey, isNight);
                
                // Обновляем модификаторы для всех игроков в мире
                for (Player player : world.players()) {
                    handlePlayerModifiers(player, dimKey, isNight);
                }
                
                // Обновляем модификаторы для мобов
                double size = world.getWorldBorder().getSize() / 2;
                Vec3 center = new Vec3(world.getWorldBorder().getCenterX(), world.getMaxBuildHeight() / 2.0, world.getWorldBorder().getCenterZ());
                AABB worldBounds = new AABB(
                    center.x - size, 0, center.z - size,
                    center.x + size, world.getMaxBuildHeight(), center.z + size
                );
                
                double healthMultiplier = getDifficultyMultiplier(world.getDifficulty(), true);
                double damageMultiplier = getDifficultyMultiplier(world.getDifficulty(), false);
                
                for (LivingEntity entity : world.getEntitiesOfClass(LivingEntity.class, worldBounds)) {
                    if (!(entity instanceof Player)) {
                        handleMobModifiers(entity, world, dimKey, isNight, healthMultiplier, damageMultiplier);
                    }
                }
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

    public static boolean isNight(Level world) {
        long time = world.getDayTime();
        return time >= 13000 && time < 23000;
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
        LOGGER.debug("Handling player modifiers for {} in dimension {}, isNight: {}", 
            player.getName().getString(), dimKey, isNight);
            
        if (isPlayerBlocked(player, dimKey)) {
            LOGGER.debug("Player {} is blocked in dimension {}", player.getName().getString(), dimKey);
            return;
        }
        
        // Получаем модификаторы игрока
        PlayerModifiers playerMods = PlayerConfigManager.getPlayerConfig().getModifiersForDimension(dimKey);
        LOGGER.debug("Player modifiers: {}, Night scaling enabled: {}", 
            playerMods != null, playerMods != null ? playerMods.isNightScalingEnabled() : false);
            
        if (playerMods == null) {
            LOGGER.warn("No player modifiers found for dimension {}", dimKey);
            return;
        }
        
        // Удаляем все существующие модификаторы
        removeAllModifiers(player);
        
        // Проверяем время суток и применяем соответствующие модификаторы
        if (isNight) {
            if (playerMods.isNightScalingEnabled()) {
                LOGGER.debug("Applying night modifiers to player {}", player.getName().getString());
                applyPlayerModifiers(player, playerMods, true);
            } else {
                LOGGER.debug("Night scaling disabled, applying day modifiers to player {}", player.getName().getString());
                applyPlayerModifiers(player, playerMods, false);
            }
        } else {
            LOGGER.debug("Applying day modifiers to player {}", player.getName().getString());
            applyPlayerModifiers(player, playerMods, false);
        }
    }

    private static void applyPlayerModifiers(Player player, PlayerModifiers modifiers, boolean isNight) {
        if (player == null || modifiers == null) return;

        // Применяем модификаторы здоровья
        applyHealthModifier(player, 
            isNight ? modifiers.getNightHealthAddition() : modifiers.getHealthAddition(),
            isNight ? modifiers.getNightHealthMultiplier() : modifiers.getHealthMultiplier(),
            1.0);

        // Применяем модификаторы брони
        applyArmorModifier(player,
            isNight ? modifiers.getNightArmorAddition() : modifiers.getArmorAddition(),
            isNight ? modifiers.getNightArmorMultiplier() : modifiers.getArmorMultiplier(),
            1.0);

        // Применяем модификаторы урона
        applyDamageModifier(player,
            isNight ? modifiers.getNightDamageAddition() : modifiers.getDamageAddition(),
            isNight ? modifiers.getNightDamageMultiplier() : modifiers.getDamageMultiplier(),
            1.0);

        // Применяем модификаторы скорости
        applySpeedModifier(player,
            isNight ? modifiers.getNightSpeedAddition() : modifiers.getSpeedAddition(),
            isNight ? modifiers.getNightSpeedMultiplier() : modifiers.getSpeedMultiplier(),
            1.0);

        // Применяем модификаторы сопротивления отбрасыванию
        applyKnockbackResistanceModifier(player,
            isNight ? modifiers.getNightKnockbackResistanceAddition() : modifiers.getKnockbackResistanceAddition(),
            isNight ? modifiers.getNightKnockbackResistanceMultiplier() : modifiers.getKnockbackResistanceMultiplier(),
            1.0);

        // Применяем модификаторы отбрасывания при атаке
        applyAttackKnockbackModifier(player,
            isNight ? modifiers.getNightAttackKnockbackAddition() : modifiers.getAttackKnockbackAddition(),
            isNight ? modifiers.getNightAttackKnockbackMultiplier() : modifiers.getAttackKnockbackMultiplier(),
            1.0);

        // Применяем модификаторы скорости атаки
        applyAttackSpeedModifier(player,
            isNight ? modifiers.getNightAttackSpeedAddition() : modifiers.getAttackSpeedAddition(),
            isNight ? modifiers.getNightAttackSpeedMultiplier() : modifiers.getAttackSpeedMultiplier(),
            1.0);

        // Применяем модификаторы дальности следования
        applyFollowRangeModifier(player,
            isNight ? modifiers.getNightFollowRangeAddition() : modifiers.getFollowRangeAddition(),
            isNight ? modifiers.getNightFollowRangeMultiplier() : modifiers.getFollowRangeMultiplier(),
            1.0);

        // Применяем модификаторы скорости полета
        applyFlyingSpeedModifier(player,
            isNight ? modifiers.getNightFlyingSpeedAddition() : modifiers.getFlyingSpeedAddition(),
            isNight ? modifiers.getNightFlyingSpeedMultiplier() : modifiers.getFlyingSpeedMultiplier(),
            1.0);
    }

    public static void handleMobModifiers(LivingEntity entity, Level world, String dimKey, boolean isNight, double healthMultiplier, double damageMultiplier) {
        ResourceLocation entityId = EntityType.getKey(entity.getType());
        
        // Получаем конфигурацию измерения
        DimensionConfig dimConfig = MobScalerConfig.DIMENSIONS.get(dimKey);
        
        if (dimConfig == null) {
            return;
        }

        // Проверяем черный список для измерения
        if (isEntityBlocked(dimConfig, entityId)) {
            return;
        }

        // Получаем пещерную конфигурацию
        CaveConfig caveConfig = CaveConfigManager.getCaveConfigs().get(dimKey);
        boolean isCaveModeEnabled = caveConfig != null && caveConfig.isCaveModeEnabled();

        // Удаляем все существующие модификаторы ПОСЛЕ всех проверок
        removeAllModifiers(entity);

        // Проверяем, находится ли моб в пещере
        if (isCaveModeEnabled) {
            double entityY = entity.getY();
            double caveHeight = caveConfig.getCaveHeight();

            if (entityY <= caveHeight && !isEntityBlocked(caveConfig, entityId)) {
                applyCaveModifiers(entity, caveConfig, healthMultiplier, damageMultiplier);
                return;
            }
        }

        // Применяем модификаторы измерения
        applyDimensionModifiers(entity, dimConfig, isNight, healthMultiplier, damageMultiplier);
    }

    private static void applyDimensionModifiers(LivingEntity entity, DimensionConfig dimConfig, boolean isNight, double healthMultiplier, double damageMultiplier) {
        if (isNight && dimConfig.isNightScalingEnabled()) {
            // Применяем ночные модификаторы
            applyHealthModifier(entity, dimConfig.getNightHealthAddition(), dimConfig.getNightHealthMultiplier(), healthMultiplier);
            applyArmorModifier(entity, dimConfig.getNightArmorAddition(), dimConfig.getNightArmorMultiplier(), healthMultiplier);
            applyDamageModifier(entity, dimConfig.getNightDamageAddition(), dimConfig.getNightDamageMultiplier(), damageMultiplier);
            applySpeedModifier(entity, dimConfig.getNightSpeedAddition(), dimConfig.getNightSpeedMultiplier(), damageMultiplier);
            applyKnockbackResistanceModifier(entity, dimConfig.getNightKnockbackResistanceAddition(), dimConfig.getNightKnockbackResistanceMultiplier(), healthMultiplier);
            applyAttackKnockbackModifier(entity, dimConfig.getNightAttackKnockbackAddition(), dimConfig.getNightAttackKnockbackMultiplier(), damageMultiplier);
            applyAttackSpeedModifier(entity, dimConfig.getNightAttackSpeedAddition(), dimConfig.getNightAttackSpeedMultiplier(), damageMultiplier);
            applyFollowRangeModifier(entity, dimConfig.getNightFollowRangeAddition(), dimConfig.getNightFollowRangeMultiplier(), damageMultiplier);
            applyFlyingSpeedModifier(entity, dimConfig.getNightFlyingSpeedAddition(), dimConfig.getNightFlyingSpeedMultiplier(), damageMultiplier);
        } else {
            // Применяем дневные модификаторы напрямую
            applyHealthModifier(entity, dimConfig.getHealthAddition(), dimConfig.getHealthMultiplier(), healthMultiplier);
            applyArmorModifier(entity, dimConfig.getArmorAddition(), dimConfig.getArmorMultiplier(), healthMultiplier);
            applyDamageModifier(entity, dimConfig.getDamageAddition(), dimConfig.getDamageMultiplier(), damageMultiplier);
            applySpeedModifier(entity, dimConfig.getSpeedAddition(), dimConfig.getSpeedMultiplier(), damageMultiplier);
            applyKnockbackResistanceModifier(entity, dimConfig.getKnockbackResistanceAddition(), dimConfig.getKnockbackResistanceMultiplier(), healthMultiplier);
            applyAttackKnockbackModifier(entity, dimConfig.getAttackKnockbackAddition(), dimConfig.getAttackKnockbackMultiplier(), damageMultiplier);
            applyAttackSpeedModifier(entity, dimConfig.getAttackSpeedAddition(), dimConfig.getAttackSpeedMultiplier(), damageMultiplier);
            applyFollowRangeModifier(entity, dimConfig.getFollowRangeAddition(), dimConfig.getFollowRangeMultiplier(), damageMultiplier);
            applyFlyingSpeedModifier(entity, dimConfig.getFlyingSpeedAddition(), dimConfig.getFlyingSpeedMultiplier(), damageMultiplier);
        }
    }

    private static void applyHealthModifier(LivingEntity entity, double addition, double multiplier, double difficultyMultiplier) {
        AttributeInstance attr = entity.getAttribute(Attributes.MAX_HEALTH);
        if (attr != null) {
            double base = attr.getBaseValue();
            double newMax = (base + addition) * multiplier * difficultyMultiplier;
            LOGGER.debug("Health: base={}, addition={}, multiplier={}, difficulty={}, new={}", 
                base, addition, multiplier, difficultyMultiplier, newMax);
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
        // Убираем повторное удаление модификаторов
        applyHealthModifier(entity, config.getHealthAddition(), config.getHealthMultiplier(), healthMultiplier);
        applyArmorModifier(entity, config.getArmorAddition(), config.getArmorMultiplier(), healthMultiplier);
        applyDamageModifier(entity, config.getDamageAddition(), config.getDamageMultiplier(), damageMultiplier);
        applySpeedModifier(entity, config.getSpeedAddition(), config.getSpeedMultiplier(), damageMultiplier);
        applyKnockbackResistanceModifier(entity, config.getKnockbackResistanceAddition(), config.getKnockbackResistanceMultiplier(), healthMultiplier);
        applyAttackKnockbackModifier(entity, config.getAttackKnockbackAddition(), config.getAttackKnockbackMultiplier(), damageMultiplier);
        applyAttackSpeedModifier(entity, config.getAttackSpeedAddition(), config.getAttackSpeedMultiplier(), damageMultiplier);
        applyFollowRangeModifier(entity, config.getFollowRangeAddition(), config.getFollowRangeMultiplier(), damageMultiplier);
        applyFlyingSpeedModifier(entity, config.getFlyingSpeedAddition(), config.getFlyingSpeedMultiplier(), damageMultiplier);
    }

    private static void removeAllModifiers(LivingEntity entity) {
        removeModifier(entity, Attributes.MAX_HEALTH, HEALTH_MODIFIER_UUID);
        removeModifier(entity, Attributes.ARMOR, ARMOR_MODIFIER_UUID);
        removeModifier(entity, Attributes.ATTACK_DAMAGE, DAMAGE_MODIFIER_UUID);
        removeModifier(entity, Attributes.MOVEMENT_SPEED, SPEED_MODIFIER_UUID);
        removeModifier(entity, Attributes.KNOCKBACK_RESISTANCE, KNOCKBACK_RESISTANCE_UUID);
        removeModifier(entity, Attributes.ATTACK_KNOCKBACK, ATTACK_KNOCKBACK_UUID);
        removeModifier(entity, Attributes.ATTACK_SPEED, ATTACK_SPEED_UUID);
        removeModifier(entity, Attributes.FOLLOW_RANGE, FOLLOW_RANGE_UUID);
        removeModifier(entity, Attributes.FLYING_SPEED, FLYING_SPEED_UUID);
    }

    private static void removeModifier(LivingEntity entity, net.minecraft.world.entity.ai.attributes.Attribute attribute, UUID modifierId) {
        AttributeInstance attr = entity.getAttribute(attribute);
        if (attr != null && attr.getModifier(modifierId) != null) {
            attr.removeModifier(modifierId);
        }
    }
}
