package com.drimoz.factoryio.core.network.packet;

import com.drimoz.factoryio.core.inserters.InserterAnimationMode;
import com.drimoz.factoryio.core.inserters.InserterBlockEntity;
import com.drimoz.factoryio.core.inserters.InserterContainer;
import com.drimoz.factoryio.core.inserters.InserterRedstoneCondition;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * Réglage d'inserter, du client vers le serveur.
 *
 * <p>Le {@code C2SInserterSettings} que prévoyait
 * <a href="../../../../../../../../docs/04-DETTE-TECHNIQUE.md">DT-01</a>. Il remplace
 * {@code FactoryIOSyncC2SWhitelistButton}, dont le nom ne décrivait plus rien depuis qu'il
 * y a trois réglages à porter : le mode de filtrage, et les deux moitiés de la condition
 * redstone (FIO-070).
 *
 * <p>La validation est celle qui avait été mise en place pour BUG-007, et elle vaut d'être
 * rappelée dans son intégralité : <b>tout ce qui arrive ici vient d'un client et doit être
 * traité comme hostile</b>. Position arbitraire, bloc inexistant, joueur à l'autre bout du
 * monde, menu jamais ouvert — chaque hypothèse est vérifiée avant la moindre mutation.
 */
public class C2SInserterSetting {

	/** Portée d'interaction maximale, au carré. Aligné sur {@code stillValid}. */
	private static final double MAX_DISTANCE_SQR = 64.0D;

	public enum Setting {
		/** Bascule liste blanche / liste noire. Valeur : 1 pour blanche, 0 pour noire. */
		FILTER_MODE,

		/** Mode de la condition redstone. Valeur : l'ordinal du mode. */
		REDSTONE_MODE,

		/** Seuil de la condition redstone. Valeur : 0 à 15. */
		REDSTONE_THRESHOLD,

		/** Mode d'animation. Valeur : l'ordinal du mode. */
		ANIMATION;

		private static final Setting[] VALUES = values();

		static Setting byOrdinal(int ordinal) {
			return ordinal < 0 || ordinal >= VALUES.length ? null : VALUES[ordinal];
		}
	}

	private final BlockPos pos;
	private final int setting;
	private final int value;

	// Life cycle

	public C2SInserterSetting(BlockPos pos, Setting setting, int value) {
		this(pos, setting.ordinal(), value);
	}

	private C2SInserterSetting(BlockPos pos, int setting, int value) {
		this.pos = pos;
		this.setting = setting;
		this.value = value;
	}

	public C2SInserterSetting(FriendlyByteBuf buf) {
		this(buf.readBlockPos(), buf.readVarInt(), buf.readVarInt());
	}

	public void toBytes(FriendlyByteBuf buf) {
		buf.writeBlockPos(pos);
		buf.writeVarInt(setting);
		buf.writeVarInt(value);
	}

	// Interface

	public void handle(Supplier<NetworkEvent.Context> supplier) {
		NetworkEvent.Context context = supplier.get();

		context.enqueueWork(() -> {
			ServerPlayer player = context.getSender();
			if (player == null) return;

			Setting target = Setting.byOrdinal(setting);
			if (target == null) return;

			// Ne jamais provoquer le chargement d'un chunk depuis un paquet client.
			if (!player.level().isLoaded(pos)) return;

			if (player.distanceToSqr(Vec3.atCenterOf(pos)) > MAX_DISTANCE_SQR) return;

			// Le joueur doit avoir CE bloc ouvert, sans quoi n'importe qui pourrait
			// reconfigurer n'importe quel inserter du monde.
			if (!(player.containerMenu instanceof InserterContainer menu)) return;
			if (menu.getBlockEntity() == null) return;
			if (!pos.equals(menu.getBlockEntity().getBlockPos())) return;

			if (!(player.level().getBlockEntity(pos) instanceof InserterBlockEntity blockEntity)) return;

			apply(blockEntity, target);
		});

		context.setPacketHandled(true);
	}

	// Inner work

	private void apply(InserterBlockEntity blockEntity, Setting target) {
		switch (target) {
			case FILTER_MODE -> {
				// Un inserter non filtrant n'a pas de mode de filtrage à changer.
				if (!blockEntity.IS_FILTER) return;

				blockEntity.setWhitelist(value == 1);
			}
			case REDSTONE_MODE -> blockEntity.setRedstoneCondition(
					blockEntity.getRedstoneCondition()
							.withMode(InserterRedstoneCondition.Mode.byOrdinal(value)));

			// Le constructeur de la condition borne le seuil : une valeur forgée hors de
			// [0, 15] est ramenée dans le domaine plutôt que rejetée.
			case REDSTONE_THRESHOLD -> blockEntity.setRedstoneCondition(
					blockEntity.getRedstoneCondition().withThreshold(value));

			// Purement visuel : aucun effet sur le débit, les coûts ou les transferts.
			case ANIMATION -> blockEntity.setAnimationMode(InserterAnimationMode.byOrdinal(value));
		}
	}
}
