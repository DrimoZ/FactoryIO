package com.drimoz.factoryio.core.generic.block;

import com.drimoz.factoryio.core.inserters.InserterBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;

import javax.annotation.Nullable;

public abstract class ModEntityBlock extends BaseEntityBlock {
    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
    public static final BooleanProperty ENABLED = BlockStateProperties.ENABLED;

    protected ModEntityBlock(Properties pProperties) {
        super(pProperties);

    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext pContext) {
        return this.defaultBlockState().setValue(FACING, pContext.getHorizontalDirection().getOpposite()).setValue(ENABLED, Boolean.valueOf(true));
    }

    @Override
    public BlockState rotate(BlockState pState, Rotation pRotation) {
        return pState.setValue(FACING, pRotation.rotate(pState.getValue(FACING)));
    }

    @Override
    public BlockState mirror(BlockState pState, Mirror pMirror) {
        return pState.rotate(pMirror.getRotation(pState.getValue(FACING)));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> pBuilder) {
        pBuilder.add(FACING, ENABLED);
    }

    @Override
    public RenderShape getRenderShape(BlockState pState) {
        return RenderShape.MODEL;
    }

    public BlockState updateShape(BlockState stateIn, Direction facing, BlockState facingState, LevelAccessor worldIn, BlockPos currentPos, BlockPos facingPos) {
        return super.updateShape(stateIn, facing, facingState, worldIn, currentPos, facingPos);
    }

    @Override
    public void onPlace(BlockState pState, Level pLevel, BlockPos pPos, BlockState pOldState, boolean pIsMoving) {
        super.onPlace(pState, pLevel, pPos, pOldState, pIsMoving);

        if (!pOldState.is(pState.getBlock())) {
            this.checkPoweredState(pLevel, pPos, pState);
        }
    }

    @Override
    public void neighborChanged(BlockState pState, Level pLevel, BlockPos pPos, Block pBlock, BlockPos pFromPos, boolean pIsMoving) {
        super.neighborChanged(pState, pLevel, pPos, pBlock, pFromPos, pIsMoving);

        this.checkPoweredState(pLevel, pPos, pState);

        // Un coffre posé ou cassé à côté doit invalider les inventaires mémorisés et
        // relancer la machine (cf. DT-07).
        if (!pLevel.isClientSide && pLevel.getBlockEntity(pPos) instanceof InserterBlockEntity inserter) {
            inserter.onNeighbourChanged();
        }
    }

    /**
     * Aligne la propriété {@code ENABLED} sur l'absence de signal redstone.
     *
     * <p>Trois corrections par rapport à la version précédente (cf. BUG-015) :
     * l'exécution est réservée au serveur, le bloc doit déclarer réagir au redstone, et
     * surtout {@code setBlock} utilise {@code UPDATE_ALL} — l'ancien flag 5 omettait le
     * bit « notifier les clients », d'où un état jamais propagé. C'est précisément ce
     * que contournait le paquet {@code SyncS2CEnabledState} envoyé à chaque tick.
     */
    private void checkPoweredState(Level pLevel, BlockPos pPos, BlockState pState) {
        if (pLevel.isClientSide) return;
        if (!isAffectedByRedstone()) return;

        boolean enabled = shouldBeEnabled(pLevel, pPos);
        if (enabled != pState.getValue(ENABLED)) {
            pLevel.setBlock(pPos, pState.setValue(ENABLED, enabled), Block.UPDATE_ALL);
        }
    }

    /** Un bloc qui ne réagit pas au redstone reste toujours actif. */
    protected boolean isAffectedByRedstone() {
        return true;
    }

    /**
     * Décide de l'état d'activation pour le signal courant.
     *
     * <p>Par défaut, la règle vanilla : actif tant qu'aucun signal n'arrive. Les inserters
     * la remplacent par une condition analogique réglable (cf. FIO-070).
     */
    protected boolean shouldBeEnabled(Level pLevel, BlockPos pPos) {
        return !pLevel.hasNeighborSignal(pPos);
    }


    public MenuProvider getMenuProvider(BlockState pBlockState, Level pLevel, BlockPos pPos) {
        BlockEntity blockEntity = pLevel.getBlockEntity(pPos);
        return blockEntity instanceof MenuProvider ? (MenuProvider)blockEntity : null;
    }

    @Nullable
    protected static <E extends BlockEntity, A extends BlockEntity> BlockEntityTicker<A> createTickerHelper(BlockEntityType<A> eTypeA, BlockEntityType<E> eTypeE, BlockEntityTicker<? super E> eTickerE) {
        return eTypeE == eTypeA ? (BlockEntityTicker<A>) eTickerE : null;
    }
}
