package com.drimoz.factoryio.core.blockentities.inserters;

import com.drimoz.factoryio.FactoryIO;
import com.drimoz.factoryio.features.inserters.inserter.InserterBlockEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib3.model.AnimatedGeoModel;

public class FactoryIOInserterBlockEntityModel extends AnimatedGeoModel<InserterBlockEntity> {

    private final String identifier;

    FactoryIOInserterBlockEntityModel(String identifier) {
        this.identifier = identifier;
    }

    @Override
    public ResourceLocation getModelLocation(InserterBlockEntity object) {
        return new ResourceLocation(FactoryIO.MOD_ID, "geo/" + identifier + ".geo.json");
    }

    @Override
    public ResourceLocation getTextureLocation(InserterBlockEntity object) {
        return new ResourceLocation(FactoryIO.MOD_ID, "textures/block/" + identifier + ".png");
    }

    @Override
    public ResourceLocation getAnimationFileLocation(InserterBlockEntity animatable) {
        return new ResourceLocation(FactoryIO.MOD_ID, "animations/animated_block.animation.json");
    }
}