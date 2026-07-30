package com.drimoz.factoryio.core.generic.item;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextColor;

import net.minecraft.world.item.ItemStack;

import javax.annotation.Nullable;

public class ColoredItem extends ModItem {

    // Private properties

    private final boolean isFoil;
    private final String color;

    public ColoredItem(Properties pProperties, boolean foiled, @Nullable String hexColorTag) {
        super(pProperties);

        this.isFoil = foiled;
        this.color = hexColorTag;
    }

    @Override
    public Component getName(ItemStack stack) {
        return Component.literal(super.getName(stack).getString()).withStyle(style -> style.withColor(TextColor.parseColor(color)));
    }

    @Override
    public boolean isFoil(ItemStack pStack) {
        return this.isFoil;
    }
}
