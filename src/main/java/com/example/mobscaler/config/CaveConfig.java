package com.example.mobscaler.config;

import java.util.List;

public class CaveConfig {
    private final boolean enableCaveMode;
    private final int caveHeight;
    private final double healthAddition;
    private final double healthMultiplier;
    private final double armorAddition;
    private final double armorMultiplier;
    private final double damageAddition;
    private final double damageMultiplier;
    private final double speedAddition;
    private final double speedMultiplier;
    private final double knockbackResistanceAddition;
    private final double knockbackResistanceMultiplier;
    private final double attackKnockbackAddition;
    private final double attackKnockbackMultiplier;
    private final double attackSpeedAddition;
    private final double attackSpeedMultiplier;
    private final double followRangeAddition;
    private final double followRangeMultiplier;
    private final double flyingSpeedAddition;
    private final double flyingSpeedMultiplier;
    private final List<String> modBlacklist;
    private final List<String> entityBlacklist;

    public CaveConfig(boolean enableCaveMode, int caveHeight,
                     double healthAddition, double healthMultiplier,
                     double armorAddition, double armorMultiplier,
                     double damageAddition, double damageMultiplier,
                     double speedAddition, double speedMultiplier,
                     double knockbackResistanceAddition, double knockbackResistanceMultiplier,
                     double attackKnockbackAddition, double attackKnockbackMultiplier,
                     double attackSpeedAddition, double attackSpeedMultiplier,
                     double followRangeAddition, double followRangeMultiplier,
                     double flyingSpeedAddition, double flyingSpeedMultiplier,
                     List<String> modBlacklist, List<String> entityBlacklist) {
        this.enableCaveMode = enableCaveMode;
        this.caveHeight = caveHeight;
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
        this.modBlacklist = modBlacklist;
        this.entityBlacklist = entityBlacklist;
    }

    public boolean isCaveModeEnabled() {
        return enableCaveMode;
    }

    public int getCaveHeight() {
        return caveHeight;
    }

    public double getHealthAddition() {
        return healthAddition;
    }

    public double getHealthMultiplier() {
        return healthMultiplier;
    }

    public double getArmorAddition() {
        return armorAddition;
    }

    public double getArmorMultiplier() {
        return armorMultiplier;
    }

    public double getDamageAddition() {
        return damageAddition;
    }

    public double getDamageMultiplier() {
        return damageMultiplier;
    }

    public double getSpeedAddition() {
        return speedAddition;
    }

    public double getSpeedMultiplier() {
        return speedMultiplier;
    }

    public double getKnockbackResistanceAddition() {
        return knockbackResistanceAddition;
    }

    public double getKnockbackResistanceMultiplier() {
        return knockbackResistanceMultiplier;
    }

    public double getAttackKnockbackAddition() {
        return attackKnockbackAddition;
    }

    public double getAttackKnockbackMultiplier() {
        return attackKnockbackMultiplier;
    }

    public double getAttackSpeedAddition() {
        return attackSpeedAddition;
    }

    public double getAttackSpeedMultiplier() {
        return attackSpeedMultiplier;
    }

    public double getFollowRangeAddition() {
        return followRangeAddition;
    }

    public double getFollowRangeMultiplier() {
        return followRangeMultiplier;
    }

    public double getFlyingSpeedAddition() {
        return flyingSpeedAddition;
    }

    public double getFlyingSpeedMultiplier() {
        return flyingSpeedMultiplier;
    }

    public List<String> getModBlacklist() {
        return modBlacklist;
    }

    public List<String> getEntityBlacklist() {
        return entityBlacklist;
    }
} 