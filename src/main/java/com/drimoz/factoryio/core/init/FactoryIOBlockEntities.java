package com.drimoz.factoryio.core.init;

import com.drimoz.factoryio.core.registery.FactoryIOInserterRegistry;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.registries.IForgeRegistry;

import java.util.Objects;

public class FactoryIOBlockEntities {
    public FactoryIOBlockEntities() {
    }

    @SubscribeEvent
    public void onRegisterBlockEntities(RegistryEvent.Register<BlockEntityType<?>> event) {
        IForgeRegistry<BlockEntityType<?>> registry = event.getRegistry();
        Objects.requireNonNull(registry);

        FactoryIOInserterRegistry.getInstance().onRegisterBlockEntities(registry);
    }

    @SubscribeEvent
    @OnlyIn(Dist.CLIENT)
    public void onClientSetup(EntityRenderersEvent.RegisterRenderers event) {
        FactoryIOInserterRegistry.getInstance().onRegisterRenderers(event);
    }
}
