package com.drimoz.factoryio.core.power;

import com.drimoz.factoryio.core.init.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.Nullable;

/**
 * Bloc de la source d'énergie inépuisable.
 *
 * <p>Volontairement <b>sans recette</b> : il n'est accessible qu'en créatif. Lui en donner
 * une supprimerait toute progression énergétique, alors que le mod n'a pas encore décidé
 * s'il produit sa propre énergie ou s'il s'appuie sur un mod tiers — c'est la question de
 * périmètre de la Phase 4, et un bloc craftable la trancherait par accident.
 */
public class CreativeEnergySourceBlock extends BaseEntityBlock {

    public CreativeEnergySourceBlock(Properties properties) {
        super(properties);
    }

    // Interface (BlockEntity)

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new CreativeEnergySourceBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(
            Level level, BlockState state, BlockEntityType<T> type) {

        if (level.isClientSide) return null;

        return createTickerHelper(type, ModBlocks.CREATIVE_ENERGY_SOURCE_ENTITY.get(),
                CreativeEnergySourceBlockEntity::tick);
    }

    /** Un bloc plein ordinaire : le rendu passe par son modèle, pas par un renderer. */
    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    // Interface (Voisinage)

    /**
     * Un consommateur posé à côté doit être alimenté au tick suivant, pas au bout du délai
     * qu'aurait mis le cache à s'invalider tout seul.
     */
    @Override
    public void neighborChanged(BlockState state, Level level, BlockPos pos, Block block, BlockPos fromPos, boolean isMoving) {
        super.neighborChanged(state, level, pos, block, fromPos, isMoving);

        if (level.isClientSide) return;

        if (level.getBlockEntity(pos) instanceof CreativeEnergySourceBlockEntity source) {
            source.onNeighbourChanged();
        }
    }
}
