package com.drimoz.factoryio.core.network.packet;

import com.drimoz.factoryio.core.blockentities.inserters.FactoryIOInserterBlockEntity;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class FactoryIOSyncC2SWhitelistButton {

	private int x;
	private int y;
	private int z;
	private int index;
	private int set;

	public FactoryIOSyncC2SWhitelistButton(ByteBuf buf) {
		x = buf.readInt();
		y = buf.readInt();
		z = buf.readInt();
		index = buf.readInt();
		set = buf.readInt();
	}

	public void toBytes(ByteBuf buf) {
		buf.writeInt(x);
		buf.writeInt(y);
		buf.writeInt(z);
		buf.writeInt(index);
		buf.writeInt(set);
	}

	public FactoryIOSyncC2SWhitelistButton(BlockPos pos, int index, int set) {
		this.x = pos.getX();
		this.y = pos.getY();
		this.z = pos.getZ();
		this.index = index;
		this.set = set;
	}

	public void handle(Supplier<NetworkEvent.Context> ctx) {
		ctx.get().enqueueWork(() -> {
			ServerPlayer player = ctx.get().getSender();
			BlockPos pos = new BlockPos(x, y, z);
			FactoryIOInserterBlockEntity te = (FactoryIOInserterBlockEntity) player.getLevel().getBlockEntity(pos);
			if (player.level.isLoaded(pos)) {
				if (index == 6 && te.IS_FILTER) {
					if (te.isWhitelist() != (set == 1 ? true: false)) {
						te.setWhitelist(set == 1 ? true: false);
						te.getLevel().markAndNotifyBlock(pos, player.getLevel().getChunkAt(pos), te.getLevel().getBlockState(pos).getBlock().defaultBlockState(), te.getLevel().getBlockState(pos), 2, 0);
						te.setChanged();
					}
				}
			}
		});
		ctx.get().setPacketHandled(true);
	}
}