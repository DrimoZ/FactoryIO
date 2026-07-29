package com.drimoz.factoryio.shared;

import com.drimoz.factoryio.FactoryIO;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

public final class FactoryIOUtils {

    private FactoryIOUtils() {}

    public static MutableComponent tooltipComponent(String name) {
        return Component.translatable("tooltip." + FactoryIO.MOD_ID + "." + name);
    }

    public static String tooltipString(String name) {
        return tooltipComponent(name).getString();
    }
}
