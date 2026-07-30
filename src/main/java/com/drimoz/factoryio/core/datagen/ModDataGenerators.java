package com.drimoz.factoryio.core.datagen;

import com.drimoz.factoryio.FactoryIO;
import com.drimoz.factoryio.core.datagen.generator.*;
import com.drimoz.factoryio.core.registry.Translations;
import net.minecraft.data.DataGenerator;
import net.minecraft.data.PackOutput;
import net.minecraftforge.common.data.ExistingFileHelper;
import net.minecraftforge.data.event.GatherDataEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public class ModDataGenerators {

    @SubscribeEvent
    public void onGatherData(GatherDataEvent event) {
        DataGenerator generator = event.getGenerator();
        PackOutput output = generator.getPackOutput();
        ExistingFileHelper existingFileHelper = event.getExistingFileHelper();

        boolean client = event.includeClient();
        boolean server = event.includeServer();

        Translations.getINSTANCE().getTranslationList().forEach(code ->
                generator.addProvider(client, new ModLangGenerator(output, FactoryIO.MOD_ID, code)));

        generator.addProvider(client, new ModBlockModelGenerator(output, FactoryIO.MOD_ID, existingFileHelper));
        generator.addProvider(client, new ModItemModelGenerator(output, FactoryIO.MOD_ID, existingFileHelper));

        ModBlockTagsGenerator blockTags =
                new ModBlockTagsGenerator(output, event.getLookupProvider(), FactoryIO.MOD_ID, existingFileHelper);

        generator.addProvider(server, blockTags);
        generator.addProvider(server, new ModItemTagsGenerator(
                output, event.getLookupProvider(), blockTags.contentsGetter(), FactoryIO.MOD_ID, existingFileHelper));

        generator.addProvider(server, new ModLootGenerator(output));
    }
}
