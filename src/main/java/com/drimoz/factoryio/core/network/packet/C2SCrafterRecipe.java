package com.drimoz.factoryio.core.network.packet;

import com.drimoz.factoryio.content.crafter.CrafterBlockEntity;
import com.drimoz.factoryio.content.crafter.CrafterMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import javax.annotation.Nullable;
import java.util.function.Supplier;

/**
 * Choix de recette d'un crafter, du client vers le serveur.
 *
 * <p>L'identifiant voyage, jamais un rang dans la liste : l'ordre n'est pas garanti entre
 * client et serveur. Recette inconnue ou au-dessus du palier : {@code selectRecipe} refuse,
 * après les six validations de {@link C2SChecks}.
 */
public class C2SCrafterRecipe {

    private final BlockPos pos;
    @Nullable private final ResourceLocation recipe;

    public C2SCrafterRecipe(BlockPos pos, @Nullable ResourceLocation recipe) {
        this.pos = pos;
        this.recipe = recipe;
    }

    public C2SCrafterRecipe(FriendlyByteBuf buf) {
        this(buf.readBlockPos(), buf.readBoolean() ? buf.readResourceLocation() : null);
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeBlockPos(this.pos);
        buf.writeBoolean(this.recipe != null);
        if (this.recipe != null) buf.writeResourceLocation(this.recipe);
    }

    public void handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();

        context.enqueueWork(() -> apply(context.getSender(), this.pos, this.recipe));

        context.setPacketHandled(true);
    }

    /** Le traitement, hors du contexte réseau : c'est ce que les GameTests éprouvent. */
    public static boolean apply(@Nullable ServerPlayer player, BlockPos pos, @Nullable ResourceLocation recipe) {
        CrafterBlockEntity crafter = C2SChecks.openedBlockEntity(player, pos,
                CrafterMenu.class, CrafterMenu::getBlockEntity, CrafterBlockEntity.class);
        if (crafter == null || !crafter.selectRecipe(recipe, player)) return false;

        ((CrafterMenu) player.containerMenu).setRecipeId(recipe);
        return true;
    }
}
