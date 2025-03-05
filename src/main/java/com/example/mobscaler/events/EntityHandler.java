package com.example.mobscaler.events;

import com.example.mobscaler.config.DimensionConfig;
import com.example.mobscaler.config.MobScalerConfig;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.ArrayList;
import java.util.UUID;

public class EntityHandler {
    private static final UUID HEALTH_MODIFIER_UUID = UUID.fromString("a9c8745e-1234-5678-90ab-cdef12345678");
    private static final UUID ARMOR_MODIFIER_UUID = UUID.fromString("b8d7654f-4321-5678-90ab-cdef654321ba");
    private static final UUID DAMAGE_MODIFIER_UUID = UUID.fromString("c3d9f8a1-2468-1357-9abc-def456789012");

    @SubscribeEvent
    public static void onEntitySpawn(EntityJoinLevelEvent event) {
        if (!event.getLevel().isClientSide() && event.getEntity() instanceof LivingEntity entity) {
            if (entity instanceof net.minecraft.world.entity.player.Player) return;
            
            Level world = event.getLevel();
            ResourceLocation dimensionId = world.dimension().location();
            String dimKey = dimensionId.toString();
            
            DimensionConfig config = MobScalerConfig.DIMENSIONS.get(dimKey);
            if (config == null) {
                config = new DimensionConfig(
                    0.0, 1.0,
                    0.0, 1.0,
                    0.0, 1.0,
                    new ArrayList<>(),
                    new ArrayList<>()
                );
                MobScalerConfig.DIMENSIONS.put(dimKey, config);
            }
            
            ResourceLocation entityId = EntityType.getKey(entity.getType());
            if (isEntityBlocked(config, entityId)) return;
            
            double healthMultiplier = getDifficultyMultiplier(world.getDifficulty(), true);
            double damageMultiplier = getDifficultyMultiplier(world.getDifficulty(), false);
            
            applyHealthModifier(entity, config, healthMultiplier);
            applyArmorModifier(entity, config, healthMultiplier);
            applyDamageModifier(entity, config, damageMultiplier);
        }
    }
    
    private static boolean isEntityBlocked(DimensionConfig config, ResourceLocation entityId) {
        return config.getModBlacklist().contains(entityId.getNamespace()) ||
               config.getEntityBlacklist().contains(entityId.toString());
    }

    private static double getDifficultyMultiplier(Difficulty difficulty, boolean isHealth) {
        return switch (difficulty) {
            case PEACEFUL -> isHealth ? MobScalerConfig.HEALTH_PEACEFUL.get() : MobScalerConfig.DAMAGE_PEACEFUL.get();
            case EASY -> isHealth ? MobScalerConfig.HEALTH_EASY.get() : MobScalerConfig.DAMAGE_EASY.get();
            case NORMAL -> isHealth ? MobScalerConfig.HEALTH_NORMAL.get() : MobScalerConfig.DAMAGE_NORMAL.get();
            case HARD -> isHealth ? MobScalerConfig.HEALTH_HARD.get() : MobScalerConfig.DAMAGE_HARD.get();
        };
    }
    

    private static void applyHealthModifier(LivingEntity entity, DimensionConfig config, double multiplier) {
        AttributeInstance attr = entity.getAttribute(Attributes.MAX_HEALTH);
        if (attr != null) {
            double base = attr.getBaseValue();
            double newMax = (base + config.getHealthAddition()) * config.getHealthMultiplier() * multiplier;
            if (attr.getModifier(HEALTH_MODIFIER_UUID) != null) {
                attr.removeModifier(HEALTH_MODIFIER_UUID);
            }
            attr.addPermanentModifier(createModifier(HEALTH_MODIFIER_UUID, "health", newMax - base));
            entity.setHealth((float)newMax);
        }
    }
    
    private static void applyArmorModifier(LivingEntity entity, DimensionConfig config, double multiplier) {
        AttributeInstance attr = entity.getAttribute(Attributes.ARMOR);
        if (attr != null) {
            double base = attr.getBaseValue();
            double newValue = (base + config.getArmorAddition()) * config.getArmorMultiplier() * multiplier;
            if (attr.getModifier(ARMOR_MODIFIER_UUID) != null) {
                attr.removeModifier(ARMOR_MODIFIER_UUID);
            }
            attr.addPermanentModifier(createModifier(ARMOR_MODIFIER_UUID, "armor", newValue - base));
        }
    }
    
    private static void applyDamageModifier(LivingEntity entity, DimensionConfig config, double multiplier) {
        AttributeInstance attr = entity.getAttribute(Attributes.ATTACK_DAMAGE);
        if (attr != null) {
            double base = attr.getBaseValue();
            double newValue = (base + config.getDamageAddition()) * config.getDamageMultiplier() * multiplier;
            if (attr.getModifier(DAMAGE_MODIFIER_UUID) != null) {
                attr.removeModifier(DAMAGE_MODIFIER_UUID);
            }
            attr.addPermanentModifier(createModifier(DAMAGE_MODIFIER_UUID, "damage", newValue - base));
        }
    }
    
    private static AttributeModifier createModifier(UUID uuid, String name, double value) {
        return new AttributeModifier(uuid, "mobscaler_" + name, value, AttributeModifier.Operation.ADDITION);
    }
}
