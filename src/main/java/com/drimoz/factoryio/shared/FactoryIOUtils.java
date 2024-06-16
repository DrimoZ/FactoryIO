package com.drimoz.factoryio.shared;

import com.drimoz.factoryio.FactoryIO;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.world.item.ItemStack;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

public class FactoryIOUtils {
    public static Path getConfigDirectory() {
        return Paths.get("config");
    }

    public static TranslatableComponent tooltipComponent(String name) {
        return new TranslatableComponent("tooltip." + FactoryIO.MOD_ID + "." + name);
    }

    public static String tooltipString(String name) {
        return tooltipComponent(name).getString();
    }

}
