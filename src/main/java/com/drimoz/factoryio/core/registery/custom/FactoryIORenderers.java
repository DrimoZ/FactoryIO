package com.drimoz.factoryio.core.registery.custom;


import com.drimoz.factoryio.features.inserters.inserter.InserterBlockEntityRenderer;
import net.minecraftforge.client.event.EntityRenderersEvent;

public class FactoryIORenderers {
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(FactoryIOBlockEntities.BLOCK_ENTITY_INSERTER.get(), InserterBlockEntityRenderer::new);
    }
}
