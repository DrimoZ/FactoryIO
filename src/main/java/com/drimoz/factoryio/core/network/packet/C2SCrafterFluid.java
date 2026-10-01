package com.drimoz.factoryio.core.network.packet;

import com.drimoz.factoryio.content.crafter.CrafterBlockEntity;
import com.drimoz.factoryio.content.crafter.CrafterMenu;
import com.drimoz.factoryio.content.crafter.CrafterRecipe;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.fluids.FluidActionResult;
import net.minecraftforge.fluids.FluidUtil;
import net.minecraftforge.fluids.capability.templates.FluidTank;
import net.minecraftforge.network.NetworkEvent;

import javax.annotation.Nullable;
import java.util.function.Supplier;

/**
 * Clic sur un réservoir du crafter avec un récipient au curseur (FIO-179) : un seau plein vide
 * son contenu dans un réservoir d'entrée, un seau vide se remplit à n'importe quel réservoir.
 * Sans tuyau dans le mod, c'est ce qui rend les fluides jouables — et testables — seuls.
 *
 * <p>Le récipient est celui que porte le curseur, côté serveur : le client ne dit que quel
 * réservoir il a cliqué. Six validations par {@link C2SChecks}.
 */
public class C2SCrafterFluid {

    private final BlockPos pos;
    private final int tank;

    public C2SCrafterFluid(BlockPos pos, int tank) {
        this.pos = pos;
        this.tank = tank;
    }

    public C2SCrafterFluid(FriendlyByteBuf buf) {
        this(buf.readBlockPos(), buf.readVarInt());
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeBlockPos(this.pos);
        buf.writeVarInt(this.tank);
    }

    public void handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> apply(context.getSender(), this.pos, this.tank));
        context.setPacketHandled(true);
    }

    /** Le traitement, hors du contexte réseau : c'est ce que les GameTests éprouvent. */
    public static boolean apply(@Nullable ServerPlayer player, BlockPos pos, int tankIndex) {
        if (tankIndex < 0 || tankIndex >= 2 * CrafterRecipe.MAX_FLUIDS) return false;

        CrafterBlockEntity crafter = C2SChecks.openedBlockEntity(player, pos,
                CrafterMenu.class, CrafterMenu::getBlockEntity, CrafterBlockEntity.class);
        if (crafter == null) return false;

        ItemStack carried = player.containerMenu.getCarried();
        if (carried.isEmpty()) return false;

        FluidTank tank = crafter.getTank(tankIndex);
        ItemStack one = carried.copyWithCount(1);

        // Remplir le récipient d'abord ; s'il est plein, le vider — dans une entrée seulement.
        FluidActionResult result = FluidUtil.tryFillContainer(one, tank, Integer.MAX_VALUE, player, true);
        if (!result.isSuccess() && tankIndex < CrafterRecipe.MAX_FLUIDS) {
            result = FluidUtil.tryEmptyContainer(one, tank, Integer.MAX_VALUE, player, true);
        }
        if (!result.isSuccess()) return false;

        ItemStack after = result.getResult();
        if (carried.getCount() == 1) {
            player.containerMenu.setCarried(after);
        } else {
            carried.shrink(1);
            if (!player.getInventory().add(after)) player.drop(after, false);
        }
        return true;
    }
}
