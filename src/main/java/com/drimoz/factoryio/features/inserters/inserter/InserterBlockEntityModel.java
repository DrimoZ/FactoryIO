package com.drimoz.factoryio.features.inserters.inserter;

import com.drimoz.factoryio.FactoryIO;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib3.model.AnimatedGeoModel;

public class InserterBlockEntityModel extends AnimatedGeoModel<InserterBlockEntity> {
    @Override
    public ResourceLocation getModelLocation(InserterBlockEntity object) {
        return new ResourceLocation(FactoryIO.MOD_ID, "geo/inserter.geo.json");
    }

    @Override
    public ResourceLocation getTextureLocation(InserterBlockEntity object) {
        return new ResourceLocation(FactoryIO.MOD_ID, "textures/block/inserter.png");
    }

    @Override
    public ResourceLocation getAnimationFileLocation(InserterBlockEntity animatable) {
        return new ResourceLocation(FactoryIO.MOD_ID, "animations/animated_block.animation.json");
    }
}