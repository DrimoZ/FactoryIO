package com.drimoz.factoryio.core.items.inserters;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import software.bernie.geckolib3.renderers.geo.GeoItemRenderer;

@OnlyIn(Dist.CLIENT)
public class FactoryIOInserterItemRenderer extends GeoItemRenderer<FactoryIOInserterItem> {
    public FactoryIOInserterItemRenderer(String identifier) {
        super(new FactoryIOInserterItemModel(identifier));
    }
}
