package com.drimoz.factoryio;

import com.drimoz.factoryio.core.configs.FactoryIOCommonConfigs;
import com.drimoz.factoryio.core.datagen.FactoryIODataGenerators;
import com.drimoz.factoryio.core.init.*;
import com.drimoz.factoryio.core.registery.*;
import com.mojang.logging.LogUtils;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;
import software.bernie.geckolib3.GeckoLib;

@Mod(FactoryIO.MOD_ID)
public class FactoryIO
{
    public static final String MOD_ID = "factory_io";
    public static final Logger LOGGER = LogUtils.getLogger();

    public FactoryIO()
    {
        FactoryIOInserterLoader.setup();

        IEventBus eventBus = FMLJavaModLoadingContext.get().getModEventBus();

        eventBus.register(new FactoryIOBlocks());
        eventBus.register(new FactoryIOBlockEntities());
        eventBus.register(new FactoryIOItems());
        eventBus.register(new FactoryIOMenuTypes());
        eventBus.register(new FactoryIODataGenerators());


        FactoryIONetworks.init();
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, FactoryIOCommonConfigs.SPEC, "factory_io-common.toml");

        eventBus.addListener(this::onCommonSetup);
        eventBus.addListener(this::onClientSetup);

        GeckoLib.initialize();

        MinecraftForge.EVENT_BUS.register(this);
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
