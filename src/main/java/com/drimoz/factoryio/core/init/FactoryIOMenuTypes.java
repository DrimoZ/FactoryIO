package com.drimoz.factoryio.core.init;

import com.drimoz.factoryio.core.registery.FactoryIOInserterRegistry;
import net.minecraft.world.inventory.MenuType;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.registries.IForgeRegistry;

import java.util.Objects;

public class FactoryIOMenuTypes {
    public FactoryIOMenuTypes() {
    }

    @SubscribeEvent
    public void onRegisterItems(RegistryEvent.Register<MenuType<?>> event) {
        IForgeRegistry<MenuType<?>> registry = event.getRegistry();
        Objects.requireNonNull(registry);

        FactoryIOInserterRegistry.getInstance().onRegisterContainers(registry);
    }
}
