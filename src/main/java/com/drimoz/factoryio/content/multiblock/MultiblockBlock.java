package com.drimoz.factoryio.content.multiblock;

import com.drimoz.factoryio.core.generic.block.ModEntityBlock;
import com.drimoz.factoryio.core.init.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.Shapes;

import javax.annotation.Nullable;

/**
 * Le bloc <b>maître</b> d'un multibloc : celui que pose l'item, au centre-bas du volume.
 *
 * <p>Il porte toute la logique ; les autres positions sont des
 * {@link MultiblockPartBlock} qui ne font que lui renvoyer ce qu'on leur demande. Poser,
 * casser, alimenter en redstone n'importe laquelle de ces positions revient à agir sur
 * lui. Voir <a href="../../../../../../../docs/12-DESIGN-CRAFTER.md">12</a> §2.
 *
 * <p>Les sous-classes déclarent {@code PushReaction.BLOCK} dans leurs propriétés : un
 * piston qui déplacerait le seul maître laisserait ses parties orphelines.
 */
public abstract class MultiblockBlock extends ModEntityBlock {

    private final MultiblockShape shape;

    protected MultiblockBlock(Properties properties, MultiblockShape shape) {
        super(properties);

        this.shape = shape;
    }

    public MultiblockShape shape() {
        return this.shape;
    }

    // Interface (Pose)

    /** {@code null} annule la pose : c'est ainsi que vanilla refuse un bloc. */
    @Override
    @Nullable
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockState state = super.getStateForPlacement(context);

        return canPlace(context, state.getValue(FACING)) ? state : null;
    }

    /**
     * Toutes les positions du volume sont remplaçables et libres d'entité.
     *
     * <p>Le maître lui-même est déjà vérifié par {@code BlockItem} ; le reprendre ne coûte
     * rien et garde l'écran de prévisualisation d'accord avec la pose réelle.
     */
    public boolean canPlace(BlockPlaceContext context, Direction facing) {
        Level level = context.getLevel();

        for (BlockPos pos : this.shape.positions(context.getClickedPos(), facing)) {
            if (level.isOutsideBuildHeight(pos)) return false;
            if (!level.getBlockState(pos).canBeReplaced(context)) return false;
            if (!level.isUnobstructed(null, Shapes.block().move(pos.getX(), pos.getY(), pos.getZ()))) return false;
        }
        return true;
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);

        placeParts(level, pos, state.getValue(FACING));
    }

    /** Pose les parties autour d'un maître déjà en place. */
    public void placeParts(Level level, BlockPos master, Direction facing) {
        BlockState part = ModBlocks.MULTIBLOCK_PART.get().defaultBlockState();

        for (BlockPos pos : this.shape.positions(master, facing)) {
            if (pos.equals(master)) continue;

            level.setBlock(pos, part, Block.UPDATE_ALL);
            if (level.getBlockEntity(pos) instanceof MultiblockPartBlockEntity partEntity) {
                partEntity.setMaster(master);
            }
        }
    }

    // Interface (Casse)

    /**
     * Le maître parti, ses parties le suivent — sans rien lâcher : l'item et le contenu
     * sortent du maître seul.
     *
     * <p>Une partie dans un chunk non chargé est laissée en place plutôt que de forcer son
     * chargement ; orpheline, elle ne renvoie plus rien.
     */
    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        if (!state.is(newState.getBlock())) {
            for (BlockPos partPos : this.shape.positions(pos, state.getValue(FACING))) {
                if (partPos.equals(pos) || !level.isLoaded(partPos)) continue;

                if (level.getBlockEntity(partPos) instanceof MultiblockPartBlockEntity part
                        && pos.equals(part.masterPos())) {
                    level.removeBlock(partPos, false);
                }
            }
        }

        super.onRemove(state, level, pos, newState, isMoving);
    }

    // Interface (Redstone)

    /** Un signal sur n'importe quelle partie compte, pas seulement sur le maître. */
    @Override
    protected boolean shouldBeEnabled(Level level, BlockPos pos) {
        if (!isAffectedByRedstone()) return true;

        BlockState state = level.getBlockState(pos);
        if (!state.is(this)) return super.shouldBeEnabled(level, pos);

        for (BlockPos position : this.shape.positions(pos, state.getValue(FACING))) {
            if (level.hasNeighborSignal(position)) return false;
        }
        return true;
    }
}
