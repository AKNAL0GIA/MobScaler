package com.example.mobscaler;

import com.example.mobscaler.commands.ReloadCommand;
import com.example.mobscaler.config.MobScalerConfig;
import com.example.mobscaler.config.PlayerConfigManager;
import com.example.mobscaler.config.DimensionConfigManager;
import com.example.mobscaler.config.IndividualMobConfigManager;
import com.example.mobscaler.events.KeyHandler;
import com.example.mobscaler.events.EntityHandler;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.fml.ModContainer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Mod(MobScalerMod.MODID)
public class MobScalerMod {
    public static final String MODID = "mobscaler";
    private static final Logger LOGGER = LoggerFactory.getLogger(MobScalerMod.class);

    public MobScalerMod(IEventBus modEventBus, ModContainer modContainer) {
        // Регистрируем конфигурацию
        modContainer.registerConfig(ModConfig.Type.COMMON, MobScalerConfig.SPEC);

        modEventBus.addListener(this::commonSetup);

        // Регистрируем клиентские события только если мы на клиенте
        if (FMLEnvironment.dist == Dist.CLIENT) {
            modEventBus.addListener(this::clientSetup);
            modEventBus.addListener(this::registerKeyMappings);
            // Регистрируем KeyHandler
            NeoForge.EVENT_BUS.register(KeyHandler.class);
        }

        // Регистрируем обработчик событий сущностей
        NeoForge.EVENT_BUS.register(EntityHandler.class);

        // Регистрируем обработчик событий команд
        NeoForge.EVENT_BUS.addListener(this::onCommandRegister);

        // Регистрируем обработчик запуска сервера для регистрации измерений из модов
        NeoForge.EVENT_BUS.addListener(this::onServerStarting);
    }

    private void onCommandRegister(RegisterCommandsEvent event) {
        ReloadCommand.register(event.getDispatcher());
        LOGGER.info("MobScaler commands registered successfully");
    }

    private void onServerStarting(ServerStartingEvent event) {
        // Регистрируем измерения из модов при запуске сервера
        DimensionConfigManager.registerModDimensions(event.getServer().registryAccess());
        LOGGER.info("MobScaler mod dimensions registered");
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

    private void registerKeyMappings(final RegisterKeyMappingsEvent event) {
        LOGGER.info("Registering MobScaler key mappings");
        KeyHandler.init(event);
    }

    private void clientSetup(final FMLClientSetupEvent event) {
        LOGGER.info("MobScaler client initialization");
    }
}
