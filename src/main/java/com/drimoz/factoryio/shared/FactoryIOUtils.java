package com.drimoz.factoryio.shared;

import net.minecraft.nbt.CompoundTag;
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

    public static List<Path> getModFilePath(String modid) {
        throw new AssertionError();
    }

    public static Optional<CompoundTag> getTag(ItemStack stack) {
        return Optional.ofNullable(stack.getTag()).filter(Predicate.not(CompoundTag::isEmpty));
    }

}
