package com.example.mobscaler;

import com.example.mobscaler.commands.ReloadCommand;
import com.example.mobscaler.config.MobScalerConfig;
import com.example.mobscaler.config.PlayerConfigManager;
import com.example.mobscaler.config.DimensionConfigManager;
import com.example.mobscaler.config.IndividualMobConfigManager;
import com.example.mobscaler.events.KeyHandler;
import com.example.mobscaler.events.EntityHandler;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.fml.loading.FMLEnvironment;

@Mod(MobScalerMod.MODID)
public class MobScalerMod {
    public static final String MODID = "mobscaler";
    private static final org.apache.logging.log4j.Logger LOGGER = org.apache.logging.log4j.LogManager.getLogger();

    public MobScalerMod() {
        // Регистрируем конфигурацию
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, MobScalerConfig.SPEC, "mobscaler-common.toml");

        // Регистрируем обработчики событий
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();
        modEventBus.addListener(this::commonSetup);
        
        // Регистрируем клиентские события только если мы на клиенте
        if (FMLEnvironment.dist == Dist.CLIENT) {
            modEventBus.addListener(this::clientSetup);
            modEventBus.addListener(this::registerKeyMappings);
        }

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
        IndividualMobConfigManager.loadConfigs();
        
        LOGGER.info("MobScaler configurations loaded successfully");
    }
    
    @OnlyIn(Dist.CLIENT)
    private void registerKeyMappings(final RegisterKeyMappingsEvent event) {
        LOGGER.info("Registering MobScaler key mappings");
        KeyHandler.init(event);
    }
    
    @OnlyIn(Dist.CLIENT)
    private void clientSetup(final FMLClientSetupEvent event) {
        LOGGER.info("MobScaler client initialization");
    }
}