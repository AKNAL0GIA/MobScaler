package com.example.mobscaler.config;

import com.google.gson.annotations.SerializedName;

public class IndividualMobAttributes {
    // Настройки
    @SerializedName("enableNightScaling")
    private final boolean enableNightScaling;
    @SerializedName("enableCaveScaling")
    private final boolean enableCaveScaling;
    @SerializedName("caveHeight")
    private final double caveHeight;
    @SerializedName("gravityMultiplier")
    private final double gravityMultiplier;
    @SerializedName("enableGravity")
    private final boolean enableGravity;


    // Обычные атрибуты
    @SerializedName("healthAddition")
    private final double healthAddition;
    @SerializedName("healthMultiplier")
    private final double healthMultiplier;
    @SerializedName("armorAddition")
    private final double armorAddition;
    @SerializedName("armorMultiplier")
    private final double armorMultiplier;
    @SerializedName("damageAddition")
    private final double damageAddition;
    @SerializedName("damageMultiplier")
    private final double damageMultiplier;
    @SerializedName("speedAddition")
    private final double speedAddition;
    @SerializedName("speedMultiplier")
    private final double speedMultiplier;
    @SerializedName("knockbackResistanceAddition")
    private final double knockbackResistanceAddition;
    @SerializedName("knockbackResistanceMultiplier")
    private final double knockbackResistanceMultiplier;
    @SerializedName("attackKnockbackAddition")
    private final double attackKnockbackAddition;
    @SerializedName("attackKnockbackMultiplier")
    private final double attackKnockbackMultiplier;
    @SerializedName("attackSpeedAddition")
    private final double attackSpeedAddition;
    @SerializedName("attackSpeedMultiplier")
    private final double attackSpeedMultiplier;
    @SerializedName("followRangeAddition")
    private final double followRangeAddition;
    @SerializedName("followRangeMultiplier")
    private final double followRangeMultiplier;
    @SerializedName("flyingSpeedAddition")
    private final double flyingSpeedAddition;
    @SerializedName("flyingSpeedMultiplier")
    private final double flyingSpeedMultiplier;
    @SerializedName("armorToughnessAddition")
    private final double armorToughnessAddition;
    @SerializedName("armorToughnessMultiplier")
    private final double armorToughnessMultiplier;
    @SerializedName("luckAddition")
    private final double luckAddition;
    @SerializedName("luckMultiplier")
    private final double luckMultiplier;
    @SerializedName("swimSpeedAddition")
    private final double swimSpeedAddition;
    @SerializedName("swimSpeedMultiplier")
    private final double swimSpeedMultiplier;
    @SerializedName("reachDistanceAddition")
    private final double reachDistanceAddition;
    @SerializedName("reachDistanceMultiplier")
    private final double reachDistanceMultiplier;

    // Ночные атрибуты
    @SerializedName("nightHealthAddition")
    private final double nightHealthAddition;
    @SerializedName("nightHealthMultiplier")
    private final double nightHealthMultiplier;
    @SerializedName("nightArmorAddition")
    private final double nightArmorAddition;
    @SerializedName("nightArmorMultiplier")
    private final double nightArmorMultiplier;
    @SerializedName("nightDamageAddition")
    private final double nightDamageAddition;
    @SerializedName("nightDamageMultiplier")
    private final double nightDamageMultiplier;
    @SerializedName("nightSpeedAddition")
    private final double nightSpeedAddition;
    @SerializedName("nightSpeedMultiplier")
    private final double nightSpeedMultiplier;
    @SerializedName("nightKnockbackResistanceAddition")
    private final double nightKnockbackResistanceAddition;
    @SerializedName("nightKnockbackResistanceMultiplier")
    private final double nightKnockbackResistanceMultiplier;
    @SerializedName("nightAttackKnockbackAddition")
    private final double nightAttackKnockbackAddition;
    @SerializedName("nightAttackKnockbackMultiplier")
    private final double nightAttackKnockbackMultiplier;
    @SerializedName("nightAttackSpeedAddition")
    private final double nightAttackSpeedAddition;
    @SerializedName("nightAttackSpeedMultiplier")
    private final double nightAttackSpeedMultiplier;
    @SerializedName("nightFollowRangeAddition")
    private final double nightFollowRangeAddition;
    @SerializedName("nightFollowRangeMultiplier")
    private final double nightFollowRangeMultiplier;
    @SerializedName("nightFlyingSpeedAddition")
    private final double nightFlyingSpeedAddition;
    @SerializedName("nightFlyingSpeedMultiplier")
    private final double nightFlyingSpeedMultiplier;
    @SerializedName("nightArmorToughnessAddition")
    private final double nightArmorToughnessAddition;
    @SerializedName("nightArmorToughnessMultiplier")
    private final double nightArmorToughnessMultiplier;
    @SerializedName("nightLuckAddition")
    private final double nightLuckAddition;
    @SerializedName("nightLuckMultiplier")
    private final double nightLuckMultiplier;
    @SerializedName("nightSwimSpeedAddition")
    private final double nightSwimSpeedAddition;
    @SerializedName("nightSwimSpeedMultiplier")
    private final double nightSwimSpeedMultiplier;
    @SerializedName("nightReachDistanceAddition")
    private final double nightReachDistanceAddition;
    @SerializedName("nightReachDistanceMultiplier")
    private final double nightReachDistanceMultiplier;

