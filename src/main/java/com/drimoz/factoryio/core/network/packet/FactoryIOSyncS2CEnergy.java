package com.drimoz.factoryio.core.network.packet;

import com.drimoz.factoryio.a_core.inserters.FactoryIOInserterBlockEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class FactoryIOSyncS2CEnergy {
    private final int energy;
    private final BlockPos pos;

    public FactoryIOSyncS2CEnergy(int energy, BlockPos pos) {
        this.energy = energy;
        this.pos = pos;
    }

    public FactoryIOSyncS2CEnergy(FriendlyByteBuf buf) {
        this.energy = buf.readInt();
        this.pos = buf.readBlockPos();
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeInt(energy);
        buf.writeBlockPos(pos);
    }

    public boolean handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> {
            if(Minecraft.getInstance().level.getBlockEntity(pos) instanceof FactoryIOInserterBlockEntity blockEntity) {
                if (blockEntity.IS_ENERGY) {
                    blockEntity.overrideCurrentEnergy(energy);
                }
            }
        });
        return true;
    }
}