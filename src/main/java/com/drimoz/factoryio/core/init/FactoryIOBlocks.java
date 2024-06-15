package com.drimoz.factoryio.core.init;

import com.drimoz.factoryio.core.registery.FactoryIOInserterRegistry;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.registries.IForgeRegistry;

import java.util.Objects;

public class FactoryIOBlocks {
    public FactoryIOBlocks() {
    }

    @SubscribeEvent
    public void onRegisterBlocks(RegistryEvent.Register<Block> event) {
        IForgeRegistry<Block> registry = event.getRegistry();
        Objects.requireNonNull(registry);

        FactoryIOInserterRegistry.getInstance().setAllowRegistration(true);
        FactoryIOInserterRegistry.getInstance().onRegisterBlocks(registry);
        FactoryIOInserterRegistry.getInstance().setAllowRegistration(false);
    }
}
