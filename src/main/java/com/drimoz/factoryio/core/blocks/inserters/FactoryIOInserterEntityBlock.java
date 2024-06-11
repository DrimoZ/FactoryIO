package com.drimoz.factoryio.core.blocks.inserters;


import com.drimoz.factoryio.core.blockentities.inserters.FactoryIOInserterBlockEntity;
import com.drimoz.factoryio.core.blocks.FactoryIOWaterLoggedEntityBlock;
import com.drimoz.factoryio.core.registery.models.InserterData;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
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

public class FactoryIOInserterEntityBlock extends FactoryIOWaterLoggedEntityBlock {

    // Private properties

    private static final VoxelShape SHAPE =  Block.box(0, 0, 0, 16, 16, 16);
    private final InserterData INSERTER_DATA;

    // Life cycle

    public FactoryIOInserterEntityBlock(Properties pProperties, InserterData inserterData) {
        super(pProperties, inserterData.isWaterlogged);

        this.INSERTER_DATA = inserterData;
    }

    public static FactoryIOInserterEntityBlock create(Properties pProperties, InserterData inserterData) {
        return new FactoryIOInserterEntityBlock(pProperties, inserterData) {

        };
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
            NetworkHooks.openGui(((ServerPlayer)pPlayer), (FactoryIOInserterBlockEntity)blockEntity, pPos);
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
        return level.isClientSide ? createTicker(level, blockEntityType, INSERTER_DATA.registries().getBlockEntity().get()) : null;
    }
}
