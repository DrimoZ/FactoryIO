package com.drimoz.factoryio.features.inserters.inserter;

import com.drimoz.factoryio.core.items.inserters.FactoryIOInserterItem;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import software.bernie.geckolib3.renderers.geo.GeoItemRenderer;

@OnlyIn(Dist.CLIENT)
public class InserterItemRenderer extends GeoItemRenderer<FactoryIOInserterItem> {
    public InserterItemRenderer() {
        super(new InserterItemModel());
    }
}
