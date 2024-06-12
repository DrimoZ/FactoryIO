package com.drimoz.factoryio.a_core.inserters;


import com.drimoz.factoryio.FactoryIO;
import com.drimoz.factoryio.a_core.generic.block.FactoryIOEntityBlockWaterLogged;
import com.drimoz.factoryio.a_core.generic.tag.FactoryIOTags;
import com.drimoz.factoryio.a_core.models.InserterData;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraftforge.network.NetworkHooks;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;

public class FactoryIOInserterEntityBlock extends FactoryIOEntityBlockWaterLogged {

    // Private properties

    private static final VoxelShape SHAPE =  Block.box(0, 0, 0, 16, 16, 16);
    private final InserterData INSERTER_DATA;

    // Life cycle

    public FactoryIOInserterEntityBlock(Properties pProperties, InserterData inserterData) {
        super(pProperties, inserterData.isWaterlogged);

        this.INSERTER_DATA = inserterData;
    }

    // Interface (Shape)

    @Override
    public VoxelShape getShape(BlockState pState, BlockGetter pLevel, BlockPos pPos, CollisionContext pContext) {
        return SHAPE;
    }

    @Override
    public RenderShape getRenderShape(BlockState pState) {
        return RenderShape.ENTITYBLOCK_ANIMATED;
    }

    // Interface (Ticks)

    @Nullable
    protected static <T extends BlockEntity> BlockEntityTicker<T> createTicker(Level pLevel, BlockEntityType<T> eTypeT, BlockEntityType<? extends FactoryIOInserterBlockEntity> eTypeI) {
        return pLevel.isClientSide ? null : createTickerHelper(eTypeT, eTypeI, FactoryIOInserterBlockEntity::tick);
    }

    // Interface (Interactions)

    @Override
    public void onRemove(BlockState pState, Level pLevel, BlockPos pPos, BlockState pNewState, boolean pIsMoving) {
        if (pState.getBlock() != pNewState.getBlock()) {
            BlockEntity blockEntity = pLevel.getBlockEntity(pPos);
            if (blockEntity instanceof FactoryIOInserterBlockEntity) {
                ((FactoryIOInserterBlockEntity) blockEntity).drops();
                pLevel.updateNeighbourForOutputSignal(pPos, this);
            }
        }
        super.onRemove(pState, pLevel, pPos, pNewState, pIsMoving);
    }

    @Override
    public InteractionResult use(BlockState pState, Level pLevel, BlockPos pPos, Player pPlayer, InteractionHand pHand, BlockHitResult pHit) {

        if (pLevel.isClientSide) return InteractionResult.SUCCESS;

        BlockEntity blockEntity = pLevel.getBlockEntity(pPos);

        if(blockEntity instanceof FactoryIOInserterBlockEntity) {
            if (pPlayer.getItemInHand(pHand).is(FactoryIOTags.Items.WRENCH_ITEM)) {
                pLevel.setBlock(pPos, pState.rotate(pLevel, pPos, Rotation.CLOCKWISE_90), 3);
            }
            else {
                NetworkHooks.openGui(((ServerPlayer)pPlayer), (FactoryIOInserterBlockEntity)blockEntity, pPos);
            }
        } else {
            throw new IllegalStateException("Missing Container Provider for FactoryIOInserterBlockEntity");
        }

        return InteractionResult.SUCCESS;
    }



    // Interface BlockEntity
    @Override
    public BlockEntity newBlockEntity(@NotNull BlockPos pPos, @NotNull BlockState pState) {
        return new FactoryIOInserterBlockEntity(INSERTER_DATA.registries().getMenu().get(), INSERTER_DATA.registries().getBlockEntity().get(), pPos, pState, INSERTER_DATA);
    }
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, @NotNull BlockState blockState, @NotNull BlockEntityType<T> blockEntityType) {
        return level.isClientSide ? null : createTicker(level, blockEntityType, INSERTER_DATA.registries().getBlockEntity().get());
    }
}
