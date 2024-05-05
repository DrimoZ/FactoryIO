package com.drimoz.factoryio.core.blockentities.convoyers;

import com.drimoz.factoryio.core.blockentities.FactoryIOBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

public abstract class FactoryIOConvoyerBlockEntity extends FactoryIOBlockEntity {
    public FactoryIOConvoyerBlockEntity(BlockEntityType<?> pType, BlockPos pPos, BlockState pBlockState) {
        super(pType, pPos, pBlockState);
    }
}
