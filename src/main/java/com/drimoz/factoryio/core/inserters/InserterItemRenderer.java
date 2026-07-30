package com.drimoz.factoryio.core.inserters;

import com.drimoz.factoryio.core.model.Inserter;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import software.bernie.geckolib.renderer.GeoItemRenderer;

@OnlyIn(Dist.CLIENT)
public class InserterItemRenderer extends GeoItemRenderer<InserterItem> {
    public InserterItemRenderer(Inserter inserter) {
        super(new InserterItemGeoModel(inserter));
    }
}
