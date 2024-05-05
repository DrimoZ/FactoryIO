package com.drimoz.factoryio.core.network.packet;

import com.drimoz.factoryio.core.blockentities.inserters.FactoryIOInserterBlockEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class FactoryIOSyncS2CFuel {
    private final int fuelLevel;
    private final BlockPos pos;

    public FactoryIOSyncS2CFuel(int fuelLevel, BlockPos pos) {
        this.fuelLevel = fuelLevel;
        this.pos = pos;
    }

    public FactoryIOSyncS2CFuel(FriendlyByteBuf buf) {
        this.fuelLevel = buf.readInt();
        this.pos = buf.readBlockPos();
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeInt(fuelLevel);
        buf.writeBlockPos(pos);
    }

    public boolean handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> {
            if(Minecraft.getInstance().level.getBlockEntity(pos) instanceof FactoryIOInserterBlockEntity blockEntity) {
                if (!blockEntity.IS_ENERGY) {
                    blockEntity.overrideCurrentFuelValue(fuelLevel);
                }
            }
        });
        return true;
    }
}