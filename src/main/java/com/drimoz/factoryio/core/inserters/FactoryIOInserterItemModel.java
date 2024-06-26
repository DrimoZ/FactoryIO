package com.drimoz.factoryio.core.inserters;

import com.drimoz.factoryio.FactoryIO;
import com.drimoz.factoryio.core.model.Inserter;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib3.model.AnimatedGeoModel;

public class FactoryIOInserterItemModel extends AnimatedGeoModel<FactoryIOInserterItem> {

    // Private properties

    private final Inserter inserter;

    public FactoryIOInserterItemModel(Inserter inserter) {
        super();

        this.inserter = inserter;
    }

    @Override
    public ResourceLocation getModelLocation(FactoryIOInserterItem object) {
        if (inserter.isFilterable())
            return new ResourceLocation(FactoryIO.MOD_ID, "geo/filter_inserter.geo.json");
        else if (inserter.useEnergy())
            return new ResourceLocation(FactoryIO.MOD_ID, "geo/energy_inserter.geo.json");
        else
            return new ResourceLocation(FactoryIO.MOD_ID, "geo/fuel_inserter.geo.json");
    }

    @Override
    public ResourceLocation getTextureLocation(FactoryIOInserterItem object) {
        return new ResourceLocation(FactoryIO.MOD_ID, "textures/block/inserters/" + inserter.getName() + ".png");
    }

    @Override
    public ResourceLocation getAnimationFileLocation(FactoryIOInserterItem animatable) {
        return new ResourceLocation(FactoryIO.MOD_ID, "animations/animated_block.animation.json");
    }
}