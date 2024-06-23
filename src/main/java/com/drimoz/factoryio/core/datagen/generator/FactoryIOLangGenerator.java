package com.drimoz.factoryio.core.datagen.generator;

import com.drimoz.factoryio.FactoryIO;
import com.drimoz.factoryio.core.init.FactoryIOItems;
import com.drimoz.factoryio.core.model.TranslationCode;
import com.drimoz.factoryio.core.registery.FactoryIOInserterRegistry;
import net.minecraft.data.DataGenerator;
import net.minecraftforge.common.data.LanguageProvider;


public class FactoryIOLangGenerator extends LanguageProvider {

    // Private properties

    private final TranslationCode translationCode;

    public FactoryIOLangGenerator(DataGenerator gen, String modid, TranslationCode code) {
        super(gen, modid, code.getFullCode());

        this.translationCode = code;
    }

    @Override
    protected void addTranslations() {
        FactoryIOInserterRegistry.getInstance().getInserters().forEach((inserter) -> {
            String translation = inserter.getTranslation().getTranslation(translationCode);

            addBlock(
                    inserter.getBlock(),
                    translation == null ? formatDisplayName(inserter.getName()) : translation
            );
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
