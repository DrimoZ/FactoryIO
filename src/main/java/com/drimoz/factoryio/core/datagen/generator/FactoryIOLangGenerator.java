package com.drimoz.factoryio.core.datagen.generator;

import com.drimoz.factoryio.FactoryIO;
import com.drimoz.factoryio.core.init.FactoryIOItems;
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
        FactoryIOInserterRegistry.getInstance().getInserters().forEach((inserter) -> {
            addBlock(inserter.getBlock(), inserter.getDisplayName().getString());
        });

        FactoryIOItems.ENTRIES.keySet().forEach((itemRegistryObject) -> {
            String itemName = itemRegistryObject.getId().getPath();
            String translationKey = "item." + FactoryIO.MOD_ID + "." + itemName;
            add(translationKey, formatDisplayName(itemName));
        });
    }

    private String formatDisplayName(String name) {
        String[] parts = name.split("_");
        StringBuilder displayName = new StringBuilder();
        for (String part : parts) {
            displayName.append(Character.toUpperCase(part.charAt(0))).append(part.substring(1)).append(" ");
        }
        return displayName.toString().trim();
    }
}
