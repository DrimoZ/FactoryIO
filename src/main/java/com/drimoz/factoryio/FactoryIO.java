package com.drimoz.factoryio;

import com.drimoz.factoryio.core.registery.FactoryIORegistry;
import com.mojang.logging.LogUtils;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.InterModComms;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.event.lifecycle.InterModEnqueueEvent;
import net.minecraftforge.fml.event.lifecycle.InterModProcessEvent;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;
import software.bernie.geckolib3.GeckoLib;

import java.util.stream.Collectors;

@Mod(FactoryIO.MOD_ID)
public class FactoryIO
{
    public static final String MOD_ID = "factory_io";
    public static final Logger LOGGER = LogUtils.getLogger();

    public FactoryIO()
    {
        IEventBus eventBus = FMLJavaModLoadingContext.get().getModEventBus();

        FactoryIORegistry.register(eventBus);

        eventBus.addListener(this::setup);
        eventBus.addListener(this::clientSetup);
        eventBus.addListener(this::registerRenderers);

        GeckoLib.initialize();

        MinecraftForge.EVENT_BUS.register(this);
    }



    private void setup(final FMLCommonSetupEvent event)
    {
        FactoryIORegistry.registerNetwork(event);
    }

    public void clientSetup(final FMLClientSetupEvent event) {
        FactoryIORegistry.registerScreens();
    }

    private void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        FactoryIORegistry.registerRenderers(event);
    }
}