    // Пещерные атрибуты
    @SerializedName("caveHealthAddition")
    private final double caveHealthAddition;
    @SerializedName("caveHealthMultiplier")
    private final double caveHealthMultiplier;
    @SerializedName("caveArmorAddition")
    private final double caveArmorAddition;
    @SerializedName("caveArmorMultiplier")
    private final double caveArmorMultiplier;
    @SerializedName("caveDamageAddition")
    private final double caveDamageAddition;
    @SerializedName("caveDamageMultiplier")
    private final double caveDamageMultiplier;
    @SerializedName("caveSpeedAddition")
    private final double caveSpeedAddition;
    @SerializedName("caveSpeedMultiplier")
    private final double caveSpeedMultiplier;
    @SerializedName("caveKnockbackResistanceAddition")
    private final double caveKnockbackResistanceAddition;
    @SerializedName("caveKnockbackResistanceMultiplier")
    private final double caveKnockbackResistanceMultiplier;
    @SerializedName("caveAttackKnockbackAddition")
    private final double caveAttackKnockbackAddition;
    @SerializedName("caveAttackKnockbackMultiplier")
    private final double caveAttackKnockbackMultiplier;
    @SerializedName("caveAttackSpeedAddition")
    private final double caveAttackSpeedAddition;
    @SerializedName("caveAttackSpeedMultiplier")
    private final double caveAttackSpeedMultiplier;
    @SerializedName("caveFollowRangeAddition")
    private final double caveFollowRangeAddition;
    @SerializedName("caveFollowRangeMultiplier")
    private final double caveFollowRangeMultiplier;
    @SerializedName("caveFlyingSpeedAddition")
    private final double caveFlyingSpeedAddition;
    @SerializedName("caveFlyingSpeedMultiplier")
    private final double caveFlyingSpeedMultiplier;
    @SerializedName("caveArmorToughnessAddition")
    private final double caveArmorToughnessAddition;
    @SerializedName("caveArmorToughnessMultiplier")
    private final double caveArmorToughnessMultiplier;
    @SerializedName("caveLuckAddition")
    private final double caveLuckAddition;
    @SerializedName("caveLuckMultiplier")
    private final double caveLuckMultiplier;
    @SerializedName("caveSwimSpeedAddition")
    private final double caveSwimSpeedAddition;
    @SerializedName("caveSwimSpeedMultiplier")
    private final double caveSwimSpeedMultiplier;
    @SerializedName("caveReachDistanceAddition")
    private final double caveReachDistanceAddition;
    @SerializedName("caveReachDistanceMultiplier")
    private final double caveReachDistanceMultiplier;

    private final boolean blacklisted;

