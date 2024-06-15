package com.drimoz.factoryio.core.init;

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

        public static final TagKey<Item> INSERTERS = tag("inserters");
        public static final TagKey<Item> INSERTER_FUEL = tag("inserter_fuel");
        public static final TagKey<Item> WRENCH_ITEM = forgeTag("tools/wrench");



        // Inner work

        private static TagKey<Item> tag(String name) {
            return ItemTags.create(new ResourceLocation(FactoryIO.MOD_ID, name));
        }

        private static TagKey<Item> forgeTag(String name) {
            return ItemTags.create(new ResourceLocation("forge", name));
        }

        private static TagKey<Item> minecraftTag(String name) {
            return ItemTags.create(new ResourceLocation("minecraft", name));
        }
    }

    public static class Blocks {

        // Public properties

        public static final TagKey<Block> MINEABLE_PICKAXE = minecraftTag("mineable/pickaxe");

        public static final TagKey<Block> TOOL_WOODEN = minecraftTag("needs_wooden_tool");
        public static final TagKey<Block> TOOL_STONE = minecraftTag("needs_stone_tool");
        public static final TagKey<Block> TOOL_IRON = minecraftTag("needs_iron_tool");
        public static final TagKey<Block> TOOL_GOLD = minecraftTag("needs_gold_tool");
        public static final TagKey<Block> TOOL_DIAMOND = minecraftTag("needs_diamond_tool");
        public static final TagKey<Block> TOOL_NETHERITE = minecraftTag("needs_netherite_tool");

        // Inner work

        private static TagKey<Block> tag(String name) {
            return BlockTags.create(new ResourceLocation(FactoryIO.MOD_ID, name));
        }

        private static TagKey<Block> forgeTag(String name) {
            return BlockTags.create(new ResourceLocation("forge", name));
        }

        private static TagKey<Block> minecraftTag(String name) {
            return BlockTags.create(new ResourceLocation("minecraft", name));
        }
    }
}
