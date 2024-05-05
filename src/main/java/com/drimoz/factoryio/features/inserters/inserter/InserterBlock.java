package com.drimoz.factoryio.features.inserters.inserter;

import com.drimoz.factoryio.core.blocks.inserters.FactoryIOInserterEntityBlock;
import com.drimoz.factoryio.core.registery.custom.FactoryIOBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

public class InserterBlock extends FactoryIOInserterEntityBlock {

    public InserterBlock(BlockBehaviour.Properties pProps) {
        super(pProps);
        this.registerDefaultState(this.stateDefinition.any().setValue(ENABLED, Boolean.TRUE).setValue(WATERLOGGED, false));
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pPos, BlockState pState) {
        return new InserterBlockEntity(pPos, pState);
    }

    @javax.annotation.Nullable
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return createTicker(level, type, FactoryIOBlockEntities.BLOCK_ENTITY_INSERTER.get());
    }
}
