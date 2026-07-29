package com.drimoz.factoryio.core.datagen;

import com.drimoz.factoryio.FactoryIO;
import com.drimoz.factoryio.core.datagen.generator.*;
import com.drimoz.factoryio.core.registery.FactoryIOTranslations;
import net.minecraft.data.DataGenerator;
import net.minecraft.data.PackOutput;
import net.minecraftforge.common.data.ExistingFileHelper;
import net.minecraftforge.data.event.GatherDataEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public class FactoryIODataGenerators {

    @SubscribeEvent
    public void onGatherData(GatherDataEvent event) {
        DataGenerator generator = event.getGenerator();
        PackOutput output = generator.getPackOutput();
        ExistingFileHelper existingFileHelper = event.getExistingFileHelper();

        boolean client = event.includeClient();
        boolean server = event.includeServer();

        FactoryIOTranslations.getINSTANCE().getTranslationList().forEach(code ->
                generator.addProvider(client, new FactoryIOLangGenerator(output, FactoryIO.MOD_ID, code)));

        generator.addProvider(client, new FactoryIOBlockModelGenerator(output, FactoryIO.MOD_ID, existingFileHelper));
        generator.addProvider(client, new FactoryIOItemModelGenerator(output, FactoryIO.MOD_ID, existingFileHelper));

        FactoryIOBlockTagsGenerator blockTags =
                new FactoryIOBlockTagsGenerator(output, event.getLookupProvider(), FactoryIO.MOD_ID, existingFileHelper);

        generator.addProvider(server, blockTags);
        generator.addProvider(server, new FactoryIOItemTagsGenerator(
                output, event.getLookupProvider(), blockTags.contentsGetter(), FactoryIO.MOD_ID, existingFileHelper));

        generator.addProvider(server, new FactoryIOLootGenerator(output));
    }
}
