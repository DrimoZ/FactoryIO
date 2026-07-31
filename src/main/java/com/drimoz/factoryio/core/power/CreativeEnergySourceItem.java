package com.drimoz.factoryio.core.power;

import com.drimoz.factoryio.shared.ModUtils;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;

import javax.annotation.Nullable;
import java.util.List;

/**
 * Item de la source d'énergie inépuisable.
 *
 * <p>N'existe que pour porter une infobulle. Un bloc qui alimente tout ce qui le touche,
 * sans limite et sans recette, doit le dire de lui-même : sans cela, quelqu'un le
 * découvrirait dans l'onglet créatif et le prendrait pour un générateur.
 */
public class CreativeEnergySourceItem extends BlockItem {

    public CreativeEnergySourceItem(Block block, Properties properties) {
        super(block, properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, level, tooltip, flag);

        tooltip.add(ModUtils.tooltipComponent("creative_energy_source").withStyle(ChatFormatting.GRAY));
    }
}
