package com.drimoz.factoryio;

import com.drimoz.factoryio.a_core.registery.FactoryIORegistry;
import com.drimoz.factoryio.a_core.registery.FactoryIORegistryNetworks;
import com.drimoz.factoryio.a_core.registery.FactoryIORegistryRenderers;
import com.drimoz.factoryio.a_core.registery.FactoryIORegistryScreens;
import com.mojang.logging.LogUtils;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
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
        IEventBus eventBus = FMLJavaModLoadingContext.get().getModEventBus();

        FactoryIORegistry.createEntitiesFromConfigs();
        FactoryIORegistry.init(eventBus);

        eventBus.addListener(this::setup);
        eventBus.addListener(this::clientSetup);
        eventBus.addListener(this::registerRenderers);

        GeckoLib.initialize();

        MinecraftForge.EVENT_BUS.register(this);
    }

    private void setup(final FMLCommonSetupEvent event)
    {
        event.enqueueWork(FactoryIORegistryNetworks::init);
    }

    public void clientSetup(final FMLClientSetupEvent event) {
        FactoryIORegistryScreens.init();
    }

    private void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        FactoryIORegistryRenderers.init(event);
    }
}
