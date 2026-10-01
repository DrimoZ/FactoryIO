package com.drimoz.factoryio.core.generic.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;


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

        if (!pLevel.isClientSide) {
            onNeighbourChanged(pLevel, pPos);
        }
    }

    /**
     * Un voisin a changé, côté serveur. Un coffre posé ou cassé à côté doit invalider les
     * inventaires mémorisés et relancer la machine (cf. DT-07) ; chaque bloc sait ce que
     * cela veut dire pour le sien.
     */
    protected void onNeighbourChanged(Level pLevel, BlockPos pPos) {
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
        return !isAffectedByRedstone() || !pLevel.hasNeighborSignal(pPos);
    }
}
