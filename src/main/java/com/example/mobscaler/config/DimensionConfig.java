package com.example.mobscaler.config;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class DimensionConfig {
    @SerializedName("enableNightScaling")
    private final boolean enableNightScaling;
    
    // Дневные настройки
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

    // Ночные настройки
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

    @SerializedName("modBlacklist")
    private final List<String> modBlacklist;
    @SerializedName("entityBlacklist")
    private final List<String> entityBlacklist;

    public DimensionConfig(
            boolean enableNightScaling,
            // Дневные настройки
            double healthAddition, double healthMultiplier,
            double armorAddition, double armorMultiplier,
            double damageAddition, double damageMultiplier,
            double speedAddition, double speedMultiplier,
            double knockbackResistanceAddition, double knockbackResistanceMultiplier,
            double attackKnockbackAddition, double attackKnockbackMultiplier,
            double attackSpeedAddition, double attackSpeedMultiplier,
            double followRangeAddition, double followRangeMultiplier,
            double flyingSpeedAddition, double flyingSpeedMultiplier,
            // Ночные настройки
            double nightHealthAddition, double nightHealthMultiplier,
            double nightArmorAddition, double nightArmorMultiplier,
            double nightDamageAddition, double nightDamageMultiplier,
            double nightSpeedAddition, double nightSpeedMultiplier,
            double nightKnockbackResistanceAddition, double nightKnockbackResistanceMultiplier,
            double nightAttackKnockbackAddition, double nightAttackKnockbackMultiplier,
            double nightAttackSpeedAddition, double nightAttackSpeedMultiplier,
            double nightFollowRangeAddition, double nightFollowRangeMultiplier,
            double nightFlyingSpeedAddition, double nightFlyingSpeedMultiplier,
            List<String> modBlacklist,
            List<String> entityBlacklist) {
        this.enableNightScaling = enableNightScaling;
        // Дневные настройки
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
        // Ночные настройки
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
        this.modBlacklist = modBlacklist;
        this.entityBlacklist = entityBlacklist;
    }

    // Геттеры для дневных настроек
    public boolean isNightScalingEnabled() { return enableNightScaling; }
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

    // Геттеры для ночных настроек
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

    public List<String> getModBlacklist() { return modBlacklist; }
    public List<String> getEntityBlacklist() { return entityBlacklist; }
}
