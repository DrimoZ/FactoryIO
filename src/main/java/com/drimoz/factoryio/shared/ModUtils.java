package com.drimoz.factoryio.shared;

import com.drimoz.factoryio.FactoryIO;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

public final class ModUtils {

    private ModUtils() {}

    /**
     * Clé à trous : les valeurs prennent la place des {@code %s} de la traduction.
     *
     * <p>C'est la seule façon de laisser chaque langue ordonner la phrase — « 3 blocs »,
     * « Portée : 3 » ou « 3 Blöcke » ne se recollent pas à partir de fragments.
     */
    public static MutableComponent tooltipComponent(String name, Object... args) {
        return Component.translatable(tooltipKey(name), args);
    }

    public static String tooltipKey(String name) {
        return "tooltip." + FactoryIO.MOD_ID + "." + name;
    }
}
