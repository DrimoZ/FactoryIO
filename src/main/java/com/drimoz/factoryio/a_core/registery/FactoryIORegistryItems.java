package com.drimoz.factoryio.a_core.registery;

import com.drimoz.factoryio.FactoryIO;
import com.drimoz.factoryio.a_core.inserters.FactoryIOInserterItem;
import com.drimoz.factoryio.a_core.models.InserterData;
import com.drimoz.factoryio.shared.FactoryIOCreativeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;

public class FactoryIORegistryItems {

    // Public properties

    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, FactoryIO.MOD_ID);

    // Interface ( Generic )

    public static void init(IEventBus eventBus) {
        ITEMS.register(eventBus);
    }

    /** +++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++ **/
    /**                                                          INSERTERS                                                          **/
    /** +++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++ **/

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