    public IndividualMobAttributes(
            // Настройки
            boolean enableNightScaling, boolean enableCaveScaling, double caveHeight, double gravityMultiplier, boolean enableGravity,  
            // Обычные атрибуты
            double healthAddition, double healthMultiplier,
            double armorAddition, double armorMultiplier,
            double damageAddition, double damageMultiplier,
            double speedAddition, double speedMultiplier,
            double knockbackResistanceAddition, double knockbackResistanceMultiplier,
            double attackKnockbackAddition, double attackKnockbackMultiplier,
            double attackSpeedAddition, double attackSpeedMultiplier,
            double followRangeAddition, double followRangeMultiplier,
            double flyingSpeedAddition, double flyingSpeedMultiplier,
            double armorToughnessAddition, double armorToughnessMultiplier,
            double luckAddition, double luckMultiplier,
            double reachDistanceAddition, double reachDistanceMultiplier,
            double swimSpeedAddition, double swimSpeedMultiplier,
            // Ночные атрибуты
            double nightHealthAddition, double nightHealthMultiplier,
            double nightArmorAddition, double nightArmorMultiplier,
            double nightDamageAddition, double nightDamageMultiplier,
            double nightSpeedAddition, double nightSpeedMultiplier,
            double nightKnockbackResistanceAddition, double nightKnockbackResistanceMultiplier,
            double nightAttackKnockbackAddition, double nightAttackKnockbackMultiplier,
            double nightAttackSpeedAddition, double nightAttackSpeedMultiplier,
            double nightFollowRangeAddition, double nightFollowRangeMultiplier,
            double nightFlyingSpeedAddition, double nightFlyingSpeedMultiplier,
            double nightArmorToughnessAddition, double nightArmorToughnessMultiplier,
            double nightLuckAddition, double nightLuckMultiplier,
            double nightReachDistanceAddition, double nightReachDistanceMultiplier,
            double nightSwimSpeedAddition, double nightSwimSpeedMultiplier,
            // Пещерные атрибуты
            double caveHealthAddition, double caveHealthMultiplier,
            double caveArmorAddition, double caveArmorMultiplier,
            double caveDamageAddition, double caveDamageMultiplier,
            double caveSpeedAddition, double caveSpeedMultiplier,
            double caveKnockbackResistanceAddition, double caveKnockbackResistanceMultiplier,
            double caveAttackKnockbackAddition, double caveAttackKnockbackMultiplier,
            double caveAttackSpeedAddition, double caveAttackSpeedMultiplier,
            double caveFollowRangeAddition, double caveFollowRangeMultiplier,
            double caveFlyingSpeedAddition, double caveFlyingSpeedMultiplier,
            double caveArmorToughnessAddition, double caveArmorToughnessMultiplier,
            double caveLuckAddition, double caveLuckMultiplier,
            double caveReachDistanceAddition, double caveReachDistanceMultiplier,
            double caveSwimSpeedAddition, double caveSwimSpeedMultiplier,
            boolean blacklisted) {
        // Настройки
        this.enableNightScaling = enableNightScaling;
        this.enableCaveScaling = enableCaveScaling;
        this.caveHeight = caveHeight;
        this.gravityMultiplier = gravityMultiplier;
        this.enableGravity = enableGravity;
        // Обычные атрибуты
        this.healthAddition = healthAddition;
        this.healthMultiplier = healthMultiplier;
        this.armorAddition = armorAddition;
        this.armorMultiplier = armorMultiplier;
        this.damageAddition = damageAddition;
        this.damageMultiplier = damageMultiplier;
        this.speedAddition = speedAddition;
        this.speedMultiplier = speedMultiplier;
        this.knockbackResistanceAddition = knockbackResistanceAddition;
        this.knockbackResistanceMultiplier = knockbackResistanceMultiplier;
        this.attackKnockbackAddition = attackKnockbackAddition;
        this.attackKnockbackMultiplier = attackKnockbackMultiplier;
        this.attackSpeedAddition = attackSpeedAddition;
        this.attackSpeedMultiplier = attackSpeedMultiplier;
        this.followRangeAddition = followRangeAddition;
        this.followRangeMultiplier = followRangeMultiplier;
        this.flyingSpeedAddition = flyingSpeedAddition;
        this.flyingSpeedMultiplier = flyingSpeedMultiplier;
        this.armorToughnessAddition = armorToughnessAddition;
        this.armorToughnessMultiplier = armorToughnessMultiplier;
        this.luckAddition = luckAddition;
        this.luckMultiplier = luckMultiplier;
        this.reachDistanceAddition = reachDistanceAddition;
        this.reachDistanceMultiplier = reachDistanceMultiplier;
        this.swimSpeedAddition = swimSpeedAddition;
        this.swimSpeedMultiplier = swimSpeedMultiplier;

        // Ночные атрибуты
        this.nightHealthAddition = nightHealthAddition;
        this.nightHealthMultiplier = nightHealthMultiplier;
        this.nightArmorAddition = nightArmorAddition;
        this.nightArmorMultiplier = nightArmorMultiplier;
        this.nightDamageAddition = nightDamageAddition;
        this.nightDamageMultiplier = nightDamageMultiplier;
        this.nightSpeedAddition = nightSpeedAddition;
        this.nightSpeedMultiplier = nightSpeedMultiplier;
        this.nightKnockbackResistanceAddition = nightKnockbackResistanceAddition;
        this.nightKnockbackResistanceMultiplier = nightKnockbackResistanceMultiplier;
        this.nightAttackKnockbackAddition = nightAttackKnockbackAddition;
        this.nightAttackKnockbackMultiplier = nightAttackKnockbackMultiplier;
        this.nightAttackSpeedAddition = nightAttackSpeedAddition;
        this.nightAttackSpeedMultiplier = nightAttackSpeedMultiplier;
        this.nightFollowRangeAddition = nightFollowRangeAddition;
        this.nightFollowRangeMultiplier = nightFollowRangeMultiplier;
        this.nightFlyingSpeedAddition = nightFlyingSpeedAddition;
        this.nightFlyingSpeedMultiplier = nightFlyingSpeedMultiplier;
        this.nightArmorToughnessAddition = nightArmorToughnessAddition;
        this.nightArmorToughnessMultiplier = nightArmorToughnessMultiplier;
        this.nightLuckAddition = nightLuckAddition;
        this.nightLuckMultiplier = nightLuckMultiplier;
        this.nightReachDistanceAddition = nightReachDistanceAddition;
        this.nightReachDistanceMultiplier = nightReachDistanceMultiplier;
        this.nightSwimSpeedAddition = nightSwimSpeedAddition;
        this.nightSwimSpeedMultiplier = nightSwimSpeedMultiplier;

        // Пещерные атрибуты
        this.caveHealthAddition = caveHealthAddition;
        this.caveHealthMultiplier = caveHealthMultiplier;
        this.caveArmorAddition = caveArmorAddition;
        this.caveArmorMultiplier = caveArmorMultiplier;
        this.caveDamageAddition = caveDamageAddition;
        this.caveDamageMultiplier = caveDamageMultiplier;
        this.caveSpeedAddition = caveSpeedAddition;
        this.caveSpeedMultiplier = caveSpeedMultiplier;
        this.caveKnockbackResistanceAddition = caveKnockbackResistanceAddition;
        this.caveKnockbackResistanceMultiplier = caveKnockbackResistanceMultiplier;
        this.caveAttackKnockbackAddition = caveAttackKnockbackAddition;
        this.caveAttackKnockbackMultiplier = caveAttackKnockbackMultiplier;
        this.caveAttackSpeedAddition = caveAttackSpeedAddition;
        this.caveAttackSpeedMultiplier = caveAttackSpeedMultiplier;
        this.caveFollowRangeAddition = caveFollowRangeAddition;
        this.caveFollowRangeMultiplier = caveFollowRangeMultiplier;
        this.caveFlyingSpeedAddition = caveFlyingSpeedAddition;
        this.caveFlyingSpeedMultiplier = caveFlyingSpeedMultiplier;
        this.caveArmorToughnessAddition = caveArmorToughnessAddition;
        this.caveArmorToughnessMultiplier = caveArmorToughnessMultiplier;
        this.caveLuckAddition = caveLuckAddition;
        this.caveLuckMultiplier = caveLuckMultiplier;
        this.caveReachDistanceAddition = caveReachDistanceAddition;
        this.caveReachDistanceMultiplier = caveReachDistanceMultiplier;
        this.caveSwimSpeedAddition = caveSwimSpeedAddition;
        this.caveSwimSpeedMultiplier = caveSwimSpeedMultiplier;

        this.blacklisted = blacklisted;
    }

