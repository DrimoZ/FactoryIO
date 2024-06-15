package com.drimoz.factoryio.core.init;

import com.drimoz.factoryio.core.registery.FactoryIOInserterRegistry;
import net.minecraft.world.item.Item;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.registries.IForgeRegistry;

import java.util.Objects;

public class FactoryIOItems {
    public FactoryIOItems() {
    }

    @SubscribeEvent
    public void onRegisterItems(RegistryEvent.Register<Item> event) {
        IForgeRegistry<Item> registry = event.getRegistry();
        Objects.requireNonNull(registry);

        FactoryIOInserterRegistry.getInstance().onRegisterItems(registry);
    }
}
