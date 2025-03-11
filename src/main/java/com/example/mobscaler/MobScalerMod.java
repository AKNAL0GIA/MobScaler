package com.example.mobscaler;

import com.example.mobscaler.commands.ReloadCommand;
import com.example.mobscaler.config.MobScalerConfig;
import com.example.mobscaler.config.PlayerConfigManager;
import com.example.mobscaler.config.DimensionConfigManager;
import com.example.mobscaler.config.CaveConfigManager;
import com.example.mobscaler.events.EntityHandler;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.ModLoadingContext;

@Mod(MobScalerMod.MODID)
public class MobScalerMod {
    public static final String MODID = "mobscaler";
    private static final Logger LOGGER = LogManager.getLogger();

    public MobScalerMod() {
        // Регистрируем конфигурацию
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, MobScalerConfig.SPEC, "mobscaler-common.toml");

        // Регистрируем обработчик событий
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();
        modEventBus.addListener(this::commonSetup);

        // Регистрируем обработчик событий сущностей
        MinecraftForge.EVENT_BUS.register(EntityHandler.class);
        
        // Регистрируем обработчик событий команд
        MinecraftForge.EVENT_BUS.addListener(this::onCommandRegister);
    }

    private void onCommandRegister(RegisterCommandsEvent event) {
        ReloadCommand.register(event.getDispatcher());
        LOGGER.info("MobScaler commands registered successfully");
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        LOGGER.info("Initializing MobScaler Mod");
        LOGGER.info("Mod ID: " + MODID);
        
        // Загружаем конфигурации
        MobScalerConfig.init();
        DimensionConfigManager.loadConfigs();
        PlayerConfigManager.loadConfigs();
        CaveConfigManager.loadConfigs();
        
        LOGGER.info("MobScaler configurations loaded successfully");
    }
}