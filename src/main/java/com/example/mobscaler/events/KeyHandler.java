package com.example.mobscaler.events;

import com.example.mobscaler.gui.MobScalerScreen;
import com.example.mobscaler.MobScalerMod;
import net.minecraft.client.Minecraft;
import net.minecraft.client.KeyMapping;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.client.settings.KeyConflictContext;
import net.minecraftforge.client.settings.KeyModifier;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.glfw.GLFW;
import com.mojang.blaze3d.platform.InputConstants;

@OnlyIn(Dist.CLIENT)
@Mod.EventBusSubscriber(modid = MobScalerMod.MODID, value = Dist.CLIENT)
public class KeyHandler {
    
    private static final String CATEGORY = "key.categories.mobscaler";
    public static final KeyMapping KEY_OPEN_GUI = new KeyMapping(
            "key.mobscaler.opengui", 
            KeyConflictContext.IN_GAME, 
            KeyModifier.ALT, 
            InputConstants.Type.KEYSYM.getOrCreate(GLFW.GLFW_KEY_APOSTROPHE), 
            CATEGORY
    );
    
    public static void init(RegisterKeyMappingsEvent event) {
        event.register(KEY_OPEN_GUI);
    }
    
    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        
        Minecraft minecraft = Minecraft.getInstance();
        
        // Проверяем нажатие клавиши
        if (minecraft.player != null && KEY_OPEN_GUI.consumeClick()) {
            minecraft.setScreen(new MobScalerScreen());
        }
    }
} 