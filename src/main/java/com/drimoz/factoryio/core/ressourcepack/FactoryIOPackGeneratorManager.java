package com.drimoz.factoryio.core.ressourcepack;

import com.drimoz.factoryio.FactoryIO;
import com.drimoz.factoryio.core.datagen.generator.*;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import net.minecraft.DetectedVersion;
import net.minecraft.data.DataGenerator;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.data.ExistingFileHelper;
import net.minecraftforge.fml.ModLoader;
import net.minecraftforge.fml.loading.FMLEnvironment;

import java.util.List;

public class FactoryIOPackGeneratorManager {
    private static DataGenerator generator;
    private static boolean hasGenerated = false;

    public static void generate() {
        if (!hasGenerated) {
            try {
                if (!ModLoader.isLoadingStateValid()) {
                    return;
                }
                generator.run();
                hasGenerated = true;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public static void registerDataProviders() {
        generator = createDataGenerator();
        ExistingFileHelper efh = new ExistingFileHelper(ImmutableList.of(), ImmutableSet.of(), false, null, null);

        generator.addProvider(new FactoryIOLootGenerator(generator));
        generator.addProvider(new FactoryIOLangGenerator(generator, FactoryIO.MOD_ID, "en_us"));

        if (FMLEnvironment.dist != Dist.DEDICATED_SERVER) {
            generator.addProvider(new FactoryIOBlockModelGenerator(generator, FactoryIO.MOD_ID, efh));
            generator.addProvider(new FactoryIOItemModelGenerator(generator, FactoryIO.MOD_ID, efh));
            generator.addProvider(new FactoryIOItemTagsGenerator(generator, FactoryIO.MOD_ID, efh));
            generator.addProvider(new FactoryIOBlockTagsGenerator(generator, FactoryIO.MOD_ID, efh));
        }
    }

    public static DataGenerator createDataGenerator() {
        generator = new DataGenerator(FactoryIORepositorySource.CONFIG_DIR, List.of());
        return generator;
    }
}
