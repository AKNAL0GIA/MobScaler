package com.example.mobscaler;

import com.example.mobscaler.config.MobScalerConfig;
import com.example.mobscaler.events.EntityHandler;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Mod(MobScalerMod.MODID)
public class MobScalerMod {
    public static final String MODID = "mobscaler";
    private static final Logger LOGGER = LogManager.getLogger();

    public MobScalerMod() {
        // Инициализируем конфигурацию сразу, чтобы spec был построен до регистрации обработчиков
        MobScalerConfig.init();
        
        @SuppressWarnings("removal")
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();
        // Регистрируем конфигурацию через модовый EventBus
        MobScalerConfig.register(modEventBus);
        
        // Добавляем слушателя на commonSetup, если потребуется дополнительная инициализация
        modEventBus.addListener(this::commonSetup);

        // Регистрация событий Forge
        MinecraftForge.EVENT_BUS.register(this);
        // Регистрация обработчика событий EntityHandler
        MinecraftForge.EVENT_BUS.register(EntityHandler.class);
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        LOGGER.info("HELLO FROM INIT");
        LOGGER.info("Мод id: {}", MODID);
    }
}