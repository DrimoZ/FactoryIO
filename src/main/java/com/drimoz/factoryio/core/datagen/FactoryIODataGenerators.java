package com.drimoz.factoryio.core.datagen;

import com.drimoz.factoryio.FactoryIO;
import com.drimoz.factoryio.core.datagen.generator.FactoryIOBlockModelGenerator;
import com.drimoz.factoryio.core.datagen.generator.FactoryIOItemModelGenerator;
import com.drimoz.factoryio.core.datagen.generator.FactoryIOItemTagsGenerator;
import net.minecraft.data.DataGenerator;
import net.minecraftforge.common.data.ExistingFileHelper;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.forge.event.lifecycle.GatherDataEvent;

public class FactoryIODataGenerators {
    public FactoryIODataGenerators() {
    }

    @SubscribeEvent
    public void onGatherData(GatherDataEvent event) {
        DataGenerator generator = event.getGenerator();
        ExistingFileHelper existingFileHelper = event.getExistingFileHelper();

        generator.addProvider(new FactoryIOBlockModelGenerator(generator, FactoryIO.MOD_ID, existingFileHelper));
        generator.addProvider(new FactoryIOItemModelGenerator(generator, FactoryIO.MOD_ID, existingFileHelper));
        generator.addProvider(new FactoryIOItemTagsGenerator(generator, FactoryIO.MOD_ID, existingFileHelper));
    }
}
