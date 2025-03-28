package com.example.mobscaler.config;

import com.google.gson.annotations.SerializedName;

public class IndividualMobConfig {
    @SerializedName("isBlacklisted")
    private boolean isBlacklisted;
    
    @SerializedName("attributes")
    private final IndividualMobAttributes attributes;

    public IndividualMobConfig(boolean isBlacklisted, IndividualMobAttributes attributes) {
        this.isBlacklisted = isBlacklisted;
        this.attributes = attributes != null ? attributes : IndividualMobAttributes.getDefault();
    }

    public boolean isBlacklisted() {
        return isBlacklisted;
    }
    
    public void setBlacklisted(boolean blacklisted) {
        this.isBlacklisted = blacklisted;
    }

    public IndividualMobAttributes getAttributes() {
        return attributes;
    }

    public static IndividualMobConfig getDefault() {
        return new IndividualMobConfig(false, IndividualMobAttributes.getDefault());
    }
} 