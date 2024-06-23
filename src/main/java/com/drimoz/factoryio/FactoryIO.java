package com.drimoz.factoryio;

import com.drimoz.factoryio.core.configs.FactoryIOCommonConfigs;
import com.drimoz.factoryio.core.datagen.FactoryIODataGenerators;
import com.drimoz.factoryio.core.init.*;
import com.drimoz.factoryio.core.registery.*;
import com.drimoz.factoryio.core.ressourcepack.EPackType;
import com.drimoz.factoryio.core.ressourcepack.FactoryIOPackGeneratorManager;
import com.drimoz.factoryio.core.ressourcepack.FactoryIORepositorySource;
import com.drimoz.factoryio.core.ressourcepack.FactoryIOResourcePackHandler;
import com.mojang.logging.LogUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.repository.PackRepository;
import net.minecraft.server.packs.repository.PackSource;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.AddPackFindersEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;
import software.bernie.geckolib3.GeckoLib;

import java.io.IOException;
import java.nio.file.Files;

@Mod(FactoryIO.MOD_ID)
public class FactoryIO
{
    public static final String MOD_ID = "factory_io";
    public static final String MOD_DISPLAY_NAME = "Factory'I/O";
    public static final Logger LOGGER = LogUtils.getLogger();

    public FactoryIO()
    {
        FactoryIOInserterLoader.setup();
        FactoryIOPackGeneratorManager.registerDataProviders();

        IEventBus eventBus = FMLJavaModLoadingContext.get().getModEventBus();

        eventBus.register(new FactoryIOBlocks());
        eventBus.register(new FactoryIOBlockEntities());
        eventBus.register(new FactoryIOItems());
        eventBus.register(new FactoryIOMenuTypes());
        eventBus.register(new FactoryIODataGenerators());

        FactoryIONetworks.init();
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, FactoryIOCommonConfigs.SPEC, "factory_io/factory_io-common.toml");

        eventBus.addListener(this::onCommonSetup);
        eventBus.addListener(this::onClientSetup);


        GeckoLib.initialize();


        FactoryIOResourcePackHandler.init();


        FMLJavaModLoadingContext.get().getModEventBus().addListener(this::onRegisterResourcePacks);

        MinecraftForge.EVENT_BUS.register(this);
    }

    private void onRegisterResourcePacks(AddPackFindersEvent e) {
        if (!Files.exists(FactoryIORepositorySource.CONFIG_DIR)) {
            try {
                Files.createDirectories(FactoryIORepositorySource.CONFIG_DIR);
            } catch (IOException ex) {
                FactoryIO.LOGGER.error("Error occurred creating \"generated\" repository : " + ex);
            }
        }

        if (e.getPackType() == PackType.SERVER_DATA) {
            e.addRepositorySource(new FactoryIORepositorySource(EPackType.DATA));
        }
        else {
            e.addRepositorySource(new FactoryIORepositorySource(EPackType.RESOURCE));
        }

        FactoryIO.LOGGER.info("Resource Pack registered!");
    }

    public void onCommonSetup(final FMLCommonSetupEvent event)
    {
        FactoryIOInserterRegistry.getInstance().onCommonSetup();
        event.enqueueWork(FactoryIONetworks::init);
    }

    public void onClientSetup(final FMLClientSetupEvent event) {
        FactoryIOInserterRegistry.getInstance().onRegisterScreens();
    }
}
