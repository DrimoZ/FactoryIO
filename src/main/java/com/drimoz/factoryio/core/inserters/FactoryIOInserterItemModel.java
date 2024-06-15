package com.drimoz.factoryio.core.inserters;

import com.drimoz.factoryio.FactoryIO;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib3.model.AnimatedGeoModel;

public class FactoryIOInserterItemModel extends AnimatedGeoModel<FactoryIOInserterItem> {

    // Private properties

    private final String IDENTIFIER;

    public FactoryIOInserterItemModel(String identifier) {
        super();

        this.IDENTIFIER = identifier;
    }

    @Override
    public ResourceLocation getModelLocation(FactoryIOInserterItem object) {
        return new ResourceLocation(FactoryIO.MOD_ID, "geo/inserter.geo.json");
    }

    @Override
    public ResourceLocation getTextureLocation(FactoryIOInserterItem object) {
        return new ResourceLocation(FactoryIO.MOD_ID, "textures/block/inserters/" + this.IDENTIFIER + ".png");
    }

    @Override
    public ResourceLocation getAnimationFileLocation(FactoryIOInserterItem animatable) {
        return new ResourceLocation(FactoryIO.MOD_ID, "animations/animated_block.animation.json");
    }
}