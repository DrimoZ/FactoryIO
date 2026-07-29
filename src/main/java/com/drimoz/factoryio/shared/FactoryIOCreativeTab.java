package com.drimoz.factoryio.shared;

import com.drimoz.factoryio.FactoryIO;
import com.drimoz.factoryio.core.init.FactoryIOItems;
import com.drimoz.factoryio.core.init.FactoryIORegistries;
import com.drimoz.factoryio.core.registery.FactoryIOInserterRegistry;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.RegistryObject;

/**
 * Onglet créatif du mod.
 *
 * <p>En 1.20.1 les onglets sont des objets de registre : {@code Item.Properties#tab()}
 * n'existe plus, le contenu se déclare via {@code displayItems}.
 */
public class FactoryIOCreativeTab {

    public static final RegistryObject<CreativeModeTab> MOD_TAB = FactoryIORegistries.CREATIVE_TABS.register(
            FactoryIO.MOD_ID,
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup." + FactoryIO.MOD_ID))
                    .icon(() -> new ItemStack(FactoryIOItems.ELECTRONIC_CIRCUIT.get()))
                    .displayItems((parameters, output) -> {
                        FactoryIOInserterRegistry.getInstance().getInserters()
                                .forEach(inserter -> output.accept(inserter.getItem().get()));

                        FactoryIOItems.ENTRIES.forEach(item -> output.accept(item.get()));
                    })
                    .build());

    private FactoryIOCreativeTab() {}
}
