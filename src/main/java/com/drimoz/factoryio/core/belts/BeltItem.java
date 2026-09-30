package com.drimoz.factoryio.core.belts;

import com.drimoz.factoryio.core.model.Belt;
import com.drimoz.factoryio.shared.ModUtils;
import com.drimoz.factoryio.shared.StringHelper;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;

import javax.annotation.Nullable;
import java.util.List;

/** L'item d'un convoyeur : il affiche son débit, la grandeur qui dimensionne une usine. */
public class BeltItem extends BlockItem {

    private final Belt belt;

    public BeltItem(Block block, Belt belt, Properties properties) {
        super(block, properties);
        this.belt = belt;
    }

    /** Le nom du bloc : c'est lui qui compose celui d'une rampe, voir {@link BeltRampBlock#getName}. */
    @Override
    public Component getName(ItemStack stack) {
        return getBlock().getName();
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        Component rate = ModUtils.tooltipComponent("value_items_per_second", StringHelper.decimal(this.belt.getItemsPerSecond()))
                .withStyle(ChatFormatting.AQUA);

        tooltip.add(ModUtils.tooltipComponent("speed", rate).withStyle(ChatFormatting.GRAY));
    }
}
