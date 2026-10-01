package com.drimoz.factoryio.core.network.packet;

import com.drimoz.factoryio.content.crafter.CrafterBlockEntity;
import com.drimoz.factoryio.content.crafter.CrafterMenu;
import com.drimoz.factoryio.core.generic.block.RedstoneCondition;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import javax.annotation.Nullable;
import java.util.function.Supplier;

/**
 * Réglage de contrôle d'un crafter, du client vers le serveur (FIO-183) : interrupteur et
 * condition redstone, comme {@link C2SInserterSetting} pour les inserters. Les six validations
 * passent par {@link C2SChecks}.
 */
public class C2SCrafterSetting {

    public enum Setting {
        /** Valeur : 1 allumé, 0 éteint. */
        POWER,
        /** Valeur : l'ordinal du mode. */
        REDSTONE_MODE,
        /** Valeur : 0 à 15, bornée par la condition elle-même. */
        REDSTONE_THRESHOLD;

        private static final Setting[] VALUES = values();

        @Nullable
        static Setting byOrdinal(int ordinal) {
            return ordinal < 0 || ordinal >= VALUES.length ? null : VALUES[ordinal];
        }
    }

    private final BlockPos pos;
    private final int setting;
    private final int value;

    public C2SCrafterSetting(BlockPos pos, Setting setting, int value) {
        this(pos, setting.ordinal(), value);
    }

    private C2SCrafterSetting(BlockPos pos, int setting, int value) {
        this.pos = pos;
        this.setting = setting;
        this.value = value;
    }

    public C2SCrafterSetting(FriendlyByteBuf buf) {
        this(buf.readBlockPos(), buf.readVarInt(), buf.readVarInt());
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeBlockPos(this.pos);
        buf.writeVarInt(this.setting);
        buf.writeVarInt(this.value);
    }

    public void handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> apply(context.getSender(), this.pos, Setting.byOrdinal(this.setting), this.value));
        context.setPacketHandled(true);
    }

    /** Le traitement, hors du contexte réseau : c'est ce que les GameTests éprouvent. */
    public static boolean apply(@Nullable ServerPlayer player, BlockPos pos, @Nullable Setting setting, int value) {
        if (setting == null) return false;

        CrafterBlockEntity crafter = C2SChecks.openedBlockEntity(player, pos,
                CrafterMenu.class, CrafterMenu::getBlockEntity, CrafterBlockEntity.class);
        if (crafter == null) return false;

        RedstoneCondition condition = crafter.getRedstoneCondition();
        switch (setting) {
            case POWER -> crafter.setSwitchedOn(value == 1);
            case REDSTONE_MODE -> crafter.setRedstoneCondition(condition.withMode(RedstoneCondition.Mode.byOrdinal(value)));
            case REDSTONE_THRESHOLD -> crafter.setRedstoneCondition(condition.withThreshold(value));
        }
        return true;
    }
}
