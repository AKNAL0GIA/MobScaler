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
        @SerializedName("blockReachAddition")
        private final double blockReachAddition;
        @SerializedName("blockReachMultiplier")
        private final double blockReachMultiplier;
        @SerializedName("entityReachAddition")
        private final double entityReachAddition;
        @SerializedName("entityReachMultiplier")
        private final double entityReachMultiplier;
        @SerializedName("burningTimeAddition")
        private final double burningTimeAddition;
        @SerializedName("burningTimeMultiplier")
        private final double burningTimeMultiplier;
        @SerializedName("fallDamageMultiplier")
        private final double fallDamageMultiplier;
        @SerializedName("explosionKnockbackResistanceAddition")
        private final double explosionKnockbackResistanceAddition;
        @SerializedName("explosionKnockbackResistanceMultiplier")
        private final double explosionKnockbackResistanceMultiplier;
        @SerializedName("jumpStrengthAddition")
        private final double jumpStrengthAddition;
        @SerializedName("jumpStrengthMultiplier")
        private final double jumpStrengthMultiplier;
        @SerializedName("miningEfficiencyAddition")
        private final double miningEfficiencyAddition;
        @SerializedName("miningEfficiencyMultiplier")
        private final double miningEfficiencyMultiplier;
        @SerializedName("movementEfficiencyAddition")
        private final double movementEfficiencyAddition;
        @SerializedName("movementEfficiencyMultiplier")
        private final double movementEfficiencyMultiplier;
        @SerializedName("oxygenBonusAddition")
        private final double oxygenBonusAddition;
        @SerializedName("oxygenBonusMultiplier")
        private final double oxygenBonusMultiplier;
        @SerializedName("safeFallDistanceAddition")
        private final double safeFallDistanceAddition;
        @SerializedName("safeFallDistanceMultiplier")
        private final double safeFallDistanceMultiplier;
        @SerializedName("blockBreakSpeedAddition")
        private final double blockBreakSpeedAddition;
        @SerializedName("blockBreakSpeedMultiplier")
        private final double blockBreakSpeedMultiplier;
        @SerializedName("stepHeightAddition")
        private final double stepHeightAddition;
        @SerializedName("stepHeightMultiplier")
        private final double stepHeightMultiplier;
        @SerializedName("submergedMiningSpeedAddition")
        private final double submergedMiningSpeedAddition;
        @SerializedName("submergedMiningSpeedMultiplier")
        private final double submergedMiningSpeedMultiplier;
        @SerializedName("waterMovementEfficiencyAddition")
        private final double waterMovementEfficiencyAddition;
        @SerializedName("waterMovementEfficiencyMultiplier")
        private final double waterMovementEfficiencyMultiplier;
        @SerializedName("SneakingSpeedAddition")
        private final double SneakingSpeedAddition;
        @SerializedName("SneakingSpeedMultiplier")
        private final double SneakingSpeedMultiplier;
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
        @SerializedName("nightArmorToughnessAddition")
        private final double nightArmorToughnessAddition;
        @SerializedName("nightArmorToughnessMultiplier")
        private final double nightArmorToughnessMultiplier;
        @SerializedName("nightLuckAddition")
        private final double nightLuckAddition;
        @SerializedName("nightLuckMultiplier")
        private final double nightLuckMultiplier;
        @SerializedName("gravityMultiplier")
        private final double gravityMultiplier;
        @SerializedName("nightSwimSpeedAddition")
        private final double nightSwimSpeedAddition;
        @SerializedName("nightSwimSpeedMultiplier")
        private final double nightSwimSpeedMultiplier;
        @SerializedName("nightBlockReachAddition")
        private final double nightBlockReachAddition;
        @SerializedName("nightBlockReachMultiplier")
        private final double nightBlockReachMultiplier;
        @SerializedName("nightEntityReachAddition")
        private final double nightEntityReachAddition;
        @SerializedName("nightEntityReachMultiplier")
        private final double nightEntityReachMultiplier;
        @SerializedName("nightBurningTimeAddition")
        private final double nightBurningTimeAddition;
        @SerializedName("nightBurningTimeMultiplier")
        private final double nightBurningTimeMultiplier;
        @SerializedName("nightFallDamageMultiplier")
        private final double nightFallDamageMultiplier;
        @SerializedName("nightExplosionKnockbackResistanceAddition")
        private final double nightExplosionKnockbackResistanceAddition;
        @SerializedName("nightExplosionKnockbackResistanceMultiplier")
        private final double nightExplosionKnockbackResistanceMultiplier;
        @SerializedName("nightJumpStrengthAddition")
        private final double nightJumpStrengthAddition;
        @SerializedName("nightJumpStrengthMultiplier")
        private final double nightJumpStrengthMultiplier;
        @SerializedName("nightMiningEfficiencyAddition")
        private final double nightMiningEfficiencyAddition;
        @SerializedName("nightMiningEfficiencyMultiplier")
        private final double nightMiningEfficiencyMultiplier;
        @SerializedName("nightMovementEfficiencyAddition")
        private final double nightMovementEfficiencyAddition;
        @SerializedName("nightMovementEfficiencyMultiplier")
        private final double nightMovementEfficiencyMultiplier;
        @SerializedName("nightOxygenBonusAddition")
        private final double nightOxygenBonusAddition;
        @SerializedName("nightOxygenBonusMultiplier")
        private final double nightOxygenBonusMultiplier;
        @SerializedName("nightSafeFallDistanceAddition")
        private final double nightSafeFallDistanceAddition;
        @SerializedName("nightSafeFallDistanceMultiplier")
        private final double nightSafeFallDistanceMultiplier;
        @SerializedName("nightBlockBreakSpeedAddition")
        private final double nightBlockBreakSpeedAddition;
        @SerializedName("nightBlockBreakSpeedMultiplier")
        private final double nightBlockBreakSpeedMultiplier;
        @SerializedName("nightStepHeightAddition")
        private final double nightStepHeightAddition;
        @SerializedName("nightStepHeightMultiplier")
        private final double nightStepHeightMultiplier;
        @SerializedName("nightSubmergedMiningSpeedAddition")
        private final double nightSubmergedMiningSpeedAddition;
        @SerializedName("nightSubmergedMiningSpeedMultiplier")
        private final double nightSubmergedMiningSpeedMultiplier;
        @SerializedName("nightWaterMovementEfficiencyAddition")
        private final double nightWaterMovementEfficiencyAddition;
        @SerializedName("nightWaterMovementEfficiencyMultiplier")
        private final double nightWaterMovementEfficiencyMultiplier;
        @SerializedName("nightSneakingSpeedAddition")
        private final double nightSneakingSpeedAddition;
        @SerializedName("nightSneakingSpeedMultiplier")
        private final double nightSneakingSpeedMultiplier;


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
            this.armorToughnessAddition = 0.0;
            this.armorToughnessMultiplier = 1.0;
            this.luckAddition = 0.0;
            this.luckMultiplier = 1.0;
            this.swimSpeedAddition = 0.0;
            this.swimSpeedMultiplier = 1.0;
            this.blockReachAddition = 0.0;
            this.blockReachMultiplier = 1.0;
            this.entityReachAddition = 0.0;
            this.entityReachMultiplier = 1.0;
            this.burningTimeAddition = 0.0;
            this.burningTimeMultiplier = 1.0;
            this.fallDamageMultiplier = 1.0;
            this.explosionKnockbackResistanceAddition = 0.0;
            this.explosionKnockbackResistanceMultiplier = 1.0;
            this.jumpStrengthAddition = 0.0;
            this.jumpStrengthMultiplier = 1.0;
            this.miningEfficiencyAddition = 0.0;
            this.miningEfficiencyMultiplier = 1.0;
            this.movementEfficiencyAddition = 0.0;
            this.movementEfficiencyMultiplier = 1.0;
            this.oxygenBonusAddition = 0.0;
            this.oxygenBonusMultiplier = 1.0;
            this.safeFallDistanceAddition = 0.0;
            this.safeFallDistanceMultiplier = 1.0;
            this.blockBreakSpeedAddition = 0.0;
            this.blockBreakSpeedMultiplier = 1.0;
            this.stepHeightAddition = 0.0;
            this.stepHeightMultiplier = 1.0;
            this.submergedMiningSpeedAddition = 0.0;
            this.submergedMiningSpeedMultiplier = 1.0;
            this.waterMovementEfficiencyAddition = 0.0;
            this.waterMovementEfficiencyMultiplier = 1.0;
            this.SneakingSpeedAddition = 0.0;
            this.SneakingSpeedMultiplier = 1.0;
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
            this.nightArmorToughnessAddition = 0.0;
            this.nightArmorToughnessMultiplier = 1.0;
            this.nightLuckAddition = 0.0;
            this.nightLuckMultiplier = 1.0;
            this.gravityMultiplier = 1.0;
            this.nightSwimSpeedAddition = 0.0;
            this.nightSwimSpeedMultiplier = 1.0;
            this.nightBlockReachAddition = 0.0;
            this.nightBlockReachMultiplier = 1.0;
            this.nightEntityReachAddition = 0.0;
            this.nightEntityReachMultiplier = 1.0;
            this.nightBurningTimeAddition = 0.0;
            this.nightBurningTimeMultiplier = 1.0;
            this.nightFallDamageMultiplier = 1.0;
            this.nightExplosionKnockbackResistanceAddition = 0.0;
            this.nightExplosionKnockbackResistanceMultiplier = 1.0;
            this.nightJumpStrengthAddition = 0.0;
            this.nightJumpStrengthMultiplier = 1.0;
            this.nightMiningEfficiencyAddition = 0.0;
            this.nightMiningEfficiencyMultiplier = 1.0;
            this.nightMovementEfficiencyAddition = 0.0;
            this.nightMovementEfficiencyMultiplier = 1.0;
            this.nightOxygenBonusAddition = 0.0;
            this.nightOxygenBonusMultiplier = 1.0;
            this.nightSafeFallDistanceAddition = 0.0;
            this.nightSafeFallDistanceMultiplier = 1.0;
            this.nightBlockBreakSpeedAddition = 0.0;
            this.nightBlockBreakSpeedMultiplier = 1.0;
            this.nightStepHeightAddition = 0.0;
            this.nightStepHeightMultiplier = 1.0;
            this.nightSubmergedMiningSpeedAddition = 0.0;
            this.nightSubmergedMiningSpeedMultiplier = 1.0;
            this.nightWaterMovementEfficiencyAddition = 0.0;
            this.nightWaterMovementEfficiencyMultiplier = 1.0;
            this.nightSneakingSpeedAddition = 0.0;
            this.nightSneakingSpeedMultiplier = 1.0;
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
        public double getArmorToughnessAddition() { return armorToughnessAddition; }
        public double getArmorToughnessMultiplier() { return armorToughnessMultiplier; }
        public double getLuckAddition() { return luckAddition; }
        public double getLuckMultiplier() { return luckMultiplier; }
        public double getSwimSpeedAddition() { return swimSpeedAddition; }
        public double getSwimSpeedMultiplier() { return swimSpeedMultiplier; }
        public double getBlockReachAddition() { return blockReachAddition; }
        public double getBlockReachMultiplier() { return blockReachMultiplier; }
        public double getEntityReachAddition() { return entityReachAddition; }
        public double getEntityReachMultiplier() { return entityReachMultiplier; }
        public double getBurningTimeAddition() { return burningTimeAddition; }
        public double getBurningTimeMultiplier() { return burningTimeMultiplier; }
        public double getFallDamageMultiplier() { return fallDamageMultiplier; }
        public double getExplosionKnockbackResistanceAddition() { return explosionKnockbackResistanceAddition; }
        public double getExplosionKnockbackResistanceMultiplier() { return explosionKnockbackResistanceMultiplier; }
        public double getJumpStrengthAddition() { return jumpStrengthAddition; }
        public double getJumpStrengthMultiplier() { return jumpStrengthMultiplier; }
        public double getMiningEfficiencyAddition() { return miningEfficiencyAddition; }
        public double getMiningEfficiencyMultiplier() { return miningEfficiencyMultiplier; }
        public double getMovementEfficiencyAddition() { return movementEfficiencyAddition; }
        public double getMovementEfficiencyMultiplier() { return movementEfficiencyMultiplier; }
        public double getOxygenBonusAddition() { return oxygenBonusAddition; }
        public double getOxygenBonusMultiplier() { return oxygenBonusMultiplier; }
        public double getSafeFallDistanceAddition() { return safeFallDistanceAddition; }
        public double getSafeFallDistanceMultiplier() { return safeFallDistanceMultiplier; }
        public double getBlockBreakSpeedAddition() { return blockBreakSpeedAddition; }
        public double getBlockBreakSpeedMultiplier() { return blockBreakSpeedMultiplier; }
        public double getStepHeightAddition() { return stepHeightAddition; }
        public double getStepHeightMultiplier() { return stepHeightMultiplier; }
        public double getSubmergedMiningSpeedAddition() { return submergedMiningSpeedAddition; }
        public double getSubmergedMiningSpeedMultiplier() { return submergedMiningSpeedMultiplier; }
        public double getWaterMovementEfficiencyAddition() { return waterMovementEfficiencyAddition; }
        public double getWaterMovementEfficiencyMultiplier() { return waterMovementEfficiencyMultiplier; }
        public double getSneakingSpeedAddition() { return SneakingSpeedAddition; }
        public double getSneakingSpeedMultiplier() { return SneakingSpeedMultiplier; }

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
        public double getNightArmorToughnessAddition() { return nightArmorToughnessAddition; }
        public double getNightArmorToughnessMultiplier() { return nightArmorToughnessMultiplier; }
        public double getNightLuckAddition() { return nightLuckAddition; }
        public double getNightLuckMultiplier() { return nightLuckMultiplier; }
        public double getGravityMultiplier() { return gravityMultiplier; }
        public double getNightSwimSpeedAddition() { return nightSwimSpeedAddition; }
        public double getNightSwimSpeedMultiplier() { return nightSwimSpeedMultiplier; }
        public double getNightBlockReachAddition() { return nightBlockReachAddition; }
        public double getNightBlockReachMultiplier() { return nightBlockReachMultiplier; }
        public double getNightEntityReachAddition() { return nightEntityReachAddition; }
        public double getNightEntityReachMultiplier() { return nightEntityReachMultiplier; }
        public double getNightBurningTimeAddition() { return nightBurningTimeAddition; }
        public double getNightBurningTimeMultiplier() { return nightBurningTimeMultiplier; }
        public double getNightFallDamageMultiplier() { return nightFallDamageMultiplier; }
        public double getNightExplosionKnockbackResistanceAddition() { return nightExplosionKnockbackResistanceAddition; }
        public double getNightExplosionKnockbackResistanceMultiplier() { return nightExplosionKnockbackResistanceMultiplier; }
        public double getNightJumpStrengthAddition() { return nightJumpStrengthAddition; }
        public double getNightJumpStrengthMultiplier() { return nightJumpStrengthMultiplier; }
        public double getNightMiningEfficiencyAddition() { return nightMiningEfficiencyAddition; }
        public double getNightMiningEfficiencyMultiplier() { return nightMiningEfficiencyMultiplier; }
        public double getNightMovementEfficiencyAddition() { return nightMovementEfficiencyAddition; }
        public double getNightMovementEfficiencyMultiplier() { return nightMovementEfficiencyMultiplier; }
        public double getNightOxygenBonusAddition() { return nightOxygenBonusAddition; }
        public double getNightOxygenBonusMultiplier() { return nightOxygenBonusMultiplier; }
        public double getNightSafeFallDistanceAddition() { return nightSafeFallDistanceAddition; }
        public double getNightSafeFallDistanceMultiplier() { return nightSafeFallDistanceMultiplier; }
        public double getNightBlockBreakSpeedAddition() { return nightBlockBreakSpeedAddition; }
        public double getNightBlockBreakSpeedMultiplier() { return nightBlockBreakSpeedMultiplier; }
        public double getNightStepHeightAddition() { return nightStepHeightAddition; }
        public double getNightStepHeightMultiplier() { return nightStepHeightMultiplier; }
        public double getNightSubmergedMiningSpeedAddition() { return nightSubmergedMiningSpeedAddition; }
        public double getNightSubmergedMiningSpeedMultiplier() { return nightSubmergedMiningSpeedMultiplier; }
        public double getNightWaterMovementEfficiencyAddition() { return nightWaterMovementEfficiencyAddition; }
        public double getNightWaterMovementEfficiencyMultiplier() { return nightWaterMovementEfficiencyMultiplier; }
        public double getNightSneakingSpeedAddition() { return nightSneakingSpeedAddition; }
        public double getNightSneakingSpeedMultiplier() { return nightSneakingSpeedMultiplier; }
    }
}
