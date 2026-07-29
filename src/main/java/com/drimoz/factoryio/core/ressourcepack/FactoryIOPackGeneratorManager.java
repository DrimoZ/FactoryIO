package com.drimoz.factoryio.core.ressourcepack;

import com.drimoz.factoryio.FactoryIO;
import com.drimoz.factoryio.core.datagen.generator.*;
import com.drimoz.factoryio.core.registery.FactoryIOTranslations;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import net.minecraft.DetectedVersion;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.DataGenerator;
import net.minecraft.data.PackOutput;
import net.minecraft.data.registries.VanillaRegistries;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.data.ExistingFileHelper;
import net.minecraftforge.fml.ModLoader;
import net.minecraftforge.fml.loading.FMLEnvironment;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;

public class FactoryIOPackGeneratorManager {
    private static DataGenerator generator;
    private static boolean hasGenerated = false;

    public static void generate() {
        if (hasGenerated) return;

        try {
            if (!ModLoader.isLoadingStateValid()) {
                return;
            }

            buildGenerator();
            generator.run();
            writePackMeta();
            hasGenerated = true;
        } catch (Exception e) {
            FactoryIO.LOGGER.error("Génération du pack {} impossible", FactoryIORepositorySource.CONFIG_DIR, e);
        }
    }

    /**
     * Écrit le {@code pack.mcmeta} du pack généré.
     *
     * <p>Indispensable depuis que {@code FactoryIOPackResources} ne surcharge plus
     * {@code getMetadataSection} : Minecraft lit ce fichier directement (cf. BUG-005).
     * Les deux formats Forge cohabitent afin que le même dossier serve de resource pack
     * et de data pack.
     */
    private static void writePackMeta() throws IOException {
        Path meta = FactoryIORepositorySource.CONFIG_DIR.resolve("pack.mcmeta");

        String json = """
                {
                  "pack": {
                    "description": "%s",
                    "pack_format": %d,
                    "forge:resource_pack_format": %d,
                    "forge:data_pack_format": %d
                  }
                }
                """.formatted(
                FactoryIOResourcePackHandler.PACK_DESCRIPTION,
                EPackType.DATA.getPackFormat(),
                EPackType.RESOURCE.getPackFormat(),
                EPackType.DATA.getPackFormat());

        Files.createDirectories(FactoryIORepositorySource.CONFIG_DIR);
        Files.writeString(meta, json, StandardCharsets.UTF_8);
    }

    /**
     * Construit le générateur et ses providers.
     *
     * <p><b>Doit rester paresseux.</b> Appeler ceci depuis le constructeur du mod
     * déclenchait {@code VanillaRegistries.createLookup()} pendant la phase CONSTRUCT,
     * sur le pool commun — donc en concurrence avec la construction des autres mods et
     * avant que les registres vanilla ne soient prêts. Résultat : le mod {@code forge}
     * lui-même échouait à se construire
     * ({@code NoSuchMethodException: EntityJoinLevelEvent.<init>()}).
     *
     * <p>Appelé depuis {@link #generate()}, au moment où le pack est réellement ouvert,
     * bien après le chargement des mods.
     */
    private static void buildGenerator() {
        if (generator != null) return;

        generator = new DataGenerator(
                FactoryIORepositorySource.CONFIG_DIR, DetectedVersion.BUILT_IN, /* alwaysGenerate */ true);

        ExistingFileHelper efh = new ExistingFileHelper(ImmutableList.of(), ImmutableSet.of(), false, null, null);
        PackOutput output = generator.getPackOutput();

        // Les tags et les loot tables ont besoin d'un registre chargé. Hors du contexte
        // GatherDataEvent on ne dispose pas de HolderLookup.Provider, on part donc du
        // registre intégré.
        CompletableFuture<HolderLookup.Provider> lookup =
                CompletableFuture.supplyAsync(VanillaRegistries::createLookup);

        generator.addProvider(true, new FactoryIOLootGenerator(output));

        FactoryIOTranslations.getINSTANCE().getTranslationList().forEach(translationCode ->
                generator.addProvider(true, new FactoryIOLangGenerator(output, FactoryIO.MOD_ID, translationCode)));

        FactoryIOBlockTagsGenerator blockTags =
                new FactoryIOBlockTagsGenerator(output, lookup, FactoryIO.MOD_ID, efh);

        generator.addProvider(true, blockTags);
        generator.addProvider(true, new FactoryIOItemTagsGenerator(
                output, lookup, blockTags.contentsGetter(), FactoryIO.MOD_ID, efh));

        if (FMLEnvironment.dist != Dist.DEDICATED_SERVER) {
            generator.addProvider(true, new FactoryIOBlockModelGenerator(output, FactoryIO.MOD_ID, efh));
            generator.addProvider(true, new FactoryIOItemModelGenerator(output, FactoryIO.MOD_ID, efh));
        }
    }
}
