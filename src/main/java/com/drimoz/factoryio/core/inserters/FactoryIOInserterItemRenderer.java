package com.drimoz.factoryio.core.inserters;

import com.drimoz.factoryio.core.model.Inserter;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import software.bernie.geckolib3.renderers.geo.GeoItemRenderer;

@OnlyIn(Dist.CLIENT)
public class FactoryIOInserterItemRenderer extends GeoItemRenderer<FactoryIOInserterItem> {
    public FactoryIOInserterItemRenderer(Inserter inserter) {
        super(new FactoryIOInserterItemModel(inserter));
    }
}
