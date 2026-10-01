package com.drimoz.factoryio.content.crafter;

import com.drimoz.factoryio.content.multiblock.MultiblockBlock;
import com.drimoz.factoryio.content.multiblock.MultiblockShape;
import com.drimoz.factoryio.core.init.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;

import javax.annotation.Nullable;

/**
 * Le maître d'un crafter : 3×2×3, un bloc par palier. Voir docs/12 §4.
 *
 * <p>{@link #WORKING} porte l'état visuel jusqu'au client sans paquet périodique : il ne
 * change qu'aux transitions, avec l'hystérésis de {@link CrafterBlockEntity}.
 */
public class CrafterBlock extends MultiblockBlock {

    public static final MultiblockShape SHAPE = new MultiblockShape(3, 2, 3);
    public static final BooleanProperty WORKING = BooleanProperty.create("working");

    private final Crafter crafter;

    public CrafterBlock(Properties properties, Crafter crafter) {
        super(properties, SHAPE);

        this.crafter = crafter;
        // Sans cela, l'état par défaut serait le premier de la liste : WORKING=true.
        registerDefaultState(this.stateDefinition.any().setValue(WORKING, false).setValue(ENABLED, true));
    }

    public Crafter getCrafter() {
        return this.crafter;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(WORKING);
    }

    // Interface (BlockEntity)

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new CrafterBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide ? null : createTickerHelper(type, ModBlocks.CRAFTER_ENTITY.get(), CrafterBlockEntity::tick);
    }

    // Interface (Casse)

    /** Tout le contenu sort, entrées comprises : un craft en cours n'a encore rien consommé. */
    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        if (!state.is(newState.getBlock()) && level.getBlockEntity(pos) instanceof CrafterBlockEntity crafter) {
            crafter.dropContents();
        }

        super.onRemove(state, level, pos, newState, isMoving);
    }
}
