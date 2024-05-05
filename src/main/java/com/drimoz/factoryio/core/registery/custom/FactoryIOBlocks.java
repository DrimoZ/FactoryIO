package com.drimoz.factoryio.core.registery.custom;

import com.drimoz.factoryio.FactoryIO;
import com.drimoz.factoryio.features.inserters.inserter.BlockInserter;
import com.drimoz.factoryio.features.inserters.inserter.ItemInserter;
import com.drimoz.factoryio.shared.ModCreativeModeTab;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import java.util.function.Supplier;

public class FactoryIOBlocks {

    // Public properties

    public static final BlockBehaviour.StatePredicate redstone = (pState, pLevel, pPos) -> false;

    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, FactoryIO.MOD_ID);

    // Inserters

    public static final RegistryObject<Block> INSERTER_BLOCK = registerInserter("inserter",
            () -> new BlockInserter(BlockBehaviour.Properties.copy(Blocks.IRON_BLOCK).noOcclusion().isRedstoneConductor(redstone)));



    // Interface

    public static void registerBlocks(IEventBus eventBus) {
        BLOCKS.register(eventBus);
    }

    private static <T extends Block> RegistryObject<T> registerInserter(String name, Supplier<T> block) {
        RegistryObject<T> toReturn = BLOCKS.register(name, block);
        FactoryIOItems.ITEMS.register(name, () -> new ItemInserter(toReturn.get(), new Item.Properties().tab(ModCreativeModeTab.MOD_TAB)));

        return toReturn;
    }
}
