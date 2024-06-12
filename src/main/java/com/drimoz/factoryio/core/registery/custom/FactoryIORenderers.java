package com.drimoz.factoryio.core.registery.custom;


import com.drimoz.factoryio.core.registery.loaders.FactoryIODataLoader;
import com.drimoz.factoryio.core.registery.models.InserterData;
import com.drimoz.factoryio.a_core.inserters.FactoryIOInserterBlockEntityRenderer;
import net.minecraftforge.client.event.EntityRenderersEvent;

public class FactoryIORenderers {
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        for (InserterData inserterData : FactoryIODataLoader.INSERTER_DATA_LIST) {
            event.registerBlockEntityRenderer(
                    inserterData.registries().getBlockEntity().get(),
                    (pContext -> new FactoryIOInserterBlockEntityRenderer(pContext, inserterData.identifier)));
        }
    }
}
