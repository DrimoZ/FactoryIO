package com.drimoz.factoryio.core.registery.custom;

import com.drimoz.factoryio.FactoryIO;
import com.drimoz.factoryio.core.items.inserters.FactoryIOInserterItem;
import com.drimoz.factoryio.core.registery.models.InserterData;
import com.drimoz.factoryio.shared.FactoryIOCreativeTab;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;

public class FactoryIOItems {
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, FactoryIO.MOD_ID);

    public static void registerItems(IEventBus eventBus) {
        ITEMS.register(eventBus);
    }

    public static void registerInserterItemFromData(InserterData inserterData) {
        ITEMS.register(
                inserterData.identifier,
                () -> FactoryIOInserterItem.create(
                        (Block) inserterData.registries().getBlock().get(),
                        new Item.Properties().tab(FactoryIOCreativeTab.MOD_TAB),
                        inserterData
                )
        );
    }
}
