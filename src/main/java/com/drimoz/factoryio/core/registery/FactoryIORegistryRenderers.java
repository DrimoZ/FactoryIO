package com.drimoz.factoryio.core.registery;


import com.drimoz.factoryio.core.model.InserterData;
import com.drimoz.factoryio.core.inserters.FactoryIOInserterBlockEntityRenderer;
import net.minecraftforge.client.event.EntityRenderersEvent;

public class FactoryIORegistryRenderers {

    // Interface ( Generic )

    public static void init(EntityRenderersEvent.RegisterRenderers event) {

        /** +++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++ **/
        /**                                                          INSERTERS                                                          **/
        /** +++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++ **/

        for (InserterData inserterData : FactoryIODataLoader.INSERTER_DATA_LIST) {
            event.registerBlockEntityRenderer(
                    inserterData.registries().getBlockEntity().get(),
                    (pContext -> new FactoryIOInserterBlockEntityRenderer(pContext, inserterData.identifier)));
        }
    }
}
