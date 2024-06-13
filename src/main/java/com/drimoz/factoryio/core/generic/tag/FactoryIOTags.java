package com.drimoz.factoryio.core.generic.tag;

import com.drimoz.factoryio.FactoryIO;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;

public class FactoryIOTags {
    public static class Items {

        // Public properties

        public static final TagKey<Item> INSERTER_FUEL = tag("inserter_fuel");
        public static final TagKey<Item> WRENCH_ITEM = forgeTag("tools/wrench");

        // Inner work

        private static TagKey<Item> tag(String name) {
            return ItemTags.create(new ResourceLocation(FactoryIO.MOD_ID, name));
        }

        private static TagKey<Item> forgeTag(String name) {
            return ItemTags.create(new ResourceLocation("forge", name));
        }
    }

    public static class Blocks {

        // Inner work

        private static TagKey<Block> tag(String name) {
            return BlockTags.create(new ResourceLocation(FactoryIO.MOD_ID, name));
        }

        private static TagKey<Block> forgeTag(String name) {
            return BlockTags.create(new ResourceLocation("forge", name));
        }
    }
}
