package com.drimoz.factoryio.content.multiblock;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.Nullable;

/**
 * Toute position d'un multibloc qui n'est pas le maître. Un seul bloc pour tous les
 * multiblocs : c'est son block entity qui sait à quel maître il appartient.
 *
 * <p>Invisible (le maître dessine tout le volume), sans item ni loot. Casser une partie
 * casse le maître, qui emporte les autres parties avec lui.
 */
public class MultiblockPartBlock extends BaseEntityBlock {

    public MultiblockPartBlock(Properties properties) {
        super(properties);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.INVISIBLE;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new MultiblockPartBlockEntity(pos, state);
    }

    // Interface (Casse)

    /** En créatif, le maître part sans rien lâcher, comme s'il avait été cassé lui-même. */
    @Override
    public void playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        if (!level.isClientSide && player.isCreative()) {
            BlockPos master = masterOf(level, pos);
            if (master != null) level.destroyBlock(master, false, player);
        }

        super.playerWillDestroy(level, pos, state, player);
    }

    /**
     * Quelle que soit la cause — joueur, explosion, {@code /setblock} — une partie qui
     * disparaît emporte le maître, qui lâche l'item et son contenu.
     *
     * <p>Appelé <i>avant</i> le retrait du block entity : on peut encore lire le maître.
     * Quand c'est le maître qui retire ses parties, il n'est déjà plus en place et rien ne
     * se relance.
     */
    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        if (!state.is(newState.getBlock())) {
            BlockPos master = masterOf(level, pos);
            if (master != null) level.destroyBlock(master, true);
        }

        super.onRemove(state, level, pos, newState, isMoving);
    }

    // Interface (Voisinage)

    /** Redstone, inventaire posé à côté : c'est au maître d'en juger. */
    @Override
    public void neighborChanged(BlockState state, Level level, BlockPos pos, Block block, BlockPos fromPos, boolean isMoving) {
        super.neighborChanged(state, level, pos, block, fromPos, isMoving);

        BlockPos master = masterOf(level, pos);
        if (master != null) level.getBlockState(master).neighborChanged(level, master, block, fromPos, isMoving);
    }

    @Override
    public ItemStack getCloneItemStack(BlockGetter level, BlockPos pos, BlockState state) {
        if (level.getBlockEntity(pos) instanceof MultiblockPartBlockEntity part && part.masterPos() != null) {
            return level.getBlockState(part.masterPos()).getBlock().getCloneItemStack(level, part.masterPos(), state);
        }
        return ItemStack.EMPTY;
    }

    // Inner work

    /** Le maître de cette partie, s'il est chargé et toujours un multibloc. */
    @Nullable
    private static BlockPos masterOf(Level level, BlockPos pos) {
        if (!(level.getBlockEntity(pos) instanceof MultiblockPartBlockEntity part)) return null;

        return part.master() != null ? part.masterPos() : null;
    }
}
