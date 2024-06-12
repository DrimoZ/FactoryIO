package com.drimoz.factoryio.a_core.generic.block;

import com.drimoz.factoryio.FactoryIO;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SimpleWaterloggedBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import org.jetbrains.annotations.NotNull;

public abstract class FactoryIOEntityBlockWaterLogged extends FactoryIOEntityBlock implements SimpleWaterloggedBlock {

    // Public constants

    public static final BooleanProperty WATERLOGGED = BlockStateProperties.WATERLOGGED;

    // Private properties

    private final boolean isWaterLogged;

    protected FactoryIOEntityBlockWaterLogged(Properties pProperties, boolean isWaterLogged) {
        super(pProperties);
        FactoryIO.LOGGER.error("Inserter waterlogged: " + isWaterLogged);
        FactoryIO.LOGGER.error("Property: " + WATERLOGGED);
        this.isWaterLogged = isWaterLogged;

        if (isWaterLogged) this.registerDefaultState(this.stateDefinition.any().setValue(ENABLED, Boolean.TRUE).setValue(WATERLOGGED, false));
        else this.registerDefaultState(this.stateDefinition.any().setValue(ENABLED, Boolean.TRUE));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> pBuilder) {
        if (isWaterLogged) pBuilder.add(FACING, ENABLED, WATERLOGGED);
        else pBuilder.add(FACING, ENABLED, WATERLOGGED);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext pContext) {
        this.defaultBlockState().setValue(FACING, pContext.getHorizontalDirection().getOpposite()).setValue(ENABLED, Boolean.TRUE);

        FluidState fluidState = pContext.getLevel().getFluidState(pContext.getClickedPos());
        if (isWaterLogged) this.defaultBlockState().setValue(WATERLOGGED, fluidState.getType() == Fluids.WATER);

        return this.defaultBlockState();
    }

    public @NotNull FluidState getFluidState(@NotNull BlockState state) {
        return isWaterLogged && (Boolean)state.getValue(WATERLOGGED) ? Fluids.WATER.getSource(false) : super.getFluidState(state);
    }

    @Override
    public BlockState updateShape(BlockState stateIn, Direction facing, BlockState facingState, LevelAccessor worldIn, BlockPos currentPos, BlockPos facingPos) {
        if (isWaterLogged && (Boolean)stateIn.getValue(WATERLOGGED)) {
            worldIn.scheduleTick(currentPos, Fluids.WATER, Fluids.WATER.getTickDelay(worldIn));
        }

        return super.updateShape(stateIn, facing, facingState, worldIn, currentPos, facingPos);
    }
}
