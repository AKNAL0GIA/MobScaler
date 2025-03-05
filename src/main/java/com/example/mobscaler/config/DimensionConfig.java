package com.example.mobscaler.config;

import java.util.List;

public class DimensionConfig {
    private final double healthAddition;
    private final double healthMultiplier;
    private final double armorAddition;
    private final double armorMultiplier;
    private final double damageAddition;
    private final double damageMultiplier;
    private final List<String> modBlacklist;
    private final List<String> entityBlacklist;

    public DimensionConfig(
            double healthAddition,
            double healthMultiplier,
            double armorAddition,
            double armorMultiplier,
            double damageAddition,
            double damageMultiplier,
            List<String> modBlacklist,
            List<String> entityBlacklist) {
        this.healthAddition = healthAddition;
        this.healthMultiplier = healthMultiplier;
        this.armorAddition = armorAddition;
        this.armorMultiplier = armorMultiplier;
        this.damageAddition = damageAddition;
        this.damageMultiplier = damageMultiplier;
        this.modBlacklist = modBlacklist;
        this.entityBlacklist = entityBlacklist;
    }

    public double getHealthAddition() { return healthAddition; }
    public double getHealthMultiplier() { return healthMultiplier; }
    public double getArmorAddition() { return armorAddition; }
    public double getArmorMultiplier() { return armorMultiplier; }
    public double getDamageAddition() { return damageAddition; }
    public double getDamageMultiplier() { return damageMultiplier; }
    public List<String> getModBlacklist() { return modBlacklist; }
    public List<String> getEntityBlacklist() { return entityBlacklist; }
}
