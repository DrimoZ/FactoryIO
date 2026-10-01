package com.drimoz.factoryio.core.network.packet;

import com.drimoz.factoryio.core.inserters.InserterAnimationMode;
import com.drimoz.factoryio.core.inserters.InserterBlockEntity;
import com.drimoz.factoryio.core.inserters.InserterContainer;
import com.drimoz.factoryio.core.inserters.InserterDropLane;
import com.drimoz.factoryio.core.inserters.InserterRedstoneCondition;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
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
 * <p>La validation est celle de BUG-007, partagée par tous les paquets C→S : voir
 * {@link C2SChecks}.
 */
public class C2SInserterSetting {

	public enum Setting {
		/** Bascule liste blanche / liste noire. Valeur : 1 pour blanche, 0 pour noire. */
		FILTER_MODE,

		/** Mode de la condition redstone. Valeur : l'ordinal du mode. */
		REDSTONE_MODE,

		/** Seuil de la condition redstone. Valeur : 0 à 15. */
		REDSTONE_THRESHOLD,

		/** Mode d'animation. Valeur : l'ordinal du mode. */
		ANIMATION,

		/** Interrupteur manuel (FIO-167). Valeur : 1 allumé, 0 éteint. */
		POWER,

		/** Plafond d'items par prise (FIO-168). Valeur : 0 pour « le maximum », sinon 1 à 64. */
		HAND_SIZE,

		/** Voie de dépose sur un convoyeur (FIO-169). Valeur : l'ordinal de la voie. */
		DROP_LANE;

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
			Setting target = Setting.byOrdinal(setting);
			if (target == null) return;

			InserterBlockEntity blockEntity = C2SChecks.openedBlockEntity(context.getSender(), pos,
					InserterContainer.class, InserterContainer::getBlockEntity, InserterBlockEntity.class);
			if (blockEntity == null) return;

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
					blockEntity.getConfiguredRedstoneCondition()
							.withMode(InserterRedstoneCondition.Mode.byOrdinal(value)));

			// Le constructeur de la condition borne le seuil : une valeur forgée hors de
			// [0, 15] est ramenée dans le domaine plutôt que rejetée.
			case REDSTONE_THRESHOLD -> blockEntity.setRedstoneCondition(
					blockEntity.getConfiguredRedstoneCondition().withThreshold(value));

			// Purement visuel : aucun effet sur le débit, les coûts ou les transferts.
			case ANIMATION -> blockEntity.setAnimationMode(InserterAnimationMode.byOrdinal(value));

			case POWER -> blockEntity.setSwitchedOn(value == 1);

			// Borné par le block entity, comme le seuil redstone.
			case HAND_SIZE -> blockEntity.setHandSizeLimit(value);

			case DROP_LANE -> blockEntity.setDropLane(InserterDropLane.byOrdinal(value));
		}
	}
}
