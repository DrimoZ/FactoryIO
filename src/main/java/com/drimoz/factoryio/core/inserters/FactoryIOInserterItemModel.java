package com.drimoz.factoryio.core.inserters;

import com.drimoz.factoryio.core.model.Inserter;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class FactoryIOInserterItemModel extends GeoModel<FactoryIOInserterItem> {

    // Private properties

    private final Inserter inserter;

    // Life cycle

    public FactoryIOInserterItemModel(Inserter inserter) {
        this.inserter = inserter;
    }

    // Interface

    @Override
    public ResourceLocation getModelResource(FactoryIOInserterItem object) {
        return FactoryIOInserterGeo.modelFor(inserter);
    }

    @Override
    public ResourceLocation getTextureResource(FactoryIOInserterItem object) {
        return FactoryIOInserterGeo.textureFor(inserter, true);
    }

    @Override
    public ResourceLocation getAnimationResource(FactoryIOInserterItem animatable) {
        return FactoryIOInserterGeo.ANIMATIONS;
    }
}
