package com.drimoz.factoryio.core.datagen.generator;

import com.drimoz.factoryio.FactoryIO;
import com.drimoz.factoryio.core.init.FactoryIOTags;
import com.drimoz.factoryio.core.registery.FactoryIOInserterRegistry;
import net.minecraft.core.Registry;
import net.minecraft.data.DataGenerator;
import net.minecraft.data.tags.TagsProvider;
import net.minecraft.world.item.Item;
import net.minecraftforge.common.data.ExistingFileHelper;
import net.minecraftforge.common.data.LanguageProvider;
import org.jetbrains.annotations.Nullable;


public class FactoryIOLangGenerator extends LanguageProvider {
    public FactoryIOLangGenerator(DataGenerator gen, String modid, String locale) {
        super(gen, modid, locale);
    }

    @Override
    protected void addTranslations() {
        add("itemGroup.modtab", "Factory'I/O");
        add("tooltip.factory_io.energy_name", "FE");
        add("tooltip.factory_io.gui_hold_shift", "Hold ");
        add("tooltip.factory_io.gui_shift_more_options", " for more options");
        add("tooltip.factory_io.hold", "Hold ");
        add("tooltip.factory_io.for_details", " for details.");
        add("tooltip.factory_io.speed", "Speed : ");
        add("tooltip.factory_io.consumption", "Consumption : ");
        add("tooltip.factory_io.fuel_consumption", "Fuel Consumption : ");
        add("tooltip.factory_io.capacity", "Capacity : ");
        add("tooltip.factory_io.grab", "Grab Items from ");
        add("tooltip.factory_io.whitelist", "Whitelist");
        add("tooltip.factory_io.blacklist", "Blacklist");

        FactoryIOInserterRegistry.getInstance().getInserters().forEach((inserter) -> {
            addBlock(inserter.getBlock(), inserter.getDisplayName().getString());
            //addItem(inserter.getItem(), inserter.getDisplayName().getString());
        });
    }
}
