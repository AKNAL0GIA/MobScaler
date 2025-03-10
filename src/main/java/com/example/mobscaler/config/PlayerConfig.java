package com.example.mobscaler.config;

import com.google.gson.annotations.SerializedName;
import java.util.Map;
import java.util.HashMap;
import java.util.List;

public class PlayerConfig {
    @SerializedName("playerBlacklist")
    private final Map<String, List<String>> playerBlacklist;
    
    @SerializedName("playerModifiers")
    private final Map<String, PlayerModifiers> playerModifiers;

    public PlayerConfig(Map<String, List<String>> playerBlacklist, Map<String, PlayerModifiers> playerModifiers) {
        this.playerBlacklist = playerBlacklist != null ? playerBlacklist : new HashMap<>();
        this.playerModifiers = playerModifiers != null ? playerModifiers : new HashMap<>();
    }

    public Map<String, List<String>> getPlayerBlacklist() {
        return playerBlacklist;
    }

    public Map<String, PlayerModifiers> getPlayerModifiers() {
        return playerModifiers;
    }

    public List<String> getBlacklistForDimension(String dimensionId) {
        return playerBlacklist.getOrDefault(dimensionId, List.of());
    }

    public PlayerModifiers getModifiersForDimension(String dimensionId) {
        return playerModifiers.getOrDefault(dimensionId, new PlayerModifiers());
    }

    public boolean isPlayerBlocked(String playerName, String dimensionId) {
        List<String> dimensionBlacklist = getBlacklistForDimension(dimensionId);
        return dimensionBlacklist.contains(playerName);
    }

    public static class PlayerModifiers {
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

        public PlayerModifiers() {
            this.enableNightScaling = false;
            // Дневные настройки
            this.healthAddition = 0.0;
            this.healthMultiplier = 1.0;
            this.armorAddition = 0.0;
            this.armorMultiplier = 1.0;
            this.damageAddition = 0.0;
            this.damageMultiplier = 1.0;
            this.speedAddition = 0.0;
            this.speedMultiplier = 1.0;
            this.knockbackResistanceAddition = 0.0;
            this.knockbackResistanceMultiplier = 1.0;
            this.attackKnockbackAddition = 0.0;
            this.attackKnockbackMultiplier = 1.0;
            this.attackSpeedAddition = 0.0;
            this.attackSpeedMultiplier = 1.0;
            this.followRangeAddition = 0.0;
            this.followRangeMultiplier = 1.0;
            this.flyingSpeedAddition = 0.0;
            this.flyingSpeedMultiplier = 1.0;
            // Ночные настройки
            this.nightHealthAddition = 0.0;
            this.nightHealthMultiplier = 1.0;
            this.nightArmorAddition = 0.0;
            this.nightArmorMultiplier = 1.0;
            this.nightDamageAddition = 0.0;
            this.nightDamageMultiplier = 1.0;
            this.nightSpeedAddition = 0.0;
            this.nightSpeedMultiplier = 1.0;
            this.nightKnockbackResistanceAddition = 0.0;
            this.nightKnockbackResistanceMultiplier = 1.0;
            this.nightAttackKnockbackAddition = 0.0;
            this.nightAttackKnockbackMultiplier = 1.0;
            this.nightAttackSpeedAddition = 0.0;
            this.nightAttackSpeedMultiplier = 1.0;
            this.nightFollowRangeAddition = 0.0;
            this.nightFollowRangeMultiplier = 1.0;
            this.nightFlyingSpeedAddition = 0.0;
            this.nightFlyingSpeedMultiplier = 1.0;
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
    }
} 