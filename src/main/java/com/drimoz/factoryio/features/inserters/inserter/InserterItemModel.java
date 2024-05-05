package com.drimoz.factoryio.features.inserters.inserter;

import com.drimoz.factoryio.FactoryIO;
import com.drimoz.factoryio.core.items.inserters.FactoryIOInserterItem;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib3.model.AnimatedGeoModel;

public class InserterItemModel extends AnimatedGeoModel<FactoryIOInserterItem> {
    @Override
    public ResourceLocation getModelLocation(FactoryIOInserterItem object) {
        return new ResourceLocation(FactoryIO.MOD_ID, "geo/inserter.geo.json");
    }

    @Override
    public ResourceLocation getTextureLocation(FactoryIOInserterItem object) {
        return new ResourceLocation(FactoryIO.MOD_ID, "textures/block/inserter.png");
    }

    @Override
    public ResourceLocation getAnimationFileLocation(FactoryIOInserterItem animatable) {
        return new ResourceLocation(FactoryIO.MOD_ID, "animations/animated_block.animation.json");
    }
}