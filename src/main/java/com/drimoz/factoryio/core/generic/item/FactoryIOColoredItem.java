package com.drimoz.factoryio.core.generic.item;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextColor;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nullable;

public class FactoryIOColoredItem extends FactoryIOItem {

    // Private properties

    private final boolean isFoil;
    private final String color;

    public FactoryIOColoredItem(Properties pProperties, boolean foiled, @Nullable String hexColorTag) {
        super(pProperties);

        this.isFoil = foiled;
        this.color = hexColorTag;
    }

    @Override
    public Component getName(ItemStack stack) {
        return new TextComponent(super.getName(stack).getString()).withStyle(style -> style.withColor(TextColor.parseColor(color)));
    }

    @Override
    public boolean isFoil(ItemStack pStack) {
        return this.isFoil;
    }
}
