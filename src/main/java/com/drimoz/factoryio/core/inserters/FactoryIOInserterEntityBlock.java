package com.drimoz.factoryio.core.inserters;


import com.drimoz.factoryio.FactoryIO;
import com.drimoz.factoryio.core.generic.block.FactoryIOEntityBlockWaterLogged;
import com.drimoz.factoryio.core.init.FactoryIOTags;
import com.drimoz.factoryio.core.model.Inserter;
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
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraftforge.network.NetworkHooks;

import javax.annotation.Nullable;

public class FactoryIOInserterEntityBlock extends FactoryIOEntityBlockWaterLogged {

    // Private properties

    /**
     * Calquée sur la géométrie GeckoLib : socle d'environ 6×6 blocs de large sur 5 de
     * haut, surmonté du palier. La version précédente utilisait un cube plein 16³, sans
     * rapport avec le modèle affiché (cf. BUG-017).
     */
    private static final VoxelShape SHAPE = Shapes.or(
            Block.box(4, 0, 4, 12, 5, 12),
            Block.box(6, 5, 6, 10, 12, 10));

    private final Inserter inserter;

    // Life cycle

    public FactoryIOInserterEntityBlock(Properties pProperties, Inserter inserter) {
        super(pProperties);

        this.inserter = inserter;
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

        if (!(pLevel.getBlockEntity(pPos) instanceof FactoryIOInserterBlockEntity blockEntity)) {
            // Ne pas lever d'exception : un BlockEntity manquant est une anomalie
            // récupérable, pas un motif de crash serveur.
            FactoryIO.LOGGER.warn("Aucun FactoryIOInserterBlockEntity en {}", pPos);
            return InteractionResult.PASS;
        }

        if (isRotationTool(pPlayer, pHand)) {
            pLevel.setBlock(pPos, pState.rotate(pLevel, pPos, Rotation.CLOCKWISE_90), Block.UPDATE_ALL);
            return InteractionResult.CONSUME;
        }

        NetworkHooks.openScreen((ServerPlayer) pPlayer, blockEntity, pPos);
        return InteractionResult.CONSUME;
    }

    /**
     * Le tag {@code forge:tools/wrench} est vide tant qu'aucun mod ne le peuple. Le
     * shift + clic droit à main nue offre donc une alternative toujours disponible
     * (cf. BUG-026).
     */
    private static boolean isRotationTool(Player player, InteractionHand hand) {
        ItemStack held = player.getItemInHand(hand);

        if (held.is(FactoryIOTags.Items.WRENCH_ITEM)) return true;

        return held.isEmpty() && player.isSecondaryUseActive();
    }



    // Interface BlockEntity
    @Override
    public BlockEntity newBlockEntity(BlockPos pPos, BlockState pState) {
        return new FactoryIOInserterBlockEntity(inserter.getBlockEntityType().get(), pPos, pState, inserter);
    }
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState blockState, BlockEntityType<T> blockEntityType) {
        return level.isClientSide ? null : createTicker(level, blockEntityType, inserter.getBlockEntityType().get());
    }
}
