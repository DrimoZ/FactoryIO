package com.drimoz.factoryio.core.inserters;

import com.drimoz.factoryio.core.model.Inserter;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class InserterItemGeoModel extends GeoModel<InserterItem> {

    // Private properties

    private final Inserter inserter;

    // Life cycle

    public InserterItemGeoModel(Inserter inserter) {
        this.inserter = inserter;
    }

    // Interface

    @Override
    public ResourceLocation getModelResource(InserterItem object) {
        return InserterGeo.modelFor(inserter);
    }

    @Override
    public ResourceLocation getTextureResource(InserterItem object) {
        return InserterGeo.textureFor(inserter, true);
    }

    @Override
    public ResourceLocation getAnimationResource(InserterItem animatable) {
        return InserterGeo.ANIMATIONS;
    }
}