    // Геттеры
    public boolean getEnableNightScaling() { return enableNightScaling; }
    public boolean getEnableCaveScaling() { return enableCaveScaling; }
    public double getCaveHeight() { return caveHeight; }
    public double getGravityMultiplier() { return gravityMultiplier; }
    public boolean isGravityEnabled() { return enableGravity; }
    public double getHealthAddition() { return healthAddition; }
    public double getHealthMultiplier() { return healthMultiplier; }
    public double getArmorAddition() { return armorAddition; }
    public double getArmorMultiplier() { return armorMultiplier; }
    public double getDamageAddition() { return damageAddition; }
    public double getDamageMultiplier() { return damageMultiplier; }
    public double getSpeedAddition() { return speedAddition; }
    public double getSpeedMultiplier() { return speedMultiplier; }
    public double getKnockbackResistanceAddition() { return knockbackResistanceAddition; }
    public double getKnockbackResistanceMultiplier() { return knockbackResistanceMultiplier; }
    public double getAttackKnockbackAddition() { return attackKnockbackAddition; }
    public double getAttackKnockbackMultiplier() { return attackKnockbackMultiplier; }
    public double getAttackSpeedAddition() { return attackSpeedAddition; }
    public double getAttackSpeedMultiplier() { return attackSpeedMultiplier; }
    public double getFollowRangeAddition() { return followRangeAddition; }
    public double getFollowRangeMultiplier() { return followRangeMultiplier; }
    public double getFlyingSpeedAddition() { return flyingSpeedAddition; }
    public double getFlyingSpeedMultiplier() { return flyingSpeedMultiplier; }
    public double getArmorToughnessAddition() { return armorToughnessAddition; }
    public double getArmorToughnessMultiplier() { return armorToughnessMultiplier; }
    public double getLuckAddition() { return luckAddition; }
    public double getLuckMultiplier() { return luckMultiplier; }

