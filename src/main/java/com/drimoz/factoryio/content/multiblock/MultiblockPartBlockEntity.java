package com.drimoz.factoryio.content.multiblock;

import com.drimoz.factoryio.core.init.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.LazyOptional;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;

/**
 * Une partie de multibloc : elle ne sait qu'où est son maître.
 *
 * <p>Ses capabilities sont celles du maître, <b>renvoyées telles quelles</b>. Ne pas
 * envelopper le {@code LazyOptional} est ce qui rend les parties transparentes : le cache
 * d'inventaire des inserters s'invalide par ce même objet quand le maître disparaît, et un
 * convoyeur ou un mod tiers ne voit qu'un inventaire ordinaire.
 */
public class MultiblockPartBlockEntity extends BlockEntity {

    private static final String MASTER_TAG = "master";

    /** Décalage vers le maître ; relatif, pour survivre à une structure déplacée. */
    private BlockPos masterOffset = BlockPos.ZERO;

    public MultiblockPartBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlocks.MULTIBLOCK_PART_ENTITY.get(), pos, state);
    }

    public void setMaster(BlockPos master) {
        this.masterOffset = master.subtract(this.worldPosition);
        setChanged();
    }

    /** {@code null} tant qu'aucun maître n'a été désigné. */
    @Nullable
    public BlockPos masterPos() {
        return this.masterOffset.equals(BlockPos.ZERO) ? null : this.worldPosition.offset(this.masterOffset);
    }

    /**
     * Le maître, s'il est chargé et bien un multibloc.
     *
     * <p>Jamais de chargement forcé : {@code getBlockEntity} sur un chunk absent le
     * chargerait, et une capability demandée à la frontière suffirait à le déclencher.
     */
    @Nullable
    public BlockEntity master() {
        BlockPos master = masterPos();
        if (this.level == null || master == null || !this.level.isLoaded(master)) return null;
        if (!(this.level.getBlockState(master).getBlock() instanceof MultiblockBlock)) return null;

        return this.level.getBlockEntity(master);
    }

    @Override
    public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        BlockEntity master = master();

        return master != null ? master.getCapability(cap, side) : super.getCapability(cap, side);
    }

    // Persistance et synchronisation — le client en a besoin pour le bloc choisi (pick block).

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.put(MASTER_TAG, NbtUtils.writeBlockPos(this.masterOffset));
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        this.masterOffset = NbtUtils.readBlockPos(tag.getCompound(MASTER_TAG));
    }

    @Override
    public CompoundTag getUpdateTag() {
        return saveWithoutMetadata();
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}
