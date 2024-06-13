package com.drimoz.factoryio.core.generic.block_entity;

import net.minecraft.core.BlockPos;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

public abstract class FactoryIOBlockEntityMenuProvided extends FactoryIOBlockEntity implements MenuProvider {
    public FactoryIOBlockEntityMenuProvided(BlockEntityType<?> pType, BlockPos pPos, BlockState pBlockState) {
        super(pType, pPos, pBlockState);
    }
}
