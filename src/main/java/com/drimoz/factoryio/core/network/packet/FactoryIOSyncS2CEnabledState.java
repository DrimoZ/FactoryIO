package com.drimoz.factoryio.core.network.packet;

import com.drimoz.factoryio.core.inserters.FactoryIOInserterBlockEntity;
import com.drimoz.factoryio.core.inserters.FactoryIOInserterEntityBlock;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class FactoryIOSyncS2CEnabledState {
    private final boolean set;
    private final BlockPos pos;

    public FactoryIOSyncS2CEnabledState(boolean set, BlockPos pos) {
        this.set = set;
        this.pos = pos;
    }

    public FactoryIOSyncS2CEnabledState(FriendlyByteBuf buf) {
        this.set = buf.readBoolean();
        this.pos = buf.readBlockPos();
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeBoolean(set);
        buf.writeBlockPos(pos);
    }

    public boolean handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> {
            if(Minecraft.getInstance().level.getBlockEntity(pos) instanceof FactoryIOInserterBlockEntity blockEntity) {
                BlockState pState = blockEntity.getBlockState().setValue(FactoryIOInserterEntityBlock.ENABLED, set);
                Minecraft.getInstance().level.setBlock(pos, pState, 3);
                blockEntity.setChanged();
            }
        });
        return true;
    }
}