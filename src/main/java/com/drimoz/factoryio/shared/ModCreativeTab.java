package com.drimoz.factoryio.shared;

import com.drimoz.factoryio.FactoryIO;
import com.drimoz.factoryio.core.init.ModItems;
import com.drimoz.factoryio.core.init.ModRegistries;
import com.drimoz.factoryio.core.registry.InserterRegistry;
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
public class ModCreativeTab {

    public static final RegistryObject<CreativeModeTab> MOD_TAB = ModRegistries.CREATIVE_TABS.register(
            FactoryIO.MOD_ID,
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup." + FactoryIO.MOD_ID))
                    .icon(() -> new ItemStack(ModItems.ELECTRONIC_CIRCUIT.get()))
                    .displayItems((parameters, output) -> {
                        InserterRegistry.getInstance().getInserters()
                                .forEach(inserter -> output.accept(inserter.getItem().get()));

                        ModItems.ENTRIES.forEach(item -> output.accept(item.get()));
                    })
                    .build());

    private ModCreativeTab() {}
}