    public double getNightHealthAddition() { return nightHealthAddition; }
    public double getNightHealthMultiplier() { return nightHealthMultiplier; }
    public double getNightArmorAddition() { return nightArmorAddition; }
    public double getNightArmorMultiplier() { return nightArmorMultiplier; }
    public double getNightDamageAddition() { return nightDamageAddition; }
    public double getNightDamageMultiplier() { return nightDamageMultiplier; }
    public double getNightSpeedAddition() { return nightSpeedAddition; }
    public double getNightSpeedMultiplier() { return nightSpeedMultiplier; }
    public double getNightKnockbackResistanceAddition() { return nightKnockbackResistanceAddition; }
    public double getNightKnockbackResistanceMultiplier() { return nightKnockbackResistanceMultiplier; }
    public double getNightAttackKnockbackAddition() { return nightAttackKnockbackAddition; }
    public double getNightAttackKnockbackMultiplier() { return nightAttackKnockbackMultiplier; }
    public double getNightAttackSpeedAddition() { return nightAttackSpeedAddition; }
    public double getNightAttackSpeedMultiplier() { return nightAttackSpeedMultiplier; }
    public double getNightFollowRangeAddition() { return nightFollowRangeAddition; }
    public double getNightFollowRangeMultiplier() { return nightFollowRangeMultiplier; }
    public double getNightFlyingSpeedAddition() { return nightFlyingSpeedAddition; }
    public double getNightFlyingSpeedMultiplier() { return nightFlyingSpeedMultiplier; }
    public double getNightArmorToughnessAddition() { return nightArmorToughnessAddition; }
    public double getNightArmorToughnessMultiplier() { return nightArmorToughnessMultiplier; }
    public double getNightLuckAddition() { return nightLuckAddition; }
    public double getNightLuckMultiplier() { return nightLuckMultiplier; }

