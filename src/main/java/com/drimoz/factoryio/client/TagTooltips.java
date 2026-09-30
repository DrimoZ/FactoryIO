package com.drimoz.factoryio.client;

import com.drimoz.factoryio.FactoryIO;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;

/**
 * Tags d'un item dans l'infobulle avancée (F3+H).
 *
 * <p>Tout le contenu du mod se branche par tags — paliers de modules, carburant, circuits. Les
 * voir sans ouvrir un datapack aide à écrire une recette ou à ajouter un item à un palier. Sont
 * listés : tous les tags des items du mod, et les tags {@code factor_io:} de n'importe quel
 * item, pour qu'un ajout fait par un datapack se vérifie en jeu.
 */
@Mod.EventBusSubscriber(modid = FactoryIO.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class TagTooltips {

    private TagTooltips() {}

    @SubscribeEvent
    public static void onTooltip(ItemTooltipEvent event) {
        if (!event.getFlags().isAdvanced()) return;

        ItemStack stack = event.getItemStack();
        ResourceLocation id = ForgeRegistries.ITEMS.getKey(stack.getItem());
        boolean ours = id != null && FactoryIO.MOD_ID.equals(id.getNamespace());

        stack.getTags()
                .map(tag -> tag.location())
                .filter(tag -> ours || FactoryIO.MOD_ID.equals(tag.getNamespace()))
                .sorted()
                .forEach(tag -> event.getToolTip().add(
                        Component.literal("#" + tag).withStyle(ChatFormatting.DARK_GRAY)));
    }
}
