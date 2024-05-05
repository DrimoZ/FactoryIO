package com.drimoz.factoryio.core.blockentities.chests;

import com.drimoz.factoryio.core.blockentities.FactoryIOBlockEntity;
import com.drimoz.factoryio.core.blockentities.FactoryIOMenuProvidedBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

public abstract class FactoryIOChestBlockEntity extends FactoryIOMenuProvidedBlockEntity {
    public FactoryIOChestBlockEntity(BlockEntityType<?> pType, BlockPos pPos, BlockState pBlockState) {
        super(pType, pPos, pBlockState);
    }
}
