package com.drimoz.factoryio.core.network.packet;

import com.drimoz.factoryio.a_core.inserters.FactoryIOInserterBlockEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class FactoryIOSyncS2CWhitelistButton {
    private final int set;
    private final int index;
    private final BlockPos pos;

    public FactoryIOSyncS2CWhitelistButton(int set, int index, BlockPos pos) {
        this.set = set;
        this.index = index;
        this.pos = pos;
    }

    public FactoryIOSyncS2CWhitelistButton(FriendlyByteBuf buf) {
        this.set = buf.readInt();
        this.index = buf.readInt();
        this.pos = buf.readBlockPos();
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeInt(set);
        buf.writeInt(index);
        buf.writeBlockPos(pos);
    }

    public boolean handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> {
            if(Minecraft.getInstance().level.getBlockEntity(pos) instanceof FactoryIOInserterBlockEntity blockEntity) {
                if (index == 6 && blockEntity.IS_FILTER) {
                    if (blockEntity.isWhitelist() != (set == 1 ? true: false)) {
                        blockEntity.setWhitelist(set == 1 ? true: false);
                        blockEntity.setChanged();
                    }
                }
            }
        });
        return true;
    }
}