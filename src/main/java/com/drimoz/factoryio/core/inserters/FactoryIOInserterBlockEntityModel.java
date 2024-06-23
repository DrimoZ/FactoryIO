package com.drimoz.factoryio.core.inserters;

import com.drimoz.factoryio.FactoryIO;
import com.drimoz.factoryio.core.model.Inserter;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import software.bernie.geckolib3.model.AnimatedGeoModel;


public class FactoryIOInserterBlockEntityModel extends AnimatedGeoModel<FactoryIOInserterBlockEntity> {

    private final Inserter inserter;

    FactoryIOInserterBlockEntityModel(Inserter inserter) {
        this.inserter = inserter;
    }

    @Override
    public ResourceLocation getModelLocation(FactoryIOInserterBlockEntity object) {
        if (inserter.isFilterable())
            return new ResourceLocation(FactoryIO.MOD_ID, "geo/filter_inserter.geo.json");
        else if (inserter.useEnergy())
            return new ResourceLocation(FactoryIO.MOD_ID, "geo/energy_inserter.geo.json");
        else
            return new ResourceLocation(FactoryIO.MOD_ID, "geo/fuel_inserter.geo.json");
    }

    @Override
    public ResourceLocation getTextureLocation(FactoryIOInserterBlockEntity object) {
        Level level = object.getLevel();
        BlockPos pos = object.getBlockPos();
        BlockState state = level.getBlockState(pos);

        if (state.getValue(FactoryIOInserterEntityBlock.ENABLED)) {
            return new ResourceLocation(FactoryIO.MOD_ID, "textures/block/inserters/" + inserter.getName() + ".png");
        } else {
            return new ResourceLocation(FactoryIO.MOD_ID, "textures/block/inserters/" + inserter.getName() + "_disabled.png");
        }
    }

    @Override
    public ResourceLocation getAnimationFileLocation(FactoryIOInserterBlockEntity animatable) {
        return new ResourceLocation(FactoryIO.MOD_ID, "animations/animated_block.animation.json");
    }
}