    public double getCaveHealthAddition() { return caveHealthAddition; }
    public double getCaveHealthMultiplier() { return caveHealthMultiplier; }
    public double getCaveArmorAddition() { return caveArmorAddition; }
    public double getCaveArmorMultiplier() { return caveArmorMultiplier; }
    public double getCaveDamageAddition() { return caveDamageAddition; }
    public double getCaveDamageMultiplier() { return caveDamageMultiplier; }
    public double getCaveSpeedAddition() { return caveSpeedAddition; }
    public double getCaveSpeedMultiplier() { return caveSpeedMultiplier; }
    public double getCaveKnockbackResistanceAddition() { return caveKnockbackResistanceAddition; }
    public double getCaveKnockbackResistanceMultiplier() { return caveKnockbackResistanceMultiplier; }
    public double getCaveAttackKnockbackAddition() { return caveAttackKnockbackAddition; }
    public double getCaveAttackKnockbackMultiplier() { return caveAttackKnockbackMultiplier; }
    public double getCaveAttackSpeedAddition() { return caveAttackSpeedAddition; }
    public double getCaveAttackSpeedMultiplier() { return caveAttackSpeedMultiplier; }
    public double getCaveFollowRangeAddition() { return caveFollowRangeAddition; }
    public double getCaveFollowRangeMultiplier() { return caveFollowRangeMultiplier; }
    public double getCaveFlyingSpeedAddition() { return caveFlyingSpeedAddition; }
    public double getCaveFlyingSpeedMultiplier() { return caveFlyingSpeedMultiplier; }
    public double getCaveArmorToughnessAddition() { return caveArmorToughnessAddition; }
    public double getCaveArmorToughnessMultiplier() { return caveArmorToughnessMultiplier; }
    public double getCaveLuckAddition() { return caveLuckAddition; }
    public double getCaveLuckMultiplier() { return caveLuckMultiplier; }

    public double getSwimSpeedAddition() { return swimSpeedAddition; }
    public double getSwimSpeedMultiplier() { return swimSpeedMultiplier; }

    public double getNightSwimSpeedAddition() { return nightSwimSpeedAddition; }
    public double getNightSwimSpeedMultiplier() { return nightSwimSpeedMultiplier; }

    public double getCaveSwimSpeedAddition() { return caveSwimSpeedAddition; }
    public double getCaveSwimSpeedMultiplier() { return caveSwimSpeedMultiplier; }

    public double getReachDistanceAddition() { return reachDistanceAddition; }
    public double getReachDistanceMultiplier() { return reachDistanceMultiplier; }

    public double getNightReachDistanceAddition() { return nightReachDistanceAddition; }
    public double getNightReachDistanceMultiplier() { return nightReachDistanceMultiplier; }

    public double getCaveReachDistanceAddition() { return caveReachDistanceAddition; }
    public double getCaveReachDistanceMultiplier() { return caveReachDistanceMultiplier; }

    public boolean isBlacklisted() {
        return blacklisted;
    }

    public static IndividualMobAttributes getDefault() {
        return new IndividualMobAttributes(
            // Настройки
            false, false, -5.0, 1.0, false, // enableNightScaling, enableCaveScaling, caveHeight, gravityMultiplier, enableGravity
            // Обычные атрибуты
            0.0, 1.0, // health
            0.0, 1.0, // armor
            0.0, 1.0, // damage
            0.0, 1.0, // speed
            0.0, 1.0, // knockback resistance
            0.0, 1.0, // attack knockback
            0.0, 1.0, // attack speed
            0.0, 1.0, // follow range
            0.0, 1.0, // flying speed
            0.0, 1.0, // armor toughness
            0.0, 1.0, // luck
            0.0, 1.0, // reach distance
            0.0, 1.0, // swim speed
            // Ночные атрибуты
            0.0, 1.0, // night health
            0.0, 1.0, // night armor
            0.0, 1.0, // night damage
            0.0, 1.0, // night speed
            0.0, 1.0, // night knockback resistance
            0.0, 1.0, // night attack knockback
            0.0, 1.0, // night attack speed
            0.0, 1.0, // night follow range
            0.0, 1.0, // night flying speed
            0.0, 1.0, // night armor toughness
            0.0, 1.0, // night luck
            0.0, 1.0, // night reach distance
            0.0, 1.0, // night swim speed
            // Пещерные атрибуты
            0.0, 1.0, // cave health
            0.0, 1.0, // cave armor
            0.0, 1.0, // cave damage
            0.0, 1.0, // cave speed
            0.0, 1.0, // cave knockback resistance
            0.0, 1.0, // cave attack knockback
            0.0, 1.0, // cave attack speed
            0.0, 1.0, // cave follow range
            0.0, 1.0, // cave flying speed
            0.0, 1.0, // cave armor toughness
            0.0, 1.0, // cave luck
            0.0, 1.0, // cave reach distance
            0.0, 1.0, // cave swim speed
            false // blacklisted
        );
    }
} 