package com.drimoz.factoryio.core.inserters;

import com.drimoz.factoryio.FactoryIO;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib3.model.AnimatedGeoModel;

public class FactoryIOInserterBlockEntityModel extends AnimatedGeoModel<FactoryIOInserterBlockEntity> {

    private final String identifier;

    FactoryIOInserterBlockEntityModel(String identifier) {
        this.identifier = identifier;
    }

    @Override
    public ResourceLocation getModelLocation(FactoryIOInserterBlockEntity object) {
        return new ResourceLocation(FactoryIO.MOD_ID, "geo/inserter.geo.json");
    }

    @Override
    public ResourceLocation getTextureLocation(FactoryIOInserterBlockEntity object) {
        return new ResourceLocation(FactoryIO.MOD_ID, "textures/block/" + identifier + ".png");
    }

    @Override
    public ResourceLocation getAnimationFileLocation(FactoryIOInserterBlockEntity animatable) {
        return new ResourceLocation(FactoryIO.MOD_ID, "animations/animated_block.animation.json");
    }
}