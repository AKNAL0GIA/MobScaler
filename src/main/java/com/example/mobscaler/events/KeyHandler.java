package com.example.mobscaler.events;

import com.example.mobscaler.gui.MobScalerScreen;
import com.example.mobscaler.config.MobScalerConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.KeyMapping;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.settings.KeyConflictContext;
import net.neoforged.neoforge.client.settings.KeyModifier;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.bus.api.SubscribeEvent;
import org.lwjgl.glfw.GLFW;
import com.mojang.blaze3d.platform.InputConstants;

@OnlyIn(Dist.CLIENT)
public class KeyHandler {

    private static final String CATEGORY = "key.categories.mobscaler";
    public static final KeyMapping KEY_OPEN_GUI = new KeyMapping(
            "key.mobscaler.opengui",
            KeyConflictContext.IN_GAME,
            KeyModifier.ALT,
            InputConstants.Type.KEYSYM.getOrCreate(GLFW.GLFW_KEY_APOSTROPHE),
            CATEGORY
    );
    private static int savedGuiScale = -1;
    private static boolean pendingOpenGui = false;

    public static void init(RegisterKeyMappingsEvent event) {
        event.register(KEY_OPEN_GUI);
    }

    public static int getSavedGuiScale() {
        return savedGuiScale;
    }

    public static void setSavedGuiScale(int scale) {
        savedGuiScale = scale;
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (pendingOpenGui) {
            pendingOpenGui = false;
            minecraft.setScreen(new MobScalerScreen());
            return;
        }

        if (minecraft.player != null && KEY_OPEN_GUI.consumeClick()) {
            savedGuiScale = minecraft.options.guiScale().get();
            int configScale = MobScalerConfig.getGuiScaleOnOpen();
            if (configScale > 0 && configScale != savedGuiScale) {
                minecraft.options.guiScale().set(configScale);
                minecraft.resizeDisplay();
                pendingOpenGui = true;
            } else {
                savedGuiScale = -1;
                minecraft.setScreen(new MobScalerScreen());
            }
        }
    }
}