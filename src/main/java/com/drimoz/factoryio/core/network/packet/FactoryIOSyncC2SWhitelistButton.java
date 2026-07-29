package com.drimoz.factoryio.core.network.packet;

import com.drimoz.factoryio.core.inserters.FactoryIOInserterBlockEntity;
import com.drimoz.factoryio.core.inserters.FactoryIOInserterContainer;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class FactoryIOSyncC2SWhitelistButton {

	/** Identifiant du bouton « whitelist / blacklist » dans l'écran de l'inserter. */
	public static final int WHITELIST_BUTTON = 6;

	/** Portée d'interaction maximale, au carré. Aligné sur {@code stillValid}. */
	private static final double MAX_DISTANCE_SQR = 64.0D;

	private final BlockPos pos;
	private final int index;
	private final int set;

	// Life cycle

	public FactoryIOSyncC2SWhitelistButton(BlockPos pos, int index, int set) {
		this.pos = pos;
		this.index = index;
		this.set = set;
	}

	public FactoryIOSyncC2SWhitelistButton(FriendlyByteBuf buf) {
		this.pos = buf.readBlockPos();
		this.index = buf.readInt();
		this.set = buf.readInt();
	}

	public void toBytes(FriendlyByteBuf buf) {
		buf.writeBlockPos(pos);
		buf.writeInt(index);
		buf.writeInt(set);
	}

	// Interface

	/**
	 * Tout ce qui arrive ici vient d'un client et doit être traité comme hostile :
	 * position arbitraire, bloc inexistant, joueur à l'autre bout du monde. Chaque
	 * hypothèse est donc vérifiée avant la moindre mutation (cf. BUG-007).
	 */
	public void handle(Supplier<NetworkEvent.Context> supplier) {
		NetworkEvent.Context context = supplier.get();

		context.enqueueWork(() -> {
			ServerPlayer player = context.getSender();
			if (player == null) return;

			if (index != WHITELIST_BUTTON) return;

			// Ne jamais provoquer le chargement d'un chunk depuis un paquet client.
			if (!player.level().isLoaded(pos)) return;

			if (player.distanceToSqr(Vec3.atCenterOf(pos)) > MAX_DISTANCE_SQR) return;

			// Le joueur doit avoir CE bloc ouvert, sans quoi n'importe qui pourrait
			// reconfigurer n'importe quel inserter du monde.
			if (!(player.containerMenu instanceof FactoryIOInserterContainer menu)) return;
			if (menu.getBlockEntity() == null) return;
			if (!pos.equals(menu.getBlockEntity().getBlockPos())) return;

			if (!(player.level().getBlockEntity(pos) instanceof FactoryIOInserterBlockEntity blockEntity)) return;
			if (!blockEntity.IS_FILTER) return;

			boolean whitelist = set == 1;
			if (blockEntity.isWhitelist() == whitelist) return;

			blockEntity.setWhitelist(whitelist);
			blockEntity.setChanged();

			player.level().sendBlockUpdated(
					pos, blockEntity.getBlockState(), blockEntity.getBlockState(), Block.UPDATE_CLIENTS);
		});

		context.setPacketHandled(true);
	}
}
